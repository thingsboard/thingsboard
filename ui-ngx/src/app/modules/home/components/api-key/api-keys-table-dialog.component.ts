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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UserId } from '@shared/models/id/user-id';

export interface ApiKeysTableDialogData {
  userId: UserId;
}

@Component({
    selector: 'tb-api-keys-table-dialog',
    templateUrl: './api-keys-table-dialog.component.html',
    styleUrls: ['api-keys-table-dialog.component.scss'],
    standalone: false
})
export class ApiKeysTableDialogComponent {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    public dialogRef: MatDialogRef<ApiKeysTableDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ApiKeysTableDialogData,
  ) {
  }

  close(): void {
    this.dialogRef.close(null);
  }
}
