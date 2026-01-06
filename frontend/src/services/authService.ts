import axios from 'axios';
import axiosInstance from './axiosConfig';
import { User } from '../store/slices/authSlice';

interface LoginRequest {
  username: string;
  password: string;
}

interface ErrorResponse {
  error: string;
  status: number;
  timestamp: string;
}

export const login = async (credentials: LoginRequest): Promise<User> => {
  try {
    const response = await axiosInstance.post('/auth/login', credentials);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Login failed');
    }
    throw new Error('Network error. Please try again.');
  }
};

export const logout = async (): Promise<void> => {
  try {
    await axiosInstance.post('/auth/logout');
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Logout failed');
    }
    throw new Error('Network error. Please try again.');
  }
};

export const getCurrentUser = async (): Promise<User> => {
  try {
    const response = await axiosInstance.get('/auth/me');
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Failed to fetch user');
    }
    throw new Error('Network error. Please try again.');
  }
};
