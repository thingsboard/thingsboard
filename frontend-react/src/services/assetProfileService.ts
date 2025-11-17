/**
 * Asset Profile Service
 * API service for asset profile management
 */

import { api } from './api'
import { AssetProfile, AssetProfileInfo } from '../types/assetprofile.types'

export const assetProfileService = {
  /**
   * Get all asset profiles
   */
  getAssetProfiles: async (): Promise<AssetProfile[]> => {
    const response = await api.get<AssetProfile[]>('/assetProfiles')
    return response.data
  },

  /**
   * Get asset profile info list (lightweight)
   */
  getAssetProfileInfos: async (): Promise<AssetProfileInfo[]> => {
    const response = await api.get<AssetProfileInfo[]>('/assetProfiles/infos')
    return response.data
  },

  /**
   * Get asset profile by ID
   */
  getAssetProfile: async (id: string): Promise<AssetProfile> => {
    const response = await api.get<AssetProfile>(`/assetProfile/${id}`)
    return response.data
  },

  /**
   * Get asset profiles with pagination
   */
  getAssetProfilesPage: async (params: {
    pageSize: number
    page: number
    textSearch?: string
    sortProperty?: string
    sortOrder?: 'ASC' | 'DESC'
  }): Promise<{
    data: AssetProfile[]
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
      data: AssetProfile[]
      totalPages: number
      totalElements: number
      hasNext: boolean
    }>(`/assetProfiles?${queryParams.toString()}`)
    return response.data
  },

  /**
   * Create new asset profile
   */
  createAssetProfile: async (profile: AssetProfile): Promise<AssetProfile> => {
    const response = await api.post<AssetProfile>('/assetProfile', profile)
    return response.data
  },

  /**
   * Update existing asset profile
   */
  updateAssetProfile: async (profile: AssetProfile): Promise<AssetProfile> => {
    const response = await api.post<AssetProfile>('/assetProfile', profile)
    return response.data
  },

  /**
   * Delete asset profile
   */
  deleteAssetProfile: async (id: string): Promise<void> => {
    await api.delete(`/assetProfile/${id}`)
  },

  /**
   * Set asset profile as default
   */
  setDefaultAssetProfile: async (id: string): Promise<AssetProfile> => {
    const response = await api.post<AssetProfile>(`/assetProfile/${id}/default`)
    return response.data
  },

  /**
   * Get default asset profile
   */
  getDefaultAssetProfile: async (): Promise<AssetProfile | null> => {
    try {
      const response = await api.get<AssetProfile>('/assetProfileInfo/default')
      return response.data
    } catch (error) {
      return null
    }
  },

  /**
   * Export asset profile
   */
  exportAssetProfile: async (id: string): Promise<AssetProfile> => {
    const response = await api.get<AssetProfile>(`/assetProfile/${id}/export`)
    return response.data
  },

  /**
   * Import asset profile
   */
  importAssetProfile: async (profile: AssetProfile): Promise<AssetProfile> => {
    const response = await api.post<AssetProfile>('/assetProfile/import', profile)
    return response.data
  },
}
