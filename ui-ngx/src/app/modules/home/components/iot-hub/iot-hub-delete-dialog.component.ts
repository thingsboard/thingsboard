// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { DeleteInstalledItemResult, IotHubInstalledItem } from 'src/app/shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { entityTypeTranslations, EntityType } from '@shared/models/entity-type.models';

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
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
  }

  confirm(): void {
    this.iotHubApiService.deleteInstalledItem(this.data.installedItemId).subscribe(
      result => {
        this.notifyKeptEntities(result);
        this.dialogRef.close(true);
      }
    );
  }

  /** Says which entities stayed because other installed items still use them. */
  private notifyKeptEntities(result: DeleteInstalledItemResult | null): void {
    const kept = result?.keptEntities ?? [];
    if (!kept.length) {
      return;
    }
    const entities = kept.map(entity => {
      const typeKey = entityTypeTranslations.get(entity.entityId.entityType as EntityType)?.type;
      const type = typeKey ? this.translate.instant(typeKey) : entity.entityId.entityType;
      return this.translate.instant('iot-hub.delete-kept-entity', { type, usedBy: entity.usedBy.join(', ') });
    }).join(' ');
    this.store.dispatch(new ActionNotificationShow({
      message: this.translate.instant('iot-hub.delete-kept-entities', { entities }),
      type: 'info',
      duration: 10000,
      verticalPosition: 'top',
      horizontalPosition: 'right'
    }));
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
