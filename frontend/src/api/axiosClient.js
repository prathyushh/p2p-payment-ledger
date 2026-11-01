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

// Handle 401: only redirect if NOT on the login page
// (login page needs to see 401 to show "Invalid credentials" message)
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const isLoginRequest = error.config?.url?.includes('/auth/login');
      if (!isLoginRequest) {
        // 401 on a non-login request = expired token, redirect to login
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
      }
      // For login requests, let the error propagate to the catch block
    }
    return Promise.reject(error);
  }
);

export default axiosClient;
