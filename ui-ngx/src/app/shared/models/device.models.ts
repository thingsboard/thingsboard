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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { DeviceId } from './id/device-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { DeviceCredentialsId } from '@shared/models/id/device-credentials-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { EntityInfoData, HasTenantId, HasVersion } from '@shared/models/entity.models';
import { FilterPredicateValue, KeyFilter } from '@shared/models/query/query.models';
import { TimeUnit } from '@shared/models/time/time.models';
import _moment from 'moment';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { OtaPackageId } from '@shared/models/id/ota-package-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { DataType } from '@shared/models/constants';
import {
  getDefaultProfileClientLwM2mSettingsConfig,
  getDefaultProfileObserveAttrConfig,
  PowerMode
} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { PageLink } from '@shared/models/page/page-link';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { EdgeId } from '@shared/models/id/edge-id';

export enum DeviceProfileType {
  DEFAULT = 'DEFAULT',
  SNMP = 'SNMP'
}

export enum DeviceTransportType {
  DEFAULT = 'DEFAULT',
  MQTT = 'MQTT',
  COAP = 'COAP',
  LWM2M = 'LWM2M',
  SNMP = 'SNMP'
}

export enum BasicTransportType {
  HTTP = 'HTTP'
}
export type TransportType =  BasicTransportType | DeviceTransportType;
export type NetworkTransportType =  BasicTransportType | Exclude<DeviceTransportType, DeviceTransportType.DEFAULT>;

export enum TransportPayloadType {
  JSON = 'JSON',
  PROTOBUF = 'PROTOBUF'
}

export enum CoapTransportDeviceType {
  DEFAULT = 'DEFAULT',
  EFENTO = 'EFENTO'
}

export enum DeviceProvisionType {
  DISABLED = 'DISABLED',
  ALLOW_CREATE_NEW_DEVICES = 'ALLOW_CREATE_NEW_DEVICES',
  CHECK_PRE_PROVISIONED_DEVICES = 'CHECK_PRE_PROVISIONED_DEVICES',
  X509_CERTIFICATE_CHAIN = 'X509_CERTIFICATE_CHAIN'
}

export interface DeviceConfigurationFormInfo {
  hasProfileConfiguration: boolean;
  hasDeviceConfiguration: boolean;
}

export const deviceProfileTypeTranslationMap = new Map<DeviceProfileType, string>(
  [
    [DeviceProfileType.DEFAULT, 'device-profile.type-default']
  ]
);

export const deviceProfileTypeConfigurationInfoMap = new Map<DeviceProfileType, DeviceConfigurationFormInfo>(
  [
    [
      DeviceProfileType.DEFAULT,
      {
        hasProfileConfiguration: false,
        hasDeviceConfiguration: false,
      }
    ],
    [
      DeviceProfileType.SNMP,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true,
      }
    ]
  ]
);

export const deviceTransportTypeTranslationMap = new Map<TransportType, string>(
  [
    [DeviceTransportType.DEFAULT, 'device-profile.transport-type-default'],
    [DeviceTransportType.MQTT, 'device-profile.transport-type-mqtt'],
    [DeviceTransportType.COAP, 'device-profile.transport-type-coap'],
    [DeviceTransportType.LWM2M, 'device-profile.transport-type-lwm2m'],
    [DeviceTransportType.SNMP, 'device-profile.transport-type-snmp'],
    [BasicTransportType.HTTP, 'device-profile.transport-type-http']
  ]
);


export const deviceProvisionTypeTranslationMap = new Map<DeviceProvisionType, string>(
  [
    [DeviceProvisionType.DISABLED, 'device-profile.provision-strategy-disabled'],
    [DeviceProvisionType.ALLOW_CREATE_NEW_DEVICES, 'device-profile.provision-strategy-created-new'],
    [DeviceProvisionType.CHECK_PRE_PROVISIONED_DEVICES, 'device-profile.provision-strategy-check-pre-provisioned'],
    [DeviceProvisionType.X509_CERTIFICATE_CHAIN, 'device-profile.provision-strategy-x509.certificate-chain']
  ]
);

