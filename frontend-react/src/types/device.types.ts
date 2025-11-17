/**
 * Device Type Definitions
 * Comprehensive types matching ThingsBoard backend + enhancements
 */

// ==================== Base Types ====================

export interface EntityId {
  id: string
  entityType: string
}

export interface DeviceId extends EntityId {
  entityType: 'DEVICE'
}

export interface DeviceProfileId extends EntityId {
  entityType: 'DEVICE_PROFILE'
}

export interface CustomerId extends EntityId {
  entityType: 'CUSTOMER'
}

export interface TenantId extends EntityId {
  entityType: 'TENANT'
}

export interface DeviceCredentialsId extends EntityId {
  entityType: 'DEVICE_CREDENTIALS'
}

export interface OtaPackageId extends EntityId {
  entityType: 'OTA_PACKAGE'
}

export interface RuleChainId extends EntityId {
  entityType: 'RULE_CHAIN'
}

export interface DashboardId extends EntityId {
  entityType: 'DASHBOARD'
}

// ==================== Enums ====================

export enum DeviceProfileType {
  DEFAULT = 'DEFAULT',
  SNMP = 'SNMP',
}

export enum DeviceTransportType {
  DEFAULT = 'DEFAULT',
  MQTT = 'MQTT',
  COAP = 'COAP',
  LWM2M = 'LWM2M',
  SNMP = 'SNMP',
}

export enum BasicTransportType {
  HTTP = 'HTTP',
}

export type TransportType = BasicTransportType | DeviceTransportType
export type NetworkTransportType = BasicTransportType | Exclude<DeviceTransportType, DeviceTransportType.DEFAULT>

export enum TransportPayloadType {
  JSON = 'JSON',
  PROTOBUF = 'PROTOBUF',
}

export enum CoapTransportDeviceType {
  DEFAULT = 'DEFAULT',
  EFENTO = 'EFENTO',
}

export enum DeviceProvisionType {
  DISABLED = 'DISABLED',
  ALLOW_CREATE_NEW_DEVICES = 'ALLOW_CREATE_NEW_DEVICES',
  CHECK_PRE_PROVISIONED_DEVICES = 'CHECK_PRE_PROVISIONED_DEVICES',
  X509_CERTIFICATE_CHAIN = 'X509_CERTIFICATE_CHAIN',
}

export enum DeviceCredentialsType {
  ACCESS_TOKEN = 'ACCESS_TOKEN',
  X509_CERTIFICATE = 'X509_CERTIFICATE',
  MQTT_BASIC = 'MQTT_BASIC',
  LWM2M_CREDENTIALS = 'LWM2M_CREDENTIALS',
}

// ==================== Device ====================

export interface DeviceData {
  configuration?: any
  transportConfiguration?: any
}

export interface Device {
  id: DeviceId
  createdTime: number
  tenantId: TenantId
  customerId?: CustomerId
  name: string
  type: string
  label?: string
  deviceProfileId: DeviceProfileId
  deviceData?: DeviceData
  firmwareId?: OtaPackageId
  softwareId?: OtaPackageId
  externalId?: DeviceId
  version?: number
  additionalInfo?: {
    gateway?: boolean
    overwriteActivityTime?: boolean
    description?: string
  }
  // Frontend additions
  active?: boolean
  deviceProfileName?: string
  customerTitle?: string
  customerIsPublic?: boolean
}

// ==================== Device Credentials ====================

export interface DeviceCredentials {
  id: DeviceCredentialsId
  createdTime: number
  deviceId: DeviceId
  credentialsType: DeviceCredentialsType
  credentialsId: string
  credentialsValue?: string
}

// ==================== Device Profile ====================

export interface DeviceProfileData {
  configuration: DeviceProfileConfiguration
  transportConfiguration: DeviceProfileTransportConfiguration
  alarms?: any[]
  provisionConfiguration?: DeviceProvisionConfiguration
}

export interface DeviceProfileConfiguration {
  type: DeviceProfileType
}

export interface DeviceProfileTransportConfiguration {
  type: DeviceTransportType
}

export interface DeviceProvisionConfiguration {
  type: DeviceProvisionType
  provisionDeviceSecret?: string
  certificateValue?: string
  allowDuplicateCertificates?: boolean
}

