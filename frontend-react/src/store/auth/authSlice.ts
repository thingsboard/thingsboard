import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { RootState } from '@/store'
import * as authService from '@/services/authService'
import { LoginRequest, UserInfo, TokenResponse } from '@/types/auth'

interface AuthState {
  user: UserInfo | null
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  loading: boolean
  error: string | null
}

const initialState: AuthState = {
  user: null,
  token: localStorage.getItem('token'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: !!localStorage.getItem('token'),
  loading: false,
  error: null,
}

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: LoginRequest, { rejectWithValue }) => {
    try {
      const response = await authService.login(credentials)
      localStorage.setItem('token', response.token)
      localStorage.setItem('refreshToken', response.refresh_token)

      // Get user info
      const userInfo = await authService.getCurrentUser()
      return { ...response, user: userInfo }
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.detail || 'Login failed')
    }
  }
)

export const getCurrentUser = createAsyncThunk(
  'auth/getCurrentUser',
  async (_, { rejectWithValue }) => {
    try {
      return await authService.getCurrentUser()
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.detail || 'Failed to get user info')
    }
  }
)

export const logout = createAsyncThunk('auth/logout', async () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  await authService.logout()
})

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null
    },
    demoLogin: (state) => {
      // Demo login - no backend call
      const demoToken = 'demo-token-' + Date.now()
      const demoUser: UserInfo = {
        id: 'demo-user-123',
        email: 'demo@payvar.io',
        first_name: 'Demo',
        last_name: 'User',
        authority: 'TENANT_ADMIN',
        tenant_id: 'demo-tenant-123',
      }

      state.loading = false
      state.isAuthenticated = true
      state.token = demoToken
      state.refreshToken = 'demo-refresh-token'
      state.user = demoUser
      state.error = null

      // Store in localStorage
      localStorage.setItem('token', demoToken)
      localStorage.setItem('refreshToken', 'demo-refresh-token')
      localStorage.setItem('demoMode', 'true')
    },
  },
  extraReducers: (builder) => {
    builder
      // Login
      .addCase(login.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false
        state.isAuthenticated = true
        state.token = action.payload.token
        state.refreshToken = action.payload.refresh_token
        state.user = action.payload.user
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      // Get current user
      .addCase(getCurrentUser.fulfilled, (state, action) => {
        state.user = action.payload
      })
      // Logout
      .addCase(logout.fulfilled, (state) => {
        state.user = null
        state.token = null
        state.refreshToken = null
        state.isAuthenticated = false
      })
  },
})

export const { clearError, demoLogin } = authSlice.actions

export const selectAuth = (state: RootState) => state.auth
export const selectIsAuthenticated = (state: RootState) => state.auth.isAuthenticated
export const selectCurrentUser = (state: RootState) => state.auth.user

export default authSlice.reducer
