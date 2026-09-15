/**
 * Single source of truth for SyncSlot's visual language.
 *
 * Everything the UI paints with is defined here: the neutral tokens (injected
 * as CSS custom properties on <html>), the two theme modes, and the semantic
 * category palettes that map SyncSlot's domain (priority / status) onto the
 * reference design's soft, rounded card language.
 *
 * No component should hardcode a hex value — import from here instead.
 */

/* ------------------------------------------------------------------ *
 * Layout + typography tokens (shared by every theme)
 * ------------------------------------------------------------------ */
const SHARED = {
  '--font-sans':
    "'Inter', 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, sans-serif",

  // Shell dimensions
  '--sidebar-w': '260px',
  '--sidebar-w-collapsed': '76px',
  '--gutter-w': '200px',

  // Sidebar — intentionally dark in every theme
  '--sidebar-bg': '#191b1f',
  '--sidebar-border': '#2a2d33',
  '--sidebar-text': '#e6e8ec',
  '--sidebar-text-muted': '#98a0ad',
  '--sidebar-text-faint': '#6a7280',
  '--sidebar-hover': 'rgba(255, 255, 255, 0.07)',
  '--sidebar-surface': '#24272e',
  '--sidebar-active-bg': 'rgba(111, 157, 255, 0.18)',
  '--sidebar-active-text': '#a9c4ff',
  '--sidebar-accent': '#6f9dff',

  // Spacing scale
  '--space-1': '4px',
  '--space-2': '8px',
  '--space-3': '12px',
  '--space-4': '16px',
  '--space-5': '20px',
  '--space-6': '24px',
  '--space-7': '32px',
  '--space-8': '40px',

  // Radii — soft-rounded everywhere, no sharp corners
  '--radius-xs': '6px',
  '--radius-sm': '10px',
  '--radius-md': '14px',
  '--radius-lg': '20px',
  '--radius-pill': '999px',
};

/* ------------------------------------------------------------------ *
 * Light theme (default) — matches the Delidoc reference
 * ------------------------------------------------------------------ */
const LIGHT = {
  '--bg-page': '#eef1f6',
  '--surface': '#ffffff',
  '--surface-2': '#f6f8fb',
  '--surface-3': '#eef1f6',

  '--text': '#26303f',
  '--text-strong': '#0f1729',
  '--text-muted': '#7b8699',
  '--text-faint': '#9aa4b5',
  '--text-onaccent': '#ffffff',

  '--border': '#e7ebf1',
  '--border-strong': '#dbe1ea',
  '--grid-line': '#eef1f6',

  '--accent': '#2f6bff',
  '--accent-strong': '#1e56e6',
  '--accent-soft': '#e9efff',
  '--accent-soft-2': '#dbe7ff',

  '--green': '#16a34a',
  '--green-soft': '#e6f7ec',
  '--green-border': 'rgba(22, 163, 74, 0.35)',
  '--amber': '#e9a23b',
  '--amber-soft': '#fdf3e2',
  '--red': '#e5484d',
  '--red-soft': '#fde8ea',
  '--red-border': 'rgba(229, 72, 77, 0.3)',

  '--hatch': 'rgba(148, 163, 184, 0.35)',
  '--hatch-bg': 'rgba(148, 163, 184, 0.08)',
  '--scrim': 'rgba(15, 23, 41, 0.42)',

  '--shadow-card': '0 1px 2px rgba(16, 24, 40, 0.05), 0 1px 3px rgba(16, 24, 40, 0.06)',
  '--shadow-pop': '0 8px 24px rgba(16, 24, 40, 0.10)',
  '--shadow-modal': '0 24px 60px rgba(16, 24, 40, 0.22)',
};

/* ------------------------------------------------------------------ *
 * Dark theme — same tokens, re-pointed
 * ------------------------------------------------------------------ */
const DARK = {
  '--bg-page': '#0e1420',
  '--surface': '#161e2c',
  '--surface-2': '#1b2536',
  '--surface-3': '#121a28',

  '--text': '#dbe3f0',
  '--text-strong': '#f5f8fd',
  '--text-muted': '#93a0b5',
  '--text-faint': '#6b7689',
  '--text-onaccent': '#ffffff',

  '--border': '#263042',
  '--border-strong': '#313d52',
  '--grid-line': '#222c3d',

  '--accent': '#4f8cff',
  '--accent-strong': '#6f9dff',
  '--accent-soft': '#1b2b4d',
  '--accent-soft-2': '#21396b',

  '--green': '#34d399',
  '--green-soft': '#12291f',
  '--green-border': 'rgba(52, 211, 153, 0.32)',
  '--amber': '#f2b74d',
  '--amber-soft': '#2c2412',
  '--red': '#f87171',
  '--red-soft': '#331a1c',
  '--red-border': 'rgba(248, 113, 113, 0.32)',

  '--hatch': 'rgba(148, 163, 184, 0.28)',
  '--hatch-bg': 'rgba(148, 163, 184, 0.06)',
  '--scrim': 'rgba(0, 0, 0, 0.6)',

  '--shadow-card': '0 1px 2px rgba(0, 0, 0, 0.35)',
  '--shadow-pop': '0 8px 24px rgba(0, 0, 0, 0.45)',
  '--shadow-modal': '0 24px 60px rgba(0, 0, 0, 0.55)',
};

