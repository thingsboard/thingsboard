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
  opcua = 'OPC-UA',
  ble = 'BLE',
  request = 'Request',
  can = 'CAN',
  bacnet = 'BACnet',
  custom = 'Custom'
}

export interface GatewayFormModels {
  gateway?: string;
  accessToken?: string;
  securityType?: SecurityType;
  host?: string;
  port?: number;
  remoteConfiguration?: boolean;
  caCertPath?: string;
  privateKeyPath?: string;
  certPath?: string;
  remoteLoggingLevel?: GatewayLogLevel;
  remoteLoggingPathToLogs?:string;
  storageType?: StorageType;
  readRecordsCount?: number;
  maxRecordsCount?: number;
  maxFilesCount?: number;
  dataFolderPath?: number;
  connectors?: Array<GatewayFormConnectorModel>;
}

export interface GatewayFormConnectorModel {
  config: object;
  name: string;
  configType: ConnectorType;
  enabled: boolean;
}

export const DEFAULT_CONNECTOR: GatewayFormConnectorModel = {
  config: {},
  name: '',
  configType: null,
  enabled: false
};

type Connector = {
  [key in ConnectorType]?: Array<ConnectorConfig>;
}

interface GatewaySetting extends Connector{
  thingsboard: GatewayMainSetting;
}

interface ConnectorConfig {
  name: string;
  config: object;
}

interface GatewayMainSetting {
  thingsboard: GatewayMainThingsboardSetting;
  connectors: Array<GatewayMainConnector>,
  logs: string,
  storage: GatewayStorage
}

interface GatewayMainThingsboardSetting {
  host: string,
  remoteConfiguration: boolean,
  port: number,
  security: GatewaySecurity
}

type GatewaySecurity = SecurityToken | SecurityCertificate;

interface SecurityToken {
  accessToken: string;
}

interface SecurityCertificate {
  caCert: string;
  privateKey: string;
  cert: string;
}

type GatewayStorage = GatewayStorageMemory | GatewayStorageFile;

interface GatewayStorageMemory {
  type: string;
  max_records_count: number;
  read_records_count: number;
}

interface GatewayStorageFile {
  type: string;
  data_folder_path: number;
  max_file_count: number;
  max_read_records_count: number;
  max_records_per_file: number;
}

interface GatewayMainConnector {
  configuration: string;
  name: string;
  type: ConnectorType;
}

export function ValidateJSON(control: AbstractControl): ValidationErrors | null {
  if (JSON.stringify(control.value) === JSON.stringify({})) {
    return { validJSON: true };
  }
  return null;
}

const TEMPLATE_LOGS_CONFIG = '[loggers]}}keys=root, service, connector, converter, tb_connection, storage, extension}}[handlers]}}keys=consoleHandler, serviceHandler, connectorHandler, converterHandler, tb_connectionHandler, storageHandler, extensionHandler}}[formatters]}}keys=LogFormatter}}[logger_root]}}level=ERROR}}handlers=consoleHandler}}[logger_connector]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=connector}}[logger_storage]}}level={ERROR}}}handlers=storageHandler}}formatter=LogFormatter}}qualname=storage}}[logger_tb_connection]}}level={ERROR}}}handlers=tb_connectionHandler}}formatter=LogFormatter}}qualname=tb_connection}}[logger_service]}}level={ERROR}}}handlers=serviceHandler}}formatter=LogFormatter}}qualname=service}}[logger_converter]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=converter}}[logger_extension]}}level={ERROR}}}handlers=connectorHandler}}formatter=LogFormatter}}qualname=extension}}[handler_consoleHandler]}}class=StreamHandler}}level={ERROR}}}formatter=LogFormatter}}args=(sys.stdout,)}}[handler_connectorHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}connector.log", "d", 1, 7,)}}[handler_storageHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}storage.log", "d", 1, 7,)}}[handler_serviceHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}service.log", "d", 1, 7,)}}[handler_converterHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}converter.log", "d", 1, 3,)}}[handler_extensionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}extension.log", "d", 1, 3,)}}[handler_tb_connectionHandler]}}level={ERROR}}}class=logging.handlers.TimedRotatingFileHandler}}formatter=LogFormatter}}args=("{./logs/}tb_connection.log", "d", 1, 3,)}}[formatter_LogFormatter]}}format="%(asctime)s - %(levelname)s - [%(filename)s] - %(module)s - %(lineno)d - %(message)s" }}datefmt="%Y-%m-%d %H:%M:%S"';

