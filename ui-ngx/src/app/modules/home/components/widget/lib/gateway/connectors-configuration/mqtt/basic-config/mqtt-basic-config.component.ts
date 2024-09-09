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

import { Component, forwardRef, Input, TemplateRef, ChangeDetectionStrategy } from '@angular/core';
import { NG_VALUE_ACCESSOR, NG_VALIDATORS } from '@angular/forms';
import {
  MQTTBasicConfig_v3_5_2,
  RequestMappingData,
  RequestMappingValue,
  RequestType
} from '@home/components/widget/lib/gateway/gateway-widget.models';
import {
  AbstractMqttBasicConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/basic-config/mqtt-basic-config.abstract';
import { isDefinedAndNotNull } from '@core/utils';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import {
  SecurityConfigComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/security-config/security-config.component';
import {
  WorkersConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/workers-config-control/workers-config-control.component';
import {
  BrokerConfigControlComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mqtt/broker-config-control/broker-config-control.component';
import {
  MappingTableComponent
} from '@home/components/widget/lib/gateway/connectors-configuration/mapping-table/mapping-table.component';

@Component({
  selector: 'tb-mqtt-basic-config',
  templateUrl: './mqtt-basic-config.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MqttBasicConfigComponent),
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
export class MqttBasicConfigComponent extends AbstractMqttBasicConfigComponent<MQTTBasicConfig_v3_5_2> {

  @Input()
  generalTabContent: TemplateRef<any>;

  writeValue(basicConfig: MQTTBasicConfig_v3_5_2): void {
    const { broker, mapping = [], requestsMapping } = basicConfig;
    const editedBase = {
      workers: broker && (broker.maxNumberOfWorkers || broker.maxMessageNumberPerWorker) ? {
        maxNumberOfWorkers: broker.maxNumberOfWorkers,
        maxMessageNumberPerWorker: broker.maxMessageNumberPerWorker,
      } : {},
      mapping: mapping ?? [],
      broker: broker ?? {},
      requestsMapping: this.getRequestDataArray(requestsMapping as Record<RequestType, RequestMappingData[]>),
    };

    this.basicFormGroup.setValue(editedBase, {emitEvent: false});
  }

  protected getMappedMQTTConfig(basicConfig: MQTTBasicConfig_v3_5_2): MQTTBasicConfig_v3_5_2 {
    let { broker, workers, mapping, requestsMapping  } = basicConfig || {};

    if (isDefinedAndNotNull(workers.maxNumberOfWorkers) || isDefinedAndNotNull(workers.maxMessageNumberPerWorker)) {
      broker = {
        ...broker,
        ...workers,
      };
    }

    if ((requestsMapping as RequestMappingData[])?.length) {
      requestsMapping = this.getRequestDataObject(requestsMapping as RequestMappingValue[]);
    }

    return { broker, mapping, requestsMapping };
  }
}
