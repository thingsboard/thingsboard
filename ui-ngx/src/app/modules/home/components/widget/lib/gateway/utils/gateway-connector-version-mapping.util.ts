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
  ModbusBasicConfig,
  MQTTBasicConfig,
  OPCBasicConfig,
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { MqttVersionProcessor } from '@home/components/widget/lib/gateway/abstract/mqtt-version-processor.abstract';
import { OpcVersionProcessor } from '@home/components/widget/lib/gateway/abstract/opc-version-processor.abstract';
import { ModbusVersionProcessor } from '@home/components/widget/lib/gateway/abstract/modbus-version-processor.abstract';
import { isNumber, isString } from '@core/utils';

export abstract class GatewayConnectorVersionMappingUtil {

  static getConfig(connector: GatewayConnector, gatewayVersion: string): GatewayConnector {
    switch(connector.type) {
      case ConnectorType.MQTT:
        return new MqttVersionProcessor(gatewayVersion, connector as GatewayConnector<MQTTBasicConfig>).getProcessedByVersion();
      case ConnectorType.OPCUA:
        return new OpcVersionProcessor(gatewayVersion, connector as GatewayConnector<OPCBasicConfig>).getProcessedByVersion();
      case ConnectorType.MODBUS:
        return new ModbusVersionProcessor(gatewayVersion, connector as GatewayConnector<ModbusBasicConfig>).getProcessedByVersion();
      default:
        return connector;
    }
  }

  static parseVersion(version: string | number): number {
    if (isNumber(version)) {
      return version as number;
    }

    return isString(version) ? parseFloat((version as string).replace(/\./g, '').slice(0, 3)) / 100 : 0;
  }
}
