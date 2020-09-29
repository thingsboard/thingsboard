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

import { Component, Inject, OnDestroy, SkipSelf, ViewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import {
  createDeviceProfileConfiguration,
  createDeviceProfileTransportConfiguration,
  DeviceProfile,
  DeviceProfileType,
  DeviceTransportType,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { MatHorizontalStepper } from '@angular/material/stepper';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { EntityId } from '@shared/models/id/entity-id';
import { BehaviorSubject, Observable, of, Subscription } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';
import { DeviceService } from '@core/http/device.service';
import { ErrorStateMatcher } from '@angular/material/core';
import { StepperSelectionEvent } from '@angular/cdk/stepper';

@Component({
  selector: 'tb-device-wizard',
  templateUrl: './device-wizard-dialog.component.html',
  providers: [],
  styleUrls: ['./device-wizard-dialog.component.scss']
})
export class DeviceWizardDialogComponent extends
  DialogComponent<DeviceWizardDialogComponent, boolean> implements OnDestroy, ErrorStateMatcher {

  @ViewChild('addDeviceWizardStepper', {static: true}) addDeviceWizardStepper: MatHorizontalStepper;

  selectedIndex = 0;

  nextStepButtonLabel$ = new BehaviorSubject<string>('action.continue');

  createdProfile = false;

  entityType = EntityType;

  deviceTransportTypes = Object.keys(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceWizardFormGroup: FormGroup;

  profileConfigFormGroup: FormGroup;

  transportConfigFormGroup: FormGroup;

  alarmRulesFormGroup: FormGroup;

  specificConfigFormGroup: FormGroup;

  private subscriptions: Subscription[] = [];

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddEntityDialogData<BaseData<EntityId>>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DeviceWizardDialogComponent, boolean>,
              private deviceProfileService: DeviceProfileService,
              private deviceService: DeviceService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.deviceWizardFormGroup = this.fb.group({
        name: ['', Validators.required],
        label: [''],
        gateway: [false],
        transportType: [DeviceTransportType.DEFAULT, Validators.required],
        description: ['']
      }
    );

    this.profileConfigFormGroup = this.fb.group({
        addProfileType: [0],
        deviceProfileId: [null, Validators.required],
        newDeviceProfileTitle: [{value: null, disabled: true}]
      }
    );

    this.subscriptions.push(this.profileConfigFormGroup.get('addProfileType').valueChanges.subscribe(
      (addProfileType: number) => {
        if (addProfileType === 0) {
          this.profileConfigFormGroup.get('deviceProfileId').setValidators([Validators.required]);
          this.profileConfigFormGroup.get('deviceProfileId').enable();
          this.profileConfigFormGroup.get('newDeviceProfileTitle').setValidators(null);
          this.profileConfigFormGroup.get('newDeviceProfileTitle').disable();
          this.profileConfigFormGroup.updateValueAndValidity();
          this.createdProfile = false;
        } else {
          this.profileConfigFormGroup.get('deviceProfileId').setValidators(null);
          this.profileConfigFormGroup.get('deviceProfileId').disable();
          this.profileConfigFormGroup.get('newDeviceProfileTitle').setValidators([Validators.required]);
          this.profileConfigFormGroup.get('newDeviceProfileTitle').enable();
          this.profileConfigFormGroup.updateValueAndValidity();
          this.createdProfile = true;
        }
      }
    ));

    this.transportConfigFormGroup = this.fb.group(
      {
        transportConfiguration: [createDeviceProfileTransportConfiguration(DeviceTransportType.DEFAULT), Validators.required]
      }
    );
    this.subscriptions.push(this.deviceWizardFormGroup.get('transportType').valueChanges.subscribe((transportType) => {
      this.deviceProfileTransportTypeChanged(transportType);
    }));

    this.alarmRulesFormGroup = this.fb.group({
        alarms: [null]
      }
    );

    this.specificConfigFormGroup = this.fb.group({
        customerId: [null],
        setCredential: [false],
        credential: [{value: null, disabled: true}]
      }
    );

    this.subscriptions.push(this.specificConfigFormGroup.get('setCredential').valueChanges.subscribe((value) => {
      if (value) {
        this.specificConfigFormGroup.get('credential').enable();
      } else {
        this.specificConfigFormGroup.get('credential').disable();
      }
    }));
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  previousStep(): void {
    this.addDeviceWizardStepper.previous();
  }

  nextStep(): void {
    if (this.selectedIndex < this.maxStepperIndex) {
      this.addDeviceWizardStepper.next();
    } else {
      this.add();
    }
  }

  get selectedForm(): FormGroup {
    const index = !this.createdProfile && this.selectedIndex === this.maxStepperIndex ? 4 : this.selectedIndex;
    switch (index) {
      case 0:
        return this.deviceWizardFormGroup;
      case 1:
        return this.profileConfigFormGroup;
      case 2:
        return this.transportConfigFormGroup;
      case 3:
        return this.alarmRulesFormGroup;
      case 4:
        return this.specificConfigFormGroup;
    }
  }

  get maxStepperIndex(): number {
    return this.addDeviceWizardStepper?._steps?.length - 1;
  }

  private deviceProfileTransportTypeChanged(deviceTransportType: DeviceTransportType): void {
    this.transportConfigFormGroup.patchValue(
      {transportConfiguration: createDeviceProfileTransportConfiguration(deviceTransportType)});
  }

  private add(): void {
    this.creatProfile().pipe(
      mergeMap(profileId => this.createdDevice(profileId)),
      mergeMap(device => this.saveCredential(device))
    ).subscribe(
      (created) => {
        this.dialogRef.close(created);
      }
    );
  }

  private creatProfile(): Observable<EntityId> {
    if (this.profileConfigFormGroup.get('addProfileType').value) {
      const deviceProfile: DeviceProfile = {
        name: this.profileConfigFormGroup.get('newDeviceProfileTitle').value,
        type: DeviceProfileType.DEFAULT,
        transportType: this.deviceWizardFormGroup.get('transportType').value,
        profileData: {
          configuration: createDeviceProfileConfiguration(DeviceProfileType.DEFAULT),
          transportConfiguration: this.transportConfigFormGroup.get('transportConfiguration').value,
          alarms: this.alarmRulesFormGroup.get('alarms').value
        }
      };
      return this.deviceProfileService.saveDeviceProfile(deviceProfile).pipe(
        map(profile => profile.id),
        tap((profileId) => {
          this.profileConfigFormGroup.patchValue({
            deviceProfileId: profileId,
            addProfileType: 0
          });
          this.addDeviceWizardStepper.selectedIndex = 2;
        })
      );
    } else {
      return of(null);
    }
  }

  private createdDevice(profileId: EntityId = this.profileConfigFormGroup.get('deviceProfileId').value): Observable<BaseData<HasId>> {
    const device = {
      name: this.deviceWizardFormGroup.get('name').value,
      label: this.deviceWizardFormGroup.get('label').value,
      deviceProfileId: profileId,
      additionalInfo: {
        gateway: this.deviceWizardFormGroup.get('gateway').value,
        description: this.deviceWizardFormGroup.get('description').value
      },
      customerId: null
    };
    if (this.specificConfigFormGroup.get('customerId').value) {
      device.customerId = {
        entityType: EntityType.CUSTOMER,
        id: this.specificConfigFormGroup.get('customerId').value
      };
    }
    return this.data.entitiesTableConfig.saveEntity(device);
  }

  private saveCredential(device: BaseData<HasId>): Observable<boolean> {
    if (this.specificConfigFormGroup.get('setCredential').value) {
      return this.deviceService.getDeviceCredentials(device.id.id).pipe(
        mergeMap(
          (deviceCredentials) => {
            const deviceCredentialsValue = {...deviceCredentials, ...this.specificConfigFormGroup.value.credential};
            return this.deviceService.saveDeviceCredentials(deviceCredentialsValue);
          }
        ),
        map(() => true));
    }
    return of(true);
  }

  changeStep($event: StepperSelectionEvent): void {
    this.selectedIndex = $event.selectedIndex;
    if (this.selectedIndex === this.maxStepperIndex) {
      this.nextStepButtonLabel$.next('action.add');
    } else {
      this.nextStepButtonLabel$.next('action.continue');
    }
  }
}