export interface DeviceProfile {
  id?: DeviceProfileId
  createdTime?: number
  tenantId: TenantId
  name: string
  description?: string
  type: DeviceProfileType
  image?: string
  transportType: DeviceTransportType
  provisionType: DeviceProvisionType
  profileData: DeviceProfileData
  defaultRuleChainId?: RuleChainId
  defaultDashboardId?: DashboardId
  defaultQueueName?: string
  provisionDeviceKey?: string
  firmwareId?: OtaPackageId
  softwareId?: OtaPackageId
  defaultEdgeRuleChainId?: RuleChainId
  externalId?: DeviceProfileId
  version?: number
  isDefault?: boolean
}

export interface DeviceProfileInfo {
  id: DeviceProfileId
  tenantId: TenantId
  name: string
  type: DeviceProfileType
  image?: string
  transportType: DeviceTransportType
  defaultDashboardId?: DashboardId
}

// ==================== Device Connectivity ====================

export interface PublishTelemetryCommand {
  [transportType: string]: {
    [method: string]: {
      command: string
      description: string
    }
  }
}

export interface DeviceConnectivityStatus {
  connected: boolean
  lastActivityTime?: number
  lastConnectTime?: number
  lastDisconnectTime?: number
}

// ==================== Alarm Rules ====================

export enum AlarmSeverity {
  CRITICAL = 'CRITICAL',
  MAJOR = 'MAJOR',
  MINOR = 'MINOR',
  WARNING = 'WARNING',
  INDETERMINATE = 'INDETERMINATE',
}

export interface AlarmRule {
  id?: string
  alarmType: string
  createRules?: Record<AlarmSeverity, AlarmConditionSpec>
  clearRule?: AlarmConditionSpec
  schedule?: AlarmSchedule
}

export interface AlarmConditionSpec {
  condition: AlarmCondition
  spec?: AlarmConditionFilterSpec
}

export interface AlarmCondition {
  spec: AlarmConditionFilter[]
}

export interface AlarmConditionFilter {
  key: AlarmConditionFilterKey
  valueType: 'NUMERIC' | 'STRING' | 'BOOLEAN' | 'DATE_TIME'
  value?: any
  predicate: any
}

export interface AlarmConditionFilterKey {
  type: 'TIME_SERIES' | 'ATTRIBUTE' | 'ENTITY_FIELD' | 'CONSTANT'
  key: string
}

export interface AlarmConditionFilterSpec {
  type: string
  unit?: string
  value?: number
}

export interface AlarmSchedule {
  type: 'ANY_TIME' | 'SPECIFIC_TIME' | 'CUSTOM'
  timezone?: string
  daysOfWeek?: number[]
  startsOn?: number
  endsOn?: number
}

// ==================== 120% Enhanced Features ====================

export interface DeviceHealth {
  deviceId: string
  status: 'healthy' | 'warning' | 'critical' | 'offline'
  uptime: number
  messageCount: number
  errorCount: number
  lastTelemetryTime?: number
  batteryLevel?: number
  signalStrength?: number
  cpuUsage?: number
  memoryUsage?: number
  temperature?: number
}

export interface DeviceBulkOperation {
  type: 'assign' | 'unassign' | 'delete' | 'activate' | 'deactivate' | 'update_profile' | 'update_credentials'
  deviceIds: string[]
  params?: any
}

export interface DeviceFilter {
  id?: string
  name: string
  type?: string[]
  deviceProfile?: string[]
  customer?: string[]
  active?: boolean
  createdFrom?: number
  createdTo?: number
  labels?: string[]
}

export interface SavedDeviceFilter {
  id: string
  name: string
  filter: DeviceFilter
  createdTime: number
}

export interface DeviceTemplate {
  id: string
  name: string
  description: string
  type: string
  deviceProfileId: string
  defaultAttributes?: Record<string, any>
  defaultTelemetry?: Record<string, any>
  tags?: string[]
}

export interface DeviceStatistics {
  total: number
  active: number
  inactive: number
  byType: Record<string, number>
  byProfile: Record<string, number>
  byCustomer: Record<string, number>
  newLast24h: number
  newLast7d: number
  newLast30d: number
}

export interface DeviceNetworkInfo {
  deviceId: string
  deviceName: string
  type: string
  connections: {
    targetDeviceId: string
    targetDeviceName: string
    relationType: string
    direction: 'from' | 'to'
  }[]
  position?: { x: number; y: number }
}

