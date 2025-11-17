/**
 * Device Profile Service
 * API service for device profile management
 */

import { api } from './api'
import { DeviceProfile, DeviceProfileInfo } from '../types/device.types'

export const deviceProfileService = {
  /**
   * Get all device profiles
   */
  getDeviceProfiles: async (): Promise<DeviceProfile[]> => {
    const response = await api.get<DeviceProfile[]>('/deviceProfiles')
    return response.data
  },

  /**
   * Get device profile info list (lightweight)
   */
  getDeviceProfileInfos: async (): Promise<DeviceProfileInfo[]> => {
    const response = await api.get<DeviceProfileInfo[]>('/deviceProfiles/infos')
    return response.data
  },

  /**
   * Get device profile by ID
   */
  getDeviceProfile: async (id: string): Promise<DeviceProfile> => {
    const response = await api.get<DeviceProfile>(`/deviceProfile/${id}`)
    return response.data
  },

  /**
   * Get device profiles with pagination
   */
  getDeviceProfilesPage: async (params: {
    pageSize: number
    page: number
    textSearch?: string
    sortProperty?: string
    sortOrder?: 'ASC' | 'DESC'
  }): Promise<{
    data: DeviceProfile[]
    totalPages: number
    totalElements: number
    hasNext: boolean
  }> => {
    const queryParams = new URLSearchParams({
      pageSize: params.pageSize.toString(),
      page: params.page.toString(),
    })

    if (params.textSearch) {
      queryParams.append('textSearch', params.textSearch)
    }
    if (params.sortProperty) {
      queryParams.append('sortProperty', params.sortProperty)
    }
    if (params.sortOrder) {
      queryParams.append('sortOrder', params.sortOrder)
    }

    const response = await api.get<{
      data: DeviceProfile[]
      totalPages: number
      totalElements: number
      hasNext: boolean
    }>(`/deviceProfiles?${queryParams.toString()}`)
    return response.data
  },

  /**
   * Create new device profile
   */
  createDeviceProfile: async (profile: DeviceProfile): Promise<DeviceProfile> => {
    const response = await api.post<DeviceProfile>('/deviceProfile', profile)
    return response.data
  },

  /**
   * Update existing device profile
   */
  updateDeviceProfile: async (profile: DeviceProfile): Promise<DeviceProfile> => {
    const response = await api.post<DeviceProfile>('/deviceProfile', profile)
    return response.data
  },

  /**
   * Delete device profile
   */
  deleteDeviceProfile: async (id: string): Promise<void> => {
    await api.delete(`/deviceProfile/${id}`)
  },

  /**
   * Set device profile as default
   */
  setDefaultDeviceProfile: async (id: string): Promise<DeviceProfile> => {
    const response = await api.post<DeviceProfile>(`/deviceProfile/${id}/default`)
    return response.data
  },

  /**
   * Get default device profile
   */
  getDefaultDeviceProfile: async (): Promise<DeviceProfile | null> => {
    try {
      const response = await api.get<DeviceProfile>('/deviceProfileInfo/default')
      return response.data
    } catch (error) {
      return null
    }
  },

  /**
   * Get device profiles by IDs
   */
  getDeviceProfilesByIds: async (ids: string[]): Promise<DeviceProfile[]> => {
    const response = await api.get<DeviceProfile[]>(`/deviceProfiles?ids=${ids.join(',')}`)
    return response.data
  },

  /**
   * Export device profile
   */
  exportDeviceProfile: async (id: string): Promise<DeviceProfile> => {
    const response = await api.get<DeviceProfile>(`/deviceProfile/${id}/export`)
    return response.data
  },

  /**
   * Import device profile
   */
  importDeviceProfile: async (profile: DeviceProfile): Promise<DeviceProfile> => {
    const response = await api.post<DeviceProfile>('/deviceProfile/import', profile)
    return response.data
  },
}
