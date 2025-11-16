import { configureStore } from '@reduxjs/toolkit'
import authReducer from './auth/authSlice'
import devicesReducer from './devices/devicesSlice'
import tenantsReducer from './tenants/tenantsSlice'
import customersReducer from './customers/customersSlice'
import telemetryReducer from './telemetry/telemetrySlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    devices: devicesReducer,
    tenants: tenantsReducer,
    customers: customersReducer,
    telemetry: telemetryReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
    }),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
