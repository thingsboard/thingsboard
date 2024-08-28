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

import { deleteNullProperties, isEqual } from '@core/utils';
import {
  AttributeRequest,
  ConnectorDeviceInfo,
  Converter,
  ConverterConnectorMapping,
  ConvertorType,
  CurrentGatewayConnector,
  GatewayConnector,
  LegacyConverter,
  LegacyConverterConnectorMapping,
  LegacyGatewayConnector,
  LegacyRequestMappingData,
  MQTTBasicConfig,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestType,
  ServerSideRpc,
  ServerSideRpcType,
  SourceType
} from '@home/components/widget/lib/gateway/gateway-widget.models';

export class MqttVersionMappingUtil {

  static readonly mqttRequestTypeKeys = Object.values(RequestType);
  static readonly mqttRequestMappingOldFields =
    ['attributeNameJsonExpression', 'deviceNameJsonExpression', 'deviceNameTopicExpression', 'extension-config'];
  static readonly mqttRequestMappingNewFields =
    ['attributeNameExpressionSource', 'responseTopicQoS', 'extensionConfig'];

  static getNewestVersion(mqttConnector: GatewayConnector): CurrentGatewayConnector {
    const {
      connectRequests,
      disconnectRequests,
      attributeRequests,
      attributeUpdates,
      serverSideRpc
    } = mqttConnector.configurationJson as MQTTLegacyBasicConfig;
    let configurationJson = {
      ...mqttConnector.configurationJson,
      requestsMapping: this.mapRequestsToNewestVersion({
          connectRequests,
          disconnectRequests,
          attributeRequests,
          attributeUpdates,
          serverSideRpc
        }),
      mapping: this.mapMappingToNewestVersion((mqttConnector.configurationJson as MQTTLegacyBasicConfig).mapping),
    };

    this.mqttRequestTypeKeys.forEach((key: RequestType) => {
      const { [key]: removedKey, ...rest } = configurationJson as MQTTLegacyBasicConfig;
      configurationJson = { ...rest } as any;
    });

    this.cleanUpConfigJson(configurationJson as MQTTBasicConfig);

    return {
      ...mqttConnector,
      configurationJson
    } as CurrentGatewayConnector;
  }

  static getLegacyVersion(mqttConnector: GatewayConnector): LegacyGatewayConnector {
    const { requestsMapping, mapping, ...restConfig } = mqttConnector.configurationJson as MQTTBasicConfig;

    const updatedRequestsMapping = this.mapRequestsToLegacyVersion(requestsMapping as Record<RequestType, RequestMappingData[]>);
    const updatedMapping = this.mapMappingToLegacyVersion(mapping);

    return {
      ...mqttConnector,
      configurationJson: {
        ...restConfig,
        ...updatedRequestsMapping,
        mapping: updatedMapping,
      },
    } as LegacyGatewayConnector;
  }

  static mapMappingToNewestVersion(mapping: LegacyConverterConnectorMapping[] | ConverterConnectorMapping[]): ConverterConnectorMapping[] {
    return mapping?.map(({ converter, topicFilter, subscriptionQos = 1 }) => {
      const deviceInfo = converter.deviceInfo ?? this.extractConverterDeviceInfo(converter);

      const newConverter = {
        ...converter,
        deviceInfo,
        extensionConfig: converter.extensionConfig || converter['extension-config'] || null
      };

      this.cleanUpOldFields(newConverter);

      return { converter: newConverter, topicFilter, subscriptionQos };
    });
  }

  static mapRequestsToNewestVersion(
    requestMapping: Record<RequestType,
      RequestMappingData[] | LegacyRequestMappingData[]>
  ): Record<RequestType, RequestMappingData[]> {
    return this.mqttRequestTypeKeys.reduce((acc, key: RequestType) => {
      if (!requestMapping[key]) {
        return acc;
      }

      acc[key] = requestMapping[key].map(value => {
        const newValue = this.mapRequestToNewest(value as LegacyRequestMappingData, key);

        this.cleanUpOldFields(newValue as {});

        return newValue;
      });

      return acc;
    }, {}) as Record<RequestType, RequestMappingData[]>;
  }

  static mapRequestsToLegacyVersion(
    requestsMapping: Record<RequestType, RequestMappingData[]>
  ): Record<RequestType, LegacyRequestMappingData[]> {
    return this.mqttRequestTypeKeys.reduce((acc, key) => {
      if (!requestsMapping[key]) {
        return acc;
      }

      acc[key] = requestsMapping[key].map((value: RequestMappingData) => {
        if (key === RequestType.SERVER_SIDE_RPC) {
          delete (value as ServerSideRpc).type;
        }

        const { attributeNameExpression, deviceInfo, ...rest } = value as AttributeRequest;

        const newValue = {
          ...rest,
          attributeNameJsonExpression: attributeNameExpression || null,
          deviceNameJsonExpression: deviceInfo?.deviceNameExpressionSource !== SourceType.TOPIC ? deviceInfo?.deviceNameExpression : null,
          deviceNameTopicExpression: deviceInfo?.deviceNameExpressionSource === SourceType.TOPIC ? deviceInfo?.deviceNameExpression : null,
        };

        this.cleanUpNewFields(newValue);

        return newValue;
      });

      return acc;
    }, {}) as Record<RequestType, LegacyRequestMappingData[]>;
  }

