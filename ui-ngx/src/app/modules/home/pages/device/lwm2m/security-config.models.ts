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

export const JSON_ALL_CONFIG = 'jsonAllConfig';
export const END_POINT = 'endPoint';
export const DEFAULT_END_POINT = 'default_client_lwm2m_end_point_no_sec';
export const BOOTSTRAP_SERVERS = 'servers';
export const BOOTSTRAP_SERVER = 'bootstrapServer';
export const LWM2M_SERVER = 'lwm2mServer';
export const OBSERVE = 'observe';
export const JSON_OBSERVE = 'jsonObserve';
export const DEFAULT_ID_SERVER = 123;
const DEFAULT_PORT_SERVER = 5686;
const DEFAULT_PORT_SERVER_NOSEC = 5685;
const DEFAULT_ID_BOOTSTRAP = 111;
const DEFAULT_PORT_BOOTSTRAP = 5688;
const DEFAULT_PORT_BOOTSTRAP_NOSEC = 5687;
const DEFAULT_CLIENT_HOLD_OFF_TIME = 1;
export const DEFAULT_LIFE_TIME = 300;
export const DEFAULT_DEFAULT_MIN_PERIOD =  1;
const DEFAULT_NOTIF_IF_DESIBLED =  true;
export const DEFAULT_BINDING = "U";
const DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT = 0;
export const KEY_IDENT_REGEXP_PSK =/^[0-9a-fA-F]{64,64}$/;
export const KEY_PRIVATE_REGEXP =/^[0-9a-fA-F]{134,134}$/;
export const KEY_PUBLIC_REGEXP =/^[0-9a-fA-F]{182,182}$/;

export interface DeviceCredentialsDialogLwm2mData {
  jsonAllConfig?: SecurityConfigModels;
  endPoint?: string;
}

export enum SECURITY_CONFIG_MODE {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NO_SEC = 'NO_SEC'
}

export const SECURITY_CONFIG_MODE_NAMES = new Map<SECURITY_CONFIG_MODE, string>(
  [
    [SECURITY_CONFIG_MODE.PSK, 'Pre-Shared Key'],
    [SECURITY_CONFIG_MODE.RPK, 'Raw Public Key'],
    [SECURITY_CONFIG_MODE.X509, 'X.509 Certificate'],
    [SECURITY_CONFIG_MODE.NO_SEC, 'No Security'],
  ]
);

export type ClientSecurityConfigType =
  ClientSecurityConfigPSK
  | ClientSecurityConfigRPK
  | ClientSecurityConfigX509
  | ClientSecurityConfigNO_SEC;

export interface ClientSecurityConfigPSK {
  securityConfigClientMode: string,
  endpoint: string,
  identity: string,
  key: string
}

export interface ClientSecurityConfigRPK {
  securityConfigClientMode: string,
  key: string
}

export interface ClientSecurityConfigX509 {
  securityConfigClientMode: string,
  x509: boolean
}

interface ClientSecurityConfigNO_SEC {
  securityConfigClientMode: string
}

interface BootstrapServersSecurityConfig {
  shortId: number,
  lifetime: number,
  defaultMinPeriod: number,
  notifIfDisabled: boolean,
  binding: string
}

export interface ServerSecurityConfig {
  host?: string,
  port?: number,
  isBootstrapServer?: boolean,
  securityMode: string,
  clientPublicKeyOrId?: string,
  clientSecretKey?: string,
  serverPublicKey?: string;
  clientHoldOffTime?: number,
  serverId?: number,
  bootstrapServerAccountTimeout: number
}

interface BootstrapSecurityConfig {
  servers: BootstrapServersSecurityConfig,
  bootstrapServer: ServerSecurityConfig,
  lwm2mServer: ServerSecurityConfig
}

export interface SecurityConfigModels {
  client: ClientSecurityConfigType,
  bootstrap: BootstrapSecurityConfig,
  observe: ObjectLwM2M[]
}

function getDefaultClientSecurityConfigType(securityConfigMode: SECURITY_CONFIG_MODE, endPoint?: string): ClientSecurityConfigType {
  let security: ClientSecurityConfigType;
  switch (securityConfigMode) {
    case SECURITY_CONFIG_MODE.PSK:
      security = {
        securityConfigClientMode: '',
        endpoint: endPoint,
        identity: '',
        key: ''
      }
      break;
    case SECURITY_CONFIG_MODE.RPK:
      security = {
        securityConfigClientMode: '',
        key: ''
      }
      break;
    case SECURITY_CONFIG_MODE.X509:
      security = {
        securityConfigClientMode: '',
        x509: true
      }
      break;
    case SECURITY_CONFIG_MODE.NO_SEC:
      security = {
        securityConfigClientMode: ''
      }
      break;
  }
  security.securityConfigClientMode = securityConfigMode.toString();
  return security;
}

