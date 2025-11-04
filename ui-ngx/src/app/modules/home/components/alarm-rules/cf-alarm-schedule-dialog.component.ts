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

import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroupDirective, NgForm, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
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
  providers: [{provide: ErrorStateMatcher, useExisting: CfAlarmScheduleDialogComponent}],
  styleUrls: ['./cf-alarm-rules-dialog.component.scss'],
})
export class CfAlarmScheduleDialogComponent extends DialogComponent<CfAlarmScheduleDialogComponent, AlarmRuleSchedule>
  implements OnInit, ErrorStateMatcher {

  readonly = this.data.readonly;
  alarmSchedule = this.data.alarmSchedule;
  arguments = this.data.arguments;

  alarmScheduleFormGroup: UntypedFormGroup;

  submitted = false;

  settingsMode: 'static' | 'dynamic' = 'static';

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmRuleScheduleDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<CfAlarmScheduleDialogComponent, AlarmRuleSchedule>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.alarmScheduleFormGroup = this.fb.group({
      alarmSchedule: [this.alarmSchedule]
    });
    this.settingsMode = this.alarmSchedule?.dynamicValueArgument ? 'dynamic' : 'static';
    if (this.readonly) {
      this.alarmScheduleFormGroup.disable({emitEvent: false});
    }
  }

  ngOnInit(): void {
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
    this.alarmSchedule = this.alarmScheduleFormGroup.get('alarmSchedule').value;
    this.dialogRef.close(this.alarmSchedule);
  }
}
