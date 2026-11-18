package com.benzourry.leap.assistant;

import com.benzourry.leap.repository.KeyValueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Guards the property names documented in {@code application.properties}: they are the per
 * deployment override path, so a renamed field would silently stop being configurable.
 */
class AssistantPropertiesTest {

    @Test
    void bindsEveryDocumentedKeyAndLeavesTheRestAtTheirDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("leap.assistant.enabled", "false")
                .withProperty("leap.assistant.max-actions", "5")
                .withProperty("leap.assistant.max-history", "3")
                .withProperty("leap.assistant.max-message-chars", "250")
                .withProperty("leap.assistant.max-context-chars", "9000")
                .withProperty("leap.assistant.stream-timeout-seconds", "45")
                .withProperty("leap.assistant.extra-instructions", "Always add an audit section.");

        AssistantProperties properties = Binder.get(environment)
                .bind("leap.assistant", Bindable.of(AssistantProperties.class))
                .orElseGet(AssistantProperties::new);

        assertFalse(properties.isEnabled());
        assertEquals(5, properties.getMaxActions());
        assertEquals(3, properties.getMaxHistory());
        assertEquals(250, properties.getMaxMessageChars());
        assertEquals(9000, properties.getMaxContextChars());
        assertEquals(45, properties.getStreamTimeoutSeconds());
        assertEquals("Always add an audit section.", properties.getExtraInstructions());
    }

    @Test
    void usesUsableDefaultsWhenNothingIsConfigured() {
        AssistantProperties properties = new AssistantProperties();

        assertTrue(properties.isEnabled());
        assertTrue(properties.getMaxActions() > 0);
        assertTrue(properties.getMaxHistory() > 0);
        assertTrue(properties.getMaxMessageChars() > 0);
        assertTrue(properties.getMaxContextChars() > properties.getMaxMessageChars());
        assertTrue(properties.getStreamTimeoutSeconds() >= 30);
        assertNotNull(properties.getExtraInstructions());
        assertTrue(properties.getExtraInstructions().isEmpty());
    }

    @Test
    void rejectsRequestsThatCannotBeServed() {
        AssistantService service = new AssistantService(
                new AssistantProperties(), new AssistantModelFactory(),
                new com.fasterxml.jackson.databind.ObjectMapper(), mock(KeyValueRepository.class));

        assertThrowsIllegalArgument(() -> service.checkRequest(null));
        assertThrowsIllegalArgument(() -> service.checkRequest(
                new AssistantDtos.PlanRequest("   ", null, null, null)));
        assertThrowsIllegalArgument(() -> service.checkRequest(
                new AssistantDtos.PlanRequest("x".repeat(4001), null, null, null)));
        assertThrowsIllegalArgument(() -> service.checkRequest(
                new AssistantDtos.PlanRequest("build a leave form", null, null, null)));

        // A normal request passes.
        service.checkRequest(new AssistantDtos.PlanRequest("build a leave form", null, null,
                new AssistantDtos.ModelSettings("https://api.example.com/v1", "model", "key")));
    }

    @Test
    void rejectsUnsupportedAndOversizedAttachments() {
        AssistantService service = new AssistantService(
                new AssistantProperties(), new AssistantModelFactory(),
                new com.fasterxml.jackson.databind.ObjectMapper(), mock(KeyValueRepository.class));

        AssistantDtos.Attachment unsupported = new AssistantDtos.Attachment(
                "form.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                Base64.getEncoder().encodeToString(new byte[]{1}));
        assertThrows(IllegalArgumentException.class, () -> service.checkRequest(
                new AssistantDtos.PlanRequest("create a form", null, null, null,
                        null, null, null, List.of(unsupported))));

        AssistantDtos.Attachment oversized = new AssistantDtos.Attachment(
                "large.pdf", "application/pdf",
                Base64.getEncoder().encodeToString(new byte[AssistantDtos.MAX_ATTACHMENT_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> service.checkRequest(
                new AssistantDtos.PlanRequest("create a form", null, null, null,
                        null, null, null, List.of(oversized))));
    }

    private static void assertThrowsIllegalArgument(Runnable call) {
        try {
            call.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected an IllegalArgumentException");
    }
}
