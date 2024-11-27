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
  Attribute,
  AttributesUpdate,
  DeviceConnectorMapping,
  LegacyAttribute,
  LegacyDeviceAttributeUpdate,
  LegacyDeviceConnectorMapping,
  LegacyRpcMethod,
  LegacyServerConfig,
  LegacyTimeseries,
  OPCBasicConfig_v3_5_2,
  OPCUaSourceType,
  RpcArgument,
  RpcMethod,
  ServerConfig,
  Timeseries
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class OpcVersionMappingUtil {

  static mapServerToUpgradedVersion(server: LegacyServerConfig): ServerConfig {
    const { mapping, disableSubscriptions, pollPeriodInMillis, ...restServer } = server;
    return {
      ...restServer,
      pollPeriodInMillis: pollPeriodInMillis ?? 5000,
      enableSubscriptions: !disableSubscriptions,
    };
  }

  static mapServerToDowngradedVersion(config: OPCBasicConfig_v3_5_2): LegacyServerConfig {
    const { mapping, server } = config;
    const { enableSubscriptions, ...restServer } = server ?? {} as ServerConfig;
    return {
      ...restServer,
      mapping: mapping ? this.mapMappingToDowngradedVersion(mapping) : [],
      disableSubscriptions: !enableSubscriptions,
    };
  }

  static mapMappingToUpgradedVersion(mapping: LegacyDeviceConnectorMapping[]): DeviceConnectorMapping[] {
    return mapping.map((legacyMapping: LegacyDeviceConnectorMapping) => ({
      ...legacyMapping,
      deviceNodeSource: this.getDeviceNodeSourceByValue(legacyMapping.deviceNodePattern),
      deviceInfo: {
        deviceNameExpression: legacyMapping.deviceNamePattern,
        deviceNameExpressionSource: this.getTypeSourceByValue(legacyMapping.deviceNamePattern),
        deviceProfileExpression: legacyMapping.deviceTypePattern ?? 'default',
        deviceProfileExpressionSource: this.getTypeSourceByValue(legacyMapping.deviceTypePattern ?? 'default'),
      },
      attributes: legacyMapping.attributes.map((attribute: LegacyAttribute) => ({
        key: attribute.key,
        type: this.getTypeSourceByValue(attribute.path),
        value: attribute.path,
      })),
      attributes_updates: legacyMapping.attributes_updates.map((attributeUpdate: LegacyDeviceAttributeUpdate) => ({
        key: attributeUpdate.attributeOnThingsBoard,
        type: this.getTypeSourceByValue(attributeUpdate.attributeOnDevice),
        value: attributeUpdate.attributeOnDevice,
      })),
      timeseries: legacyMapping.timeseries.map((timeseries: LegacyTimeseries) => ({
        key: timeseries.key,
        type: this.getTypeSourceByValue(timeseries.path),
        value: timeseries.path,
      })),
      rpc_methods: legacyMapping.rpc_methods.map((rpcMethod: LegacyRpcMethod) => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map(arg => ({
          value: arg,
          type: this.getArgumentType(arg),
        } as RpcArgument))
      }))
    }));
  }

  static mapMappingToDowngradedVersion(mapping: DeviceConnectorMapping[]): LegacyDeviceConnectorMapping[] {
    return mapping.map((upgradedMapping: DeviceConnectorMapping) => ({
      ...upgradedMapping,
      deviceNamePattern: upgradedMapping.deviceInfo.deviceNameExpression,
      deviceTypePattern: upgradedMapping.deviceInfo.deviceProfileExpression,
      attributes: upgradedMapping.attributes.map((attribute: Attribute) => ({
        key: attribute.key,
        path: attribute.value,
      })),
      attributes_updates: upgradedMapping.attributes_updates.map((attributeUpdate: AttributesUpdate) => ({
        attributeOnThingsBoard: attributeUpdate.key,
        attributeOnDevice: attributeUpdate.value,
      })),
      timeseries: upgradedMapping.timeseries.map((timeseries: Timeseries) => ({
        key: timeseries.key,
        path: timeseries.value,
      })),
      rpc_methods: upgradedMapping.rpc_methods.map((rpcMethod: RpcMethod) => ({
        method: rpcMethod.method,
        arguments: rpcMethod.arguments.map((arg: RpcArgument) => arg.value)
      }))
    }));
  }

  private static getTypeSourceByValue(value: string): OPCUaSourceType {
    if (value.includes('${')) {
      return OPCUaSourceType.IDENTIFIER;
    }
    if (value.includes(`/`) || value.includes('\\')) {
      return OPCUaSourceType.PATH;
    }
    return OPCUaSourceType.CONST;
  }

  private static getDeviceNodeSourceByValue(value: string): OPCUaSourceType {
    if (value.includes('${')) {
      return OPCUaSourceType.IDENTIFIER;
    } else {
      return OPCUaSourceType.PATH;
    }
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
