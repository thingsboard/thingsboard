// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-custom-table-config',
    templateUrl: './save-to-custom-table-config.component.html',
    styleUrls: [],
    standalone: false
})
export class SaveToCustomTableConfigComponent extends RuleNodeConfigurationComponent {

  saveToCustomTableConfigForm: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.saveToCustomTableConfigForm;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.saveToCustomTableConfigForm = this.fb.group({
      tableName: [configuration ? configuration.tableName : null, [Validators.required, Validators.pattern(/.*\S.*/)]],
      fieldsMapping: [configuration ? configuration.fieldsMapping : null, [Validators.required]],
      defaultTtl: [configuration ? configuration.defaultTtl : 0, [Validators.required, Validators.min(0)]]
    });
  }

  protected prepareOutputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    configuration.tableName = configuration.tableName.trim();
    return configuration;
  }
}
