import { useApp } from '../context/AppContext.jsx';
import { toHHMM } from '../utils/time.js';
import { priorityOf } from '../theme.js';
import { Check, AlertTriangle } from './Icons.jsx';
import AiExplanation from './AiExplanation.jsx';

function PriorityBadge({ priority }) {
  const p = priorityOf(priority);
  return (
    <span className="prio" style={{ '--p-hex': p.hex, '--p-rgb': p.rgb }}>
      {p.label}
    </span>
  );
}

export default function ImpactPreview({ preview, onApply, onCancel, applying, aiPayload }) {
  const { users } = useApp();

  const affected = preview.participantsAffected || [];
  const conflicts = preview.conflictsCreated || [];
  const safe = preview.status === 'Safe to apply';

  const affectedNames = affected
    .map((id) => {
      const u = users.find((x) => x.id === id);
      return u ? u.name : `#${id}`;
    })
    .join(', ');

  return (
    <div className="impact-preview">
      <div className="impact-section">
        <h4>Request</h4>
        <p>
          {preview.requestTitle}, {toHHMM(preview.requestStartTime)}–
          {toHHMM(preview.requestEndTime)}, {preview.requestPriority}
        </p>
        {preview.requestedNewStartTime && (
          <p className="notice">
            Requested meeting will be moved to {toHHMM(preview.requestedNewStartTime)}–
            {toHHMM(preview.requestedNewEndTime)}.
          </p>
        )}
      </div>

      <div className="impact-section">
        <h4>Impact</h4>
        {preview.impact.length === 0 ? (
          <p className="muted">No existing appointments need to move.</p>
        ) : (
          <ul className="impact-list">
            {preview.impact.map((m, i) => (
              <li key={i}>
                <div className="move-row">
                  <span className="move-time">
                    {m.title}: {toHHMM(m.oldStartTime)}–{toHHMM(m.oldEndTime)} →{' '}
                    {toHHMM(m.newStartTime)}–{toHHMM(m.newEndTime)}
                  </span>
                  <PriorityBadge priority={m.priority} />
                </div>
                <p className="muted" style={{ marginTop: '4px' }}>{m.reason}</p>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="impact-section">
        <h4>Participants affected</h4>
        <p>{affected.length ? affectedNames : 'None'}</p>
      </div>

      <div className="impact-section">
        <h4>Conflicts created</h4>
        <p>
          {conflicts.length ? conflicts.map((c) => c.message).join('; ') : 'None'}
        </p>
      </div>

      <div className="impact-explain-row">
        <div className={`impact-status ${safe ? 'safe' : 'unsafe'}`}>
          <span className="status-line">
            {safe ? <Check size={15} /> : <AlertTriangle size={15} />}
            {preview.status}
          </span>
          <p>{preview.reason}</p>
        </div>

        <AiExplanation payload={aiPayload} auto />
      </div>

      <div className="modal-actions">
        <button type="button" className="btn btn-outline" onClick={onCancel}>
          Cancel
        </button>
        <button
          type="button"
          className="btn btn-primary"
          onClick={onApply}
          disabled={applying}
        >
          {applying && <span className="spinner" />}
          {applying ? 'Applying…' : 'Confirm & apply'}
        </button>
      </div>
    </div>
  );
}
