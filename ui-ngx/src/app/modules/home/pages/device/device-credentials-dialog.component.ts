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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { DeviceCredentials, DeviceProfileInfo, DeviceTransportType } from '@shared/models/device.models';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { forkJoin, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpStatusCode } from '@angular/common/http';

export interface DeviceCredentialsDialogData {
  isReadOnly: boolean;
  deviceId: string;
  deviceProfileId: string;
}

@Component({
    selector: 'tb-device-credentials-dialog',
    templateUrl: './device-credentials-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: DeviceCredentialsDialogComponent }],
    styleUrls: ['./device-credentials-dialog.component.scss'],
    standalone: false
})
export class DeviceCredentialsDialogComponent extends
  DialogComponent<DeviceCredentialsDialogComponent, DeviceCredentials> implements OnInit, ErrorStateMatcher {

  deviceCredentialsFormGroup: UntypedFormGroup;
  deviceTransportType: DeviceTransportType;
  isReadOnly: boolean;
  loadingCredentials = true;

  private deviceCredentials: DeviceCredentials;
  private submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceCredentialsDialogData,
              private deviceService: DeviceService,
              private deviceProfileService: DeviceProfileService,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<DeviceCredentialsDialogComponent, DeviceCredentials>,
              public fb: UntypedFormBuilder) {
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

  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  loadDeviceCredentials() {
    const task = [
      this.deviceService.getDeviceCredentials(this.data.deviceId),
      this.deviceProfileService.getDeviceProfileInfo(this.data.deviceProfileId)
    ];
    forkJoin(task).subscribe(([deviceCredentials, deviceProfile]: [DeviceCredentials, DeviceProfileInfo]) => {
      this.deviceTransportType = deviceProfile.transportType;
      this.deviceCredentials = deviceCredentials;
      this.deviceCredentialsFormGroup.patchValue({
        credential: deviceCredentials
      }, {emitEvent: false});
      this.loadingCredentials = false;
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    const deviceCredentialsValue = this.deviceCredentialsFormGroup.value.credential;
    this.deviceCredentials = {...this.deviceCredentials, ...deviceCredentialsValue};
    this.deviceService.saveDeviceCredentials(this.deviceCredentials)
      .pipe(
        catchError((err) => {
          if (err.status === HttpStatusCode.Conflict) {
            return this.deviceService.getDeviceCredentials(this.deviceCredentials.deviceId.id);
          }
          return throwError(() => err);
        })
      )
      .subscribe(
      (deviceCredentials) => {
        this.dialogRef.close(deviceCredentials);
      }
    );
  }
}
