///
/// Copyright © 2016-2023 The Thingsboard Authors
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
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { NotificationTarget, NotificationTargetTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import {
  RecipientNotificationDialogComponent,
  RecipientNotificationDialogData
} from '@home/pages/notification/recipient/recipient-notification-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RecipientTableHeaderComponent } from '@home/pages/notification/recipient/recipient-table-header.component';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { Injectable } from '@angular/core';
import { DatePipe } from '@angular/common';

@Injectable()
export class RecipientTableConfigResolver implements Resolve<EntityTableConfig<NotificationTarget>> {

  private readonly config: EntityTableConfig<NotificationTarget> = new EntityTableConfig<NotificationTarget>();

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.NOTIFICATION_TARGET;
    this.config.detailsPanelEnabled = false;
    this.config.addEnabled = false;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_TARGET);
    this.config.entityResources = {} as EntityTypeResource<NotificationTarget>;

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTargets(pageLink);

    this.config.deleteEntityTitle = target => this.translate.instant('notification.delete-recipient-title', {recipientName: target.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-recipient-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-recipients-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-recipients-text');

    this.config.deleteEntity = id => this.notificationService.deleteNotificationTarget(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.headerComponent = RecipientTableHeaderComponent;
    this.config.onEntityAction = action => this.onTargetAction(action);

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.handleRowClick = ($event, target) => {
      this.editTarget($event, target);
      return true;
    };

    this.config.columns.push(
      new DateEntityTableColumn<NotificationTarget>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationTarget>('name', 'notification.recipient-group', '20%'),
      new EntityTableColumn<NotificationTarget>('configuration.type', 'notification.type', '20%',
        (target) => this.translate.instant(NotificationTargetTypeTranslationMap.get(target.configuration.type)),
        () => ({}), false),
      new EntityTableColumn<NotificationTarget>('configuration.description', 'notification.description', '60%',
      (target) => target.configuration.description || '',
      () => ({}), false)
    );
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<NotificationTarget> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationTarget>> {
    return [];
  }

  private editTarget($event: Event, target: NotificationTarget, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RecipientNotificationDialogComponent, RecipientNotificationDialogData,
      NotificationTarget>(RecipientNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        target
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  private onTargetAction(action: EntityAction<NotificationTarget>): boolean {
    switch (action.action) {
      case 'add':
        this.editTarget(action.event, action.entity, true);
        return true;
    }
    return false;
  }
}