export function getDefaultBootstrapServersSecurityConfig(): BootstrapServersSecurityConfig {
  return {
    shortId: DEFAULT_ID_SERVER,
    lifetime: DEFAULT_LIFE_TIME,
    defaultMinPeriod: DEFAULT_DEFAULT_MIN_PERIOD,
    notifIfDisabled: DEFAULT_NOTIF_IF_DESIBLED,
    binding: DEFAULT_BINDING
  }
}

export function getDefaultBootstrapServerSecurityConfig(): ServerSecurityConfig {
  return {
    host: '',
    port: getDefaultPortBootstrap(),
    isBootstrapServer: true,
    securityMode: SECURITY_CONFIG_MODE.NO_SEC.toString(),
    clientPublicKeyOrId: '',
    clientSecretKey: '',
    serverPublicKey: '',
    clientHoldOffTime: DEFAULT_CLIENT_HOLD_OFF_TIME,
    serverId: DEFAULT_ID_BOOTSTRAP,
    bootstrapServerAccountTimeout: DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT
  }
}

export function getDefaultLwM2MServerSecurityConfig(): ServerSecurityConfig {
  const DefaultLwM2MServerSecurityConfig =  getDefaultBootstrapServerSecurityConfig();
  DefaultLwM2MServerSecurityConfig.isBootstrapServer = false;
  return DefaultLwM2MServerSecurityConfig;
}

export function getDefaultPortBootstrap (securityMode?: string): number {
  return (!securityMode || securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_BOOTSTRAP_NOSEC : DEFAULT_PORT_BOOTSTRAP;
}

export function getDefaultPortServer (securityMode: string): number {
  return (!securityMode || securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_SERVER_NOSEC : DEFAULT_PORT_SERVER;
}

function getDefaultBootstrapSecurityConfig (): BootstrapSecurityConfig {
  return {
    servers: getDefaultBootstrapServersSecurityConfig(),
    bootstrapServer: getDefaultBootstrapServerSecurityConfig(),
    lwm2mServer: getDefaultLwM2MServerSecurityConfig()
  }
}
export function getDefaultSecurityConfig(): SecurityConfigModels {
  const securityConfigModels = {
    client: getDefaultClientSecurityConfigType(SECURITY_CONFIG_MODE.NO_SEC),
    bootstrap: getDefaultBootstrapSecurityConfig (),
    observe: getDefaultObserve()
  };
  return securityConfigModels;
}


interface ResourceLwM2M {
  id: number,
  name: string,
  isObserv: boolean
}

interface Instance {
  id: number,
  isObserv: boolean,
  resource: ResourceLwM2M[]
}

export interface ObjectLwM2M {
  id: string,
  name: string,
  instance: Instance []
}

export function getDefaultObserve (): ObjectLwM2M [] {
  return [
    {
      id: "1",
      name: "LwM2M Server",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Short Server ID",
            isObserv: false
          },
          {
            id: 1,
            name: "Lifetime",
            isObserv: false
          },
          {
            id: 2,
            name: "Default Minimum Period",
            isObserv: false
          },
          {
            id: 3,
            name: "Default Maximum Period",
            isObserv: false
          },
          {
            id: 5,
            name: "Disable Timeout",
            isObserv: false
          },
          {
            id: 6,
            name: "Notification Storing When Disabled or Offline",
            isObserv: false
          },
          {
            id: 7,
            name: "Binding",
            isObserv: false
          }
        ]
      }]
    },
    {
      id: "3",
      name: "Device",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Manufacturer",
            isObserv: false
          },
          {
            id: 1,
            name: "Model Number",
            isObserv: false
          },
          {
            id: 2,
            name: "Serial Number",
            isObserv: false
          },
          {
            id: 3,
            name: "Firmware Version",
            isObserv: false
          },
          {
            id: 6,
            name: "Available Power Sources",
            isObserv: false
          },
          {
            id: 7,
            name: "Power Source Voltage",
            isObserv: false
          },
          {
            id: 8,
            name: "Power Source Current",
            isObserv: false
          },
          {
            id: 9,
            name: "Battery Level",
            isObserv: false
          },
          {
            id: 10,
            name: "Memory Free",
            isObserv: false
          },
          {
            id: 11,
            name: "Error Code",
            isObserv: false
          },
          {
            id: 13,
            name: "Current Time",
            isObserv: false
          },
          {
            id: 14,
            name: "UTC Offset",
            isObserv: false
          },
          {
            id: 15,
            name: "Timezone",
            isObserv: false
          },
          {
            id: 16,
            name: "Supported Binding and Modes",
            isObserv: false
          },
          {
            id: 17,
            name: "Device Type",
            isObserv: false
          },
          {
            id: 18,
            name: "Hardware Version",
            isObserv: false
          },
          {
            id: 19,
            name: "Software Version",
            isObserv: false
          },
          {
            id: 20,
            name: "Battery Status",
            isObserv: false
          },
          {
            id: 21,
            name: "Memory Total",
            isObserv: false
          },
          {
            id: 22,
            name: "ExtDevInfo",
            isObserv: false
          },
        ]
      }]
    }
  ]
}


