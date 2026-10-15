// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { RuleNodeConfiguration, RuleNodeConfigurationComponent } from '@shared/models/rule-node.models';

@Component({
    selector: 'tb-action-node-device-profile-config',
    templateUrl: './device-profile-config.component.html',
    styleUrls: [],
    standalone: false
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
