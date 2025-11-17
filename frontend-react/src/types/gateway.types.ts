/**
 * Gateway Type Definitions
 * Comprehensive types for IoT Gateway management
 */

// ==================== Base Types ====================

export interface EntityId {
  id: string
  entityType: string
}

export interface GatewayId extends EntityId {
  entityType: 'DEVICE'  // Gateways are special devices
}

export interface TenantId extends EntityId {
  entityType: 'TENANT'
}

export interface CustomerId extends EntityId {
  entityType: 'CUSTOMER'
}

// ==================== Gateway ====================

export interface Gateway {
  id: GatewayId
  createdTime: number
  tenantId?: TenantId
  customerId?: CustomerId
  name: string
  type: string
  label?: string
  active: boolean
  connected: boolean
  lastActivityTime?: number
  lastConnectTime?: number
  lastDisconnectTime?: number
  additionalInfo?: {
    gateway?: boolean
    description?: string
  }
}

// ==================== Connector Types ====================

export enum ConnectorType {
  MQTT = 'mqtt',
  MODBUS = 'modbus',
  OPCUA = 'opcua',
  BLE = 'ble',
  REQUEST = 'request',
  CAN = 'can',
  BACNET = 'bacnet',
  ODBC = 'odbc',
  REST = 'rest',
  SNMP = 'snmp',
  FTP = 'ftp',
  SOCKET = 'socket',
}

export enum ConnectorStatus {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  ERROR = 'ERROR',
}

export interface ConnectorConfiguration {
  // Common fields
  name: string
  type: ConnectorType
  enabled: boolean

  // MQTT specific
  broker?: {
    host: string
    port: number
    clientId?: string
    username?: string
    password?: string
    security?: {
      type: 'basic' | 'certificates'
      caCert?: string
      cert?: string
      privateKey?: string
    }
  }

  // Modbus specific
  modbus?: {
    host: string
    port: number
    unitId?: number
    timeout?: number
    byteOrder?: string
    wordOrder?: string
  }

  // OPC-UA specific
  opcua?: {
    applicationName?: string
    applicationUri?: string
    host: string
    port: number
    timeoutInMillis?: number
    scanPeriodInMillis?: number
    security?: string
    identity?: {
      type: string
      username?: string
      password?: string
    }
  }

  // BLE specific
  ble?: {
    scanTimeSeconds?: number
    rescanTimeSeconds?: number
    checkIntervalSeconds?: number
  }

  // Generic config
  config?: any
}

export interface Connector {
  name: string
  type: ConnectorType
  enabled: boolean
  status: ConnectorStatus
  configuration: ConnectorConfiguration
  devicesCount?: number
  messagesCount?: number
  errors?: number
  lastActivity?: number
}

// ==================== Gateway Statistics ====================

export interface GatewayStatistics {
  gatewayId: string
  uptime: number
  connectedDevices: number
  totalDevices: number
  messagesReceived: number
  messagesSent: number
  errorsCount: number
  connectorsStatus: {
    connected: number
    disconnected: number
    error: number
  }
  cpuUsage?: number
  memoryUsage?: number
  diskUsage?: number
}

// ==================== Gateway Logs ====================

export enum LogLevel {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  CRITICAL = 'CRITICAL',
}

export interface GatewayLog {
  timestamp: number
  level: LogLevel
  connector?: string
  message: string
  details?: string
  stackTrace?: string
}

export interface GatewayLogsFilter {
  level?: LogLevel[]
  connector?: string[]
  search?: string
  startTime?: number
  endTime?: number
}

// ==================== Gateway Events ====================

export enum GatewayEventType {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  CONNECTOR_ADDED = 'CONNECTOR_ADDED',
  CONNECTOR_REMOVED = 'CONNECTOR_REMOVED',
  CONNECTOR_UPDATED = 'CONNECTOR_UPDATED',
  DEVICE_CONNECTED = 'DEVICE_CONNECTED',
  DEVICE_DISCONNECTED = 'DEVICE_DISCONNECTED',
  CONFIG_UPDATED = 'CONFIG_UPDATED',
  ERROR = 'ERROR',
}

export interface GatewayEvent {
  id: string
  timestamp: number
  gatewayId: string
  type: GatewayEventType
  connector?: string
  deviceName?: string
  message: string
  severity: 'info' | 'warning' | 'error'
}

// ==================== 120% Enhanced Features ====================

export interface GatewayHealth {
  gatewayId: string
  status: 'healthy' | 'degraded' | 'critical' | 'offline'
  uptime: number
  lastHeartbeat: number
  connectors: {
    total: number
    active: number
    failed: number
  }
  devices: {
    total: number
    connected: number
    lastHour: number
  }
  performance: {
    cpu: number
    memory: number
    disk: number
    network: {
      rx: number  // bytes received
      tx: number  // bytes transmitted
    }
  }
  errors: {
    last24h: number
    lastHour: number
    critical: number
  }
}

