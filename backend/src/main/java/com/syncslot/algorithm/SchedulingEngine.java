package com.syncslot.algorithm;

import com.syncslot.model.Priority;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * INHERITED from the A9 repo (algorithm/SchedulingEngine.java) and adapted for
 * Sprint 2. The original was a disconnected command-line scheduling component;
 * this version keeps the same minute-level scheduling logic but removes the
 * CLI main()/stdin-IO and exposes a clean object API that the Spring service
 * layer (SchedulingService / ConflictService / CascadeService /
 * SlotRankingService) calls directly.
 *
 * <p>Responsibilities (per Sprint 2 spec section 8):
 * <ul>
 *   <li>A. Multi-person availability intersection</li>
 *   <li>B. Minute-level scheduling</li>
 *   <li>C. Exact duration constraints</li>
 *   <li>D. Buffer padding around appointments</li>
 *   <li>E. Existing appointments as hard constraints</li>
 *   <li>F. Conflict detection (unavailable, overlap, duration, buffer, intersection)</li>
 *   <li>G. Alternative slot generation</li>
 *   <li>H. Priority-aware protection</li>
 *   <li>I. Human-readable, factor-driven explanations</li>
 * </ul>
 *
 * <p>This class is deliberately pure Java (no Spring/JPA imports except the
 * shared {@link Priority} enum) so the scheduling logic remains testable in
 * isolation and independent of the web layer.
 */
public class SchedulingEngine {

    // ---- Tunable scoring weights (see spec section 9) ----
    private double weightAvailability = 3.0;
    private double weightPriority = 1.5;
    private double weightPreference = 1.0;
    private double weightBuffer = 1.5;
    private double weightDisruption = 2.0;

    /** Extra disruption penalty when a participant is displaced more than once (fairness). */
    private double fairnessPenalty = 1.0;

    /** Maximum cascade depth (a cascade caused by a cascade caused by the change). */
    public static final int MAX_CASCADE_DEPTH = 2;

    public SchedulingEngine() {
    }

    // ---- weight accessors (tuned via EngineConfig) ----
    public void setWeightAvailability(double v) { this.weightAvailability = v; }
    public void setWeightPriority(double v) { this.weightPriority = v; }
    public void setWeightPreference(double v) { this.weightPreference = v; }
    public void setWeightBuffer(double v) { this.weightBuffer = v; }
    public void setWeightDisruption(double v) { this.weightDisruption = v; }
    public void setFairnessPenalty(double v) { this.fairnessPenalty = v; }

    /** Numeric weight of a priority (stronger protection for higher priorities). */
    public static double priorityWeight(Priority p) {
        if (p == null) return 0.25;
        return switch (p) {
            case URGENT -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
        };
    }

    // ---------------------------------------------------------------------
    // Nested value types
    // ---------------------------------------------------------------------

    /** A candidate meeting slot (date + start/end minutes, end exclusive). */
    public static class Slot {
        private final LocalDate date;
        private final int startMinute;
        private final int endMinute;

        public Slot(LocalDate date, int startMinute, int endMinute) {
            this.date = date;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }

        public LocalDate getDate() { return date; }
        public int getStartMinute() { return startMinute; }
        public int getEndMinute() { return endMinute; }
        public int duration() { return endMinute - startMinute; }

        @Override
        public String toString() {
            return String.format("%s %02d:%02d-%02d:%02d",
                    date, startMinute / 60, startMinute % 60, endMinute / 60, endMinute % 60);
        }
    }

    /** A ranked slot with its score breakdown and a human-readable reason. */
    public static class ScoredSlot {
        private final Slot slot;
        private final double score;
        private final double availabilityFit;
        private final double priorityFit;
        private final double preferenceFit;
        private final double bufferQuality;
        private final double disruptionCost;
        private final String reason;
        private final List<Long> displacedAppointmentIds;

        public ScoredSlot(Slot slot, double score, double availabilityFit, double priorityFit,
                          double preferenceFit, double bufferQuality, double disruptionCost,
                          String reason, List<Long> displacedAppointmentIds) {
            this.slot = slot;
            this.score = score;
            this.availabilityFit = availabilityFit;
            this.priorityFit = priorityFit;
            this.preferenceFit = preferenceFit;
            this.bufferQuality = bufferQuality;
            this.disruptionCost = disruptionCost;
            this.reason = reason;
            this.displacedAppointmentIds = displacedAppointmentIds;
        }

