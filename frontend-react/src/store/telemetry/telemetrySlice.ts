import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { RootState } from '@/store'
import * as telemetryService from '@/services/telemetryService'

interface TelemetryState {
  data: Record<string, any>
  loading: boolean
  error: string | null
}

const initialState: TelemetryState = {
  data: {},
  loading: false,
  error: null,
}

export const fetchTelemetry = createAsyncThunk(
  'telemetry/fetch',
  async ({ entityType, entityId, keys }: { entityType: string; entityId: string; keys?: string }, { rejectWithValue }) => {
    try {
      return await telemetryService.getLatestTelemetry(entityType, entityId, keys)
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.detail || 'Failed to fetch telemetry')
    }
  }
)

const telemetrySlice = createSlice({
  name: 'telemetry',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchTelemetry.pending, (state) => {
        state.loading = true
      })
      .addCase(fetchTelemetry.fulfilled, (state, action) => {
        state.loading = false
        state.data = action.payload
      })
      .addCase(fetchTelemetry.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
  },
})

export const selectTelemetry = (state: RootState) => state.telemetry.data

export default telemetrySlice.reducer
