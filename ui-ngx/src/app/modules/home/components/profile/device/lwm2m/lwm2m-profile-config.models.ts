///
/// Copyright © 2016-2021 The Thingsboard Authors
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

export const PAGE_SIZE_LIMIT = 50;
export const INSTANCES = 'instances';
export const RESOURCES = 'resources';
export const OBSERVE = 'observe';
export const ATTRIBUTE = 'attribute';
export const TELEMETRY = 'telemetry';
export const KEY_NAME = 'keyName';
export const DEFAULT_ID_SERVER = 123;
export const DEFAULT_ID_BOOTSTRAP = 111;
export const DEFAULT_LOCAL_HOST_NAME = 'localhost';
export const DEFAULT_PORT_SERVER_NO_SEC = 5685;
export const DEFAULT_PORT_BOOTSTRAP_NO_SEC = 5687;
export const DEFAULT_CLIENT_HOLD_OFF_TIME = 1;
export const DEFAULT_LIFE_TIME = 300;
export const DEFAULT_MIN_PERIOD = 1;
export const DEFAULT_NOTIF_IF_DESIBLED = true;
export const DEFAULT_BINDING = 'UQ';
export const DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT = 0;
export const KEY_REGEXP_HEX_DEC = /^[-+]?[0-9A-Fa-f]+\.?[0-9A-Fa-f]*?$/;
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
  UQ = 'UQ',
  T = 'T',
  TQ = 'TQ',
  S = 'S',
  SQ = 'SQ',
  US = 'US',
  TS = 'TS',
  UQS = 'UQS',
  TQS = 'TQS'
}

export const BingingModeTranslationsMap = new Map<BingingMode, string>(
  [
    [BingingMode.U, 'device-profile.lwm2m.binding-type.u'],
    [BingingMode.UQ, 'device-profile.lwm2m.binding-type.uq'],
    [BingingMode.US, 'device-profile.lwm2m.binding-type.us'],
    [BingingMode.UQS, 'device-profile.lwm2m.binding-type.uqs'],
    [BingingMode.T, 'device-profile.lwm2m.binding-type.t'],
    [BingingMode.TQ, 'device-profile.lwm2m.binding-type.tq'],
    [BingingMode.TS, 'device-profile.lwm2m.binding-type.ts'],
    [BingingMode.TQS, 'device-profile.lwm2m.binding-type.tqs'],
    [BingingMode.S, 'device-profile.lwm2m.binding-type.s'],
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

export enum securityConfigMode {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NO_SEC = 'NO_SEC'
}

export const securityConfigModeNames = new Map<securityConfigMode, string>(
  [
    [securityConfigMode.PSK, 'Pre-Shared Key'],
    [securityConfigMode.RPK, 'Raw Public Key'],
    [securityConfigMode.X509, 'X.509 Certificate'],
    [securityConfigMode.NO_SEC, 'No Security']
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

export interface BootstrapServersSecurityConfig {
  shortId: number;
  lifetime: number;
  defaultMinPeriod: number;
  notifIfDisabled: boolean;
  binding: string;
}

export interface ServerSecurityConfig {
  host?: string;
  port?: number;
  securityMode: securityConfigMode;
  serverPublicKey?: string;
  clientHoldOffTime?: number;
  serverId?: number;
  bootstrapServerAccountTimeout: number;
}

export interface ServerSecurityConfigInfo extends ServerSecurityConfig {
  securityHost?: string;
  securityPort?: number;
  bootstrapServerIs: boolean;
}

interface BootstrapSecurityConfig {
  servers: BootstrapServersSecurityConfig;
  bootstrapServer: ServerSecurityConfig;
  lwm2mServer: ServerSecurityConfig;
}

export interface Lwm2mProfileConfigModels {
  clientLwM2mSettings: ClientLwM2mSettings;
  observeAttr: ObservableAttributes;
  bootstrap: BootstrapSecurityConfig;
}

export interface ClientLwM2mSettings {
  clientOnlyObserveAfterConnect: number;
  fwUpdateStrategy: number;
  swUpdateStrategy: number;
  fwUpdateResource?: string;
  swUpdateResource?: string;
  powerMode: PowerMode;
  edrxCycle?: number;
  pagingTransmissionWindow?: number;
  psmActivityTimer?: number;
  compositeOperationsSupport: boolean;
}

export interface ObservableAttributes {
  observe: string[];
  attribute: string[];
  telemetry: string[];
  keyName: {};
  attributeLwm2m: AttributesNameValueMap;
}

export function getDefaultBootstrapServersSecurityConfig(): BootstrapServersSecurityConfig {
  return {
    shortId: DEFAULT_ID_SERVER,
    lifetime: DEFAULT_LIFE_TIME,
    defaultMinPeriod: DEFAULT_MIN_PERIOD,
    notifIfDisabled: DEFAULT_NOTIF_IF_DESIBLED,
    binding: DEFAULT_BINDING
  };
}

export function getDefaultBootstrapServerSecurityConfig(): ServerSecurityConfig {
  return {
    bootstrapServerAccountTimeout: DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT,
    clientHoldOffTime: DEFAULT_CLIENT_HOLD_OFF_TIME,
    host: DEFAULT_LOCAL_HOST_NAME,
    port: DEFAULT_PORT_BOOTSTRAP_NO_SEC,
    securityMode: securityConfigMode.NO_SEC,
    serverId: DEFAULT_ID_BOOTSTRAP,
    serverPublicKey: ''
  };
}

export function getDefaultLwM2MServerSecurityConfig(): ServerSecurityConfig {
  const DefaultLwM2MServerSecurityConfig = getDefaultBootstrapServerSecurityConfig();
  DefaultLwM2MServerSecurityConfig.port = DEFAULT_PORT_SERVER_NO_SEC;
  DefaultLwM2MServerSecurityConfig.serverId = DEFAULT_ID_SERVER;
  return DefaultLwM2MServerSecurityConfig;
}

export function getDefaultProfileObserveAttrConfig(): ObservableAttributes {
  return {
    observe: [],
    attribute: [],
    telemetry: [],
    keyName: {},
    attributeLwm2m: {}
  };
}

export function getDefaultProfileClientLwM2mSettingsConfig(): ClientLwM2mSettings {
  return {
    clientOnlyObserveAfterConnect: 1,
    fwUpdateStrategy: 1,
    swUpdateStrategy: 1,
    powerMode: PowerMode.DRX,
    compositeOperationsSupport: false
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