        public Slot getSlot() { return slot; }
        public double getScore() { return score; }
        public double getAvailabilityFit() { return availabilityFit; }
        public double getPriorityFit() { return priorityFit; }
        public double getPreferenceFit() { return preferenceFit; }
        public double getBufferQuality() { return bufferQuality; }
        public double getDisruptionCost() { return disruptionCost; }
        public String getReason() { return reason; }
        public List<Long> getDisplacedAppointmentIds() { return displacedAppointmentIds; }
    }

    public enum ConflictType {
        PARTICIPANT_UNAVAILABLE,
        APPOINTMENT_OVERLAP,
        INSUFFICIENT_DURATION,
        BUFFER_VIOLATION,
        IMPOSSIBLE_INTERSECTION
    }

    /** A single detected conflict with a structured, human-readable detail. */
    public static class ConflictInfo {
        private final ConflictType type;
        private final String detail;
        private final List<Long> participantIds;
        private final List<Long> appointmentIds;

        public ConflictInfo(ConflictType type, String detail,
                            List<Long> participantIds, List<Long> appointmentIds) {
            this.type = type;
            this.detail = detail;
            this.participantIds = participantIds == null ? List.of() : participantIds;
            this.appointmentIds = appointmentIds == null ? List.of() : appointmentIds;
        }

        public ConflictType getType() { return type; }
        public String getDetail() { return detail; }
        public List<Long> getParticipantIds() { return participantIds; }
        public List<Long> getAppointmentIds() { return appointmentIds; }
    }

    /** Lightweight appointment representation the engine works with (no JPA). */
    public static class ScheduleItem {
        private final Long id; // null or < 0 for a brand-new (not-yet-persisted) item
        private final String title;
        private final List<Long> participantIds;
        private final LocalDate date;
        private final int startMinute;
        private final int endMinute;
        private final int durationMin;
        private final int bufferBefore;
        private final int bufferAfter;
        private final Priority priority;

        public ScheduleItem(Long id, String title, List<Long> participantIds, LocalDate date,
                            int startMinute, int endMinute, int durationMin,
                            int bufferBefore, int bufferAfter, Priority priority) {
            this.id = id;
            this.title = title;
            this.participantIds = participantIds == null ? List.of() : List.copyOf(participantIds);
            this.date = date;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
            this.durationMin = durationMin;
            this.bufferBefore = bufferBefore;
            this.bufferAfter = bufferAfter;
            this.priority = priority == null ? Priority.LOW : priority;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public List<Long> getParticipantIds() { return participantIds; }
        public LocalDate getDate() { return date; }
        public int getStartMinute() { return startMinute; }
        public int getEndMinute() { return endMinute; }
        public int getDurationMin() { return durationMin; }
        public int getBufferBefore() { return bufferBefore; }
        public int getBufferAfter() { return bufferAfter; }
        public Priority getPriority() { return priority; }

        public int occupiedStart() { return startMinute - bufferBefore; }
        public int occupiedEnd() { return endMinute + bufferAfter; }

        /** A copy relocated to a new start minute (same duration and buffers). */
        public ScheduleItem withStart(int newStartMinute) {
            return new ScheduleItem(id, title, participantIds, date,
                    newStartMinute, newStartMinute + durationMin, durationMin,
                    bufferBefore, bufferAfter, priority);
        }
    }

    /** A proposed relocation of an existing appointment (old slot -> new slot). */
    public static class Move {
        private final ScheduleItem item;
        private final Slot newSlot;
        private final String reason;

        public Move(ScheduleItem item, Slot newSlot, String reason) {
            this.item = item;
            this.newSlot = newSlot;
            this.reason = reason;
        }

        public ScheduleItem getItem() { return item; }
        public Slot getNewSlot() { return newSlot; }
        public String getReason() { return reason; }
        public ScheduleItem itemAtNewTime() { return item.withStart(newSlot.getStartMinute()); }
    }

    /** The result of planning a cascade: a dry-run diff (never mutates data). */
    public static class CascadePlan {
        private final boolean safe;
        private final String status;
        private final String reason;
        private final ScheduleItem requested;       // the requested appointment (possibly re-slotted)
        private final List<Move> moves;             // existing appointments to move
        private final List<Long> participantsAffected;
        private final List<ConflictInfo> conflictsCreated;

