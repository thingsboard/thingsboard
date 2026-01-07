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
  selector: 'tb-external-node-send-sms-config',
  templateUrl: './send-sms-config.component.html',
  styleUrls: []
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
