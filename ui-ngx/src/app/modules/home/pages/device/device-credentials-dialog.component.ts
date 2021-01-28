///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { credentialTypeNames, DeviceCredentials, DeviceCredentialsType } from '@shared/models/device.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';

export interface DeviceCredentialsDialogData {
  isReadOnly: boolean;
  deviceId: string;
}

@Component({
  selector: 'tb-device-credentials-dialog',
  templateUrl: './device-credentials-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DeviceCredentialsDialogComponent}],
  styleUrls: []
})
export class DeviceCredentialsDialogComponent extends
  DialogComponent<DeviceCredentialsDialogComponent, DeviceCredentials> implements OnInit, ErrorStateMatcher {

  deviceCredentialsFormGroup: FormGroup;

  isReadOnly: boolean;

  deviceCredentials: DeviceCredentials;

  submitted = false;

  deviceCredentialsType = DeviceCredentialsType;

  credentialsTypes = Object.keys(DeviceCredentialsType);

  credentialTypeNamesMap = credentialTypeNames;

  hidePassword = true;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogData,
              private deviceService: DeviceService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DeviceCredentialsDialogComponent, DeviceCredentials>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.isReadOnly = data.isReadOnly;
  }

  ngOnInit(): void {
    this.deviceCredentialsFormGroup = this.fb.group({
      credential: [null]
    });
    if (this.isReadOnly) {
      this.deviceCredentialsFormGroup.disable({emitEvent: false});
    }
    this.loadDeviceCredentials();
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  loadDeviceCredentials() {
    this.deviceService.getDeviceCredentials(this.data.deviceId).subscribe(
      (deviceCredentials) => {
        this.deviceCredentials = deviceCredentials;
        this.deviceCredentialsFormGroup.patchValue({
          credential: deviceCredentials
        }, {emitEvent: false});
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value.credential;
    this.deviceCredentials = {...this.deviceCredentials, ...deviceCredentialsValue};
    this.deviceService.saveDeviceCredentials(this.deviceCredentials).subscribe(
      (deviceCredentials) => {
        this.dialogRef.close(deviceCredentials);
      }
    );
  }
}
