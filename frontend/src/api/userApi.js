import axiosClient from './axiosClient';

// Search for recipients (autocomplete)
export const searchRecipients = (query) => axiosClient.get('/users/search', { params: { query } });
