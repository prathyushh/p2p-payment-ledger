import axiosClient from './axiosClient';
export const sendMoney = (data) => axiosClient.post('/transactions/send', data);
export const getTransactions = (params) => axiosClient.get('/transactions', { params });
export const getMiniStatement = () => axiosClient.get('/transactions/mini-statement');
export const getTransaction = (id) => axiosClient.get('/transactions/' + id);
export const getStats = () => axiosClient.get('/transactions/stats');