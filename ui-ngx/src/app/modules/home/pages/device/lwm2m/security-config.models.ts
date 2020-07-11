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

import {SecurityConfigComponent} from "@home/pages/device/lwm2m/security-config.component";
import {GatewayFormConnectorModel, GatewayFormModels} from "@home/components/widget/lib/gateway/gateway-form.models";

export const JSON_ALL_CONFIG = 'jsonAllConfig';
export const END_POINT = 'endPoint';

export interface SecurityConfigModels {
  server: {
    securityModeServer: string,
    endpoint?: string,
    identity?: string,
    key?: string,
    x509?: boolean
  },
  bootstrap: {
    bootstrapServer: ServerSecurityConfig,
    lwm2mServer: ServerSecurityConfig
  }
}
const  DefaultServerSecurityConfig: ServerSecurityConfig = {
  host: null,
  port: null,
  bootstrapServer: false,
  securityMode: null,
  clientPublicKeyOrId: null,
  clientSecretKey: null,
  clientOldOffTime: null,
  serverId: null,
  bootstrapServerAccountTimeoutBootstrapBs: null
}

function getDefaultServerSecurityConfigBootstrap (): ServerSecurityConfig {
  let conf =  DefaultServerSecurityConfig;
  conf.bootstrapServer = true;
  return conf;
}

export const DefaultSecurityConfigModels: SecurityConfigModels = {
  server: {
    securityModeServer: '',
    endpoint: null,
    identity: null,
    key: null,
    x509: false
  },
  bootstrap: {
    bootstrapServer: DefaultServerSecurityConfig,
    lwm2mServer: getDefaultServerSecurityConfigBootstrap ()
  }
}

export enum DeviceSecurityConfigLwM2MType {
  PSK = 'PSK',
  RPK = 'RPK',
  X509 = 'X509',
  NOSEC = 'NOSEC'
}

export const credentialTypeLwM2MNames = new Map<DeviceSecurityConfigLwM2MType, string>(
  [
    [DeviceSecurityConfigLwM2MType.PSK, 'Pre-Shared Key'],
    [DeviceSecurityConfigLwM2MType.RPK, 'Raw Public Key'],
    [DeviceSecurityConfigLwM2MType.X509, 'X.509 Certificate'],
    [DeviceSecurityConfigLwM2MType.NOSEC, 'No Security'],
  ]
);

interface ServerSecurityConfig {
  host?: string,
  port?: number,
  bootstrapServer?: boolean,
  securityMode: string,
  clientPublicKeyOrId?: string,
  clientSecretKey?: string,
  clientOldOffTime?: number,
  serverId?: number,
  bootstrapServerAccountTimeoutBootstrapBs: number
}


