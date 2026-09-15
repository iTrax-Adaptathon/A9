package com.syncslot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the optional AI explanation feature.
 *
 * <p>The API key is never hardcoded: it is read from the {@code AI_API_KEY}
 * environment variable (see application.properties). When it is blank the
 * feature degrades gracefully to the deterministic explanation.
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** OpenAI-compatible chat-completions endpoint. */
    private String url = "https://api.deepseek.com/chat/completions";

    /** Bearer token for the provider. Supplied via the AI_API_KEY env var. */
    private String apiKey = "";

    /** Model name understood by the configured provider. */
    private String model = "deepseek-chat";

    /** Connect / read timeout in seconds. */
    private int timeoutSeconds = 20;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
