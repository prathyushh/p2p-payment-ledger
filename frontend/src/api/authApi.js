import axiosClient from './axiosClient';
export const register = (data) => axiosClient.post('/auth/register', data);
export const login = (data) => axiosClient.post('/auth/login', data);
export const getMe = () => axiosClient.get('/auth/me');