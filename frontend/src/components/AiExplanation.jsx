import { useCallback, useEffect, useState } from 'react';
import { api } from '../services/api.js';

/**
 * AI narration control. The AI only explains a decision the backend scheduling
 * engine already made; it never influences scheduling.
 *
 * - manual mode (default): a "Generate AI Explanation" button.
 * - auto mode: fetches immediately on mount and shows the result inline, used
 *   next to the deterministic engine message when reviewing a booking.
 *
 * On any failure we fall back to the deterministic engine explanation.
 */
export default function AiExplanation({ payload, label = '✨ Generate AI Explanation', auto = false }) {
  const [status, setStatus] = useState('idle');
  const [explanation, setExplanation] = useState('');
  const [aiGenerated, setAiGenerated] = useState(true);

  const generate = useCallback(async () => {
    if (!payload) return;
    setStatus('loading');
    try {
      const res = await api.explainSchedule(payload);
      setExplanation(res?.explanation || payload.fallbackExplanation || 'No explanation available.');
      setAiGenerated(res?.aiGenerated === true);
    } catch {
      setExplanation(payload.fallbackExplanation || 'Unable to generate an explanation right now.');
      setAiGenerated(false);
    } finally {
      setStatus('done');
    }
  }, [payload]);

  useEffect(() => {
    if (auto && payload) generate();
  }, [auto, payload, generate]);

  if (auto) {
    return (
      <div className="ai-explain-box ai-explain-box--auto">
        <div className="ai-explain-head">
          <span className="ai-explain-title">✨ AI Summary</span>
          <button
            type="button"
            className="ai-explain-refresh"
            onClick={generate}
            disabled={status === 'loading' || !payload}
          >
            {status === 'loading' ? '…' : 'Regenerate'}
          </button>
        </div>

        {status === 'loading' && (
          <p className="ai-explain-pending">
            <span className="spinner spinner-muted" />
            Asking AI…
          </p>
        )}

        {status === 'done' && aiGenerated && (
          <p className="ai-explain-text">{explanation}</p>
        )}

        {status === 'done' && !aiGenerated && (
          <p className="ai-explain-text muted">
            AI unavailable — using the engine's explanation.
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="ai-explain">
      <button
        type="button"
        className="btn btn-outline ai-explain-btn"
        onClick={generate}
        disabled={!payload || status === 'loading'}
      >
        {status === 'loading' && <span className="spinner spinner-muted" />}
        {status === 'loading' ? 'Generating explanation…' : label}
      </button>

      {status === 'done' && (
        <div className="ai-explain-box">
          <p className="ai-explain-text">{explanation}</p>
          {!aiGenerated && (
            <span className="ai-explain-note">Built-in explanation — AI unavailable</span>
          )}
        </div>
      )}
    </div>
  );
}
