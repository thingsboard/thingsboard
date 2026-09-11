// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';

export interface IotHubInstalledItemsDialogData {
  item: MpItemVersionView;
}

@Component({
  selector: 'tb-iot-hub-installed-items-dialog',
  standalone: false,
  templateUrl: './iot-hub-installed-items-dialog.component.html',
  styleUrls: ['./iot-hub-installed-items-dialog.component.scss']
})
export class TbIotHubInstalledItemsDialogComponent extends DialogComponent<TbIotHubInstalledItemsDialogComponent> {

  item: MpItemVersionView;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubInstalledItemsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubInstalledItemsDialogData
  ) {
    super(store, router, dialogRef);
    this.item = data.item;
  }

  close(): void {
    this.dialogRef.close();
  }
}
