import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import * as fileService from '../../services/fileService';

// Re-export ComicFile type from service
export type { ComicFile } from '../../services/fileService';

interface FilesState {
  items: fileService.ComicFile[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
  currentFile: fileService.ComicFile | null;
  uploadProgress: number;  // 0-100
  uploadStatus: 'idle' | 'uploading' | 'succeeded' | 'failed';
  uploadError: string | null;
  filters: {
    search: string;
    sortBy: 'title' | 'uploadDate' | 'rating';
    sortOrder: 'asc' | 'desc';
  };
}

const initialState: FilesState = {
  items: [],
  status: 'idle',
  error: null,
  currentFile: null,
  uploadProgress: 0,
  uploadStatus: 'idle',
  uploadError: null,
  filters: {
    search: '',
    sortBy: 'title',
    sortOrder: 'asc',
  },
};

// Async Thunks

/**
 * Fetch paginated list of comic files
 */
export const fetchFiles = createAsyncThunk(
  'files/fetchFiles',
  async (_, { rejectWithValue }) => {
    try {
      const response = await fileService.fetchFiles();
      return response.content;
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to fetch files';
      return rejectWithValue(message);
    }
  }
);

/**
 * Upload a new comic file with progress tracking
 */
export const uploadFile = createAsyncThunk(
  'files/uploadFile',
  async (file: File, { dispatch, rejectWithValue }) => {
    try {
      const response = await fileService.uploadFile(file, (percent) => {
        dispatch(setUploadProgress(percent));
      });
      return response;
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to upload file';
      return rejectWithValue(message);
    }
  }
);

/**
 * Delete a comic file
 */
export const deleteFile = createAsyncThunk(
  'files/deleteFile',
  async (fileId: number, { rejectWithValue }) => {
    try {
      await fileService.deleteFile(fileId);
      return fileId;
    } catch (error) {
      const message = (error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to delete file';
      return rejectWithValue(message);
    }
  }
);

const filesSlice = createSlice({
  name: 'files',
  initialState,
  reducers: {
    setCurrentFile: (state, action: PayloadAction<fileService.ComicFile | null>) => {
      state.currentFile = action.payload;
    },
    setUploadProgress: (state, action: PayloadAction<number>) => {
      state.uploadProgress = action.payload;
    },
    clearUploadState: (state) => {
      state.uploadProgress = 0;
      state.uploadStatus = 'idle';
      state.uploadError = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch Files
    builder.addCase(fetchFiles.pending, (state) => {
      state.status = 'loading';
      state.error = null;
    });
    builder.addCase(fetchFiles.fulfilled, (state, action) => {
      state.status = 'succeeded';
      state.items = action.payload;
    });
    builder.addCase(fetchFiles.rejected, (state, action) => {
      state.status = 'failed';
      state.error = action.payload as string;
    });

    // Upload File
    builder.addCase(uploadFile.pending, (state) => {
      state.uploadStatus = 'uploading';
      state.uploadError = null;
      state.uploadProgress = 0;
    });
    builder.addCase(uploadFile.fulfilled, (state, action) => {
      state.uploadStatus = 'succeeded';
      state.uploadProgress = 100;
      // Add new file to the beginning of the list
      state.items.unshift(action.payload);
    });
    builder.addCase(uploadFile.rejected, (state, action) => {
      state.uploadStatus = 'failed';
      state.uploadError = action.payload as string;
      state.uploadProgress = 0;
    });

    // Delete File
    builder.addCase(deleteFile.fulfilled, (state, action) => {
      // Remove deleted file from list
      state.items = state.items.filter(file => file.id !== action.payload);
    });
  },
});

export const { setCurrentFile, setUploadProgress, clearUploadState } = filesSlice.actions;
export default filesSlice.reducer;
