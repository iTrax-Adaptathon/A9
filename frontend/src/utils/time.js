export function pad(n) {
  return String(n).padStart(2, '0');
}

/** "10:00:00" or "10:00" -> "10:00" */
export function toHHMM(t) {
  if (!t) return '';
  return t.slice(0, 5);
}

export function minutesToHHMM(m) {
  return `${pad(Math.floor(m / 60))}:${pad(m % 60)}`;
}

export function hhmmToMinutes(s) {
  if (!s) return 0;
  const [h, m] = s.split(':').map(Number);
  return (h || 0) * 60 + (m || 0);
}

export function addMinutes(hhmm, minutes) {
  return minutesToHHMM(hhmmToMinutes(hhmm) + minutes);
}

export function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function parseISO(iso) {
  if (!iso) return new Date();
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, (m || 1) - 1, d || 1);
}

/** "2024-07-11" -> "Thu, 11 July 2024" */
export function formatLongDate(iso) {
  const d = parseISO(iso);
  return d.toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

/** "2024-07-11" -> "11 July 2024" */
export function formatDate(iso) {
  return parseISO(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

/** Monday-anchored week range label for the given ISO date. */
export function formatWeekRange(iso) {
  const d = parseISO(iso);
  const day = (d.getDay() + 6) % 7; // Monday = 0
  const start = new Date(d);
  start.setDate(d.getDate() - day);
  const end = new Date(start);
  end.setDate(start.getDate() + 6);
  const sameMonth = start.getMonth() === end.getMonth();
  const startLabel = start.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
  const endLabel = end.toLocaleDateString('en-GB', {
    day: 'numeric',
    month: sameMonth ? undefined : 'short',
    year: 'numeric',
  });
  return `${startLabel} – ${endLabel}`;
}

/** Month + year label for the given ISO date. */
export function formatMonth(iso) {
  return parseISO(iso).toLocaleDateString('en-GB', { month: 'long', year: 'numeric' });
}

/** Current wall-clock time as "HH:MM". */
export function nowHHMM() {
  const d = new Date();
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Current time in minutes since midnight. */
export function nowMinutes() {
  const d = new Date();
  return d.getHours() * 60 + d.getMinutes();
}
