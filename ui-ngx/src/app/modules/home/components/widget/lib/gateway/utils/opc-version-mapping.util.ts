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
  DeviceConnectorMapping,
  LegacyDeviceConnectorMapping,
  LegacyServerConfig,
  OPCBasicConfig_v3_5_2,
  ServerConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class OpcVersionMappingUtil {
  static mapServerToUpgradedVersion(server: LegacyServerConfig): ServerConfig {
    const { mapping, disableSubscriptions, ...restServer } = server;
    return {
      ...restServer,
      enableSubscriptions: !disableSubscriptions,
    };
  }

  static mapServerToDowngradedVersion(config: OPCBasicConfig_v3_5_2): LegacyServerConfig {
    const { mapping, server } = config;
    const { enableSubscriptions, ...restServer } = server;
    return {
      ...restServer,
      mapping: mapping ? this.mapMappingToDowngradedVersion(mapping) : [],
      disableSubscriptions: !enableSubscriptions,
    };
  }

  static mapMappingToUpgradedVersion(mapping: LegacyDeviceConnectorMapping[]): DeviceConnectorMapping[] {
    return mapping?.map((oldMapping: any) => ({
      ...oldMapping,
      deviceNodeSource: 'path',
      deviceInfo: {
        deviceNameExpression: oldMapping.deviceNamePattern,
        deviceNameExpressionSource: 'path',
        deviceProfileExpression: oldMapping.deviceTypePattern ?? 'default',
        deviceProfileExpressionSource: 'path',
      },
      attributes: oldMapping.attributes.map(attribute => ({
        key: attribute.key,
        type: 'path',
        value: attribute.path,
      })),
      attributes_updates: oldMapping.attributes_updates.map(attributeUpdate => ({
        key: attributeUpdate.attributeOnThingsBoard,
        type: 'path',
        value: attributeUpdate.attributeOnDevice,
      })),
      timeseries: oldMapping.timeseries.map(timeseries => ({
        key: timeseries.key,
        type: 'path',
        value: timeseries.path,
      })),
      rpc_methods: oldMapping.rpc_methods.map(rpcMethod => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map(arg => ({
          value: arg,
          type: this.getArgumentType(arg),
        }))
      }))
    }));
  }

  static mapMappingToDowngradedVersion(mapping: DeviceConnectorMapping[]): LegacyDeviceConnectorMapping[] {
    return mapping?.map((newMapping: DeviceConnectorMapping) => ({
      ...newMapping,
      deviceNamePattern: newMapping.deviceInfo.deviceNameExpression,
      deviceTypePattern: newMapping.deviceInfo.deviceProfileExpression,
      attributes: newMapping.attributes.map((attribute: any) => ({
        key: attribute.key,
        path: attribute.value,
      })),
      attributes_updates: newMapping.attributes_updates.map((attributeUpdate: any) => ({
        attributeOnThingsBoard: attributeUpdate.key,
        attributeOnDevice: attributeUpdate.value,
      })),
      timeseries: newMapping.timeseries.map((timeseries: any) => ({
        key: timeseries.key,
        path: timeseries.value,
      })),
      rpc_methods: newMapping.rpc_methods.map((rpcMethod: any) => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map((arg: any) => arg.value)
      }))
    }));
  }

  private static getArgumentType(arg: unknown): string {
    switch (typeof arg) {
      case 'boolean':
        return 'boolean';
      case 'number':
        return Number.isInteger(arg) ? 'integer' : 'float';
      default:
        return 'string';
    }
  }
}
