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

export abstract class GatewayConnectorVersionProcessor<BasicConfig> {
  gatewayVersion: number;
  configVersion: number;

  protected constructor(protected gatewayVersionStr: string, protected connector: GatewayConnector<BasicConfig>) {
    this.gatewayVersion = this.parseVersion(this.gatewayVersionStr);
    this.configVersion = this.parseVersion(connector.configVersion);
  }

  getProcessedByVersion(): GatewayConnector<BasicConfig> {
    if (this.isVersionUpdateNeeded()) {
      return this.isVersionUpgradeNeeded()
        ? this.getUpgradedVersion()
        : this.getDowngradedVersion();
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
    return this.gatewayVersionStr === GatewayVersion.Current && (!this.configVersion || this.configVersion < this.gatewayVersion);
  }

  private parseVersion(version: string): number {
    return Number(version?.replace(/\./g, ''));
  }

  protected abstract getDowngradedVersion(): GatewayConnector<BasicConfig>;
  protected abstract getUpgradedVersion(): GatewayConnector<BasicConfig>;
}
