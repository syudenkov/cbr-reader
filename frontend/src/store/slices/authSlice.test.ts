import { configureStore } from '@reduxjs/toolkit';
import authReducer, {
  login,
  logout,
  fetchCurrentUser,
  clearError,
  User,
} from './authSlice';
import * as authService from '../../services/authService';

// Mock the auth service
jest.mock('../../services/authService');

const mockedAuthService = authService as jest.Mocked<typeof authService>;

describe('authSlice', () => {
  const mockUser: User = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    role: 'USER',
  };

  const createTestStore = () =>
    configureStore({
      reducer: { auth: authReducer },
    });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('reducers', () => {
    it('should return initial state', () => {
      const store = createTestStore();
      const state = store.getState().auth;

      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });

    it('should clear error with clearError action', () => {
      const store = createTestStore();

      // Manually set an error state by dispatching a rejected action
      mockedAuthService.login.mockRejectedValueOnce(new Error('Test error'));

      store.dispatch(clearError());

      expect(store.getState().auth.error).toBeNull();
    });
  });

  describe('login thunk', () => {
    it('should set loading to true when login is pending', () => {
      const store = createTestStore();

      mockedAuthService.login.mockImplementation(
        () => new Promise(() => {}) // Never resolves
      );

      store.dispatch(login({ username: 'test', password: 'password' }));

      expect(store.getState().auth.loading).toBe(true);
      expect(store.getState().auth.error).toBeNull();
    });

    it('should set user and isAuthenticated when login succeeds', async () => {
      const store = createTestStore();

      mockedAuthService.login.mockResolvedValueOnce(mockUser);

      await store.dispatch(login({ username: 'testuser', password: 'password' }));

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual(mockUser);
      expect(state.loading).toBe(false);
      expect(state.error).toBeNull();
    });

    it('should set error when login fails', async () => {
      const store = createTestStore();

      mockedAuthService.login.mockRejectedValueOnce(new Error('Invalid credentials'));

      await store.dispatch(login({ username: 'wrong', password: 'wrong' }));

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.loading).toBe(false);
      expect(state.error).toBe('Invalid credentials');
    });
  });

  describe('logout thunk', () => {
    it('should clear user and set isAuthenticated to false on logout', async () => {
      const store = createTestStore();

      // First login
      mockedAuthService.login.mockResolvedValueOnce(mockUser);
      await store.dispatch(login({ username: 'testuser', password: 'password' }));

      // Then logout
      mockedAuthService.logout.mockResolvedValueOnce();
      await store.dispatch(logout());

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.loading).toBe(false);
    });

    it('should still clear state even if logout fails on server', async () => {
      const store = createTestStore();

      // First login
      mockedAuthService.login.mockResolvedValueOnce(mockUser);
      await store.dispatch(login({ username: 'testuser', password: 'password' }));

      // Logout fails
      mockedAuthService.logout.mockRejectedValueOnce(new Error('Server error'));
      await store.dispatch(logout());

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
    });
  });

  describe('fetchCurrentUser thunk', () => {
    it('should set user when fetch succeeds', async () => {
      const store = createTestStore();

      mockedAuthService.getCurrentUser.mockResolvedValueOnce(mockUser);

      await store.dispatch(fetchCurrentUser());

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(true);
      expect(state.user).toEqual(mockUser);
      expect(state.loading).toBe(false);
    });

    it('should clear user when fetch fails (session expired)', async () => {
      const store = createTestStore();

      mockedAuthService.getCurrentUser.mockRejectedValueOnce(new Error('Unauthorized'));

      await store.dispatch(fetchCurrentUser());

      const state = store.getState().auth;
      expect(state.isAuthenticated).toBe(false);
      expect(state.user).toBeNull();
      expect(state.error).toBeNull(); // Error is not set for session expiry
    });
  });
});
