package com.syncslot.dto;

import java.util.List;

public record CascadeApplyResponse(
        boolean applied,
        String message,
        List<AppointmentDto> appointments) {
}
