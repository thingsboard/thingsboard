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
export const OBSERVE_ATTR = 'observeAttr';
export const OBSERVE = 'observe';
export const ATTR = 'attribute';
export const JSON_OBSERVE = 'jsonObserve';
export const DEFAULT_ID_SERVER = 123;
const DEFAULT_ID_BOOTSTRAP = 111;
export const DEFAULT_PORT_SERVER_NO_SEC = 5685;
export const DEFAULT_PORT_SERVER_SEC = 5686;
export const DEFAULT_PORT_SERVER_SEC_CERT = 5688;
export const DEFAULT_PORT_BOOTSTRAP_NO_SEC = 5689;
export const DEFAULT_PORT_BOOTSTRAP_SEC = 5690;
export const DEFAULT_PORT_BOOTSTRAP_SEC_CERT = 5692;
export const DEFAULT_CLIENT_HOLD_OFF_TIME = 1;
export const DEFAULT_LIFE_TIME = 300;
export const DEFAULT_DEFAULT_MIN_PERIOD =  1;
const DEFAULT_NOTIF_IF_DESIBLED =  true;
export const DEFAULT_BINDING = "U";
const DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT = 0;
export const LEN_MAX_PSK = 64;
export const LEN_MAX_PRIVATE_KEY = 134;
export const LEN_MAX_PUBLIC_KEY_RPK = 182;
export const LEN_MAX_PUBLIC_KEY_X509 = 3000;
export const KEY_IDENT_REGEXP_PSK =/^[0-9a-fA-F]{64,64}$/;
export const KEY_PRIVATE_REGEXP =/^[0-9a-fA-F]{134,134}$/;
export const KEY_PUBLIC_REGEXP_PSK =/^[0-9a-fA-F]{182,182}$/;
export const KEY_PUBLIC_REGEXP_X509 =/^[0-9a-fA-F]{0,3000}$/;

