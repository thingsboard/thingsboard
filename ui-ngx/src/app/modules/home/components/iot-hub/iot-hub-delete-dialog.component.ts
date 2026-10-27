// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { IotHubInstalledItemKeptEntities } from 'src/app/shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';

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

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubDeleteDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubDeleteDialogData,
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
    this.description = data.keptEntities?.entities.length ? this.describeKeptEntities(data.keptEntities) :
      this.translate.instant(data.itemType === 'SOLUTION_TEMPLATE' ? 'iot-hub.remove-desc-solution-template' : 'iot-hub.remove-desc');
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

  /** A device package keeps the entities other installed packages use, and the description names them before the user confirms. */
  private describeKeptEntities(kept: IotHubInstalledItemKeptEntities): string {
    const names = kept.entities.map(entity => {
      const key = `iot-hub.remove-kept-entity.${entity.entityId.entityType}`;
      const named = this.translate.instant(key, { name: entity.name });
      return named === key ? `'${entity.name}'` : named;
    });
    const entities = names.length === 1 ? names[0] :
      this.translate.instant('iot-hub.remove-kept-entities', { first: names.slice(0, -1).join(', '), last: names[names.length - 1] });
    return this.translate.instant('iot-hub.remove-desc-kept',
      { entities, count: kept.usedByItemsCount, kept: kept.entities.length });
  }
}
