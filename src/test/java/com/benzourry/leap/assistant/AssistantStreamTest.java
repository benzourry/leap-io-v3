package com.benzourry.leap.assistant;

import com.benzourry.leap.model.Cogna;
import com.benzourry.leap.repository.CognaRepository;
import com.benzourry.leap.service.ChatService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
        return new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                for (int i = 0; i < answer.length(); i += chunkSize) {
                    handler.onPartialResponse(answer.substring(i, Math.min(answer.length(), i + chunkSize)));
                }
                handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("")).build());
            }
        };
    }

    private AssistantService serviceFor(StreamingChatModel model) {
        AssistantProperties properties = new AssistantProperties();

        Cogna cogna = new Cogna();
        cogna.setId(1L);
        cogna.setInferModelType("openai");
        cogna.setInferModelName("test-model");

        CognaRepository repository = mock(CognaRepository.class);
        when(repository.findFirstByCode(anyString())).thenReturn(Optional.of(cogna));

        ChatService chatService = mock(ChatService.class);
        when(chatService.getStreamingChatModel(cogna)).thenReturn(model);

        return new AssistantService(properties, repository, chatService, mapper);
    }

    private AssistantDtos.PlanRequest request() {
        return new AssistantDtos.PlanRequest(
                "make a leave form", "me@example.com", List.of(), mapper.createObjectNode());
    }

    private String stream(String answer, int chunkSize) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serviceFor(fakeModel(answer, chunkSize))
                .streamPlan(7, request(), out);
        return out.toString(StandardCharsets.UTF_8);
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
}
