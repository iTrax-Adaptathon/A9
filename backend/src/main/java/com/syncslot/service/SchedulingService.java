package com.syncslot.service;

import com.syncslot.algorithm.SchedulingEngine;
import com.syncslot.algorithm.SchedulingEngine.ScheduleItem;
import com.syncslot.algorithm.SchedulingEngine.Slot;
import com.syncslot.dto.AppointmentRequest;
import com.syncslot.dto.FindSlotsRequest;
import com.syncslot.dto.FindSlotsResponse;
import com.syncslot.dto.ScoredSlotDto;
import com.syncslot.model.Appointment;
import com.syncslot.model.Priority;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.repository.AvailabilityRepository;
import com.syncslot.util.TimeUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Orchestrates slot-finding requests and owns the conversion between JPA
 * entities and the pure-Java engine inputs. Controller -> Service -> Engine
 * -> Repository layering is preserved: this service never writes scheduling
 * decisions into the frontend and never talks to the web layer directly.
 */
@Service
public class SchedulingService {

    private final SchedulingEngine engine;
    private final SlotRankingService slotRankingService;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    public SchedulingService(SchedulingEngine engine,
                             SlotRankingService slotRankingService,
                             AvailabilityRepository availabilityRepository,
                             AppointmentRepository appointmentRepository) {
        this.engine = engine;
        this.slotRankingService = slotRankingService;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /** POST /api/schedule/find-slots — find + rank practical free slots. */
    public FindSlotsResponse findSlots(FindSlotsRequest req) {
        Objects.requireNonNull(req.date(), "date is required");
        Set<Long> participantIds = new LinkedHashSet<>(
                req.participantIds() == null ? List.of() : req.participantIds());
        int duration = req.duration() == null ? 60 : req.duration();
        int bufferBefore = req.bufferBefore() == null ? 0 : req.bufferBefore();
        int bufferAfter = req.bufferAfter() == null ? 0 : req.bufferAfter();
        Priority priority = req.priority() == null ? Priority.MEDIUM : req.priority();

        if (participantIds.isEmpty() || duration <= 0) {
            return new FindSlotsResponse(List.of());
        }

        Map<Long, List<com.syncslot.algorithm.Availability>> availabilityByParticipant =
                loadAvailabilityMap(req.date());
        List<ScheduleItem> existing = loadExistingItems(req.date());

        ScheduleItem requested = new ScheduleItem(null, req.title() == null ? "New appointment" : req.title(),
                new ArrayList<>(participantIds), req.date(), 0, duration,
                duration, bufferBefore, bufferAfter, priority);

        List<Slot> candidates = engine.findCommonSlots(participantIds,
                flatten(availabilityByParticipant), existing, duration, bufferBefore, bufferAfter);

        List<ScoredSlotDto> ranked = slotRankingService.rank(candidates, requested,
                availabilityByParticipant, existing, Set.of());

        // Minute-level search can produce many near-identical slots; keep the top ones.
        List<ScoredSlotDto> top = ranked.stream().limit(10).toList();

        return new FindSlotsResponse(top);
    }

    // ------------------------------------------------------------------
    // Shared conversion helpers (used by ConflictService and CascadeService)
    // ------------------------------------------------------------------

    public Map<Long, List<com.syncslot.algorithm.Availability>> loadAvailabilityMap(LocalDate date) {
        Map<Long, List<com.syncslot.algorithm.Availability>> map = new HashMap<>();
        for (var a : availabilityRepository.findByDate(date)) {
            map.computeIfAbsent(a.getUserId(), k -> new ArrayList<>())
                    .add(new com.syncslot.algorithm.Availability(a.getUserId(), a.getDate(),
                            TimeUtil.toMinutes(a.getStartTime()), TimeUtil.toMinutes(a.getEndTime())));
        }
        return map;
    }

    public List<ScheduleItem> loadExistingItems(LocalDate date) {
        List<ScheduleItem> items = new ArrayList<>();
        for (Appointment a : appointmentRepository.findByDate(date)) {
            items.add(toScheduleItem(a));
        }
        return items;
    }

    public ScheduleItem toScheduleItem(Appointment a) {
        return new ScheduleItem(a.getId(), a.getTitle(), a.getParticipantIds(), a.getDate(),
                TimeUtil.toMinutes(a.getStartTime()), TimeUtil.toMinutes(a.getEndTime()),
                a.getDuration(), a.getBufferBefore(), a.getBufferAfter(), a.getPriority());
    }

    public ScheduleItem toScheduleItem(AppointmentRequest req, Long id) {
        int start = TimeUtil.toMinutes(req.startTime());
        int end = TimeUtil.toMinutes(req.endTime());
        int duration = req.duration() == null ? Math.max(1, end - start) : req.duration();
        int bufferBefore = req.bufferBefore() == null ? 0 : req.bufferBefore();
        int bufferAfter = req.bufferAfter() == null ? 0 : req.bufferAfter();
        Priority priority = req.priority() == null ? Priority.MEDIUM : req.priority();
        return new ScheduleItem(id, req.title(), req.participantIds(), req.date(),
                start, end, duration, bufferBefore, bufferAfter, priority);
    }

    private List<com.syncslot.algorithm.Availability> flatten(
            Map<Long, List<com.syncslot.algorithm.Availability>> byParticipant) {
        List<com.syncslot.algorithm.Availability> all = new ArrayList<>();
        for (List<com.syncslot.algorithm.Availability> list : byParticipant.values()) all.addAll(list);
        return all;
    }
}