        public CascadePlan(boolean safe, String status, String reason, ScheduleItem requested,
                           List<Move> moves, List<Long> participantsAffected,
                           List<ConflictInfo> conflictsCreated) {
            this.safe = safe;
            this.status = status;
            this.reason = reason;
            this.requested = requested;
            this.moves = moves == null ? List.of() : moves;
            this.participantsAffected = participantsAffected == null ? List.of() : participantsAffected;
            this.conflictsCreated = conflictsCreated == null ? List.of() : conflictsCreated;
        }

        public boolean isSafe() { return safe; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
        public ScheduleItem getRequested() { return requested; }
        public List<Move> getMoves() { return moves; }
        public List<Long> getParticipantsAffected() { return participantsAffected; }
        public List<ConflictInfo> getConflictsCreated() { return conflictsCreated; }
    }

    private static class Relocation {
        final List<Move> moves;
        final double cost;

        Relocation(List<Move> moves, double cost) {
            this.moves = moves;
            this.cost = cost;
        }
    }

    // ---------------------------------------------------------------------
    // A/B/C/D. Common free slot search (multi-person, minute-level, buffers)
    // ---------------------------------------------------------------------

    /**
     * Find every minute-level slot that works for ALL required participants,
     * accounting for duration and buffers, avoiding every existing appointment
     * that shares a participant with the request.
     */
    public List<Slot> findCommonSlots(Set<Long> requiredParticipantIds,
                                      List<Availability> availabilities,
                                      List<ScheduleItem> existingAppointments,
                                      int durationMin, int bufferBefore, int bufferAfter) {
        List<Slot> slots = new ArrayList<>();
        if (durationMin <= 0 || requiredParticipantIds == null || requiredParticipantIds.isEmpty()) {
            return slots;
        }

        LocalDate date = deriveDate(availabilities);
        if (date == null) return slots;

        Map<Long, List<Availability>> byParticipant = new HashMap<>();
        for (Availability a : availabilities) {
            byParticipant.computeIfAbsent(a.getParticipantId(), k -> new ArrayList<>()).add(a);
        }

        List<int[]> common = null;
        for (Long pid : requiredParticipantIds) {
            List<int[]> intervals = mergedIntervals(byParticipant.getOrDefault(pid, List.of()), date);
            if (intervals.isEmpty()) {
                return slots; // a required participant has no availability
            }
            common = (common == null) ? intervals : intersectIntervals(common, intervals);
            if (common.isEmpty()) {
                return slots; // no shared availability
            }
        }

        // Subtract existing appointments that involve any required participant.
        for (ScheduleItem appt : existingAppointments) {
            if (!date.equals(appt.getDate())) continue;
            boolean shares = appt.getParticipantIds().stream().anyMatch(requiredParticipantIds::contains);
            if (!shares) continue;
            common = subtractInterval(common, appt.occupiedStart(), appt.occupiedEnd());
            if (common.isEmpty()) return slots;
        }

        for (int[] iv : common) {
            int earliest = iv[0] + bufferBefore;
            int latest = iv[1] - bufferAfter - durationMin;
            for (int s = earliest; s <= latest; s++) {
                slots.add(new Slot(date, s, s + durationMin));
            }
        }
        return slots;
    }

    // ---------------------------------------------------------------------
    // F. Conflict detection
    // ---------------------------------------------------------------------