export const deviceTransportTypeHintMap = new Map<TransportType, string>(
  [
    [DeviceTransportType.DEFAULT, 'device-profile.transport-type-default-hint'],
    [DeviceTransportType.MQTT, 'device-profile.transport-type-mqtt-hint'],
    [DeviceTransportType.COAP, 'device-profile.transport-type-coap-hint'],
    [DeviceTransportType.LWM2M, 'device-profile.transport-type-lwm2m-hint'],
    [DeviceTransportType.SNMP, 'device-profile.transport-type-snmp-hint'],
    [BasicTransportType.HTTP, '']
  ]
);

export const transportPayloadTypeTranslationMap = new Map<TransportPayloadType, string>(
  [
    [TransportPayloadType.JSON, 'device-profile.transport-device-payload-type-json'],
    [TransportPayloadType.PROTOBUF, 'device-profile.transport-device-payload-type-proto']
  ]
);

export const defaultTelemetrySchema =
  'syntax ="proto3";\n' +
  'package telemetry;\n' +
  '\n' +
  'message SensorDataReading {\n' +
  '\n' +
  '  optional double temperature = 1;\n' +
  '  optional double humidity = 2;\n' +
  '  InnerObject innerObject = 3;\n' +
  '\n' +
  '  message InnerObject {\n' +
  '    optional string key1 = 1;\n' +
  '    optional bool key2 = 2;\n' +
  '    optional double key3 = 3;\n' +
  '    optional int32 key4 = 4;\n' +
  '    optional string key5 = 5;\n' +
  '  }\n' +
  '}\n';

export const defaultAttributesSchema =
  'syntax ="proto3";\n' +
  'package attributes;\n' +
  '\n' +
  'message SensorConfiguration {\n' +
  '  optional string firmwareVersion = 1;\n' +
  '  optional string serialNumber = 2;\n' +
  '}';

export const defaultRpcRequestSchema =
  'syntax ="proto3";\n' +
  'package rpc;\n' +
  '\n' +
  'message RpcRequestMsg {\n' +
  '  optional string method = 1;\n' +
  '  optional int32 requestId = 2;\n' +
  '  optional string params = 3;\n' +
  '}';

export const defaultRpcResponseSchema =
  'syntax ="proto3";\n' +
  'package rpc;\n' +
  '\n' +
  'message RpcResponseMsg {\n' +
  '  optional string payload = 1;\n' +
  '}';

export const coapDeviceTypeTranslationMap = new Map<CoapTransportDeviceType, string>(
  [
    [CoapTransportDeviceType.DEFAULT, 'device-profile.coap-device-type-default'],
    [CoapTransportDeviceType.EFENTO, 'device-profile.coap-device-type-efento']
  ]
);


export const deviceTransportTypeConfigurationInfoMap = new Map<DeviceTransportType, DeviceConfigurationFormInfo>(
  [
    [
      DeviceTransportType.DEFAULT,
      {
        hasProfileConfiguration: false,
        hasDeviceConfiguration: false,
      }
    ],
    [
      DeviceTransportType.MQTT,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: false,
      }
    ],
    [
      DeviceTransportType.LWM2M,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true,
      }
    ],
    [
      DeviceTransportType.COAP,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true,
      }
    ],
    [
      DeviceTransportType.SNMP,
      {
        hasProfileConfiguration: true,
        hasDeviceConfiguration: true
      }
    ]
  ]
);

export interface DefaultDeviceProfileConfiguration {
  [key: string]: any;
}

export type DeviceProfileConfigurations = DefaultDeviceProfileConfiguration;

export interface DeviceProfileConfiguration extends DeviceProfileConfigurations {
  type: DeviceProfileType;
}

export interface DefaultDeviceProfileTransportConfiguration {
  [key: string]: any;
}

