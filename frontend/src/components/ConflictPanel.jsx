import { AlertTriangle } from './Icons.jsx';

export default function ConflictPanel({ conflicts }) {
  const list = conflicts || [];

  return (
    <section className="panel">
      <h3 className="panel-title">Conflicts</h3>
      {list.length === 0 ? (
        <p className="muted">No unresolved conflicts.</p>
      ) : (
        <ul className="conflict-list">
          {list.map((c, i) => (
            <li key={i} className="conflict-card">
              <span className="conflict-icon"><AlertTriangle size={16} /></span>
              <div>
                <span className="conflict-type">{c.type}</span>
                <p>{c.message}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
