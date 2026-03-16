import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="sidebar-logo">S</div>
        <span className="sidebar-title">SEO Dashboard</span>
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/" end className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
          </svg>
          Dashboard
        </NavLink>

        <NavLink to="/analysis" className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/>
          </svg>
          AI Analysis
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user">{user?.email || 'User'}</div>
        <button className="sidebar-logout" onClick={handleLogout}>
          Logout
        </button>
      </div>
    </aside>
  );
}
