// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { IotHubInstalledItemKeptEntities, IotHubKeptEntity } from 'src/app/shared/models/iot-hub/iot-hub-installed-item.models';
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

  keptGroups: { reason: string; references: ResourceReferences }[] = [];
  keptTitle: string;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubDeleteDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubDeleteDialogData,
    private iotHubApiService: IotHubApiService,
    private translate: TranslateService
  ) {
    super(store, router, dialogRef);
    this.description = this.translate.instant(data.itemType === 'SOLUTION_TEMPLATE' ? 'iot-hub.remove-desc-solution-template' : 'iot-hub.remove-desc');
    const kept = data.keptEntities;
    const shared = kept?.entities ?? [];
    const referenced = kept?.referencedEntities ?? [];
    const references = (entities: IotHubKeptEntity[]): ResourceReferences =>
      entities.map(entity => ({ id: entity.entityId as EntityId, name: entity.name }));
    if (shared.length) {
      // Kept because other packages use them, because the user had them before the package, or both
      const reasonKey = !kept.usedExistingCount ? 'iot-hub.remove-kept-used' :
        !kept.usedByItemsCount ? 'iot-hub.remove-kept-own' : 'iot-hub.remove-kept-own-or-used';
      this.keptGroups.push({ reason: this.translate.instant(reasonKey, { kept: shared.length }), references: references(shared) });
    }
    if (referenced.length) {
      this.keptGroups.push({ reason: this.translate.instant('iot-hub.remove-kept-referenced'), references: references(referenced) });
    }
    if (this.keptGroups.length) {
      this.keptTitle = this.translate.instant('iot-hub.remove-kept-title', { kept: shared.length + referenced.length });
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
