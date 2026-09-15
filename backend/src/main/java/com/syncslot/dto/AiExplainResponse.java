package com.syncslot.dto;

/**
 * Result of POST /api/ai/explain. {@code aiGenerated} is false when the LLM was
 * unavailable and the deterministic engine explanation was returned instead.
 */
public record AiExplainResponse(String explanation, boolean aiGenerated) {
}
