// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { IntegrationId } from '@shared/models/id/integration-id';
import { ConverterId } from '@shared/models/id/converter-id';
import { EntityGroupParams } from '@shared/models/entity-group.models';
import { ActivatedRouteSnapshot } from '@angular/router';
import { HasEntityDebugSettings } from '@shared/models/entity.models';
import { MqttVersion } from '@shared/models/mqtt.models';

export enum IntegrationType {
  MQTT = 'MQTT',
  HTTP = 'HTTP',
  TTN = 'TTN',
  AWS_IOT = 'AWS_IOT',
  TCP = 'TCP',
  SIGFOX = 'SIGFOX',
  TTI = 'TTI',
  CHIRPSTACK = 'CHIRPSTACK',
  PARTICLE = 'PARTICLE',
  KPN = 'KPN',
  AZURE_EVENT_HUB = 'AZURE_EVENT_HUB',
  AZURE_SERVICE_BUS = 'AZURE_SERVICE_BUS',
  COAP = 'COAP',
  OPC_UA = 'OPC_UA',
  APACHE_PULSAR = 'APACHE_PULSAR',
  AWS_KINESIS = 'AWS_KINESIS',
  AWS_SQS = 'AWS_SQS',
  AZURE_IOT_HUB = 'AZURE_IOT_HUB',
  CUSTOM = 'CUSTOM',
  KAFKA = 'KAFKA',
  LORIOT = 'LORIOT',
  OCEANCONNECT = 'OCEANCONNECT',
  PUB_SUB = 'PUB_SUB',
  RABBITMQ = 'RABBITMQ',
  THINGPARK = 'THINGPARK',
  TMOBILE_IOT_CDP = 'TMOBILE_IOT_CDP',
  TPE = 'TPE',
  UDP = 'UDP',
  TUYA = 'TUYA'
}

export enum CoapSecurityMode {
  NO_SECURE = 'NO_SECURE',
  DTLS = 'DTLS',
  MIXED = 'MIXED',
}

export type IntegrationsConvertersInfo = Record<IntegrationType, IntegrationConvertersInfo>;

export interface IntegrationConvertersInfo {
  uplink: ConverterInfo,
  downlink: ConverterInfo,
}

export interface ConverterInfo {
  library: boolean;
  existing: boolean;
  keys?: string[];
}

export const coapSecurityModeTranslationsMap = new Map<CoapSecurityMode, string>(
  [
    [CoapSecurityMode.NO_SECURE, 'integration.coap-security-mode-no-secure'],
    [CoapSecurityMode.DTLS, 'integration.coap-security-mode-dtls'],
    [CoapSecurityMode.MIXED, 'integration.coap-security-mode-mixed']
  ]
);

export interface IntegrationTypeInfo {
  name: string;
  description: string;
  icon: string;
  tags?: string[];
  remote?: boolean;
  checkConnection?: boolean;
  hideDownlink?: boolean;
}

