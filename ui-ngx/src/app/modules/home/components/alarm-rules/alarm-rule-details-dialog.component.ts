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

import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';

export interface AlarmRuleDetailsDialogData {
  alarmDetails: string;
  readonly: boolean;
}

@Component({
    selector: 'tb-edit-alarm-details-dialog',
    templateUrl: './alarm-rule-details-dialog.component.html',
    providers: [],
    styleUrls: ['./cf-alarm-rules-dialog.component.scss'],
    standalone: false
})
export class AlarmRuleDetailsDialogComponent extends DialogComponent<AlarmRuleDetailsDialogComponent, string> {

  alarmDetailsControl: FormControl<string>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleDetailsDialogData,
              public dialogRef: MatDialogRef<AlarmRuleDetailsDialogComponent, string>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.alarmDetailsControl = this.fb.control(this.data.alarmDetails);
    if (this.data.readonly) {
      this.alarmDetailsControl.disable();
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.alarmDetailsControl.value);
  }
}
