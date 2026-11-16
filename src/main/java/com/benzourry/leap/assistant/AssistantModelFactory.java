package com.benzourry.leap.assistant;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

/** Creates OpenAI-compatible clients from the creator's assistant settings. */
@Component
public class AssistantModelFactory {

    AssistantDtos.ModelSettings normalize(AssistantDtos.ModelSettings settings) {
        if (settings == null || settings.apiBaseUrl() == null || settings.model() == null
                || settings.apiKey() == null) {
            throw new IllegalArgumentException("Enter the assistant API base URL, model, and API key.");
        }

        String baseUrl = settings.apiBaseUrl().trim().replaceAll("/+$", "");
        String model = settings.model().trim();
        String apiKey = settings.apiKey().trim();
        if (baseUrl.isBlank() || model.isBlank() || apiKey.isBlank()) {
            throw new IllegalArgumentException("Enter the assistant API base URL, model, and API key.");
        }
        if (baseUrl.endsWith("/chat/completions")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - "/chat/completions".length());
        }

        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Enter a valid assistant API base URL.");
        }
        if (!uri.isAbsolute() || uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("The assistant API base URL must be an HTTP or HTTPS URL.");
        }

        return new AssistantDtos.ModelSettings(baseUrl, model, apiKey);
    }

    ChatModel createChatModel(AssistantDtos.ModelSettings settings, String responseFormat) {
        return OpenAiChatModel.builder()
                .apiKey(settings.apiKey())
                .baseUrl(settings.apiBaseUrl())
                .modelName(settings.model())
                .temperature(0.0)
                .responseFormat(responseFormat)
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    StreamingChatModel createStreamingChatModel(AssistantDtos.ModelSettings settings) {
        return OpenAiStreamingChatModel.builder()
                .apiKey(settings.apiKey())
                .baseUrl(settings.apiBaseUrl())
                .modelName(settings.model())
                .temperature(0.0)
                .timeout(Duration.ofMinutes(10))
                .build();
    }
}
