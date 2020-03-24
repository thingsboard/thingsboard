///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { AbstractControl, ValidationErrors } from '@angular/forms';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export enum SecurityType {
  tls = 'tls',
  accessToken = 'accessToken'
}

export interface WidgetSetting {
  widgetTitle: string;
  archiveFileName: string;
  gatewayType: string;
  successfulSave: string;
  gatewayNameExists: string;
}

export const CURRENT_CONFIGURATION_ATTRIBUTE = 'current_configuration';
export const CONFIGURATION_DRAFT_ATTRIBUTE = 'configuration_drafts';
export const CONFIGURATION_ATTRIBUTE = 'configuration';
export const REMOTE_LOGGING_LEVEL_ATTRIBUTE = 'RemoteLoggingLevel';

export const SecurityTypeTranslationMap = new Map<SecurityType, string>(
  [
    [SecurityType.tls, 'gateway.security-types.tls'],
    [SecurityType.accessToken, 'gateway.security-types.access-token']
  ]
);

export enum GatewayLogLevel {
  none = 'NONE',
  critical = 'CRITICAL',
  error = 'ERROR',
  warning = 'WARNING',
  info = 'INFO',
  debug = 'DEBUG'
}

export enum StorageType {
  memory = 'memory',
  file = 'file'
}

export const StorageTypeTranslationMap = new Map<StorageType, string>(
  [
    [StorageType.memory, 'gateway.storage-types.memory-storage'],
    [StorageType.file, 'gateway.storage-types.file-storage']
  ]
);

export enum ConnectorType {
  mqtt= 'MQTT',
  modbus = 'Modbus',
  opc_ua = 'OPC-UA',
  ble = 'BLE'
}

export interface MainGatewaySetting {
  thingsboard: GatewaySetting;
  [key: string]: object;
}

export interface GatewaySetting {
  thingsboard: GatewaySettingThingsboard;
  connectors: Array<ConnectorConfig>,
  logs: string,
  storage: GatewaySettingStorage
}

export interface GatewaySettingThingsboard {
  host: number,
  remoteConfiguration: boolean,
  port: number,
  security: Security
}

export type Security = SecurityToken | SecurityCertificate;

export interface SecurityToken {
  accessToken: string;
}

export interface SecurityCertificate {
  caCert: string;
  privateKey: string;
  cert: string;
}

export type GatewaySettingStorage = GatewaySettingStorageMemory | GatewaySettingStorageFile;

export interface GatewaySettingStorageMemory {
  type: string;
  max_records_count: number;
  read_records_count: number;
}

export interface GatewaySettingStorageFile {
  type: string;
  data_folder_path: number;
  max_file_count: number;
  max_read_records_count: number;
  max_records_per_file: number;
}

export interface ConnectorConfig {
  configuration: string;
  name: string;
  type: ConnectorType;
}

export interface ConnectorForm {
  config: object;
  name: string;
  configType: ConnectorType;
  enabled: boolean;
}

export const DEFAULT_CONNECTOR: ConnectorForm = {
  config: {},
  name: '',
  configType: null,
  enabled: false
};

export function ValidateJSON(control: AbstractControl): ValidationErrors | null {
  if (JSON.stringify(control.value) === JSON.stringify({})) {
    return { validJSON: true };
  }
  return null;
}

