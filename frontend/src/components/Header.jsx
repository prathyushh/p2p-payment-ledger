import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { useEffect } from 'react';
import './Header.css';

export default function Header() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const { unreadCount, refreshUnreadCount } = useNotifications();

  // Refresh unread count on mount
  useEffect(() => {
    refreshUnreadCount();
  }, [refreshUnreadCount]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) return null;

  // Check if current page is an admin page
  const isAdminPage = location.pathname.startsWith('/admin');

  return (
    <header className="app-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-title">P2P Ledger</h1>
        </div>

        <nav className="header-nav">
          {/* Show admin link only for admins, unless on admin page */}
          {user.role === 'ADMIN' && !isAdminPage && (
            <a href="/admin/dashboard" className="nav-link admin-link">
              ?? Admin Panel
            </a>
          )}
          
          {/* Show user navigation only if not on admin page */}
          {!isAdminPage && (
            <>
              <a href="/dashboard" className="nav-link">Dashboard</a>
              <a href="/wallet" className="nav-link">Wallet</a>
              <a href="/bank-accounts" className="nav-link">Bank Accounts</a>
              <a href="/auto-reload" className="nav-link">Auto Reload</a>
              <a href="/send" className="nav-link">Send</a>
              <a href="/transactions" className="nav-link">History</a>
            </>
          )}

          {/* Show back to user dashboard link if on admin page */}
          {isAdminPage && (
            <a href="/dashboard" className="nav-link back-link">
              ? Back to Dashboard
            </a>
          )}
        </nav>

        <div className="header-right">
          <button
            className="bell-icon"
            onClick={() => navigate('/notifications')}
            title="View notifications"
          >
            <span style={{ fontSize: '1.5em' }}>{'??'}</span>
            {unreadCount > 0 && (
              <span className="unread-badge">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          <div className="user-menu">
            <span className="user-name">{user.name}</span>
            {user.role === 'ADMIN' && <span className="admin-badge">Admin</span>}
            <button className="logout-btn" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
