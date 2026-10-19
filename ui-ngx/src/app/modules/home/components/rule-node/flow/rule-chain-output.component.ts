// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-flow-node-rule-chain-output-config',
    templateUrl: './rule-chain-output.component.html',
    styleUrls: [],
    standalone: false
})
export class RuleChainOutputComponent extends RuleNodeConfigurationComponent {

  ruleChainOutputConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.ruleChainOutputConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.ruleChainOutputConfigForm = this.fb.group({});
  }

}