export interface MqttDeviceProfileTransportConfiguration {
  deviceTelemetryTopic?: string;
  deviceAttributesTopic?: string;
  deviceAttributesSubscribeTopic?: string;
  sparkplug?: boolean;
  sendAckOnValidationException?: boolean;
  transportPayloadTypeConfiguration?: {
    transportPayloadType?: TransportPayloadType;
    enableCompatibilityWithJsonPayloadFormat?: boolean;
    useJsonPayloadFormatForDefaultDownlinkTopics?: boolean;
  };
  [key: string]: any;
}

export interface CoapClientSetting {
  powerMode?: PowerMode | null;
  edrxCycle?: number;
  pagingTransmissionWindow?: number;
  psmActivityTimer?: number;
}

export interface CoapDeviceProfileTransportConfiguration {
  coapDeviceTypeConfiguration?: {
    coapDeviceType?: CoapTransportDeviceType;
    transportPayloadTypeConfiguration?: {
      transportPayloadType?: TransportPayloadType;
      [key: string]: any;
    };
  };
  clientSettings?: CoapClientSetting;
}

export interface Lwm2mDeviceProfileTransportConfiguration {
  [key: string]: any;
}

export interface SnmpDeviceProfileTransportConfiguration {
  timeoutMs?: number;
  retries?: number;
  communicationConfigs?: SnmpCommunicationConfig[];
}

export enum SnmpSpecType {
  TELEMETRY_QUERYING = 'TELEMETRY_QUERYING',
  CLIENT_ATTRIBUTES_QUERYING = 'CLIENT_ATTRIBUTES_QUERYING',
  SHARED_ATTRIBUTES_SETTING = 'SHARED_ATTRIBUTES_SETTING',
  TO_DEVICE_RPC_REQUEST = 'TO_DEVICE_RPC_REQUEST',
  TO_SERVER_RPC_REQUEST = 'TO_SERVER_RPC_REQUEST'
}

export const SnmpSpecTypeTranslationMap = new Map<SnmpSpecType, string>([
  [SnmpSpecType.TELEMETRY_QUERYING, ' Telemetry (SNMP GET)'],
  [SnmpSpecType.CLIENT_ATTRIBUTES_QUERYING, 'Client attributes (SNMP GET)'],
  [SnmpSpecType.SHARED_ATTRIBUTES_SETTING, 'Shared attributes (SNMP SET)'],
  [SnmpSpecType.TO_DEVICE_RPC_REQUEST, 'To-device RPC request (SNMP GET/SET)'],
  [SnmpSpecType.TO_SERVER_RPC_REQUEST, 'From-device RPC request (SNMP TRAP)']
]);

export interface SnmpCommunicationConfig {
  spec: SnmpSpecType;
  mappings: SnmpMapping[];
  queryingFrequencyMs?: number;
}

export interface SnmpMapping {
  oid: string;
  key: string;
  dataType: DataType;
}

export type DeviceProfileTransportConfigurations = DefaultDeviceProfileTransportConfiguration &
                                                   MqttDeviceProfileTransportConfiguration &
                                                   CoapDeviceProfileTransportConfiguration &
                                                   Lwm2mDeviceProfileTransportConfiguration &
                                                   SnmpDeviceProfileTransportConfiguration;

export interface DeviceProfileTransportConfiguration extends DeviceProfileTransportConfigurations {
  type: DeviceTransportType;
}

export interface DeviceProvisionConfiguration {
  type: DeviceProvisionType;
  provisionDeviceSecret?: string;
  provisionDeviceKey?: string;
  certificateValue?: string;
  certificateRegExPattern?: string;
  allowCreateNewDevicesByX509Certificate?: boolean;
}

