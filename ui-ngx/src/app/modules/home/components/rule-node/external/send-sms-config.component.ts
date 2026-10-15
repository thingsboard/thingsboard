// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-send-sms-config',
    templateUrl: './send-sms-config.component.html',
    styleUrls: [],
    standalone: false
})
export class SendSmsConfigComponent extends RuleNodeConfigurationComponent {

  sendSmsConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.sendSmsConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.sendSmsConfigForm = this.fb.group({
      numbersToTemplate: [configuration ? configuration.numbersToTemplate : null, [Validators.required]],
      smsMessageTemplate: [configuration ? configuration.smsMessageTemplate : null, [Validators.required]],
      useSystemSmsSettings: [configuration ? configuration.useSystemSmsSettings : false, []],
      smsProviderConfiguration: [configuration ? configuration.smsProviderConfiguration : null, []],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSmsSettings'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSmsSettings: boolean = this.sendSmsConfigForm.get('useSystemSmsSettings').value;
    if (useSystemSmsSettings) {
      this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([]);
    } else {
      this.sendSmsConfigForm.get('smsProviderConfiguration').setValidators([Validators.required]);
    }
    this.sendSmsConfigForm.get('smsProviderConfiguration').updateValueAndValidity({emitEvent});
  }

}
