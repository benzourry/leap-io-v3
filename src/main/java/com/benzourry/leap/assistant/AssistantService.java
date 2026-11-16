package com.benzourry.leap.assistant;

import com.benzourry.leap.model.Cogna;
import com.benzourry.leap.repository.CognaRepository;
import com.benzourry.leap.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Turns a creator's request into a reviewable plan of steps.
 *
 * <p>The assistant is a pure planner: it reads the app through the context the drawer sends and
 * answers with steps the browser applies itself through the normal editor services. That keeps
 * permissions, validation and restore points where they already live, and means a bad answer can
 * never corrupt an app.</p>
 */
@Service
public class AssistantService {

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);

    /** Sentinel pushed on the token queue when the model is done. */
    private static final Object END = new Object();

    private static final String VERSION = "1.0";

    private final AssistantProperties properties;
    private final CognaRepository cognaRepository;
    private final ChatService chatService;
    private final ObjectMapper mapper;

    /**
     * Stateless assistants keyed by a fingerprint of the model configuration, so a change made in
     * the admin UI takes effect without restarting the server and clients are not rebuilt per call.
     */
    private final Map<String, ChatService.StreamingAssistant> streamingHolders = new ConcurrentHashMap<>();
    private final Map<String, ChatService.Assistant> blockingHolders = new ConcurrentHashMap<>();

    public AssistantService(AssistantProperties properties,
                            CognaRepository cognaRepository,
                            ChatService chatService,
                            ObjectMapper MAPPER) {
        this.properties = properties;
        this.cognaRepository = cognaRepository;
        this.chatService = chatService;
        this.mapper = MAPPER;
    }

    // ------------------------------------------------------------------ health

    /**
     * Availability probe. Never throws: the drawer hides itself when {@code enabled} is false, and
     * the reason is surfaced so an administrator can see what is missing.
     */
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", VERSION);
        if (!properties.isEnabled()) {
            result.put("enabled", false);
            result.put("reason", "The assistant is switched off (leap.assistant.enabled=false).");
            return result;
        }
        try {
            Cogna cogna = requireCogna();
            result.put("enabled", true);
            result.put("model", cogna.getInferModelType() + "/" + cogna.getInferModelName());
            result.put("streaming", cogna.getStreamSupport());
        } catch (AssistantUnavailableException e) {
            result.put("enabled", false);
            result.put("reason", e.getMessage());
        } catch (RuntimeException e) {
            // A probe must never fail loudly: the drawer treats an error as "not available".
            logger.warn("Assistant health check failed: {}", rootMessage(e));
            result.put("enabled", false);
            result.put("reason", "The assistant is not reachable: " + rootMessage(e));
        }
        return result;
    }

    // -------------------------------------------------------------------- plan

    /**
     * Verifies that the assistant can serve a request. Called before a streaming response is
     * committed so a missing configuration surfaces as HTTP 503 instead of a truncated body.
     */
    public void requireReady() {
        requireCogna();
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
    }

    /** Non-streaming variant. Used as a fallback and for clients without progressive rendering. */
    public ObjectNode plan(long appId, AssistantDtos.PlanRequest request) {
        Cogna cogna = requireCogna();
        ChatService.Assistant assistant = blockingAssistant(cogna);
        String answer;
        try {
            answer = assistant.chat(
                    AssistantPrompts.userPrompt(request, properties),
                    List.of(),
                    AssistantPrompts.systemPrompt(properties, false));
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
     * the browser parses is valid JSON, repairing it with a second call when needed.</p>
     */
    public void streamPlan(long appId, AssistantDtos.PlanRequest request, OutputStream out) throws IOException {
        Cogna cogna = requireCogna();

        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        try {
            streamingAssistant(cogna)
                    .chat(AssistantPrompts.userPrompt(request, properties),
                            List.of(),
                            AssistantPrompts.systemPrompt(properties, true))
                    .onPartialResponse(token -> {
                        if (token != null && !token.isEmpty()) {
                            queue.add(token);
                        }
                    })
                    .onCompleteResponse(response -> queue.add(END))
                    .onError(error -> queue.add(error))
                    .start();
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
        ObjectNode plan = sanitizer().sanitize(resolvePlan(cogna, request, raw), true);

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

    private JsonNode resolvePlan(Cogna cogna, AssistantDtos.PlanRequest request, String raw) {
        int marker = raw.indexOf(AssistantPrompts.PLAN_MARKER);
        JsonNode parsed = parseJsonObject(
                marker >= 0 ? raw.substring(marker + AssistantPrompts.PLAN_MARKER.length()) : raw);
        if (parsed == null) {
            parsed = repairPlan(cogna, request);
        }
        return parsed;
    }

    /** Second, cheap call used when the streamed answer did not contain readable JSON. */
    private JsonNode repairPlan(Cogna cogna, AssistantDtos.PlanRequest request) {
        ChatService.Assistant assistant;
        try {
            assistant = blockingAssistant(cogna);
        } catch (AssistantUnavailableException e) {
            logger.warn("Assistant repair skipped: {}", e.getMessage());
            return null;
        }
        try {
            String answer = assistant.chat(
                    AssistantPrompts.repairPrompt(request, properties),
                    List.of(),
                    AssistantPrompts.systemPrompt(properties, false));
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

    private AssistantPlanSanitizer sanitizer() {
        return new AssistantPlanSanitizer(mapper, properties.getMaxActions());
    }

    private Cogna requireCogna() {
        if (!properties.isEnabled()) {
            throw new AssistantUnavailableException("The assistant is switched off (leap.assistant.enabled=false).");
        }
        String code = properties.getCognaCode();
        Cogna cogna = cognaRepository.findFirstByCode(code).orElse(null);
        if (cogna == null) {
            throw new AssistantUnavailableException("No AI configuration is registered for the assistant. "
                    + "Create an AI configuration (Cogna) with the code '" + code + "' and point "
                    + "leap.assistant.cogna-code at it.");
        }
        return cogna;
    }

    private ChatService.StreamingAssistant streamingAssistant(Cogna cogna) {
        return cached(streamingHolders, fingerprint("stream", cogna), key -> {
            StreamingChatModel model = chatService.getStreamingChatModel(cogna);
            if (model == null) {
                throw new AssistantUnavailableException("The AI provider '" + cogna.getInferModelType()
                        + "' is not configured for streaming.");
            }
            return AiServices.builder(ChatService.StreamingAssistant.class)
                    .streamingChatModel(model)
                    .build();
        });
    }

    private ChatService.Assistant blockingAssistant(Cogna cogna) {
        return cached(blockingHolders, fingerprint("block", cogna), key -> {
            ChatModel model = null;
            String format = responseFormatFor(cogna);
            try {
                model = chatService.getChatModel(cogna, format);
            } catch (RuntimeException e) {
                logger.warn("Assistant JSON mode unavailable for '{}': {}", cogna.getInferModelType(), rootMessage(e));
            }
            if (model == null) {
                model = chatService.getChatModel(cogna, null);
            }
            if (model == null) {
                throw new AssistantUnavailableException("The AI provider '" + cogna.getInferModelType()
                        + "' is not configured or is not supported.");
            }
            return AiServices.builder(ChatService.Assistant.class)
                    .chatModel(model)
                    .build();
        });
    }

    /**
     * {@link ChatService#getChatModel} only switches some providers into JSON mode, and it wants a
     * different token for each family, so ask for the one that actually works elsewhere in this app.
     */
    private static String responseFormatFor(Cogna cogna) {
        String type = cogna.getInferModelType();
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "ollama" -> "json";
            case "gemini" -> "json_schema";
            default -> "json_object";
        };
    }

    /** Everything that changes which client the admin would expect to be used. */
    private static String fingerprint(String flavour, Cogna cogna) {
        return flavour + '|' + cogna.getId() + '|' + cogna.getInferModelType() + '|' + cogna.getInferModelName()
                + '|' + cogna.getInferModelApiKey() + '|' + cogna.getTemperature() + '|' + cogna.getMaxToken()
                + '|' + cogna.getData();
    }

    private static <T> T cached(Map<String, T> cache, String key, java.util.function.Function<String, T> factory) {
        // Configurations are edited rarely; a handful of entries is normal and this keeps the map
        // from growing across a long session of edits.
        if (cache.size() > 8) {
            cache.clear();
        }
        return cache.computeIfAbsent(key, factory);
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
