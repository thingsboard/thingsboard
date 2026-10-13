// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, Inject } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface CancelTaskDialogData {
  title: string;
  message: string;
}

@Component({
    selector: 'tb-confirm-dialog',
    templateUrl: './cancel-task-dialog.component.html',
    standalone: false
})
export class CancelTaskDialogComponent extends DialogComponent<CancelTaskDialogComponent, boolean>{
  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<CancelTaskDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: CancelTaskDialogData) {
    super(store, router, dialogRef);
  }
}
