package com.syncslot.dto;

import java.time.LocalDate;

/**
 * Request for an AI summary of the schedule. {@code range} is one of
 * {@code day} (default), {@code week} or {@code month}.
 */
public record AiSummaryRequest(LocalDate date, String range) {
}
