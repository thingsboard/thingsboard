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
import { MpItemVersionView } from '@shared/models/iot-hub/iot-hub-version.models';
import { ItemType, itemTypeTranslations } from '@shared/models/iot-hub/iot-hub-item.models';
import { IotHubInstalledItemDescriptor } from '@shared/models/iot-hub/iot-hub-installed-item.models';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';

export interface IotHubInstallDialogData {
  item: MpItemVersionView;
  iotHubApiService: IotHubApiService;
}

export type InstallState = 'confirm' | 'installing' | 'success' | 'error';

@Component({
  selector: 'tb-iot-hub-install-dialog',
  standalone: false,
  template: `
    @switch (state) {
      @case ('confirm') {
        <h2 mat-dialog-title>{{ 'iot-hub.install-item-title' | translate }}</h2>
        <mat-dialog-content>
          <p>{{ 'iot-hub.install-confirm' | translate:{ name: item.name, version: item.version } }}</p>
          <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-type' | translate:{ type: getTypeLabel() } }}</p>
          <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-creator' | translate:{ creator: item.creatorDisplayName } }}</p>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button (click)="cancel()">{{ 'action.cancel' | translate }}</button>
          <button mat-raised-button color="primary" (click)="install()">{{ 'iot-hub.install' | translate }}</button>
        </mat-dialog-actions>
      }
      @case ('installing') {
        <h2 mat-dialog-title>{{ 'iot-hub.install-item-title' | translate }}</h2>
        <mat-dialog-content>
          <p>{{ 'iot-hub.install-confirm' | translate:{ name: item.name, version: item.version } }}</p>
          <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-type' | translate:{ type: getTypeLabel() } }}</p>
          <p class="tb-iot-hub-install-meta">{{ 'iot-hub.install-creator' | translate:{ creator: item.creatorDisplayName } }}</p>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button disabled>{{ 'action.cancel' | translate }}</button>
          <button mat-raised-button color="primary" disabled>
            <mat-spinner diameter="18" class="tb-iot-hub-inline-spinner"></mat-spinner>
            {{ 'iot-hub.installing' | translate }}
          </button>
        </mat-dialog-actions>
      }
      @case ('success') {
        <h2 mat-dialog-title>
          <mat-icon class="tb-iot-hub-result-icon tb-iot-hub-success-icon">check_circle</mat-icon>
          {{ 'iot-hub.install-success-title' | translate }}
        </h2>
        <mat-dialog-content>
          <p>{{ 'iot-hub.install-success-message' | translate:{ name: item.name, version: item.version } }}</p>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button (click)="close()">{{ 'action.close' | translate }}</button>
          @if (entityDetailsUrl) {
            <button mat-raised-button color="primary" (click)="openEntityDetails()">
              {{ 'iot-hub.open-item-type-details' | translate:{ type: getTypeLabel() } }}
            </button>
          }
        </mat-dialog-actions>
      }
      @case ('error') {
        <h2 mat-dialog-title>
          <mat-icon class="tb-iot-hub-result-icon tb-iot-hub-error-icon">error</mat-icon>
          {{ 'iot-hub.install-error-title' | translate }}
        </h2>
        <mat-dialog-content>
          <p>{{ 'iot-hub.install-error-message' | translate:{ name: item.name } }}</p>
          <div class="tb-iot-hub-error-details">{{ errorMessage }}</div>
        </mat-dialog-content>
        <mat-dialog-actions align="end">
          <button mat-button (click)="close()">{{ 'action.close' | translate }}</button>
        </mat-dialog-actions>
      }
    }
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
    .tb-iot-hub-result-icon {
      vertical-align: middle;
      margin-right: 8px;
    }
    .tb-iot-hub-success-icon {
      color: #2e7d32;
    }
    .tb-iot-hub-error-icon {
      color: #c62828;
    }
    .tb-iot-hub-error-details {
      max-height: 200px;
      overflow-y: auto;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 4px;
      font-family: monospace;
      font-size: 12px;
      color: rgba(0, 0, 0, 0.7);
      white-space: pre-wrap;
      word-break: break-word;
      margin-top: 8px;
    }
  `]
})
export class TbIotHubInstallDialogComponent {

  private static readonly ITEM_TYPE_TO_ENTITY_TYPE: Record<string, EntityType> = {
    'WIDGET': EntityType.WIDGET_TYPE,
    'DASHBOARD': EntityType.DASHBOARD,
    'CALCULATED_FIELD': EntityType.CALCULATED_FIELD,
    'RULE_CHAIN': EntityType.RULE_CHAIN,
    'DEVICE': EntityType.DEVICE_PROFILE
  };

  item: MpItemVersionView;
  typeTranslations = itemTypeTranslations;
  state: InstallState = 'confirm';
  errorMessage = '';
  entityDetailsUrl: string | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IotHubInstallDialogData,
    private dialogRef: MatDialogRef<TbIotHubInstallDialogComponent>,
    private router: Router,
    private translate: TranslateService
  ) {
    this.item = data.item;
  }

  getTypeLabel(): string {
    const key = this.typeTranslations.get(this.item.type);
    return key ? this.translate.instant(key) : '';
  }

  install(): void {
    this.state = 'installing';
    const versionId = this.item.id as string;
    this.data.iotHubApiService.installItemVersion(versionId, { ignoreLoading: true }).subscribe({
      next: (result) => {
        if (result.success) {
          this.state = 'success';
          this.entityDetailsUrl = this.resolveEntityDetailsUrl(result.descriptor);
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
      this.router.navigateByUrl(this.entityDetailsUrl);
    }
  }

  close(): void {
    this.dialogRef.close(this.state === 'success' ? 'installed' : false);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  private resolveEntityDetailsUrl(descriptor: IotHubInstalledItemDescriptor): string | null {
    if (!descriptor) {
      return null;
    }
    const entityType = TbIotHubInstallDialogComponent.ITEM_TYPE_TO_ENTITY_TYPE[this.item.type];
    if (!entityType) {
      return null;
    }
    let entityId: string | null = null;
    switch (descriptor.type) {
      case 'WIDGET': entityId = descriptor.widgetTypeId?.id; break;
      case 'DASHBOARD': entityId = descriptor.dashboardId?.id; break;
      case 'CALCULATED_FIELD': entityId = descriptor.calculatedFieldId?.id; break;
      case 'RULE_CHAIN': entityId = descriptor.ruleChainId?.id; break;
    }
    if (!entityId) {
      return null;
    }
    return getEntityDetailsPageURL(entityId, entityType) || null;
  }
}
