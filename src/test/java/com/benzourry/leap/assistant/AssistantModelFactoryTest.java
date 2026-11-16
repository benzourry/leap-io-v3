package com.benzourry.leap.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistantModelFactoryTest {

    private final AssistantModelFactory factory = new AssistantModelFactory();

    @Test
    void normalizesSettingsAndAcceptsAChatCompletionsUrl() {
        AssistantDtos.ModelSettings normalized = factory.normalize(new AssistantDtos.ModelSettings(
                "https://provider.example/v1/chat/completions/",
                " test-model ",
                " test-key "));

        assertEquals("https://provider.example/v1", normalized.apiBaseUrl());
        assertEquals("test-model", normalized.model());
        assertEquals("test-key", normalized.apiKey());
    }

    @Test
    void rejectsNonHttpOrCredentialBearingUrls() {
        assertThrows(IllegalArgumentException.class, () -> factory.normalize(
                new AssistantDtos.ModelSettings("file:///etc/passwd", "model", "key")));
        assertThrows(IllegalArgumentException.class, () -> factory.normalize(
                new AssistantDtos.ModelSettings("https://user:pass@provider.example/v1", "model", "key")));
    }
}
