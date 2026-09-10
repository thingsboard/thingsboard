// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

export interface IotHubUnpublishedWarningDialogData {
  item: MpItemVersionView;
}

@Component({
  selector: 'tb-iot-hub-unpublished-warning-dialog',
  standalone: false,
  templateUrl: './iot-hub-unpublished-warning-dialog.component.html',
  styleUrls: ['./iot-hub-unpublished-warning-dialog.component.scss']
})
export class TbIotHubUnpublishedWarningDialogComponent extends DialogComponent<TbIotHubUnpublishedWarningDialogComponent, boolean> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubUnpublishedWarningDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubUnpublishedWarningDialogData
  ) {
    super(store, router, dialogRef);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
