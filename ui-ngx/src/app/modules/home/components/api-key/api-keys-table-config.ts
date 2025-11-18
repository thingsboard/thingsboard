///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig,
  CellActionDescriptor
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Injectable, Renderer2, ViewContainerRef } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Observable } from 'rxjs';
import { ApiKeyInfo, ApiKey } from '@shared/models/api-key.models';
import { ApiKeyService } from '@core/http/api-key.service';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbPopoverService } from '@shared/components/popover.service';
import { map } from 'rxjs/operators';
import { UserId } from '@shared/models/id/user-id';
import { AddApiKeyDialogComponent } from '@home/components/api-key/add-api-key-dialog.component';
import { EditApiKeyDescriptionPanelComponent } from '@home/components/api-key/edit-api-key-description-panel.component';
import { ApiKeysTableDialogData } from '@home/components/api-key/api-keys-table-dialog.component';
import {
  ApiKeyGeneratedDialogComponent,
  ApiKeyGeneratedDialogData
} from '@home/components/api-key/api-key-generated-dialog.component';

@Injectable()
export class ApiKeysTableConfig extends EntityTableConfig<ApiKeyInfo> {

  constructor(
    private apiKeyService: ApiKeyService,
    private translate: TranslateService,
    private customTranslate: CustomTranslatePipe,
    private dialog: MatDialog,
    private datePipe: DatePipe,
    private popoverService: TbPopoverService,
    private renderer: Renderer2,
    private viewContainerRef: ViewContainerRef,
    private userId: UserId,
  ) {
    super();

    this.entityType = EntityType.API_KEY;
    this.detailsPanelEnabled = false;
    this.addAsTextButton = true;
    this.pageMode = false;

    this.entityTranslations = entityTypeTranslations.get(EntityType.API_KEY);
    this.entityResources = {} as EntityTypeResource<ApiKeyInfo>;
    this.tableTitle = this.translate.instant('api-key.api-keys');
    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.entitiesFetchFunction = pageLink => this.apiKeyService.getUserApiKeys(this.userId.id, pageLink);
    this.addEntity = () => this.addApiKey();

    this.deleteEntityTitle = entity => this.translate.instant('api-key.delete-api-key-title', {name: entity.description});
    this.deleteEntityContent = () => this.translate.instant('api-key.delete-api-key-text');
    this.deleteEntitiesTitle = count => this.translate.instant('api-key.delete-api-keys-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('api-key.delete-api-keys-text');
    this.deleteEntity = id => this.apiKeyService.deleteApiKey(id.id);

    this.cellActionDescriptors = this.configureCellActions();
    this.columns.push(
      new DateEntityTableColumn<ApiKeyInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<ApiKeyInfo>('description', 'api-key.description', '100%',
        (entity) => this.customTranslate.transform(entity?.description), () => ({}), true, () => ({}),
        (entity) => entity?.description.length > 80 ? this.customTranslate.transform(entity.description) : undefined, false,
        {
          name: this.translate.instant('api-key.edit-description'),
          icon: 'edit',
          isEnabled: () => true,
          onAction: ($event, entity) => this.updateApiKeyDescription($event, entity)
        }),
      new EntityTableColumn<ApiKeyInfo>('active', 'api-key.status', '80px',
        entity => this.apiKeyStatus(entity), entity => this.apiKeyStatusStyle(entity), false),
      new EntityTableColumn<ApiKeyInfo>('expirationTime', 'api-key.expiration-time', '120px',
        (entity) => entity.expirationTime != 0 ?
          this.datePipe.transform(entity.expirationTime, 'dd/MM/yyyy, HH:mm') :
          this.translate.instant('api-key.expiration-time-never'),
        ),
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<ApiKeyInfo>> {
    const actions: Array<CellActionDescriptor<ApiKeyInfo>> = [];
    actions.push(
      {
        name: '',
        nameFunction: (entity) => this.translate.instant(entity.enabled ? 'api-key.disable' : 'api-key.enable'),
        icon: 'mdi:toggle-switch',
        isEnabled: (entity) => !entity.expired,
        iconFunction: (entity) => entity.enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
        onAction: ($event, entity) => this.toggleEnableMode($event, entity)
      }
    )
    return actions;
  }

  private addApiKey(): Observable<ApiKey> {
    return this.dialog.open<AddApiKeyDialogComponent, ApiKeysTableDialogData, ApiKey>(AddApiKeyDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        userId: this.userId
      }
    }).afterClosed().pipe(map(res => {
      if (res) {
        this.apiKeyGenerated(res);
      } else {
        return null;
      }
    }));
  }

  private apiKeyGenerated(apiKey: ApiKey) {
    this.dialog.open<ApiKeyGeneratedDialogComponent, ApiKeyGeneratedDialogData>(ApiKeyGeneratedDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        apiKey
      }
    }).afterClosed()
      .subscribe(() => {
        this.updateData();
      });
  }

  private toggleEnableMode($event: Event, entity: ApiKeyInfo): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.apiKeyService.enableApiKey(entity.id.id, !entity.enabled, {ignoreLoading: true})
      .subscribe(
        () => this.updateData()
      );
  }

  private apiKeyStatus(apiKey: ApiKeyInfo): string {
    let translateKey = 'api-key.status-active';
    let backgroundColor = 'rgba(25, 128, 56, 0.08)';
    if (apiKey.expired) {
      translateKey = 'api-key.status-expired';
      backgroundColor = 'rgba(0, 0, 0, 0.04)';
    } else if (!apiKey.enabled) {
      translateKey = 'api-key.status-inactive';
      backgroundColor = 'rgba(209, 39, 48, 0.08)';
    }
    return `<div class="status" style="border-radius: 16px; height: 32px;
                line-height: 32px; padding: 0 12px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private apiKeyStatusStyle(apiKey: ApiKeyInfo): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038',
      cursor: 'pointer'
    };
    if (apiKey.expired) {
      styleObj.color = 'rgba(0, 0, 0, 0.54)';
    } else if (!apiKey.enabled) {
      styleObj.color = '#d12730';
    }
    return styleObj;
  }

  private updateApiKeyDescription($event: Event, entity: ApiKeyInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = ($event.target || $event.srcElement || $event.currentTarget) as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const editSecretDescriptionPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: EditApiKeyDescriptionPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['right', 'bottom', 'top'],
        context: {
          apiKeyId: entity.id.id,
          description: entity.description
        },
        isModal: true
      });
      editSecretDescriptionPanelPopover.tbComponentRef.instance.descriptionApplied.subscribe(() => {
        editSecretDescriptionPanelPopover.hide();
        this.updateData();
      });
    }
  }
}
