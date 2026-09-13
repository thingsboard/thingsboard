// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface IotHubSelectCfEntityDialogData {
  itemName: string;
}

@Component({
  selector: 'tb-iot-hub-select-cf-entity-dialog',
  standalone: false,
  templateUrl: './iot-hub-select-cf-entity-dialog.component.html',
  styleUrls: ['./iot-hub-install-dialog.component.scss']
})
export class TbIotHubSelectCfEntityDialogComponent
  extends DialogComponent<TbIotHubSelectCfEntityDialogComponent, EntityId | null> {

  selectedEntityId: EntityId | null = null;
  cfEntityTypes: EntityType[] = [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE];
  defaultCfEntityType = EntityType.DEVICE_PROFILE;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubSelectCfEntityDialogComponent, EntityId | null>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubSelectCfEntityDialogData
  ) {
    super(store, router, dialogRef);
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (this.selectedEntityId) {
      this.dialogRef.close(this.selectedEntityId);
    }
  }
}
