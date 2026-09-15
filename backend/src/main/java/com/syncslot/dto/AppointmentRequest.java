package com.syncslot.dto;

import com.syncslot.model.Priority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A request to book (or schedule) an appointment. Used by the plain CRUD
 * endpoint POST /api/appointments and by the scheduling endpoints
 * (find-slots / preview-cascade / apply-cascade).
 */
public record AppointmentRequest(
        String title,
        List<Long> participantIds,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer duration,
        Integer bufferBefore,
        Integer bufferAfter,
        Priority priority) {
}
