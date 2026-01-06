import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import * as ttsService from '../../services/ttsService';

interface TtsJob {
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

interface TtsState {
  jobs: Record<number, TtsJob>; // fileId -> job
  activeJob: TtsJob | null; // Currently active/monitoring job

  // Audio playback state
  currentAudio: {
    fileId: number | null;
    pageNumber: number | null;
    playing: boolean;
    volume: number;
    currentTime: number;
    duration: number;
  };

  // Loading states
  jobLoadingStatus: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: TtsState = {
  jobs: {},
  activeJob: null,
  currentAudio: {
    fileId: null,
    pageNumber: null,
    playing: false,
    volume: 100,
    currentTime: 0,
    duration: 0,
  },
  jobLoadingStatus: 'idle',
  error: null,
};

// Async thunks

/**
 * Create a new TTS job for a comic file
 */
export const createTtsJob = createAsyncThunk(
  'tts/createJob',
  async (fileId: number, { rejectWithValue }) => {
    try {
      const job = await ttsService.createTtsJob(fileId);
      return job;
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string }; status?: number } };

      // Handle 409 Conflict (job already exists)
      if (axiosError.response?.status === 409) {
        return rejectWithValue('An active TTS job already exists for this file');
      }

      return rejectWithValue(axiosError.response?.data?.message || 'Failed to create TTS job');
    }
  }
);

/**
 * Poll the status of an active TTS job
 */
export const pollJobStatus = createAsyncThunk(
  'tts/pollJobStatus',
  async (jobId: number, { rejectWithValue }) => {
    try {
      const job = await ttsService.getJobStatus(jobId);
      return job;
    } catch (error) {
      const axiosError = error as { response?: { data?: { message?: string } } };
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to poll job status');
    }
  }
);

const ttsSlice = createSlice({
  name: 'tts',
  initialState,
  reducers: {
    // Audio playback controls
    setPlaying: (state, action: PayloadAction<boolean>) => {
      state.currentAudio.playing = action.payload;
    },
    setVolume: (state, action: PayloadAction<number>) => {
      state.currentAudio.volume = action.payload;
    },
    setCurrentTime: (state, action: PayloadAction<number>) => {
      state.currentAudio.currentTime = action.payload;
    },
    setDuration: (state, action: PayloadAction<number>) => {
      state.currentAudio.duration = action.payload;
    },
    setCurrentAudio: (state, action: PayloadAction<{ fileId: number; pageNumber: number }>) => {
      state.currentAudio.fileId = action.payload.fileId;
      state.currentAudio.pageNumber = action.payload.pageNumber;
      state.currentAudio.playing = false;
      state.currentAudio.currentTime = 0;
      state.currentAudio.duration = 0;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // createTtsJob
      .addCase(createTtsJob.pending, (state) => {
        state.jobLoadingStatus = 'loading';
        state.error = null;
      })
      .addCase(createTtsJob.fulfilled, (state, action) => {
        state.jobLoadingStatus = 'succeeded';
        const job = action.payload;
        state.jobs[job.fileId] = job;
        state.activeJob = job;
        state.error = null;
      })
      .addCase(createTtsJob.rejected, (state, action) => {
        state.jobLoadingStatus = 'failed';
        state.error = action.payload as string;
      })
      // pollJobStatus
      .addCase(pollJobStatus.fulfilled, (state, action) => {
        const job = action.payload;
        state.jobs[job.fileId] = job;

        // Update activeJob if it's the same job
        if (state.activeJob?.jobId === job.jobId) {
          state.activeJob = job;
        }

        state.error = null;
      })
      .addCase(pollJobStatus.rejected, (state, action) => {
        // Don't change loading status on poll failure (silent fail)
        state.error = action.payload as string;
      });
  },
});

export const {
  setPlaying,
  setVolume,
  setCurrentTime,
  setDuration,
  setCurrentAudio,
  clearError,
} = ttsSlice.actions;

export default ttsSlice.reducer;
