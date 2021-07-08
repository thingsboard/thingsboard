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

export const LEN_MAX_PSK = 64;
export const LEN_MAX_PRIVATE_KEY = 134;
export const LEN_MAX_PUBLIC_KEY_RPK = 182;
export const LEN_MAX_PUBLIC_KEY_X509 = 3000;
export const KEY_REGEXP_HEX_DEC = /^[-+]?[0-9A-Fa-f]+\.?[0-9A-Fa-f]*?$/;

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
  identity?: string;
  key?: string;
  cert?: string;
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

export function getDefaultClientSecurityConfig(securityConfigMode: Lwm2mSecurityType, endPoint = ''): ClientSecurityConfig {
  let security =  {
    securityConfigClientMode: securityConfigMode,
    endpoint: endPoint,
    identity: '',
    key: '',
  };
  switch (securityConfigMode) {
    case Lwm2mSecurityType.X509:
      security = { ...security, ...{cert: ''}};
      break;
    case Lwm2mSecurityType.PSK:
      security = { ...security, ...{identity: endPoint, key: ''}};
      break;
    case Lwm2mSecurityType.RPK:
      security = { ...security, ...{key: ''}};
      break;
  }
  return security;
}

export function getDefaultServerSecurityConfig(): ServerSecurityConfig {
  return {
    securityMode: Lwm2mSecurityType.NO_SEC
  };
}