    public List<ConflictInfo> detectConflicts(ScheduleItem requested,
                                              Map<Long, List<Availability>> availabilityByParticipant,
                                              List<ScheduleItem> existingAppointments) {
        List<ConflictInfo> conflicts = new ArrayList<>();

        if (requested.getDurationMin() <= 0) {
            conflicts.add(new ConflictInfo(ConflictType.INSUFFICIENT_DURATION,
                    "Meeting duration must be at least 1 minute (got " + requested.getDurationMin() + ").",
                    requested.getParticipantIds(), List.of()));
            return conflicts;
        }
        if (requested.getParticipantIds().isEmpty()) {
            conflicts.add(new ConflictInfo(ConflictType.IMPOSSIBLE_INTERSECTION,
                    "At least one participant is required.", List.of(), List.of()));
            return conflicts;
        }

        int start = requested.getStartMinute();
        int end = requested.getEndMinute();
        int oStart = requested.occupiedStart();
        int oEnd = requested.occupiedEnd();

        List<int[]> allIntervals = new ArrayList<>();
        boolean allHaveAvailability = true;

        for (Long pid : requested.getParticipantIds()) {
            List<int[]> intervals = mergedIntervals(
                    availabilityByParticipant.getOrDefault(pid, List.of()), requested.getDate());
            if (intervals.isEmpty()) {
                allHaveAvailability = false;
                conflicts.add(new ConflictInfo(ConflictType.PARTICIPANT_UNAVAILABLE,
                        "Participant #" + pid + " has no availability on " + requested.getDate() + ".",
                        List.of(pid), List.of()));
                continue;
            }
            allIntervals.addAll(intervals);

            boolean meetingCovered = covers(intervals, start, end);
            boolean occupiedCovered = covers(intervals, oStart, oEnd);
            if (!meetingCovered) {
                conflicts.add(new ConflictInfo(ConflictType.PARTICIPANT_UNAVAILABLE,
                        "Participant #" + pid + " is unavailable at the requested time.",
                        List.of(pid), List.of()));
            } else if (!occupiedCovered) {
                conflicts.add(new ConflictInfo(ConflictType.BUFFER_VIOLATION,
                        "Participant #" + pid + "'s buffer extends outside their availability.",
                        List.of(pid), List.of()));
            }

            for (ScheduleItem appt : existingAppointments) {
                if (!requested.getDate().equals(appt.getDate())) continue;
                if (!appt.getParticipantIds().contains(pid)) continue;
                if (appt.getId() != null && appt.getId().equals(requested.getId())) continue;

                if (overlaps(start, end, appt.getStartMinute(), appt.getEndMinute())) {
                    conflicts.add(new ConflictInfo(ConflictType.APPOINTMENT_OVERLAP,
                            "Participant #" + pid + " already has '" + appt.getTitle()
                                    + "' at " + format(appt.getStartMinute(), appt.getEndMinute()) + ".",
                            List.of(pid), List.of(appt.getId())));
                } else if (overlaps(oStart, oEnd, appt.occupiedStart(), appt.occupiedEnd())) {
                    conflicts.add(new ConflictInfo(ConflictType.BUFFER_VIOLATION,
                            "Participant #" + pid + "'s buffer overlaps '" + appt.getTitle() + "'.",
                            List.of(pid), List.of(appt.getId())));
                }
            }
        }

        // Multi-person intersection impossible (distinct from a single person being absent).
        if (allHaveAvailability && requested.getParticipantIds().size() > 1) {
            List<int[]> common = null;
            for (Long pid : requested.getParticipantIds()) {
                List<int[]> intervals = mergedIntervals(
                        availabilityByParticipant.getOrDefault(pid, List.of()), requested.getDate());
                common = (common == null) ? intervals : intersectIntervals(common, intervals);
            }
            if (common == null || common.isEmpty()) {
                conflicts.add(new ConflictInfo(ConflictType.IMPOSSIBLE_INTERSECTION,
                        "No time exists where all " + requested.getParticipantIds().size()
                                + " participants are simultaneously available.",
                        requested.getParticipantIds(), List.of()));
            }
        }

        return conflicts;
    }

    // ---------------------------------------------------------------------
    // G + I. Alternative slot generation and ranking (spec section 9)
    // ---------------------------------------------------------------------

    public List<ScoredSlot> rankSlots(List<Slot> candidates, ScheduleItem requested,
                                      Map<Long, List<Availability>> availabilityByParticipant,
                                      List<ScheduleItem> existingAppointments,
                                      Set<Long> recentlyDisplacedParticipantIds) {
        List<ScoredSlot> scored = new ArrayList<>();
        for (Slot slot : candidates) {
            scored.add(scoreSlot(slot, requested, availabilityByParticipant,
                    existingAppointments, recentlyDisplacedParticipantIds));
        }
        scored.sort(Comparator.comparingDouble(ScoredSlot::getScore).reversed());
        return scored;
    }

