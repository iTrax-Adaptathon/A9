package com.syncslot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncslot.config.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal client for an OpenAI-compatible chat-completions provider.
 *
 * <p>Shared by every AI feature so the provider configuration, timeouts and
 * response parsing live in one place. It is purely a transport: it never makes
 * scheduling decisions.
 */
@Service
public class LlmClient {

    private final AiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LlmClient(AiProperties properties, RestClient.Builder restClientBuilder,
                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(1, properties.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = restClientBuilder.requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    /**
     * Sends a system + user prompt and returns the model's reply.
     *
     * @return the reply text, or {@code null} when the client is not configured
     *         (or the provider returned an empty reply). Transport / parsing
     *         failures are thrown so callers can fall back deterministically.
     */
    public String complete(String systemPrompt, String userPrompt) throws JsonProcessingException {
        if (!isConfigured()) {
            return null;
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        body.put("messages", messages);

        JsonNode response = objectMapper.readTree(restClient.post()
                .uri(properties.getUrl())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));

        if (response == null) {
            return null;
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() || content.isNull() ? null : content.asText();
    }
}
