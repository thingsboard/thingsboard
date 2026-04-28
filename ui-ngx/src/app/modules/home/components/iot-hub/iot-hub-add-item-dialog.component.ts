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
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityId } from '@shared/models/id/entity-id';
import { IotHubActionsService } from './iot-hub-actions.service';

export interface IotHubAddItemDialogData {
  itemType: ItemType;
  itemSubType?: string;
  entityId?: EntityId;
}

export interface IotHubAddItemDialogResult {
  item: MpItemVersionView;
  descriptor: any;
}

@Component({
  selector: 'tb-iot-hub-add-item-dialog',
  standalone: false,
  templateUrl: './iot-hub-add-item-dialog.component.html',
  styleUrls: ['./iot-hub-add-item-dialog.component.scss']
})
export class TbIotHubAddItemDialogComponent extends DialogComponent<TbIotHubAddItemDialogComponent, IotHubAddItemDialogResult> {

  itemType: ItemType;
  itemSubType: string;
  isInstalling = false;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubAddItemDialogComponent, IotHubAddItemDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubAddItemDialogData,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService,
    private dialogService: DialogService,
    private iotHubActions: IotHubActionsService
  ) {
    super(store, router, dialogRef);
    this.itemType = data.itemType;
    this.itemSubType = data.itemSubType;
  }

  getTitle(): string {
    const typeKey = itemTypeTranslations.get(this.itemType);
    const typeLabel = typeKey ? this.translate.instant(typeKey) : '';
    return this.translate.instant('iot-hub.add-item-from-iot-hub', { type: typeLabel });
  }

  onAddItem(item: MpItemVersionView): void {
    if (this.itemType === ItemType.DEVICE) {
      this.installDeviceItem(item);
      return;
    }
    this.isInstalling = true;
    const versionId = item.id as string;
    const installData = this.data.entityId ? { entityId: this.data.entityId } : undefined;
    this.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }, installData).subscribe({
      next: (result) => {
        this.isInstalling = false;
        if (result.success) {
          this.dialogRef.close({ item, descriptor: result.descriptor } as IotHubAddItemDialogResult);
        } else {
          const message = result.errorMessage || this.translate.instant('iot-hub.install-error', { name: item.name });
          this.dialogService.alert(
            this.translate.instant('iot-hub.install-error-title'),
            message
          );
        }
      },
      error: (err) => {
        this.isInstalling = false;
        const message = err?.error?.message || err?.message || this.translate.instant('iot-hub.install-error', { name: item.name });
        this.dialogService.alert(
          this.translate.instant('iot-hub.install-error-title'),
          message
        );
      }
    });
  }

  private installDeviceItem(item: MpItemVersionView): void {
    this.iotHubActions.installDevice(item).subscribe(result => {
      if (result === 'installed') {
        this.dialogRef.close({ item, descriptor: { type: 'DEVICE' } } as IotHubAddItemDialogResult);
      }
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