export interface DeviceCredentialsDialogLwm2mData {
  jsonAllConfig?: SecurityConfigModels;
  endPoint?: string;
  isNew?: boolean;
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

export interface ClientSecurityConfigNO_SEC {
  securityConfigClientMode: string
}

export interface BootstrapServersSecurityConfig {
  shortId: number,
  lifetime: number,
  defaultMinPeriod: number,
  notifIfDisabled: boolean,
  binding: string
}

export interface ServerSecurityConfig {
  host?: string,
  port?: number,
  bootstrapServerIs?: boolean,
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
  observeAttr: ObjectLwM2M[]
}

export function getDefaultClientSecurityConfigType(securityConfigMode: SECURITY_CONFIG_MODE, endPoint?: string): ClientSecurityConfigType {
  let security: ClientSecurityConfigType;
  switch (securityConfigMode) {
    case SECURITY_CONFIG_MODE.PSK:
      security = {
        securityConfigClientMode: '',
        endpoint: endPoint,
        identity: endPoint,
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

export function getDefaultBootstrapServerSecurityConfig(hostname: any): ServerSecurityConfig {
  return {
    host: hostname,
    port: getDefaultPortBootstrap(),
    bootstrapServerIs: true,
    securityMode: SECURITY_CONFIG_MODE.NO_SEC.toString(),
    clientPublicKeyOrId: '',
    clientSecretKey: '',
    serverPublicKey: '',
    clientHoldOffTime: DEFAULT_CLIENT_HOLD_OFF_TIME,
    serverId: DEFAULT_ID_BOOTSTRAP,
    bootstrapServerAccountTimeout: DEFAULT_BOOTSTRAP_SERVER_ACCOUNT_TIME_OUT
  }
}

export function getDefaultLwM2MServerSecurityConfig(hostname): ServerSecurityConfig {
  const DefaultLwM2MServerSecurityConfig =  getDefaultBootstrapServerSecurityConfig(hostname);
  DefaultLwM2MServerSecurityConfig.bootstrapServerIs = false;
  DefaultLwM2MServerSecurityConfig.port = getDefaultPortServer();
  DefaultLwM2MServerSecurityConfig.serverId = DEFAULT_ID_SERVER;
  return DefaultLwM2MServerSecurityConfig;
}

export function getDefaultPortBootstrap (securityMode?: string): number {
  return (!securityMode || securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_BOOTSTRAP_NO_SEC :
    (securityMode === SECURITY_CONFIG_MODE.X509.toString()) ? DEFAULT_PORT_BOOTSTRAP_SEC_CERT : DEFAULT_PORT_BOOTSTRAP_SEC;
}

export function getDefaultPortServer (securityMode?: string): number {
  return (!securityMode || securityMode === SECURITY_CONFIG_MODE.NO_SEC.toString()) ? DEFAULT_PORT_SERVER_NO_SEC :
    (securityMode === SECURITY_CONFIG_MODE.X509.toString()) ?  DEFAULT_PORT_SERVER_SEC_CERT : DEFAULT_PORT_SERVER_SEC;
}

function getDefaultBootstrapSecurityConfig(hostname: any): BootstrapSecurityConfig {
  return {
    servers: getDefaultBootstrapServersSecurityConfig(),
    bootstrapServer: getDefaultBootstrapServerSecurityConfig(hostname),
    lwm2mServer: getDefaultLwM2MServerSecurityConfig(hostname)
  }
}
export function getDefaultSecurityConfig(hostname: any): SecurityConfigModels {
  const securityConfigModels = {
    client: getDefaultClientSecurityConfigType(SECURITY_CONFIG_MODE.NO_SEC),
    bootstrap: getDefaultBootstrapSecurityConfig (hostname),
    observeAttr: []
  };
  return securityConfigModels;
}


export interface ResourceLwM2M {
  id: number,
  name: string,
  isObserv: boolean,
  isAttr: boolean
}

export interface Instance {
  id: number,
  isObserv: boolean,
  resource: ResourceLwM2M[]
}

export interface ObjectLwM2M {
  id: number,
  name: string,
  instance: Instance []
}

export function getDefaultClientObserveAttr (): ObjectLwM2M [] {
  return [
    {
      id: 1,
      name: "LwM2M Server",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Short Server ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Lifetime",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Default Minimum Period",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Default Maximum Period",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Disable Timeout",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "Notification Storing When Disabled or Offline",
            isObserv: false,
            isAttr: false
          },
          {
            id: 7,
            name: "Binding",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 2,
      name: "Access control",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Object ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Object Instance ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "ACL",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Access Control Owner",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3,
      name: "Device",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Manufacturer",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Model Number",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Serial Number",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Firmware Version",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "Available Power Sources",
            isObserv: false,
            isAttr: false
          },
          {
            id: 7,
            name: "Power Source Voltage",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Power Source Current",
            isObserv: false,
            isAttr: false
          },
          {
            id: 9,
            name: "Battery Level",
            isObserv: false,
            isAttr: false
          },
          {
            id: 10,
            name: "Memory Free",
            isObserv: false,
            isAttr: false
          },
          {
            id: 11,
            name: "Error Code",
            isObserv: false,
            isAttr: false
          },
          {
            id: 13,
            name: "Current Time",
            isObserv: false,
            isAttr: false
          },
          {
            id: 14,
            name: "UTC Offset",
            isObserv: false,
            isAttr: false
          },
          {
            id: 15,
            name: "Timezone",
            isObserv: false,
            isAttr: false
          },
          {
            id: 16,
            name: "Supported Binding and Modes",
            isObserv: false,
            isAttr: false
          },
          {
            id: 17,
            name: "Device Type",
            isObserv: false,
            isAttr: false
          },
          {
            id: 18,
            name: "Hardware Version",
            isObserv: false,
            isAttr: false
          },
          {
            id: 19,
            name: "Software Version",
            isObserv: false,
            isAttr: false
          },
          {
            id: 20,
            name: "Battery Status",
            isObserv: false,
            isAttr: false
          },
          {
            id: 21,
            name: "Memory Total",
            isObserv: false,
            isAttr: false
          },
          {
            id: 22,
            name: "ExtDevInfo",
            isObserv: false,
            isAttr: false
          },
        ]
      }]
    },
    {
      id: 4,
      name: "Connectivity monitoring",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Network Bearer",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Available Network Bearer",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Radio Signal Strength",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Link Quality",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "IP Addresses",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Router IP Addresses",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "Link Utilization",
            isObserv: false,
            isAttr: false
          },
          {
            id: 7,
            name: "APN",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Cell ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 9,
            name: "SMNC",
            isObserv: false,
            isAttr: false
          },
          {
            id: 10,
            name: "SMCC",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 5,
      name: "Firmware upgrade",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Package",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Package URI",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "State",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Update Result",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "PkgName",
            isObserv: false,
            isAttr: false
          },
          {
            id: 7,
            name: "PkgVersion",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Firmware Update Protocol Support",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 6,
      name: "Location",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Latitude",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Longitude",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Altitude",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Radius",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "Velocity",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Timestamp",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "Speed",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 7,
      name: "Connectivity Statistics",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "SMS Tx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "SMS Rx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Tx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Rx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "Max Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Average Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Collection Period",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 10,
      name: "Cellular connectivity",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "SMSC address",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Disable radio period",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Module activation code",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Vendor specific extensions",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "PSM Timer (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Active Timer (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6,
            name: "Serving PLMN Rate control",
            isObserv: false,
            isAttr: false
          },
          {
            id: 7,
            name: "eDRX parameters for lu mode (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "eDRX parameters for WB-S1 mode (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 9,
            name: "eDRX parameters for NB-S1 mode (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 10,
            name: "eDRX parameters for A/Gb mode (1)",
            isObserv: false,
            isAttr: false
          },
          {
            id: 11,
            name: "Activated Profile Names",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 11,
      name: "APN Connection Profile",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "SMS Tx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "SMS Rx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Tx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Rx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "Max Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Average Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Collection Period",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 13,
      name: "Bearer Selection",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "SMS Tx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "SMS Rx Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Tx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Rx Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "Max Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "Average Message Size",
            isObserv: false,
            isAttr: false
          },
          {
            id: 8,
            name: "Collection Period",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 16,
      name: "Portfolio",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Identity",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "AuthData",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "AuthStatus",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 18,
      name: "Stratum (NAS) Configuration",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Identity",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "AuthData",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "AuthStatus",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 19,
      name: "BinaryAppDataContainer",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Data",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Data Priority",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Data Creation Time",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Data Description",
            isObserv: false,
            isAttr: false
          },
          {
            id: 4,
            name: "Data Format",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5,
            name: "App ID ",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3200,
      name: "Digital input",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 5500,
            name: "Digital Input State",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5501,
            name: "Digital Input Counter",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5502,
            name: "Digital Input Polarity",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5503,
            name: "Digital Input Debounce",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5504,
            name: "Digital Input Edge Selection",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5505,
            name: "Digital Input Counter Reset",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5750,
            name: "Application Type",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5751,
            name: "Sensor Type",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3201,
      name: "Digital output",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 5550,
            name: "Digital Output State",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5551,
            name: "Digital Output Polarity",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5750,
            name: "Application Type",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3202,
      name: "Analog input",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 5600,
            name: "Analog Input Current Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5601,
            name: "Min Measured Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5602,
            name: "Max Measured Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5603,
            name: "Min Range Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5604,
            name: "Max Range Value",
            isObserv: false,
            isAttr: false,
          },
          {
            id: 5750,
            name: "Application Type",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5751,
            name: "Sensor Type",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3303,
      name: "Temperature",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 5700,
            name: "Sensor Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5601,
            name: "Min Measured Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5602,
            name: "Max Measured Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5603,
            name: "Min Range Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5604,
            name: "Max Range Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5701,
            name: "Sensor Units",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5605,
            name: "Reset Min and Max Measured Values",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3311,
      name: "Light Control",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 5701,
            name: "Sensor Units",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5706,
            name: "Colour",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5750,
            name: "Application Type",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5805,
            name: "Cumulative active power",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5820,
            name: "Power factor",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5850,
            name: "On/Off",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5851,
            name: "Dimmer",
            isObserv: false,
            isAttr: false
          },
          {
            id: 5852,
            name: "On time",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 3353,
      name: "scellID",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 6030,
            name: "plmnID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6031,
            name: "BandIndicator",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6032,
            name: "TrackingAreaCode",
            isObserv: false,
            isAttr: false
          },
          {
            id: 6033,
            name: "CellID",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 26241,
      name: "RTU coil",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Slave ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Address",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Status",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    },
    {
      id: 26242,
      name: "RTU Register",
      instance:[{
        id: 0,
        isObserv: false,
        resource: [
          {
            id: 0,
            name: "Value",
            isObserv: false,
            isAttr: false
          },
          {
            id: 1,
            name: "Slave ID",
            isObserv: false,
            isAttr: false
          },
          {
            id: 2,
            name: "Address",
            isObserv: false,
            isAttr: false
          },
          {
            id: 3,
            name: "Status",
            isObserv: false,
            isAttr: false
          }
        ]
      }]
    }
  ]
}

// Bootsrap server -> api: bootstrap
export const BOOTSTRAP_PUBLIC_KEY_RPK = '3059301306072A8648CE3D020106082A8648CE3D03010703420004993EF2B698C6A9C0C1D8BE78B13A9383C0854C7C7C7A504D289B403794648183267412D5FC4E5CEB2257CB7FD7F76EBDAC2FA9AA100AFB162E990074CC0BFAA2';
export const LWM2M_SERVER_PUBLIC_KEY_RPK = '3059301306072A8648CE3D020106082A8648CE3D03010703420004405354EA8893471D9296AFBC8B020A5C6201B0BB25812A53B849D4480FA5F06930C9237E946A3A1692C1CAFAA01A238A077F632C99371348337512363F28212B';
export const BOOTSTRAP_PUBLIC_KEY_X509 = '30820249308201eca003020102020439d220d5300c06082a8648ce3d04030205003076310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f6172643121301f060355040313186e69636b2d5468696e6773626f6172642020726f6f7443413020170d3230303632343039313230395a180f32313230303533313039313230395a308197310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f61726431423040060355040313396e69636b2d5468696e6773626f61726420626f6f74737472617020736572766572204c774d324d207369676e656420627920726f6f742043413059301306072a8648ce3d020106082a8648ce3d03010703420004cf870030ce976dd3d1b034f135ef299fbbb288b0c54af5a5aef08239c635d615577f37fb8282f0ce1706db2bd83bb46eea05584b6db04ce0f08494875153d140a3423040301f0603551d23041830168014330c72547f0c8ae50332260ee1d29e172cdcbde7301d0603551d0e041604143ee7b65fef5f50da8b026b10ab0a4835e9db0aec300c06082a8648ce3d04030205000349003046022100a2c5a3617f9315d10782e3911519b7c9a27b6bbc87c8ca7aad2c5978a88cf8ad022100bd6682c9f87e09d94f498d277d2e8b86b35c4c0b0f3541305ed3f4e8c30d971f';
export const LWM2M_SERVER_PUBLIC_KEY_X509 = '3082023f308201e2a003020102020452e452ab300c06082a8648ce3d04030205003076310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f6172643121301f060355040313186e69636b2d5468696e6773626f6172642020726f6f7443413020170d3230303632343039313230385a180f32313230303533313039313230385a30818d310b3009060355040613025553310b3009060355040813024341310b300906035504071302534631143012060355040a130b5468696e6773626f61726431143012060355040b130b5468696e6773626f617264313830360603550403132f6e69636b2d5468696e6773626f61726420736572766572204c774d324d207369676e656420627920726f6f742043413059301306072a8648ce3d020106082a8648ce3d0301070342000461cde351cfeca5e4c65957d538982226b6625d2e456f0c7e993d8be8f23d5779441ba34cffe84d34acd4ba67d100861100edd0e77e70c0582324f4ed335c171ca3423040301f0603551d23041830168014330c72547f0c8ae50332260ee1d29e172cdcbde7301d0603551d0e0416041490eff1d5323fcd5620da145a7cfd27eeefb8d34a300c06082a8648ce3d04030205000349003046022100b21c02023ed29382441d2b4fcba2d28dbfad6f7e37349594819acc87dd7600d4022100c053000ef668187f6ab567c3401e8a67206e97a534c0db8400d819151a9c0f2e';