    private ScoredSlot scoreSlot(Slot slot, ScheduleItem requested,
                                 Map<Long, List<Availability>> availabilityByParticipant,
                                 List<ScheduleItem> existingAppointments,
                                 Set<Long> recentlyDisplacedParticipantIds) {
        double availabilityFit = availabilityFit(slot, requested, availabilityByParticipant);
        double priorityFit = priorityWeight(requested.getPriority());
        double preferenceFit = 1.0 - (slot.getStartMinute() / 1440.0);
        double bufferQuality = bufferQuality(slot, requested, availabilityByParticipant);

        Disruption disruption = disruptionCost(slot, requested, existingAppointments,
                recentlyDisplacedParticipantIds);
        double disruptionCost = disruption.cost;

        double score = weightAvailability * availabilityFit
                + weightPriority * priorityFit
                + weightPreference * preferenceFit
                + weightBuffer * bufferQuality
                - weightDisruption * disruptionCost;

        String reason = buildReason(slot, requested, disruption);

        return new ScoredSlot(slot, score, availabilityFit, priorityFit, preferenceFit,
                bufferQuality, disruptionCost, reason, disruption.displacedIds);
    }

    private double availabilityFit(Slot slot, ScheduleItem requested,
                                   Map<Long, List<Availability>> availabilityByParticipant) {
        if (requested.getParticipantIds().isEmpty()) return 0.0;
        int oStart = slot.getStartMinute() - requested.getBufferBefore();
        int oEnd = slot.getEndMinute() + requested.getBufferAfter();
        double sum = 0.0;
        for (Long pid : requested.getParticipantIds()) {
            List<int[]> intervals = mergedIntervals(
                    availabilityByParticipant.getOrDefault(pid, List.of()), slot.getDate());
            int[] containing = containingInterval(intervals, oStart, oEnd);
            if (containing == null) {
                sum += 0.0;
                continue;
            }
            int span = containing[1] - containing[0];
            if (span <= 0) {
                sum += 0.0;
            } else {
                int slackBefore = oStart - containing[0];
                int slackAfter = containing[1] - oEnd;
                sum += Math.min(1.0, (slackBefore + slackAfter) / (double) span);
            }
        }
        return sum / requested.getParticipantIds().size();
    }

    private double bufferQuality(Slot slot, ScheduleItem requested,
                                 Map<Long, List<Availability>> availabilityByParticipant) {
        int bufferBefore = requested.getBufferBefore();
        int bufferAfter = requested.getBufferAfter();
        int oStart = slot.getStartMinute() - bufferBefore;
        int oEnd = slot.getEndMinute() + bufferAfter;

        double qBefore = 1.0;
        double qAfter = 1.0;
        double count = 0.0;
        for (Long pid : requested.getParticipantIds()) {
            List<int[]> intervals = mergedIntervals(
                    availabilityByParticipant.getOrDefault(pid, List.of()), slot.getDate());
            int[] containing = containingInterval(intervals, oStart, oEnd);
            if (containing == null) continue;
            int slackBefore = oStart - containing[0];
            int slackAfter = containing[1] - oEnd;
            if (bufferBefore > 0) qBefore += Math.min(1.0, slackBefore / (double) bufferBefore);
            if (bufferAfter > 0) qAfter += Math.min(1.0, slackAfter / (double) bufferAfter);
            count += 1.0;
        }
        if (count == 0.0) return 0.0;
        if (bufferBefore > 0) qBefore /= count;
        if (bufferAfter > 0) qAfter /= count;
        return (qBefore + qAfter) / 2.0;
    }

    private static class Disruption {
        double cost = 0.0;
        List<Long> displacedIds = new ArrayList<>();
    }

    private Disruption disruptionCost(Slot slot, ScheduleItem requested,
                                      List<ScheduleItem> existingAppointments,
                                      Set<Long> recentlyDisplacedParticipantIds) {
        Disruption d = new Disruption();
        Set<Long> displaced = recentlyDisplacedParticipantIds == null
                ? Set.of() : recentlyDisplacedParticipantIds;
        for (ScheduleItem appt : existingAppointments) {
            if (!slot.getDate().equals(appt.getDate())) continue;
            boolean shares = appt.getParticipantIds().stream()
                    .anyMatch(requested.getParticipantIds()::contains);
            if (!shares) continue;
            if (overlaps(slot.getStartMinute() - requested.getBufferBefore(),
                    slot.getEndMinute() + requested.getBufferAfter(),
                    appt.occupiedStart(), appt.occupiedEnd())) {
                d.cost += priorityWeight(appt.getPriority());
                d.displacedIds.add(appt.getId());
                for (Long p : appt.getParticipantIds()) {
                    if (displaced.contains(p)) d.cost += fairnessPenalty;
                }
            }
        }
        return d;
    }

