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
import { Validators } from '@angular/forms';

export const noLeadTrailSpacesRegex: RegExp = /^(?! )[\S\s]*(?<! )$/;

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

export enum PortLimits {
  MIN = 1,
  MAX = 65535
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
  type: ConnectorType;
  configuration?: string;
  configurationJson: string | {[key: string]: any};
  basicConfig?: string | {[key: string]: any};
  logLevel: string;
  key?: string;
  class?: string;
}

export enum ConnectorType {
  MQTT = 'mqtt',
  MODBUS = 'modbus',
  GRPC = 'grpc',
  OPCUA = 'opcua',
  OPCUA_ASYNCIO = 'opcua_asyncio',
  BLE = 'ble',
  REQUEST = 'request',
  CAN = 'can',
  BACNET = 'bacnet',
  ODBC = 'odbc',
  REST = 'rest',
  SNMP = 'snmp',
  FTP = 'ftp',
  SOCKET = 'socket',
  XMPP = 'xmpp',
  OCPP = 'ocpp',
  CUSTOM = 'custom'
}

export const GatewayConnectorDefaultTypesTranslatesMap = new Map<ConnectorType, string>([
  [ConnectorType.MQTT, 'MQTT'],
  [ConnectorType.MODBUS, 'MODBUS'],
  [ConnectorType.GRPC, 'GRPC'],
  [ConnectorType.OPCUA, 'OPCUA'],
  [ConnectorType.OPCUA_ASYNCIO, 'OPCUA ASYNCIO'],
  [ConnectorType.BLE, 'BLE'],
  [ConnectorType.REQUEST, 'REQUEST'],
  [ConnectorType.CAN, 'CAN'],
  [ConnectorType.BACNET, 'BACNET'],
  [ConnectorType.ODBC, 'ODBC'],
  [ConnectorType.REST, 'REST'],
  [ConnectorType.SNMP, 'SNMP'],
  [ConnectorType.FTP, 'FTP'],
  [ConnectorType.SOCKET, 'SOCKET'],
  [ConnectorType.XMPP, 'XMPP'],
  [ConnectorType.OCPP, 'OCPP'],
  [ConnectorType.CUSTOM, 'CUSTOM']
]);

export interface RPCCommand {
  command: string,
  params: any,
  time: number
}


export enum ModbusCommandTypes {
  Bits = 'bits',
  Bit = 'bit',
  String = 'string',
  Bytes = 'bytes',
  Int8 = '8int',
  Uint8 = '8uint',
  Int16 = '16int',
  Uint16 = '16uint',
  Float16 = '16float',
  Int32 = '32int',
  Uint32 = '32uint',
  Float32 = '32float',
  Int64 = '64int',
  Uint64 = '64uint',
  Float64 = '64float'
}

export const ModbusCodesTranslate = new Map<number, string>([
  [1, 'gateway.rpc.read-coils'],
  [2, 'gateway.rpc.read-discrete-inputs'],
  [3, 'gateway.rpc.read-multiple-holding-registers'],
  [4, 'gateway.rpc.read-input-registers'],
  [5, 'gateway.rpc.write-single-coil'],
  [6, 'gateway.rpc.write-single-holding-register'],
  [15, 'gateway.rpc.write-multiple-coils'],
  [16, 'gateway.rpc.write-multiple-holding-registers']
])

export enum BACnetRequestTypes {
  WriteProperty = 'writeProperty',
  ReadProperty = 'readProperty'
}

export const BACnetRequestTypesTranslates = new Map<BACnetRequestTypes, string>([
  [BACnetRequestTypes.WriteProperty, 'gateway.rpc.write-property'],
  [BACnetRequestTypes.ReadProperty, "gateway.rpc.read-property"]
])

export enum BACnetObjectTypes {
  BinaryInput = 'binaryInput',
  BinaryOutput = 'binaryOutput',
  AnalogInput = 'analogInput',
  AnalogOutput = 'analogOutput',
  BinaryValue = 'binaryValue',
  AnalogValue = 'analogValue'
}

export const BACnetObjectTypesTranslates = new Map<BACnetObjectTypes, string>([
  [BACnetObjectTypes.AnalogOutput, 'gateway.rpc.analog-output'],
  [BACnetObjectTypes.AnalogInput, 'gateway.rpc.analog-input'],
  [BACnetObjectTypes.BinaryOutput, 'gateway.rpc.binary-output'],
  [BACnetObjectTypes.BinaryInput, 'gateway.rpc.binary-input'],
  [BACnetObjectTypes.BinaryValue, 'gateway.rpc.binary-value'],
  [BACnetObjectTypes.AnalogValue, 'gateway.rpc.analog-value']
])

export enum BLEMethods {
  WRITE = 'write',
  READ = 'read',
  SCAN = 'scan'
}

export const BLEMethodsTranslates = new Map<BLEMethods, string>([
  [BLEMethods.WRITE, 'gateway.rpc.write'],
  [BLEMethods.READ, 'gateway.rpc.read'],
  [BLEMethods.SCAN, 'gateway.rpc.scan'],
])

export enum CANByteOrders {
  LITTLE = 'LITTLE',
  BIG = 'BIG'
}

export enum SocketMethodProcessings {
  WRITE = 'write'
}

export const SocketMethodProcessingsTranslates = new Map<SocketMethodProcessings, string>([
  [SocketMethodProcessings.WRITE, 'gateway.rpc.write']
])

export enum SNMPMethods {
  SET = 'set',
  MULTISET = "multiset",
  GET = "get",
  BULKWALK = "bulkwalk",
  TABLE = "table",
  MULTIGET = "multiget",
  GETNEXT = "getnext",
  BULKGET = "bulkget",
  WALKS = "walk"
}

