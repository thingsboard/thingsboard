// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-external-node-send-email-config',
    templateUrl: './send-email-config.component.html',
    styleUrls: [],
    standalone: false
})
export class SendEmailConfigComponent extends RuleNodeConfigurationComponent {

  sendEmailConfigForm: UntypedFormGroup;

  smtpProtocols: string[] = [
    'smtp',
    'smtps'
  ];

  tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.sendEmailConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.sendEmailConfigForm = this.fb.group({
      useSystemSmtpSettings: [configuration ? configuration.useSystemSmtpSettings : false, []],
      smtpProtocol: [configuration ? configuration.smtpProtocol : null, []],
      smtpHost: [configuration ? configuration.smtpHost : null, []],
      smtpPort: [configuration ? configuration.smtpPort : null, []],
      timeout: [configuration ? configuration.timeout : null, []],
      enableTls: [configuration ? configuration.enableTls : false, []],
      tlsVersion: [configuration ? configuration.tlsVersion : null, []],
      enableProxy: [configuration ? configuration.enableProxy : false, []],
      proxyHost: [configuration ? configuration.proxyHost : null, []],
      proxyPort: [configuration ? configuration.proxyPort : null, []],
      proxyUser: [configuration ? configuration.proxyUser :null, []],
      proxyPassword: [configuration ? configuration.proxyPassword :null, []],
      username: [configuration ? configuration.username : null, []],
      password: [configuration ? configuration.password : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useSystemSmtpSettings', 'enableProxy'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useSystemSmtpSettings: boolean = this.sendEmailConfigForm.get('useSystemSmtpSettings').value;
    const enableProxy: boolean = this.sendEmailConfigForm.get('enableProxy').value;
    if (useSystemSmtpSettings) {
      this.sendEmailConfigForm.get('smtpProtocol').setValidators([]);
      this.sendEmailConfigForm.get('smtpHost').setValidators([]);
      this.sendEmailConfigForm.get('smtpPort').setValidators([]);
      this.sendEmailConfigForm.get('timeout').setValidators([]);
      this.sendEmailConfigForm.get('proxyHost').setValidators([]);
      this.sendEmailConfigForm.get('proxyPort').setValidators([]);
    } else {
      this.sendEmailConfigForm.get('smtpProtocol').setValidators([Validators.required]);
      this.sendEmailConfigForm.get('smtpHost').setValidators([Validators.required]);
      this.sendEmailConfigForm.get('smtpPort').setValidators([Validators.required, Validators.min(1), Validators.max(65535)]);
      this.sendEmailConfigForm.get('timeout').setValidators([Validators.required, Validators.min(0)]);
      this.sendEmailConfigForm.get('proxyHost').setValidators(enableProxy ? [Validators.required] : []);
      this.sendEmailConfigForm.get('proxyPort').setValidators(enableProxy ?
        [Validators.required, Validators.min(1), Validators.max(65535)] : []);
    }
    this.sendEmailConfigForm.get('smtpProtocol').updateValueAndValidity({emitEvent});
    this.sendEmailConfigForm.get('smtpHost').updateValueAndValidity({emitEvent});
    this.sendEmailConfigForm.get('smtpPort').updateValueAndValidity({emitEvent});
    this.sendEmailConfigForm.get('timeout').updateValueAndValidity({emitEvent});
    this.sendEmailConfigForm.get('proxyHost').updateValueAndValidity({emitEvent});
    this.sendEmailConfigForm.get('proxyPort').updateValueAndValidity({emitEvent});
  }

}
