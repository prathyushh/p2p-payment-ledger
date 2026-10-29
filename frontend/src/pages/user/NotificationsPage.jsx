import { useState, useEffect } from 'react';
import { useNotifications } from '../../context/NotificationContext';
import * as notificationApi from '../../api/notificationApi';
import './UserPages.css';
import './NotificationsPage.css';

export default function NotificationsPage() {
  const { refreshUnreadCount } = useNotifications();
  
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [marking, setMarking] = useState(null);

  const pageSize = 15;

  // Fetch notifications
  const fetchNotifications = async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const response = await notificationApi.getNotifications({
        page,
        size: pageSize,
      });

      if (response.data && response.data.content) {
        setNotifications(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
        setCurrentPage(page);
      }
    } catch (err) {
      setError('Failed to load notifications.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Mark single notification as read
  const handleMarkAsRead = async (notificationId, isRead) => {
    if (isRead) return;

    setMarking(notificationId);
    try {
      await notificationApi.markAsRead(notificationId);
      
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === notificationId ? { ...n, isRead: true } : n
        )
      );
      
      refreshUnreadCount();
    } catch (err) {
      console.error('Failed to mark as read:', err);
    } finally {
      setMarking(null);
    }
  };

  // Mark all as read
  const handleMarkAllAsRead = async () => {
    setMarking('all');
    try {
      await notificationApi.markAllAsRead();
      
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true }))
      );
      
      refreshUnreadCount();
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    } finally {
      setMarking(null);
    }
  };

  // Initial load
  useEffect(() => {
    fetchNotifications(0);
  }, []);

  // Handle page change
  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchNotifications(newPage);
    }
  };

  const getNotificationIcon = (type) => {
    const icons = {
      'TRANSACTION_INITIATED': '??',
      'TRANSACTION_RECEIVED': '?',
      'TRANSACTION_SETTLED': '?',
      'TRANSACTION_FAILED': '?',
      'WALLET_STATUS_CHANGED': '??',
      'DAILY_LIMIT_REACHED': '?',
      'FRAUD_ALERT': '??',
      'AUTO_RELOAD_TRIGGERED': '??',
      'SETTLEMENT_COMPLETED': '??',
      'ACCOUNT_LOCKED': '??',
      'ACCOUNT_UNLOCKED': '??',
      'ADMIN_ACTION': '??',
    };
    return icons[type] || '??';
  };

  if (loading && notifications.length === 0) {
    return (
      <div className="user-page notifications-page">
        <div className="page-header">
          <h1>Notifications</h1>
        </div>
        <div className="loading">Loading notifications...</div>
      </div>
    );
  }

  return (
    <div className="user-page notifications-page">
      <div className="page-header">
        <h1>Notifications</h1>
        <p>You have {totalElements} notification{totalElements !== 1 ? 's' : ''}</p>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Mark all as read button */}
      {notifications.some((n) => !n.isRead) && (
        <div className="actions-bar">
          <button
            className="btn btn-secondary"
            onClick={handleMarkAllAsRead}
            disabled={marking === 'all'}
          >
            {marking === 'all' ? 'Marking...' : 'Mark all as read'}
          </button>
        </div>
      )}

      {/* Notifications list */}
      {notifications.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">??</div>
          <h3>No Notifications</h3>
          <p>You are all caught up!</p>
        </div>
      ) : (
        <>
          <div className="notifications-list">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
              >
                <div className="notification-icon">
                  {getNotificationIcon(notification.type)}
                </div>
                <div className="notification-content">
                  <div className="notification-header">
                    <h3 className="notification-title">{notification.title}</h3>
                    <span className="notification-type">{notification.type}</span>
                  </div>
                  <p className="notification-message">{notification.message}</p>
                  <div className="notification-footer">
                    <span className="notification-time">
                      {new Date(notification.createdAt).toLocaleString()}
                    </span>
                    {notification.relatedTransactionId && (
                      <span className="transaction-link">
                        TXN #{notification.relatedTransactionId}
                      </span>
                    )}
                  </div>
                </div>
                {!notification.isRead && (
                  <button
                    className="mark-read-btn"
                    onClick={() => handleMarkAsRead(notification.id, notification.isRead)}
                    disabled={marking === notification.id}
                    title="Mark as read"
                  >
                    {marking === notification.id ? '...' : '?'}
                  </button>
                )}
              </div>
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="pagination">
              <button
                className="btn btn-secondary"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                Previous
              </button>
              <span className="page-info">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                className="btn btn-secondary"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
