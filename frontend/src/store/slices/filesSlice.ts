import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface ComicFile {
  id: number;
  title: string;
  uploadDate: string;
  rating: number;
  // Additional fields will be added in future iterations
}

interface FilesState {
  items: ComicFile[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
  currentFile: ComicFile | null;
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
  filters: {
    search: '',
    sortBy: 'title',
    sortOrder: 'asc',
  },
};

const filesSlice = createSlice({
  name: 'files',
  initialState,
  reducers: {
    // Placeholder - will be implemented in I2.T3
    setCurrentFile: (state, action: PayloadAction<ComicFile | null>) => {
      state.currentFile = action.payload;
    },
  },
});

export const { setCurrentFile } = filesSlice.actions;
export default filesSlice.reducer;
