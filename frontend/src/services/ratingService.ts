import axiosInstance from './axiosConfig';

/**
 * Rating response from backend API
 */
export interface RatingResponse {
  id: number;
  userId: number;
  fileId: number;
  rating: number;
  createdAt: string;
}

/**
 * Average rating response from backend API
 */
export interface AverageRatingResponse {
  fileId: number;
  averageRating: number | null;
  totalRatings: number;
}

/**
 * Submit or update a rating for a comic file
 * @param fileId File ID to rate
 * @param rating Rating value (1-5)
 * @returns Rating response
 */
export const submitRating = async (
  fileId: number,
  rating: number
): Promise<RatingResponse> => {
  const response = await axiosInstance.post<RatingResponse>(
    `/ratings/${fileId}`,
    { rating }
  );
  return response.data;
};

/**
 * Get average rating for a comic file
 * @param fileId File ID
 * @returns Average rating response
 */
export const getAverageRating = async (
  fileId: number
): Promise<AverageRatingResponse> => {
  const response = await axiosInstance.get<AverageRatingResponse>(
    `/ratings/${fileId}/average`
  );
  return response.data;
};

/**
 * Get current user's rating for a comic file
 * @param fileId File ID
 * @returns Rating response or null if not rated
 */
export const getUserRating = async (
  fileId: number
): Promise<RatingResponse | null> => {
  const response = await axiosInstance.get<RatingResponse | null>(
    `/ratings/${fileId}`
  );
  return response.data;
};
