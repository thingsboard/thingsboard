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
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import {
  Notification,
  NotificationStatus,
  NotificationTemplateTypeTranslateMap
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { InboxTableHeaderComponent } from '@home/pages/notification-center/inbox-table/inbox-table-header.component';
import { TranslateService } from '@ngx-translate/core';
import { take } from 'rxjs/operators';

export class InboxTableConfig extends EntityTableConfig<Notification> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private datePipe: DatePipe) {
    super();
    this.entitiesDeleteEnabled = false;
    this.entityTranslations = {
      noEntities: 'notification.no-inbox-notification',
      search: 'notification.search-notification'
    };
    this.entityResources = {} as EntityTypeResource<Notification>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotifications(pageLink, this.componentsData.unreadOnly);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.componentsData = {
      unreadOnly: true
    };

    this.cellActionDescriptors = this.configureCellActions();

    this.headerComponent = InboxTableHeaderComponent;

    this.headerActionDescriptors = [{
      name: this.translate.instant('notification.mark-all-as-read'),
      icon: 'done_all',
      isEnabled: () => true,
      onAction: $event => this.markAllRead($event)
    }];

    this.columns.push(
      new DateEntityTableColumn<Notification>('createdTime', 'notification.created-time', this.datePipe, '170px'),
      new EntityTableColumn<Notification>('type', 'notification.type', '10%', (notification) =>
        this.translate.instant(NotificationTemplateTypeTranslateMap.get(notification.type).name)),
      new EntityTableColumn<Notification>('subject', 'notification.subject', '30%'),
      new EntityTableColumn<Notification>('text', 'notification.message', '60%')
    );

  }

  private configureCellActions(): Array<CellActionDescriptor<Notification>> {
    return [{
      name: this.translate.instant('notification.mark-as-read'),
      icon: 'check_circle_outline',
      isEnabled: (notification) => notification.status !== NotificationStatus.READ,
      onAction: ($event, entity) => this.markAsRead($event, entity)
    }];
  }

  private markAllRead($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.markAllNotificationsAsRead().subscribe(() => {
      if (this.componentsData.unreadOnly) {
        this.getTable().resetSortAndFilter(true);
      } else {
        this.updateData();
      }
    });
  }

  private markAsRead($event, entity){
    if ($event) {
      $event.stopPropagation();
    }
    this.notificationService.markNotificationAsRead(entity.id.id).subscribe(() => {
      if (this.componentsData.unreadOnly) {
        this.getTable().dataSource.pageData$.pipe(take(1)).subscribe(
          (value) => {
            if (value.data.length === 1 && this.getTable().pageLink.page) {
              this.getTable().pageLink.page--;
            }
            this.updateData();
          }
        );
      } else {
        entity.status = NotificationStatus.READ;
        this.getTable().detectChanges();
      }
    });
  }
}
