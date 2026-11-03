import axios from 'axios';

const axiosClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token to every request
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401: redirect to login ONLY for non-auth requests
// Auth endpoints (login, transactions/send) need to see 401 to show proper error messages
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url = error.config?.url || '';
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/transactions/send');
      if (!isAuthEndpoint) {
        // 401 on a protected resource = expired/invalid token, redirect to login
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
      }
      // For auth endpoints, let the error reach the catch block
    }
    
    // Attach user-friendly error message to the error object
    if (error.response?.data?.message) {
      error.userMessage = error.response.data.message;
    } else if (error.response?.data?.error) {
      error.userMessage = error.response.data.error;
    } else if (error.message) {
      error.userMessage = error.message;
    } else {
      error.userMessage = 'An unexpected error occurred. Please try again.';
    }
    
    return Promise.reject(error);
  }
);

export default axiosClient;
