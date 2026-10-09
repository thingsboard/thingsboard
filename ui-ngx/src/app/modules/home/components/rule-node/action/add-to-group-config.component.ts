// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-add-to-group-config',
    templateUrl: './add-to-group-config.component.html',
    styleUrls: [],
    standalone: false
})
export class AddToGroupConfigComponent extends RuleNodeConfigurationComponent {

  addToGroupConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.addToGroupConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.addToGroupConfigForm = this.fb.group({
      groupNamePattern: [configuration ? configuration.groupNamePattern : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
      createGroupIfNotExists: [configuration ? configuration.createGroupIfNotExists : false, []],
      removeFromCurrentGroups: [configuration ? configuration.removeFromCurrentGroups : false, []],
      groupCacheExpiration: [configuration ? configuration.customerCacheExpiration : null, [Validators.required, Validators.min(0)]]
    });
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.groupNamePattern = configuration.groupNamePattern.trim();
    return configuration;
  }
}
