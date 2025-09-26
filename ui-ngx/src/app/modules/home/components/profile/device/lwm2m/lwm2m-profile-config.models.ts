///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { ValidatorFn, Validators } from '@angular/forms';
import { Lwm2mSecurityType } from '@shared/models/lwm2m-security-config.models';

export const PAGE_SIZE_LIMIT = 50;
export const INSTANCES = 'instances';
export const RESOURCES = 'resources';
export const OBSERVE = 'observe';
export const ATTRIBUTE = 'attribute';
export const TELEMETRY = 'telemetry';
export const KEY_NAME = 'keyName';
export const DEFAULT_ID_SERVER = 123;
export const DEFAULT_ID_BOOTSTRAP = 0;
export const DEFAULT_LOCAL_HOST_NAME = 'localhost';
export const DEFAULT_PORT_SERVER_NO_SEC = 5685;
export const DEFAULT_PORT_BOOTSTRAP_NO_SEC = 5687;
export const DEFAULT_CLIENT_HOLD_OFF_TIME = 1;
export const DEFAULT_LIFE_TIME = 300;
export const DEFAULT_MIN_PERIOD = 1;
export const DEFAULT_NOTIF_IF_DESIBLED = true;
export const DEFAULT_BINDING = 'U';
export const DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT = 0;
export const INSTANCES_ID_VALUE_MIN = 0;
export const INSTANCES_ID_VALUE_MAX = 65535;
export const DEFAULT_OTA_UPDATE_PROTOCOL = 'coap://';
export const DEFAULT_FW_UPDATE_RESOURCE = DEFAULT_OTA_UPDATE_PROTOCOL + DEFAULT_LOCAL_HOST_NAME + ':' + DEFAULT_PORT_SERVER_NO_SEC;
export const DEFAULT_SW_UPDATE_RESOURCE = DEFAULT_OTA_UPDATE_PROTOCOL + DEFAULT_LOCAL_HOST_NAME + ':' + DEFAULT_PORT_SERVER_NO_SEC;
export const DEFAULT_PSM_ACTIVITY_TIMER = 10000;
export const DEFAULT_EDRX_CYCLE = 81000;
export const DEFAULT_PAGING_TRANSMISSION_WINDOW = 10000;

export enum BingingMode {
  U = 'U',
  M = 'M',
  H = 'H',
  T = 'T',
  S = 'S',
  N = 'N',
  UQ = 'UQ',
  UQS = 'UQS',
  TQ = 'TQ',
  TQS = 'TQS',
  SQ = 'SQ'
}

export const BingingModeTranslationsMap = new Map<BingingMode, string>(
  [
    [BingingMode.U, 'device-profile.lwm2m.binding-type.u'],
    [BingingMode.M, 'device-profile.lwm2m.binding-type.m'],
    [BingingMode.H, 'device-profile.lwm2m.binding-type.h'],
    [BingingMode.T, 'device-profile.lwm2m.binding-type.t'],
    [BingingMode.S, 'device-profile.lwm2m.binding-type.s'],
    [BingingMode.N, 'device-profile.lwm2m.binding-type.n'],
    [BingingMode.UQ, 'device-profile.lwm2m.binding-type.uq'],
    [BingingMode.UQS, 'device-profile.lwm2m.binding-type.uqs'],
    [BingingMode.TQ, 'device-profile.lwm2m.binding-type.tq'],
    [BingingMode.TQS, 'device-profile.lwm2m.binding-type.tqs'],
    [BingingMode.SQ, 'device-profile.lwm2m.binding-type.sq']
  ]
);
// TODO: wait release Leshan for issues: https://github.com/eclipse/leshan/issues/1026
export enum AttributeName {
  pmin = 'pmin',
  pmax = 'pmax',
  gt = 'gt',
  lt = 'lt',
  st = 'st'
  // epmin = 'epmin',
  // epmax = 'epmax'
}

export const AttributeNameTranslationMap = new Map<AttributeName, string>(
  [
    [AttributeName.pmin, 'device-profile.lwm2m.attributes-name.min-period'],
    [AttributeName.pmax, 'device-profile.lwm2m.attributes-name.max-period'],
    [AttributeName.gt, 'device-profile.lwm2m.attributes-name.greater-than'],
    [AttributeName.lt, 'device-profile.lwm2m.attributes-name.less-than'],
    [AttributeName.st, 'device-profile.lwm2m.attributes-name.step'],
    // [AttributeName.epmin, 'device-profile.lwm2m.attributes-name.min-evaluation-period'],
    // [AttributeName.epmax, 'device-profile.lwm2m.attributes-name.max-evaluation-period']
  ]
);

export enum ServerConfigType {
  LWM2M = 'LWM2M',
  BOOTSTRAP = 'BOOTSTRAP'
}

export const ServerConfigTypeTranslationMap = new Map<ServerConfigType, string>(
  [
    [ServerConfigType.LWM2M, 'device-profile.lwm2m.lwm2m-server'],
    [ServerConfigType.BOOTSTRAP, 'device-profile.lwm2m.bootstrap-server']
  ]
);

export enum PowerMode {
  PSM = 'PSM',
  DRX = 'DRX',
  E_DRX = 'E_DRX'
}

export const PowerModeTranslationMap = new Map<PowerMode, string>(
  [
    [PowerMode.PSM, 'device-profile.power-saving-mode-type.psm'],
    [PowerMode.DRX, 'device-profile.power-saving-mode-type.drx'],
    [PowerMode.E_DRX, 'device-profile.power-saving-mode-type.edrx']
  ]
);

