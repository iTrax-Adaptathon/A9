package com.syncslot.service;

import com.syncslot.algorithm.SchedulingEngine;
import com.syncslot.algorithm.SchedulingEngine.CascadePlan;
import com.syncslot.algorithm.SchedulingEngine.Move;
import com.syncslot.algorithm.SchedulingEngine.ScheduleItem;
import com.syncslot.dto.*;
import com.syncslot.model.Appointment;
import com.syncslot.model.AppointmentStatus;
import com.syncslot.model.User;
import com.syncslot.repository.AppointmentRepository;
import com.syncslot.repository.UserRepository;
import com.syncslot.util.TimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the cascade workflow (spec sections 11-13):
 * preview-cascade is a pure dry-run (no writes), apply-cascade persists only
 * after explicit confirmation and is fully transactional.
 */
@Service
public class CascadeService {

    private final SchedulingEngine engine;
    private final SchedulingService schedulingService;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public CascadeService(SchedulingEngine engine,
                          SchedulingService schedulingService,
                          AppointmentRepository appointmentRepository,
                          UserRepository userRepository) {
        this.engine = engine;
        this.schedulingService = schedulingService;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    /** POST /api/schedule/preview-cascade — read-only dry run. */
    public CascadePreviewDto previewCascade(AppointmentRequest req) {
        Map<Long, String> names = userNames();
        ScheduleItem requested = schedulingService.toScheduleItem(req, null);
        Map<Long, List<com.syncslot.algorithm.Availability>> availabilityByParticipant =
                schedulingService.loadAvailabilityMap(req.date());
        List<ScheduleItem> existing = schedulingService.loadExistingItems(req.date());

        CascadePlan plan = engine.planCascade(requested, availabilityByParticipant, existing);
        return toPreview(plan, req, names);
    }

    /** POST /api/schedule/apply-cascade — transactional; rolls back on any failure. */
    @Transactional
    public CascadeApplyResponse applyCascade(CascadeApplyRequest apply) {
        AppointmentRequest req = apply.appointment();
        ScheduleItem requested = schedulingService.toScheduleItem(req, null);
        Map<Long, List<com.syncslot.algorithm.Availability>> availabilityByParticipant =
                schedulingService.loadAvailabilityMap(req.date());
        List<ScheduleItem> existing = schedulingService.loadExistingItems(req.date());

        // Re-derive the plan against the current DB state. This keeps the apply
        // authoritative and consistent even if data changed since the preview.
        CascadePlan plan = engine.planCascade(requested, availabilityByParticipant, existing);

        if (!plan.isSafe()) {
            return new CascadeApplyResponse(false,
                    "Cannot apply: " + plan.getReason(), appointmentsForDate(req.date()));
        }

        // 1. Persist the requested appointment at its final (possibly moved) time.
        ScheduleItem finalRequested = plan.getRequested();
        Appointment newAppt = new Appointment(
                finalRequested.getTitle(),
                finalRequested.getDate(),
                TimeUtil.toLocalTime(finalRequested.getStartMinute()),
                TimeUtil.toLocalTime(finalRequested.getEndMinute()),
                finalRequested.getDurationMin(),
                finalRequested.getBufferBefore(),
                finalRequested.getBufferAfter(),
                finalRequested.getPriority(),
                AppointmentStatus.CONFIRMED,
                finalRequested.getParticipantIds());
        appointmentRepository.save(newAppt);

        // 2. Move every affected existing appointment (skip the requested self-move, id == null).
        for (Move move : plan.getMoves()) {
            if (move.getItem().getId() == null) continue;
            Optional<Appointment> opt = appointmentRepository.findById(move.getItem().getId());
            if (opt.isEmpty()) {
                throw new IllegalStateException("Moved appointment not found: " + move.getItem().getId());
            }
            Appointment appt = opt.get();
            appt.setStartTime(TimeUtil.toLocalTime(move.getNewSlot().getStartMinute()));
            appt.setEndTime(TimeUtil.toLocalTime(move.getNewSlot().getEndMinute()));
            appt.setStatus(AppointmentStatus.RESCHEDULED);
            appointmentRepository.save(appt);
        }

        return new CascadeApplyResponse(true, plan.getReason(), appointmentsForDate(req.date()));
    }

    // ------------------------------------------------------------------
    // DTO mapping
    // ------------------------------------------------------------------

    private CascadePreviewDto toPreview(CascadePlan plan, AppointmentRequest req, Map<Long, String> names) {
        List<Move> selfMoves = plan.getMoves().stream()
                .filter(m -> m.getItem().getId() == null).toList();
        List<Move> existingMoves = plan.getMoves().stream()
                .filter(m -> m.getItem().getId() != null).toList();

        LocalTime reqNewStart = null;
        LocalTime reqNewEnd = null;
        if (!selfMoves.isEmpty()) {
            Move self = selfMoves.get(0);
            reqNewStart = TimeUtil.toLocalTime(self.getNewSlot().getStartMinute());
            reqNewEnd = TimeUtil.toLocalTime(self.getNewSlot().getEndMinute());
        }

        List<MoveDto> impact = existingMoves.stream().map(this::toMoveDto).toList();
        List<ConflictDto> conflicts = plan.getConflictsCreated().stream()
                .map(c -> new ConflictDto(c.getType().name(), enrich(c.getDetail(), names),
                        c.getParticipantIds(), c.getAppointmentIds()))
                .toList();

        String priority = req.priority() == null ? "MEDIUM" : req.priority().name();

        return new CascadePreviewDto(
                req.title(),
                req.startTime(),
                req.endTime(),
                priority,
                reqNewStart,
                reqNewEnd,
                impact,
                plan.getParticipantsAffected(),
                conflicts,
                plan.getStatus(),
                enrich(plan.getReason(), names));
    }

    private MoveDto toMoveDto(Move move) {
        ScheduleItem item = move.getItem();
        return new MoveDto(
                item.getId(),
                item.getTitle(),
                TimeUtil.toLocalTime(item.getStartMinute()),
                TimeUtil.toLocalTime(item.getEndMinute()),
                TimeUtil.toLocalTime(move.getNewSlot().getStartMinute()),
                TimeUtil.toLocalTime(move.getNewSlot().getEndMinute()),
                item.getPriority(),
                move.getReason());
    }

    private List<AppointmentDto> appointmentsForDate(java.time.LocalDate date) {
        return appointmentRepository.findByDateOrderByStartTimeAsc(date).stream()
                .map(this::toAppointmentDto).collect(Collectors.toList());
    }

    private AppointmentDto toAppointmentDto(Appointment a) {
        return new AppointmentDto(a.getId(), a.getTitle(), a.getDate(),
                a.getStartTime(), a.getEndTime(), a.getDuration(),
                a.getBufferBefore(), a.getBufferAfter(), a.getPriority(),
                a.getStatus().name(), a.getParticipantIds());
    }

    private Map<Long, String> userNames() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private String enrich(String detail, Map<Long, String> names) {
        String out = detail;
        for (Map.Entry<Long, String> e : names.entrySet()) {
            out = out.replace("#" + e.getKey(), e.getValue());
        }
        return out;
    }
}
