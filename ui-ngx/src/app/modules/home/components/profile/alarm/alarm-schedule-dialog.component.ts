// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { AlarmSchedule } from '@shared/models/device.models';

export interface AlarmScheduleDialogData {
  readonly: boolean;
  alarmSchedule: AlarmSchedule;
}

@Component({
    selector: 'tb-alarm-schedule-dialog',
    templateUrl: './alarm-schedule-dialog.component.html',
    providers: [{ provide: ErrorStateMatcher, useExisting: AlarmScheduleDialogComponent }],
    styleUrls: [],
    standalone: false
})
export class AlarmScheduleDialogComponent extends DialogComponent<AlarmScheduleDialogComponent, AlarmSchedule>
  implements OnInit, ErrorStateMatcher {

  readonly = this.data.readonly;
  alarmSchedule = this.data.alarmSchedule;

  alarmScheduleFormGroup: UntypedFormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmScheduleDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<AlarmScheduleDialogComponent, AlarmSchedule>,
              private fb: UntypedFormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.alarmScheduleFormGroup = this.fb.group({
      alarmSchedule: [this.alarmSchedule]
    });
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
