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
  styleUrls: []
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
