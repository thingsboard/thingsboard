///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { isDefinedAndNotNull } from '@core/public-api';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@app/shared/models/rule-node.models';

@Component({
    selector: 'tb-filter-node-check-alarm-status-config',
    templateUrl: './check-alarm-status.component.html',
    styleUrls: [],
    standalone: false
})
export class CheckAlarmStatusComponent extends RuleNodeConfigurationComponent {
  alarmStatusConfigForm: FormGroup;

  searchText = '';

  constructor(private fb: FormBuilder) {
    super();
  }

  protected configForm(): FormGroup {
    return this.alarmStatusConfigForm;
  }

  protected prepareInputConfig(configuration: RuleNodeConfiguration): RuleNodeConfiguration {
    return {
      alarmStatusList: isDefinedAndNotNull(configuration?.alarmStatusList) ? configuration.alarmStatusList : null
    };
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.alarmStatusConfigForm = this.fb.group({
      alarmStatusList: [configuration.alarmStatusList, [Validators.required]],
    });
  }
}

