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
  Component,
  Inject,
  OnInit,
  SkipSelf
} from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { DialogComponent } from '@shared/components/dialog.component';
import { Router } from '@angular/router';
import { DeviceProfileAlarm } from '@shared/models/device.models';

export interface DeviceProfileAlarmDialogData {
  alarm: DeviceProfileAlarm;
  isAdd: boolean;
  isReadOnly: boolean;
}

@Component({
  selector: 'tb-device-profile-alarm-dialog',
  templateUrl: './device-profile-alarm-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: DeviceProfileAlarmDialogComponent}],
  styleUrls: []
})
export class DeviceProfileAlarmDialogComponent extends
  DialogComponent<DeviceProfileAlarmDialogComponent, DeviceProfileAlarm> implements OnInit, ErrorStateMatcher {

  alarmFormGroup: FormGroup;

  isReadOnly = this.data.isReadOnly;
  alarm = this.data.alarm;
  isAdd = this.data.isAdd;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: DeviceProfileAlarmDialogData,
              public dialogRef: MatDialogRef<DeviceProfileAlarmDialogComponent, DeviceProfileAlarm>,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
    this.isAdd = this.data.isAdd;
    this.alarm = this.data.alarm;
  }

  ngOnInit(): void {
    this.alarmFormGroup = this.fb.group({
      id: [null, Validators.required],
      alarmType: [null, Validators.required],
      createRules: [null],
      clearRule: [null],
      propagate: [null],
      propagateRelationTypes: [null]
    });
    this.alarmFormGroup.reset(this.alarm, {emitEvent: false});
    if (this.isReadOnly) {
      this.alarmFormGroup.disable({emitEvent: false});
    }
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    if (this.alarmFormGroup.valid) {
      this.alarm = {...this.alarm, ...this.alarmFormGroup.value};
      this.dialogRef.close(this.alarm);
    }
  }

}
