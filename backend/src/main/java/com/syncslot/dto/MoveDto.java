package com.syncslot.dto;

import com.syncslot.model.Priority;

import java.time.LocalTime;

public record MoveDto(
        Long appointmentId,
        String title,
        LocalTime oldStartTime,
        LocalTime oldEndTime,
        LocalTime newStartTime,
        LocalTime newEndTime,
        Priority priority,
        String reason) {
}