    private String buildReason(Slot slot, ScheduleItem requested, Disruption disruption) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fits all ").append(requested.getParticipantIds().size())
                .append(" participant(s) at ").append(format(slot.getStartMinute(), slot.getEndMinute()));
        if (requested.getBufferBefore() > 0 || requested.getBufferAfter() > 0) {
            sb.append("; ").append(requested.getBufferBefore()).append("m before / ")
                    .append(requested.getBufferAfter()).append("m after buffer satisfied");
        }
        if (disruption.displacedIds.isEmpty()) {
            sb.append("; no appointments need to move");
        } else {
            sb.append("; requires moving ").append(disruption.displacedIds.size())
                    .append(" appointment(s)");
        }
        sb.append("; ").append(requested.getPriority()).append(" priority").append(".");
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // H + section 11. Cascading rescheduling
    // ---------------------------------------------------------------------

    public CascadePlan planCascade(ScheduleItem requested,
                                   Map<Long, List<Availability>> availabilityByParticipant,
                                   List<ScheduleItem> existingAppointments) {
        List<ConflictInfo> conflicts = detectConflicts(requested, availabilityByParticipant,
                existingAppointments);

        boolean hasOverlapConflict = conflicts.stream().anyMatch(c ->
                c.getType() == ConflictType.APPOINTMENT_OVERLAP
                        || c.getType() == ConflictType.BUFFER_VIOLATION);

        if (!hasOverlapConflict) {
            if (!conflicts.isEmpty()) {
                ConflictInfo first = conflicts.get(0);
                return new CascadePlan(false, "UNSAFE", first.getDetail(), requested,
                        List.of(), List.of(), conflicts);
            }
            return new CascadePlan(true, "Safe to apply",
                    "Requested slot is free for all participants.", requested,
                    List.of(), List.of(), List.of());
        }

        Set<Long> conflictingIds = conflicts.stream()
                .filter(c -> c.getType() == ConflictType.APPOINTMENT_OVERLAP
                        || c.getType() == ConflictType.BUFFER_VIOLATION)
                .flatMap(c -> c.getAppointmentIds().stream())
                .collect(Collectors.toSet());

        List<ScheduleItem> conflicting = existingAppointments.stream()
                .filter(a -> conflictingIds.contains(a.getId()))
                .collect(Collectors.toList());

        boolean canStay = conflicting.stream().allMatch(a ->
                priorityWeight(a.getPriority()) < priorityWeight(requested.getPriority()));

        if (!canStay) {
            // Requested appointment cannot displace equal/higher priority; offer alternatives.
            List<Slot> alternatives = findCommonSlots(
                    new LinkedHashSet<>(requested.getParticipantIds()),
                    flatten(availabilityByParticipant), existingAppointments,
                    requested.getDurationMin(), requested.getBufferBefore(), requested.getBufferAfter());
            if (alternatives.isEmpty()) {
                return new CascadePlan(false, "UNSAFE",
                        "Requested slot conflicts with an equal/higher-priority appointment and no "
                                + "alternative slot exists for the requested meeting.",
                        requested, List.of(), List.of(), conflicts);
            }
            ScoredSlot best = rankSlots(alternatives, requested, availabilityByParticipant,
                    existingAppointments, Set.of()).get(0);
            Slot newSlot = best.getSlot();
            ScheduleItem moved = requested.withStart(newSlot.getStartMinute());
            Move selfMove = new Move(requested, newSlot,
                    "Requested meeting moved to a free slot (its requested time conflicts with a "
                            + "protected appointment).");
            return new CascadePlan(true, "Safe to apply",
                    "Requested meeting rescheduled to " + format(newSlot.getStartMinute(), newSlot.getEndMinute())
                            + " (" + best.getReason() + ").",
                    moved, List.of(selfMove), requested.getParticipantIds(), List.of());
        }

        // Requested stays; relocate lower-priority conflicting appointments (lowest first).
        conflicting.sort(Comparator.comparingDouble(a -> priorityWeight(a.getPriority())));
        Set<Long> visited = new HashSet<>();
        Set<Long> displaced = new HashSet<>();
        List<ScheduleItem> schedule = new ArrayList<>(existingAppointments);
        schedule.removeAll(conflicting);
        schedule.add(requested);

        List<Move> moves = new ArrayList<>();
        for (ScheduleItem item : conflicting) {
            Relocation relocation = relocate(item, schedule, availabilityByParticipant,
                    visited, displaced, 1);
            if (relocation == null) {
                return new CascadePlan(false, "UNSAFE",
                        "Could not find a valid new time for '" + item.getTitle()
                                + "' without creating a secondary conflict.",
                        requested, moves, affectedParticipants(moves), conflicts);
            }
            moves.addAll(relocation.moves);
            for (Move m : relocation.moves) {
                schedule.add(m.itemAtNewTime());
            }
        }

        List<Long> affected = affectedParticipants(moves);
        return new CascadePlan(true, "Safe to apply",
                "Kept '" + requested.getTitle() + "' at its requested time; "
                        + moves.size() + " lower-priority appointment(s) will move.",
                requested, moves, affected, List.of());
    }

