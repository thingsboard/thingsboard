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

import { helpBaseUrl, ValueTypeData } from '@shared/models/constants';
import { AttributeData } from '@shared/models/telemetry/telemetry.models';

export const noLeadTrailSpacesRegex = /^\S+(?: \S+)*$/;
export const integerRegex = /^[-+]?\d+$/;
export const nonZeroFloat = /^-?(?!0(\.0+)?$)\d+(\.\d+)?$/;

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
  DEBUG = 'DEBUG',
  TRACE = 'TRACE'
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

export interface GatewayAttributeData extends AttributeData {
  skipSync?: boolean;
}

export interface GatewayConnectorBase {
  name: string;
  type: ConnectorType;
  configuration?: string;
  logLevel: string;
  key?: string;
  class?: string;
  mode?: ConfigurationModes;
  configVersion?: string;
  reportStrategy?: ReportStrategyConfig;
  sendDataOnlyOnChange?: boolean;
  ts?: number;
}

export interface GatewayConnector<BaseConfig = ConnectorBaseConfig> extends GatewayConnectorBase {
  configurationJson: BaseConfig;
  basicConfig?: BaseConfig;
}

export interface GatewayVersionedDefaultConfig {
  legacy: GatewayConnector<ConnectorLegacyConfig>;
  '3.5.2': GatewayConnector<ConnectorBaseConfig_v3_5_2>;
}

export interface DataMapping {
  topicFilter: string;
  QoS: string | number;
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
  pollPeriodInMillis: number;
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
  mode?: ModeType;
}

export enum GatewayVersion {
  Current = '3.5.2',
  Legacy = 'legacy'
}

export type ConnectorMapping = DeviceConnectorMapping | RequestMappingValue | ConverterConnectorMapping;

export type ConnectorMappingFormValue = DeviceConnectorMapping | RequestMappingFormValue | ConverterMappingFormValue;

export type ConnectorBaseConfig = ConnectorBaseConfig_v3_5_2 | ConnectorLegacyConfig;

export type ConnectorLegacyConfig = ConnectorBaseInfo | MQTTLegacyBasicConfig | OPCLegacyBasicConfig | ModbusBasicConfig;

export type ConnectorBaseConfig_v3_5_2 = ConnectorBaseInfo | MQTTBasicConfig_v3_5_2 | OPCBasicConfig_v3_5_2;

export interface ConnectorBaseInfo {
  name: string;
  id: string;
  enableRemoteLogging: boolean;
  logLevel: GatewayLogLevel;
  configVersion: string | number;
  reportStrategy?: ReportStrategyConfig;
}

export type MQTTBasicConfig = MQTTBasicConfig_v3_5_2 | MQTTLegacyBasicConfig;

export interface MQTTBasicConfig_v3_5_2 {
  mapping: ConverterConnectorMapping[];
  requestsMapping: Record<RequestType, RequestMappingData[] | RequestMappingValue[]> | RequestMappingData[] | RequestMappingValue[];
  broker: BrokerConfig;
  workers?: WorkersConfig;
}

export interface MQTTLegacyBasicConfig {
  mapping: LegacyConverterConnectorMapping[];
  broker: BrokerConfig;
  workers?: WorkersConfig;
  connectRequests: LegacyRequestMappingData[];
  disconnectRequests: LegacyRequestMappingData[];
  attributeRequests: LegacyRequestMappingData[];
  attributeUpdates: LegacyRequestMappingData[];
  serverSideRpc: LegacyRequestMappingData[];
}

export type OPCBasicConfig = OPCBasicConfig_v3_5_2 | OPCLegacyBasicConfig;

export interface OPCBasicConfig_v3_5_2 {
  mapping: DeviceConnectorMapping[];
  server: ServerConfig;
}

export interface OPCLegacyBasicConfig {
  server: LegacyServerConfig;
}

export interface LegacyServerConfig extends Omit<ServerConfig, 'enableSubscriptions'> {
  mapping: LegacyDeviceConnectorMapping[];
  disableSubscriptions: boolean;
}

