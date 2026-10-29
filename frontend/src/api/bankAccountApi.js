import axiosClient from './axiosClient';
export const getBankAccounts = () => axiosClient.get('/bank-accounts');
export const createBankAccount = (d) => axiosClient.post('/bank-accounts', d);
export const addDemoFunds = (id, d) => axiosClient.post('/bank-accounts/' + id + '/demo-funds', d);
export const deactivateBankAccount = (id) => axiosClient.post('/bank-accounts/' + id + '/deactivate');