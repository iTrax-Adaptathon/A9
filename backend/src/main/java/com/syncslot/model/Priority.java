package com.syncslot.model;

/**
 * Appointment priority. Higher values receive stronger protection during
 * scheduling and conflict resolution: URGENT > HIGH > MEDIUM > LOW.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
