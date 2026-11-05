// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';

export interface IotHubDeviceUpdateDialogData {
  itemName: string;
}

/**
 * Update of a device package. This version can only reinstall it, so the dialog leads to the upgrade that updates device
 * packages without reinstalling, as the dialog of an item that needs a newer version does, and keeps the reinstall as
 * the other way. Closes with 'reinstall' when the user chooses it.
 */
@Component({
  selector: 'tb-iot-hub-device-update-dialog',
  standalone: false,
  templateUrl: './iot-hub-device-update-dialog.component.html',
  styleUrls: ['./iot-hub-pe-required-dialog.component.scss']
})
export class TbIotHubDeviceUpdateDialogComponent extends DialogComponent<TbIotHubDeviceUpdateDialogComponent, string | boolean> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubDeviceUpdateDialogComponent, string | boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubDeviceUpdateDialogData
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close(false);
  }

  reinstall(): void {
    this.dialogRef.close('reinstall');
  }

  viewUpgradeOptions(): void {
    window.open('https://thingsboard.io/community-grant-program/', '_blank');
  }
}
