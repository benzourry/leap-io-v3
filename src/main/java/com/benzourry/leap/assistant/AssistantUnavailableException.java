package com.benzourry.leap.assistant;

/**
 * Thrown when the assistant cannot serve a request because it is switched off or has no usable
 * model configuration. Mapped to HTTP 503 so the drawer can explain the situation instead of
 * reporting a generic failure.
 */
public class AssistantUnavailableException extends RuntimeException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
