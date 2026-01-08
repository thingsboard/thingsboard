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
  styleUrls: ['./alert-dialog.component.scss']
})
export class AlertDialogComponent extends DialogComponent<AlertDialogComponent, boolean>{
  constructor(protected store: Store<AppState>,
              protected router: Router,
              public dialogRef: MatDialogRef<AlertDialogComponent, boolean>,
              @Inject(MAT_DIALOG_DATA) public data: AlertDialogData) {
    super(store,  router, dialogRef);
  }
}