const TEMPLATE_LOGS_CONFIG = '[loggers]}}keys=root, service, connector, converter, tb_connection, storage, extension}}[handlers]}}keys=consoleHandler, serviceHandler, connectorHandler, converterHandler, tb_connectionHandler, storageHandler, extensionHandler}}[formatters]}}keys=LogFormatter}}[logger_root]}}level=ERROR}}handlers=consoleHandler}}[logger_connector]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=connector}}[logger_storage]}}level={ERROR}}}handlers=storageHandler}}formatter=LogFormatter}}qualname=storage}}[logger_tb_connection]}}level={ERROR}}}handlers=tb_connectionHandler}}formatter=LogFormatter}}qualname=tb_connection}}[logger_service]}}level={ERROR}}}handlers=serviceHandler}}formatter=LogFormatter}}qualname=service}}[logger_converter]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=converter}}[logger_extension]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=extension}}[handler_consoleHandler]}}class=StreamHandler}}level={ERROR}}}formatter=LogFormatter}}args=(sys.stdout,)}}[handler_connectorHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}connector.log", "d", 1, 7,)}}[handler_storageHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}storage.log", "d", 1, 7,)}}[handler_serviceHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}service.log", "d", 1, 7,)}}[handler_converterHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}converter.log", "d", 1, 3,)}}[handler_extensionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}extension.log", "d", 1, 3,)}}[handler_tb_connectionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}tb_connection.log", "d", 1, 3,)}}[formatter_LogFormatter]}}format="%(asctime)s - %(levelname)s - [%(filename)s] - %(module)s - %(lineno)d - %(message)s" }}datefmt="%Y-%m-%d %H:%M:%S"';

export function getLogsConfig(logsLevel: string, logsPath: string): string {
  return TEMPLATE_LOGS_CONFIG
    .replace(/{ERROR}/g, logsLevel)
    .replace(/{.\/logs\/}/g, logsPath);
}

export function generateYAMLConfigurationFile(gatewaySetting): string {
  let config;
  config = 'thingsboard:\n';
  config += '  host: ' + gatewaySetting.host + '\n';
  config += '  remoteConfiguration: ' + gatewaySetting.remoteConfiguration + '\n';
  config += '  port: ' + gatewaySetting.port + '\n';
  config += '  security:\n';
  if (gatewaySetting.securityType === SecurityType.accessToken) {
    config += '    access-token: ' + gatewaySetting.accessToken + '\n';
  } else if (gatewaySetting.securityType === SecurityType.tls) {
    config += '    ca_cert: ' + gatewaySetting.caCertPath + '\n';
    config += '    privateKey: ' + gatewaySetting.privateKeyPath + '\n';
    config += '    cert: ' + gatewaySetting.certPath + '\n';
  }
  config += 'storage:\n';
  if (gatewaySetting.storageType === StorageType.memory) {
    config += '  type: memory\n';
    config += '  read_records_count: ' + gatewaySetting.readRecordsCount + '\n';
    config += '  max_records_count: ' + gatewaySetting.maxRecordsCount + '\n';
  } else if (gatewaySetting.storageType === StorageType.file) {
    config += '  type: file\n';
    config += '  data_folder_path: ' + gatewaySetting.dataFolderPath + '\n';
    config += '  max_file_count: ' + gatewaySetting.maxFilesCount + '\n';
    config += '  max_read_records_count: ' + gatewaySetting.readRecordsCount + '\n';
    config += '  max_records_per_file: ' + gatewaySetting.maxRecordsCount + '\n';
  }
  config += 'connectors:\n';
  for(const connector of gatewaySetting.connectors){
    if (connector.enabled) {
      config += '  -\n';
      config += '    name: ' + connector.name + '\n';
      config += '    type: ' + connector.configType + '\n';
      config += '    configuration: ' + generateFileName(connector.name) + '\n';
    }
  }
  return config;
}

export function generateConfigConnectorFiles(fileZipAdd: object, connectors: Array<ConnectorForm>): void {
  for(const connector of connectors) {
    if (connector.enabled) {
      fileZipAdd[generateFileName(connector.name)] = JSON.stringify(connector.config);
    }
  }
}

export function generateLogConfigFile(fileZipAdd: object, logsLevel: string, logsPath: string): void {
  fileZipAdd['logs.conf'] = getLogsConfig(logsLevel, logsPath);
}

export function generateFileName(fileName): string {
  return fileName.replace('_', '')
    .replace('-', '')
    .replace(/^\s+|\s+/g, '')
    .toLowerCase() + '.json';
}

export function getEntityId(gatewayId: string): EntityId {
  return {
    id: gatewayId,
    entityType: EntityType.DEVICE
  }
}