export function generateYAMLConfigFile(gatewaySetting: GatewayFormModels): string {
  let config;
  config = 'thingsboard:\n';
  config += '  host: ' + gatewaySetting.host + '\n';
  config += '  remoteConfiguration: ' + gatewaySetting.remoteConfiguration + '\n';
  config += '  port: ' + gatewaySetting.port + '\n';
  config += '  security:\n';
  if (gatewaySetting.securityType === SecurityType.accessToken) {
    config += '    access-token: ' + gatewaySetting.accessToken + '\n';
  } else {
    config += '    ca_cert: ' + gatewaySetting.caCertPath + '\n';
    config += '    privateKey: ' + gatewaySetting.privateKeyPath + '\n';
    config += '    cert: ' + gatewaySetting.certPath + '\n';
  }
  config += 'storage:\n';
  if (gatewaySetting.storageType === StorageType.memory) {
    config += '  type: memory\n';
    config += '  read_records_count: ' + gatewaySetting.readRecordsCount + '\n';
    config += '  max_records_count: ' + gatewaySetting.maxRecordsCount + '\n';
  } else {
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

export function generateConnectorConfigFiles(fileZipAdd: object, connectors: Array<GatewayFormConnectorModel>): void {
  for(const connector of connectors) {
    if (connector.enabled) {
      fileZipAdd[generateFileName(connector.name)] = JSON.stringify(connector.config);
    }
  }
}

function generateFileName(fileName): string {
  return fileName.replace('_', '')
    .replace('-', '')
    .replace(/^\s+|\s+/g, '')
    .toLowerCase() + '.json';
}

export function generateLogConfigFile(fileZipAdd: object, logsLevel: string, logsPath: string): void {
  fileZipAdd['logs.conf'] = getLogsConfig(logsLevel, logsPath);
}

function getLogsConfig(logsLevel: string, logsPath: string): string {
  return TEMPLATE_LOGS_CONFIG
    .replace(/{ERROR}/g, logsLevel)
    .replace(/{.\/logs\/}/g, logsPath);
}

export function getEntityId(gatewayId: string): EntityId {
  return {
    id: gatewayId,
    entityType: EntityType.DEVICE
  }
}

export function createFormConfig(keyValue: GatewayMainSetting): GatewayFormModels {
  const formSetting: GatewayFormModels = {};
  if (Object.prototype.hasOwnProperty.call(keyValue, 'thingsboard')) {
    formSetting.host = keyValue.thingsboard.host;
    formSetting.port = keyValue.thingsboard.port;
    formSetting.remoteConfiguration = keyValue.thingsboard.remoteConfiguration;
    if (Object.prototype.hasOwnProperty.call(keyValue.thingsboard.security, SecurityType.accessToken)) {
      formSetting.securityType = SecurityType.accessToken;
      formSetting.accessToken = (keyValue.thingsboard.security as SecurityToken).accessToken;
    } else {
      formSetting.securityType = SecurityType.tls;
      formSetting.caCertPath = (keyValue.thingsboard.security as SecurityCertificate).caCert;
      formSetting.privateKeyPath = (keyValue.thingsboard.security as SecurityCertificate).privateKey;
      formSetting.certPath = (keyValue.thingsboard.security as SecurityCertificate).cert;
    }
  }

  if (Object.prototype.hasOwnProperty.call(keyValue, 'storage') && Object.prototype.hasOwnProperty.call(keyValue.storage, 'type')) {
    if (keyValue.storage.type === StorageType.memory) {
      formSetting.storageType = StorageType.memory;
      formSetting.readRecordsCount = (keyValue.storage as GatewayStorageMemory).read_records_count;
      formSetting.maxRecordsCount = (keyValue.storage as GatewayStorageMemory).max_records_count;
    } else if (keyValue.storage.type === StorageType.file) {
      formSetting.storageType = StorageType.file;
      formSetting.dataFolderPath = (keyValue.storage as GatewayStorageFile).data_folder_path;
      formSetting.maxFilesCount = (keyValue.storage as GatewayStorageFile).max_file_count;
      formSetting.readRecordsCount = (keyValue.storage as GatewayStorageMemory).read_records_count;
      formSetting.maxRecordsCount = (keyValue.storage as GatewayStorageMemory).max_records_count;
    }
  }
  return formSetting;
}

export function getDraftConnectorsJSON(currentConnectors: Array<GatewayFormConnectorModel>) {
  const draftConnectors = {};
  for(const connector of currentConnectors){
    if (!connector.enabled) {
      draftConnectors[connector.name] = {
        connector: connector.configType,
        config: connector.config
      };
    }
  }
  return draftConnectors;
}

export function gatewayConfigJSON(gatewayConfiguration: GatewayFormModels): GatewaySetting {
  const gatewayConfig = {
    thingsboard: gatewayMainConfigJSON(gatewayConfiguration)
  };
  gatewayConnectorJSON(gatewayConfig, gatewayConfiguration.connectors);
  return gatewayConfig;
}

function gatewayMainConfigJSON(gatewayConfiguration: GatewayFormModels): GatewayMainSetting {
  let security: GatewaySecurity;
  if (gatewayConfiguration.securityType === SecurityType.accessToken) {
    security = {
      accessToken: gatewayConfiguration.accessToken
    }
  } else {
    security = {
      caCert: gatewayConfiguration.caCertPath,
      privateKey: gatewayConfiguration.privateKeyPath,
      cert: gatewayConfiguration.certPath
    }
  }
  const thingsboard: GatewayMainThingsboardSetting = {
    host: gatewayConfiguration.host,
    remoteConfiguration: gatewayConfiguration.remoteConfiguration,
    port: gatewayConfiguration.port,
    security
  };

  let storage: GatewayStorage;
  if (gatewayConfiguration.storageType === StorageType.memory) {
    storage = {
      type: StorageType.memory,
      read_records_count: gatewayConfiguration.readRecordsCount,
      max_records_count: gatewayConfiguration.maxRecordsCount
    };
  } else {
    storage = {
      type: StorageType.file,
      data_folder_path: gatewayConfiguration.dataFolderPath,
      max_file_count: gatewayConfiguration.maxFilesCount,
      max_read_records_count: gatewayConfiguration.readRecordsCount,
      max_records_per_file: gatewayConfiguration.maxRecordsCount
    };
  }

  const connectors: Array<GatewayMainConnector> = [];
  for (const connector of gatewayConfiguration.connectors) {
    if (connector.enabled) {
      const connectorConfig: GatewayMainConnector = {
        configuration: generateFileName(connector.name),
        name: connector.name,
        type: connector.configType
      };
      connectors.push(connectorConfig);
    }
  }

  return {
    thingsboard,
    connectors,
    storage,
    logs: window.btoa(getLogsConfig(gatewayConfiguration.remoteLoggingLevel, gatewayConfiguration.remoteLoggingPathToLogs))
  }
}

function gatewayConnectorJSON(gatewayConfiguration, currentConnectors: Array<GatewayFormConnectorModel>): void {
  for (const connector of currentConnectors) {
    if (connector.enabled) {
      const typeConnector = connector.configType;
      if (!Array.isArray(gatewayConfiguration[typeConnector])) {
        gatewayConfiguration[typeConnector] = [];
      }

      const connectorConfig: ConnectorConfig = {
        name: connector.name,
        config: connector.config
      };
      gatewayConfiguration[typeConnector].push(connectorConfig);
    }
  }
}
