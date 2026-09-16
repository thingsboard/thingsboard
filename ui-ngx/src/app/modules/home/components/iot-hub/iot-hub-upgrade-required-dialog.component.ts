// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { tbVersionIntToString } from '@core/http/iot-hub-api.service';
import { environment as env } from '@env/environment';

export interface IotHubUpgradeRequiredDialogData {
  minTbVersion: number;
}

@Component({
  selector: 'tb-iot-hub-upgrade-required-dialog',
  standalone: false,
  templateUrl: './iot-hub-upgrade-required-dialog.component.html',
  styleUrls: ['./iot-hub-upgrade-required-dialog.component.scss']
})
export class TbIotHubUpgradeRequiredDialogComponent extends DialogComponent<TbIotHubUpgradeRequiredDialogComponent> {

  readonly minVersion: string;
  readonly currentVersion: string;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubUpgradeRequiredDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubUpgradeRequiredDialogData
  ) {
    super(store, router, dialogRef);
    this.minVersion = `v${tbVersionIntToString(data.minTbVersion)}`;
    this.currentVersion = `v${env.tbVersion}`;
  }

  close(): void {
    this.dialogRef.close();
  }

  upgradeInstance(): void {
    window.open('https://thingsboard.io/docs/installation/upgrade-instructions/', '_blank');
  }
}