// ==================== Translation Maps ====================

export const deviceProfileTypeNames: Record<DeviceProfileType, string> = {
  [DeviceProfileType.DEFAULT]: 'Default',
  [DeviceProfileType.SNMP]: 'SNMP',
}

export const deviceTransportTypeNames: Record<TransportType, string> = {
  [DeviceTransportType.DEFAULT]: 'Default',
  [DeviceTransportType.MQTT]: 'MQTT',
  [DeviceTransportType.COAP]: 'CoAP',
  [DeviceTransportType.LWM2M]: 'LwM2M',
  [DeviceTransportType.SNMP]: 'SNMP',
  [BasicTransportType.HTTP]: 'HTTP',
}

export const deviceProvisionTypeNames: Record<DeviceProvisionType, string> = {
  [DeviceProvisionType.DISABLED]: 'Disabled',
  [DeviceProvisionType.ALLOW_CREATE_NEW_DEVICES]: 'Allow Create New Devices',
  [DeviceProvisionType.CHECK_PRE_PROVISIONED_DEVICES]: 'Check Pre-Provisioned Devices',
  [DeviceProvisionType.X509_CERTIFICATE_CHAIN]: 'X.509 Certificate Chain',
}

export const deviceCredentialsTypeNames: Record<DeviceCredentialsType, string> = {
  [DeviceCredentialsType.ACCESS_TOKEN]: 'Access Token',
  [DeviceCredentialsType.X509_CERTIFICATE]: 'X.509 Certificate',
  [DeviceCredentialsType.MQTT_BASIC]: 'MQTT Basic',
  [DeviceCredentialsType.LWM2M_CREDENTIALS]: 'LwM2M Credentials',
}

// ==================== Helper Functions ====================

export function getDeviceHealthStatus(device: Device, health?: DeviceHealth): 'healthy' | 'warning' | 'critical' | 'offline' {
  if (!device.active) return 'offline'
  if (!health) return 'healthy'

  if (health.errorCount > 100) return 'critical'
  if (health.errorCount > 10) return 'warning'
  if (health.batteryLevel !== undefined && health.batteryLevel < 20) return 'critical'
  if (health.batteryLevel !== undefined && health.batteryLevel < 50) return 'warning'

  const now = Date.now()
  const lastActivity = health.lastTelemetryTime || 0
  const inactiveTime = now - lastActivity

  if (inactiveTime > 86400000) return 'offline' // 24 hours
  if (inactiveTime > 3600000) return 'warning' // 1 hour

  return 'healthy'
}

export function getTransportIcon(transportType: TransportType): string {
  const iconMap: Record<TransportType, string> = {
    [DeviceTransportType.DEFAULT]: 'Storage',
    [DeviceTransportType.MQTT]: 'CloudQueue',
    [DeviceTransportType.COAP]: 'Wifi',
    [DeviceTransportType.LWM2M]: 'DeviceHub',
    [DeviceTransportType.SNMP]: 'NetworkCheck',
    [BasicTransportType.HTTP]: 'Http',
  }
  return iconMap[transportType] || 'DeviceUnknown'
}

export function createDefaultDeviceProfile(): DeviceProfile {
  return {
    tenantId: { id: '', entityType: 'TENANT' },
    name: '',
    description: '',
    type: DeviceProfileType.DEFAULT,
    transportType: DeviceTransportType.DEFAULT,
    provisionType: DeviceProvisionType.DISABLED,
    isDefault: false,
    profileData: {
      configuration: {
        type: DeviceProfileType.DEFAULT,
      },
      transportConfiguration: {
        type: DeviceTransportType.DEFAULT,
      },
    },
  }
}

export function isValidDeviceProfile(profile: DeviceProfile): boolean {
  return (
    profile.name.trim().length > 0 &&
    profile.transportType !== undefined &&
    profile.provisionType !== undefined
  )
}

export function getAlarmSeverityColor(severity: AlarmSeverity): string {
  switch (severity) {
    case AlarmSeverity.CRITICAL:
      return '#D32F2F'
    case AlarmSeverity.MAJOR:
      return '#F57C00'
    case AlarmSeverity.MINOR:
      return '#FBC02D'
    case AlarmSeverity.WARNING:
      return '#FDD835'
    case AlarmSeverity.INDETERMINATE:
      return '#757575'
    default:
      return '#757575'
  }
}
