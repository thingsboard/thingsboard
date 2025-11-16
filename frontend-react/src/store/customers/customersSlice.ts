import { createSlice } from '@reduxjs/toolkit'

const customersSlice = createSlice({
  name: 'customers',
  initialState: { customers: [], loading: false, error: null },
  reducers: {},
})

export default customersSlice.reducer
