///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  ConfigurationModes,
  GatewayConnector,
  LocalLogsConfigs,
  LogSavingPeriod,
  SecurityTypes,
  StorageTypes
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { GatewayLogLevel } from '@home/components/widget/lib/gateway/gateway-form.models';

export interface GatewayConfigValue {
  mode: ConfigurationModes;
  thingsboard: GatewayGeneralConfig;
  storage: GatewayStorageConfig;
  grpc: GatewayGRPCConfig;
  connectors?: GatewayConnector[];
  logs: GatewayLogsConfig;
}

export interface GatewayGRPCConfig {
  enabled: boolean;
  serverPort: number;
  keepAliveTimeMs: number;
  keepAliveTimeoutMs: number;
  keepalivePermitWithoutCalls: boolean;
  maxPingsWithoutData: number;
  minTimeBetweenPingsMs: number;
  minPingIntervalWithoutDataMs: number;
}

export interface GatewayStorageConfig {
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
  level: string | number;
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

