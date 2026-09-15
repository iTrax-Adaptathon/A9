const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api';

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const data = await res.json();
      message = data.message || data.error || data.detail || message;
    } catch {
      /* ignore non-JSON error bodies */
    }
    throw new Error(message);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  // participants
  getUsers: () => request('/users'),
  createUser: (name, email) =>
    request('/users', { method: 'POST', body: JSON.stringify({ name, email }) }),

  // availability
  getAvailability: (date) => request(`/availability?date=${date}`),
  createAvailability: (payload) =>
    request('/availability', { method: 'POST', body: JSON.stringify(payload) }),
  updateAvailability: (id, payload) =>
    request(`/availability/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteAvailability: (id) =>
    request(`/availability/${id}`, { method: 'DELETE' }),

  // appointments
  getAppointments: (date) => request(`/appointments?date=${date}`),
  createAppointment: (payload) =>
    request('/appointments', { method: 'POST', body: JSON.stringify(payload) }),
  updateAppointment: (id, payload) =>
    request(`/appointments/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteAppointment: (id) =>
    request(`/appointments/${id}`, { method: 'DELETE' }),

  // scheduling
  findSlots: (payload) =>
    request('/schedule/find-slots', { method: 'POST', body: JSON.stringify(payload) }),
  previewCascade: (payload) =>
    request('/schedule/preview-cascade', { method: 'POST', body: JSON.stringify(payload) }),
  applyCascade: (payload) =>
    request('/schedule/apply-cascade', { method: 'POST', body: JSON.stringify(payload) }),
  checkConflict: (payload) =>
    request('/schedule/check-conflict', { method: 'POST', body: JSON.stringify(payload) }),

  // AI narration (explains a decision the backend already made)
  explainSchedule: (payload) =>
    request('/ai/explain', { method: 'POST', body: JSON.stringify(payload) }),
  summarizeSchedule: (payload) =>
    request('/ai/summary', { method: 'POST', body: JSON.stringify(payload) }),
};

export default api;
