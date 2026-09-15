package com.syncslot.algorithm;

import com.syncslot.algorithm.SchedulingEngine.CascadePlan;
import com.syncslot.algorithm.SchedulingEngine.ConflictInfo;
import com.syncslot.algorithm.SchedulingEngine.ConflictType;
import com.syncslot.algorithm.SchedulingEngine.ScheduleItem;
import com.syncslot.algorithm.SchedulingEngine.Slot;
import com.syncslot.model.Priority;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the pure scheduling engine, covering spec section 14
 * scenarios A-I (J/K are covered by the Spring Boot integration test).
 */
class SchedulingEngineTest {

    private static final LocalDate D = LocalDate.of(2026, 9, 15);
    private final SchedulingEngine engine = new SchedulingEngine();

    // ---- helpers -------------------------------------------------------

    private Availability av(long pid, int start, int end) {
        return new Availability(pid, D, start, end);
    }

    private Map<Long, List<Availability>> availMap(Availability... list) {
        Map<Long, List<Availability>> map = new HashMap<>();
        for (Availability a : list) {
            map.computeIfAbsent(a.getParticipantId(), k -> new ArrayList<>()).add(a);
        }
        return map;
    }

    private List<Availability> availList(Availability... list) {
        return new ArrayList<>(Arrays.asList(list));
    }

    private ScheduleItem item(Long id, String title, List<Long> pids, int start, int end,
                              int duration, int bufferBefore, int bufferAfter, Priority priority) {
        return new ScheduleItem(id, title, pids, D, start, end, duration, bufferBefore, bufferAfter, priority);
    }

    private boolean containsSlot(List<Slot> slots, int start, int end) {
        return slots.stream().anyMatch(s -> s.getStartMinute() == start && s.getEndMinute() == end);
    }

    // ---- A. Two people with one common slot -----------------------------

