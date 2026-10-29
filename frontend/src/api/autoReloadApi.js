import axiosClient from './axiosClient';

export const getAutoReloadSettings = () => axiosClient.get('/auto-reload');
export const updateAutoReloadSettings = (data) => axiosClient.put('/auto-reload', data);