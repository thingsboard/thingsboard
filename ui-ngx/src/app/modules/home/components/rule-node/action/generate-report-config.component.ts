// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-generate-report-config',
    templateUrl: './generate-report-config.component.html',
    styleUrls: [],
    standalone: false
})
export class GenerateReportConfigComponent extends RuleNodeConfigurationComponent {

  generateReportConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.generateReportConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.generateReportConfigForm = this.fb.group({
      useConfigFromMessage: [configuration ? configuration.useConfigFromMessage : false, []],
      config: [configuration ? configuration.config : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useConfigFromMessage'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useConfigFromMessage: boolean = this.generateReportConfigForm.get('useConfigFromMessage').value;
    if (useConfigFromMessage) {
      this.generateReportConfigForm.get('config').setValidators([]);
    } else {
      this.generateReportConfigForm.get('config').setValidators([Validators.required]);
    }
    this.generateReportConfigForm.get('config').updateValueAndValidity({emitEvent});
  }

}
