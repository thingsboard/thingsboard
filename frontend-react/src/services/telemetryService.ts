import apiClient from './apiClient'

export const saveTelemetry = async (
  entityType: string,
  entityId: string,
  scope: string,
  data: Record<string, any>
) => {
  const response = await apiClient.post(
    `/telemetry/${entityType}/${entityId}/timeseries/${scope}`,
    data
  )
  return response.data
}

export const getLatestTelemetry = async (
  entityType: string,
  entityId: string,
  keys?: string
) => {
  const params = keys ? { keys } : {}
  const response = await apiClient.get(
    `/telemetry/${entityType}/${entityId}/values/timeseries`,
    { params }
  )
  return response.data
}

export const getTelemetryKeys = async (entityType: string, entityId: string) => {
  const response = await apiClient.get(
    `/telemetry/${entityType}/${entityId}/keys/timeseries`
  )
  return response.data
}

export const saveAttributes = async (
  entityType: string,
  entityId: string,
  scope: string,
  attributes: Record<string, any>
) => {
  const response = await apiClient.post(
    `/telemetry/${entityType}/${entityId}/attributes/${scope}`,
    attributes
  )
  return response.data
}

export const getAttributes = async (
  entityType: string,
  entityId: string,
  keys?: string
) => {
  const params = keys ? { keys } : {}
  const response = await apiClient.get(
    `/telemetry/${entityType}/${entityId}/attributes`,
    { params }
  )
  return response.data
}

export const deleteAttributes = async (
  entityType: string,
  entityId: string,
  scope: string,
  keys: string
) => {
  await apiClient.delete(
    `/telemetry/${entityType}/${entityId}/attributes/${scope}`,
    { params: { keys } }
  )
}
