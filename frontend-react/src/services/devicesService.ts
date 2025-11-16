import apiClient from './apiClient'
import { Device, DeviceCreate, DeviceUpdate } from '@/types/device'

export const getDevices = async (): Promise<Device[]> => {
  const response = await apiClient.get('/devices')
  return response.data
}

export const getDevice = async (deviceId: string): Promise<Device> => {
  const response = await apiClient.get(`/devices/${deviceId}`)
  return response.data
}

export const createDevice = async (device: DeviceCreate): Promise<Device> => {
  const response = await apiClient.post('/devices', device)
  return response.data
}

export const updateDevice = async (deviceId: string, device: DeviceUpdate): Promise<Device> => {
  const response = await apiClient.put(`/devices/${deviceId}`, device)
  return response.data
}

export const deleteDevice = async (deviceId: string): Promise<void> => {
  await apiClient.delete(`/devices/${deviceId}`)
}

export const getDeviceCredentials = async (deviceId: string) => {
  const response = await apiClient.get(`/devices/${deviceId}/credentials`)
  return response.data
}
