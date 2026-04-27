///
/// Copyright © 2016-2026 The Thingsboard Authors
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

export enum InstallMethod {
  // Direct device-to-platform transports
  DIRECT_HTTP = 'DIRECT_HTTP',
  DIRECT_MQTT = 'DIRECT_MQTT',
  DIRECT_COAP = 'DIRECT_COAP',
  DIRECT_LWM2M = 'DIRECT_LWM2M',
  DIRECT_SNMP = 'DIRECT_SNMP',
  // ThingsBoard IoT Gateway connectors
  GATEWAY_MQTT = 'GATEWAY_MQTT',
  GATEWAY_MODBUS = 'GATEWAY_MODBUS',
  GATEWAY_OPCUA = 'GATEWAY_OPCUA',
  GATEWAY_BACNET = 'GATEWAY_BACNET',
  GATEWAY_BLE = 'GATEWAY_BLE',
  GATEWAY_CAN = 'GATEWAY_CAN',
  GATEWAY_FTP = 'GATEWAY_FTP',
  GATEWAY_KNX = 'GATEWAY_KNX',
  GATEWAY_OCPP = 'GATEWAY_OCPP',
  GATEWAY_ODBC = 'GATEWAY_ODBC',
  GATEWAY_REQUEST = 'GATEWAY_REQUEST',
  GATEWAY_REST = 'GATEWAY_REST',
  GATEWAY_SNMP = 'GATEWAY_SNMP',
  GATEWAY_SOCKET = 'GATEWAY_SOCKET',
  GATEWAY_XMPP = 'GATEWAY_XMPP',
  // ChirpStack (CE-compatible LoRaWAN integration)
  CHIRPSTACK = 'CHIRPSTACK',
  // ThingsBoard PE integrations (CE shows "PE only" gate)
  INTEGRATION_APACHE_PULSAR = 'INTEGRATION_APACHE_PULSAR',
  INTEGRATION_AWS_IOT = 'INTEGRATION_AWS_IOT',
  INTEGRATION_AWS_KINESIS = 'INTEGRATION_AWS_KINESIS',
  INTEGRATION_AWS_SQS = 'INTEGRATION_AWS_SQS',
  INTEGRATION_AZURE_EVENT_HUB = 'INTEGRATION_AZURE_EVENT_HUB',
  INTEGRATION_AZURE_IOT_HUB = 'INTEGRATION_AZURE_IOT_HUB',
  INTEGRATION_AZURE_SERVICE_BUS = 'INTEGRATION_AZURE_SERVICE_BUS',
  INTEGRATION_CHIRPSTACK = 'INTEGRATION_CHIRPSTACK',
  INTEGRATION_COAP = 'INTEGRATION_COAP',
  INTEGRATION_CUSTOM = 'INTEGRATION_CUSTOM',
  INTEGRATION_HTTP = 'INTEGRATION_HTTP',
  INTEGRATION_IOT_CREATORS = 'INTEGRATION_IOT_CREATORS',
  INTEGRATION_KAFKA = 'INTEGRATION_KAFKA',
  INTEGRATION_KPN_THINGS = 'INTEGRATION_KPN_THINGS',
  INTEGRATION_LORIOT = 'INTEGRATION_LORIOT',
  INTEGRATION_MQTT = 'INTEGRATION_MQTT',
  INTEGRATION_OPC_UA = 'INTEGRATION_OPC_UA',
  INTEGRATION_PARTICLE = 'INTEGRATION_PARTICLE',
  INTEGRATION_PUB_SUB = 'INTEGRATION_PUB_SUB',
  INTEGRATION_RABBITMQ = 'INTEGRATION_RABBITMQ',
  INTEGRATION_REMOTE = 'INTEGRATION_REMOTE',
  INTEGRATION_SIGFOX = 'INTEGRATION_SIGFOX',
  INTEGRATION_TCP = 'INTEGRATION_TCP',
  INTEGRATION_THINGPARK = 'INTEGRATION_THINGPARK',
  INTEGRATION_THINGPARK_ENTERPRISE = 'INTEGRATION_THINGPARK_ENTERPRISE',
  INTEGRATION_TTI = 'INTEGRATION_TTI',
  INTEGRATION_TTN = 'INTEGRATION_TTN',
  INTEGRATION_TUYA = 'INTEGRATION_TUYA',
  INTEGRATION_UDP = 'INTEGRATION_UDP'
}

