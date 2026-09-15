import { toHHMM } from '../utils/time.js';
import { categoryColor } from '../theme.js';
import AiExplanation from './AiExplanation.jsx';

export default function SlotRecommendations({ slots, aiPayload }) {
  const list = slots || [];

  if (list.length === 0) {
    return (
      <section className="panel">
        <h3 className="panel-title">Recommendations</h3>
        <p className="muted">
          No slots found. Adjust participants, duration or availability.
        </p>
      </section>
    );
  }

  return (
    <section className="panel">
      <h3 className="panel-title">Recommended slots</h3>
      <ol className="rec-list">
        {list.map((s, i) => {
          const c = categoryColor(i);
          return (
            <li
              key={i}
              className="rec-card"
              style={{ '--card-rgb': c.rgb, '--card-accent': c.hex }}
            >
              <div className="rec-top">
                <span className="rec-time">
                  {toHHMM(s.startTime)} – {toHHMM(s.endTime)}
                </span>
                <span className="rank-badge">
                  <span className="pill-dot" />
                  {i === 0 ? 'Best fit' : `#${i + 1}`} · {s.score.toFixed(2)}
                </span>
              </div>
              <p className="rec-reason">{s.reason}</p>
              <div className="breakdown">
                <span>availability {s.availabilityFit.toFixed(2)}</span>
                <span>priority {s.priorityFit.toFixed(2)}</span>
                <span>preference {s.preferenceFit.toFixed(2)}</span>
                <span>buffer {s.bufferQuality.toFixed(2)}</span>
                <span>disruption −{s.disruptionCost.toFixed(2)}</span>
              </div>
            </li>
          );
        })}
      </ol>
      <div style={{ marginTop: '16px' }}>
        <AiExplanation payload={aiPayload} />
      </div>
    </section>
  );
}
