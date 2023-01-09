///
/// Copyright © 2016-2022 The Thingsboard Authors
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
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { NotificationTarget, NotificationTargetConfigTypeTranslateMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import {
  TargetNotificationDialogComponent,
  TargetsNotificationDialogData
} from '@home/pages/notification-center/targets-table/target-notification-dialog.componet';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';

export class TargetsTableConfig extends EntityTableConfig<NotificationTarget> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
    this.loadDataOnInit = false;
    this.entityTranslations = {
      noEntities: 'notification.no-targets-notification'
    };
    this.entityResources = {} as EntityTypeResource<NotificationTarget>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTargets(pageLink);

    this.deleteEntityTitle = target => this.translate.instant('notification.delete-target-title', {targetName: target.name});
    this.deleteEntityContent = () => this.translate.instant('notification.delete-target-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationTarget(id.id);

    this.cellActionDescriptors = this.configureCellActions();
    this.onEntityAction = action => this.onTargetAction(action);

    this.defaultSortOrder = {property: 'name', direction: Direction.ASC};

    this.columns.push(
      new EntityTableColumn<NotificationTarget>('name', 'notification.notification-target', '30%'),
      new EntityTableColumn<NotificationTarget>('configuration.type', 'notification.type', '40%',
        (target) => this.translate.instant(NotificationTargetConfigTypeTranslateMap.get(target.configuration.type)),
        () => ({}), false)
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationTarget>> {
    return [{
      name: this.translate.instant('device.make-public'),
      icon: 'edit',
      isEnabled: () => true,
      onAction: ($event, entity) => this.editTarget($event, entity)
    }];
  }

  private editTarget($event: Event, target: NotificationTarget, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<TargetNotificationDialogComponent, TargetsNotificationDialogData,
      NotificationTarget>(TargetNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        target
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.updateData();
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
