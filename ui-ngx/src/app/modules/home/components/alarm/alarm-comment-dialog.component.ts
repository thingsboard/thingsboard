// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AlarmInfo } from '@shared/models/alarm.models';

export interface AlarmCommentDialogData {
  alarmId?: string;
  alarm?: AlarmInfo;
}

@Component({
    selector: 'tb-alarm-comment-dialog',
    templateUrl: './alarm-comment-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class AlarmCommentDialogComponent extends DialogComponent<AlarmCommentDialogComponent, void> {

  alarmId: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AlarmCommentDialogData,
              public dialogRef: MatDialogRef<AlarmCommentDialogComponent, void>) {
    super(store, router, dialogRef);
    this.alarmId = this.data.alarmId;
  }

  close(): void {
    this.dialogRef.close();
  }
}
