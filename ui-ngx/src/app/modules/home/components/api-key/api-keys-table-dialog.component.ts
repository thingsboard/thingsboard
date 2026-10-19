// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0

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
