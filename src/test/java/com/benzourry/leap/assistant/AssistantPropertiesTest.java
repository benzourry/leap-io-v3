package com.benzourry.leap.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the property names documented in {@code application.properties}: they are the per
 * deployment override path, so a renamed field would silently stop being configurable.
 */
class AssistantPropertiesTest {

    @Test
    void bindsEveryDocumentedKeyAndLeavesTheRestAtTheirDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("leap.assistant.enabled", "false")
                .withProperty("leap.assistant.cogna-code", "company-assistant")
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
        assertEquals("company-assistant", properties.getCognaCode());
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
        assertEquals("design-assistant", properties.getCognaCode());
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
                new AssistantProperties(), null, null, new com.fasterxml.jackson.databind.ObjectMapper());

        assertThrowsIllegalArgument(() -> service.checkRequest(null));
        assertThrowsIllegalArgument(() -> service.checkRequest(
                new AssistantDtos.PlanRequest("   ", null, null, null)));
        assertThrowsIllegalArgument(() -> service.checkRequest(
                new AssistantDtos.PlanRequest("x".repeat(4001), null, null, null)));

        // A normal request passes.
        service.checkRequest(new AssistantDtos.PlanRequest("build a leave form", null, null, null));
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
