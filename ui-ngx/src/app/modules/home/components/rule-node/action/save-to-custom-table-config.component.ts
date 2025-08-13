///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-action-node-custom-table-config',
  templateUrl: './save-to-custom-table-config.component.html',
  styleUrls: []
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
