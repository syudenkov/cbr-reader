import axios from 'axios';

const axiosInstance = axios.create({
  baseURL: '/api',  // Relative URL, Vite proxy handles forwarding to backend
  withCredentials: true,  // CRITICAL: Include session cookies in requests
  headers: {
    'Content-Type': 'application/json',
  },
});

export default axiosInstance;