    private Relocation relocate(ScheduleItem item, List<ScheduleItem> schedule,
                                Map<Long, List<Availability>> availabilityByParticipant,
                                Set<Long> visited, Set<Long> displaced, int depth) {
        if (item.getId() != null && visited.contains(item.getId())) return null;
        if (item.getId() != null) visited.add(item.getId());
        try {
            List<Slot> free = findCommonSlots(new LinkedHashSet<>(item.getParticipantIds()),
                    flatten(availabilityByParticipant), schedule,
                    item.getDurationMin(), item.getBufferBefore(), item.getBufferAfter());
            if (!free.isEmpty()) {
                ScoredSlot best = rankSlots(free, item, availabilityByParticipant,
                        schedule, displaced).get(0);
                Move m = new Move(item, best.getSlot(),
                        "Moved to avoid a conflict with a higher-priority request.");
                displaced.addAll(item.getParticipantIds());
                return new Relocation(List.of(m), priorityWeight(item.getPriority()));
            }

            if (depth >= MAX_CASCADE_DEPTH) {
                return null;
            }

            // No free slot: try to make room by displacing strictly-lower-priority blockers.
            List<Slot> candidates = findCommonSlots(new LinkedHashSet<>(item.getParticipantIds()),
                    flatten(availabilityByParticipant), List.of(),
                    item.getDurationMin(), item.getBufferBefore(), item.getBufferAfter());

            Relocation bestPlan = null;
            for (Slot cand : candidates) {
                List<ScheduleItem> blockers = schedule.stream()
                        .filter(b -> b.getId() != null
                                && overlaps(cand.getStartMinute() - item.getBufferBefore(),
                                        cand.getEndMinute() + item.getBufferAfter(),
                                        b.occupiedStart(), b.occupiedEnd())
                                && sharesParticipant(item, b))
                        .collect(Collectors.toList());
                if (blockers.isEmpty()) continue;
                boolean allLower = blockers.stream().allMatch(b ->
                        priorityWeight(b.getPriority()) < priorityWeight(item.getPriority()));
                if (!allLower) continue;

                blockers.sort(Comparator.comparingDouble(b -> priorityWeight(b.getPriority())));
                Set<Long> subVisited = new HashSet<>(visited);
                Set<Long> subDisplaced = new HashSet<>(displaced);
                List<ScheduleItem> subSchedule = new ArrayList<>(schedule);
                subSchedule.removeAll(blockers);

                boolean ok = true;
                List<Move> subMoves = new ArrayList<>();
                for (ScheduleItem blocker : blockers) {
                    Relocation r = relocate(blocker, subSchedule, availabilityByParticipant,
                            subVisited, subDisplaced, depth + 1);
                    if (r == null) { ok = false; break; }
                    subMoves.addAll(r.moves);
                    for (Move m : r.moves) subSchedule.add(m.itemAtNewTime());
                }
                if (ok) {
                    Move self = new Move(item, cand,
                            "Moved to make room for a higher-priority request.");
                    List<Move> plan = new ArrayList<>();
                    plan.add(self);
                    plan.addAll(subMoves);
                    double cost = totalDisruptionCost(plan, displaced);
                    if (bestPlan == null || cost < bestPlan.cost) {
                        bestPlan = new Relocation(plan, cost);
                    }
                }
            }
            return bestPlan;
        } finally {
            if (item.getId() != null) visited.remove(item.getId());
        }
    }

