// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-msg-count-config',
    templateUrl: './msg-count-config.component.html',
    styleUrls: [],
    standalone: false
})
export class MsgCountConfigComponent extends RuleNodeConfigurationComponent {

  msgCountConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.msgCountConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.msgCountConfigForm = this.fb.group({
      interval: [configuration ? configuration.interval : null, [Validators.required, Validators.min(1)]],
      telemetryPrefix: [configuration ? configuration.telemetryPrefix : null, [Validators.required]]
    });
  }

}
