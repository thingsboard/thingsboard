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

export const noLeadTrailSpacesRegex = /^(?! )[\S\s]*(?<! )$/;

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
  configurationJson: ConnectorBaseConfig;
  basicConfig?: ConnectorBaseConfig;
  logLevel: string;
  key?: string;
  class?: string;
  mode?: ConnectorConfigurationModes;
}

export interface DataMapping {
  topicFilter: string;
  QoS: string;
  converter: Converter;
}

export interface RequestsMapping {
  requestType: RequestType;
  type: string;
  details: string;
}

export interface OpcUaMapping {
  deviceNodePattern?: string;
  deviceNamePattern?: string;
  deviceProfileExpression?: string;
}

export type MappingValue = DataMapping | RequestsMapping | OpcUaMapping;

export interface ServerConfig {
  name: string;
  url: string;
  timeoutInMillis: number;
  scanPeriodInMillis: number;
  enableSubscriptions: boolean;
  subCheckPeriodInMillis: number;
  showMap: boolean;
  security: string;
  identity: ConnectorSecurity;
}

export interface BrokerConfig {
  name: string;
  host: string;
  port: number;
  version: number;
  clientId: string;
  maxNumberOfWorkers: number;
  maxMessageNumberPerWorker: number;
  security: ConnectorSecurity;
}

export interface ConnectorSecurity {
  type: SecurityType;
  username?: string;
  password?: string;
  pathToCACert?: string;
  pathToPrivateKey?: string;
  pathToClientCert?: string;
}

export type ConnectorMapping = DeviceConnectorMapping | RequestMappingData | ConverterConnectorMapping;

export interface ConnectorBaseConfig {
  mapping?: DeviceConnectorMapping[];
  dataMapping?: ConverterConnectorMapping[];
  requestsMapping?: Record<RequestType, RequestMappingData> | RequestMappingData[];
  server?: ServerConfig;
  broker?: BrokerConfig;
  workers?: WorkersConfig;
}

export interface WorkersConfig {
  maxNumberOfWorkers: number;
  maxMessageNumberPerWorker: number;
}

interface DeviceInfo {
  deviceNameExpression: string;
  deviceNameExpressionSource: string;
  deviceProfileExpression: string;
  deviceProfileExpressionSource: string;
}

export interface Attribute {
  key: string;
  type: string;
  value: string;
}

export interface Timeseries {
  key: string;
  type: string;
  value: string;
}

interface RpcArgument {
  type: string;
  value: number;
}

export interface RpcMethod {
  method: string;
  arguments: RpcArgument[];
}

export interface AttributesUpdate {
  key: string;
  type: string;
  value: string;
}

interface Converter {
  type: ConvertorType;
  deviceNameJsonExpression: string;
  deviceTypeJsonExpression: string;
  sendDataOnlyOnChange: boolean;
  timeout: number;
  attributes: Attribute[];
  timeseries: Timeseries[];
}

export interface ConverterConnectorMapping {
  topicFilter: string;
  subscriptionQos?: string;
  converter: Converter;
}

export interface DeviceConnectorMapping {
  deviceNodePattern: string;
  deviceNodeSource: string;
  deviceInfo: DeviceInfo;
  attributes: Attribute[];
  timeseries: Timeseries[];
  rpc_methods: RpcMethod[];
  attributes_updates: AttributesUpdate[];
}

export enum ConnectorType {
  MQTT = 'mqtt',
  MODBUS = 'modbus',
  GRPC = 'grpc',
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
  XMPP = 'xmpp',
  OCPP = 'ocpp',
  CUSTOM = 'custom'
}

export const GatewayConnectorDefaultTypesTranslatesMap = new Map<ConnectorType, string>([
  [ConnectorType.MQTT, 'MQTT'],
  [ConnectorType.MODBUS, 'MODBUS'],
  [ConnectorType.GRPC, 'GRPC'],
  [ConnectorType.OPCUA, 'OPCUA'],
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
  command: string;
  params: any;
  time: number;
}


export enum ModbusCommandTypes {
  Bits = 'bits',
  Bit = 'bit',
  // eslint-disable-next-line id-blacklist
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
]);

