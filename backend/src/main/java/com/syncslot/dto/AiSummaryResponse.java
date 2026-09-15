package com.syncslot.dto;

/**
 * Result of POST /api/ai/summary. {@code aiGenerated} is false when the LLM was
 * unavailable and the deterministic summary was returned instead.
 */
public record AiSummaryResponse(String summary, boolean aiGenerated, int meetingCount, String period) {
}
