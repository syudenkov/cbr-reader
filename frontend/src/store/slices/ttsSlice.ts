import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface TtsJob {
  id: string;
  fileId: number;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  progress: number;
  error: string | null;
}

interface TtsResult {
  pageNumber: number;
  audioUrl: string;
  text: string;
}

interface TtsState {
  jobs: Record<number, TtsJob>; // fileId -> job
  results: Record<number, TtsResult[]>; // fileId -> pages
  currentAudio: {
    fileId: number | null;
    pageNumber: number | null;
    playing: boolean;
    currentTime: number;
    duration: number;
  };
}

const initialState: TtsState = {
  jobs: {},
  results: {},
  currentAudio: {
    fileId: null,
    pageNumber: null,
    playing: false,
    currentTime: 0,
    duration: 0,
  },
};

const ttsSlice = createSlice({
  name: 'tts',
  initialState,
  reducers: {
    // Placeholder - will be implemented in I3.T1-I3.T3
    setPlaying: (state, action: PayloadAction<boolean>) => {
      state.currentAudio.playing = action.payload;
    },
    setCurrentTime: (state, action: PayloadAction<number>) => {
      state.currentAudio.currentTime = action.payload;
    },
  },
});

export const { setPlaying, setCurrentTime } = ttsSlice.actions;
export default ttsSlice.reducer;
