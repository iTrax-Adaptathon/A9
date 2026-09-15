package com.syncslot.dto;

import java.time.LocalTime;
import java.util.List;

/**
 * Dry-run result of a cascade. Mirrors the shape described in the spec (section 12):
 * REQUEST / IMPACT / PARTICIPANTS AFFECTED / CONFLICTS CREATED / STATUS.
 *
 * <p>If the requested meeting itself must move (it conflicts with an equal or
 * higher-priority appointment), {@code requestedNewStartTime}/{@code requestedNewEndTime}
 * carry the alternative and {@code impact} is empty. Otherwise the requested
 * meeting is kept as submitted and {@code impact} lists the existing
 * appointments that will move.
 */
public record CascadePreviewDto(
        String requestTitle,
        LocalTime requestStartTime,
        LocalTime requestEndTime,
        String requestPriority,
        LocalTime requestedNewStartTime,
        LocalTime requestedNewEndTime,
        List<MoveDto> impact,
        List<Long> participantsAffected,
        List<ConflictDto> conflictsCreated,
        String status,
        String reason) {
}
