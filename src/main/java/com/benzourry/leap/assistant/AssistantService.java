package com.benzourry.leap.assistant;

import com.benzourry.leap.repository.KeyValueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Turns a creator's request into a reviewable plan of steps.
 *
 * <p>The assistant is a pure planner: it reads the app through the context the drawer sends and
 * answers with steps the browser applies itself through the normal editor services. That keeps
 * permissions, validation and restore points where they already live, and means a bad answer can
 * never corrupt an app.
 * </p>
 */
@Service
public class AssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);

    /** Sentinel pushed on the token queue when the model is done. */
    private static final Object END = new Object();

    private static final int MAX_PDF_TEXT_CHARS = 100_000;

    private static final String VERSION = "1.0";

    private final AssistantProperties properties;
    private final AssistantModelFactory modelFactory;
    private final ObjectMapper mapper;
    private final KeyValueRepository keyValueRepository;

    public AssistantService(AssistantProperties properties,
                            AssistantModelFactory modelFactory,
                            ObjectMapper MAPPER,
                            KeyValueRepository keyValueRepository) {
        this.properties = properties;
        this.modelFactory = modelFactory;
        this.mapper = MAPPER;
        this.keyValueRepository = keyValueRepository;
    }

    // ------------------------------------------------------------------ health

    /**
     * Availability probe. Never throws: the drawer hides itself when {@code enabled} is false.
     */
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", VERSION);
        if (!properties.isEnabled()) {
            result.put("enabled", false);
            result.put("reason", "The assistant is switched off (leap.assistant.enabled=false).");
            return result;
        }
        result.put("enabled", true);
        result.put("streaming", true);
        return result;
    }

    // -------------------------------------------------------------------- plan

    /**
     * Verifies that the assistant can serve a request before a streaming response is committed.
     */
    public void requireReady(AssistantDtos.PlanRequest request) {
        requireEnabled();
        AssistantDtos.ModelSettings settings = modelFactory.normalize(request == null ? null : request.settings());
        try {
            modelFactory.createStreamingChatModel(settings);
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("The assistant model could not be configured: "
                    + rootMessage(e), e);
        }
    }

    /**
     * Rejects requests that cannot be served as written, before any model call is made.
     *
     * @throws IllegalArgumentException mapped to HTTP 400 by the controller
     */
    public void checkRequest(AssistantDtos.PlanRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("Describe what you want the assistant to build.");
        }
        int max = properties.getMaxMessageChars();
        if (max > 0 && request.prompt().length() > max) {
            throw new IllegalArgumentException("Keep the request under " + max
                    + " characters; " + request.prompt().length() + " were sent.");
        }
        validateAttachments(request.attachments());
        requireEnabled();
        modelFactory.normalize(request.settings());
    }

    /** Non-streaming variant. Used as a fallback and for clients without progressive rendering. */
    public ObjectNode plan(long appId, AssistantDtos.PlanRequest request) {
        AssistantDtos.ModelSettings settings = modelFactory.normalize(request.settings());
        String answer;
        ChatRequest modelRequest = chatRequest(
                AssistantPrompts.userPrompt(request, properties), buildSystemPrompt(false), request);
        try {
            ChatResponse response = blockingModel(settings).chat(modelRequest);
            answer = response.aiMessage().text();
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("The assistant model failed: " + rootMessage(e), e);
        }
        ObjectNode plan = sanitizer().sanitize(parseJsonObject(answer), false);
        logger.info("Assistant plan for app {}: {} step(s)", appId, plan.path("actions").size());
        return plan;
    }

    // ------------------------------------------------------------- plan-stream

    /**
     * Streams the explanation token by token, then the JSON plan.
     *
     * <p>The model is asked to answer {@code <prose> <!--PLAN--> <json>}. Only the prose is relayed
     * live; everything after the separator is held back so this method can guarantee that the tail
     * the browser parses is valid JSON, repairing it with a second call when needed.
     * </p>
     */
    public void streamPlan(long appId, AssistantDtos.PlanRequest request, OutputStream out) throws IOException {
        AssistantDtos.ModelSettings settings = modelFactory.normalize(request.settings());
        ChatRequest modelRequest;
        try {
            modelRequest = chatRequest(
                    AssistantPrompts.userPrompt(request, properties), buildSystemPrompt(true), request);
        } catch (IllegalArgumentException e) {
            write(out, "\n\n" + AssistantPrompts.PLAN_MARKER + "\n" + problemPlan(e.getMessage()));
            return;
        }

        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        try {
            StreamingChatModel model = modelFactory.createStreamingChatModel(settings);
            model.chat(modelRequest,
                    new StreamingChatResponseHandler() {
                        @Override
                        public void onPartialResponse(String token) {
                            if (token != null && !token.isEmpty()) {
                                queue.add(token);
                            }
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            queue.add(END);
                        }

                        @Override
                        public void onError(Throwable error) {
                            queue.add(error);
                        }
                    });
        } catch (RuntimeException e) {
            throw new AssistantUnavailableException("The assistant model could not be started: " + rootMessage(e), e);
        }

        String raw;
        try {
            raw = drain(queue, out);
        } catch (IOException failure) {
            // The response is already committed, so report the problem inside the protocol.
            logger.warn("Assistant stream failed for app {}: {}", appId, failure.getMessage());
            write(out, "\n\n" + AssistantPrompts.PLAN_MARKER + "\n" + problemPlan(failure.getMessage()));
            return;
        }
        ObjectNode plan = sanitizer().sanitize(resolvePlan(settings, request, raw), true);

        write(out, "\n\n" + AssistantPrompts.PLAN_MARKER + "\n" + plan);
        logger.info("Assistant stream for app {}: {} step(s)", appId, plan.path("actions").size());
    }

    /** A plan-shaped answer used when the stream cannot be completed. */
    private ObjectNode problemPlan(String reason) {
        ObjectNode plan = mapper.createObjectNode();
        plan.put("planId", UUID.randomUUID().toString());
        plan.put("message", "");
        plan.putArray("actions");
        plan.putArray("warnings").add(reason == null || reason.isBlank()
                ? "The assistant stopped responding. Try again."
                : reason);
        return plan;
    }

    /**
     * Relays the prose while the model is writing and returns everything it produced.
     */
    private String drain(BlockingQueue<Object> queue, OutputStream out) throws IOException {
        int keep = AssistantPrompts.PLAN_MARKER.length() - 1;
        long deadline = System.currentTimeMillis()
                + Math.max(30, properties.getStreamTimeoutSeconds()) * 1000L;

        StringBuilder raw = new StringBuilder();
        StringBuilder held = new StringBuilder();
        boolean inPlan = false;

        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                throw new IOException("The assistant stopped responding before it finished.");
            }
            Object item;
            try {
                item = queue.poll(remaining, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("The assistant request was interrupted.", e);
            }
            if (item == null) {
                throw new IOException("The assistant stopped responding before it finished.");
            }
            if (item == END) {
                break;
            }
            if (item instanceof Throwable failure) {
                throw new IOException("The assistant model failed: " + rootMessage(failure), failure);
            }

            String token = (String) item;
            raw.append(token);
            if (inPlan) {
                continue;
            }
            held.append(token);
            int marker = held.indexOf(AssistantPrompts.PLAN_MARKER);
            if (marker >= 0) {
                write(out, held.substring(0, marker));
                inPlan = true;
                held.setLength(0);
            } else if (held.length() > keep) {
                int cut = held.length() - keep;
                write(out, held.substring(0, cut));
                held.delete(0, cut);
            }
        }

        if (!inPlan) {
            write(out, held.toString());
        }
        return raw.toString();
    }

    private JsonNode resolvePlan(AssistantDtos.ModelSettings settings, AssistantDtos.PlanRequest request, String raw) {
        int marker = raw.indexOf(AssistantPrompts.PLAN_MARKER);
        JsonNode parsed = parseJsonObject(
                marker >= 0 ? raw.substring(marker + AssistantPrompts.PLAN_MARKER.length()) : raw);
        if (parsed == null) {
            parsed = repairPlan(settings, request);
        }
        return parsed;
    }

    /** Second, cheap call used when the streamed answer did not contain readable JSON. */
    private JsonNode repairPlan(AssistantDtos.ModelSettings settings, AssistantDtos.PlanRequest request) {
        ChatModel model;
        try {
            model = blockingModel(settings);
        } catch (RuntimeException e) {
            logger.warn("Assistant repair skipped: {}", e.getMessage());
            return null;
        }
        try {
            ChatResponse response = model.chat(chatRequest(
                    AssistantPrompts.repairPrompt(request, properties),
                    buildSystemPrompt(false),
                    request));
            String answer = response.aiMessage().text();
            JsonNode parsed = parseJsonObject(answer);
            if (parsed == null) {
                logger.warn("Assistant repair produced unreadable output");
            }
            return parsed;
        } catch (RuntimeException e) {
            logger.warn("Assistant repair failed: {}", rootMessage(e));
            return null;
        }
    }

    // ----------------------------------------------------------------- helpers

    private String buildSystemPrompt(boolean streaming) {
        String actionSchema = keyValueRepository.getValue("platform", "ai_assistant_action_schema").orElse(null);
        String rules = keyValueRepository.getValue("platform", "ai_assistant_rules").orElse(null);
        return AssistantPrompts.systemPrompt(properties, streaming, actionSchema, rules);
    }

    private AssistantPlanSanitizer sanitizer() {
        return new AssistantPlanSanitizer(mapper, properties.getMaxActions());
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new AssistantUnavailableException("The assistant is switched off (leap.assistant.enabled=false).");
        }
    }

    private ChatModel blockingModel(AssistantDtos.ModelSettings settings) {
        ChatModel model;
        try {
            model = modelFactory.createChatModel(settings, "json_object");
        } catch (RuntimeException e) {
            logger.warn("Assistant JSON mode unavailable: {}", rootMessage(e));
            model = modelFactory.createChatModel(settings, null);
        }
        return model;
    }

    private ChatRequest chatRequest(String userPrompt, String systemPrompt, AssistantDtos.PlanRequest request) {
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(userPrompt));
        if (request.attachments() != null) {
            for (AssistantDtos.Attachment attachment : request.attachments()) {
                byte[] bytes = decodeAttachment(attachment);
                String mimeType = attachment.mimeType().trim().toLowerCase(Locale.ROOT);
                if ("application/pdf".equals(mimeType)) {
                    contents.add(TextContent.from("Extracted text from PDF attachment \""
                            + AssistantDtos.safeAttachmentName(attachment.name())
                            + "\":\n" + extractPdfText(bytes, attachment)));
                } else {
                    contents.add(TextContent.from("Image attachment \""
                            + AssistantDtos.safeAttachmentName(attachment.name()) + "\" follows."));
                    contents.add(ImageContent.from("data:" + mimeType
                            + ";base64," + attachment.dataBase64()));
                }
            }
        }
        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt + "\nToday is " + LocalDate.now() + "."),
                UserMessage.from(contents));
        return ChatRequest.builder().messages(messages).build();
    }

    private static void validateAttachments(List<AssistantDtos.Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        if (attachments.size() > AssistantDtos.MAX_ATTACHMENTS) {
            throw new IllegalArgumentException("Attach no more than "
                    + AssistantDtos.MAX_ATTACHMENTS + " files.");
        }

        int totalBytes = 0;
        for (AssistantDtos.Attachment attachment : attachments) {
            byte[] bytes = decodeAttachment(attachment);
            totalBytes += bytes.length;
            if (totalBytes > AssistantDtos.MAX_ATTACHMENT_TOTAL_BYTES) {
                throw new IllegalArgumentException("Attachments cannot exceed 8 MB total.");
            }
        }
    }

    private static byte[] decodeAttachment(AssistantDtos.Attachment attachment) {
        if (attachment == null || attachment.name() == null || attachment.name().isBlank()) {
            throw new IllegalArgumentException("Every attachment must have a file name.");
        }
        String mimeType = attachment.mimeType() == null ? "" : attachment.mimeType().trim().toLowerCase(Locale.ROOT);
        if (!"application/pdf".equals(mimeType)
                && !"image/png".equals(mimeType)
                && !"image/jpeg".equals(mimeType)
                && !"image/gif".equals(mimeType)
                && !"image/webp".equals(mimeType)) {
            throw new IllegalArgumentException("Only PDF, PNG, JPEG, GIF, and WebP files can be attached.");
        }
        String encoded = attachment.dataBase64();
        int maxEncodedBytes = ((AssistantDtos.MAX_ATTACHMENT_BYTES + 2) / 3) * 4;
        if (encoded == null || encoded.isBlank() || encoded.length() > maxEncodedBytes) {
            throw new IllegalArgumentException("Attachment \"" + AssistantDtos.safeAttachmentName(attachment.name())
                    + "\" is empty or exceeds the 4 MB per-file limit.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Attachment \"" + AssistantDtos.safeAttachmentName(attachment.name())
                    + "\" could not be decoded.", e);
        }
        if (bytes.length == 0 || bytes.length > AssistantDtos.MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachment \"" + AssistantDtos.safeAttachmentName(attachment.name())
                    + "\" is empty or exceeds the 4 MB per-file limit.");
        }
        if ("application/pdf".equals(mimeType) && !hasPdfSignature(bytes)) {
            throw new IllegalArgumentException("Attachment \"" + AssistantDtos.safeAttachmentName(attachment.name())
                    + "\" is not a valid PDF file.");
        }
        return bytes;
    }

    private static String extractPdfText(byte[] bytes, AssistantDtos.Attachment attachment) {
        PdfReader reader;
        try {
            reader = new PdfReader(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read PDF attachment \""
                    + AssistantDtos.safeAttachmentName(attachment.name()) + "\".", e);
        }
        String extracted;
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                String pageText = PdfTextExtractor.getTextFromPage(reader, page);
                int remaining = MAX_PDF_TEXT_CHARS - text.length();
                if (remaining <= 0) {
                    break;
                }
                text.append(pageText, 0, Math.min(pageText.length(), remaining));
                if (text.length() >= MAX_PDF_TEXT_CHARS) {
                    text.append("\n[PDF text truncated]");
                    break;
                }
                text.append('\n');
            }
            if (text.toString().isBlank()) {
                throw new IllegalArgumentException("PDF attachment \""
                        + AssistantDtos.safeAttachmentName(attachment.name())
                        + "\" contains no selectable text. Attach an image of the form instead.");
            }
            extracted = text.toString();
        } catch (IOException e) {
            reader.close();
            throw new IllegalArgumentException("Could not extract text from PDF attachment \""
                    + AssistantDtos.safeAttachmentName(attachment.name()) + "\".", e);
        } catch (RuntimeException e) {
            reader.close();
            throw e;
        }
        reader.close();
        return extracted;
    }

    private static boolean hasPdfSignature(byte[] bytes) {
        int lastStart = Math.min(bytes.length - 5, 1024);
        for (int i = 0; i <= lastStart; i++) {
            if (bytes[i] == '%' && bytes[i + 1] == 'P' && bytes[i + 2] == 'D'
                    && bytes[i + 3] == 'F' && bytes[i + 4] == '-') {
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts anything a model may return: bare JSON, a fenced block, or prose with a JSON object
     * embedded (which is exactly what the streaming protocol looks like once the marker is stripped).
     */
    JsonNode parseJsonObject(String answer) {
        if (answer == null) {
            return null;
        }
        String text = answer.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
            text = text.trim();
        }
        if (text.startsWith("{")) {
            try {
                JsonNode node = mapper.readTree(text);
                if (node != null && node.isObject()) {
                    return node;
                }
            } catch (Exception ignored) {
                // fall through to the embedded-object extraction below
            }
        }
        return extractJsonObject(text);
    }

    private JsonNode extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = mapper.readTree(text.substring(start, end + 1));
            return node != null && node.isObject() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void write(OutputStream out, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}