export const createDeviceProfileConfiguration = (type: DeviceProfileType): DeviceProfileConfiguration => {
  let configuration: DeviceProfileConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceProfileType.DEFAULT:
        const defaultConfiguration: DefaultDeviceProfileConfiguration = {};
        configuration = {...defaultConfiguration, type: DeviceProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
};

export const createDeviceConfiguration = (type: DeviceProfileType): DeviceConfiguration => {
  let configuration: DeviceConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceProfileType.DEFAULT:
        const defaultConfiguration: DefaultDeviceConfiguration = {};
        configuration = {...defaultConfiguration, type: DeviceProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
};

export const createDeviceProfileTransportConfiguration = (type: DeviceTransportType): DeviceProfileTransportConfiguration => {
  let transportConfiguration: DeviceProfileTransportConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceTransportType.DEFAULT:
        const defaultTransportConfiguration: DefaultDeviceProfileTransportConfiguration = {};
        transportConfiguration = {...defaultTransportConfiguration, type: DeviceTransportType.DEFAULT};
        break;
      case DeviceTransportType.MQTT:
        const mqttTransportConfiguration: MqttDeviceProfileTransportConfiguration = {
          deviceTelemetryTopic: 'v1/devices/me/telemetry',
          deviceAttributesTopic: 'v1/devices/me/attributes',
          deviceAttributesSubscribeTopic: 'v1/devices/me/attributes',
          sparkplug: false,
          sparkplugAttributesMetricNames: ['Node Control/*', 'Device Control/*', 'Properties/*'],
          sendAckOnValidationException: false,
          transportPayloadTypeConfiguration: {
            transportPayloadType: TransportPayloadType.JSON,
            enableCompatibilityWithJsonPayloadFormat: false,
            useJsonPayloadFormatForDefaultDownlinkTopics: false,
          }
        };
        transportConfiguration = {...mqttTransportConfiguration, type: DeviceTransportType.MQTT};
        break;
      case DeviceTransportType.COAP:
        const coapTransportConfiguration: CoapDeviceProfileTransportConfiguration = {
          coapDeviceTypeConfiguration: {
            coapDeviceType: CoapTransportDeviceType.DEFAULT,
            transportPayloadTypeConfiguration: {transportPayloadType: TransportPayloadType.JSON}
          },
          clientSettings: {
            powerMode: PowerMode.DRX
          }
        };
        transportConfiguration = {...coapTransportConfiguration, type: DeviceTransportType.COAP};
        break;
      case DeviceTransportType.LWM2M:
        const lwm2mTransportConfiguration: Lwm2mDeviceProfileTransportConfiguration = {
          observeAttr: getDefaultProfileObserveAttrConfig(),
          bootstrap: [],
          clientLwM2mSettings: getDefaultProfileClientLwM2mSettingsConfig()
        };
        transportConfiguration = {...lwm2mTransportConfiguration, type: DeviceTransportType.LWM2M};
        break;
      case DeviceTransportType.SNMP:
        const snmpTransportConfiguration: SnmpDeviceProfileTransportConfiguration = {
          timeoutMs: 500,
          retries: 0,
          communicationConfigs: null
        };
        transportConfiguration = {...snmpTransportConfiguration, type: DeviceTransportType.SNMP};
        break;
    }
  }
  return transportConfiguration;
};

export const createDeviceTransportConfiguration = (type: DeviceTransportType): DeviceTransportConfiguration => {
  let transportConfiguration: DeviceTransportConfiguration = null;
  if (type) {
    switch (type) {
      case DeviceTransportType.DEFAULT:
        const defaultTransportConfiguration: DefaultDeviceTransportConfiguration = {};
        transportConfiguration = {...defaultTransportConfiguration, type: DeviceTransportType.DEFAULT};
        break;
      case DeviceTransportType.MQTT:
        const mqttTransportConfiguration: MqttDeviceTransportConfiguration = {};
        transportConfiguration = {...mqttTransportConfiguration, type: DeviceTransportType.MQTT};
        break;
      case DeviceTransportType.COAP:
        const coapTransportConfiguration: CoapDeviceTransportConfiguration = {
          powerMode: null
        };
        transportConfiguration = {...coapTransportConfiguration, type: DeviceTransportType.COAP};
        break;
      case DeviceTransportType.LWM2M:
        const lwm2mTransportConfiguration: Lwm2mDeviceTransportConfiguration = {
          powerMode: null
        };
        transportConfiguration = {...lwm2mTransportConfiguration, type: DeviceTransportType.LWM2M};
        break;
      case DeviceTransportType.SNMP:
        const snmpTransportConfiguration: SnmpDeviceTransportConfiguration = {
          host: 'localhost',
          port: 161,
          protocolVersion: SnmpDeviceProtocolVersion.V2C,
          community: 'public'
        };
        transportConfiguration = {...snmpTransportConfiguration, type: DeviceTransportType.SNMP};
        break;
    }
  }
  return transportConfiguration;
};

