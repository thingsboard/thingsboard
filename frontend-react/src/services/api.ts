/**
 * API Service
 * Centralized API client for all backend communication
 */

import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

// API Base URL - can be configured via environment variable
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000/api'

// Create axios instance with default config
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - Add auth token to all requests
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - Handle token refresh and errors
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // Handle 401 Unauthorized - attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          })

          const { token, refreshToken: newRefreshToken } = response.data
          localStorage.setItem('token', token)
          localStorage.setItem('refreshToken', newRefreshToken)

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${token}`
          return apiClient(originalRequest)
        }
      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

// Generic API methods
export const api = {
  // GET request
  get: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> => {
    return apiClient.get<T>(url, config)
  },

  // POST request
  post: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.post<T>(url, data, config)
  },

  // PUT request
  put: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.put<T>(url, data, config)
  },

  // PATCH request
  patch: <T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> => {
    return apiClient.patch<T>(url, data, config)
  },

  // DELETE request
  delete: <T = any>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> => {
    return apiClient.delete<T>(url, config)
  },
}

// Authentication API
export const authApi = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),

  logout: () => api.post('/auth/logout'),

  refresh: (refreshToken: string) =>
    api.post('/auth/refresh', { refreshToken }),

  getCurrentUser: () => api.get('/auth/user'),
}

// Devices API
export const devicesApi = {
  getAll: (params?: any) => api.get('/devices', { params }),

  getById: (id: string) => api.get(`/devices/${id}`),

  create: (data: any) => api.post('/devices', data),

  update: (id: string, data: any) => api.put(`/devices/${id}`, data),

  delete: (id: string) => api.delete(`/devices/${id}`),

  assignToCustomer: (id: string, customerId: string) =>
    api.post(`/devices/${id}/customer/${customerId}`),

  unassignFromCustomer: (id: string) => api.delete(`/devices/${id}/customer`),
}

// Assets API
export const assetsApi = {
  getAll: (params?: any) => api.get('/assets', { params }),

  getById: (id: string) => api.get(`/assets/${id}`),

  create: (data: any) => api.post('/assets', data),

  update: (id: string, data: any) => api.put(`/assets/${id}`, data),

  delete: (id: string) => api.delete(`/assets/${id}`),

  assignToCustomer: (id: string, customerId: string) =>
    api.post(`/assets/${id}/customer/${customerId}`),
}

// Customers API
export const customersApi = {
  getAll: (params?: any) => api.get('/customers', { params }),

  getById: (id: string) => api.get(`/customers/${id}`),

  create: (data: any) => api.post('/customers', data),

  update: (id: string, data: any) => api.put(`/customers/${id}`, data),

  delete: (id: string) => api.delete(`/customers/${id}`),
}

// Users API
export const usersApi = {
  getAll: (params?: any) => api.get('/users', { params }),

  getById: (id: string) => api.get(`/users/${id}`),

  create: (data: any) => api.post('/users', data),

  update: (id: string, data: any) => api.put(`/users/${id}`, data),

  delete: (id: string) => api.delete(`/users/${id}`),

  activateUser: (id: string) => api.post(`/users/${id}/activate`),

  sendActivationEmail: (id: string) => api.post(`/users/${id}/sendActivationEmail`),
}

// Tenants API (SYS_ADMIN only)
export const tenantsApi = {
  getAll: (params?: any) => api.get('/tenants', { params }),

  getById: (id: string) => api.get(`/tenants/${id}`),

  create: (data: any) => api.post('/tenants', data),

  update: (id: string, data: any) => api.put(`/tenants/${id}`, data),

  delete: (id: string) => api.delete(`/tenants/${id}`),
}

// Alarms API
export const alarmsApi = {
  getAll: (params?: any) => api.get('/alarms', { params }),

  getById: (id: string) => api.get(`/alarms/${id}`),

  acknowledge: (id: string) => api.post(`/alarms/${id}/ack`),

  clear: (id: string) => api.post(`/alarms/${id}/clear`),

  delete: (id: string) => api.delete(`/alarms/${id}`),
}

// Rule Chains API
export const ruleChainsApi = {
  getAll: (params?: any) => api.get('/rule-chains', { params }),

  getById: (id: string) => api.get(`/rule-chains/${id}`),

  create: (data: any) => api.post('/rule-chains', data),

  update: (id: string, data: any) => api.put(`/rule-chains/${id}`, data),

  delete: (id: string) => api.delete(`/rule-chains/${id}`),

  setRoot: (id: string) => api.post(`/rule-chains/${id}/root`),

  export: (id: string) => api.get(`/rule-chains/${id}/export`),

  import: (data: any) => api.post('/rule-chains/import', data),
}

// Gateways API
export const gatewaysApi = {
  getAll: (params?: any) => api.get('/gateways', { params }),

  getById: (id: string) => api.get(`/gateways/${id}`),

  create: (data: any) => api.post('/gateways', data),

  update: (id: string, data: any) => api.put(`/gateways/${id}`, data),

  delete: (id: string) => api.delete(`/gateways/${id}`),

  getConnectors: (id: string) => api.get(`/gateways/${id}/connectors`),

  updateConnectors: (id: string, data: any) =>
    api.put(`/gateways/${id}/connectors`, data),

  restart: (id: string) => api.post(`/gateways/${id}/restart`),
}

// Telemetry API
export const telemetryApi = {
  getLatest: (entityType: string, entityId: string, keys?: string[]) => {
    const params = keys ? { keys: keys.join(',') } : undefined
    return api.get(`/telemetry/${entityType}/${entityId}/values/latest`, { params })
  },

  getTimeseries: (
    entityType: string,
    entityId: string,
    keys: string[],
    startTs: number,
    endTs: number,
    interval?: number,
    limit?: number
  ) => {
    const params = {
      keys: keys.join(','),
      startTs,
      endTs,
      ...(interval && { interval }),
      ...(limit && { limit }),
    }
    return api.get(`/telemetry/${entityType}/${entityId}/values/timeseries`, { params })
  },

  saveTelemetry: (entityType: string, entityId: string, data: any) =>
    api.post(`/telemetry/${entityType}/${entityId}`, data),

  deleteKeys: (entityType: string, entityId: string, keys: string[]) =>
    api.delete(`/telemetry/${entityType}/${entityId}`, {
      data: { keys },
    }),
}

// Attributes API
export const attributesApi = {
  getByScope: (
    scope: 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE',
    entityType: string,
    entityId: string,
    keys?: string[]
  ) => {
    const params = keys ? { keys: keys.join(',') } : undefined
    return api.get(`/attributes/${scope}/${entityType}/${entityId}`, { params })
  },

  saveAttributes: (
    scope: 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE',
    entityType: string,
    entityId: string,
    data: any
  ) => api.post(`/attributes/${scope}/${entityType}/${entityId}`, data),

  deleteAttributes: (
    scope: 'SERVER_SCOPE' | 'SHARED_SCOPE' | 'CLIENT_SCOPE',
    entityType: string,
    entityId: string,
    keys: string[]
  ) =>
    api.delete(`/attributes/${scope}/${entityType}/${entityId}`, {
      data: { keys },
    }),
}

// Widget Bundles API
export const widgetBundlesApi = {
  getAll: (params?: any) => api.get('/widget-bundles', { params }),

  getById: (id: string) => api.get(`/widget-bundles/${id}`),

  create: (data: any) => api.post('/widget-bundles', data),

  update: (id: string, data: any) => api.put(`/widget-bundles/${id}`, data),

  delete: (id: string) => api.delete(`/widget-bundles/${id}`),
}

// Audit Logs API
export const auditLogsApi = {
  getAll: (params?: any) => api.get('/audit-logs', { params }),

  getById: (id: string) => api.get(`/audit-logs/${id}`),

  export: (params?: any) => api.get('/audit-logs/export', { params }),
}

// Dashboards API
export const dashboardsApi = {
  getAll: (params?: any) => api.get('/dashboards', { params }),

  getById: (id: string) => api.get(`/dashboards/${id}`),

  create: (data: any) => api.post('/dashboards', data),

  update: (id: string, data: any) => api.put(`/dashboards/${id}`, data),

  delete: (id: string) => api.delete(`/dashboards/${id}`),

  assignToCustomer: (id: string, customerId: string) =>
    api.post(`/dashboards/${id}/customer/${customerId}`),
}

export default api
