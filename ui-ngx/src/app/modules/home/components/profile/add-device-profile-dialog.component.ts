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

import { Component, Inject, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileType,
  deviceProfileTypeTranslationMap,
  DeviceProvisionConfiguration,
  DeviceProvisionType,
  DeviceTransportType,
  deviceTransportTypeHintMap,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { EntityType } from '@shared/models/entity-type.models';
import { MatStepper, StepperOrientation } from '@angular/material/stepper';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { deepTrim } from '@core/utils';
import { ServiceType } from '@shared/models/queue.models';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { RuleChainType } from '@shared/models/rule-chain.models';
import { Observable } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MediaBreakpoints } from '@shared/models/constants';
import { map } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

export interface AddDeviceProfileDialogData {
  deviceProfileName: string;
  transportType: DeviceTransportType;
}

@Component({
  selector: 'tb-add-device-profile-dialog',
  templateUrl: './add-device-profile-dialog.component.html',
  providers: [],
  styleUrls: ['./add-device-profile-dialog.component.scss']
})
export class AddDeviceProfileDialogComponent extends
  DialogComponent<AddDeviceProfileDialogComponent, DeviceProfile> {

  @ViewChild('addDeviceProfileStepper', {static: true}) addDeviceProfileStepper: MatStepper;
  stepperOrientation: Observable<StepperOrientation>;
  stepperLabelPosition: Observable<'bottom' | 'end'>;
  selectedIndex = 0;

  showNext = true;

  entityType = EntityType;

  deviceProfileTypes = Object.values(DeviceProfileType);

  deviceProfileTypeTranslations = deviceProfileTypeTranslationMap;

  deviceTransportTypeHints = deviceTransportTypeHintMap;

  deviceTransportTypes = Object.values(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceProfileDetailsFormGroup: UntypedFormGroup;

  transportConfigFormGroup: UntypedFormGroup;

  alarmRulesFormGroup: UntypedFormGroup;

  provisionConfigFormGroup: UntypedFormGroup;

  serviceType = ServiceType.TB_RULE_ENGINE;

  edgeRuleChainType = RuleChainType.EDGE;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddDeviceProfileDialogData,
              public dialogRef: MatDialogRef<AddDeviceProfileDialogComponent, DeviceProfile>,
              private breakpointObserver: BreakpointObserver,
              private deviceProfileService: DeviceProfileService,
              private fb: UntypedFormBuilder) {
    super(store, router, dialogRef);
    this.stepperOrientation = this.breakpointObserver.observe(MediaBreakpoints['gt-sm'])
      .pipe(map(({matches}) => matches ? 'horizontal' : 'vertical'));

    this.stepperLabelPosition = this.breakpointObserver.observe(MediaBreakpoints['gt-md'])
      .pipe(map(({matches}) => matches ? 'end' : 'bottom'));

    this.deviceProfileDetailsFormGroup = this.fb.group(
      {
        name: [data.deviceProfileName, [Validators.required, Validators.maxLength(255)]],
        type: [DeviceProfileType.DEFAULT, [Validators.required]],
        image: [null, []],
        defaultRuleChainId: [null, []],
        defaultDashboardId: [null, []],
        defaultQueueName: [null, []],
        defaultEdgeRuleChainId: [null, []],
        description: ['', []]
      }
    );
    this.transportConfigFormGroup = this.fb.group(
      {
        transportType: [data.transportType ? data.transportType : DeviceTransportType.DEFAULT, [Validators.required]],
        transportConfiguration: [createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT),
          [Validators.required]]
      }
    );
    this.transportConfigFormGroup.get('transportType').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(() => {
      this.deviceProfileTransportTypeChanged();
    });

    this.alarmRulesFormGroup = this.fb.group(
      {
        alarms: [null]
      }
    );

    this.provisionConfigFormGroup = this.fb.group(
      {
        provisionConfiguration: [{
          type: DeviceProvisionType.DISABLED
        } as DeviceProvisionConfiguration, [Validators.required]]
      }
    );
  }

  private deviceProfileTransportTypeChanged() {
    const deviceTransportType: DeviceTransportType = this.transportConfigFormGroup.get('transportType').value;
    this.transportConfigFormGroup.patchValue(
      {transportConfiguration: createDeviceProfileTransportConfiguration(deviceTransportType)});
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep() {
    this.addDeviceProfileStepper.previous();
  }

  nextStep() {
    if (this.selectedIndex < 3) {
      this.addDeviceProfileStepper.next();
    } else {
      this.add();
    }
  }

  selectedForm(): UntypedFormGroup {
    switch (this.selectedIndex) {
      case 0:
        return this.deviceProfileDetailsFormGroup;
      case 1:
        return this.transportConfigFormGroup;
      case 2:
        return this.alarmRulesFormGroup;
      case 3:
        return this.provisionConfigFormGroup;
    }
  }

  add(): void {
    if (this.allValid()) {
      const deviceProvisionConfiguration: DeviceProvisionConfiguration = this.provisionConfigFormGroup.get('provisionConfiguration').value;
      const provisionDeviceKey = deviceProvisionConfiguration.provisionDeviceKey;
      delete deviceProvisionConfiguration.provisionDeviceKey;
      const deviceProfile: DeviceProfile = {
        name: this.deviceProfileDetailsFormGroup.get('name').value,
        type: this.deviceProfileDetailsFormGroup.get('type').value,
        image: this.deviceProfileDetailsFormGroup.get('image').value,
        defaultQueueName: this.deviceProfileDetailsFormGroup.get('defaultQueueName').value,
        transportType: this.transportConfigFormGroup.get('transportType').value,
        provisionType: deviceProvisionConfiguration.type,
        provisionDeviceKey,
        description: this.deviceProfileDetailsFormGroup.get('description').value,
        profileData: {
          configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
          transportConfiguration: this.transportConfigFormGroup.get('transportConfiguration').value,
          alarms: this.alarmRulesFormGroup.get('alarms').value,
          provisionConfiguration: deviceProvisionConfiguration
        }
      };
      if (this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value) {
        deviceProfile.defaultRuleChainId = new RuleChainId(this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value);
      }
      if (this.deviceProfileDetailsFormGroup.get('defaultDashboardId').value) {
        deviceProfile.defaultDashboardId = new DashboardId(this.deviceProfileDetailsFormGroup.get('defaultDashboardId').value);
      }
      if (this.deviceProfileDetailsFormGroup.get('defaultEdgeRuleChainId').value) {
        deviceProfile.defaultEdgeRuleChainId = new RuleChainId(this.deviceProfileDetailsFormGroup.get('defaultEdgeRuleChainId').value);
      }
      this.deviceProfileService.saveDeviceProfile(deepTrim(deviceProfile)).subscribe(
        (savedDeviceProfile) => {
          this.dialogRef.close(savedDeviceProfile);
        }
      );
    }
  }

  getFormLabel(index: number): string {
    switch (index) {
      case 0:
        return 'device-profile.device-profile-details';
      case 1:
        return 'device-profile.transport-configuration';
      case 2:
        return 'device-profile.alarm-rules';
      case 3:
        return 'device-profile.device-provisioning';
    }
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.showNext = false;
    } else {
      this.showNext = true;
    }
  }

  private get maxStepperIndex(): number {
    return this.addDeviceProfileStepper?._steps?.length - 1;
  }

  allValid(): boolean {
    return !this.addDeviceProfileStepper.steps.find((item, index) => {
      if (item.stepControl.invalid) {
        item.interacted = true;
        this.addDeviceProfileStepper.selectedIndex = index;
        return true;
      } else {
        return false;
      }
    });
  }
}
