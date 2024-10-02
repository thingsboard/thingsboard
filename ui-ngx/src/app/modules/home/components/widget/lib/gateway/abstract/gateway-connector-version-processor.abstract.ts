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

import { GatewayConnector, GatewayVersion } from '@home/components/widget/lib/gateway/gateway-widget.models';
import {
  GatewayConnectorVersionMappingUtil
} from '@home/components/widget/lib/gateway/utils/gateway-connector-version-mapping.util';

export abstract class GatewayConnectorVersionProcessor<BasicConfig> {
  gatewayVersion: number;
  configVersion: number;

  protected constructor(protected gatewayVersionIn: string | number, protected connector: GatewayConnector<BasicConfig>) {
    this.gatewayVersion = GatewayConnectorVersionMappingUtil.parseVersion(this.gatewayVersionIn);
    this.configVersion = GatewayConnectorVersionMappingUtil.parseVersion(this.connector.configVersion);
  }

  getProcessedByVersion(): GatewayConnector<BasicConfig> {
    if (!this.isVersionUpdateNeeded()) {
      return this.connector;
    }

    return this.processVersionUpdate();
  }

  private processVersionUpdate(): GatewayConnector<BasicConfig> {
    if (this.isVersionUpgradeNeeded()) {
      return this.getUpgradedVersion();
    } else if (this.isVersionDowngradeNeeded()) {
      return this.getDowngradedVersion();
    }

    return this.connector;
  }

  private isVersionUpdateNeeded(): boolean {
    if (!this.gatewayVersion) {
      return false;
    }

    return this.configVersion !== this.gatewayVersion;
  }

  private isVersionUpgradeNeeded(): boolean {
    return this.gatewayVersion >= GatewayConnectorVersionMappingUtil.parseVersion(GatewayVersion.Current)
      && (!this.configVersion || this.configVersion < this.gatewayVersion);
  }

  private isVersionDowngradeNeeded(): boolean {
    return this.configVersion && this.configVersion >= GatewayConnectorVersionMappingUtil.parseVersion(GatewayVersion.Current)
      && (this.configVersion > this.gatewayVersion);
  }

  protected abstract getDowngradedVersion(): GatewayConnector<BasicConfig>;
  protected abstract getUpgradedVersion(): GatewayConnector<BasicConfig>;
}
