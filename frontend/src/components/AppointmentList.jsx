import { useApp } from '../context/AppContext.jsx';
import { toHHMM, hhmmToMinutes, nowMinutes } from '../utils/time.js';
import { isConflicting, applyFilters } from '../utils/schedule.js';
import { priorityOf, statusOf } from '../theme.js';

export default function AppointmentList({ filters }) {
  const { users, appointments } = useApp();
  const now = nowMinutes();
  const visible = applyFilters(appointments, filters, now);

  function namesFor(ids) {
    return ids
      .map((id) => users.find((u) => u.id === id)?.name)
      .filter(Boolean)
      .join(', ');
  }

  if (visible.length === 0) {
    return (
      <div className="calendar-card">
        <div className="cal-empty">No appointments match the current view.</div>
      </div>
    );
  }

  return (
    <div className="appt-list">
      {visible.map((a) => {
        const prio = priorityOf(a.priority);
        const conflicting = isConflicting(a, appointments);
        const done = a.status === 'CONFIRMED' && hhmmToMinutes(toHHMM(a.endTime)) <= now;
        const status = statusOf(a.status, conflicting, done);
        const accent = conflicting ? status : prio;
        return (
          <div
            key={a.id}
            className="appt-list-item"
            style={{ '--card-rgb': accent.rgb, '--card-accent': accent.hex }}
          >
            <div className="appt-list-time">
              {toHHMM(a.startTime)} – {toHHMM(a.endTime)}
            </div>
            <div className="appt-list-main">
              <div className="appt-list-title">{a.title}</div>
              <div className="appt-list-sub">{namesFor(a.participantIds)}</div>
            </div>
            <div className="appt-list-right">
              <span className="prio" style={{ '--p-hex': prio.hex, '--p-rgb': prio.rgb }}>
                {prio.label}
              </span>
              <span
                className="status-pill"
                style={{ '--pill-rgb': status.rgb, '--pill-accent': status.hex }}
              >
                <span className="pill-dot" />
                {status.label}
              </span>
            </div>
          </div>
        );
      })}
    </div>
  );
}
