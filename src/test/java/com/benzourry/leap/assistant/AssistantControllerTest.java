package com.benzourry.leap.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the wire contract the Angular drawer is written against: the three URLs, the request body
 * shape, the plain-text streaming headers, and the status codes the drawer translates into hints.
 */
class AssistantControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private AssistantService service;
    private AssistantController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AssistantService.class);
        controller = new AssistantController(service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void healthAnswersWhereTheDrawerLooksForIt() throws Exception {
        when(service.health()).thenReturn(Map.of("enabled", true, "provider", "openai"));

        mvc.perform(get("/api/assistant/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.provider").value("openai"));
    }

    @Test
    void planAcceptsTheBrowserContractAndForwardsTheContext() throws Exception {
        ObjectNode answer = mapper.createObjectNode();
        answer.put("summary", "Adds a leave form");
        when(service.plan(anyLong(), any())).thenReturn(answer);

        String body = """
                {
                  "prompt": "make a leave form",
                  "email": "me@example.com",
                  "history": [{"role": "user", "content": "hi"}],
                  "context": {"app": {"id": 7, "name": "Acme"}}
                }
                """;

        mvc.perform(post("/api/assistant/7/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Adds a leave form"));

        ArgumentCaptor<AssistantDtos.PlanRequest> captor = ArgumentCaptor.forClass(AssistantDtos.PlanRequest.class);
        verify(service).plan(eq(7L), captor.capture());
        AssistantDtos.PlanRequest sent = captor.getValue();
        assertEquals("make a leave form", sent.prompt());
        assertEquals("me@example.com", sent.email());
        assertEquals(1, sent.history().size());
        assertEquals("user", sent.history().get(0).role());
        assertEquals("Acme", sent.context().at("/app/name").asText());
    }

    @Test
    void streamFailsBeforeCommittingSoTheDrawerCanShowTheReason() throws Exception {
        doThrow(new AssistantUnavailableException("No AI configuration is registered"))
                .when(service).requireReady();

        mvc.perform(post("/api/assistant/7/plan-stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"make a leave form\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("No AI configuration is registered"));
    }

    @Test
    void streamAnnouncesUncompressedPlainTextSoTokensArriveOneByOne() throws Exception {
        ResponseEntity<StreamingResponseBody> response = controller.planStream(7L, request());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text/plain;charset=UTF-8", response.getHeaders().getContentType().toString());
        assertEquals("no-cache, no-transform", response.getHeaders().getFirst("Cache-Control"));
        assertEquals("identity", response.getHeaders().getFirst("Content-Encoding"));
        assertEquals("no", response.getHeaders().getFirst("X-Accel-Buffering"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getBody().writeTo(out);
        verify(service).streamPlan(eq(7L), any(), any());
    }

    @Test
    void rejectedPromptsAndMissingConfigurationUseDistinctStatuses() throws Exception {
        doThrow(new IllegalArgumentException("Prompt is required.")).when(service).checkRequest(any());

        mvc.perform(post("/api/assistant/7/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Prompt is required."));

        ResponseEntity<String> unavailable =
                controller.unavailable(new AssistantUnavailableException("Assistant is switched off."));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
        assertTrue(unavailable.getBody().contains("switched off"));
    }

    private AssistantDtos.PlanRequest request() {
        return new AssistantDtos.PlanRequest("make a leave form", "me@example.com", null, mapper.createObjectNode());
    }
}