export enum ObjectIDVer {
  V1_0 = '1.0',
  V1_1 = '1.1',
  V1_2 = '1.2',
}

export const ObjectIDVerTranslationMap = new Map<ObjectIDVer, string>(
  [
    [ObjectIDVer.V1_0, 'device-profile.lwm2m.default-object-id-ver.v1-0'],
    [ObjectIDVer.V1_1, 'device-profile.lwm2m.default-object-id-ver.v1-1'],
    [ObjectIDVer.V1_2, 'device-profile.lwm2m.default-object-id-ver.v1-2'],
  ]
);

export interface ObserveStrategyData {
  name: string;
  description: string;
}

export enum ObserveStrategy {
  SINGLE = 'SINGLE',
  COMPOSITE_ALL = 'COMPOSITE_ALL',
  COMPOSITE_BY_OBJECT = 'COMPOSITE_BY_OBJECT'
}

export const ObserveStrategyMap = new Map<ObserveStrategy, ObserveStrategyData>([
  [
    ObserveStrategy.SINGLE,
    {
      name: 'device-profile.lwm2m.observe-strategy.single',
      description: 'device-profile.lwm2m.observe-strategy.single-description'
    }
  ],
  [
    ObserveStrategy.COMPOSITE_ALL,
    {
      name: 'device-profile.lwm2m.observe-strategy.composite-all',
      description: 'device-profile.lwm2m.observe-strategy.composite-all-description'
    }
  ],
  [
    ObserveStrategy.COMPOSITE_BY_OBJECT,
    {
      name: 'device-profile.lwm2m.observe-strategy.composite-by-object',
      description: 'device-profile.lwm2m.observe-strategy.composite-by-object-description'
    }
  ]
]);

export interface ServerSecurityConfig {
  host?: string;
  port?: number;
  securityMode: Lwm2mSecurityType;
  securityHost?: string;
  securityPort?: number;
  serverPublicKey?: string;
  serverCertificate?: string;
  clientHoldOffTime?: number;
  shortServerId?: number;
  bootstrapServerAccountTimeout: number;
  lifetime: number;
  defaultMinPeriod: number;
  notifIfDisabled: boolean;
  binding: string;
  bootstrapServerIs: boolean;
}

export interface ServerSecurityConfigInfo extends ServerSecurityConfig {
  securityHost?: string;
  securityPort?: number;
  bootstrapServerIs: boolean;
}

export interface Lwm2mProfileConfigModels {
  clientLwM2mSettings: ClientLwM2mSettings;
  observeAttr: ObservableAttributes;
  bootstrapServerUpdateEnable: boolean;
  bootstrap: Array<ServerSecurityConfig>;
}

export interface ClientLwM2mSettings {
  clientOnlyObserveAfterConnect: number;
  useObject19ForOtaInfo?: boolean;
  fwUpdateStrategy: number;
  swUpdateStrategy: number;
  fwUpdateResource?: string;
  swUpdateResource?: string;
  powerMode: PowerMode;
  edrxCycle?: number;
  pagingTransmissionWindow?: number;
  psmActivityTimer?: number;
  defaultObjectIDVer: ObjectIDVer;
}

export interface ObservableAttributes {
  observe: string[];
  attribute: string[];
  telemetry: string[];
  keyName: {};
  attributeLwm2m: AttributesNameValueMap;
  observeStrategy: ObserveStrategy;
  initAttrTelAsObsStrategy?: boolean;
}

export function getDefaultProfileObserveAttrConfig(): ObservableAttributes {
  return {
    observe: [],
    attribute: [],
    telemetry: [],
    keyName: {},
    attributeLwm2m: {},
    observeStrategy: ObserveStrategy.SINGLE
  };
}

export function getDefaultProfileClientLwM2mSettingsConfig(): ClientLwM2mSettings {
  return {
    clientOnlyObserveAfterConnect: 1,
    fwUpdateStrategy: 1,
    swUpdateStrategy: 1,
    powerMode: PowerMode.DRX,
    defaultObjectIDVer: ObjectIDVer.V1_0
  };
}

export type ResourceSettingTelemetry = 'observe' | 'attribute' | 'telemetry';

export interface ResourceLwM2M {
  id: number;
  name: string;
  observe: boolean;
  attribute: boolean;
  telemetry: boolean;
  keyName: string;
  attributes?: AttributesNameValueMap;
}

export interface Instance {
  id: number;
  attributes?: AttributesNameValueMap;
  resources: ResourceLwM2M[];
}

/**
 * multiple  == true  => Multiple
 * multiple  == false => Single
 * mandatory == true  => Mandatory
 * mandatory == false => Optional
 */
export interface ObjectLwM2M {
  id: number;
  keyId: string;
  name: string;
  multiple?: boolean;
  mandatory?: boolean;
  attributes?: AttributesNameValueMap;
  instances?: Instance [];
}

export type AttributesNameValueMap = {
  [key in AttributeName]?: number;
};

export interface AttributesNameValue {
  name: AttributeName;
  value: number;
}

export function valueValidatorByAttributeName(attributeName: AttributeName): ValidatorFn[] {
  const validators = [Validators.required];
  switch (attributeName) {
    case AttributeName.pmin:
    case AttributeName.pmax:
    // case AttributeName.epmin:
    // case AttributeName.epmax:
      validators.push(Validators.min(0), Validators.pattern('[0-9]*'));
      break;
  }
  return validators;
}
