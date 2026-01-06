import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface ViewerState {
  mode: 'page' | 'scroll';
  currentPage: number;
  totalPages: number;
  zoomLevel: number;
  isFullscreen: boolean;
  preloadedPages: Record<number, string>; // pageNumber -> imageUrl
}

const initialState: ViewerState = {
  mode: 'page',
  currentPage: 1,
  totalPages: 0,
  zoomLevel: 100,
  isFullscreen: false,
  preloadedPages: {},
};

const viewerSlice = createSlice({
  name: 'viewer',
  initialState,
  reducers: {
    // Placeholder - will be implemented in I2.T4
    setViewMode: (state, action: PayloadAction<'page' | 'scroll'>) => {
      state.mode = action.payload;
    },
    setCurrentPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
    setZoomLevel: (state, action: PayloadAction<number>) => {
      state.zoomLevel = action.payload;
    },
    toggleFullscreen: (state) => {
      state.isFullscreen = !state.isFullscreen;
    },
  },
});

export const { setViewMode, setCurrentPage, setZoomLevel, toggleFullscreen } = viewerSlice.actions;
export default viewerSlice.reducer;
