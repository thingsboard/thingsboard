// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { DialogComponent } from '@shared/components/dialog.component';

export interface ErrorAlertDialogData {
  title: string;
  message: string;
  error: any;
  ok: string;
}

@Component({
    selector: 'tb-error-alert-dialog',
    templateUrl: './error-alert-dialog.component.html',
    styleUrls: ['./error-alert-dialog.component.scss'],
    standalone: false
})
export class ErrorAlertDialogComponent extends DialogComponent<ErrorAlertDialogComponent, boolean>{

  title: string;
  message: string;
  errorMessage: string;
  errorDetails?: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<ErrorAlertDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: ErrorAlertDialogData) {
    super(store, router, dialogRef);
    this.title = this.data.title;
    this.message = this.data.message;
    this.errorMessage = this.data.error.message ? this.data.error.message : JSON.stringify(this.data.error);
    if (this.data.error.stack) {
      this.errorDetails = this.data.error.stack.replaceAll('\n', '<br/>');
    }
  }

}
