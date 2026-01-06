import axios from 'axios';
import axiosInstance from './axiosConfig';

export interface ReadingProgressResponse {
  fileId: number;
  currentPage: number;
  updatedAt: string | null;
}

interface ProgressUpdateRequest {
  currentPage: number;
}

interface ErrorResponse {
  error: string;
  status: number;
  timestamp: string;
}

export const getAllProgress = async (): Promise<ReadingProgressResponse[]> => {
  try {
    const response = await axiosInstance.get('/progress');
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Failed to fetch progress');
    }
    throw new Error('Network error. Please try again.');
  }
};

export const getProgress = async (fileId: number): Promise<ReadingProgressResponse> => {
  try {
    const response = await axiosInstance.get(`/progress/${fileId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Failed to fetch progress');
    }
    throw new Error('Network error. Please try again.');
  }
};

export const updateProgress = async (
  fileId: number,
  currentPage: number
): Promise<ReadingProgressResponse> => {
  try {
    const request: ProgressUpdateRequest = { currentPage };
    const response = await axiosInstance.put(`/progress/${fileId}`, request);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data as ErrorResponse;
      throw new Error(errorData.error || 'Failed to update progress');
    }
    throw new Error('Network error. Please try again.');
  }
};
