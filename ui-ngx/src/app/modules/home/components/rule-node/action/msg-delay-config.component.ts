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

@Component({
  selector: 'tb-action-node-msg-delay-config',
  templateUrl: './msg-delay-config.component.html',
  styleUrls: []
})
export class MsgDelayConfigComponent extends RuleNodeConfigurationComponent {

  msgDelayConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.msgDelayConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.msgDelayConfigForm = this.fb.group({
      useMetadataPeriodInSecondsPatterns: [configuration ? configuration.useMetadataPeriodInSecondsPatterns : false, []],
      periodInSeconds: [configuration ? configuration.periodInSeconds : null, []],
      periodInSecondsPattern: [configuration ? configuration.periodInSecondsPattern : null, []],
      maxPendingMsgs: [configuration ? configuration.maxPendingMsgs : null,
        [Validators.required, Validators.min(1), Validators.max(100000)]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useMetadataPeriodInSecondsPatterns'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useMetadataPeriodInSecondsPatterns: boolean = this.msgDelayConfigForm.get('useMetadataPeriodInSecondsPatterns').value;
    if (useMetadataPeriodInSecondsPatterns) {
      this.msgDelayConfigForm.get('periodInSecondsPattern').setValidators([Validators.required]);
      this.msgDelayConfigForm.get('periodInSeconds').setValidators([]);
    } else {
      this.msgDelayConfigForm.get('periodInSecondsPattern').setValidators([]);
      this.msgDelayConfigForm.get('periodInSeconds').setValidators([Validators.required, Validators.min(0)]);
    }
    this.msgDelayConfigForm.get('periodInSecondsPattern').updateValueAndValidity({emitEvent});
    this.msgDelayConfigForm.get('periodInSeconds').updateValueAndValidity({emitEvent});
  }

}
