package com.syncslot.algorithm;

import java.time.LocalDate;

/**
 * INHERITED from the A9 repo (algorithm/Availability.java) and adapted for
 * Sprint 2: it was originally a standalone command-line helper representing a
 * participant's available window. In Sprint 2 it is reused verbatim as the
 * engine's minute-level time-window primitive and is now consumed by
 * {@link SchedulingEngine} (via the service layer) instead of by a CLI main().
 *
 * <p>Assumption on prior shape: the original class held a participant id, a
 * date and integer start/end minutes since midnight. If the inherited file
 * used different field names, only the service-layer conversion would change.
 *
 * <p>Times are minutes-since-midnight (0..1439). {@code startMinute} is
 * inclusive, {@code endMinute} is exclusive.
 */
public final class Availability {

    private final long participantId;
    private final LocalDate date;
    private final int startMinute;
    private final int endMinute;

    public Availability(long participantId, LocalDate date, int startMinute, int endMinute) {
        if (startMinute < 0 || endMinute < startMinute || endMinute > 1440) {
            throw new IllegalArgumentException("Invalid availability window: "
                    + startMinute + ".." + endMinute);
        }
        this.participantId = participantId;
        this.date = date;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    public long getParticipantId() {
        return participantId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public int duration() {
        return endMinute - startMinute;
    }

    public boolean contains(int minute) {
        return minute >= startMinute && minute < endMinute;
    }

    public boolean overlaps(Availability other) {
        return sameDate(other) && startMinute < other.endMinute && other.startMinute < endMinute;
    }

    public boolean sameDate(Availability other) {
        return other != null && date.equals(other.date);
    }

    @Override
    public String toString() {
        return String.format("%s %02d:%02d-%02d:%02d (pid=%d)",
                date, startMinute / 60, startMinute % 60, endMinute / 60, endMinute % 60, participantId);
    }
}
