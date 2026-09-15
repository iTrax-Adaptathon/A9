package com.syncslot.dto;

import java.util.List;

public record ConflictDto(
        String type,
        String message,
        List<Long> participantIds,
        List<Long> appointmentIds) {
}