    private double totalDisruptionCost(List<Move> moves, Set<Long> alreadyDisplaced) {
        double cost = 0.0;
        Set<Long> displaced = new HashSet<>(alreadyDisplaced);
        for (Move m : moves) {
            cost += priorityWeight(m.getItem().getPriority());
            for (Long p : m.getItem().getParticipantIds()) {
                if (displaced.contains(p)) cost += fairnessPenalty;
                displaced.add(p);
            }
        }
        return cost;
    }

    private List<Long> affectedParticipants(List<Move> moves) {
        Set<Long> set = new LinkedHashSet<>();
        for (Move m : moves) set.addAll(m.getItem().getParticipantIds());
        return new ArrayList<>(set);
    }

    // ---------------------------------------------------------------------
    // Interval helpers (minute-level, all end-exclusive)
    // ---------------------------------------------------------------------

    private LocalDate deriveDate(List<Availability> availabilities) {
        for (Availability a : availabilities) return a.getDate();
        return null;
    }

    private List<Availability> flatten(Map<Long, List<Availability>> byParticipant) {
        List<Availability> all = new ArrayList<>();
        for (List<Availability> list : byParticipant.values()) all.addAll(list);
        return all;
    }

    private List<int[]> mergedIntervals(List<Availability> availabilities, LocalDate date) {
        List<int[]> raw = availabilities.stream()
                .filter(a -> a.getDate().equals(date))
                .map(a -> new int[]{a.getStartMinute(), a.getEndMinute()})
                .sorted(Comparator.comparingInt(i -> i[0]))
                .collect(Collectors.toList());
        return merge(raw);
    }

    private static List<int[]> merge(List<int[]> intervals) {
        List<int[]> result = new ArrayList<>();
        for (int[] iv : intervals) {
            if (result.isEmpty() || result.get(result.size() - 1)[1] < iv[0]) {
                result.add(new int[]{iv[0], iv[1]});
            } else {
                int[] last = result.get(result.size() - 1);
                last[1] = Math.max(last[1], iv[1]);
            }
        }
        return result;
    }

    private static List<int[]> intersectIntervals(List<int[]> a, List<int[]> b) {
        List<int[]> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            int start = Math.max(a.get(i)[0], b.get(j)[0]);
            int end = Math.min(a.get(i)[1], b.get(j)[1]);
            if (start < end) out.add(new int[]{start, end});
            if (a.get(i)[1] < b.get(j)[1]) i++;
            else j++;
        }
        return out;
    }

    private static List<int[]> subtractInterval(List<int[]> intervals, int start, int end) {
        List<int[]> out = new ArrayList<>();
        for (int[] iv : intervals) {
            if (end <= iv[0] || start >= iv[1]) {
                out.add(iv);
            } else {
                if (iv[0] < start) out.add(new int[]{iv[0], start});
                if (iv[1] > end) out.add(new int[]{end, iv[1]});
            }
        }
        return out;
    }

    private static boolean covers(List<int[]> intervals, int start, int end) {
        for (int[] iv : intervals) {
            if (iv[0] <= start && iv[1] >= end) return true;
        }
        return false;
    }

    private static int[] containingInterval(List<int[]> intervals, int start, int end) {
        for (int[] iv : intervals) {
            if (iv[0] <= start && iv[1] >= end) return iv;
        }
        return null;
    }

    private static boolean overlaps(int s1, int e1, int s2, int e2) {
        return s1 < e2 && s2 < e1;
    }

    private boolean sharesParticipant(ScheduleItem a, ScheduleItem b) {
        return a.getParticipantIds().stream().anyMatch(b.getParticipantIds()::contains);
    }

    private static String format(int startMinute, int endMinute) {
        return String.format("%02d:%02d-%02d:%02d",
                startMinute / 60, startMinute % 60, endMinute / 60, endMinute % 60);
    }
}
