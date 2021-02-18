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

export const JSON_ALL_CONFIG = 'jsonAllConfig';
export const END_POINT = 'endPoint';
export const DEFAULT_END_POINT = 'default_client_lwm2m_end_point_no_sec';
export const BOOTSTRAP_SERVERS = 'servers';
export const BOOTSTRAP_SERVER = 'bootstrapServer';
export const LWM2M_SERVER = 'lwm2mServer';
export const JSON_OBSERVE = 'jsonObserve';
export const LEN_MAX_PSK = 64;
export const LEN_MAX_PRIVATE_KEY = 134;
export const LEN_MAX_PUBLIC_KEY_RPK = 182;
export const LEN_MAX_PUBLIC_KEY_X509 = 3000;
export const KEY_REGEXP_HEX_DEC = /^[-+]?[0-9A-Fa-f]+\.?[0-9A-Fa-f]*?$/;


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
  | ClientSecurityConfigNoSEC;

export interface ClientSecurityConfigPSK {
  securityConfigClientMode: string;
  endpoint: string;
  identity: string;
  key: string;
}

export interface ClientSecurityConfigRPK {
  securityConfigClientMode: string;
  key: string;
}

export interface ClientSecurityConfigX509 {
  securityConfigClientMode: string;
  x509: boolean;
}

export interface ClientSecurityConfigNoSEC {
  securityConfigClientMode: string;
}

export interface ServerSecurityConfig {
  securityMode: string;
  clientPublicKeyOrId?: string;
  clientSecretKey?: string;
}

interface BootstrapSecurityConfig {
  bootstrapServer: ServerSecurityConfig;
  lwm2mServer: ServerSecurityConfig;
}

export interface SecurityConfigModels {
  client: ClientSecurityConfigType;
  bootstrap: BootstrapSecurityConfig;
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
      };
      break;
    case SECURITY_CONFIG_MODE.RPK:
      security = {
        securityConfigClientMode: '',
        key: ''
      };
      break;
    case SECURITY_CONFIG_MODE.X509:
      security = {
        securityConfigClientMode: '',
        x509: true
      };
      break;
    case SECURITY_CONFIG_MODE.NO_SEC:
      security = {
        securityConfigClientMode: ''
      };
      break;
  }
  security.securityConfigClientMode = securityConfigMode.toString();
  return security;
}

export function getDefaultServerSecurityConfig(): ServerSecurityConfig {
  return {
    securityMode: SECURITY_CONFIG_MODE.NO_SEC.toString(),
    clientPublicKeyOrId: '',
    clientSecretKey: ''
  };
}

function getDefaultBootstrapSecurityConfig(): BootstrapSecurityConfig {
  return {
    bootstrapServer: getDefaultServerSecurityConfig(),
    lwm2mServer:  getDefaultServerSecurityConfig()
  };
}

export function getDefaultSecurityConfig(): SecurityConfigModels {
  const securityConfigModels = {
    client: getDefaultClientSecurityConfigType(SECURITY_CONFIG_MODE.NO_SEC),
    bootstrap: getDefaultBootstrapSecurityConfig()
  };
  return securityConfigModels;
}


