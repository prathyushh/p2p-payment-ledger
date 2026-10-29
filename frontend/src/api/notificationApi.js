import axiosClient from './axiosClient';
export const getNotifications = (params) => axiosClient.get('/notifications', { params });
export const getUnreadCount = () => axiosClient.get('/notifications/unread-count');
export const markAsRead = (id) => axiosClient.put('/notifications/' + id + '/read');
export const markAllAsRead = () => axiosClient.put('/notifications/read-all');