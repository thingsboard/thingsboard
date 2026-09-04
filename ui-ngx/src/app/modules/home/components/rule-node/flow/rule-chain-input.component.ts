// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
    selector: 'tb-flow-node-rule-chain-input-config',
    templateUrl: './rule-chain-input.component.html',
    styleUrls: [],
    standalone: false
})
export class RuleChainInputComponent extends RuleNodeConfigurationComponent {

  entityType = EntityType;

  ruleChainInputConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.ruleChainInputConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.ruleChainInputConfigForm = this.fb.group({
      forwardMsgToDefaultRuleChain: [configuration ? configuration?.forwardMsgToDefaultRuleChain : false, []],
      ruleChainId: [configuration ? configuration.ruleChainId : null, [Validators.required]]
    });
  }

}
