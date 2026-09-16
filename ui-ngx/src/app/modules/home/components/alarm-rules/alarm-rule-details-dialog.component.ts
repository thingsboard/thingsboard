// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
