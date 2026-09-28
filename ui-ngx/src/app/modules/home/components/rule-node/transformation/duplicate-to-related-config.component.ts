// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-transformation-node-duplicate-to-related-config',
    templateUrl: './duplicate-to-related-config.component.html',
    styleUrls: [],
    standalone: false
})
export class DuplicateToRelatedConfigComponent extends RuleNodeConfigurationComponent {

  duplicateToRelatedConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.duplicateToRelatedConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.duplicateToRelatedConfigForm = this.fb.group({
      relationsQuery: [configuration ? configuration.relationsQuery : null, [Validators.required]]
    });
  }

}
