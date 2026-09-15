package com.syncslot.service;

import com.syncslot.algorithm.SchedulingEngine;
import com.syncslot.algorithm.SchedulingEngine.ConflictInfo;
import com.syncslot.algorithm.SchedulingEngine.ScheduleItem;
import com.syncslot.algorithm.SchedulingEngine.Slot;
import com.syncslot.dto.AppointmentRequest;
import com.syncslot.dto.ConflictDto;
import com.syncslot.dto.ConflictResponse;
import com.syncslot.dto.ScoredSlotDto;
import com.syncslot.model.User;
import com.syncslot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects conflicts for a proposed appointment and returns practical ranked
 * alternatives (never a bare rejection). All decisions come from the engine;
 * this service only enriches messages with participant names and adapts to DTOs.
 */
@Service
public class ConflictService {

    private final SchedulingEngine engine;
    private final SchedulingService schedulingService;
    private final SlotRankingService slotRankingService;
    private final UserRepository userRepository;

    public ConflictService(SchedulingEngine engine,
                           SchedulingService schedulingService,
                           SlotRankingService slotRankingService,
                           UserRepository userRepository) {
        this.engine = engine;
        this.schedulingService = schedulingService;
        this.slotRankingService = slotRankingService;
        this.userRepository = userRepository;
    }

    public ConflictResponse detectConflicts(AppointmentRequest req) {
        Map<Long, String> names = userNames();

        ScheduleItem requested = schedulingService.toScheduleItem(req, null);
        Map<Long, List<com.syncslot.algorithm.Availability>> availabilityByParticipant =
                schedulingService.loadAvailabilityMap(req.date());
        List<ScheduleItem> existing = schedulingService.loadExistingItems(req.date());

        List<ConflictInfo> conflicts = engine.detectConflicts(requested, availabilityByParticipant, existing);

        List<ConflictDto> conflictDtos = conflicts.stream()
                .map(c -> new ConflictDto(c.getType().name(), enrich(c.getDetail(), names),
                        c.getParticipantIds(), c.getAppointmentIds()))
                .collect(Collectors.toList());

        List<ScoredSlotDto> alternatives = List.of();
        if (!conflicts.isEmpty()) {
            Set<Long> participants = new LinkedHashSet<>(requested.getParticipantIds());
            List<Slot> candidates = engine.findCommonSlots(participants,
                    flatten(availabilityByParticipant), existing,
                    requested.getDurationMin(), requested.getBufferBefore(), requested.getBufferAfter());
            List<ScoredSlotDto> ranked = slotRankingService.rank(candidates, requested,
                    availabilityByParticipant, existing, Set.of());
            alternatives = ranked.stream().limit(5).collect(Collectors.toList());
        }

        return new ConflictResponse(!conflicts.isEmpty(), conflictDtos, alternatives);
    }

    private Map<Long, String> userNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    /** Replace engine "#id" placeholders with real participant names. */
    private String enrich(String detail, Map<Long, String> names) {
        String out = detail;
        for (Map.Entry<Long, String> e : names.entrySet()) {
            out = out.replace("#" + e.getKey(), e.getValue());
        }
        return out;
    }

    private List<com.syncslot.algorithm.Availability> flatten(
            Map<Long, List<com.syncslot.algorithm.Availability>> byParticipant) {
        List<com.syncslot.algorithm.Availability> all = new ArrayList<>();
        for (List<com.syncslot.algorithm.Availability> list : byParticipant.values()) all.addAll(list);
        return all;
    }
}
