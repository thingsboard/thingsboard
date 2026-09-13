// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { AfterViewInit, Component, Inject, SkipSelf, ViewChild } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormControl } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DeviceProfile } from '@shared/models/device.models';
import { DeviceProfileComponent } from './device-profile.component';
import { DeviceProfileService } from '@core/http/device-profile.service';

export interface DeviceProfileDialogData {
  deviceProfile: DeviceProfile;
  isAdd: boolean;
}

@Component({
    selector: 'tb-device-profile-dialog',
    templateUrl: './device-profile-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: DeviceProfileDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class DeviceProfileDialogComponent extends
  DialogComponent<DeviceProfileDialogComponent, DeviceProfile> implements ErrorStateMatcher, AfterViewInit {

  isAdd: boolean;
  deviceProfile: DeviceProfile;

  submitted = false;

  @ViewChild('deviceProfileComponent', {static: true}) deviceProfileComponent: DeviceProfileComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceProfileDialogData,
              public dialogRef: MatDialogRef<DeviceProfileDialogComponent, DeviceProfile>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              private deviceProfileService: DeviceProfileService) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.deviceProfile = this.data.deviceProfile;
  }

  ngAfterViewInit(): void {
    if (this.isAdd) {
      setTimeout(() => {
        this.deviceProfileComponent.entityForm.markAsDirty();
      }, 0);
    }
  }

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.deviceProfileComponent.entityForm.valid) {
      this.deviceProfile = {...this.deviceProfile, ...this.deviceProfileComponent.entityFormValue()};
      this.deviceProfileService.saveDeviceProfileAndConfirmOtaChange(this.deviceProfile, this.deviceProfile).subscribe(
        (deviceProfile) => {
          this.dialogRef.close(deviceProfile);
        }
      );
    }
  }
}
