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

import { Component, forwardRef, ChangeDetectionStrategy } from '@angular/core';
import { NG_VALUE_ACCESSOR, NG_VALIDATORS } from '@angular/forms';
import {
  BrokerConfig,
  MQTTBasicConfig_v3_5_2,
  MQTTLegacyBasicConfig,
  RequestMappingData,
  RequestMappingValue,
  RequestType, WorkersConfig
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import { MqttVersionMappingUtil } from '@home/components/widget/lib/gateway/utils/mqtt-version-mapping.util';
import {
  MqttBasicConfigDirective
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/basic-config/mqtt-basic-config.abstract';
import { isDefinedAndNotNull } from '@core/utils';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  SecurityConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/security-config/security-config.component';
import {
  WorkersConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/workers-config-control/workers-config-control.component';
import {
  BrokerConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/broker-config-control/broker-config-control.component';
import {
  MappingTableComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mapping-table/mapping-table.component';

@Component({
  selector: 'tb-mqtt-legacy-basic-config',
  templateUrl: './mqtt-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttLegacyBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttLegacyBasicConfigComponent),
      multi: true
    }
  ],
  styleUrls: ['./mqtt-basic-config.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    SharedModule,
    SecurityConfigComponent,
    WorkersConfigControlComponent,
    BrokerConfigControlComponent,
    MappingTableComponent,
  ],
})
export class MqttLegacyBasicConfigComponent extends MqttBasicConfigDirective<MQTTLegacyBasicConfig> {

  protected override mapConfigToFormValue(config: MQTTLegacyBasicConfig): MQTTBasicConfig_v3_5_2 {
    const {
      broker,
      mapping = [],
      connectRequests = [],
      disconnectRequests = [],
      attributeRequests = [],
      attributeUpdates = [],
      serverSideRpc = []
    } = config as MQTTLegacyBasicConfig;
    const updatedRequestMapping = MqttVersionMappingUtil.mapRequestsToUpgradedVersion({
      connectRequests,
      disconnectRequests,
      attributeRequests,
      attributeUpdates,
      serverSideRpc
    });
    return {
      workers: broker && (broker.maxNumberOfWorkers || broker.maxMessageNumberPerWorker) ? {
        maxNumberOfWorkers: broker.maxNumberOfWorkers,
        maxMessageNumberPerWorker: broker.maxMessageNumberPerWorker,
      } : {} as WorkersConfig,
      mapping: MqttVersionMappingUtil.mapMappingToUpgradedVersion(mapping) || [],
      broker: broker || {} as BrokerConfig,
      requestsMapping: this.getRequestDataArray(updatedRequestMapping),
    };
  }

  protected override getMappedValue(basicConfig: MQTTBasicConfig_v3_5_2): MQTTLegacyBasicConfig {
    const { broker, workers, mapping, requestsMapping  } = basicConfig || {};

    const updatedRequestMapping = (requestsMapping as RequestMappingData[])?.length
      ? this.getRequestDataObject(requestsMapping as RequestMappingValue[])
      : {} as Record<RequestType, RequestMappingData[]>;

    return {
      broker: this.getBrokerMappedValue(broker, workers),
      mapping: MqttVersionMappingUtil.mapMappingToDowngradedVersion(mapping),
      ...(MqttVersionMappingUtil.mapRequestsToDowngradedVersion(updatedRequestMapping as Record<RequestType, RequestMappingData[]>))
    };
  }
}
