package com.benzourry.leap.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the AI app-builder assistant.
 *
 * <p>Every value can be overridden either in {@code application.properties} or, at runtime, through
 * the {@code key_value} table (group {@code app.prop}) which {@code PropsInitializer} publishes as
 * the highest priority property source.</p>
 */
@Component
@ConfigurationProperties(prefix = "leap.assistant")
public class AssistantProperties {

    /** Master switch. When false the drawer reports itself as unavailable. */
    private boolean enabled = true;

    /**
     * Code of the {@code Cogna} row that holds the model configuration (provider, model name, api
     * key, temperature, system message). Reusing a Cogna means the assistant needs no dedicated
     * settings and can use every provider already supported by {@code ChatService}.
     */
    private String cognaCode = "design-assistant";

    /** Hard cap on the number of steps a single plan may contain. */
    private int maxActions = 60;

    /** How many earlier chat messages are replayed back to the model. */
    private int maxHistory = 20;

    /** Characters per individual chat message. */
    private int maxMessageChars = 4000;

    /** Characters for the serialized app context (forms, datasets, dashboards, screens). */
    private int maxContextChars = 120000;

    /** Extensions appended to the built-in system prompt, e.g. house conventions. */
    private String extraInstructions = "";

    /** Upper bound for a single streaming call before the request is abandoned. */
    private int streamTimeoutSeconds = 240;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCognaCode() {
        return cognaCode;
    }

    public void setCognaCode(String cognaCode) {
        this.cognaCode = cognaCode;
    }

    public int getMaxActions() {
        return maxActions;
    }

    public void setMaxActions(int maxActions) {
        this.maxActions = maxActions;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public int getMaxMessageChars() {
        return maxMessageChars;
    }

    public void setMaxMessageChars(int maxMessageChars) {
        this.maxMessageChars = maxMessageChars;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public String getExtraInstructions() {
        return extraInstructions == null ? "" : extraInstructions;
    }

    public void setExtraInstructions(String extraInstructions) {
        this.extraInstructions = extraInstructions;
    }

    public int getStreamTimeoutSeconds() {
        return streamTimeoutSeconds;
    }

    public void setStreamTimeoutSeconds(int streamTimeoutSeconds) {
        this.streamTimeoutSeconds = streamTimeoutSeconds;
    }
}
