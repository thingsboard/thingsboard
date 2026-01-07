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
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { deduplicationStrategiesTranslations, FetchMode } from '@home/components/rule-node/rule-node-config.models';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-transformation-node-deduplication-config',
  templateUrl: './deduplication-config.component.html',
  styleUrls: []
})

export class DeduplicationConfigComponent extends RuleNodeConfigurationComponent {

  deduplicationConfigForm: FormGroup;
  deduplicationStrategie = FetchMode;
  deduplicationStrategies = Object.keys(this.deduplicationStrategie);
  deduplicationStrategiesTranslations = deduplicationStrategiesTranslations;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.deduplicationConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deduplicationConfigForm = this.fb.group({
      interval: [isDefinedAndNotNull(configuration?.interval) ? configuration.interval : null, [Validators.required,
        Validators.min(1)]],
      strategy: [isDefinedAndNotNull(configuration?.strategy) ? configuration.strategy : null, [Validators.required]],
      outMsgType: [isDefinedAndNotNull(configuration?.outMsgType) ? configuration.outMsgType : null, [Validators.required]],
      maxPendingMsgs: [isDefinedAndNotNull(configuration?.maxPendingMsgs) ? configuration.maxPendingMsgs : null, [Validators.required,
        Validators.min(1), Validators.max(1000)]],
      maxRetries: [isDefinedAndNotNull(configuration?.maxRetries) ? configuration.maxRetries : null,
        [Validators.required, Validators.min(0), Validators.max(100)]]
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    if (!configuration) {
      configuration = {};
    }
    if (!configuration.outMsgType) {
      configuration.outMsgType = 'POST_TELEMETRY_REQUEST';
    }
    return super.prepareInputConfig(configuration);
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.deduplicationConfigForm.get('strategy').value === this.deduplicationStrategie.ALL) {
      this.deduplicationConfigForm.get('outMsgType').enable({emitEvent: false});
    } else {
      this.deduplicationConfigForm.get('outMsgType').disable({emitEvent: false});
    }
    this.deduplicationConfigForm.get('outMsgType').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['strategy'];
  }
}
