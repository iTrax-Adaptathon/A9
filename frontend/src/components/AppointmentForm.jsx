import { useState } from 'react';
import { useApp } from '../context/AppContext.jsx';
import { addMinutes, toHHMM } from '../utils/time.js';
import { PRIORITIES, PRIORITY, avatarColor, initials } from '../theme.js';

export default function AppointmentForm({ appointment, onFindSlots, onBook, onSave, onCancel }) {
  const { users, selectedDate } = useApp();
  const editing = Boolean(appointment);
  const [title, setTitle] = useState(appointment?.title ?? '');
  const [selected, setSelected] = useState(appointment?.participantIds ?? []);
  const [start, setStart] = useState(appointment ? toHHMM(appointment.startTime) : '10:00');
  const [duration, setDuration] = useState(appointment?.duration ?? 60);
  const [bufferBefore, setBufferBefore] = useState(appointment?.bufferBefore ?? 0);
  const [bufferAfter, setBufferAfter] = useState(appointment?.bufferAfter ?? 0);
  const [priority, setPriority] = useState(appointment?.priority ?? 'HIGH');

  function toggle(id) {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  function buildPayload() {
    const participantIds = selected;
    const startTime = `${start}:00`;
    const endTime = `${addMinutes(start, Number(duration))}:00`;
    return {
      title: title.trim() || 'Untitled',
      participantIds,
      date: appointment?.date ?? selectedDate,
      startTime,
      endTime,
      duration: Number(duration),
      bufferBefore: Number(bufferBefore),
      bufferAfter: Number(bufferAfter),
      priority,
    };
  }

  function findSlots() {
    onFindSlots?.(buildPayload());
  }

  function submit() {
    if (selected.length === 0) {
      alert('Select at least one participant.');
      return;
    }
    if (editing) onSave?.(buildPayload());
    else onBook?.(buildPayload());
  }

  return (
    <form className="stack" onSubmit={(e) => e.preventDefault()}>
      <div>
        <label className="field full">
          Title
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Executive Review"
          />
        </label>
      </div>

      <div className="form-grid">
        <label className="field">
          Date
          <input value={appointment?.date ?? selectedDate} disabled />
        </label>
        <label className="field">
          Start
          <input type="time" value={start} onChange={(e) => setStart(e.target.value)} />
        </label>
        <label className="field">
          Duration (min)
          <input
            type="number"
            min="15"
            step="15"
            value={duration}
            onChange={(e) => setDuration(e.target.value)}
          />
        </label>
        <label className="field">
          Buffer before (min)
          <input
            type="number"
            min="0"
            step="5"
            value={bufferBefore}
            onChange={(e) => setBufferBefore(e.target.value)}
          />
        </label>
        <label className="field">
          Buffer after (min)
          <input
            type="number"
            min="0"
            step="5"
            value={bufferAfter}
            onChange={(e) => setBufferAfter(e.target.value)}
          />
        </label>
      </div>

      <div>
        <h4 className="panel-title">Priority</h4>
        <div className="priority-options">
          {PRIORITIES.map((p) => (
            <button
              type="button"
              key={p}
              className={`priority-option${priority === p ? ' active' : ''}`}
              style={{ '--p-hex': PRIORITY[p].hex, '--p-rgb': PRIORITY[p].rgb }}
              onClick={() => setPriority(p)}
            >
              <span className="priority-dot" />
              {PRIORITY[p].label}
            </button>
          ))}
        </div>
      </div>

      <div>
        <h4 className="panel-title">Participants</h4>
        <div className="pill-checks">
          {users.map((u) => {
            const checked = selected.includes(u.id);
            return (
              <label key={u.id} className={`pill-check${checked ? ' checked' : ''}`}>
                <input
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggle(u.id)}
                />
                <span className="avatar xs" style={{ background: avatarColor(u.id) }}>
                  {initials(u.name)}
                </span>
                {u.name}
              </label>
            );
          })}
        </div>
      </div>

      <div className="form-actions">
        {editing ? (
          <>
            <button type="button" className="btn btn-outline" onClick={onCancel}>
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={submit}
              disabled={selected.length === 0}
            >
              Save changes
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              className="btn btn-outline"
              onClick={findSlots}
              disabled={selected.length === 0}
            >
              Find slots
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={submit}
              disabled={selected.length === 0}
            >
              Book
            </button>
          </>
        )}
      </div>
    </form>
  );
}
