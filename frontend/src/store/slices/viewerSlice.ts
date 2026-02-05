import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import * as viewerService from '../../services/viewerService';
import * as progressService from '../../services/progressService';
import type { ComicFile } from '../../services/viewerService';
import type { RootState } from '../store';

// Local storage keys for persisting viewer preferences
const STORAGE_KEYS = {
  ZOOM_LEVEL: 'viewer_zoom_level',
  VIEW_MODE: 'viewer_mode',
};

// Load persisted values from localStorage
const loadPersistedZoom = (): number => {
  try {
    const saved = localStorage.getItem(STORAGE_KEYS.ZOOM_LEVEL);
    if (saved) {
      const zoom = parseInt(saved, 10);
      if (zoom >= 50 && zoom <= 200) return zoom;
    }
  } catch {
    // localStorage not available
  }
  return 100;
};

const loadPersistedMode = (): 'page' | 'scroll' => {
  try {
    const saved = localStorage.getItem(STORAGE_KEYS.VIEW_MODE);
    if (saved === 'scroll') return 'scroll';
  } catch {
    // localStorage not available
  }
  return 'page';
};

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
  progressLoadingStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: ViewerState = {
  currentFile: null,
  mode: loadPersistedMode(),
  currentPage: 1,
  totalPages: 0,
  zoomLevel: loadPersistedZoom(),
  isFullscreen: false,
  preloadedPages: {},
  pageLoadingStatus: 'idle',
  fileLoadingStatus: 'idle',
  progressLoadingStatus: 'idle',
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

export const fetchReadingProgress = createAsyncThunk(
  'viewer/fetchReadingProgress',
  async (fileId: number, { rejectWithValue }) => {
    try {
      const progress = await progressService.getProgress(fileId);
      return progress;
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to load reading progress');
    }
  }
);

export const updateReadingProgress = createAsyncThunk(
  'viewer/updateReadingProgress',
  async ({ fileId, currentPage }: { fileId: number; currentPage: number }, { rejectWithValue }) => {
    try {
      const progress = await progressService.updateProgress(fileId, currentPage);
      return progress;
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to update reading progress');
    }
  }
);

const viewerSlice = createSlice({
  name: 'viewer',
  initialState,
  reducers: {
    setViewMode: (state, action: PayloadAction<'page' | 'scroll'>) => {
      state.mode = action.payload;
      // Persist to localStorage
      try {
        localStorage.setItem(STORAGE_KEYS.VIEW_MODE, action.payload);
      } catch {
        // localStorage not available
      }
    },
    setCurrentPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setTotalPages: (state, action: PayloadAction<number>) => {
      state.totalPages = action.payload;
    },
    setZoomLevel: (state, action: PayloadAction<number>) => {
      state.zoomLevel = action.payload;
      // Persist to localStorage
      try {
        localStorage.setItem(STORAGE_KEYS.ZOOM_LEVEL, action.payload.toString());
      } catch {
        // localStorage not available
      }
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
    resetViewer: (state) => {
      // Preserve user preferences (zoom, mode) when resetting
      const preservedZoom = state.zoomLevel;
      const preservedMode = state.mode;
      return {
        ...initialState,
        zoomLevel: preservedZoom,
        mode: preservedMode,
      };
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
      })
      // fetchReadingProgress
      .addCase(fetchReadingProgress.pending, (state) => {
        state.progressLoadingStatus = 'loading';
      })
      .addCase(fetchReadingProgress.fulfilled, (state, action) => {
        state.progressLoadingStatus = 'succeeded';
        // Restore the last read page
        state.currentPage = action.payload.currentPage;
      })
      .addCase(fetchReadingProgress.rejected, (state) => {
        state.progressLoadingStatus = 'failed';
        // Don't set error for progress fetch failure (not critical)
      })
      // updateReadingProgress (silent - no loading state changes)
      .addCase(updateReadingProgress.fulfilled, () => {
        // Progress update succeeded silently
      })
      .addCase(updateReadingProgress.rejected, () => {
        // Progress update failed silently (not critical)
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
