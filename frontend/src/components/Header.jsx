import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { useEffect } from 'react';
import './Header.css';

export default function Header() {
  const navigate = useNavigate();
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

  return (
    <header className="app-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-title">P2P Ledger</h1>
        </div>

        <nav className="header-nav">
          <a href="/dashboard" className="nav-link">Dashboard</a>
          <a href="/wallet" className="nav-link">Wallet</a>
          <a href="/send" className="nav-link">Send</a>
          <a href="/transactions" className="nav-link">History</a>
        </nav>

        <div className="header-right">
          <button
            className="bell-icon"
            onClick={() => navigate('/notifications')}
            title="View notifications"
          >
            ??
            {unreadCount > 0 && (
              <span className="unread-badge">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          <div className="user-menu">
            <span className="user-name">{user.name}</span>
            <button className="logout-btn" onClick={handleLogout}>
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}
