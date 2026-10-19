// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

