package com.syncslot.dto;

import com.syncslot.model.Priority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AppointmentDto(
        Long id,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int duration,
        int bufferBefore,
        int bufferAfter,
        Priority priority,
        String status,
        List<Long> participantIds) {
}
