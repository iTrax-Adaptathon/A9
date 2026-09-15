import { useApp } from '../context/AppContext.jsx';
import { avatarColor, initials } from '../theme.js';
import { ChevronDown } from './Icons.jsx';

function currentUser(users) {
  const first = users && users.length ? users[0] : null;
  if (first) {
    const handle = first.email
      ? `@${first.email.split('@')[0]}`
      : `@${String(first.name || 'user').toLowerCase().replace(/\s+/g, '')}`;
    return { name: first.name, handle, seed: first.id, role: 'Manager' };
  }
  return { name: 'SyncSlot User', handle: '@syncslot', seed: 0, role: 'Manager' };
}

export default function Header({ title = 'My Calendar', onAiSummary }) {
  const { users } = useApp();

  const user = currentUser(users);

  return (
    <header className="header">
      <h1 className="page-title">{title}</h1>

      <div className="header-right">
        <button
          type="button"
          className="btn btn-outline ai-summary-btn"
          onClick={onAiSummary}
          title="Summarise the schedule with AI"
        >
          AI Summary
        </button>

        <button type="button" className="user-chip">
          <span className="avatar" style={{ background: avatarColor(user.seed) }}>
            {initials(user.name)}
          </span>
          <span className="user-meta">
            <span className="user-name">
              {user.name}
              <span className="user-role">{user.role}</span>
            </span>
            <span className="user-handle">{user.handle}</span>
          </span>
          <span className="chev"><ChevronDown size={16} /></span>
        </button>
      </div>
    </header>
  );
}
