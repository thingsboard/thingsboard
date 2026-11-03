// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';

export interface ConfirmDialogData {
  title: string;
  message: string;
  cancel: string;
  ok: string;
}

// @dynamic
@Component({
    selector: 'tb-confirm-dialog',
    templateUrl: './confirm-dialog.component.html',
    styleUrls: ['./confirm-dialog.component.scss'],
    standalone: false
})
export class ConfirmDialogComponent extends DialogComponent<ConfirmDialogComponent, boolean>{
  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ConfirmDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData) {
    super(store, router, dialogRef);
  }
}
