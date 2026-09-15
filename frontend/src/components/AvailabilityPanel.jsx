import { useState } from 'react';
import { useApp } from '../context/AppContext.jsx';
import { api } from '../services/api.js';
import { toHHMM } from '../utils/time.js';
import { Plus, X } from './Icons.jsx';

export default function AvailabilityPanel() {
  const { users, availabilities, selectedDate, refresh } = useApp();
  const [userId, setUserId] = useState('');
  const [start, setStart] = useState('09:00');
  const [end, setEnd] = useState('17:00');

  const byUser = {};
  for (const a of availabilities) {
    (byUser[a.userId] = byUser[a.userId] || []).push(a);
  }

  async function add() {
    if (!userId || !start || !end) return;
    try {
      await api.createAvailability({
        userId: Number(userId),
        date: selectedDate,
        startTime: `${start}:00`,
        endTime: `${end}:00`,
      });
      await refresh();
    } catch (e) {
      alert(e.message);
    }
  }

  async function remove(id) {
    try {
      await api.deleteAvailability(id);
      await refresh();
    } catch (e) {
      alert(e.message);
    }
  }

  return (
    <section className="panel">
      <h3 className="panel-title">Availability</h3>
      <div className="availability-rows">
        {users.map((u) => {
          const rows = byUser[u.id] || [];
          return (
            <div className="availability-row" key={u.id}>
              <span className="name">{u.name}</span>
              <span className="windows">
                {rows.length === 0 ? (
                  <em className="muted">unavailable</em>
                ) : (
                  rows.map((a) => (
                    <span className="chip" key={a.id}>
                      {toHHMM(a.startTime)}–{toHHMM(a.endTime)}
                      <button
                        type="button"
                        className="chip-x"
                        onClick={() => remove(a.id)}
                        title="Remove"
                      >
                        <X size={12} />
                      </button>
                    </span>
                  ))
                )}
              </span>
            </div>
          );
        })}
      </div>

      <div className="row">
        <select className="input" value={userId} onChange={(e) => setUserId(e.target.value)}>
          <option value="">Participant…</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>{u.name}</option>
          ))}
        </select>
        <input className="input" type="time" value={start} onChange={(e) => setStart(e.target.value)} />
        <input className="input" type="time" value={end} onChange={(e) => setEnd(e.target.value)} />
      </div>
      <div className="form-actions">
        <button
          type="button"
          className="btn btn-primary"
          onClick={add}
          disabled={!userId}
        >
          <Plus size={16} />
          Add window
        </button>
      </div>
    </section>
  );
}
