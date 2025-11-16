import { createSlice } from '@reduxjs/toolkit'

const tenantsSlice = createSlice({
  name: 'tenants',
  initialState: { tenants: [], loading: false, error: null },
  reducers: {},
})

export default tenantsSlice.reducer
