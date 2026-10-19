// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';

@Component({
  selector: 'tb-iot-hub-pe-required-dialog',
  standalone: false,
  templateUrl: './iot-hub-pe-required-dialog.component.html',
  styleUrls: ['./iot-hub-pe-required-dialog.component.scss']
})
export class TbIotHubPeRequiredDialogComponent extends DialogComponent<TbIotHubPeRequiredDialogComponent> {

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubPeRequiredDialogComponent>
  ) {
    super(store, router, dialogRef);
  }

  close(): void {
    this.dialogRef.close();
  }

  upgradeInstance(): void {
    window.open('https://thingsboard.io/docs/pe/installation/upgrade-from-ce/', '_blank');
  }
}
