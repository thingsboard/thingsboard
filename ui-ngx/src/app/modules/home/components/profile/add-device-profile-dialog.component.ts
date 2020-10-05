///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import {
  AfterViewInit,
  Component,
  ComponentFactoryResolver,
  Inject,
  Injector,
  SkipSelf,
  ViewChild
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileType,
  deviceProfileTypeTranslationMap,
  DeviceTransportType,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { EntityType } from '@shared/models/entity-type.models';
import { MatHorizontalStepper } from '@angular/material/stepper';
import { RuleChainId } from '@shared/models/id/rule-chain-id';

export interface AddDeviceProfileDialogData {
  deviceProfileName: string;
}

@Component({
  selector: 'tb-add-device-profile-dialog',
  templateUrl: './add-device-profile-dialog.component.html',
  providers: [],
  styleUrls: ['./add-device-profile-dialog.component.scss']
})
export class AddDeviceProfileDialogComponent extends
  DialogComponent<AddDeviceProfileDialogComponent, DeviceProfile> implements AfterViewInit {

  @ViewChild('addDeviceProfileStepper', {static: true}) addDeviceProfileStepper: MatHorizontalStepper;

  selectedIndex = 0;

  entityType = EntityType;

  deviceProfileTypes = Object.keys(DeviceProfileType);

  deviceProfileTypeTranslations = deviceProfileTypeTranslationMap;

  deviceTransportTypes = Object.keys(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceProfileDetailsFormGroup: FormGroup;

  transportConfigFormGroup: FormGroup;

  alarmRulesFormGroup: FormGroup;

  provisionConfigurationFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddDeviceProfileDialogData,
              public dialogRef: MatDialogRef<AddDeviceProfileDialogComponent, DeviceProfile>,
              private componentFactoryResolver: ComponentFactoryResolver,
              private injector: Injector,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private deviceProfileService: DeviceProfileService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.deviceProfileDetailsFormGroup = this.fb.group(
      {
        name: [data.deviceProfileName, [Validators.required]],
        type: [DeviceProfileType.DEFAULT, [Validators.required]],
        defaultRuleChainId: [null, []],
        description: ['', []]
      }
    );
    this.transportConfigFormGroup = this.fb.group(
      {
        transportType: [DeviceTransportType.DEFAULT, [Validators.required]],
        transportConfiguration: [createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT),
          [Validators.required]]
      }
    );
    this.transportConfigFormGroup.get('transportType').valueChanges.subscribe(() => {
      this.deviceProfileTransportTypeChanged();
    });

    this.alarmRulesFormGroup = this.fb.group(
      {
        alarms: [null]
      }
    );

    this.provisionConfigurationFormGroup = this.fb.group(
      {
        provisionConfiguration: [null]
      }
    )
  }

  private deviceProfileTransportTypeChanged() {
    const deviceTransportType: DeviceTransportType = this.transportConfigFormGroup.get('transportType').value;
    this.transportConfigFormGroup.patchValue(
      {transportConfiguration: createDeviceProfileTransportConfiguration(deviceTransportType)});
  }

  ngAfterViewInit(): void {
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

  selectedForm(): FormGroup {
    switch (this.selectedIndex) {
      case 0:
        return this.deviceProfileDetailsFormGroup;
      case 1:
        return this.transportConfigFormGroup;
      case 2:
        return this.alarmRulesFormGroup;
      case 3:
        return this.provisionConfigurationFormGroup;
    }
  }

  private add(): void {
    const deviceProfile: DeviceProfile = {
      name: this.deviceProfileDetailsFormGroup.get('name').value,
      type: this.deviceProfileDetailsFormGroup.get('type').value,
      transportType: this.transportConfigFormGroup.get('transportType').value,
      provisionType: this.provisionConfigurationFormGroup.get('provisionConfiguration').value.type,
      provisionDeviceKey: this.provisionConfigurationFormGroup.get('provisionConfiguration').value.provisionDeviceKey,
      description: this.deviceProfileDetailsFormGroup.get('description').value,
      profileData: {
        configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
        transportConfiguration: this.transportConfigFormGroup.get('transportConfiguration').value,
        alarms: this.alarmRulesFormGroup.get('alarms').value,
        provisionConfiguration: {
          type: this.provisionConfigurationFormGroup.get('provisionConfiguration').value.type,
          provisionDeviceSecret: this.provisionConfigurationFormGroup.get('provisionConfiguration').value.provisionDeviceSecret
        }
      }
    };
    if (this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value) {
      deviceProfile.defaultRuleChainId = new RuleChainId(this.deviceProfileDetailsFormGroup.get('defaultRuleChainId').value);
    }
    this.deviceProfileService.saveDeviceProfile(deviceProfile).subscribe(
      (savedDeviceProfile) => {
        this.dialogRef.close(savedDeviceProfile);
      }
    );
  }
}
