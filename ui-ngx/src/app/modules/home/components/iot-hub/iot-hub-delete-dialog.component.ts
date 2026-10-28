// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { IotHubInstalledItemKeptEntities } from 'src/app/shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { ResourceReferences } from '@shared/models/resource.models';
import { EntityId } from '@shared/models/id/entity-id';

export interface IotHubDeleteDialogData {
  installedItemId: string;
  itemName: string;
  itemType?: string;
  keptEntities?: IotHubInstalledItemKeptEntities | null;
}

@Component({
  selector: 'tb-iot-hub-delete-dialog',
  standalone: false,
  templateUrl: './iot-hub-delete-dialog.component.html',
  styleUrls: ['./iot-hub-delete-dialog.component.scss']
})
export class TbIotHubDeleteDialogComponent extends DialogComponent<TbIotHubDeleteDialogComponent, boolean> {

  description: string;

  // A device package keeps the entities other installed packages use, and the dialog lists them before the user confirms
  keptReferences: ResourceReferences | null = null;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubDeleteDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubDeleteDialogData,
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
    const kept = data.keptEntities;
    if (kept?.entities.length) {
      this.keptReferences = kept.entities.map(entity => ({ id: entity.entityId as EntityId, name: entity.name }));
      this.description = this.translate.instant('iot-hub.remove-desc-kept',
        { count: kept.usedByItemsCount, kept: kept.entities.length });
    } else {
      this.description = this.translate.instant(data.itemType === 'SOLUTION_TEMPLATE' ? 'iot-hub.remove-desc-solution-template' : 'iot-hub.remove-desc');
    }
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
