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
import { FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { AlarmRuleSchedule } from "@shared/models/alarm-rule.models";
import { CalculatedFieldArgument } from "@shared/models/calculated-field.models";

export interface AlarmRuleScheduleDialogData {
  readonly: boolean;
  alarmSchedule: AlarmRuleSchedule;
  arguments: Record<string, CalculatedFieldArgument>;
}

@Component({
    selector: 'tb-cf-alarm-schedule-dialog',
    templateUrl: './cf-alarm-schedule-dialog.component.html',
    providers: [],
    styleUrls: ['./cf-alarm-rules-dialog.component.scss'],
    standalone: false
})
export class CfAlarmScheduleDialogComponent extends DialogComponent<CfAlarmScheduleDialogComponent, AlarmRuleSchedule>{

  readonly = this.data.readonly;
  alarmSchedule = this.data.alarmSchedule;
  arguments = this.data.arguments;

  alarmScheduleControl = this.fb.control<AlarmRuleSchedule>(null);

  dynamicModeControl = this.fb.control(false);

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleScheduleDialogData,
              public dialogRef: MatDialogRef<CfAlarmScheduleDialogComponent, AlarmRuleSchedule>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.alarmScheduleControl.patchValue(this.alarmSchedule, {emitEvent: false});
    this.dynamicModeControl.patchValue(!!this.alarmSchedule?.dynamicValueArgument, {emitEvent: false});
    if (this.readonly) {
      this.alarmScheduleControl.disable({emitEvent: false});
      this.dynamicModeControl.disable({emitEvent: false});
    }
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.dialogRef.close(this.alarmScheduleControl.value);
  }
}