export const installMethodLabels = new Map<string, string>(
  [
    // Direct
    [InstallMethod.DIRECT_HTTP, 'HTTP'],
    [InstallMethod.DIRECT_MQTT, 'MQTT'],
    [InstallMethod.DIRECT_COAP, 'CoAP'],
    [InstallMethod.DIRECT_LWM2M, 'LwM2M'],
    [InstallMethod.DIRECT_SNMP, 'SNMP'],
    // Gateway connectors
    [InstallMethod.GATEWAY_MQTT, 'MQTT Gateway'],
    [InstallMethod.GATEWAY_MODBUS, 'Modbus Gateway'],
    [InstallMethod.GATEWAY_OPCUA, 'OPC-UA Gateway'],
    [InstallMethod.GATEWAY_BACNET, 'BACnet Gateway'],
    [InstallMethod.GATEWAY_BLE, 'BLE Gateway'],
    [InstallMethod.GATEWAY_CAN, 'CAN Gateway'],
    [InstallMethod.GATEWAY_FTP, 'FTP Gateway'],
    [InstallMethod.GATEWAY_KNX, 'KNX Gateway'],
    [InstallMethod.GATEWAY_OCPP, 'OCPP Gateway'],
    [InstallMethod.GATEWAY_ODBC, 'ODBC Gateway'],
    [InstallMethod.GATEWAY_REQUEST, 'Request Gateway'],
    [InstallMethod.GATEWAY_REST, 'REST Gateway'],
    [InstallMethod.GATEWAY_SNMP, 'SNMP Gateway'],
    [InstallMethod.GATEWAY_SOCKET, 'Socket Gateway'],
    [InstallMethod.GATEWAY_XMPP, 'XMPP Gateway'],
    // ChirpStack
    [InstallMethod.CHIRPSTACK, 'ChirpStack'],
    // PE integrations
    [InstallMethod.INTEGRATION_APACHE_PULSAR, 'Apache Pulsar'],
    [InstallMethod.INTEGRATION_AWS_IOT, 'AWS IoT'],
    [InstallMethod.INTEGRATION_AWS_KINESIS, 'AWS Kinesis'],
    [InstallMethod.INTEGRATION_AWS_SQS, 'AWS SQS'],
    [InstallMethod.INTEGRATION_AZURE_EVENT_HUB, 'Azure Event Hub'],
    [InstallMethod.INTEGRATION_AZURE_IOT_HUB, 'Azure IoT Hub'],
    [InstallMethod.INTEGRATION_AZURE_SERVICE_BUS, 'Azure Service Bus'],
    [InstallMethod.INTEGRATION_CHIRPSTACK, 'ChirpStack (Integration)'],
    [InstallMethod.INTEGRATION_COAP, 'CoAP Integration'],
    [InstallMethod.INTEGRATION_CUSTOM, 'Custom Integration'],
    [InstallMethod.INTEGRATION_HTTP, 'HTTP Integration'],
    [InstallMethod.INTEGRATION_IOT_CREATORS, 'IoT Creators'],
    [InstallMethod.INTEGRATION_KAFKA, 'Apache Kafka'],
    [InstallMethod.INTEGRATION_KPN_THINGS, 'KPN Things'],
    [InstallMethod.INTEGRATION_LORIOT, 'LORIOT'],
    [InstallMethod.INTEGRATION_MQTT, 'MQTT Integration'],
    [InstallMethod.INTEGRATION_OPC_UA, 'OPC-UA Integration'],
    [InstallMethod.INTEGRATION_PARTICLE, 'Particle'],
    [InstallMethod.INTEGRATION_PUB_SUB, 'Google Pub/Sub'],
    [InstallMethod.INTEGRATION_RABBITMQ, 'RabbitMQ'],
    [InstallMethod.INTEGRATION_REMOTE, 'Remote Integration'],
    [InstallMethod.INTEGRATION_SIGFOX, 'Sigfox'],
    [InstallMethod.INTEGRATION_TCP, 'TCP Integration'],
    [InstallMethod.INTEGRATION_THINGPARK, 'ThingPark Wireless'],
    [InstallMethod.INTEGRATION_THINGPARK_ENTERPRISE, 'ThingPark Enterprise'],
    [InstallMethod.INTEGRATION_TTI, 'The Things Industries'],
    [InstallMethod.INTEGRATION_TTN, 'The Things Stack'],
    [InstallMethod.INTEGRATION_TUYA, 'Tuya'],
    [InstallMethod.INTEGRATION_UDP, 'UDP Integration']
  ]
);

