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
export const LEN_MAX_PSK = 64;
export const LEN_MAX_PRIVATE_KEY = 134;
export const LEN_MAX_PUBLIC_KEY_RPK = 182;
export const LEN_MAX_PUBLIC_KEY_X509 = 3000;
export const KEY_REGEXP_HEX_DEC = /^[-+]?[0-9A-Fa-f]+\.?[0-9A-Fa-f]*?$/;


export interface DeviceCredentialsDialogLwm2mData {
  jsonAllConfig?: Lwm2mSecurityConfigModels;
  endPoint?: string;
}

export enum Lwm2mSecurityType {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NO_SEC = 'NO_SEC'
}

export const Lwm2mSecurityTypeTranslationMap = new Map<Lwm2mSecurityType, string>(
  [
    [Lwm2mSecurityType.PSK, 'Pre-Shared Key'],
    [Lwm2mSecurityType.RPK, 'Raw Public Key'],
    [Lwm2mSecurityType.X509, 'X.509 Certificate'],
    [Lwm2mSecurityType.NO_SEC, 'No Security'],
  ]
);

export interface ClientSecurityConfig {
  securityConfigClientMode: Lwm2mSecurityType;
  endpoint: string;
  identity: string;
  key: string;
  x509: boolean;
}

export interface ServerSecurityConfig {
  securityMode: Lwm2mSecurityType;
  clientPublicKeyOrId?: string;
  clientSecretKey?: string;
}

interface BootstrapSecurityConfig {
  bootstrapServer: ServerSecurityConfig;
  lwm2mServer: ServerSecurityConfig;
}

export interface Lwm2mSecurityConfigModels {
  client: ClientSecurityConfig;
  bootstrap: BootstrapSecurityConfig;
}

export function getClientSecurityConfig(securityConfigMode: Lwm2mSecurityType, endPoint = ''): ClientSecurityConfig {
  const security = getDefaultClientSecurityConfig(securityConfigMode);
  switch (securityConfigMode) {
    case Lwm2mSecurityType.PSK:
      security.endpoint =  endPoint;
      security.identity =  endPoint;
      break;
    case Lwm2mSecurityType.X509:
      security.x509 = true;
      break;
  }

  return security;
}

export function getDefaultClientSecurityConfig(securityConfigMode: Lwm2mSecurityType): ClientSecurityConfig {
  return {
    securityConfigClientMode: securityConfigMode,
    endpoint: '',
    identity: '',
    key: '',
    x509: false
  };
}

export function getDefaultServerSecurityConfig(): ServerSecurityConfig {
  return {
    securityMode: Lwm2mSecurityType.NO_SEC,
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

export function getDefaultSecurityConfig(): Lwm2mSecurityConfigModels {
  const securityConfigModels = {
    client: getClientSecurityConfig(Lwm2mSecurityType.NO_SEC),
    bootstrap: getDefaultBootstrapSecurityConfig()
  };
  return securityConfigModels;
}

const isSecurityConfigModels = (p: any): boolean =>
  p.hasOwnProperty('client') &&
    isClientSecurityConfigType(p.client) &&
  p.hasOwnProperty('bootstrap') &&
    isBootstrapSecurityConfig(p.bootstrap);

const isClientSecurityConfigType = (p: any): boolean =>
  p.hasOwnProperty('securityConfigClientMode') &&
  p.hasOwnProperty('endpoint') &&
  p.hasOwnProperty('identity') &&
  p.hasOwnProperty('key') &&
  p.hasOwnProperty('x509');

const isBootstrapSecurityConfig = (p: any): boolean =>
  p.hasOwnProperty('bootstrapServer') &&
    isServerSecurityConfig(p.bootstrapServer) &&
  p.hasOwnProperty('lwm2mServer') &&
    isServerSecurityConfig(p.lwm2mServer);

const isServerSecurityConfig = (p: any): boolean =>
  p.hasOwnProperty('securityMode') &&
  p.hasOwnProperty('clientPublicKeyOrId') &&
  p.hasOwnProperty('clientSecretKey');

export function validateSecurityConfig(config: string): boolean {
  try {
    const securityConfig = JSON.parse(config);
    return isSecurityConfigModels(securityConfig);
  } catch (e) {
    return false;
  }
}


