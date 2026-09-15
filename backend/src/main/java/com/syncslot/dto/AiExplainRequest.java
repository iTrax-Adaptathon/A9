package com.syncslot.dto;

import java.util.List;

/**
 * A scheduling outcome the frontend wants explained in plain language.
 *
 * <p>This is a pure description of a decision the SchedulingEngine has already
 * made. The AI is only asked to narrate it and must never alter it.
 */
public record AiExplainRequest(
        String meetingTitle,
        List<String> participants,
        Integer durationMinutes,
        String priority,
        String selectedTime,
        String conflict,
        String conflictPriority,
        String proposedChange,
        Integer newConflicts,
        Boolean buffersPreserved,
        String fallbackExplanation) {
}
