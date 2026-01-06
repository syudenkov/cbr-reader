import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import * as viewerService from '../../services/viewerService';
import type { ComicFile } from '../../services/viewerService';
import type { RootState } from '../store';

interface ViewerState {
  // Current comic being viewed
  currentFile: ComicFile | null;

  // Page navigation
  mode: 'page' | 'scroll';
  currentPage: number;
  totalPages: number;

  // View settings
  zoomLevel: number;
  isFullscreen: boolean;

  // Preloading
  preloadedPages: Record<number, string>; // pageNumber -> imageUrl

  // Loading states
  pageLoadingStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  fileLoadingStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: ViewerState = {
  currentFile: null,
  mode: 'page',
  currentPage: 1,
  totalPages: 0,
  zoomLevel: 100,
  isFullscreen: false,
  preloadedPages: {},
  pageLoadingStatus: 'idle',
  fileLoadingStatus: 'idle',
  error: null,
};

// Async thunks
export const fetchFileMetadata = createAsyncThunk(
  'viewer/fetchFileMetadata',
  async (fileId: number, { rejectWithValue }) => {
    try {
      const file = await viewerService.fetchFileMetadata(fileId);
      return file;
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to load comic file');
    }
  }
);

export const preloadPage = createAsyncThunk(
  'viewer/preloadPage',
  async ({ fileId, pageNumber }: { fileId: number; pageNumber: number }, { getState, rejectWithValue }) => {
    try {
      const state = getState() as RootState;
      const { preloadedPages } = state.viewer;

      // Skip if already preloaded
      if (preloadedPages[pageNumber]) {
        return { pageNumber, imageUrl: preloadedPages[pageNumber] };
      }

      // Load image blob
      const imageUrl = await viewerService.loadPageImage(fileId, pageNumber);

      return { pageNumber, imageUrl };
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to load page');
    }
  }
);

const viewerSlice = createSlice({
  name: 'viewer',
  initialState,
  reducers: {
    setViewMode: (state, action: PayloadAction<'page' | 'scroll'>) => {
      state.mode = action.payload;
    },
    setCurrentPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setTotalPages: (state, action: PayloadAction<number>) => {
      state.totalPages = action.payload;
    },
    setZoomLevel: (state, action: PayloadAction<number>) => {
      state.zoomLevel = action.payload;
    },
    setFullscreen: (state, action: PayloadAction<boolean>) => {
      state.isFullscreen = action.payload;
    },
    toggleFullscreen: (state) => {
      state.isFullscreen = !state.isFullscreen;
    },
    clearPreloadedPages: (state) => {
      state.preloadedPages = {};
    },
    resetViewer: () => {
      return initialState;
    },
  },
  extraReducers: (builder) => {
    builder
      // fetchFileMetadata
      .addCase(fetchFileMetadata.pending, (state) => {
        state.fileLoadingStatus = 'loading';
        state.error = null;
      })
      .addCase(fetchFileMetadata.fulfilled, (state, action) => {
        state.fileLoadingStatus = 'succeeded';
        state.currentFile = action.payload;
        state.totalPages = action.payload.pageCount;
        state.currentPage = 1;
        state.error = null;
      })
      .addCase(fetchFileMetadata.rejected, (state, action) => {
        state.fileLoadingStatus = 'failed';
        state.error = action.payload as string;
      })
      // preloadPage
      .addCase(preloadPage.pending, (state) => {
        state.pageLoadingStatus = 'loading';
      })
      .addCase(preloadPage.fulfilled, (state, action) => {
        state.pageLoadingStatus = 'succeeded';
        const { pageNumber, imageUrl } = action.payload;
        state.preloadedPages[pageNumber] = imageUrl;
      })
      .addCase(preloadPage.rejected, (state, action) => {
        state.pageLoadingStatus = 'failed';
        state.error = action.payload as string;
      });
  },
});

export const {
  setViewMode,
  setCurrentPage,
  setTotalPages,
  setZoomLevel,
  setFullscreen,
  toggleFullscreen,
  clearPreloadedPages,
  resetViewer,
} = viewerSlice.actions;
export default viewerSlice.reducer;