export type ModbusBasicConfig = ModbusBasicConfig_v3_5_2 | ModbusLegacyBasicConfig;

export interface ModbusBasicConfig_v3_5_2 {
  master: ModbusMasterConfig;
  slave: ModbusSlave;
}

export interface ModbusLegacyBasicConfig {
  master: ModbusMasterConfig<LegacySlaveConfig>;
  slave: ModbusLegacySlave;
}

export interface WorkersConfig {
  maxNumberOfWorkers: number;
  maxMessageNumberPerWorker: number;
}

export interface ConnectorDeviceInfo {
  deviceNameExpression: string;
  deviceNameExpressionSource: SourceType | OPCUaSourceType;
  deviceProfileExpression: string;
  deviceProfileExpressionSource: SourceType | OPCUaSourceType;
}

export interface Attribute {
  key: string;
  type: string;
  value: string;
}

export interface LegacyAttribute {
  key: string;
  path: string;
}

export interface Timeseries {
  key: string;
  type: string;
  value: string;
}

export interface LegacyTimeseries {
  key: string;
  path: string;
}

export interface RpcArgument {
  type: string;
  value: number | string | boolean;
}

export interface RpcMethod {
  method: string;
  arguments: RpcArgument[];
}

export interface LegacyRpcMethod {
  method: string;
  arguments: unknown[];
}

export interface AttributesUpdate {
  key: string;
  type: string;
  value: string;
}

export interface LegacyDeviceAttributeUpdate {
  attributeOnThingsBoard: string;
  attributeOnDevice: string;
}

export interface Converter {
  type: ConvertorType;
  deviceInfo?: ConnectorDeviceInfo;
  sendDataOnlyOnChange: boolean;
  timeout: number;
  attributes?: Attribute[];
  timeseries?: Timeseries[];
  extension?: string;
  cached?: boolean;
  extensionConfig?: Record<string, number>;
}

export interface LegacyConverter extends Converter {
  deviceNameJsonExpression?: string;
  deviceTypeJsonExpression?: string;
  deviceNameTopicExpression?: string;
  deviceTypeTopicExpression?: string;
  deviceNameExpression?: string;
  deviceNameExpressionSource?: string;
  deviceTypeExpression?: string;
  deviceProfileExpression?: string;
  deviceProfileExpressionSource?: string;
  ['extension-config']?: Record<string, unknown>;
}

export interface ConverterConnectorMapping {
  topicFilter: string;
  subscriptionQos?: string | number;
  converter: Converter;
}

export interface LegacyConverterConnectorMapping {
  topicFilter: string;
  subscriptionQos?: string | number;
  converter: LegacyConverter;
}

export type ConverterMappingFormValue = Omit<ConverterConnectorMapping, 'converter'> & {
  converter: {
    type: ConvertorType;
  } & Record<ConvertorType, Converter>;
};

export interface DeviceConnectorMapping {
  deviceNodePattern: string;
  deviceNodeSource: OPCUaSourceType;
  deviceInfo: ConnectorDeviceInfo;
  attributes?: Attribute[];
  timeseries?: Timeseries[];
  rpc_methods?: RpcMethod[];
  attributes_updates?: AttributesUpdate[];
}