export const peOnlyInstallMethods: ReadonlySet<string> = new Set<string>([
  InstallMethod.INTEGRATION_APACHE_PULSAR,
  InstallMethod.INTEGRATION_AWS_IOT,
  InstallMethod.INTEGRATION_AWS_KINESIS,
  InstallMethod.INTEGRATION_AWS_SQS,
  InstallMethod.INTEGRATION_AZURE_EVENT_HUB,
  InstallMethod.INTEGRATION_AZURE_IOT_HUB,
  InstallMethod.INTEGRATION_AZURE_SERVICE_BUS,
  InstallMethod.INTEGRATION_CHIRPSTACK,
  InstallMethod.INTEGRATION_COAP,
  InstallMethod.INTEGRATION_CUSTOM,
  InstallMethod.INTEGRATION_HTTP,
  InstallMethod.INTEGRATION_IOT_CREATORS,
  InstallMethod.INTEGRATION_KAFKA,
  InstallMethod.INTEGRATION_KPN_THINGS,
  InstallMethod.INTEGRATION_LORIOT,
  InstallMethod.INTEGRATION_MQTT,
  InstallMethod.INTEGRATION_OPC_UA,
  InstallMethod.INTEGRATION_PARTICLE,
  InstallMethod.INTEGRATION_PUB_SUB,
  InstallMethod.INTEGRATION_RABBITMQ,
  InstallMethod.INTEGRATION_REMOTE,
  InstallMethod.INTEGRATION_SIGFOX,
  InstallMethod.INTEGRATION_TCP,
  InstallMethod.INTEGRATION_THINGPARK,
  InstallMethod.INTEGRATION_THINGPARK_ENTERPRISE,
  InstallMethod.INTEGRATION_TTI,
  InstallMethod.INTEGRATION_TTN,
  InstallMethod.INTEGRATION_TUYA,
  InstallMethod.INTEGRATION_UDP
]);

export enum InstallStepType {
  SHOW_INSTRUCTION = 'SHOW_INSTRUCTION',
  SHOW_FORM = 'SHOW_FORM',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  CONVERTER = 'CONVERTER',
  INTEGRATION = 'INTEGRATION',
  DEVICE = 'DEVICE',
  GATEWAY = 'GATEWAY',
  GATEWAY_CONNECTOR = 'GATEWAY_CONNECTOR',
  DASHBOARD = 'DASHBOARD',
  RULE_CHAIN = 'RULE_CHAIN'
}

export const ENTITY_STEP_TYPES: Set<string> = new Set([
  InstallStepType.DEVICE_PROFILE,
  InstallStepType.DEVICE,
  InstallStepType.GATEWAY,
  InstallStepType.GATEWAY_CONNECTOR,
  InstallStepType.DASHBOARD,
  InstallStepType.RULE_CHAIN
]);

export const stepTypeAliasMap: Record<string, string> = {
  [InstallStepType.DEVICE_PROFILE]: 'deviceProfile',
  [InstallStepType.DEVICE]: 'device',
  [InstallStepType.GATEWAY]: 'gateway',
  [InstallStepType.GATEWAY_CONNECTOR]: 'gatewayConnector',
  [InstallStepType.DASHBOARD]: 'dashboard',
  [InstallStepType.RULE_CHAIN]: 'ruleChain'
};

export interface DeviceInstallStep {
  type: InstallStepType;
  name: string;
  file?: string;
  template?: string;
  serverAttributes?: string;
  sharedAttributes?: string;
  credentials?: string;
  dockerCompose?: string;
}

export interface DevicePackageInfo {
  name: string;
  description: string;
  vendor: string;
  hardwareType: string;
  installMethods: string[];
  installSteps: Record<string, DeviceInstallStep[]>;
}

export enum FormFieldType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  BOOLEAN = 'BOOLEAN',
  SELECT = 'SELECT',
  PASSWORD = 'PASSWORD'
}

export interface FormFieldValidator {
  pattern: string;
  message: string;
}

export interface FormFieldOption {
  value: string;
  label: string;
}

export interface FormFieldDefinition {
  key: string;
  label: string;
  type: FormFieldType;
  defaultValue?: any;
  required?: boolean;
  helpText?: string;
  helpImage?: string;
  validators?: FormFieldValidator[];
  options?: FormFieldOption[];
}

export interface EntityStepOutput {
  id: string;
  name: string;
  url?: string;
  token?: string;
  dockerComposeUrl?: string;
}

export type EntityStepStatus = 'pending' | 'running' | 'success' | 'error' | 'conflict';

export type ConflictType = 'use-or-overwrite' | 'overwrite-or-copy';

export interface EntityStepProgress {
  step: DeviceInstallStep;
  status: EntityStepStatus;
  resolvedName?: string;
  entityOutput?: EntityStepOutput;
  errorMessage?: string;
  existingEntity?: EntityStepOutput;
  conflictType?: ConflictType;
  resolution?: string;
}
