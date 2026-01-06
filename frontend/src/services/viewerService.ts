import axiosInstance from './axiosConfig';

export interface ComicFile {
  id: number;
  originalFilename: string;
  filePath: string;
  fileSize: number;
  pageCount: number;
  archiveType: string;
  uploadedAt: string;
}

/**
 * Fetch comic file metadata by ID
 */
export const fetchFileMetadata = async (fileId: number): Promise<ComicFile> => {
  const response = await axiosInstance.get<ComicFile>(`/files/${fileId}`);
  return response.data;
};

/**
 * Load a page image from the API and return a blob URL
 * CRITICAL: Uses responseType: 'blob' to get binary image data
 * Page numbers are 1-indexed (page 1 = first page)
 */
export const loadPageImage = async (fileId: number, pageNumber: number): Promise<string> => {
  // Fetch image as blob
  const response = await axiosInstance.get(
    `/files/${fileId}/page/${pageNumber}`,
    { responseType: 'blob' }  // CRITICAL: Must use 'blob' response type
  );

  // Create object URL (memory-managed URL for blob)
  const imageUrl = URL.createObjectURL(response.data);

  return imageUrl;  // Returns "blob:http://localhost:5173/abc-123-def"
};
