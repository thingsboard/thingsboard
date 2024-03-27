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

import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';
import { ValueTypeData } from '@shared/models/constants';

export enum StorageTypes {
  MEMORY = 'memory',
  FILE = 'file',
  SQLITE = 'sqlite'
}

export enum DeviceGatewayStatus {
  EXCEPTION = 'EXCEPTION'
}

export enum GatewayLogLevel {
  NONE = 'NONE',
  CRITICAL = 'CRITICAL',
  ERROR = 'ERROR',
  WARNING = 'WARNING',
  INFO = 'INFO',
  DEBUG = 'DEBUG'
}

export const GatewayStatus = {
  ...GatewayLogLevel,
  ...DeviceGatewayStatus
};

export type GatewayStatus = DeviceGatewayStatus | GatewayLogLevel;

export enum LogSavingPeriod {
  days = 'D',
  hours = 'H',
  minutes = 'M',
  seconds = 'S'
}

export enum LocalLogsConfigs {
  service = 'service',
  connector = 'connector',
  converter = 'converter',
  tb_connection = 'tb_connection',
  storage = 'storage',
  extension = 'extension'
}

export const LocalLogsConfigTranslateMap = new Map<LocalLogsConfigs, string>([
  [LocalLogsConfigs.service, 'Service'],
  [LocalLogsConfigs.connector, 'Connector'],
  [LocalLogsConfigs.converter, 'Converter'],
  [LocalLogsConfigs.tb_connection, 'TB Connection'],
  [LocalLogsConfigs.storage, 'Storage'],
  [LocalLogsConfigs.extension, 'Extension']
]);

export const LogSavingPeriodTranslations = new Map<LogSavingPeriod, string>(
  [
    [LogSavingPeriod.days, 'gateway.logs.days'],
    [LogSavingPeriod.hours, 'gateway.logs.hours'],
    [LogSavingPeriod.minutes, 'gateway.logs.minutes'],
    [LogSavingPeriod.seconds, 'gateway.logs.seconds']
  ]
);

export const StorageTypesTranslationMap = new Map<StorageTypes, string>(
  [
    [StorageTypes.MEMORY, 'gateway.storage-types.memory-storage'],
    [StorageTypes.FILE, 'gateway.storage-types.file-storage'],
    [StorageTypes.SQLITE, 'gateway.storage-types.sqlite']
  ]
);

export enum SecurityTypes {
  ACCESS_TOKEN = 'accessToken',
  USERNAME_PASSWORD = 'usernamePassword',
  TLS_ACCESS_TOKEN = 'tlsAccessToken',
  TLS_PRIVATE_KEY = 'tlsPrivateKey'
}

export const GecurityTypesTranslationsMap = new Map<SecurityTypes, string>(
  [
    [SecurityTypes.ACCESS_TOKEN, 'gateway.security-types.access-token'],
    [SecurityTypes.USERNAME_PASSWORD, 'gateway.security-types.username-password'],
    [SecurityTypes.TLS_ACCESS_TOKEN, 'gateway.security-types.tls-access-token']
  ]
);

export interface GatewayConnector {
  name: string;
  type: string;
  configuration?: string;
  configurationJson: string;
  logLevel: string;
  key?: string;
}


export const GatewayConnectorDefaultTypesTranslates = new Map<string, string>([
  ['mqtt', 'MQTT'],
  ['modbus', 'MODBUS'],
  ['grpc', 'GRPC'],
  ['opcua', 'OPCUA'],
  ['opcua_asyncio', 'OPCUA ASYNCIO'],
  ['ble', 'BLE'],
  ['request', 'REQUEST'],
  ['can', 'CAN'],
  ['bacnet', 'BACNET'],
  ['odbc', 'ODBC'],
  ['rest', 'REST'],
  ['snmp', 'SNMP'],
  ['ftp', 'FTP'],
  ['socket', 'SOCKET'],
  ['xmpp', 'XMPP'],
  ['ocpp', 'OCPP'],
  ['custom', 'CUSTOM']
]);

export interface LogLink {
  name: string;
  key: string;
  filterFn?: (arg: any) => boolean;
}

export interface GatewayLogData {
  ts: number;
  key: string;
  message: string;
  status: GatewayStatus;
}

export interface AddConnectorConfigData {
  dataSourceData: Array<any>
}

export enum ConnectorConfigurationModes {
  BASIC = 'basic',
  ADVANCED = 'advanced'
}

export enum BrokerSecurityTypes {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
  CERTIFICATES = 'certificates'
}

export const BrokerSecurityTypeTranslations = new Map<BrokerSecurityTypes, string>(
  [
    [BrokerSecurityTypes.ANONYMOUS, 'gateway.broker.security-types.anonymous'],
    [BrokerSecurityTypes.BASIC, 'gateway.broker.security-types.basic'],
    [BrokerSecurityTypes.CERTIFICATES, 'gateway.broker.security-types.certificates']
  ]
);

