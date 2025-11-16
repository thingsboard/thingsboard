import apiClient from './apiClient'
import { LoginRequest, TokenResponse, UserInfo } from '@/types/auth'

export const login = async (credentials: LoginRequest): Promise<TokenResponse> => {
  const response = await apiClient.post('/auth/login', credentials)
  return response.data
}

export const getCurrentUser = async (): Promise<UserInfo> => {
  const response = await apiClient.get('/auth/user')
  return response.data
}

export const logout = async (): Promise<void> => {
  await apiClient.post('/auth/logout')
}

export const refreshToken = async (refreshToken: string): Promise<TokenResponse> => {
  const response = await apiClient.post('/auth/token/refresh', {
    refresh_token: refreshToken,
  })
  return response.data
}