export enum AlarmConditionType {
  SIMPLE = 'SIMPLE',
  DURATION = 'DURATION',
  REPEATING = 'REPEATING'
}

export const AlarmConditionTypeTranslationMap = new Map<AlarmConditionType, string>(
  [
    [AlarmConditionType.SIMPLE, 'device-profile.condition-type-simple'],
    [AlarmConditionType.DURATION, 'device-profile.condition-type-duration'],
    [AlarmConditionType.REPEATING, 'device-profile.condition-type-repeating']
  ]
);

export interface AlarmConditionSpec{
  type?: AlarmConditionType;
  unit?: TimeUnit;
  predicate: FilterPredicateValue<number>;
}

export interface AlarmCondition {
  condition: Array<KeyFilter>;
  spec?: AlarmConditionSpec;
}

export enum AlarmScheduleType {
  ANY_TIME = 'ANY_TIME',
  SPECIFIC_TIME = 'SPECIFIC_TIME',
  CUSTOM = 'CUSTOM'
}

export const AlarmScheduleTypeTranslationMap = new Map<AlarmScheduleType, string>(
  [
    [AlarmScheduleType.ANY_TIME, 'device-profile.schedule-any-time'],
    [AlarmScheduleType.SPECIFIC_TIME, 'device-profile.schedule-specific-time'],
    [AlarmScheduleType.CUSTOM, 'device-profile.schedule-custom']
  ]
);

export interface AlarmSchedule{
  dynamicValue?: {
    sourceAttribute: string;
    sourceType: string;
  };
  type: AlarmScheduleType;
  timezone?: string;
  daysOfWeek?: number[];
  startsOn?: number;
  endsOn?: number;
  items?: CustomTimeSchedulerItem[];
}

export interface CustomTimeSchedulerItem{
  enabled: boolean;
  dayOfWeek: number;
  startsOn: number;
  endsOn: number;
}

interface AlarmRule {
  condition: AlarmCondition;
  alarmDetails?: string;
  dashboardId?: DashboardId;
  schedule?: AlarmSchedule;
}

export { AlarmRule as DeviceProfileAlarmRule };

const alarmRuleValid = (alarmRule: AlarmRule): boolean =>
  !(!alarmRule || !alarmRule.condition || !alarmRule.condition.condition || !alarmRule.condition.condition.length);

export const alarmRuleValidator = (control: AbstractControl): ValidationErrors | null => {
  const alarmRule: AlarmRule = control.value;
  return alarmRuleValid(alarmRule) ? null : {alarmRule: true};
};

export interface DeviceProfileAlarm {
  id: string;
  alarmType: string;
  createRules: {[severity: string]: AlarmRule};
  clearRule?: AlarmRule;
  propagate?: boolean;
  propagateToOwner?: boolean;
  propagateToTenant?: boolean;
  propagateRelationTypes?: Array<string>;
}

export const deviceProfileAlarmValidator = (control: AbstractControl): ValidationErrors | null => {
  const deviceProfileAlarm: DeviceProfileAlarm = control.value;
  if (deviceProfileAlarm && deviceProfileAlarm.id && deviceProfileAlarm.alarmType &&
    deviceProfileAlarm.createRules) {
    const severities = Object.keys(deviceProfileAlarm.createRules);
    if (severities.length) {
      let alarmRulesValid = true;
      for (const severity of severities) {
        const alarmRule = deviceProfileAlarm.createRules[severity];
        if (!alarmRuleValid(alarmRule)) {
          alarmRulesValid = false;
          break;
        }
      }
      if (alarmRulesValid) {
        if (deviceProfileAlarm.clearRule && !alarmRuleValid(deviceProfileAlarm.clearRule)) {
          alarmRulesValid = false;
        }
      }
      if (alarmRulesValid) {
        return null;
      }
    }
  }
  return {deviceProfileAlarm: true};
};


