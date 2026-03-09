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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';

export interface IotHubInstallDialogData {
  item: MpItemVersionView;
  iotHubApiService: IotHubApiService;
}

export type InstallState = 'confirm' | 'installing' | 'success' | 'error';

@Component({
  selector: 'tb-iot-hub-install-dialog',
  standalone: false,
  template: `
    <h2 mat-dialog-title>{{ 'iot-hub.install-item-title' | translate }}</h2>
    <mat-dialog-content>
      <p>{{ 'iot-hub.install-confirm' | translate:{ name: item.name, version: item.version } }}</p>
      <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-type' | translate:{ type: (typeTranslations.get(item.type) | translate) } }}</p>
      <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-creator' | translate:{ creator: item.creatorDisplayName } }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()" [disabled]="state === 'installing'">
        {{ 'action.cancel' | translate }}
      </button>
      @switch (state) {
        @case ('confirm') {
          <button mat-raised-button color="primary" (click)="install()">
            {{ 'iot-hub.install' | translate }}
          </button>
        }
        @case ('installing') {
          <button mat-raised-button color="primary" disabled>
            <mat-spinner diameter="18" class="tb-iot-hub-inline-spinner"></mat-spinner>
            {{ 'iot-hub.installing' | translate }}
          </button>
        }
        @case ('success') {
          <button mat-raised-button color="primary" disabled>
            <mat-icon>check</mat-icon>
            {{ 'iot-hub.installed' | translate }}
          </button>
        }
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .tb-iot-hub-install-meta {
      margin: 4px 0;
      color: rgba(0, 0, 0, 0.54);
      font-size: 14px;
    }
    .tb-iot-hub-inline-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `]
})
export class TbIotHubInstallDialogComponent {

  item: MpItemVersionView;
  typeTranslations = itemTypeTranslations;
  state: InstallState = 'confirm';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IotHubInstallDialogData,
    private dialogRef: MatDialogRef<TbIotHubInstallDialogComponent>,
    private store: Store<AppState>,
    private translate: TranslateService
  ) {
    this.item = data.item;
  }

  install(): void {
    this.state = 'installing';
    const versionId = this.item.id as string;
    this.data.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }).subscribe({
      next: () => {
        this.state = 'success';
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('iot-hub.install-success', { name: this.item.name }),
          type: 'success',
          duration: 3000
        }));
        setTimeout(() => this.dialogRef.close(true), 1500);
      },
      error: () => {
        this.state = 'confirm';
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('iot-hub.install-error', { name: this.item.name }),
          type: 'error',
          duration: 5000
        }));
      }
    });
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
