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

import { Directive } from '@angular/core';
import { FormGroup } from '@angular/forms';
import {
  BrokerConfig,
  MappingType,
  MQTTBasicConfig,
  MQTTBasicConfig_v3_5_2,
  RequestMappingData,
  RequestMappingValue,
  RequestType,
  WorkersConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { isObject } from '@core/utils';
import {
  GatewayConnectorBasicConfigDirective
} from '@home/components/widget/lib/gateway/abstract/gateway-connector-basic-config.abstract';

@Directive()
export abstract class MqttBasicConfigDirective<BasicConfig>
  extends GatewayConnectorBasicConfigDirective<MQTTBasicConfig_v3_5_2, BasicConfig> {

  MappingType = MappingType;

  protected override initBasicFormGroup(): FormGroup {
    return this.fb.group({
      mapping: [],
      requestsMapping: [],
      broker: [],
      workers: [],
    });
  }

  protected getRequestDataArray(value: Record<RequestType, RequestMappingData[]>): RequestMappingData[] {
    const mappingConfigs = [];

    if (isObject(value)) {
      Object.keys(value).forEach((configKey: string) => {
        for (const mapping of value[configKey]) {
          mappingConfigs.push({
            requestType: configKey,
            requestValue: mapping
          });
        }
      });
    }

    return mappingConfigs;
  }

  protected getRequestDataObject(array: RequestMappingValue[]): Record<RequestType, RequestMappingValue[]> {
    return array.reduce((result, { requestType, requestValue }) => {
      result[requestType].push(requestValue);
      return result;
    }, {
      connectRequests: [],
      disconnectRequests: [],
      attributeRequests: [],
      attributeUpdates: [],
      serverSideRpc: [],
    });
  }

  protected getBrokerMappedValue(broker: BrokerConfig, workers: WorkersConfig): BrokerConfig {
    return {
      ...broker,
      maxNumberOfWorkers: workers.maxNumberOfWorkers ?? 100,
      maxMessageNumberPerWorker: workers.maxMessageNumberPerWorker ?? 10,
    };
  }

  writeValue(basicConfig: BasicConfig): void {
    this.basicFormGroup.setValue(this.mapConfigToFormValue(basicConfig), { emitEvent: false });
  }

  protected abstract override mapConfigToFormValue(config: BasicConfig): MQTTBasicConfig_v3_5_2;
  protected abstract override getMappedValue(config: MQTTBasicConfig): BasicConfig;
}
