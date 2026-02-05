import { configureStore } from '@reduxjs/toolkit';
import filesReducer, {
  fetchFiles,
  uploadFile,
  deleteFile,
  setCurrentFile,
  setUploadProgress,
  clearUploadState,
} from './filesSlice';
import * as fileService from '../../services/fileService';

// Mock the file service
jest.mock('../../services/fileService');

const mockedFileService = fileService as jest.Mocked<typeof fileService>;

describe('filesSlice', () => {
  const mockFile: fileService.ComicFile = {
    id: 1,
    filename: 'test-comic.cbz',
    originalFilename: 'Test Comic.cbz',
    fileSize: 1024000,
    pageCount: 20,
    uploadedAt: '2024-01-15T10:00:00Z',
    uploadedBy: 'testuser',
    coverUrl: '/api/pages/1/cover',
  };

  const createTestStore = () =>
    configureStore({
      reducer: { files: filesReducer },
    });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('reducers', () => {
    it('should return initial state', () => {
      const store = createTestStore();
      const state = store.getState().files;

      expect(state.items).toEqual([]);
      expect(state.status).toBe('idle');
      expect(state.currentFile).toBeNull();
      expect(state.uploadProgress).toBe(0);
      expect(state.uploadStatus).toBe('idle');
    });

    it('should set current file', () => {
      const store = createTestStore();

      store.dispatch(setCurrentFile(mockFile));

      expect(store.getState().files.currentFile).toEqual(mockFile);
    });

    it('should clear current file', () => {
      const store = createTestStore();

      store.dispatch(setCurrentFile(mockFile));
      store.dispatch(setCurrentFile(null));

      expect(store.getState().files.currentFile).toBeNull();
    });

    it('should set upload progress', () => {
      const store = createTestStore();

      store.dispatch(setUploadProgress(50));

      expect(store.getState().files.uploadProgress).toBe(50);
    });

    it('should clear upload state', () => {
      const store = createTestStore();

      store.dispatch(setUploadProgress(75));
      store.dispatch(clearUploadState());

      const state = store.getState().files;
      expect(state.uploadProgress).toBe(0);
      expect(state.uploadStatus).toBe('idle');
      expect(state.uploadError).toBeNull();
    });
  });

  describe('fetchFiles thunk', () => {
    it('should set loading state when fetching', () => {
      const store = createTestStore();

      mockedFileService.fetchFiles.mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      store.dispatch(fetchFiles());

      expect(store.getState().files.status).toBe('loading');
    });

    it('should populate items when fetch succeeds', async () => {
      const store = createTestStore();

      mockedFileService.fetchFiles.mockResolvedValueOnce({
        content: [mockFile],
        totalPages: 1,
        totalElements: 1,
        page: 0,
        size: 20,
      });

      await store.dispatch(fetchFiles());

      const state = store.getState().files;
      expect(state.status).toBe('succeeded');
      expect(state.items).toEqual([mockFile]);
    });

    it('should set error when fetch fails', async () => {
      const store = createTestStore();

      mockedFileService.fetchFiles.mockRejectedValueOnce({
        response: { data: { message: 'Failed to fetch' } },
      });

      await store.dispatch(fetchFiles());

      const state = store.getState().files;
      expect(state.status).toBe('failed');
      expect(state.error).toBe('Failed to fetch');
    });
  });

  describe('uploadFile thunk', () => {
    it('should set uploading state when upload starts', () => {
      const store = createTestStore();

      mockedFileService.uploadFile.mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      const file = new File(['test'], 'test.cbz', { type: 'application/zip' });
      store.dispatch(uploadFile(file));

      expect(store.getState().files.uploadStatus).toBe('uploading');
    });

    it('should add file to items when upload succeeds', async () => {
      const store = createTestStore();

      mockedFileService.uploadFile.mockResolvedValueOnce(mockFile);

      const file = new File(['test'], 'test.cbz', { type: 'application/zip' });
      await store.dispatch(uploadFile(file));

      const state = store.getState().files;
      expect(state.uploadStatus).toBe('succeeded');
      expect(state.uploadProgress).toBe(100);
      expect(state.items).toContainEqual(mockFile);
    });

    it('should set error when upload fails', async () => {
      const store = createTestStore();

      mockedFileService.uploadFile.mockRejectedValueOnce({
        response: { data: { message: 'File too large' } },
      });

      const file = new File(['test'], 'test.cbz', { type: 'application/zip' });
      await store.dispatch(uploadFile(file));

      const state = store.getState().files;
      expect(state.uploadStatus).toBe('failed');
      expect(state.uploadError).toBe('File too large');
    });
  });

  describe('deleteFile thunk', () => {
    it('should remove file from items when delete succeeds', async () => {
      const store = createTestStore();

      // First, populate with files
      mockedFileService.fetchFiles.mockResolvedValueOnce({
        content: [mockFile, { ...mockFile, id: 2, filename: 'other.cbz' }],
        totalPages: 1,
        totalElements: 2,
        page: 0,
        size: 20,
      });
      await store.dispatch(fetchFiles());

      // Then delete one
      mockedFileService.deleteFile.mockResolvedValueOnce();
      await store.dispatch(deleteFile(1));

      const state = store.getState().files;
      expect(state.items).toHaveLength(1);
      expect(state.items.find(f => f.id === 1)).toBeUndefined();
    });
  });
});
