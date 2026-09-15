package com.syncslot.dto;

import java.util.List;

/**
 * Confirmed cascade application. The frontend posts this only after showing
 * {@link CascadePreviewDto} to the user. The moves must match the preview.
 */
public record CascadeApplyRequest(
        AppointmentRequest appointment,
        List<MoveDto> moves) {
}
