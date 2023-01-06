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
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { DatePipe } from '@angular/common';
import { Direction } from '@shared/models/page/sort-order';
import { Notification } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';

export class InboxTableConfig extends EntityTableConfig<Notification> {

  constructor(private notificationService: NotificationService,
              private datePipe: DatePipe,
              updateOnInit = true) {
    super();
    this.loadDataOnInit = true;
    this.entitiesDeleteEnabled = false;
    this.entityTranslations = {
      noEntities: 'notification.no-inbox-notification'
    };
    this.entityResources = {} as EntityTypeResource<Notification>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotifications(pageLink);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<Notification>('createdTime', 'notification.created-time', this.datePipe, '150px'),
      new EntityTableColumn<Notification>('type', 'notification.type', '10%'),
      new EntityTableColumn<Notification>('text', 'notification.text', '40%'),
      new EntityTableColumn<Notification>('info.description', 'notification.description', '50%')
    );

  }

}
