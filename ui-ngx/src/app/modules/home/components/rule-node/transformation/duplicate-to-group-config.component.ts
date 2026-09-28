// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';
import { allowedEntityGroupTypes } from '@home/components/rule-node/rule-node-config.models';

@Component({
    selector: 'tb-transformation-node-duplicate-to-group-config',
    templateUrl: './duplicate-to-group-config.component.html',
    styleUrls: ['./duplicate-to-group-config.component.scss'],
    standalone: false
})

export class DuplicateToGroupConfigComponent extends RuleNodeConfigurationComponent {

  isTypeSelected = false;
  duplicateToGroupConfigForm: FormGroup;

  entityGroupTypes = allowedEntityGroupTypes;

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.duplicateToGroupConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.duplicateToGroupConfigForm = this.fb.group({
      entityGroupIsMessageOriginator: [configuration ? configuration.entityGroupIsMessageOriginator : false, []],
      entityGroupId: [configuration ? configuration.entityGroupId : null, []]
    });
  }

  protected validatorTriggers(): string[] {
    return ['entityGroupIsMessageOriginator'];
  }

  protected updateValidators(emitEvent: boolean) {
    const entityGroupIsMessageOriginator: boolean = this.duplicateToGroupConfigForm.get('entityGroupIsMessageOriginator').value;
    const entityGroupId: string = this.duplicateToGroupConfigForm.get('entityGroupId').value;
    if (emitEvent) {
      if (entityGroupIsMessageOriginator && entityGroupId) {
        this.duplicateToGroupConfigForm.get('entityGroupId').reset(null, {emitEvent: false});
      }
    }
    if (entityGroupIsMessageOriginator) {
      this.duplicateToGroupConfigForm.get('entityGroupId').setValidators([]);
    } else {
      this.duplicateToGroupConfigForm.get('entityGroupId').setValidators([Validators.required]);
    }
    this.duplicateToGroupConfigForm.get('entityGroupId').updateValueAndValidity({emitEvent});
  }

}
