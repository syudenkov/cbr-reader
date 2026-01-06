import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt: string;
}

interface SystemLog {
  id: number;
  level: string;
  message: string;
  timestamp: string;
}

interface AuditLog {
  id: number;
  userId: number;
  action: string;
  resource: string;
  timestamp: string;
}

interface LlmConfig {
  id: number;
  provider: string;
  model: string;
  apiKey: string;
  enabled: boolean;
}

interface AdminState {
  users: User[];
  logs: SystemLog[];
  auditLogs: AuditLog[];
  llmConfigs: LlmConfig[];
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error: string | null;
}

const initialState: AdminState = {
  users: [],
  logs: [],
  auditLogs: [],
  llmConfigs: [],
  status: 'idle',
  error: null,
};

const adminSlice = createSlice({
  name: 'admin',
  initialState,
  reducers: {
    // Placeholder - will be implemented in I4.T1-I4.T3
    setStatus: (state, action: PayloadAction<'idle' | 'loading' | 'succeeded' | 'failed'>) => {
      state.status = action.payload;
    },
  },
});

export const { setStatus } = adminSlice.actions;
export default adminSlice.reducer;
