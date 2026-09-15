import { useState } from 'react';
import { useApp } from '../context/AppContext.jsx';
import { api } from '../services/api.js';
import { avatarColor, initials } from '../theme.js';
import { MoreHorizontal, Plus } from './Icons.jsx';

/**
 * Renders participants in two contexts:
 *  - default: the management panel (list + add form) shown in a drawer
 *  - row:     a single calendar row header for the grid
 */
export default function ParticipantPanel({ variant = 'panel', participant, appointmentCount = 0, onEdit }) {
  const { users, refresh } = useApp();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);

  async function add() {
    if (!name.trim()) return;
    setBusy(true);
    try {
      await api.createUser(name.trim(), email.trim() || null);
      setName('');
      setEmail('');
      await refresh();
    } catch (e) {
      alert(e.message);
    } finally {
      setBusy(false);
    }
  }

  if (variant === 'row') {
    const count = appointmentCount;
    return (
      <div className="cal-row-head">
        <span className="avatar sm" style={{ background: avatarColor(participant.id) }}>
          {initials(participant.name)}
        </span>
        <div className="col-head-main">
          <div className="col-name">{participant.name}</div>
          <div className="col-sub">
            {count} appointment{count === 1 ? '' : 's'} today
          </div>
        </div>
        <button
          type="button"
          className="icon-btn-xs"
          onClick={onEdit}
          title={`Manage ${participant.name}`}
        >
          <MoreHorizontal size={17} />
        </button>
      </div>
    );
  }

  return (
    <section className="panel">
      <h3 className="panel-title">Participants</h3>
      {users.length === 0 && <p className="muted">No participants yet.</p>}
      <div className="participant-chips">
        {users.map((u) => (
          <span className="participant-chip" key={u.id}>
            <span className="avatar xs" style={{ background: avatarColor(u.id) }}>
              {initials(u.name)}
            </span>
            {u.name}
          </span>
        ))}
      </div>
      <div className="form-grid">
        <label className="field">
          Name
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Alice"
          />
        </label>
        <label className="field">
          Email (optional)
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="alice@syncslot.local"
          />
        </label>
      </div>
      <div className="form-actions">
        <button
          type="button"
          className="btn btn-primary"
          onClick={add}
          disabled={busy || !name.trim()}
        >
          <Plus size={16} />
          Add participant
        </button>
      </div>
    </section>
  );
}
