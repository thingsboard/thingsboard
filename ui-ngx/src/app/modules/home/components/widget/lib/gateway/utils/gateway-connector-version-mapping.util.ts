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
  ConnectorType,
  GatewayConnector,
  LegacyGatewayConnector,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { MqttVersionMappingUtil } from './mqtt-version-mapping.util';

export class GatewayConnectorVersionMappingUtil {

  static getMappedByVersion(connector: GatewayConnector, gatewayVersion: string): GatewayConnector {
    switch (connector.type) {
      case ConnectorType.MQTT:
        return this.getMappedMQTTByVersion(connector, gatewayVersion);
      default:
        return connector;
    }
  }

  private static getMappedMQTTByVersion(
    connector: GatewayConnector | LegacyGatewayConnector,
    gatewayVersion: string
  ): GatewayConnector | LegacyGatewayConnector {
    if (this.isVersionUpdateNeeded(gatewayVersion, connector.configVersion)) {
      return this.isGatewayOutdated(gatewayVersion, connector.configVersion)
        ? MqttVersionMappingUtil.getLegacyVersion(connector)
        : MqttVersionMappingUtil.getNewestVersion(connector);
    }
    return connector;
  }

  private static isGatewayOutdated(gatewayVersion: string, configVersion: string): boolean {
    if (!gatewayVersion || !configVersion) {
      return false;
    }

    return this.parseVersion(configVersion) > this.parseVersion(gatewayVersion);
  }

  private static isVersionUpdateNeeded(configVersion: string, gatewayVersion: string): boolean {
    if (!gatewayVersion || !configVersion) {
      return false;
    }

    return this.parseVersion(configVersion) !== this.parseVersion(gatewayVersion);
  }

  private static parseVersion(version: string): number {
    return Number(version?.replace(/\./g, ''));
  }
}
