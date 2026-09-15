import { useApp } from '../context/AppContext.jsx';
import { hhmmToMinutes, toHHMM, nowMinutes, pad } from '../utils/time.js';
import { isConflicting, applyFilters } from '../utils/schedule.js';
import { priorityOf, statusOf, initials } from '../theme.js';
import { Pencil, Trash } from './Icons.jsx';
import ParticipantPanel from './ParticipantPanel.jsx';

const START = 8 * 60; // 08:00
const END = 18 * 60; // 18:00
const HOURS = [];
for (let h = 8; h < 18; h++) HOURS.push(h);

function hourLabel(h) {
  if (h === 12) return '12pm';
  if (h > 12) return `${h - 12}pm`;
  return `${h}am`;
}

function left(minute) {
  const clamped = Math.max(START, Math.min(END, minute));
  return `${((clamped - START) / (END - START)) * 100}%`;
}

function width(from, to) {
  const s = Math.max(START, from);
  const e = Math.min(END, to);
  return `${(Math.max(0, e - s) / (END - START)) * 100}%`;
}

function timezoneLabel() {
  const offset = -new Date().getTimezoneOffset();
  const sign = offset >= 0 ? '+' : '-';
  const h = Math.floor(Math.abs(offset) / 60);
  const m = Math.abs(offset) % 60;
  return `${sign}${pad(h)}:${pad(m)}`;
}

function ParticipantChips({ ids, users, value }) {
  const people = ids.map((id) => users.find((u) => u.id === id)).filter(Boolean);
  const shown = people.slice(0, 3);
  const extra = people.length - shown.length;
  return (
    <div className="avatar-stack" title={people.map((p) => p.name).join(', ')}>
      {shown.map((p, i) => (
        <span
          className="avatar xs"
          key={p.id}
          style={{ background: value, zIndex: 10 - i }}
        >
          {initials(p.name)}
        </span>
      ))}
      {extra > 0 && <span className="avatar-more">+{extra}</span>}
    </div>
  );
}

/**
 * Row-per-participant calendar grid with a horizontal time axis.
 * Presentation only: it renders appointments, buffers and overlaps — every
 * scheduling decision itself stays in the Java backend.
 */
export default function CalendarTimeline({
  filters,
  onEditParticipant,
  onEditAppointment,
  onDeleteAppointment,
}) {
  const { users, appointments, availabilities } = useApp();

  const visible = applyFilters(appointments, filters);

  const availByUser = {};
  for (const a of availabilities) {
    (availByUser[a.userId] = availByUser[a.userId] || []).push(a);
  }

  const timeTemplate = {
    gridTemplateColumns: `repeat(${HOURS.length}, minmax(0, 1fr))`,
  };

  const now = nowMinutes();
  const showNow = now >= START && now <= END;

  if (users.length === 0) {
    return (
      <div className="calendar-card">
        <div className="cal-empty">
          No participants yet. Add a participant to start scheduling.
        </div>
      </div>
    );
  }

  return (
    <div className="calendar-card">
      <div className="cal-inner">
        {/* Time axis header */}
        <div className="cal-head">
          <div className="cal-gutter-head">
            <div>
              <div>GMT</div>
              <span className="tz-offset">{timezoneLabel()}</span>
            </div>
          </div>
          <div className="cal-head-times" style={timeTemplate}>
            {HOURS.map((h) => (
              <div className="cal-hour-head" key={h}>
                <span>{hourLabel(h)}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Participant rows */}
        <div className="cal-body">
          <div className="cal-rows">
            {users.map((u) => {
              const userAppts = visible.filter((a) => a.participantIds.includes(u.id));
              const windows = availByUser[u.id] || [];
              return (
                <div className="cal-row" key={u.id}>
                  <ParticipantPanel
                    variant="row"
                    participant={u}
                    appointmentCount={userAppts.length}
                    onEdit={() => onEditParticipant?.(u)}
                  />
                  <div className="cal-row-body">
                    {windows.length === 0 && (
                      <div className="hatch-block">
                        <span className="hatch-title">Unavailable</span>
                        <span className="hatch-sub">08:00 – 18:00</span>
                      </div>
                    )}

                    {windows.map((w) => {
                      const s = hhmmToMinutes(toHHMM(w.startTime));
                      const e = hhmmToMinutes(toHHMM(w.endTime));
                      return (
                        <div
                          key={`av-${w.id}`}
                          className="avail-band"
                          style={{ left: left(s), width: width(s, e) }}
                        />
                      );
                    })}

                    {userAppts.map((a) => {
                      const s = hhmmToMinutes(toHHMM(a.startTime));
                      const e = hhmmToMinutes(toHHMM(a.endTime));
                      const conflicting = isConflicting(a, appointments);
                      const prio = priorityOf(a.priority);
                      const done = a.status === 'CONFIRMED' && e <= now;
                      const status = statusOf(a.status, conflicting, done);
                      const accent = conflicting ? status : prio;
                      const compact = e - s < 45;
                      const cardStyle = {
                        left: left(s),
                        width: width(s, e),
                        '--card-rgb': accent.rgb,
                        '--card-accent': accent.hex,
                      };

                      return (
                        <div key={a.id}>
                          {a.bufferBefore > 0 && (
                            <div
                              className="buffer-strip"
                              style={{ left: left(s - a.bufferBefore), width: width(s - a.bufferBefore, s) }}
                              title={`Buffer ${a.bufferBefore} min`}
                            />
                          )}
                          <div
                            className={`appt-card${compact ? ' compact' : ''}`}
                            style={cardStyle}
                            title={`${a.title} (${toHHMM(a.startTime)}–${toHHMM(a.endTime)})`}
                          >
                            <div className="appt-actions">
                              <button
                                type="button"
                                className="appt-action"
                                title="Edit appointment"
                                onClick={(ev) => {
                                  ev.stopPropagation();
                                  onEditAppointment?.(a);
                                }}
                              >
                                <Pencil size={14} />
                              </button>
                              <button
                                type="button"
                                className="appt-action danger"
                                title="Delete appointment"
                                onClick={(ev) => {
                                  ev.stopPropagation();
                                  onDeleteAppointment?.(a);
                                }}
                              >
                                <Trash size={14} />
                              </button>
                            </div>
                            <div className="appt-head">
                              <span className="appt-title">{a.title}</span>
                            </div>
                            <span className="appt-time">
                              {toHHMM(a.startTime)} – {toHHMM(a.endTime)}
                            </span>
                            {!compact && (
                              <div className="appt-foot">
                                <ParticipantChips
                                  ids={a.participantIds}
                                  users={users}
                                  value={accent.hex}
                                />
                              </div>
                            )}
                          </div>
                          {a.bufferAfter > 0 && (
                            <div
                              className="buffer-strip"
                              style={{ left: left(e), width: width(e, e + a.bufferAfter) }}
                              title={`Buffer ${a.bufferAfter} min`}
                            />
                          )}
                        </div>
                      );
                    })}

                    {showNow && <div className="now-line" style={{ left: left(now) }} />}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