    @Test
    void twoPeopleOneCommonSlot() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60));
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(), 60, 0, 0);

        // common window is 10:00-14:00, so 60-min slots from 10:00 to 13:00
        assertTrue(containsSlot(slots, 10 * 60, 11 * 60));
        assertTrue(containsSlot(slots, 13 * 60, 14 * 60));
        assertFalse(containsSlot(slots, 9 * 60, 10 * 60), "Bob not available before 10:00");
        assertFalse(containsSlot(slots, 14 * 60, 15 * 60), "Alice not available after 14:00");
    }

    // ---- B. Three people with partial overlap ----------------------------

    @Test
    void threePeoplePartialOverlap() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60), av(3, 10 * 60, 12 * 60));
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L, 3L)), avail,
                List.of(), 60, 0, 0);

        // intersection is 10:00-12:00 -> minute-level slots from 10:00 to 11:00 (start times)
        assertFalse(slots.isEmpty());
        assertTrue(containsSlot(slots, 10 * 60, 11 * 60));
        assertTrue(containsSlot(slots, 11 * 60, 12 * 60));
        assertFalse(containsSlot(slots, 12 * 60, 13 * 60), "Charlie unavailable after 12:00");
        assertTrue(slots.stream().allMatch(s -> s.getStartMinute() >= 10 * 60 && s.getEndMinute() <= 12 * 60));
    }

    // ---- C. Insert into an already-busy schedule -------------------------

    @Test
    void insertIntoBusySchedule() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60));
        ScheduleItem existing = item(10L, "Team Catchup", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.LOW);

        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(existing), 60, 0, 0);
        assertFalse(containsSlot(slots, 10 * 60, 11 * 60), "busy slot must be excluded");
        assertTrue(containsSlot(slots, 11 * 60, 12 * 60));

        // conflict detection for a 10:00-11:00 request
        ScheduleItem request = item(null, "New", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.HIGH);
        List<ConflictInfo> conflicts = engine.detectConflicts(request, availMap(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60)), List.of(existing));
        assertTrue(conflicts.stream().anyMatch(c -> c.getType() == ConflictType.APPOINTMENT_OVERLAP));
    }

    // ---- D. Change an appointment's duration -----------------------------

    @Test
    void changeDuration() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60));
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(), 90, 0, 0);
        // common 10:00-14:00 -> 90-min slots from 10:00 to 12:30
        assertTrue(containsSlot(slots, 10 * 60, 11 * 60 + 30));
        assertTrue(containsSlot(slots, 12 * 60 + 30, 14 * 60));
        assertFalse(containsSlot(slots, 12 * 60 + 31, 14 * 60 + 1));

        // 5-hour duration cannot fit a 4-hour common window
        assertTrue(engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(), 300, 0, 0).isEmpty());
    }

    // ---- E. A participant becomes unavailable ----------------------------

    @Test
    void participantBecomesUnavailable() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60));
        // Charlie has no availability at all
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L, 3L)), avail,
                List.of(), 60, 0, 0);
        assertTrue(slots.isEmpty());

        ScheduleItem request = item(null, "New", List.of(1L, 2L, 3L), 10 * 60, 11 * 60, 60, 0, 0, Priority.MEDIUM);
        List<ConflictInfo> conflicts = engine.detectConflicts(request, availMap(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60)), List.of());
        assertTrue(conflicts.stream().anyMatch(c ->
                c.getType() == ConflictType.PARTICIPANT_UNAVAILABLE && c.getParticipantIds().contains(3L)));
    }

    // ---- F. Two candidate slots, different priority implications ---------

    @Test
    void priorityAffectsRanking() {
        Map<Long, List<Availability>> avail = availMap(av(1, 9 * 60, 14 * 60), av(2, 9 * 60, 14 * 60));
        Slot early = new Slot(D, 9 * 60, 10 * 60);
        Slot late = new Slot(D, 13 * 60, 14 * 60);

        ScheduleItem urgent = item(null, "Urgent", List.of(1L, 2L), 0, 60, 60, 0, 0, Priority.URGENT);
        List<SchedulingEngine.ScoredSlot> urgentRanked =
                engine.rankSlots(List.of(late, early), urgent, avail, List.of(), Set.of());
        // earlier slot outranks later slot (preference fit)
        assertEquals(9 * 60, urgentRanked.get(0).getSlot().getStartMinute());

        ScheduleItem low = item(null, "Low", List.of(1L, 2L), 0, 60, 60, 0, 0, Priority.LOW);
        SchedulingEngine.ScoredSlot lowScore = engine.rankSlots(List.of(early), low, avail, List.of(), Set.of()).get(0);
        SchedulingEngine.ScoredSlot highScore = engine.rankSlots(List.of(early), urgent, avail, List.of(), Set.of()).get(0);
        assertTrue(highScore.getPriorityFit() > lowScore.getPriorityFit(),
                "higher priority must contribute a higher priority fit");
    }

    // ---- G. Buffers invalidate a slot that looks free without them -------

    @Test
    void buffersInvalidateEdgeSlot() {
        List<Availability> avail = availList(av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60));
        // without buffer, 10:00-11:00 is free
        assertTrue(containsSlot(engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(), 60, 0, 0), 10 * 60, 11 * 60));

        // with a 15-min buffer before, 10:00 is invalid for Bob (needs 09:45, before his 10:00 start)
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L, 2L)), avail,
                List.of(), 60, 15, 0);
        assertFalse(containsSlot(slots, 10 * 60, 11 * 60), "buffer before extends outside Bob's availability");
        assertTrue(containsSlot(slots, 10 * 60 + 15, 11 * 60 + 15));
    }

    // ---- H. High priority displaces lower priority (Alice/Bob/Charlie) ---

    @Test
    void highPriorityDisplacesLowerPriority() {
        Map<Long, List<Availability>> avail = availMap(
                av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 15 * 60), av(3, 10 * 60, 12 * 60));

        ScheduleItem teamCatchup = item(10L, "Team Catchup", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.LOW);
        ScheduleItem executive = item(null, "Executive Review", List.of(1L, 2L, 3L), 10 * 60, 11 * 60, 60, 0, 0, Priority.HIGH);

        CascadePlan plan = engine.planCascade(executive, avail, List.of(teamCatchup));

        assertTrue(plan.isSafe(), "plan should be safe");
        assertEquals("Safe to apply", plan.getStatus());
        assertEquals(1, plan.getMoves().size(), "exactly one existing appointment moves");
        SchedulingEngine.Move move = plan.getMoves().get(0);
        assertEquals(10L, move.getItem().getId());
        assertEquals("Team Catchup", move.getItem().getTitle());
        // requested meeting stays at 10:00-11:00
        assertEquals(10 * 60, plan.getRequested().getStartMinute());
        assertEquals(11 * 60, plan.getRequested().getEndMinute());
        // moved to a valid alternative (11:00+ given everyone's availability)
        assertNotEquals(10 * 60, move.getNewSlot().getStartMinute());
        assertTrue(move.getNewSlot().getStartMinute() >= 11 * 60);
    }

    // ---- I. Cascade with no valid alternative must report failure --------

    @Test
    void cascadeWithNoAlternativeReportsFailure() {
        Map<Long, List<Availability>> avail = availMap(
                av(1, 9 * 60, 14 * 60), av(2, 10 * 60, 11 * 60), av(3, 10 * 60, 11 * 60));

        ScheduleItem teamCatchup = item(10L, "Team Catchup", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.LOW);
        ScheduleItem executive = item(null, "Executive Review", List.of(1L, 2L, 3L), 10 * 60, 11 * 60, 60, 0, 0, Priority.HIGH);

        CascadePlan plan = engine.planCascade(executive, avail, List.of(teamCatchup));

        assertFalse(plan.isSafe(), "plan must not force an invalid schedule");
        assertEquals("UNSAFE", plan.getStatus());
        assertTrue(plan.getReason().contains("Could not find"), "reason should explain the failure");
    }

    // ---- extra: minute-level scheduling (not whole hours) ----------------

    @Test
    void minuteLevelScheduling() {
        List<Availability> avail = availList(av(1, 9 * 60, 10 * 60));
        List<Slot> slots = engine.findCommonSlots(new LinkedHashSet<>(List.of(1L)), avail,
                List.of(), 30, 0, 0);
        assertTrue(containsSlot(slots, 9 * 60, 9 * 60 + 30));
        assertTrue(containsSlot(slots, 9 * 60 + 30, 10 * 60));
    }

    // ---- extra: equal priority cannot be displaced -----------------------

    @Test
    void equalPriorityCannotBeDisplaced() {
        Map<Long, List<Availability>> avail = availMap(av(1, 9 * 60, 14 * 60), av(2, 9 * 60, 14 * 60));
        ScheduleItem existing = item(10L, "Existing", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.HIGH);
        ScheduleItem request = item(null, "New", List.of(1L, 2L), 10 * 60, 11 * 60, 60, 0, 0, Priority.HIGH);

        CascadePlan plan = engine.planCascade(request, avail, List.of(existing));

        // requested cannot displace equal priority, so it is moved to a free slot instead
        assertTrue(plan.isSafe());
        assertNotEquals(10 * 60, plan.getRequested().getStartMinute(),
                "requested meeting should be moved to a free slot");
    }
}
