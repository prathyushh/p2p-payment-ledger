import axiosClient from './axiosClient';
export const getWallet = () => axiosClient.get('/wallet');
export const deactivateWallet = () => axiosClient.post('/wallet/deactivate');
export const reactivateWallet = () => axiosClient.post('/wallet/reactivate');
export const topUp = (data) => axiosClient.post('/wallet/top-up', data);