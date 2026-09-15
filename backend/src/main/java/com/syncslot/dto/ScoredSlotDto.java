package com.syncslot.dto;

import java.time.LocalTime;

public record ScoredSlotDto(
        LocalTime startTime,
        LocalTime endTime,
        double score,
        double availabilityFit,
        double priorityFit,
        double preferenceFit,
        double bufferQuality,
        double disruptionCost,
        String reason) {
}