export const integrationTypeInfoMap = new Map<IntegrationType, IntegrationTypeInfo>(
  [
    [
      IntegrationType.HTTP,
      {
        name: 'integration.type-http',
        description: 'integration.type-http-description',
        icon: 'assets/integration-icon/http.svg'
      }
    ],
    [
      IntegrationType.OCEANCONNECT,
      {
        name: 'integration.type-ocean-connect',
        description: 'integration.type-ocean-connect-description',
        icon: 'assets/integration-icon/ocean-connect.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.SIGFOX,
      {
        name: 'integration.type-sigfox',
        description: 'integration.type-sigfox-description',
        icon: 'assets/integration-icon/sigfox.svg'
      }
    ],
    [
      IntegrationType.THINGPARK,
      {
        name: 'integration.type-thingpark',
        description: 'integration.type-thingpark-description',
        icon: 'assets/integration-icon/thingpark.svg'
      }
    ],
    [
      IntegrationType.TPE,
      {
        name: 'integration.type-thingpark-enterprise',
        description: 'integration.type-thingpark-enterprise-description',
        icon: 'assets/integration-icon/thingpark-enterprise.svg'
      }
    ],
    [
      IntegrationType.TMOBILE_IOT_CDP,
      {
        name: 'integration.type-tmobile-iot-cdp',
        description: 'integration.type-tmobile-iot-cdp-description',
        icon: 'assets/integration-icon/iotcreators.com.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.LORIOT,
      {
        name: 'integration.type-loriot',
        description: 'integration.type-loriot-description',
        icon: 'assets/integration-icon/loriot.svg'
      }
    ],
    [
      IntegrationType.MQTT,
      {
        name: 'integration.type-mqtt',
        description: 'integration.type-mqtt-description',
        icon: 'assets/integration-icon/mqtt.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.AWS_IOT,
      {
        name: 'integration.type-aws-iot',
        description: 'integration.type-aws-iot-description',
        icon: 'assets/integration-icon/aws-iot.svg'
      }
    ],
    [
      IntegrationType.AWS_SQS,
      {
        name: 'integration.type-aws-sqs',
        description: 'integration.type-aws-sqs-description',
        icon: 'assets/integration-icon/aws-sqs.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.AWS_KINESIS,
      {
        name: 'integration.type-aws-kinesis',
        description: 'integration.type-aws-kinesis-description',
        icon: 'assets/integration-icon/aws-kinesis.svg'
      }
    ],
    [
      IntegrationType.TTN,
      {
        name: 'integration.type-ttn',
        description: 'integration.type-ttn-description',
        icon: 'assets/integration-icon/ttn.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.TTI,
      {
        name: 'integration.type-tti',
        description: 'integration.type-tti-description',
        icon: 'assets/integration-icon/things-stack-industries.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.CHIRPSTACK,
      {
        name: 'integration.type-chirpstack',
        description: 'integration.type-chirpstack-description',
        icon: 'assets/integration-icon/chirpstack.svg'
      }
    ],
    [
      IntegrationType.PARTICLE,
      {
        name: 'integration.type-particle',
        description: 'integration.type-particle-description',
        icon: 'assets/integration-icon/particle.svg'
      }
    ],
    [
      IntegrationType.KPN,
      {
        name: 'integration.type-kpn',
        description: 'integration.type-kpn-description',
        icon: 'assets/integration-icon/kpn.svg'
      }
    ],
    [
      IntegrationType.AZURE_EVENT_HUB,
      {
        name: 'integration.type-azure-event-hub',
        description: 'integration.type-azure-event-hub-description',
        icon: 'assets/integration-icon/azure-event-hub.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.AZURE_SERVICE_BUS,
      {
        name: 'integration.type-azure-service-bus',
        description: 'integration.type-azure-service-bus-description',
        icon: 'assets/integration-icon/azure-service-bus.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.AZURE_IOT_HUB,
      {
        name: 'integration.type-azure-iot-hub',
        description: 'integration.type-azure-iot-hub-description',
        icon: 'assets/integration-icon/azure-iot-hub.svg'
      }
    ],
    [
      IntegrationType.OPC_UA,
      {
        name: 'integration.type-opc-ua',
        description: 'integration.type-opc-ua-description',
        icon: 'assets/integration-icon/opc-ua.svg'
      }
    ],
    [
      IntegrationType.UDP,
      {
        name: 'integration.type-udp',
        description: 'integration.type-udp-description',
        icon: 'assets/integration-icon/udp.svg',
        remote: true
      }
    ],
    [
      IntegrationType.TCP,
      {
        name: 'integration.type-tcp',
        description: 'integration.type-tcp-description',
        icon: 'assets/integration-icon/tcp.svg',
        remote: true
      }
    ],
    [
      IntegrationType.KAFKA,
      {
        name: 'integration.type-kafka',
        description: 'integration.type-kafka-description',
        icon: 'assets/integration-icon/kafka.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.RABBITMQ,
      {
        name: 'integration.type-rabbitmq',
        description: 'integration.type-rabbitmq-description',
        icon: 'assets/integration-icon/rabbitmq.svg',
        checkConnection: true
      }
    ],
    [
      IntegrationType.APACHE_PULSAR,
      {
        name: 'integration.type-apache-pulsar',
        description: 'integration.type-apache-pulsar-description',
        icon: 'assets/integration-icon/apache-pulsar.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.PUB_SUB,
      {
        name: 'integration.type-pubsub',
        description: 'integration.type-pubsub-description',
        icon: 'assets/integration-icon/pub-sub.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.COAP,
      {
        name: 'integration.type-coap',
        description: 'integration.type-coap-description',
        icon: 'assets/integration-icon/coap.svg',
        hideDownlink: true
      }
    ],
    [
      IntegrationType.TUYA,
      {
        name: 'integration.type-tuya',
        description: 'integration.type-tuya-description',
        icon: 'assets/integration-icon/tuya.svg'
      }
    ],
    [
      IntegrationType.CUSTOM,
      {
        name: 'integration.type-custom',
        description: '',
        icon: 'assets/integration-icon/custom.svg',
        remote: true
      }
    ]
  ]
);

const integrationHelpLinkMap = new Map<IntegrationType, string>(
  [
    [IntegrationType.HTTP, 'integrationHttp'],
    [IntegrationType.OCEANCONNECT, 'integrationOceanConnect'],
    [IntegrationType.SIGFOX, 'integrationSigFox'],
    [IntegrationType.THINGPARK, 'integrationThingPark'],
    [IntegrationType.TPE, 'integrationThingParkEnterprise'],
    [IntegrationType.TMOBILE_IOT_CDP, 'integrationTMobileIotCdp'],
    [IntegrationType.LORIOT, 'integrationLoriot'],
    [IntegrationType.MQTT, 'integrationMqtt'],
    [IntegrationType.AWS_IOT, 'integrationAwsIoT'],
    [IntegrationType.AWS_SQS, 'integrationAwsSQS'],
    [IntegrationType.AWS_KINESIS, 'integrationAwsKinesis'],
    [IntegrationType.TTN, 'integrationTheThingsNetwork'],
    [IntegrationType.TTI, 'integrationTheThingsIndustries'],
    [IntegrationType.CHIRPSTACK, 'integrationChirpStack'],
    [IntegrationType.PARTICLE, 'integrationParticle'],
    [IntegrationType.KPN, 'integrationKpn'],
    [IntegrationType.AZURE_EVENT_HUB, 'integrationAzureEventHub'],
    [IntegrationType.AZURE_IOT_HUB, 'integrationAzureIoTHub'],
    [IntegrationType.AZURE_SERVICE_BUS, 'integrationAzureServiceBus'],
    [IntegrationType.OPC_UA, 'integrationOpcUa'],
    [IntegrationType.UDP, 'integrationUdp'],
    [IntegrationType.TCP, 'integrationTcp'],
    [IntegrationType.KAFKA, 'integrationKafka'],
    [IntegrationType.RABBITMQ, 'integrationRabbitmq'],
    [IntegrationType.APACHE_PULSAR, 'integrationApachePulsar'],
    [IntegrationType.PUB_SUB, 'integrationPubsub'],
    [IntegrationType.COAP, 'integrationCoAP'],
    [IntegrationType.TUYA, 'integrationTuya'],
    [IntegrationType.CUSTOM, 'integrationCustom']
  ]
);

export type IntegrationConfiguration = ApachePulsarIntegration | HttpIntegration | ThingParkIntegration | LoriotIntegration |
  MqttIntegration | AwsIotIntegration | AwsSqsIntegration | AwsKinesisIntegration | TtnIntegration |
  ChipStackIntegration | ParticleIntegration | KpnIntegration | AzureEventHubIntegration | AzureIotHubIntegration | OpcUaIntegration |
  UpdIntegration | TcpIntegration | KafkaIntegration | RabbitMqIntegration | PubSubIntegration | CoapIntegration | TuyaIntegration |
  CustomIntegration;

export function getIntegrationHelpLink(integration: Integration): string {
  if (integration) {
    return getIntegrationHelpLinkByType(integration.type);
  }
  return 'integrations';
}

export function getIntegrationHelpLinkByType(type: IntegrationType): string {
  if (type) {
    if (integrationHelpLinkMap.has(type)) {
      return integrationHelpLinkMap.get(type);
    }
  }
  return 'integrations';
}

export interface IntegrationMetaData {
  metadata?: { [k: string]: string };
}

export interface IntegrationBasic extends BaseData<IntegrationId>, ExportableEntity<IntegrationId>, HasEntityDebugSettings {
  type: IntegrationType;
  enabled: boolean;
  remote: boolean;
  allowCreateDevicesOrAssets: boolean;
  edgeTemplate: boolean;
}

export interface Integration extends IntegrationBasic {
  configuration: IntegrationConfiguration & IntegrationMetaData;
  defaultConverterId: ConverterId;
  downlinkConverterId?: ConverterId;
  routingKey: string;
  secret: string;
  additionalInfo?: any;
}

export interface IntegrationInfo extends IntegrationBasic {
  status?: IntegrationStatus;
  stats: Array<number>;
}

export interface IntegrationStatus {
  success: boolean;
  serviceId?: string;
  error?: any;
}

export interface IntegrationParams extends EntityGroupParams {
  integrationScope: string;
}

export enum IntegrationSubType {
  CORE = 'CORE',
  EDGE = 'EDGE'
}

export function resolveIntegrationParams(route: ActivatedRouteSnapshot): IntegrationParams {
  const routeParams = {...route.params};
  const routeData = {...route.data};
  let backNavigationCommands: any[];
  if (routeData.backNavigationCommands) {
    backNavigationCommands = routeData.backNavigationCommands;
  }
  let edgeId: string;
  let integrationScope: string;
  if (routeParams?.hierarchyView) {
    edgeId = routeParams.edgeId;
    integrationScope = routeParams.integrationScope;
  } else {
    edgeId = routeParams?.edgeId;
    integrationScope = routeData.integrationsType ? routeData.integrationsType : 'tenant';
  }
  return {
    edgeId,
    integrationScope,
    hierarchyView: routeParams?.hierarchyView,
    entityGroupId: routeParams?.entityGroupId,
    childEntityGroupId: routeParams?.childEntityGroupId,
    customerId: routeParams?.customerId,
    backNavigationCommands
  };
}

export enum MqttQos {
  AT_MOST_ONE = 0,
  AT_LEAST_ONCE = 1,
  EXACTLY_ONCE = 2
}

export const MqttQosTranslation = new Map<MqttQos, string>([
  [MqttQos.AT_MOST_ONE, 'integration.mqtt-qos-at-most-once'],
  [MqttQos.AT_LEAST_ONCE, 'integration.mqtt-qos-at-least-once'],
  [MqttQos.EXACTLY_ONCE, 'integration.mqtt-qos-exactly-once']
]);

export interface MqttTopicFilter {
  filter: string;
  qos: MqttQos;
}

export enum TcpBinaryByteOrder {
  LITTLE_ENDIAN = 'LITTLE_ENDIAN',
  BIG_ENDIAN = 'BIG_ENDIAN'
}

export enum TcpTextMessageSeparator {
  SYSTEM_LINE_SEPARATOR = 'SYSTEM_LINE_SEPARATOR',
  NUL_DELIMITER = 'NUL_DELIMITER',
  CUSTOM_SEPARATOR = 'CUSTOM_SEPARATOR'
}

export const TcpTextMessageSeparatorTranslation = new Map<TcpTextMessageSeparator, string>([
  [TcpTextMessageSeparator.SYSTEM_LINE_SEPARATOR, 'integration.tcp.system-line-separator'],
  [TcpTextMessageSeparator.NUL_DELIMITER, 'integration.tcp.nul-delimiter'],
  [TcpTextMessageSeparator.CUSTOM_SEPARATOR, 'integration.tcp.custom-separator'],
]);

export enum IntegrationCredentialType {
  Anonymous = 'anonymous',
  Basic = 'basic',
  CertPEM = 'cert.PEM',
  Token = 'token',
  SAS = 'sas'
}


export interface Credentials {
  type: IntegrationCredentialType;
}

export interface TokenCredentials extends Credentials{
  token: string;
}

export interface CertPemCredentials extends Credentials{
  caCertFileName: string;
  caCert: string;
  certFileName: string;
  cert: string;
  privateKeyFileName: string;
  privateKey: string;
  privateKeyPassword?: string;
}

export interface SasCredentials extends Credentials{
  caCertFileName: string;
  caCert: string;
  sasKey: string;
}

export interface BasicCredentials extends Credentials{
  username: string;
  password: string;
}

export const IntegrationCredentialTypeTranslation = new Map<IntegrationCredentialType, string>([
  [IntegrationCredentialType.Anonymous, 'extension.anonymous'],
  [IntegrationCredentialType.Basic, 'extension.basic'],
  [IntegrationCredentialType.CertPEM, 'extension.pem'],
  [IntegrationCredentialType.Token, 'extension.token'],
  [IntegrationCredentialType.SAS, 'extension.sas']
]);

export enum TcpHandlerConfigurationType {
  TEXT = 'TEXT',
  BINARY = 'BINARY',
  JSON = 'JSON'
}

export enum UpdHandlerConfigurationType {
  HEX = 'HEX'
}

export type HandlerConfigurationType = TcpHandlerConfigurationType | UpdHandlerConfigurationType;
export const HandlerConfigurationType = {...TcpHandlerConfigurationType, ...UpdHandlerConfigurationType};

export const HandlerConfigurationTypeTranslation = new Map<HandlerConfigurationType, string>([
  [HandlerConfigurationType.TEXT, 'extension.text'],
  [HandlerConfigurationType.BINARY, 'extension.binary'],
  [HandlerConfigurationType.JSON, 'extension.json'],
  [HandlerConfigurationType.HEX, 'extension.hex']
]);

export enum IdentityType {
  Anonymous = 'anonymous',
  Username = 'username'
}

export interface AnonymousIdentityType {
  type: IdentityType;
}

export interface UsernameIdentityType extends AnonymousIdentityType{
  username: string;
  password: string;
}

export const IdentityTypeTranslation = new Map<IdentityType, string>([
  [IdentityType.Anonymous, 'extension.anonymous'],
  [IdentityType.Username, 'extension.username']
]);

export enum OpcSecurityType {
  Basic128Rsa15 = 'Basic128Rsa15',
  Basic256 = 'Basic256',
  Basic256Sha256 = 'Basic256Sha256',
  None = 'None'
}

export enum OpcKeystoreType {
  PKCS12 = 'PKCS12',
  JKS = 'JKS'
}

export enum OpcMappingType {
  ID = 'ID',
  FQN = 'FQN'
}

export const OpcMappingTypeTranslation = new Map<OpcMappingType, string>([
  [OpcMappingType.ID, 'ID'],
  [OpcMappingType.FQN, 'Fully Qualified Name']
]);


export enum InitialPositionInStream {
  LATEST = 'LATEST',
  TRIM_HORIZON = 'TRIM_HORIZON',
  AT_TIMESTAMP = 'AT_TIMESTAMP'
}

export const InitialPositionInStreamTranslation = new Map<InitialPositionInStream, string>([
  [InitialPositionInStream.LATEST, 'Latest'],
  [InitialPositionInStream.TRIM_HORIZON, 'Trim horizon'],
  [InitialPositionInStream.AT_TIMESTAMP, 'At timestamp']
]);

export interface Topics {
  topicFilters: Array<MqttTopicFilter>;
  downlinkTopicPattern: string;
}

export interface ApachePulsarIntegration {
  clientConfiguration: {
    serviceUrl: string;
    topics: string;
    subscriptionName: string;
    maxNumMessages: number;
    maxNumBytes: number;
    timeoutInMs: number;
    credentials: Credentials | TokenCredentials;
  };
}

export interface AwsIotIntegration extends Topics{
  clientConfiguration: {
    host: string;
    connectTimeoutSec: number;
    clientId: string;
    maxBytesInMessage: number;
    credentials: CertPemCredentials;
  };
}

export interface AwsKinesisIntegration {
  clientConfiguration: {
    streamName: string;
    region: string;
    accessKeyId: string;
    secretAccessKey: string;
    useCredentialsFromInstanceMetadata: boolean;
    applicationName?: string;
    initialPositionInStream: InitialPositionInStream;
    useConsumersWithEnhancedFanOut: boolean;
    maxRecords: number;
    requestTimeout: number;
  };
}

export interface AwsSqsIntegration {
  sqsConfiguration: {
    queueUrl: string;
    pollingPeriodSeconds: number;
    region: string;
    accessKeyId: string;
    secretAccessKey: string;
  };
}

export interface AzureEventHubIntegration {
  clientConfiguration: {
    connectTimeoutSec: number;
    connectionString: string;
    consumerGroup?: string;
    iotHubName?: string;
    storageConnectionString?: string;
    containerName?: string;
    enablePersistentCheckpoints?: boolean;
  };
}

export interface AzureServicesBusIntegration {
  clientConfiguration: {
    connectionString: string;
    topicName: string;
    subName: string;
    downlinkConnectionString: string;
    downlinkTopicName: string;
  }
}

export interface AzureIotHubIntegration{
  clientConfiguration: {
    host: string;
    clientId: string;
    maxBytesInMessage: number;
    credentials: CertPemCredentials | SasCredentials;
  };
  topicFilters: Array<MqttTopicFilter>;
}

export interface ChipStackIntegration {
  clientConfiguration: {
    baseUrl: string;
    httpEndpoint: string,
    applicationServerUrl: string;
    applicationServerAPIToken: string;
    useAPI4Plus: boolean;
    allowDownlink: boolean;
  };
}

export interface ParticleIntegration {
  clientConfiguration: {
    baseUrl: string;
    httpEndpoint: string;
    enableSecurity?: boolean;
    headersFilter?: {[key: string]: string} | null;
  };
}

export interface KpnIntegration {
  baseUrl: string;
  httpEndpoint: string;
  destinationSharedSecret: string;
  apiId: string;
  apiKey: string;
  enableSecurity?: boolean;
  headersFilter?: {[key: string]: string} | null;
}

export interface CoapIntegration {
  clientConfiguration: {
    baseUrl: string;
    dtlsBaseUrl: string;
    securityMode: CoapSecurityMode;
    coapEndpoint: string;
    dtlsCoapEndpoint: string;
  };
}

export interface CustomIntegration {
  clazz: string;
  configuration: string;
}

export interface HttpIntegration {
  baseUrl: string;
  httpEndpoint: string;
  enableSecurity?: boolean;
  headersFilter?: {[key: string]: string} | null;
  replaceNoContentToOk: boolean;
}

export interface KafkaIntegration {
  clientConfiguration: {
    groupId: string;
    clientId: string;
    topics: string;
    bootstrapServers: string;
    pollInterval: number;
    autoCreateTopics: boolean
    otherProperties?: {[key: string]: string} | null;
  };
}

export interface LoriotIntegration {
  baseUrl: string;
  httpEndpoint: string;
  enableSecurity: boolean;
  headersFilter?: {[key: string]: string} | null;
  replaceNoContentToOk: boolean;
  createLoriotOutput: boolean;
  sendDownlink: boolean;
  server: string;
  domain: string;
  appId: string;
  token: string;
  credentials: BasicCredentials | TokenCredentials;
  loriotDownlinkUrl: string;
}

export interface MqttIntegration extends Topics{
  clientConfiguration: {
    host: string;
    port: number;
    cleanSession: boolean
    ssl: boolean;
    connectTimeoutSec: number
    clientId: string;
    maxBytesInMessage: number;
    protocolVersion: MqttVersion;
    credentials: Credentials | BasicCredentials | CertPemCredentials;
  };
}

export interface OpcUaIntegration {
  clientConfiguration: {
    applicationName?: string;
    applicationUri?: string;
    host: string;
    port: number;
    scanPeriodInSeconds: number;
    timeoutInMillis: number;
    security: OpcSecurityType;
    identity: UsernameIdentityType | AnonymousIdentityType;
    mapping: Array<OpcUaMapping>;
    keystore: {
      location: string;
      type: OpcKeystoreType;
      fileContent: string;
      password: string;
      alias: string;
      keyPassword: string;
    }
  };
}

export interface OpcUaMapping {
  deviceNodePattern: string;
  mappingType: OpcMappingType;
  subscriptionTags: Array<OpcUaSubscription>;
  namespace: number | null;
}

export interface OpcUaSubscription {
  key: string;
  path: string;
  required: boolean;
}

export interface PubSubIntegration {
  clientConfiguration: {
    projectId: string;
    subscriptionId: string;
    serviceAccountKey: string;
    serviceAccountKeyFileName: string;
  };
}

export interface RabbitMqIntegration {
  clientConfiguration: {
    exchangeName: string;
    host: string;
    port: number;
    virtualHost: string;
    username: string;
    password: string;
    downlinkTopic: string;
    queues: string;
    connectionTimeout: number;
    handshakeTimeout: number;
    pollPeriod: number;
    durable: boolean;
    exclusive: boolean;
    autoDelete: boolean;
  };
}

export enum TuyaRegion {
  CN = 'CN',
  US = 'US',
  EU = 'EU',
  IN = 'IN'
}

export enum TuyaEnv {
  PROD = 'PROD',
  TEST = 'TEST'
}

export interface TuyaIntegration {
  clientConfiguration: {
    region: TuyaRegion;
    accessId: string;
    accessKey: string;
    env: TuyaEnv;
  };
}

export const TuyaRegionTranslation = new Map<TuyaRegion, string>([
  [TuyaRegion.CN, 'integration.region-cn'],
  [TuyaRegion.US, 'integration.region-us'],
  [TuyaRegion.EU, 'integration.region-eu'],
  [TuyaRegion.IN, 'integration.region-in']
]);

interface TcpHandlerConfiguration {
  handlerType: TcpHandlerConfigurationType;
}

interface BinaryTcpHandlerConfiguration extends TcpHandlerConfiguration {
  byteOrder: TcpBinaryByteOrder;
  maxFrameLength: number;
  lengthFieldOffset: number;
  lengthFieldLength: number;
  lengthAdjustment: number;
  initialBytesToStrip: number;
  failFast: boolean;
}

interface TextTcpHandlerConfiguration extends TcpHandlerConfiguration{
  stripDelimiter: boolean;
  messageSeparator: TcpTextMessageSeparator;
}

export interface TcpIntegration {
  clientConfiguration: {
    port: number;
    soBacklogOption: number;
    soRcvBuf: number;
    soSndBuf: number;
    soKeepaliveOption: boolean;
    tcpNoDelay: boolean;
    cacheSize: number;
    timeToLiveInMinutes: number;
    handlerConfiguration: BinaryTcpHandlerConfiguration | TextTcpHandlerConfiguration | TcpHandlerConfiguration;
  };
}

export interface ThingParkIntegration {
  baseUrl: string;
  httpEndpoint: string;
  enableSecurity: boolean;
  replaceNoContentToOk: boolean;
  downlinkUrl?: string;
  enableSecurityNew: boolean;
  asId?: string;
  asIdNew?: string;
  asKey?: string;
  clientIdNew?: string;
  clientSecret?: string;
  maxTimeDiffInSeconds: number;
}

export interface TtnIntegration extends Topics{
  clientConfiguration: {
    host: string;
    hostEdit?: string;
    customHost: boolean;
    port: number;
    ssl: boolean;
    maxBytesInMessage: number;
    connectTimeoutSec: number;
    apiVersion?: boolean;
    credentials: BasicCredentials;
  };
}

interface UpdHandlerConfiguration {
  type: HandlerConfigurationType;
}

interface TextUpdHandlerConfiguration extends UpdHandlerConfiguration{
  charsetName: string;
}

interface HexUpdHandlerConfiguration extends UpdHandlerConfiguration{
  maxFrameLength: number;
}

export interface UpdIntegration {
  clientConfiguration: {
    port: number;
    soBroadcast: boolean;
    soRcvBuf: number;
    cacheSize: number;
    timeToLiveInMinutes: number;
    handlerConfiguration: HexUpdHandlerConfiguration | TextUpdHandlerConfiguration | UpdHandlerConfiguration;
  };
}
