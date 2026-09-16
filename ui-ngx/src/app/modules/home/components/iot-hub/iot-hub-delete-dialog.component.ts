// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { IotHubInstalledItem } from 'src/app/shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';

export interface IotHubDeleteDialogData {
  installedItemId: string;
  itemName: string;
  itemType?: string;
}

@Component({
  selector: 'tb-iot-hub-delete-dialog',
  standalone: false,
  templateUrl: './iot-hub-delete-dialog.component.html',
  styleUrls: ['./iot-hub-delete-dialog.component.scss']
})
export class TbIotHubDeleteDialogComponent extends DialogComponent<TbIotHubDeleteDialogComponent, boolean> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubDeleteDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubDeleteDialogData,
    private iotHubApiService: IotHubApiService
  ) {
    super(store, router, dialogRef);
  }

  confirm(): void {
    this.iotHubApiService.deleteInstalledItem(this.data.installedItemId).subscribe(
      () => {
        this.dialogRef.close(true);
      }
    );
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
