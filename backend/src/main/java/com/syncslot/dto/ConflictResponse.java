package com.syncslot.dto;

import java.util.List;

public record ConflictResponse(
        boolean hasConflict,
        List<ConflictDto> conflicts,
        List<ScoredSlotDto> alternatives) {
}
