// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DialogComponent } from '@shared/components/dialog.component';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';

export interface AlertDialogData {
  title: string;
  message: string;
  ok: string;
  textMode?: boolean;
}

@Component({
    selector: 'tb-alert-dialog',
    templateUrl: './alert-dialog.component.html',
    styleUrls: ['./alert-dialog.component.scss'],
    standalone: false
})
export class AlertDialogComponent extends DialogComponent<AlertDialogComponent, boolean>{
  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AlertDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) public data: AlertDialogData) {
    super(store,  router, dialogRef);
  }
}
