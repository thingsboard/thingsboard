// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, SlackChanelType, SlackChanelTypesTranslateMap } from '@app/shared/public-api';
import { RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-slack-config',
    templateUrl: './slack-config.component.html',
    styleUrls: ['./slack-config.component.scss'],
    standalone: false
})
export class SlackConfigComponent extends RuleNodeConfigurationComponent {

  slackConfigForm: FormGroup;
  slackChanelTypes = Object.keys(SlackChanelType) as SlackChanelType[];
  slackChanelTypesTranslateMap = SlackChanelTypesTranslateMap;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.slackConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.slackConfigForm = this.fb.group({
      botToken: [configuration ? configuration.botToken : null],
      useSystemSettings: [configuration ? configuration.useSystemSettings : false],
      messageTemplate: [configuration ? configuration.messageTemplate : null, [Validators.required]],
      conversationType: [configuration ? configuration.conversationType : null, [Validators.required]],
      conversation: [configuration ? configuration.conversation : null, [Validators.required]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSettings'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSettings: boolean = this.slackConfigForm.get('useSystemSettings').value;
    if (useSystemSettings) {
      this.slackConfigForm.get('botToken').clearValidators();
    } else {
      this.slackConfigForm.get('botToken').setValidators([Validators.required]);
    }
    this.slackConfigForm.get('botToken').updateValueAndValidity({emitEvent});
  }
}
