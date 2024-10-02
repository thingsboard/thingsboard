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

import { isEqual } from '@core/utils';
import {
  GatewayConnector,
  MQTTBasicConfig,
  MQTTBasicConfig_v3_5_2,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestType,
} from '../gateway-widget.models';
import { MqttVersionMappingUtil } from '../utils/mqtt-version-mapping.util';
import { GatewayConnectorVersionProcessor } from './gateway-connector-version-processor.abstract';

export class MqttVersionProcessor extends GatewayConnectorVersionProcessor<MQTTBasicConfig> {

  private readonly mqttRequestTypeKeys = Object.values(RequestType);

  constructor(
    protected gatewayVersionIn: string,
    protected connector: GatewayConnector<MQTTBasicConfig>
  ) {
    super(gatewayVersionIn, connector);
  }

  getUpgradedVersion(): GatewayConnector<MQTTBasicConfig_v3_5_2> {
    const {
      connectRequests,
      disconnectRequests,
      attributeRequests,
      attributeUpdates,
      serverSideRpc
    } = this.connector.configurationJson as MQTTLegacyBasicConfig;
    let configurationJson = {
      ...this.connector.configurationJson,
      requestsMapping: MqttVersionMappingUtil.mapRequestsToUpgradedVersion({
        connectRequests,
        disconnectRequests,
        attributeRequests,
        attributeUpdates,
        serverSideRpc
      }),
      mapping: MqttVersionMappingUtil.mapMappingToUpgradedVersion((this.connector.configurationJson as MQTTLegacyBasicConfig).mapping),
    };

    this.mqttRequestTypeKeys.forEach((key: RequestType) => {
      const { [key]: removedValue, ...rest } = configurationJson as MQTTLegacyBasicConfig;
      configurationJson = { ...rest } as any;
    });

    this.cleanUpConfigJson(configurationJson as MQTTBasicConfig_v3_5_2);

    return {
      ...this.connector,
      configurationJson,
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<MQTTBasicConfig_v3_5_2>;
  }

  getDowngradedVersion(): GatewayConnector<MQTTLegacyBasicConfig> {
    const { requestsMapping, mapping, ...restConfig } = this.connector.configurationJson as MQTTBasicConfig_v3_5_2;

    const updatedRequestsMapping = requestsMapping
      ? MqttVersionMappingUtil.mapRequestsToDowngradedVersion(requestsMapping as Record<RequestType, RequestMappingData[]>) : {};
    const updatedMapping = MqttVersionMappingUtil.mapMappingToDowngradedVersion(mapping);

    return {
      ...this.connector,
      configurationJson: {
        ...restConfig,
        ...updatedRequestsMapping,
        mapping: updatedMapping,
      },
      configVersion: this.gatewayVersionIn
    } as GatewayConnector<MQTTLegacyBasicConfig>;
  }

  private cleanUpConfigJson(configurationJson: MQTTBasicConfig_v3_5_2): void {
    if (isEqual(configurationJson.requestsMapping, {})) {
      delete configurationJson.requestsMapping;
    }

    if (isEqual(configurationJson.mapping, [])) {
      delete configurationJson.mapping;
    }
  }
}
