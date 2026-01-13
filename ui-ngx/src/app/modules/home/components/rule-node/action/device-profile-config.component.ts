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
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
  selector: 'tb-action-node-device-profile-config',
  templateUrl: './device-profile-config.component.html',
  styleUrls: []
})
export class DeviceProfileConfigComponent extends RuleNodeConfigurationComponent {

  deviceProfile: UntypedFormGroup;

  constructor(private fb: UntypedFormBuilder) {
    super();
  }

  protected configForm(): UntypedFormGroup {
    return this.deviceProfile;
  }

  protected onConfigurationSet(configuration: RuleNodeConfiguration) {
    this.deviceProfile = this.fb.group({
      persistAlarmRulesState: [configuration ? configuration.persistAlarmRulesState : false],
      fetchAlarmRulesStateOnStart: [configuration ? configuration.fetchAlarmRulesStateOnStart : false]
    });
  }

  protected validatorTriggers(): string[] {
    return ['persistAlarmRulesState'];
  }

  protected updateValidators(emitEvent: boolean) {
    if (this.deviceProfile.get('persistAlarmRulesState').value) {
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').enable({emitEvent: false});
    } else {
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').setValue(false, {emitEvent: false});
      this.deviceProfile.get('fetchAlarmRulesStateOnStart').disable({emitEvent: false});
    }
    this.deviceProfile.get('fetchAlarmRulesStateOnStart').updateValueAndValidity({emitEvent});
  }

}