export const MqttVersions = [
  { name: 3.1, value: 3 },
  { name: 3.11, value: 4 },
  { name: 5, value: 5 }
];

export enum MappingTypes {
  DATA = 'data',
  REQUESTS = 'requests'
}

export const MappingTypeTranslationsMap = new Map<MappingTypes, string>(
  [
    [MappingTypes.DATA, 'gateway.data-mapping'],
    [MappingTypes.REQUESTS, 'gateway.requests-mapping']
  ]
);

export const MappingHintTranslationsMap = new Map<MappingTypes, string>(
  [
    [MappingTypes.DATA, 'gateway.data-mapping-hint'],
    [MappingTypes.REQUESTS, 'gateway.requests-mapping-hint']
  ]
);

export const QualityTypes = [0, 1 ,2];

export const QualityTypeTranslationsMap = new Map<number, string>(
  [
    [0, 'gateway.qos.at-most-once'],
    [1, 'gateway.qos.at-least-once'],
    [2, 'gateway.qos.exactly-once']
  ]
);

export enum ConvertorTypes {
  JSON = 'json',
  BYTES = 'bytes',
  CUSTOM = 'custom'
}

export const ConvertorTypeTranslationsMap = new Map<ConvertorTypes, string>(
  [
    [ConvertorTypes.JSON, 'gateway.JSON'],
    [ConvertorTypes.BYTES, 'gateway.bytes'],
    [ConvertorTypes.CUSTOM, 'gateway.custom']
  ]
);

export enum SourceTypes {
  MSG = 'message',
  TOPIC = 'topic',
  CONST = 'constant'
}

export const SourceTypeTranslationsMap = new Map<SourceTypes, string>(
  [
    [SourceTypes.MSG, 'gateway.source-type.msg'],
    [SourceTypes.TOPIC, 'gateway.source-type.topic'],
    [SourceTypes.CONST, 'gateway.source-type.const'],
  ]
);

export enum RequestTypes {
  CONNECT_REQUEST = 'connectRequests',
  DISCONNECT_REQUEST = 'disconnectRequests',
  ATTRIBUTE_REQUEST = 'attributeRequests',
  ATTRIBUTE_UPDATE = 'attributeUpdates',
  SERVER_SIDE_RPC = 'serverSideRpc'
}

export const RequestTypesTranslationsMap = new Map<RequestTypes, string>(
  [
    [RequestTypes.CONNECT_REQUEST, 'gateway.request.connect-request'],
    [RequestTypes.DISCONNECT_REQUEST, 'gateway.request.disconnect-request'],
    [RequestTypes.ATTRIBUTE_REQUEST, 'gateway.request.attribute-request'],
    [RequestTypes.ATTRIBUTE_UPDATE, 'gateway.request.attribute-update'],
    [RequestTypes.SERVER_SIDE_RPC, 'gateway.request.rpc-connection'],
  ]
);

export enum MappingKeysType {
  ATTRIBUTES = 'attributes',
  TIMESERIES = 'timeseries',
  CUSTOM = 'extensionConfig'
}

export const MappingKeysPanelTitleTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.attributes'],
    [MappingKeysType.TIMESERIES, 'gateway.telemetry'],
    [MappingKeysType.CUSTOM, 'gateway.keys']
  ]
);

export const MappingKeysAddKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.add-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.add-telemetry'],
    [MappingKeysType.CUSTOM, 'gateway.add-key']
  ]
);

export const MappingKeysDeleteKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.delete-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.delete-telemetry'],
    [MappingKeysType.CUSTOM, 'gateway.delete-key']
  ]
);

export const MappingKeysNoKeysTextTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.no-attributes'],
    [MappingKeysType.TIMESERIES, 'gateway.no-telemetry'],
    [MappingKeysType.CUSTOM, 'gateway.no-keys']
  ]
);

export enum ServerSideRPCType {
  ONE_WAY = 'oneWay',
  TWO_WAY = 'twoWay'
}


export const getDefaultConfig = (resourcesService: ResourcesService, type: string): Observable<any> =>
  resourcesService.loadJsonResource(`/assets/metadata/connector-default-configs/${type}.json`);

export enum MappingValueType {
  STRING = 'string',
  INTEGER = 'integer',
  DOUBLE = 'double',
  BOOLEAN = 'boolean'
}

export const mappingValueTypesMap = new Map<MappingValueType, ValueTypeData>(
  [
    [
      MappingValueType.STRING,
      {
        name: 'value.string',
        icon: 'mdi:format-text'
      }
    ],
    [
      MappingValueType.INTEGER,
      {
        name: 'value.integer',
        icon: 'mdi:numeric'
      }
    ],
    [
      MappingValueType.DOUBLE,
      {
        name: 'value.double',
        icon: 'mdi:numeric'
      }
    ],
    [
      MappingValueType.BOOLEAN,
      {
        name: 'value.boolean',
        icon: 'mdi:checkbox-marked-outline'
      }
    ]
  ]
);

