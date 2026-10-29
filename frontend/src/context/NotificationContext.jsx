import { createContext, useContext, useState, useCallback } from 'react';
import axiosClient from '../api/axiosClient';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const [unreadCount, setUnreadCount] = useState(0);

  const refreshUnreadCount = useCallback(async () => {
    try {
      const res = await axiosClient.get('/notifications/unread-count');
      setUnreadCount(res.data.unreadCount ?? 0);
    } catch {
      // Fail silently - not critical
    }
  }, []);

  return (
    <NotificationContext.Provider value={{ unreadCount, refreshUnreadCount, setUnreadCount }}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotifications must be used within NotificationProvider');
  return ctx;
}