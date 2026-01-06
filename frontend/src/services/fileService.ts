import axiosInstance from './axiosConfig';

/**
 * Comic file response from backend API
 */
export interface ComicFile {
  id: number;
  originalFilename: string;
  fileType: string;
  fileSize: number;
  pageCount: number;
  coverImagePath?: string;  // Optional - added in I2.T6
  uploadedBy: number;
  averageRating?: number;    // Optional - added in I2.T6
  createdAt: string;         // ISO 8601 format
}

/**
 * Paginated response from GET /api/files
 */
export interface FilesResponse {
  content: ComicFile[];
  totalPages: number;
  totalElements: number;
  pageNumber: number;
  pageSize: number;
}

/**
 * Fetch paginated list of comic files
 * @param page Page number (0-indexed)
 * @param size Items per page
 * @param sort Sort field and direction (e.g., "createdAt,desc")
 * @returns Paginated files response
 */
export const fetchFiles = async (
  page: number = 0,
  size: number = 20,
  sort: string = 'createdAt,desc'
): Promise<FilesResponse> => {
  const response = await axiosInstance.get<FilesResponse>('/files', {
    params: { page, size, sort }
  });
  return response.data;
};

/**
 * Upload a new comic file (CBZ/CBR)
 * @param file File to upload
 * @param onProgress Progress callback (0-100)
 * @returns Created comic file metadata
 */
export const uploadFile = async (
  file: File,
  onProgress: (percent: number) => void
): Promise<ComicFile> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axiosInstance.post<ComicFile>('/files', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total) {
        const percentCompleted = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );
        onProgress(percentCompleted);
      }
    },
  });

  return response.data;
};

/**
 * Delete a comic file and all related data
 * @param fileId File ID to delete
 * @returns Success message
 */
export const deleteFile = async (fileId: number): Promise<{ message: string }> => {
  const response = await axiosInstance.delete<{ message: string }>(`/files/${fileId}`);
  return response.data;
};
