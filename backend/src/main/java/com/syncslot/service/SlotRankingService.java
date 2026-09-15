package com.syncslot.service;

import com.syncslot.algorithm.Availability;
import com.syncslot.algorithm.SchedulingEngine;
import com.syncslot.algorithm.SchedulingEngine.ScheduleItem;
import com.syncslot.algorithm.SchedulingEngine.ScoredSlot;
import com.syncslot.algorithm.SchedulingEngine.Slot;
import com.syncslot.dto.ScoredSlotDto;
import com.syncslot.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ranks candidate slots using the engine's priority-aware scoring function
 * (spec section 9) and converts results into DTOs with the score breakdown
 * and human-readable reason exposed to the frontend.
 */
@Service
public class SlotRankingService {

    private final SchedulingEngine engine;

    public SlotRankingService(SchedulingEngine engine) {
        this.engine = engine;
    }

    public List<ScoredSlotDto> rank(List<Slot> candidates,
                                    ScheduleItem requested,
                                    Map<Long, List<Availability>> availabilityByParticipant,
                                    List<ScheduleItem> existingAppointments,
                                    Set<Long> recentlyDisplacedParticipantIds) {
        List<ScoredSlot> ranked = engine.rankSlots(candidates, requested, availabilityByParticipant,
                existingAppointments, recentlyDisplacedParticipantIds);
        return ranked.stream().map(this::toDto).toList();
    }

    private ScoredSlotDto toDto(ScoredSlot s) {
        return new ScoredSlotDto(
                TimeUtil.toLocalTime(s.getSlot().getStartMinute()),
                TimeUtil.toLocalTime(s.getSlot().getEndMinute()),
                s.getScore(),
                s.getAvailabilityFit(),
                s.getPriorityFit(),
                s.getPreferenceFit(),
                s.getBufferQuality(),
                s.getDisruptionCost(),
                s.getReason());
    }
}
