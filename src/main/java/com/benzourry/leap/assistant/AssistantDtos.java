package com.benzourry.leap.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Transport objects for {@code /api/assistant}. They mirror the Angular contract one to one. */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    /** A single turn of the conversation that the drawer replays back to us. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelSettings(String apiBaseUrl, String model, String apiKey) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileRequest(String name,
                                 String apiBaseUrl,
                                 String model,
                                 String apiKey,
                                 String additionalInstructions,
                                 Integer maxActions) {
    }

    public record ProfileResponse(Long id,
                                  String name,
                                  String apiBaseUrl,
                                  String model,
                                  String additionalInstructions,
                                  Integer maxActions,
                                  boolean selected,
                                  boolean apiKeyConfigured) {
    }

    /**
     * Request body for both {@code /plan} and {@code /plan-stream}.
     *
     * <p>{@code context} is kept as a raw tree on purpose: it is produced by the browser
     * ({@code AssistantContextService}) and forwarded to the model verbatim, so the backend never
     * needs to be redeployed when the frontend starts describing more entities.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlanRequest(String prompt,
                              List<ChatMessage> history,
                              JsonNode context,
                              ModelSettings settings,
                              Long assistantId,
                              String additionalInstructions,
                              Integer maxActions) {
        public PlanRequest(String prompt, List<ChatMessage> history, JsonNode context, ModelSettings settings) {
            this(prompt, history, context, settings, null, null, null);
        }
    }
}
