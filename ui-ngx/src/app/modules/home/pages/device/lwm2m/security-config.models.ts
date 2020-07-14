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

export const JSON_PROFILE = 'jsonProfile';
export const JSON_ALL_CONFIG = 'jsonAllConfig';
export const END_POINT = 'endPoint';
export const BOOTSTRAP_SERVER = 'bootstrapServer';
export const LWM2M_SERVER = 'lwm2mServer';
export const DEFAULT_ID_SERVER = 123;
export const DEFAULT_PORT_SERVER = 5686;
export const DEFAULT_PORT_SERVER_NOSEC = 5685;
export const DEFAULT_ID_BOOTSTRAP = 111;
export const DEFAULT_PORT_BOOTSTRAP = 5688;
export const DEFAULT_PORT_BOOTSTRAP_NOSEC = 5687;
export const DEFAULT_HOST = 'localhost';

export type ClientSecurityInfo =
  ClientSecurityInfoPSK
  | ClientSecurityInfoRPK
  | ClientSecurityInfoX509
  | ClientSecurityInfoNoSec;

export interface ClientSecurityInfoPSK {
  securityModeServer: string,
  endpoint: string,
  identity: string,
  key: string
}

export interface ClientSecurityInfoRPK {
  securityModeServer: string,
  key: string
}

export interface ClientSecurityInfoX509 {
  securityModeServer: string,
  x509: boolean
}

interface ClientSecurityInfoNoSec {
  securityModeServer: string
}

export interface SecurityConfigModels {
  client: ClientSecurityInfo,
  bootstrap: {
    servers: {
      shortId: number,
      lifetime: number,
      defaultMinPeriod: number,
      notifIfDisabled: boolean,
      binding: string
    },
    bootstrapServer: ServerSecurityConfig,
    lwm2mServer: ServerSecurityConfig
  },
  observe: ObjectLwM2M[]
}

function getDefaultBootstrapSecurityConfig(securityConfigMode: SECURITY_CONFIG_MODE): BootstrapSecurityConfig {
  const DefaultBootstrapSecurityConfig: BootstrapSecurityConfig = {
    host: '',
    port: DEFAULT_PORT_BOOTSTRAP,
    isBootstrapServer: true,
    securityMode: securityConfigMode.toString(),
    clientPublicKeyOrId: '',
    clientSecretKey: '',
    serverPublicKey: '',
    clientHoldOffTime: 1,
    serverId: DEFAULT_ID_BOOTSTRAP,
    bootstrapServerAccountTimeout: 0
  }
  return DefaultBootstrapSecurityConfig;
}

function getDefaultServerSecurityConfig(securityConfigMode: SECURITY_CONFIG_MODE): ServerSecurityConfig {
  const DefaultServerSecurityConfig: ServerSecurityConfig = {
    host: '',
    port: DEFAULT_PORT_SERVER,
    isBootstrapServer: false,
    securityMode: securityConfigMode.toString(),
    clientPublicKeyOrId: '',
    clientSecretKey: '',
    serverPublicKey: '',
    clientHoldOffTime: 1,
    serverId: DEFAULT_ID_SERVER,
    bootstrapServerAccountTimeout: 0
  }
  return DefaultServerSecurityConfig;
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

export interface SecurityConfig {
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

interface ServerSecurityConfig extends SecurityConfig {
}

interface BootstrapSecurityConfig extends SecurityConfig {
}

export function gatDefaultSecurityConfig(securityConfigModeIn: SECURITY_CONFIG_MODE, endPoint: string): SecurityConfigModels {
  debugger
  const securityConfigModels = {
    client: getClientSecurityInfo(securityConfigModeIn, endPoint),
    bootstrap: {
      servers: {
        shortId: DEFAULT_ID_SERVER,
        lifetime: 300,
        defaultMinPeriod: 1,
        notifIfDisabled: true,
        binding: "U"
      },
      bootstrapServer: getDefaultBootstrapSecurityConfig(securityConfigModeIn),
      lwm2mServer: getDefaultServerSecurityConfig(securityConfigModeIn)
    },
    observe: getDefaultProfile()
  };
  return securityConfigModels;
}

function getClientSecurityInfo(securityConfigMode: SECURITY_CONFIG_MODE, endPoint: string): ClientSecurityInfo {
  debugger
  let security: ClientSecurityInfo;
  switch (securityConfigMode) {
    case SECURITY_CONFIG_MODE.PSK:
      security = {
        securityModeServer: '',
        endpoint: endPoint,
        identity: '',
        key: ''
      }
      break;
    case SECURITY_CONFIG_MODE.RPK:
      security = {
        securityModeServer: '',
        key: null
      }
      break;
    case SECURITY_CONFIG_MODE.X509:
      security = {
        securityModeServer: '',
        x509: true
      }
      break;
    case SECURITY_CONFIG_MODE.NO_SEC:
      security = {
        securityModeServer: ''
      }
      break;
  }
  security.securityModeServer = securityConfigMode.toString();
  return security;
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

interface ObjectLwM2M {
  id: string,
  name: string,
  instance: Instance []
}

function getDefaultProfile (): ObjectLwM2M [] {
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