export interface DeviceProfileData {
  configuration: DeviceProfileConfiguration;
  transportConfiguration: DeviceProfileTransportConfiguration;
  alarms?: Array<DeviceProfileAlarm>;
  provisionConfiguration?: DeviceProvisionConfiguration;
}

export interface DeviceProfile extends BaseData<DeviceProfileId>, HasTenantId, HasVersion, ExportableEntity<DeviceProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  default?: boolean;
  type: DeviceProfileType;
  image?: string;
  transportType: DeviceTransportType;
  provisionType: DeviceProvisionType;
  provisionDeviceKey?: string;
  defaultRuleChainId?: RuleChainId;
  defaultDashboardId?: DashboardId;
  defaultQueueName?: string;
  firmwareId?: OtaPackageId;
  softwareId?: OtaPackageId;
  profileData: DeviceProfileData;
  defaultEdgeRuleChainId?: RuleChainId;
}

export interface DeviceProfileInfo extends EntityInfoData, HasTenantId {
  tenantId?: TenantId;
  type: DeviceProfileType;
  transportType: DeviceTransportType;
  image?: string;
  defaultDashboardId?: DashboardId;
}

export interface DefaultDeviceConfiguration {
  [key: string]: any;
}

export type DeviceConfigurations = DefaultDeviceConfiguration;

export interface DeviceConfiguration extends DeviceConfigurations {
  type: DeviceProfileType;
}

export interface DefaultDeviceTransportConfiguration {
  [key: string]: any;
}

export interface MqttDeviceTransportConfiguration {
  [key: string]: any;
}

export interface CoapDeviceTransportConfiguration {
  powerMode?: PowerMode | null;
  edrxCycle?: number;
  pagingTransmissionWindow?: number;
  psmActivityTimer?: number;
}

export interface Lwm2mDeviceTransportConfiguration {
  powerMode?: PowerMode | null;
  edrxCycle?: number;
  pagingTransmissionWindow?: number;
  psmActivityTimer?: number;
}

export enum SnmpDeviceProtocolVersion {
  V1 = 'V1',
  V2C = 'V2C',
  V3 = 'V3'
}

export enum SnmpAuthenticationProtocol {
  SHA_1 = 'SHA_1',
  SHA_224 = 'SHA_224',
  SHA_256 = 'SHA_256',
  SHA_384 = 'SHA_384',
  SHA_512 = 'SHA_512',
  MD5 = 'MD5'
}

export const SnmpAuthenticationProtocolTranslationMap = new Map<SnmpAuthenticationProtocol, string>([
  [SnmpAuthenticationProtocol.SHA_1, 'SHA-1'],
  [SnmpAuthenticationProtocol.SHA_224, 'SHA-224'],
  [SnmpAuthenticationProtocol.SHA_256, 'SHA-256'],
  [SnmpAuthenticationProtocol.SHA_384, 'SHA-384'],
  [SnmpAuthenticationProtocol.SHA_512, 'SHA-512'],
  [SnmpAuthenticationProtocol.MD5, 'MD5']
]);

export enum SnmpPrivacyProtocol {
  DES = 'DES',
  AES_128 = 'AES_128',
  AES_192 = 'AES_192',
  AES_256 = 'AES_256'
}

export const SnmpPrivacyProtocolTranslationMap = new Map<SnmpPrivacyProtocol, string>([
  [SnmpPrivacyProtocol.DES, 'DES'],
  [SnmpPrivacyProtocol.AES_128, 'AES-128'],
  [SnmpPrivacyProtocol.AES_192, 'AES-192'],
  [SnmpPrivacyProtocol.AES_256, 'AES-256'],
]);

