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
