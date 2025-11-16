import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { RootState } from '@/store'
import * as devicesService from '@/services/devicesService'
import { Device, DeviceCreate } from '@/types/device'

interface DevicesState {
  devices: Device[]
  currentDevice: Device | null
  loading: boolean
  error: string | null
}

const initialState: DevicesState = {
  devices: [],
  currentDevice: null,
  loading: false,
  error: null,
}

export const fetchDevices = createAsyncThunk(
  'devices/fetchDevices',
  async (_, { rejectWithValue }) => {
    try {
      return await devicesService.getDevices()
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.detail || 'Failed to fetch devices')
    }
  }
)

export const createDevice = createAsyncThunk(
  'devices/createDevice',
  async (device: DeviceCreate, { rejectWithValue }) => {
    try {
      return await devicesService.createDevice(device)
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.detail || 'Failed to create device')
    }
  }
)

const devicesSlice = createSlice({
  name: 'devices',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchDevices.pending, (state) => {
        state.loading = true
      })
      .addCase(fetchDevices.fulfilled, (state, action) => {
        state.loading = false
        state.devices = action.payload
      })
      .addCase(fetchDevices.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload as string
      })
      .addCase(createDevice.fulfilled, (state, action) => {
        state.devices.push(action.payload)
      })
  },
})

export const selectDevices = (state: RootState) => state.devices.devices
export const selectDevicesLoading = (state: RootState) => state.devices.loading

export default devicesSlice.reducer