export interface SnmpDeviceTransportConfiguration {
  host?: string;
  port?: number;
  protocolVersion?: SnmpDeviceProtocolVersion;
  community?: string;
  username?: string;
  securityName?: string;
  contextName?: string;
  authenticationProtocol?: SnmpAuthenticationProtocol;
  authenticationPassphrase?: string;
  privacyProtocol?: SnmpPrivacyProtocol;
  privacyPassphrase?: string;
  engineId?: string;
}

export type DeviceTransportConfigurations = DefaultDeviceTransportConfiguration &
  MqttDeviceTransportConfiguration &
  CoapDeviceTransportConfiguration &
  Lwm2mDeviceTransportConfiguration &
  SnmpDeviceTransportConfiguration;

export interface DeviceTransportConfiguration extends DeviceTransportConfigurations {
  type: DeviceTransportType;
}

export interface DeviceData {
  configuration: DeviceConfiguration;
  transportConfiguration: DeviceTransportConfiguration;
}

export interface Device extends BaseData<DeviceId>, HasTenantId, HasVersion, ExportableEntity<DeviceId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type?: string;
  label: string;
  firmwareId?: OtaPackageId;
  softwareId?: OtaPackageId;
  deviceProfileId?: DeviceProfileId;
  deviceData?: DeviceData;
  additionalInfo?: any;
}

export interface DeviceInfo extends Device {
  customerTitle: string;
  customerIsPublic: boolean;
  deviceProfileName: string;
  active: boolean;
}

export interface DeviceInfoFilter {
  customerId?: CustomerId;
  edgeId?: EdgeId;
  type?: string;
  deviceProfileId?: DeviceProfileId;
  active?: boolean;
}

export class DeviceInfoQuery  {

  pageLink: PageLink;
  deviceInfoFilter: DeviceInfoFilter;

  constructor(pageLink: PageLink, deviceInfoFilter: DeviceInfoFilter) {
    this.pageLink = pageLink;
    this.deviceInfoFilter = deviceInfoFilter;
  }

  public toQuery(): string {
    let query;
    if (this.deviceInfoFilter.customerId) {
      query = `/customer/${this.deviceInfoFilter.customerId.id}/deviceInfos`;
    } else if (this.deviceInfoFilter.edgeId) {
      query = `/edge/${this.deviceInfoFilter.edgeId.id}/devices`;
    } else {
      query = '/tenant/deviceInfos';
    }
    query += this.pageLink.toQuery();
    if (isNotEmptyStr(this.deviceInfoFilter.type)) {
      query += `&type=${this.deviceInfoFilter.type}`;
    } else if (this.deviceInfoFilter.deviceProfileId) {
      query += `&deviceProfileId=${this.deviceInfoFilter.deviceProfileId.id}`;
    }
    if (isDefinedAndNotNull(this.deviceInfoFilter.active)) {
      query += `&active=${this.deviceInfoFilter.active}`;
    }
    return query;
  }
}

export enum DeviceCredentialsType {
  ACCESS_TOKEN = 'ACCESS_TOKEN',
  X509_CERTIFICATE = 'X509_CERTIFICATE',
  MQTT_BASIC = 'MQTT_BASIC',
  LWM2M_CREDENTIALS = 'LWM2M_CREDENTIALS'
}

export const credentialTypeNames = new Map<DeviceCredentialsType, string>(
  [
    [DeviceCredentialsType.ACCESS_TOKEN, 'Access token'],
    [DeviceCredentialsType.X509_CERTIFICATE, 'X.509'],
    [DeviceCredentialsType.MQTT_BASIC, 'MQTT Basic'],
    [DeviceCredentialsType.LWM2M_CREDENTIALS, 'LwM2M Credentials']
  ]
);