export interface ConnectorTemplate {
  id: string
  name: string
  type: ConnectorType
  description: string
  icon: string
  defaultConfig: ConnectorConfiguration
  requiredFields: string[]
  optionalFields: string[]
  documentation?: string
}

export interface GatewayBackup {
  id: string
  gatewayId: string
  timestamp: number
  name: string
  configuration: {
    connectors: Connector[]
    settings: any
  }
  size: number
}

export interface GatewayDiagnostics {
  gatewayId: string
  timestamp: number
  systemInfo: {
    platform: string
    architecture: string
    cpuCount: number
    totalMemory: number
    gatewayVersion: string
    pythonVersion?: string
  }
  networkInfo: {
    interfaces: {
      name: string
      address: string
      mac: string
      speed?: string
    }[]
  }
  connectorDiagnostics: {
    connector: string
    status: string
    lastError?: string
    config: any
  }[]
}

// ==================== Translation Maps ====================

export const connectorTypeNames: Record<ConnectorType, string> = {
  [ConnectorType.MQTT]: 'MQTT',
  [ConnectorType.MODBUS]: 'Modbus',
  [ConnectorType.OPCUA]: 'OPC-UA',
  [ConnectorType.BLE]: 'Bluetooth Low Energy',
  [ConnectorType.REQUEST]: 'Request',
  [ConnectorType.CAN]: 'CAN Bus',
  [ConnectorType.BACNET]: 'BACnet',
  [ConnectorType.ODBC]: 'ODBC',
  [ConnectorType.REST]: 'REST API',
  [ConnectorType.SNMP]: 'SNMP',
  [ConnectorType.FTP]: 'FTP',
  [ConnectorType.SOCKET]: 'Socket',
}

export const connectorTypeIcons: Record<ConnectorType, string> = {
  [ConnectorType.MQTT]: 'CloudQueue',
  [ConnectorType.MODBUS]: 'Memory',
  [ConnectorType.OPCUA]: 'Engineering',
  [ConnectorType.BLE]: 'Bluetooth',
  [ConnectorType.REQUEST]: 'Send',
  [ConnectorType.CAN]: 'DirectionsCar',
  [ConnectorType.BACNET]: 'Business',
  [ConnectorType.ODBC]: 'Storage',
  [ConnectorType.REST]: 'Api',
  [ConnectorType.SNMP]: 'NetworkCheck',
  [ConnectorType.FTP]: 'FolderOpen',
  [ConnectorType.SOCKET]: 'Cable',
}

export const logLevelColors: Record<LogLevel, string> = {
  [LogLevel.DEBUG]: '#9E9E9E',
  [LogLevel.INFO]: '#2196F3',
  [LogLevel.WARNING]: '#FF9800',
  [LogLevel.ERROR]: '#F44336',
  [LogLevel.CRITICAL]: '#D32F2F',
}

// ==================== Helper Functions ====================

export function getGatewayHealthStatus(gateway: Gateway, health?: GatewayHealth): 'healthy' | 'degraded' | 'critical' | 'offline' {
  if (!gateway.connected) return 'offline'
  if (!health) return 'healthy'

  // Check error rate
  if (health.errors.critical > 0) return 'critical'
  if (health.errors.last24h > 100) return 'critical'
  if (health.errors.lastHour > 10) return 'degraded'

  // Check connector health
  const connectorHealth = health.connectors.active / health.connectors.total
  if (connectorHealth < 0.5) return 'critical'
  if (connectorHealth < 0.8) return 'degraded'

  // Check performance
  if (health.performance.cpu > 90 || health.performance.memory > 90) return 'critical'
  if (health.performance.cpu > 70 || health.performance.memory > 70) return 'degraded'

  // Check last heartbeat
  const now = Date.now()
  const heartbeatAge = now - health.lastHeartbeat
  if (heartbeatAge > 300000) return 'offline'  // 5 minutes
  if (heartbeatAge > 60000) return 'degraded'  // 1 minute

  return 'healthy'
}

export function getConnectorStatusColor(status: ConnectorStatus): string {
  const colors = {
    [ConnectorStatus.CONNECTED]: '#4CAF50',
    [ConnectorStatus.DISCONNECTED]: '#9E9E9E',
    [ConnectorStatus.CONNECTING]: '#2196F3',
    [ConnectorStatus.ERROR]: '#F44336',
  }
  return colors[status]
}

export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

export function formatUptime(ms: number): string {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}d ${hours % 24}h`
  if (hours > 0) return `${hours}h ${minutes % 60}m`
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`
  return `${seconds}s`
}
