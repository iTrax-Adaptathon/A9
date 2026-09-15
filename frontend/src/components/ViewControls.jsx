import { useRef, useState } from 'react';
import { formatLongDate, formatWeekRange, formatMonth } from '../utils/time.js';
import { PRIORITY, STATUS, PRIORITIES } from '../theme.js';
import {
  Calendar,
  ChevronDown,
  GridView,
  ListView,
  Filter,
  Plus,
} from './Icons.jsx';

const RANGES = [
  { key: 'today', label: 'Today' },
  { key: 'week', label: 'This Week' },
  { key: 'month', label: 'This Month' },
];

const STATUS_KEYS = ['CONFIRMED', 'DONE', 'PENDING', 'RESCHEDULED', 'CANCELLED', 'CONFLICT'];

export default function ViewControls({
  range,
  onRangeChange,
  view,
  onViewChange,
  date,
  onDateChange,
  onToday,
  count,
  onCreate,
  filters,
  onFiltersChange,
}) {
  const dateInput = useRef(null);
  const [filterOpen, setFilterOpen] = useState(false);

  const dateLabel =
    range === 'week'
      ? formatWeekRange(date)
      : range === 'month'
        ? formatMonth(date)
        : formatLongDate(date);

  function openPicker() {
    const el = dateInput.current;
    if (!el) return;
    if (typeof el.showPicker === 'function') el.showPicker();
    else el.click();
  }

  function toggleFilter(kind, value) {
    const active = filters[kind].includes(value);
    const next = active
      ? filters[kind].filter((v) => v !== value)
      : [...filters[kind], value];
    onFiltersChange({ ...filters, [kind]: next });
  }

  const activeFilterCount = filters.priorities.length + filters.statuses.length;

  return (
    <div className="view-controls">
      <div className="tabs">
        {RANGES.map((r) => (
          <button
            key={r.key}
            type="button"
            className={`tab${range === r.key ? ' active' : ''}`}
            onClick={() => onRangeChange(r.key)}
          >
            {r.label}
          </button>
        ))}
      </div>

      <div className="controls-row">
        <button type="button" className="date-select" onClick={openPicker}>
          <span className="lead-icon"><Calendar size={17} /></span>
          <span>{dateLabel}</span>
          <span className="chev"><ChevronDown size={15} /></span>
          <input
            ref={dateInput}
            type="date"
            value={date}
            onChange={(e) => onDateChange(e.target.value)}
            tabIndex={-1}
            aria-label="Select date"
          />
        </button>

        <button type="button" className="btn btn-outline" onClick={onToday}>
          Today
        </button>

        <div className="controls-right">
          <span className="count-readout">
            <strong>{count}</strong> total appointments
          </span>

          <div className="segmented">
            <button
              type="button"
              className={`seg-btn${view === 'grid' ? ' active' : ''}`}
              onClick={() => onViewChange('grid')}
              title="Grid view"
            >
              <GridView size={17} />
            </button>
            <button
              type="button"
              className={`seg-btn${view === 'list' ? ' active' : ''}`}
              onClick={() => onViewChange('list')}
              title="List view"
            >
              <ListView size={17} />
            </button>
          </div>

          <div className="popover-wrap">
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setFilterOpen((v) => !v)}
            >
              <Filter size={16} />
              Filters{activeFilterCount ? ` (${activeFilterCount})` : ''}
            </button>

            {filterOpen && (
              <div className="popover">
                <div className="popover-title">Priority</div>
                {PRIORITIES.map((p) => (
                  <label key={p} className="filter-option">
                    <input
                      type="checkbox"
                      checked={filters.priorities.includes(p)}
                      onChange={() => toggleFilter('priorities', p)}
                    />
                    <span
                      className="filter-swatch"
                      style={{ background: PRIORITY[p].hex }}
                    />
                    {PRIORITY[p].label}
                  </label>
                ))}

                <div className="popover-title" style={{ marginTop: '12px' }}>Status</div>
                {STATUS_KEYS.map((s) => (
                  <label key={s} className="filter-option">
                    <input
                      type="checkbox"
                      checked={filters.statuses.includes(s)}
                      onChange={() => toggleFilter('statuses', s)}
                    />
                    <span
                      className="filter-swatch"
                      style={{ background: STATUS[s].hex }}
                    />
                    {STATUS[s].label}
                  </label>
                ))}
              </div>
            )}
          </div>

          <button type="button" className="btn btn-primary" onClick={onCreate}>
            <Plus size={16} />
            New Appointment
          </button>
        </div>
      </div>
    </div>
  );
}