export const SNMPMethodsTranslations = new Map<SNMPMethods, string>([
  [SNMPMethods.SET, 'gateway.rpc.set'],
  [SNMPMethods.MULTISET, 'gateway.rpc.multiset'],
  [SNMPMethods.GET, 'gateway.rpc.get'],
  [SNMPMethods.BULKWALK, 'gateway.rpc.bulk-walk'],
  [SNMPMethods.TABLE, 'gateway.rpc.table'],
  [SNMPMethods.MULTIGET, 'gateway.rpc.multi-get'],
  [SNMPMethods.GETNEXT, 'gateway.rpc.get-next'],
  [SNMPMethods.BULKGET, 'gateway.rpc.bul-kget'],
  [SNMPMethods.WALKS, 'gateway.rpc.walk']
])

export enum HTTPMethods {
  CONNECT = 'CONNECT',
  DELETE = 'DELETE',
  GET = 'GET',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
  PATCH = 'PATCH',
  POST = 'POST',
  PUT = 'PUT',
  TRACE = 'TRACE'

}

export enum SocketEncodings {
  UTF_8 = 'utf-8'
}

export interface RPCTemplate {
  name?: string;
  config: RPCTemplateConfig;
}

export interface RPCTemplateConfig {
  [key: string]: any;
}

export interface SaveRPCTemplateData {
  config: RPCTemplateConfig,
  templates: Array<RPCTemplate>
}

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

export interface CreatedConnectorConfigData {
  type: ConnectorType,
  name: string,
  logLevel: GatewayLogLevel,
  useDefaults: boolean,
  sendDataOnlyOnChange: boolean,
  configurationJson?: {[key: string]: any}
}

export interface MappingDataKey {
  key: string,
  value: any,
  type: MappingValueType
}
export interface MappingInfo {
  mappingType: MappingType,
  value: {[key: string]: any},
  buttonTitle: string
}

export enum ConnectorConfigurationModes {
  BASIC = 'basic',
  ADVANCED = 'advanced'
}

export enum BrokerSecurityType {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
  CERTIFICATES = 'certificates'
}

export const BrokerSecurityTypeTranslationsMap = new Map<BrokerSecurityType, string>(
  [
    [BrokerSecurityType.ANONYMOUS, 'gateway.broker.security-types.anonymous'],
    [BrokerSecurityType.BASIC, 'gateway.broker.security-types.basic'],
    [BrokerSecurityType.CERTIFICATES, 'gateway.broker.security-types.certificates']
  ]
);

export const MqttVersions = [
  { name: 3.1, value: 3 },
  { name: 3.11, value: 4 },
  { name: 5, value: 5 }
];

export enum MappingType {
  DATA = 'data',
  REQUESTS = 'requests'
}

export const MappingTypeTranslationsMap = new Map<MappingType, string>(
  [
    [MappingType.DATA, 'gateway.data-mapping'],
    [MappingType.REQUESTS, 'gateway.requests-mapping']
  ]
);

export const MappingHintTranslationsMap = new Map<MappingType, string>(
  [
    [MappingType.DATA, 'gateway.data-mapping-hint'],
    [MappingType.REQUESTS, 'gateway.requests-mapping-hint']
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

export enum ConvertorType {
  JSON = 'json',
  BYTES = 'bytes',
  CUSTOM = 'custom'
}

export const ConvertorTypeTranslationsMap = new Map<ConvertorType, string>(
  [
    [ConvertorType.JSON, 'gateway.JSON'],
    [ConvertorType.BYTES, 'gateway.bytes'],
    [ConvertorType.CUSTOM, 'gateway.custom']
  ]
);

export enum SourceTypes {
  MSG = 'message',
  TOPIC = 'topic',
  CONST = 'constant'
}

export enum DeviceInfoType {
  FULL = 'full',
  PARTIAL = 'partial'
}

export const SourceTypeTranslationsMap = new Map<SourceTypes, string>(
  [
    [SourceTypes.MSG, 'gateway.source-type.msg'],
    [SourceTypes.TOPIC, 'gateway.source-type.topic'],
    [SourceTypes.CONST, 'gateway.source-type.const'],
  ]
);

export enum RequestType {
  CONNECT_REQUEST = 'connectRequests',
  DISCONNECT_REQUEST = 'disconnectRequests',
  ATTRIBUTE_REQUEST = 'attributeRequests',
  ATTRIBUTE_UPDATE = 'attributeUpdates',
  SERVER_SIDE_RPC = 'serverSideRpc'
}

export const RequestTypesTranslationsMap = new Map<RequestType, string>(
  [
    [RequestType.CONNECT_REQUEST, 'gateway.request.connect-request'],
    [RequestType.DISCONNECT_REQUEST, 'gateway.request.disconnect-request'],
    [RequestType.ATTRIBUTE_REQUEST, 'gateway.request.attribute-request'],
    [RequestType.ATTRIBUTE_UPDATE, 'gateway.request.attribute-update'],
    [RequestType.SERVER_SIDE_RPC, 'gateway.request.rpc-connection'],
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
    [MappingKeysType.TIMESERIES, 'gateway.timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.keys']
  ]
);

export const MappingKeysAddKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.add-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.add-timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.add-key']
  ]
);

export const MappingKeysDeleteKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.delete-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.delete-timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.delete-key']
  ]
);

export const MappingKeysNoKeysTextTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.no-attributes'],
    [MappingKeysType.TIMESERIES, 'gateway.no-timeseries'],
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

export const DataConversionTranslationsMap = new Map<ConvertorType, string>(
  [
    [ConvertorType.JSON, 'gateway.JSON-hint'],
    [ConvertorType.BYTES, 'gateway.bytes-hint'],
    [ConvertorType.CUSTOM, 'gateway.custom-hint']
  ]
);
