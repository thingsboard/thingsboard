// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-node-empty-config',
    template: '<div></div>',
    styleUrls: [],
    standalone: false
})
export class EmptyConfigComponent extends RuleNodeConfigurationComponent {

  emptyConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.emptyConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.emptyConfigForm = this.fb.group({});
  }

}
