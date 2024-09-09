import {
  GatewayConnectorVersionProcessor
} from '@home/components/widget/lib/gateway/abstract/gateway-connector-version-processor.abstract';
import {
  GatewayConnector,
  MQTTBasicConfig, MQTTBasicConfig_v3_5_2,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestType,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isEqual } from '@core/utils';
import { MqttVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/mqtt-version-mapping.util';

export class MqttVersionProcessor extends GatewayConnectorVersionProcessor<MQTTBasicConfig> {
  private readonly mqttRequestTypeKeys = Object.values(RequestType);

  constructor(
    protected gatewayVersionStr: string,
    protected connector: GatewayConnector
  ) {
    super(gatewayVersionStr, connector);
  }
  getUpgradedVersion(): MQTTBasicConfig {
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
      const { [key]: removedKey, ...rest } = configurationJson as MQTTLegacyBasicConfig;
      configurationJson = { ...rest } as any;
    });

    this.cleanUpConfigJson(configurationJson as MQTTBasicConfig_v3_5_2);

    return configurationJson as MQTTBasicConfig;
  }

  getDowngradedVersion(): MQTTLegacyBasicConfig {
    const { requestsMapping, mapping, ...restConfig } = this.connector.configurationJson as MQTTBasicConfig_v3_5_2;

    const updatedRequestsMapping =
      MqttVersionMappingUtil.mapRequestsToDowngradedVersion(requestsMapping as Record<RequestType, RequestMappingData[]>);
    const updatedMapping = MqttVersionMappingUtil.mapMappingToDowngradedVersion(mapping);

    return {
      ...restConfig,
      ...updatedRequestsMapping,
      mapping: updatedMapping,
    };
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