export enum BACnetRequestTypes {
  WriteProperty = 'writeProperty',
  ReadProperty = 'readProperty'
}

export const BACnetRequestTypesTranslates = new Map<BACnetRequestTypes, string>([
  [BACnetRequestTypes.WriteProperty, 'gateway.rpc.write-property'],
  [BACnetRequestTypes.ReadProperty, 'gateway.rpc.read-property']
]);

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
]);

export enum BLEMethods {
  WRITE = 'write',
  READ = 'read',
  SCAN = 'scan'
}

export const BLEMethodsTranslates = new Map<BLEMethods, string>([
  [BLEMethods.WRITE, 'gateway.rpc.write'],
  [BLEMethods.READ, 'gateway.rpc.read'],
  [BLEMethods.SCAN, 'gateway.rpc.scan'],
]);

export enum CANByteOrders {
  LITTLE = 'LITTLE',
  BIG = 'BIG'
}

export enum SocketMethodProcessings {
  WRITE = 'write'
}

export const SocketMethodProcessingsTranslates = new Map<SocketMethodProcessings, string>([
  [SocketMethodProcessings.WRITE, 'gateway.rpc.write']
]);

export enum SNMPMethods {
  SET = 'set',
  MULTISET = 'multiset',
  GET = 'get',
  BULKWALK = 'bulkwalk',
  TABLE = 'table',
  MULTIGET = 'multiget',
  GETNEXT = 'getnext',
  BULKGET = 'bulkget',
  WALKS = 'walk'
}

export const SNMPMethodsTranslations = new Map<SNMPMethods, string>([
  [SNMPMethods.SET, 'gateway.rpc.set'],
  [SNMPMethods.MULTISET, 'gateway.rpc.multiset'],
  [SNMPMethods.GET, 'gateway.rpc.get'],
  [SNMPMethods.BULKWALK, 'gateway.rpc.bulk-walk'],
  [SNMPMethods.TABLE, 'gateway.rpc.table'],
  [SNMPMethods.MULTIGET, 'gateway.rpc.multi-get'],
  [SNMPMethods.GETNEXT, 'gateway.rpc.get-next'],
  [SNMPMethods.BULKGET, 'gateway.rpc.bulk-get'],
  [SNMPMethods.WALKS, 'gateway.rpc.walk']
]);

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
  config: RPCTemplateConfig;
  templates: Array<RPCTemplate>;
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
  dataSourceData: Array<any>;
}

export interface CreatedConnectorConfigData {
  type: ConnectorType;
  name: string;
  logLevel: GatewayLogLevel;
  useDefaults: boolean;
  sendDataOnlyOnChange: boolean;
  configurationJson?: {[key: string]: any};
}

export interface MappingDataKey {
  key: string;
  value: any;
  type: MappingValueType;
}

export interface RpcMethodsMapping {
  method: string;
  arguments: Array<MappingDataKey>;
}

export interface MappingInfo {
  mappingType: MappingType;
  value: {[key: string]: any};
  buttonTitle: string;
}

export enum ConnectorConfigurationModes {
  BASIC = 'basic',
  ADVANCED = 'advanced'
}

export enum SecurityType {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
  CERTIFICATES = 'certificates'
}

export enum ModeType {
  NONE = 'None',
  SIGN = 'Sign',
  SIGNANDENCRYPT = 'SignAndEncrypt'
}

export const SecurityTypeTranslationsMap = new Map<SecurityType, string>(
  [
    [SecurityType.ANONYMOUS, 'gateway.broker.security-types.anonymous'],
    [SecurityType.BASIC, 'gateway.broker.security-types.basic'],
    [SecurityType.CERTIFICATES, 'gateway.broker.security-types.certificates']
  ]
);

export enum RestSecurityType {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
}

export const RestSecurityTypeTranslationsMap = new Map<RestSecurityType, string>(
  [
    [RestSecurityType.ANONYMOUS, 'gateway.broker.security-types.anonymous'],
    [RestSecurityType.BASIC, 'gateway.broker.security-types.basic'],
  ]
);

