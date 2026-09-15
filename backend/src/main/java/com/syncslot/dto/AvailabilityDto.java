package com.syncslot.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilityDto(Long id, Long userId, LocalDate date, LocalTime startTime, LocalTime endTime) {
}
