export interface Device {
  id: string
  tenant_id: string
  customer_id?: string
  name: string
  type: string
  label?: string
  device_profile_id: string
  additional_info?: Record<string, any>
  created_time: number
}

export interface DeviceCreate {
  name: string
  type: string
  label?: string
  device_profile_id: string
  customer_id?: string
  additional_info?: Record<string, any>
}

export interface DeviceUpdate {
  name?: string
  type?: string
  label?: string
  device_profile_id?: string
  customer_id?: string
  additional_info?: Record<string, any>
}

export interface DeviceCredentials {
  id: string
  device_id: string
  credentials_type: string
  credentials_id?: string
}
