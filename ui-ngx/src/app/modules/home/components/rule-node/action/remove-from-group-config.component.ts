// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-remove-from-group-config',
    templateUrl: './remove-from-group-config.component.html',
    styleUrls: [],
    standalone: false
})
export class RemoveFromGroupConfigComponent extends RuleNodeConfigurationComponent {

  removeFromGroupConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.removeFromGroupConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.removeFromGroupConfigForm = this.fb.group({
      groupNamePattern: [configuration ? configuration.groupNamePattern : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
      groupCacheExpiration: [configuration ? configuration.groupCacheExpiration : null, [Validators.required, Validators.min(0)]]
    });
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.groupNamePattern = configuration.groupNamePattern.trim();
    return configuration;
  }
}
