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
  styleUrls: ['./error-alert-dialog.component.scss']
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
