package com.benzourry.leap.assistant;

import com.benzourry.leap.repository.KeyValueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Drives the streaming path with a fake model so the marker handling can be checked without a
 * provider: only the prose may reach the client while the model is still writing, and the tail
 * after the marker must always be a plan the browser can apply.
 */
class AssistantStreamTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** A model that answers with fixed chunks of {@code answer}. */
    private static StreamingChatModel fakeModel(String answer, int chunkSize) {
        return fakeModel(answer, chunkSize, request -> { });
    }

    private static StreamingChatModel fakeModel(
            String answer, int chunkSize, Consumer<ChatRequest> onRequest) {
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                onRequest.accept(request);
                for (int i = 0; i < answer.length(); i += chunkSize) {
                    handler.onPartialResponse(answer.substring(i, Math.min(answer.length(), i + chunkSize)));
                }
                handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("")).build());
            }
        };
    }

    private AssistantService serviceFor(StreamingChatModel model) {
        AssistantProperties properties = new AssistantProperties();
        AssistantModelFactory factory = mock(AssistantModelFactory.class);
        when(factory.normalize(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(factory.createStreamingChatModel(any())).thenReturn(model);
        return new AssistantService(properties, factory, mapper, mock(KeyValueRepository.class));
    }

    private AssistantDtos.PlanRequest request() {
        return new AssistantDtos.PlanRequest(
                "make a leave form", List.of(), mapper.createObjectNode(),
                new AssistantDtos.ModelSettings("https://api.example.com/v1", "test-model", "test-key"));
    }

    private String stream(String answer, int chunkSize) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        stream(request(), fakeModel(answer, chunkSize), out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private void stream(AssistantDtos.PlanRequest request, StreamingChatModel model, ByteArrayOutputStream out)
            throws Exception {
        serviceFor(model).streamPlan(7, request, out);
    }

    private JsonNode planOf(String body) throws Exception {
        int marker = body.indexOf(AssistantPrompts.PLAN_MARKER);
        assertTrue(marker >= 0, "no plan marker in: " + body);
        return mapper.readTree(body.substring(marker + AssistantPrompts.PLAN_MARKER.length()).trim());
    }

    @Test
    void relaysTheProseThenASanitisedPlan() throws Exception {
        String answer = """
                I will create a leave request form with the fields you listed.
                <!--PLAN-->
                {"message": "model summary", "actions": [
                  {"id": "leave", "kind": "create_form", "payload": {"title": "Leave"}},
                  {"id": "bad", "kind": "not_a_real_step", "payload": {}}
                ]}""";

        String body = stream(answer, 7);
        int marker = body.indexOf(AssistantPrompts.PLAN_MARKER);

        assertEquals("I will create a leave request form with the fields you listed.",
                body.substring(0, marker).trim());

        JsonNode plan = planOf(body);
        assertEquals(1, plan.path("actions").size());
        assertEquals("create_form", plan.path("actions").get(0).path("kind").asText());
        assertEquals("Leave", plan.path("actions").get(0).path("title").asText());
        // Streaming clears the JSON message so the drawer keeps the prose it already rendered.
        assertEquals("", plan.path("message").asText());
        assertTrue(plan.path("warnings").toString().contains("1 proposed step"));
    }

    @Test
    void fullBodyIsProseThenMarkerThenPlan() throws Exception {
        String body = stream("Adding a chart dashboard.\n<!--PLAN-->\n{\"actions\": []}", 3);

        int marker = body.indexOf(AssistantPrompts.PLAN_MARKER);
        assertEquals("Adding a chart dashboard.", body.substring(0, marker).trim());
        assertFalse(body.substring(marker + AssistantPrompts.PLAN_MARKER.length()).contains("Adding"));
        assertEquals(0, planOf(body).path("actions").size());
    }

    @Test
    void keepsTheAnswerWhenTheModelNeverEmitsTheMarker() throws Exception {
        String body = stream("{\"actions\": [{\"id\": \"f\", \"kind\": \"create_form\", \"payload\": {\"title\": \"A\"}}]}", 11);

        JsonNode plan = planOf(body);
        assertEquals(1, plan.path("actions").size());
    }

    @Test
    void keepsScreenDslPlaceholdersLiteralInTheModelRequest() throws Exception {
        AtomicReference<ChatRequest> captured = new AtomicReference<>();
        JsonNode context = mapper.readTree("""
                {"knowledge": {"screenDsl": "<strong>{{ row.code }}</strong>"}}
                """);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        stream(request(context), fakeModel(
                "I will preserve the template expression.\n<!--PLAN-->\n{\"actions\":[]}",
                16,
                captured::set), out);

        assertTrue(captured.get().messages().toString().contains("{{ row.code }}"));
        assertEquals(0, planOf(out.toString(StandardCharsets.UTF_8)).path("actions").size());
    }

    private AssistantDtos.PlanRequest request(JsonNode context) {
        return new AssistantDtos.PlanRequest(
                "make a screen", List.of(),
                context,
                new AssistantDtos.ModelSettings("https://api.example.com/v1", "test-model", "test-key"));
    }
}