export const credentialTypesByTransportType = new Map<DeviceTransportType, DeviceCredentialsType[]>(
  [
    [DeviceTransportType.DEFAULT, [
      DeviceCredentialsType.ACCESS_TOKEN, DeviceCredentialsType.X509_CERTIFICATE, DeviceCredentialsType.MQTT_BASIC
    ]],
    [DeviceTransportType.MQTT, [
      DeviceCredentialsType.ACCESS_TOKEN, DeviceCredentialsType.X509_CERTIFICATE, DeviceCredentialsType.MQTT_BASIC
    ]],
    [DeviceTransportType.COAP, [DeviceCredentialsType.ACCESS_TOKEN, DeviceCredentialsType.X509_CERTIFICATE]],
    [DeviceTransportType.LWM2M, [DeviceCredentialsType.LWM2M_CREDENTIALS]],
    [DeviceTransportType.SNMP, [DeviceCredentialsType.ACCESS_TOKEN]]
  ]
);

export interface DeviceCredentials extends BaseData<DeviceCredentialsId>, HasTenantId {
  deviceId: DeviceId;
  credentialsType: DeviceCredentialsType;
  credentialsId: string;
  credentialsValue: string;
}

export interface DeviceCredentialMQTTBasic {
  clientId: string;
  userName: string;
  password: string;
}

export const getDeviceCredentialMQTTDefault = (): DeviceCredentialMQTTBasic => ({
  clientId: '',
  userName: '',
  password: ''
});

export interface DeviceSearchQuery extends EntitySearchQuery {
  deviceTypes: Array<string>;
}

export interface ClaimRequest {
  secretKey: string;
}

export enum ClaimResponse {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  CLAIMED = 'CLAIMED'
}

export interface ClaimResult {
  device: Device;
  response: ClaimResponse;
}

export interface PublishTelemetryCommand {
  http?: {
    http?: string;
    https?: string;
  };
  mqtt: {
    mqtt?: string;
    mqtts?: string | Array<string>;
    docker?: {
      mqtt?: string;
      mqtts?: string | Array<string>;
    };
    sparkplug?: string;
  };
  coap: {
    coap?: string;
    coaps?: string | Array<string>;
    docker?: {
      coap?: string;
      coaps?: string | Array<string>;
    };
  };
  lwm2m?: string;
  snmp?: string;
}

export const dayOfWeekTranslations = new Array<string>(
  'device-profile.schedule-day.monday',
  'device-profile.schedule-day.tuesday',
  'device-profile.schedule-day.wednesday',
  'device-profile.schedule-day.thursday',
  'device-profile.schedule-day.friday',
  'device-profile.schedule-day.saturday',
  'device-profile.schedule-day.sunday'
);

export const timeOfDayToUTCTimestamp = (date: Date | number): number => {
  if (typeof date === 'number' || date === null) {
    return 0;
  }
  return _moment.utc([1970, 0, 1, date.getHours(), date.getMinutes(), date.getSeconds(), 0]).valueOf();
};

export const utcTimestampToTimeOfDay = (time = 0): Date => new Date(time + new Date(time).getTimezoneOffset() * 60 * 1000);

const timeOfDayToMoment = (date: Date | number): _moment.Moment => {
  if (typeof date === 'number' || date === null) {
    return _moment([1970, 0, 1, 0, 0, 0, 0]);
  }
  return _moment([1970, 0, 1, date.getHours(), date.getMinutes(), 0, 0]);
};

export const getAlarmScheduleRangeText = (startsOn: Date | number, endsOn: Date | number): string => {
  const start = timeOfDayToMoment(startsOn);
  const end = timeOfDayToMoment(endsOn);
  if (start < end) {
    return `<span><span class="nowrap">${start.format('hh:mm A')}</span> – <span class="nowrap">${end.format('hh:mm A')}</span></span>`;
  } else if (start.valueOf() === 0 && end.valueOf() === 0 || start.isSame(_moment([1970, 0])) && end.isSame(_moment([1970, 0]))) {
    return '<span><span class="nowrap">12:00 AM</span> – <span class="nowrap">12:00 PM</span></span>';
  }
  return `<span><span class="nowrap">12:00 AM</span> – <span class="nowrap">${end.format('hh:mm A')}</span>` +
    ` and <span class="nowrap">${start.format('hh:mm A')}</span> – <span class="nowrap">12:00 PM</span></span>`;
};
