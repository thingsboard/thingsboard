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
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import {
  getInstalledItemUrl,
  SolutionTemplateInstalledItemDescriptor
} from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { DialogService } from '@core/services/dialog.service';
import { TranslateService } from '@ngx-translate/core';
import { SolutionInstallDialogComponent } from '@home/components/iot-hub/solution-install-dialog.component';

export interface IotHubUpdateDialogData {
  installedItemId: string;
  itemName: string;
  itemType: ItemType;
  version: string;
  versionId: string;
}

export type UpdateState = 'confirm' | 'updating' | 'success' | 'error';

@Component({
  selector: 'tb-iot-hub-update-dialog',
  standalone: false,
  templateUrl: './iot-hub-update-dialog.component.html',
  styleUrls: ['./iot-hub-install-dialog.component.scss']
})
export class TbIotHubUpdateDialogComponent extends DialogComponent<TbIotHubUpdateDialogComponent, string | boolean> {

  ItemType = ItemType;

  typeTranslations = itemTypeTranslations;
  state: UpdateState = 'confirm';
  errorMessage = '';
  entityDetailsUrl: string | null = null;

  constructor(
    protected store: Store<AppState>,
    protected router: Router,
    protected dialogRef: MatDialogRef<TbIotHubUpdateDialogComponent, string | boolean>,
    @Inject(MAT_DIALOG_DATA) public data: IotHubUpdateDialogData,
    private dialog: MatDialog,
    private dialogService: DialogService,
    private translate: TranslateService,
    private iotHubApiService: IotHubApiService
  ) {
    super(store, router, dialogRef);
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.data.itemType);
    return key ? this.translate.instant(key) : '';
  }

  update(force = false): void {
    this.state = 'updating';
    this.iotHubApiService.updateItemVersion(this.data.installedItemId, this.data.versionId, { ignoreLoading: true }, force).subscribe({
      next: (result) => {
        if (result.success) {
          if (result.descriptor?.type === 'SOLUTION_TEMPLATE') {
            this.dialogRef.close('updated');
            this.dialog.open(SolutionInstallDialogComponent, {
              disableClose: true,
              autoFocus: false,
              panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
              data: { descriptor: result.descriptor as SolutionTemplateInstalledItemDescriptor }
            });
          } else {
            this.state = 'success';
            this.entityDetailsUrl = getInstalledItemUrl(result.descriptor);
          }
        } else if (result.entityModified) {
          this.state = 'confirm';
          this.dialogService.confirm(
            this.translate.instant('iot-hub.entity-modified-title'),
            this.translate.instant('iot-hub.entity-modified-text', { type: this.getTypeLabel() }),
            this.translate.instant('action.no'),
            this.translate.instant('action.yes')
          ).subscribe(confirmed => {
            if (confirmed) {
              this.update(true);
            }
          });
        } else {
          this.state = 'error';
          this.errorMessage = result.errorMessage || this.translate.instant('iot-hub.update-error', { name: this.data.itemName });
        }
      },
      error: (err) => {
        this.state = 'error';
        this.errorMessage = err?.error?.message || err?.message || this.translate.instant('iot-hub.update-error', { name: this.data.itemName });
      }
    });
  }

  openEntityDetails(): void {
    if (this.entityDetailsUrl) {
      this.dialogRef.close('updated');
      void this.router.navigateByUrl(this.entityDetailsUrl);
    }
  }

  close(): void {
    this.dialogRef.close(this.state === 'success' ? 'updated' : false);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

}
