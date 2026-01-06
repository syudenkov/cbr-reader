import axiosInstance from './axiosConfig';

/**
 * TTS Job Data Transfer Object matching API spec
 */
export interface TtsJobDto {
  jobId: number;
  fileId: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number;
  currentPage: number;
  totalPages: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request body for creating a TTS job
 */
export interface CreateTtsJobRequest {
  fileId: number;
}

/**
 * Create a new TTS processing job for a comic file
 *
 * @param fileId - The ID of the comic file to process
 * @returns Promise resolving to the created TTS job
 * @throws 400 if file not found
 * @throws 409 if an active job already exists for this file
 */
export const createTtsJob = async (fileId: number): Promise<TtsJobDto> => {
  const response = await axiosInstance.post<TtsJobDto>('/tts/jobs', { fileId });
  return response.data;
};

/**
 * Get the current status and progress of a TTS job
 *
 * @param jobId - The ID of the TTS job
 * @returns Promise resolving to the job status
 * @throws 404 if job not found
 */
export const getJobStatus = async (jobId: number): Promise<TtsJobDto> => {
  const response = await axiosInstance.get<TtsJobDto>(`/tts/jobs/${jobId}`);
  return response.data;
};

/**
 * Get the audio URL for a specific page of a comic file
 *
 * @param fileId - The ID of the comic file
 * @param pageNumber - The page number (1-indexed)
 * @returns The full URL to fetch the audio file
 */
export const getAudioUrl = (fileId: number, pageNumber: number): string => {
  return `/api/tts/audio/${fileId}/${pageNumber}`;
};
