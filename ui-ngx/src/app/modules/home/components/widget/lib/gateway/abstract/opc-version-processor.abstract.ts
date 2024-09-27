///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  GatewayConnector, LegacyServerConfig,
  OPCBasicConfig,
  OPCBasicConfig_v3_5_2,
  OPCLegacyBasicConfig,
} from '../gateway-widget.models';
import { GatewayConnectorVersionProcessor } from './gateway-connector-version-processor.abstract';
import { OpcVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/opc-version-mapping.util';

export class OpcVersionProcessor extends GatewayConnectorVersionProcessor<OPCBasicConfig> {

  constructor(
    protected gatewayVersionIn: string,
    protected connector: GatewayConnector<OPCBasicConfig>
  ) {
    super(gatewayVersionIn, connector);
  }

  getUpgradedVersion(): GatewayConnector<OPCBasicConfig_v3_5_2> {
    const server = this.connector.configurationJson.server as LegacyServerConfig;
    return {
      ...this.connector,
      configurationJson: {
        server: server ? OpcVersionMappingUtil.mapServerToUpgradedVersion(server) : {},
        mapping: server?.mapping ? OpcVersionMappingUtil.mapMappingToUpgradedVersion(server.mapping) : [],
      },
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<OPCBasicConfig_v3_5_2>;
  }

  getDowngradedVersion(): GatewayConnector<OPCLegacyBasicConfig> {
    return {
      ...this.connector,
      configurationJson: {
        server: OpcVersionMappingUtil.mapServerToDowngradedVersion(this.connector.configurationJson as OPCBasicConfig_v3_5_2)
      },
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<OPCLegacyBasicConfig>;
  }
}