export const MqttVersions = [
  { name: 3.1, value: 3 },
  { name: 3.11, value: 4 },
  { name: 5, value: 5 }
];

export enum MappingType {
  DATA = 'data',
  REQUESTS = 'requests',
  OPCUA = 'OPCua'
}

export const MappingTypeTranslationsMap = new Map<MappingType, string>(
  [
    [MappingType.DATA, 'gateway.data-mapping'],
    [MappingType.REQUESTS, 'gateway.requests-mapping'],
    [MappingType.OPCUA, 'gateway.data-mapping']
  ]
);

export const MappingHintTranslationsMap = new Map<MappingType, string>(
  [
    [MappingType.DATA, 'gateway.data-mapping-hint'],
    [MappingType.OPCUA, 'gateway.opcua-data-mapping-hint'],
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

export enum OPCUaSourceTypes {
  PATH = 'path',
  IDENTIFIER = 'identifier',
  CONST = 'constant'
}

export enum DeviceInfoType {
  FULL = 'full',
  PARTIAL = 'partial'
}

export const SourceTypeTranslationsMap = new Map<SourceTypes | OPCUaSourceTypes, string>(
  [
    [SourceTypes.MSG, 'gateway.source-type.msg'],
    [SourceTypes.TOPIC, 'gateway.source-type.topic'],
    [SourceTypes.CONST, 'gateway.source-type.const'],
    [OPCUaSourceTypes.PATH, 'gateway.source-type.path'],
    [OPCUaSourceTypes.IDENTIFIER, 'gateway.source-type.identifier'],
    [OPCUaSourceTypes.CONST, 'gateway.source-type.const']
  ]
);

export interface RequestMappingData {
  requestType: RequestType;
  requestValue: RequestDataItem;
}

export interface RequestDataItem {
  type: string;
  details: string;
  requestType: RequestType;
  methodFilter?: string;
  attributeFilter?: string;
  topicFilter?: string;
}

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
  CUSTOM = 'extensionConfig',
  RPC_METHODS = 'rpc_methods',
  ATTRIBUTES_UPDATES = 'attributes_updates'
}

export const MappingKeysPanelTitleTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.attributes'],
    [MappingKeysType.TIMESERIES, 'gateway.timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.keys'],
    [MappingKeysType.ATTRIBUTES_UPDATES, 'gateway.attribute-updates'],
    [MappingKeysType.RPC_METHODS, 'gateway.rpc-methods']
  ]
);

export const MappingKeysAddKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.add-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.add-timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.add-key'],
    [MappingKeysType.ATTRIBUTES_UPDATES, 'gateway.add-attribute-update'],
    [MappingKeysType.RPC_METHODS, 'gateway.add-rpc-method']
  ]
);

export const MappingKeysDeleteKeyTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.delete-attribute'],
    [MappingKeysType.TIMESERIES, 'gateway.delete-timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.delete-key'],
    [MappingKeysType.ATTRIBUTES_UPDATES, 'gateway.delete-attribute-update'],
    [MappingKeysType.RPC_METHODS, 'gateway.delete-rpc-method']
  ]
);

export const MappingKeysNoKeysTextTranslationsMap = new Map<MappingKeysType, string>(
  [
    [MappingKeysType.ATTRIBUTES, 'gateway.no-attributes'],
    [MappingKeysType.TIMESERIES, 'gateway.no-timeseries'],
    [MappingKeysType.CUSTOM, 'gateway.no-keys'],
    [MappingKeysType.ATTRIBUTES_UPDATES, 'gateway.no-attribute-updates'],
    [MappingKeysType.RPC_METHODS, 'gateway.no-rpc-methods']
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

export enum SecurityPolicy {
  BASIC128 = 'Basic128Rsa15',
  BASIC256 = 'Basic256',
  BASIC256SHA = 'Basic256Sha256'
}

export const SecurityPolicyTypes = [
  { value: SecurityPolicy.BASIC128, name: 'Basic128RSA15' },
  { value: SecurityPolicy.BASIC256, name: 'Basic256' },
  { value: SecurityPolicy.BASIC256SHA, name: 'Basic256SHA256' }
];
