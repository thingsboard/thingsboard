///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import {
  ToByteStandartCharsetTypes,
  ToByteStandartCharsetTypeTranslations
} from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-external-node-kafka-config',
    templateUrl: './kafka-config.component.html',
    styleUrls: [],
    standalone: false
})
export class KafkaConfigComponent extends RuleNodeConfigurationComponent {

  kafkaConfigForm: UntypedFormGroup;

  ackValues: string[] = ['all', '-1', '0', '1'];

  ToByteStandartCharsetTypesValues = ToByteStandartCharsetTypes;
  ToByteStandartCharsetTypeTranslationMap = ToByteStandartCharsetTypeTranslations;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.kafkaConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.kafkaConfigForm = this.fb.group({
      topicPattern: [configuration ? configuration.topicPattern : null, [Validators.required]],
      keyPattern: [configuration ? configuration.keyPattern : null],
      bootstrapServers: [configuration ? configuration.bootstrapServers : null, [Validators.required]],
      retries: [configuration ? configuration.retries : null, [Validators.min(0)]],
      batchSize: [configuration ? configuration.batchSize : null, [Validators.min(0)]],
      linger: [configuration ? configuration.linger : null, [Validators.min(0)]],
      bufferMemory: [configuration ? configuration.bufferMemory : null, [Validators.min(0)]],
      acks: [configuration ? configuration.acks : null, [Validators.required]],
      otherProperties: [configuration ? configuration.otherProperties : null, []],
      addMetadataKeyValuesAsKafkaHeaders: [configuration ? configuration.addMetadataKeyValuesAsKafkaHeaders : false, []],
      kafkaHeadersCharset: [configuration ? configuration.kafkaHeadersCharset : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['addMetadataKeyValuesAsKafkaHeaders'];
  }

  protected updateValidators(emitEvent: boolean) {
    const addMetadataKeyValuesAsKafkaHeaders: boolean = this.kafkaConfigForm.get('addMetadataKeyValuesAsKafkaHeaders').value;
    if (addMetadataKeyValuesAsKafkaHeaders) {
      this.kafkaConfigForm.get('kafkaHeadersCharset').setValidators([Validators.required]);
    } else {
      this.kafkaConfigForm.get('kafkaHeadersCharset').setValidators([]);
    }
    this.kafkaConfigForm.get('kafkaHeadersCharset').updateValueAndValidity({emitEvent});
  }

}
