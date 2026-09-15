package com.syncslot.util;

import java.time.LocalTime;

/** Converts between the JPA entities' LocalTime and the engine's minute-of-day ints. */
public final class TimeUtil {

    private TimeUtil() {
    }

    public static int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    public static LocalTime toLocalTime(int minutes) {
        int m = Math.max(0, Math.min(1439, minutes));
        return LocalTime.of(m / 60, m % 60);
    }

    public static int durationBetween(LocalTime start, LocalTime end) {
        return toMinutes(end) - toMinutes(start);
    }
}
