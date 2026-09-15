import { useMemo, useState } from 'react';
import { useApp } from '../context/AppContext.jsx';
import { api } from '../services/api.js';
import { todayISO, toHHMM } from '../utils/time.js';
import { applyFilters } from '../utils/schedule.js';

import Sidebar from '../components/Sidebar.jsx';
import Header from '../components/Header.jsx';
import ViewControls from '../components/ViewControls.jsx';
import ParticipantPanel from '../components/ParticipantPanel.jsx';
import AvailabilityPanel from '../components/AvailabilityPanel.jsx';
import AppointmentForm from '../components/AppointmentForm.jsx';
import CalendarTimeline from '../components/CalendarTimeline.jsx';
import AppointmentList from '../components/AppointmentList.jsx';
import SlotRecommendations from '../components/SlotRecommendations.jsx';
import ConflictPanel from '../components/ConflictPanel.jsx';
import ImpactPreview from '../components/ImpactPreview.jsx';
import ConfirmationModal from '../components/ConfirmationModal.jsx';
import Drawer from '../components/Drawer.jsx';

const EMPTY_FILTERS = { priorities: [], statuses: [] };

export default function Dashboard() {
  const { users, appointments, selectedDate, setSelectedDate, refresh } = useApp();

  const [view, setView] = useState('grid');
  const [range, setRange] = useState('today');
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [collapsed, setCollapsed] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [participantsOpen, setParticipantsOpen] = useState(false);

  const [recommendations, setRecommendations] = useState([]);
  const [findRequest, setFindRequest] = useState(null);
  const [conflicts, setConflicts] = useState([]);
  const [preview, setPreview] = useState(null);
  const [lastRequest, setLastRequest] = useState(null);
  const [finding, setFinding] = useState(false);
  const [applying, setApplying] = useState(false);
  const [editingAppt, setEditingAppt] = useState(null);
  const [deletingAppt, setDeletingAppt] = useState(null);
  const [savingAppt, setSavingAppt] = useState(false);
  const [summaryOpen, setSummaryOpen] = useState(false);
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState('');
  const [error, setError] = useState('');

  const visibleCount = applyFilters(appointments, filters).length;

  function participantNames(ids) {
    return (ids || [])
      .map((id) => users.find((u) => u.id === id)?.name)
      .filter(Boolean);
  }

  // Stable payloads so the AI panel doesn't refetch on every render.
  const aiSlotPayload = useMemo(() => {
    const top = recommendations[0];
    if (!top || !findRequest) return null;
    return {
      meetingTitle: findRequest.title,
      participants: participantNames(findRequest.participantIds),
      durationMinutes: findRequest.duration,
      priority: findRequest.priority,
      selectedTime: `${toHHMM(top.startTime)}–${toHHMM(top.endTime)}`,
      fallbackExplanation: top.reason,
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [recommendations, findRequest, users]);

  const aiPreviewPayload = useMemo(() => {
    if (!preview || !lastRequest) return null;
    return {
      meetingTitle: lastRequest.title,
      participants: participantNames(lastRequest.participantIds),
      durationMinutes: lastRequest.duration,
      priority: lastRequest.priority,
      selectedTime: `${toHHMM(preview.requestStartTime)}–${toHHMM(preview.requestEndTime)}`,
      conflict: preview.impact?.[0]?.title ?? preview.conflictsCreated?.[0]?.message,
      conflictPriority: preview.impact?.[0]?.priority,
      proposedChange: preview.impact?.[0]
        ? `Move ${preview.impact[0].title} to ${toHHMM(preview.impact[0].newStartTime)}–${toHHMM(preview.impact[0].newEndTime)}`
        : undefined,
      newConflicts: preview.conflictsCreated?.length ?? 0,
      fallbackExplanation: preview.reason,
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [preview, lastRequest, users]);

  async function handleFindSlots(payload) {
    setError('');
    setFinding(true);
    try {
      const result = await api.findSlots(payload);
      setRecommendations(result.slots || []);
      setFindRequest(payload);
      setConflicts([]);
    } catch (e) {
      setError(e.message);
    } finally {
      setFinding(false);
    }
  }

  async function handleBook(payload) {
    setError('');
    try {
      const result = await api.previewCascade(payload);
      setLastRequest(payload);
      setConflicts(result.conflictsCreated || []);
      setPreview(result);
    } catch (e) {
      setError(e.message);
    }
  }

  async function handleApply() {
    if (!preview || !lastRequest) return;
    setApplying(true);
    try {
      await api.applyCascade({ appointment: lastRequest, moves: preview.impact });
      setPreview(null);
      setLastRequest(null);
      setRecommendations([]);
      setConflicts([]);
      setFormOpen(false);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setApplying(false);
    }
  }

  function handleCancel() {
    setPreview(null);
  }

  async function handleSaveAppointment(payload) {
    if (!editingAppt) return;
    setError('');
    setSavingAppt(true);
    try {
      await api.updateAppointment(editingAppt.id, payload);
      setEditingAppt(null);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setSavingAppt(false);
    }
  }

  async function handleDeleteAppointment() {
    if (!deletingAppt) return;
    setError('');
    setSavingAppt(true);
    try {
      await api.deleteAppointment(deletingAppt.id);
      setDeletingAppt(null);
      await refresh();
    } catch (e) {
      setError(e.message);
    } finally {
      setSavingAppt(false);
    }
  }

  async function handleAiSummary() {
    setSummaryOpen(true);
    setSummary(null);
    setSummaryError('');
    setSummaryLoading(true);
    try {
      const result = await api.summarizeSchedule({
        date: selectedDate,
        range: range === 'today' ? 'day' : range,
      });
      setSummary(result);
    } catch (e) {
      setSummaryError(e.message);
    } finally {
      setSummaryLoading(false);
    }
  }

  function handleRange(next) {
    setRange(next);
  }

  function handleToday() {
    setRange('today');
    setSelectedDate(todayISO());
  }

  function handleNavigate(key) {
    if (key === 'participants') setParticipantsOpen(true);
    else if (key === 'appointments') setFormOpen(true);
    else if (key === 'calendar') setView('grid');
  }

  return (
    <div className="app-shell">
      <Sidebar
        active="calendar"
        collapsed={collapsed}
        onToggleCollapse={() => setCollapsed((v) => !v)}
        onNavigate={handleNavigate}
      />

      <div className="main">
        <Header title="My Calendar" onAiSummary={handleAiSummary} />

        <ViewControls
          range={range}
          onRangeChange={handleRange}
          view={view}
          onViewChange={setView}
          date={selectedDate}
          onDateChange={setSelectedDate}
          onToday={handleToday}
          count={visibleCount}
          onCreate={() => setFormOpen(true)}
          filters={filters}
          onFiltersChange={setFilters}
        />

        {error && <div className="error-banner">{error}</div>}

        <div className="main-scroll">
          {view === 'grid' ? (
            <CalendarTimeline
              filters={filters}
              onEditParticipant={() => setParticipantsOpen(true)}
              onEditAppointment={setEditingAppt}
              onDeleteAppointment={setDeletingAppt}
            />
          ) : (
            <AppointmentList filters={filters} />
          )}
        </div>
      </div>

      <Drawer
        open={formOpen}
        title="New Appointment"
        onClose={() => setFormOpen(false)}
        wide
      >
        <AppointmentForm onFindSlots={handleFindSlots} onBook={handleBook} />
        <div style={{ marginTop: '20px' }}>
          {finding ? (
            <div className="empty-note">Finding best slots…</div>
          ) : (
            <SlotRecommendations
              key={aiSlotPayload?.selectedTime ?? 'none'}
              slots={recommendations}
              aiPayload={aiSlotPayload}
            />
          )}
        </div>
      </Drawer>

      <Drawer
        open={participantsOpen}
        title="Participants & Availability"
        onClose={() => setParticipantsOpen(false)}
      >
        <ParticipantPanel />
        <AvailabilityPanel />
      </Drawer>

      <Drawer
        open={!!editingAppt}
        title="Edit Appointment"
        onClose={() => setEditingAppt(null)}
        wide
      >
        {editingAppt && (
          <AppointmentForm
            key={editingAppt.id}
            appointment={editingAppt}
            onSave={handleSaveAppointment}
            onCancel={() => setEditingAppt(null)}
          />
        )}
      </Drawer>

      <ConfirmationModal
        open={!!deletingAppt}
        title="Delete appointment"
        onClose={() => setDeletingAppt(null)}
      >
        {deletingAppt && (
          <div>
            <p className="confirm-text">
              Delete <strong>{deletingAppt.title}</strong> (
              {toHHMM(deletingAppt.startTime)}–{toHHMM(deletingAppt.endTime)})? This
              cannot be undone.
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setDeletingAppt(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDeleteAppointment}
                disabled={savingAppt}
              >
                {savingAppt && <span className="spinner" />}
                {savingAppt ? 'Deleting…' : 'Delete'}
              </button>
            </div>
          </div>
        )}
      </ConfirmationModal>

      <ConfirmationModal
        open={summaryOpen}
        title="AI Summary"
        onClose={() => setSummaryOpen(false)}
      >
        {summaryLoading && (
          <p className="ai-explain-pending">
            <span className="spinner spinner-muted" />
            Summarising your schedule…
          </p>
        )}

        {!summaryLoading && summaryError && (
          <p className="muted">Could not generate a summary: {summaryError}</p>
        )}

        {!summaryLoading && summary && (
          <div>
            <span className="ai-summary-meta">
              {summary.period} · {summary.meetingCount}{' '}
              {summary.meetingCount === 1 ? 'meeting' : 'meetings'}
            </span>
            <p className="ai-explain-text ai-summary-text">{summary.summary}</p>
            {!summary.aiGenerated && (
              <span className="ai-explain-note">Built-in summary — AI unavailable</span>
            )}
          </div>
        )}

        <div className="modal-actions">
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => setSummaryOpen(false)}
          >
            Close
          </button>
        </div>
      </ConfirmationModal>

      <ConfirmationModal
        open={!!preview}
        title="Review schedule changes"
        onClose={handleCancel}
      >
        {preview && (
          <>
            <ImpactPreview
              preview={preview}
              onApply={handleApply}
              onCancel={handleCancel}
              applying={applying}
              aiPayload={aiPreviewPayload}
            />
            {conflicts.length > 0 && (
              <div style={{ marginTop: '20px' }}>
                <ConflictPanel conflicts={conflicts} />
              </div>
            )}
          </>
        )}
      </ConfirmationModal>
    </div>
  );
}
