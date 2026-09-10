// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-transformation-node-to-email-config',
    templateUrl: './to-email-config.component.html',
    styleUrls: ['./to-email-config.component.scss'],
    standalone: false
})
export class ToEmailConfigComponent extends RuleNodeConfigurationComponent {

  toEmailConfigForm: FormGroup;
  mailBodyTypes = [
    {
      name: 'rule-node-config.mail-body-types.plain-text',
      description: 'rule-node-config.mail-body-types.plain-text-description',
      value: 'false',
    },
    {
      name: 'rule-node-config.mail-body-types.html',
      description: 'rule-node-config.mail-body-types.html-text-description',
      value: 'true',
    },
    {
      name: 'rule-node-config.mail-body-types.use-body-type-template',
      description: 'rule-node-config.mail-body-types.dynamic-text-description',
      value: 'dynamic',
    }
  ];

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.toEmailConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.toEmailConfigForm = this.fb.group({
      fromTemplate: [configuration ? configuration.fromTemplate : null, [Validators.required]],
      toTemplate: [configuration ? configuration.toTemplate : null, [Validators.required]],
      ccTemplate: [configuration ? configuration.ccTemplate : null, []],
      bccTemplate: [configuration ? configuration.bccTemplate : null, []],
      subjectTemplate: [configuration ? configuration.subjectTemplate : null, [Validators.required]],
      mailBodyType: [configuration ? configuration.mailBodyType : null],
      isHtmlTemplate: [configuration ? configuration.isHtmlTemplate : null, [Validators.required]],
      bodyTemplate: [configuration ? configuration.bodyTemplate : null, [Validators.required]],
    });
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      fromTemplate: isDefinedAndNotNull(configuration?.fromTemplate) ? configuration.fromTemplate : null,
      toTemplate: isDefinedAndNotNull(configuration?.toTemplate) ? configuration.toTemplate : null,
      ccTemplate: isDefinedAndNotNull(configuration?.ccTemplate) ? configuration.ccTemplate : null,
      bccTemplate: isDefinedAndNotNull(configuration?.bccTemplate) ? configuration.bccTemplate : null,
      subjectTemplate: isDefinedAndNotNull(configuration?.subjectTemplate) ? configuration.subjectTemplate : null,
      mailBodyType: isDefinedAndNotNull(configuration?.mailBodyType) ? configuration.mailBodyType : null,
      isHtmlTemplate: isDefinedAndNotNull(configuration?.isHtmlTemplate) ? configuration.isHtmlTemplate : null,
      bodyTemplate: isDefinedAndNotNull(configuration?.bodyTemplate) ? configuration.bodyTemplate : null,
    };
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.toEmailConfigForm.get('mailBodyType').value === 'dynamic') {
      this.toEmailConfigForm.get('isHtmlTemplate').enable({emitEvent: false});
    } else {
      this.toEmailConfigForm.get('isHtmlTemplate').disable({emitEvent: false});
    }
    this.toEmailConfigForm.get('isHtmlTemplate').updateValueAndValidity({emitEvent});
  }

  protected validatorTriggers(): string[] {
    return ['mailBodyType'];
  }

  getBodyTypeName(): string {
    return this.mailBodyTypes.find(type => type.value === this.toEmailConfigForm.get('mailBodyType').value).name;
  }
}