  static mapMappingToLegacyVersion(
    mapping: ConverterConnectorMapping[]
  ): LegacyConverterConnectorMapping[] {
    return mapping?.map((converterMapping: ConverterConnectorMapping) => {
      const converter = this.mapConverterToLegacy(converterMapping.converter);

      this.cleanUpNewFields(converter as {});

      return { converter, topicFilter: converterMapping.topicFilter };
    });
  }

  private static mapConverterToLegacy(converter: Converter): LegacyConverter {
    const { deviceInfo, ...restConverter } = converter;

    return converter.type !== ConvertorType.BYTES ? {
      ...restConverter,
      deviceNameJsonExpression: deviceInfo?.deviceNameExpressionSource === SourceType.MSG ? deviceInfo.deviceNameExpression : null,
      deviceTypeJsonExpression:
        deviceInfo?.deviceProfileExpressionSource === SourceType.MSG ? deviceInfo.deviceProfileExpression : null,
      deviceNameTopicExpression:
        deviceInfo?.deviceNameExpressionSource !== SourceType.MSG
          ? deviceInfo?.deviceNameExpression
          : null,
      deviceTypeTopicExpression: deviceInfo?.deviceProfileExpressionSource !== SourceType.MSG
        ? deviceInfo?.deviceProfileExpression
        : null,
    } : {
      ...restConverter,
      deviceNameExpression: deviceInfo.deviceNameExpression,
      deviceTypeExpression: deviceInfo.deviceProfileExpression,
      ['extension-config']: converter.extensionConfig,
    };
  }

  private static cleanUpOldFields(obj: Record<string, unknown>): void {
    this.mqttRequestMappingOldFields.forEach(field => delete obj[field]);
    deleteNullProperties(obj);
  }

  private static cleanUpNewFields(obj: Record<string, unknown>): void {
    this.mqttRequestMappingNewFields.forEach(field => delete obj[field]);
    deleteNullProperties(obj);
  }

  private static getTypeSourceByValue(value: string): SourceType {
    if (value.includes('${')) {
      return SourceType.MSG;
    }
    if (value.includes(`/`)) {
      return SourceType.TOPIC;
    }
    return SourceType.CONST;
  }

  private static cleanUpConfigJson(configurationJson: MQTTBasicConfig): void {
    if (isEqual(configurationJson.requestsMapping, {})) {
      delete configurationJson.requestsMapping;
    }

    if (isEqual(configurationJson.mapping, [])) {
      delete configurationJson.mapping;
    }
  }

  private static extractConverterDeviceInfo(converter: LegacyConverter): ConnectorDeviceInfo {
    const deviceNameExpression = converter.deviceNameExpression
      || converter.deviceNameJsonExpression
      || converter.deviceNameTopicExpression
      || null;
    const deviceNameExpressionSource = converter.deviceNameExpressionSource
      ? converter.deviceNameExpressionSource as SourceType
      : deviceNameExpression ? this.getTypeSourceByValue(deviceNameExpression) : null;
    const deviceProfileExpression = converter.deviceProfileExpression
      || converter.deviceTypeTopicExpression
      || converter.deviceTypeJsonExpression
      || 'default';
    const deviceProfileExpressionSource = converter.deviceProfileExpressionSource
      ? converter.deviceProfileExpressionSource as SourceType
      : deviceProfileExpression ? this.getTypeSourceByValue(deviceProfileExpression) : null;

    return deviceNameExpression || deviceProfileExpression ? {
      deviceNameExpression,
      deviceNameExpressionSource,
      deviceProfileExpression,
      deviceProfileExpressionSource
    } : null;
  }

  private static mapRequestToNewest(value, key: RequestType): RequestMappingData {
    const deviceNameExpression = value.deviceNameJsonExpression || value.deviceNameTopicExpression || null;
    const deviceProfileExpression = value.deviceTypeTopicExpression || value.deviceTypeJsonExpression || 'default';
    const deviceProfileExpressionSource = deviceProfileExpression ? this.getTypeSourceByValue(deviceProfileExpression) : null;
    const attributeNameExpression = value.attributeNameExpressionSource || value.attributeNameJsonExpression || null;
    const responseTopicQoS = key === RequestType.SERVER_SIDE_RPC ? 1 : null;
    const type = key === RequestType.SERVER_SIDE_RPC
      ? (value as ServerSideRpc).responseTopicExpression
        ? ServerSideRpcType.WithResponse
        : ServerSideRpcType.WithoutResponse
      : null;

    return {
      ...value,
      attributeNameExpression,
      attributeNameExpressionSource: attributeNameExpression ? this.getTypeSourceByValue(attributeNameExpression) : null,
      deviceInfo: value.deviceInfo ? value.deviceInfo : deviceNameExpression ? {
        deviceNameExpression,
        deviceNameExpressionSource: this.getTypeSourceByValue(deviceNameExpression),
        deviceProfileExpression,
        deviceProfileExpressionSource
      } : null,
      responseTopicQoS,
      type
    };
  }
}