export interface LegacyDeviceConnectorMapping {
  deviceNamePattern: string;
  deviceNodePattern: string;
  deviceTypePattern: string;
  attributes?: LegacyAttribute[];
  timeseries?: LegacyTimeseries[];
  rpc_methods?: LegacyRpcMethod[];
  attributes_updates?: LegacyDeviceAttributeUpdate[];
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

export const ModbusFunctionCodeTranslationsMap = new Map<number, string>([
  [1, 'gateway.function-codes.read-coils'],
  [2, 'gateway.function-codes.read-discrete-inputs'],
  [3, 'gateway.function-codes.read-multiple-holding-registers'],
  [4, 'gateway.function-codes.read-input-registers'],
  [5, 'gateway.function-codes.write-single-coil'],
  [6, 'gateway.function-codes.write-single-holding-register'],
  [15, 'gateway.function-codes.write-multiple-coils'],
  [16, 'gateway.function-codes.write-multiple-holding-registers']
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

export interface RPCTemplateConfigMQTT {
  methodFilter: string;
  requestTopicExpression: string;
  responseTopicExpression?: string;
  responseTimeout?: number;
  valueExpression: string;
  withResponse: boolean;
}

export interface RPCTemplateConfigModbus {
  tag: string;
  type: ModbusDataType;
  functionCode?: number;
  objectsCount: number;
  address: number;
  value?: string;
}

export interface RPCTemplateConfigOPC {
  method: string;
  arguments: RpcArgument[];
}

export interface OPCTypeValue {
  type: MappingValueType;
  boolean?: boolean;
  double?: number;
  integer?: number;
  string?: string;
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
  gatewayVersion: string;
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

export interface ModbusSlaveInfo<Slave = SlaveConfig> {
  value: Slave;
  buttonTitle: string;
  hideNewFields: boolean;
}

export enum ConfigurationModes {
  BASIC = 'basic',
  ADVANCED = 'advanced'
}

export enum SecurityType {
  ANONYMOUS = 'anonymous',
  BASIC = 'basic',
  CERTIFICATES = 'certificates'
}

export enum ReportStrategyType {
  OnChange = 'ON_CHANGE',
  OnReportPeriod = 'ON_REPORT_PERIOD',
  OnChangeOrReportPeriod = 'ON_CHANGE_OR_REPORT_PERIOD'
}

export enum ReportStrategyDefaultValue {
  Connector = 60000,
  Device = 30000,
  Key = 15000
}

export const ReportStrategyTypeTranslationsMap = new Map<ReportStrategyType, string>(
  [
    [ReportStrategyType.OnChange, 'gateway.report-strategy.on-change'],
    [ReportStrategyType.OnReportPeriod, 'gateway.report-strategy.on-report-period'],
    [ReportStrategyType.OnChangeOrReportPeriod, 'gateway.report-strategy.on-change-or-report-period']
  ]
);

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

export const HelpLinkByMappingTypeMap = new Map<MappingType, string>(
  [
    [MappingType.DATA, helpBaseUrl + '/docs/iot-gateway/config/mqtt/#section-mapping'],
    [MappingType.OPCUA, helpBaseUrl + '/docs/iot-gateway/config/opc-ua/#section-mapping'],
    [MappingType.REQUESTS, helpBaseUrl + '/docs/iot-gateway/config/mqtt/#requests-mapping']
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

export enum SourceType {
  MSG = 'message',
  TOPIC = 'topic',
  CONST = 'constant'
}

export enum OPCUaSourceType {
  PATH = 'path',
  IDENTIFIER = 'identifier',
  CONST = 'constant'
}

export enum DeviceInfoType {
  FULL = 'full',
  PARTIAL = 'partial'
}

export const SourceTypeTranslationsMap = new Map<SourceType | OPCUaSourceType, string>(
  [
    [SourceType.MSG, 'gateway.source-type.msg'],
    [SourceType.TOPIC, 'gateway.source-type.topic'],
    [SourceType.CONST, 'gateway.source-type.const'],
    [OPCUaSourceType.PATH, 'gateway.source-type.path'],
    [OPCUaSourceType.IDENTIFIER, 'gateway.source-type.identifier'],
    [OPCUaSourceType.CONST, 'gateway.source-type.const']
  ]
);

export interface RequestMappingValue {
  requestType: RequestType;
  requestValue: RequestMappingData;
}

export interface RequestMappingFormValue {
  requestType: RequestType;
  requestValue: Record<RequestType, RequestMappingData>;
}

export type RequestMappingData = ConnectRequest | DisconnectRequest | AttributeRequest | AttributeUpdate | ServerSideRpc;

export type LegacyRequestMappingData =
  LegacyConnectRequest
  | LegacyDisconnectRequest
  | LegacyAttributeRequest
  | LegacyAttributeUpdate
  | LegacyServerSideRpc;

export interface ConnectRequest {
  topicFilter: string;
  deviceInfo: ConnectorDeviceInfo;
}

export interface DisconnectRequest {
  topicFilter: string;
  deviceInfo: ConnectorDeviceInfo;
}

export interface AttributeRequest {
  retain: boolean;
  topicFilter: string;
  deviceInfo: ConnectorDeviceInfo;
  attributeNameExpressionSource: SourceType;
  attributeNameExpression: string;
  topicExpression: string;
  valueExpression: string;
}

export interface AttributeUpdate {
  retain: boolean;
  deviceNameFilter: string;
  attributeFilter: string;
  topicExpression: string;
  valueExpression: string;
}

export interface ServerSideRpc {
  type: ServerSideRpcType;
  deviceNameFilter: string;
  methodFilter: string;
  requestTopicExpression: string;
  responseTopicExpression?: string;
  responseTopicQoS?: number;
  responseTimeout?: number;
  valueExpression: string;
}

export enum ServerSideRpcType {
  WithResponse = 'twoWay',
  WithoutResponse = 'oneWay'
}

export interface LegacyConnectRequest {
  topicFilter: string;
  deviceNameJsonExpression?: string;
  deviceNameTopicExpression?: string;
}

interface LegacyDisconnectRequest {
  topicFilter: string;
  deviceNameJsonExpression?: string;
  deviceNameTopicExpression?: string;
}

interface LegacyAttributeRequest {
  retain: boolean;
  topicFilter: string;
  deviceNameJsonExpression: string;
  attributeNameJsonExpression: string;
  topicExpression: string;
  valueExpression: string;
}

interface LegacyAttributeUpdate {
  retain: boolean;
  deviceNameFilter: string;
  attributeFilter: string;
  topicExpression: string;
  valueExpression: string;
}

interface LegacyServerSideRpc {
  deviceNameFilter: string;
  methodFilter: string;
  requestTopicExpression: string;
  responseTopicExpression?: string;
  responseTimeout?: number;
  valueExpression: string;
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

export enum MappingValueType {
  STRING = 'string',
  INTEGER = 'integer',
  DOUBLE = 'double',
  BOOLEAN = 'boolean'
}

export enum ModifierType {
  DIVIDER = 'divider',
  MULTIPLIER = 'multiplier',
}

export const ModifierTypesMap = new Map<ModifierType, ValueTypeData>(
  [
    [
      ModifierType.DIVIDER,
      {
        name: 'gateway.divider',
        icon: 'mdi:division'
      }
    ],
    [
      ModifierType.MULTIPLIER,
      {
        name: 'gateway.multiplier',
        icon: 'mdi:multiplication'
      }
    ],
  ]
);

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

export enum ModbusProtocolType {
  TCP = 'tcp',
  UDP = 'udp',
  Serial = 'serial',
}

export const ModbusProtocolLabelsMap = new Map<ModbusProtocolType, string>(
  [
    [ModbusProtocolType.TCP, 'TCP'],
    [ModbusProtocolType.UDP, 'UDP'],
    [ModbusProtocolType.Serial, 'Serial'],
  ]
);

export enum ModbusMethodType {
  SOCKET = 'socket',
  RTU = 'rtu',
}

export enum ModbusSerialMethodType {
  RTU = 'rtu',
  ASCII = 'ascii',
}

export const ModbusMethodLabelsMap = new Map<ModbusMethodType | ModbusSerialMethodType, string>(
  [
    [ModbusMethodType.SOCKET, 'Socket'],
    [ModbusMethodType.RTU, 'RTU'],
    [ModbusSerialMethodType.ASCII, 'ASCII'],
  ]
);

export const ModbusByteSizes = [5, 6, 7 ,8];

export enum ModbusParity {
  Even = 'E',
  Odd = 'O',
  None = 'N'
}

export const ModbusParityLabelsMap = new Map<ModbusParity, string>(
  [
    [ModbusParity.Even, 'Even'],
    [ModbusParity.Odd, 'Odd'],
    [ModbusParity.None, 'None'],
  ]
);

export enum ModbusOrderType {
  BIG = 'BIG',
  LITTLE = 'LITTLE',
}

export enum ModbusRegisterType {
  HoldingRegisters = 'holding_registers',
  CoilsInitializer = 'coils_initializer',
  InputRegisters = 'input_registers',
  DiscreteInputs = 'discrete_inputs'
}

export const ModbusRegisterTranslationsMap = new Map<ModbusRegisterType, string>(
  [
    [ModbusRegisterType.HoldingRegisters, 'gateway.holding_registers'],
    [ModbusRegisterType.CoilsInitializer, 'gateway.coils_initializer'],
    [ModbusRegisterType.InputRegisters, 'gateway.input_registers'],
    [ModbusRegisterType.DiscreteInputs, 'gateway.discrete_inputs']
  ]
);

export enum ModbusDataType {
  STRING = 'string',
  BYTES = 'bytes',
  BITS = 'bits',
  INT8 = '8int',
  UINT8 = '8uint',
  FLOAT8 = '8float',
  INT16 = '16int',
  UINT16 = '16uint',
  FLOAT16 = '16float',
  INT32 = '32int',
  UINT32 = '32uint',
  FLOAT32 = '32float',
  INT64 = '64int',
  UINT64 = '64uint',
  FLOAT64 = '64float'
}

export const ModbusEditableDataTypes = [ModbusDataType.BYTES, ModbusDataType.BITS, ModbusDataType.STRING];

export enum ModbusObjectCountByDataType {
  '8int' = 1,
  '8uint' = 1,
  '8float' = 1,
  '16int' = 1,
  '16uint' = 1,
  '16float' = 1,
  '32int' = 2,
  '32uint' = 2,
  '32float' = 2,
  '64int' = 4,
  '64uint' = 4,
  '64float' = 4,
}

export enum ModbusValueKey {
  ATTRIBUTES = 'attributes',
  TIMESERIES = 'timeseries',
  ATTRIBUTES_UPDATES = 'attributeUpdates',
  RPC_REQUESTS = 'rpc',
}

export const ModbusKeysPanelTitleTranslationsMap = new Map<ModbusValueKey, string>(
  [
    [ModbusValueKey.ATTRIBUTES, 'gateway.attributes'],
    [ModbusValueKey.TIMESERIES, 'gateway.timeseries'],
    [ModbusValueKey.ATTRIBUTES_UPDATES, 'gateway.attribute-updates'],
    [ModbusValueKey.RPC_REQUESTS, 'gateway.rpc-requests']
  ]
);

export const ModbusKeysAddKeyTranslationsMap = new Map<ModbusValueKey, string>(
  [
    [ModbusValueKey.ATTRIBUTES, 'gateway.add-attribute'],
    [ModbusValueKey.TIMESERIES, 'gateway.add-timeseries'],
    [ModbusValueKey.ATTRIBUTES_UPDATES, 'gateway.add-attribute-update'],
    [ModbusValueKey.RPC_REQUESTS, 'gateway.add-rpc-request']
  ]
);

export const ModbusKeysDeleteKeyTranslationsMap = new Map<ModbusValueKey, string>(
  [
    [ModbusValueKey.ATTRIBUTES, 'gateway.delete-attribute'],
    [ModbusValueKey.TIMESERIES, 'gateway.delete-timeseries'],
    [ModbusValueKey.ATTRIBUTES_UPDATES, 'gateway.delete-attribute-update'],
    [ModbusValueKey.RPC_REQUESTS, 'gateway.delete-rpc-request']
  ]
);

export const ModbusKeysNoKeysTextTranslationsMap = new Map<ModbusValueKey, string>(
  [
    [ModbusValueKey.ATTRIBUTES, 'gateway.no-attributes'],
    [ModbusValueKey.TIMESERIES, 'gateway.no-timeseries'],
    [ModbusValueKey.ATTRIBUTES_UPDATES, 'gateway.no-attribute-updates'],
    [ModbusValueKey.RPC_REQUESTS, 'gateway.no-rpc-requests']
  ]
);

export interface ModbusMasterConfig<Slave = SlaveConfig> {
  slaves: Slave[];
}

export interface LegacySlaveConfig extends Omit<SlaveConfig, 'reportStrategy'> {
  sendDataOnlyOnChange: boolean;
}

export interface SlaveConfig {
  name: string;
  host?: string;
  port: string | number;
  serialPort?: string;
  type: ModbusProtocolType;
  method: ModbusMethodType;
  timeout: number;
  byteOrder: ModbusOrderType;
  wordOrder: ModbusOrderType;
  retries: boolean;
  retryOnEmpty: boolean;
  retryOnInvalid: boolean;
  pollPeriod: number;
  unitId: number;
  deviceName: string;
  deviceType?: string;
  reportStrategy: ReportStrategyConfig;
  connectAttemptTimeMs: number;
  connectAttemptCount: number;
  waitAfterFailedAttemptsMs: number;
  attributes: ModbusValue[];
  timeseries: ModbusValue[];
  attributeUpdates: ModbusValue[];
  rpc: ModbusValue[];
  security?: ModbusSecurity;
  baudrate?: number;
  stopbits?: number;
  bytesize?: number;
  parity?: ModbusParity;
  strict?: boolean;
}

export interface ModbusValue {
  tag: string;
  type: ModbusDataType;
  functionCode?: number;
  objectsCount: number;
  address: number;
  value?: string;
  reportStrategy?: ReportStrategyConfig;
  multiplier?: number;
  divider?: number;
}

export interface ModbusFormValue extends ModbusValue {
  modifierType?: ModifierType;
  modifierValue?: string;
}

export interface ModbusSecurity {
  certfile?: string;
  keyfile?: string;
  password?: string;
  server_hostname?: string;
  reqclicert?: boolean;
}

export interface ModbusSlave {
  host?: string;
  type: ModbusProtocolType;
  method: ModbusMethodType;
  unitId: number;
  serialPort?: string;
  baudrate?: number;
  deviceName: string;
  deviceType: string;
  pollPeriod: number;
  sendDataToThingsBoard: boolean;
  byteOrder: ModbusOrderType;
  wordOrder: ModbusOrderType;
  identity: ModbusIdentity;
  values?: ModbusValuesState;
  port: string | number;
  security: ModbusSecurity;
}

export interface ModbusLegacySlave extends Omit<ModbusSlave, 'values'> {
  values?: ModbusLegacyRegisterValues;
}

export type ModbusValuesState = ModbusRegisterValues | ModbusValues;

export interface ModbusLegacyRegisterValues {
  holding_registers: ModbusValues[];
  coils_initializer: ModbusValues[];
  input_registers: ModbusValues[];
  discrete_inputs: ModbusValues[];
}

export interface ModbusRegisterValues {
  holding_registers: ModbusValues;
  coils_initializer: ModbusValues;
  input_registers: ModbusValues;
  discrete_inputs: ModbusValues;
}

export interface ModbusValues {
  attributes: ModbusValue[];
  timeseries: ModbusValue[];
  attributeUpdates: ModbusValue[];
  rpc: ModbusValue[];
}

export interface ModbusIdentity {
  vendorName?: string;
  productCode?: string;
  vendorUrl?: string;
  productName?: string;
  modelName?: string;
}

export interface ReportStrategyConfig {
  type: ReportStrategyType;
  reportPeriod?: number;
}

export const ModbusBaudrates = [4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600];
