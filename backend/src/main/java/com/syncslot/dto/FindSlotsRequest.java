package com.syncslot.dto;

import com.syncslot.model.Priority;

import java.time.LocalDate;
import java.util.List;

public record FindSlotsRequest(
        List<Long> participantIds,
        LocalDate date,
        Integer duration,
        Integer bufferBefore,
        Integer bufferAfter,
        Priority priority,
        String title) {
}
