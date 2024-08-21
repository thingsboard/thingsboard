import {
  ConfigurationModes,
  GatewayConnector, LocalLogsConfigs, LogSavingPeriod,
  SecurityTypes,
  StorageTypes
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { GatewayLogLevel } from '@home/components/widget/lib/gateway/gateway-form.models';

export interface GatewayConfigValue {
  mode: ConfigurationModes;
  thingsboard: GatewayGeneralConfig;
  storage: {
    type: StorageTypes;
    read_records_count?: number;
    max_records_count?: number;
    data_folder_path?: string;
    max_file_count?: number;
    max_read_records_count?: number;
    max_records_per_file?: number;
    data_file_path?: string;
    messages_ttl_check_in_hours?: number;
    messages_ttl_in_days?: number;
  };
  grpc: {
    enabled: boolean;
    serverPort: number;
    keepAliveTimeMs: number;
    keepAliveTimeoutMs: number;
    keepalivePermitWithoutCalls: boolean;
    maxPingsWithoutData: number;
    minTimeBetweenPingsMs: number;
    minPingIntervalWithoutDataMs: number;
  };
  connectors?: GatewayConnector[];
  logs: GatewayLogsConfig;
}

export interface GatewayGeneralConfig {
  host: string;
  port: number;
  remoteShell: boolean;
  remoteConfiguration: boolean;
  checkConnectorsConfigurationInSeconds: number;
  statistics: {
    enable: boolean;
    statsSendPeriodInSeconds: number;
    commands: GatewayConfigCommand[];
  };
  maxPayloadSizeBytes: number;
  minPackSendDelayMS: number;
  minPackSizeToSend: number;
  handleDeviceRenaming: boolean;
  checkingDeviceActivity: {
    checkDeviceInactivity: boolean;
    inactivityTimeoutSeconds?: number;
    inactivityCheckPeriodSeconds?: number;
  };
  security: GatewayConfigSecurity;
  qos: number;
}

export interface GatewayLogsConfig {
  dateFormat: string;
  logFormat: string;
  type?: string;
  remote?: {
    enabled: boolean;
    logLevel: GatewayLogLevel;
  };
  local: LocalLogs;
}

export interface GatewayConfigSecurity {
  type: SecurityTypes;
  accessToken?: string;
  clientId?: string;
  username?: string;
  password?: string;
  caCert?: string;
  cert?: string;
  privateKey?: string;
}

export interface GatewayConfigCommand {
  attributeOnGateway: string;
  command: string;
  timeout: number;
}

export interface LogConfig {
  logLevel: GatewayLogLevel;
  filePath: string;
  backupCount: number;
  savingTime: number;
  savingPeriod: LogSavingPeriod;
}

export type LocalLogs = Record<LocalLogsConfigs, LogConfig>;

interface LogFormatterConfig {
  class: string;
  format: string;
  datefmt: string;
}

interface StreamHandlerConfig {
  class: string;
  formatter: string;
  level: string;
  stream: string;
}

interface FileHandlerConfig {
  class: string;
  formatter: string;
  filename: string;
  backupCount: number;
  encoding: string;
}

interface LoggerConfig {
  handlers: string[];
  level: string;
  propagate: boolean;
}

interface RootConfig {
  level: string;
  handlers: string[];
}

export interface LogAttribute {
  version: number;
  disable_existing_loggers: boolean;
  formatters: {
    LogFormatter: LogFormatterConfig;
  };
  handlers: {
    consoleHandler: StreamHandlerConfig;
    databaseHandler: FileHandlerConfig;
  };
  loggers: {
    database: LoggerConfig;
  };
  root: RootConfig;
  ts: number;
}

