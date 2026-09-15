package com.syncslot.service;

import com.syncslot.dto.AiExplainRequest;
import com.syncslot.dto.AiExplainResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns an already-decided scheduling outcome into a plain-language explanation
 * by calling an OpenAI-compatible chat-completions API through {@link LlmClient}.
 *
 * <p>The AI is strictly a narrator: it receives a description of the result and
 * is told not to make or change any scheduling decisions. The Java
 * SchedulingEngine remains the single source of truth for availability,
 * conflicts, priorities, buffers, slot selection and cascades.
 *
 * <p>Any failure (missing key, network error, malformed response) falls back to
 * the deterministic explanation supplied with the request.
 */
@Service
public class AiExplainService {

    private static final Logger log = LoggerFactory.getLogger(AiExplainService.class);

    private static final String SYSTEM_PROMPT = """
            You are SyncSlot's scheduling explainer.
            Explain the scheduling decision you are given in 2-3 short, simple sentences.
            You must NOT make, change or suggest scheduling decisions - only narrate the outcome provided.
            Do not invent details that are not present. If a detail is missing, leave it out.
            """;

    private final LlmClient llmClient;

    public AiExplainService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public AiExplainResponse explain(AiExplainRequest request) {
        if (request == null) {
            return new AiExplainResponse("No scheduling result to explain.", false);
        }
        if (!llmClient.isConfigured()) {
            log.debug("app.ai.api-key is not set; returning the deterministic explanation");
            return fallback(request);
        }
        try {
            String explanation = llmClient.complete(SYSTEM_PROMPT, buildUserPrompt(request));
            if (explanation == null || explanation.isBlank()) {
                return fallback(request);
            }
            return new AiExplainResponse(explanation.trim(), true);
        } catch (Exception ex) {
            log.warn("AI explanation request failed ({}); returning the deterministic explanation", ex.getMessage());
            return fallback(request);
        }
    }

    /** Renders the scheduling outcome as the labelled block the model receives. */
    private String buildUserPrompt(AiExplainRequest r) {
        List<String> lines = new ArrayList<>();
        add(lines, "Meeting", r.meetingTitle());
        if (r.participants() != null && !r.participants().isEmpty()) {
            add(lines, "Participants", String.join(", ", r.participants()));
        }
        if (r.durationMinutes() != null) {
            add(lines, "Duration", r.durationMinutes() + " minutes");
        }
        add(lines, "Priority", r.priority());
        add(lines, "Selected time", r.selectedTime());
        add(lines, "Conflict", r.conflict());
        add(lines, "Conflict priority", r.conflictPriority());
        add(lines, "Proposed change", r.proposedChange());
        if (r.newConflicts() != null) {
            add(lines, "New conflicts", String.valueOf(r.newConflicts()));
        }
        if (r.buffersPreserved() != null) {
            add(lines, "Buffers preserved", r.buffersPreserved() ? "Yes" : "No");
        }
        return String.join("\n", lines);
    }

    private void add(List<String> lines, String label, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(label + ": " + value);
        }
    }

    private AiExplainResponse fallback(AiExplainRequest r) {
        if (r.fallbackExplanation() != null && !r.fallbackExplanation().isBlank()) {
            return new AiExplainResponse(r.fallbackExplanation(), false);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(r.selectedTime() == null || r.selectedTime().isBlank() ? "The slot" : r.selectedTime());
        sb.append(" was selected");
        if (r.meetingTitle() != null && !r.meetingTitle().isBlank()) {
            sb.append(" for ").append(r.meetingTitle());
        }
        sb.append(" by SyncSlot's scheduling engine based on availability, priority and buffers.");
        return new AiExplainResponse(sb.toString(), false);
    }
}