/* ------------------------------------------------------------------ *
 * Category palette — priority drives each appointment's accent + tint.
 *   LOW = teal, MEDIUM = blue, HIGH = amber, URGENT = pink/red
 * `hex` feeds inline text/bar colours, `rgb` feeds translucent tints so the
 * same value works on both light and dark surfaces.
 * ------------------------------------------------------------------ */
export const PRIORITY = {
  LOW: { label: 'Low', hex: '#12b3a6', rgb: '18, 179, 166' },
  MEDIUM: { label: 'Medium', hex: '#2f6bff', rgb: '47, 107, 255' },
  HIGH: { label: 'High', hex: '#e9a23b', rgb: '233, 162, 59' },
  URGENT: { label: 'Urgent', hex: '#ef4d6b', rgb: '239, 77, 107' },
};

export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export function priorityOf(priority) {
  return PRIORITY[priority] || PRIORITY.MEDIUM;
}

/* ------------------------------------------------------------------ *
 * Status palette — a channel independent of priority. A scheduler-flagged
 * conflict always wins the pill, regardless of the stored status.
 * ------------------------------------------------------------------ */
export const STATUS = {
  CONFIRMED: { label: 'Upcoming', hex: '#2f6bff', rgb: '47, 107, 255' },
  DONE: { label: 'Done', hex: '#16a34a', rgb: '22, 163, 74' },
  PENDING: { label: 'Pending', hex: '#e9a23b', rgb: '233, 162, 59' },
  RESCHEDULED: { label: 'Moved', hex: '#7c5cff', rgb: '124, 92, 255' },
  CANCELLED: { label: 'Cancelled', hex: '#8b96ad', rgb: '139, 150, 173' },
  CONFLICT: { label: 'Conflict', hex: '#e5484d', rgb: '229, 72, 77' },
};

export function statusOf(status, conflicting = false, done = false) {
  if (conflicting) return STATUS.CONFLICT;
  if (done && status === 'CONFIRMED') return STATUS.DONE;
  return STATUS[status] || STATUS.PENDING;
}

/* Rotating palette for content that has no category of its own (e.g. ranked
 * slot recommendations) — keeps the reference's soft multi-colour rhythm. */
export const CATEGORY_PALETTE = [
  { hex: '#2f6bff', rgb: '47, 107, 255' },
  { hex: '#12b3a6', rgb: '18, 179, 166' },
  { hex: '#e9a23b', rgb: '233, 162, 59' },
  { hex: '#ef4d6b', rgb: '239, 77, 107' },
  { hex: '#7c5cff', rgb: '124, 92, 255' },
];

export function categoryColor(index) {
  return CATEGORY_PALETTE[index % CATEGORY_PALETTE.length];
}

/* ------------------------------------------------------------------ *
 * Initials-based avatars (no photos exist in the data model)
 * ------------------------------------------------------------------ */
const AVATAR_COLORS = [
  '#2f6bff',
  '#12b3a6',
  '#e9a23b',
  '#ef4d6b',
  '#7c5cff',
  '#0ea5e9',
  '#f97316',
  '#10b981',
];

export function avatarColor(seed) {
  const key = String(seed ?? '');
  const sum = key.split('').reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  return AVATAR_COLORS[Math.abs(sum) % AVATAR_COLORS.length];
}

export function initials(name) {
  if (!name) return '?';
  const parts = String(name).trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/* ------------------------------------------------------------------ *
 * Theme installation — pushes tokens onto <html> as CSS custom properties.
 * ------------------------------------------------------------------ */
export function tokensFor(mode = 'light') {
  return { ...SHARED, ...(mode === 'dark' ? DARK : LIGHT) };
}

export function installTheme(mode = 'light') {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  const vars = tokensFor(mode);
  for (const [key, value] of Object.entries(vars)) {
    root.style.setProperty(key, value);
  }
  root.setAttribute('data-theme', mode);
}

export default {
  PRIORITY,
  PRIORITIES,
  STATUS,
  CATEGORY_PALETTE,
  priorityOf,
  statusOf,
  categoryColor,
  avatarColor,
  initials,
  installTheme,
  tokensFor,
};
