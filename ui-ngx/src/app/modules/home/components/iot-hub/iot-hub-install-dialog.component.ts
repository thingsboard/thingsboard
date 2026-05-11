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
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DialogComponent } from '@shared/components/dialog.component';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { SolutionTemplateInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { resolveEntityDetailsUrl } from './iot-hub-components.models';
import { SolutionInstallDialogComponent } from '@home/components/iot-hub/solution-install-dialog.component';

export interface IotHubInstallDialogData {
  item: MpItemVersionView;
}

export type InstallState = 'select-entity' | 'confirm' | 'installing' | 'success' | 'error';

@Component({
  selector: 'tb-iot-hub-install-dialog',
  standalone: false,
  templateUrl: './iot-hub-install-dialog.component.html',
  styleUrls: ['./iot-hub-install-dialog.component.scss']
})
export class TbIotHubInstallDialogComponent extends DialogComponent<TbIotHubInstallDialogComponent> {

  ItemType = ItemType;

  item: MpItemVersionView;
  typeTranslations = itemTypeTranslations;
  state: InstallState = 'confirm';
  errorMessage = '';
  entityDetailsUrl: string | null = null;

  selectedEntityId: EntityId | null = null;
  cfEntityTypes: EntityType[] = [EntityType.DEVICE, EntityType.ASSET, EntityType.DEVICE_PROFILE, EntityType.ASSET_PROFILE];
  defaultCfEntityType = EntityType.DEVICE_PROFILE;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubInstallDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubInstallDialogData,
    private dialog: MatDialog,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService
  ) {
    super(store, router, dialogRef);
    this.item = data.item;
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.item.type);
    return key ? this.translate.instant(key) : '';
  }

  install(): void {
    if (this.item.type === ItemType.CALCULATED_FIELD || this.item.type === ItemType.ALARM_RULE) {
      this.state = 'select-entity';
      return;
    }
    this.doInstall();
  }

  doInstall(): void {
    this.state = 'installing';
    const versionId = this.item.id as string;
    const data = this.selectedEntityId ? { entityId: this.selectedEntityId } : undefined;
    this.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }, data).subscribe({
      next: (result) => {
        if (result.success) {
          if (result.descriptor?.type === 'SOLUTION_TEMPLATE') {
            const timeout = this.item.dataDescriptor?.installTimeoutMs;
            const openSolutionDialog = () => {
              this.dialogRef.close('installed');
              this.dialog.open(SolutionInstallDialogComponent, {
                disableClose: true,
                autoFocus: false,
                panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
                data: { descriptor: result.descriptor as SolutionTemplateInstalledItemDescriptor }
              });
            };
            if (timeout > 0) {
              setTimeout(openSolutionDialog, timeout);
            } else {
              openSolutionDialog();
            }
          } else {
            this.state = 'success';
            this.entityDetailsUrl = resolveEntityDetailsUrl(result.descriptor, this.item.type);
          }
        } else {
          this.state = 'error';
          this.errorMessage = result.errorMessage || this.translate.instant('iot-hub.install-error', { name: this.item.name });
        }
      },
      error: (err) => {
        this.state = 'error';
        this.errorMessage = err?.error?.message || err?.message || this.translate.instant('iot-hub.install-error', { name: this.item.name });
      }
    });
  }

  openEntityDetails(): void {
    if (this.entityDetailsUrl) {
      this.dialogRef.close('installed');
      void this.router.navigateByUrl(this.entityDetailsUrl);
    }
  }

  close(): void {
    this.dialogRef.close(this.state === 'success' ? 'installed' : false);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
