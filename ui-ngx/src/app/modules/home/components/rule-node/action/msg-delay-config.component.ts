// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-msg-delay-config',
    templateUrl: './msg-delay-config.component.html',
    styleUrls: [],
    standalone: false
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
