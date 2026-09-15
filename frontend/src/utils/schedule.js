import { hhmmToMinutes, toHHMM, nowMinutes } from './time.js';

/**
 * Presentation-only overlap detection. The scheduler's real conflict
 * decisions live in the Java backend; this is only used to highlight
 * overlapping appointments on the grid and to filter the list view.
 */
export function isConflicting(appt, all) {
  const s1 = hhmmToMinutes(toHHMM(appt.startTime));
  const e1 = hhmmToMinutes(toHHMM(appt.endTime));
  for (const other of all) {
    if (other.id === appt.id) continue;
    const shareParticipant = appt.participantIds.some((p) => other.participantIds.includes(p));
    if (!shareParticipant) continue;
    const s2 = hhmmToMinutes(toHHMM(other.startTime));
    const e2 = hhmmToMinutes(toHHMM(other.endTime));
    if (s1 < e2 && s2 < e1) return true;
  }
  return false;
}

/**
 * Apply the view-controls Filters (priority + status) to an appointment list.
 * A CONFLICT filter matches appointments the presentation layer flags as
 * overlapping; other statuses match the stored status string.
 */
export function applyFilters(appointments, filters, now = nowMinutes()) {
  const priorities = filters?.priorities || [];
  const statuses = filters?.statuses || [];
  if (priorities.length === 0 && statuses.length === 0) return appointments;

  return appointments.filter((a) => {
    if (priorities.length && !priorities.includes(a.priority)) return false;
    if (statuses.length) {
      const conflictMatch = statuses.includes('CONFLICT') && isConflicting(a, appointments);
      const isDone =
        a.status === 'CONFIRMED' && hhmmToMinutes(toHHMM(a.endTime)) <= now;
      const doneMatch = statuses.includes('DONE') && isDone;
      const statusMatch = statuses.includes(a.status);
      if (!conflictMatch && !doneMatch && !statusMatch) return false;
    }
    return true;
  });
}
