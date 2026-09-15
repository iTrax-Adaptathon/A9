import { useApp } from '../context/AppContext.jsx';
import {
  LogoMark,
  ChevronLeft,
  ChevronRight,
  Users,
  CalendarCheck,
  Calendar,
  Moon,
} from './Icons.jsx';

const MENU_ITEMS = [
  { key: 'participants', label: 'Participants', icon: Users },
  { key: 'appointments', label: 'Appointments', icon: CalendarCheck },
  { key: 'calendar', label: 'My Calendar', icon: Calendar },
];

export default function Sidebar({
  active = 'calendar',
  onNavigate,
  collapsed = false,
  onToggleCollapse,
}) {
  const { theme, toggleTheme } = useApp();
  const dark = theme === 'dark';

  function renderItem(item) {
    const Icon = item.icon;
    return (
      <li key={item.key}>
        <button
          type="button"
          className={`nav-item${active === item.key ? ' active' : ''}`}
          onClick={() => onNavigate?.(item.key)}
          title={item.label}
        >
          <span className="nav-icon"><Icon size={19} /></span>
          <span className="nav-label">{item.label}</span>
        </button>
      </li>
    );
  }

  return (
    <aside className={`sidebar${collapsed ? ' collapsed' : ''}`}>
      <div className="sidebar-top">
        <div className="brand">
          <span className="brand-mark"><LogoMark size={32} /></span>
          <span className="brand-name">SyncSlot</span>
        </div>
        <button
          type="button"
          className="sidebar-collapse"
          onClick={onToggleCollapse}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-group">
          <div className="nav-group-head">
            <span className="nav-group-label">Menu</span>
          </div>
          <ul className="nav-list">{MENU_ITEMS.map(renderItem)}</ul>
        </div>
      </nav>

      <div className="sidebar-bottom">
        <button type="button" className="theme-row" onClick={toggleTheme}>
          <span className="nav-icon"><Moon size={18} /></span>
          <span className="theme-label">Dark Mode</span>
          <span className={`switch${dark ? ' on' : ''}`}>
            <span className="switch-thumb" />
          </span>
        </button>
      </div>
    </aside>
  );
}
