package com.syncslot.dto;

import java.util.List;

public record FindSlotsResponse(List<ScoredSlotDto> slots) {
}
