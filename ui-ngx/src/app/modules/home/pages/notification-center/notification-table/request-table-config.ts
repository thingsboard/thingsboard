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
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  NotificationRequest,
  NotificationRequestStatus,
  NotificationRequestStatusTranslateMap,
  NotificationTemplate
} from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityTypeResource } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  RequestNotificationDialogComponent,
  RequestNotificationDialogData
} from '@home/pages/notification-center/request-table/request-notification-dialog.componet';

export class RequestTableConfig extends EntityTableConfig<NotificationRequest> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {
    super();
    this.loadDataOnInit = false;
    this.searchEnabled = false;
    this.entityTranslations = {
      noEntities: 'notification.no-notification-request',
    };
    this.entityResources = {} as EntityTypeResource<NotificationRequest>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRequests(pageLink);

    this.deleteEnabled = (request) => request.status === NotificationRequestStatus.SCHEDULED;
    this.deleteEntityTitle = () => this.translate.instant('notification.delete-request-title');
    this.deleteEntityContent = () => this.translate.instant('notification.delete-request-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationRequest(id.id);

    this.cellActionDescriptors = this.configureCellActions();

    this.onEntityAction = action => this.onRequestAction(action);

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(
      new DateEntityTableColumn<NotificationRequest>('createdTime', 'notification.created-time', this.datePipe, '150px'),
      new EntityTableColumn<NotificationRequest>('status', 'notification.status', '100%',
        request => this.requestStatus(request.status), request => this.requestStatusStyle(request.status)),
      // new EntityTableColumn<NotificationRequest>('deliveryMethods', 'notification.delivery-method', '20%',
      //   (request) => request.deliveryMethods.toString(),
      //   () => ({}), false)
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRequest>> {
    return [];
  }

  private createRequest($event: Event, request: NotificationRequest, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RequestNotificationDialogComponent, RequestNotificationDialogData,
      NotificationTemplate>(RequestNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        request
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private onRequestAction(action: EntityAction<NotificationRequest>): boolean {
    switch (action.action) {
      case 'add':
        this.createRequest(action.event, action.entity, true);
        return true;
    }
    return false;
  }

  private requestStatus(status: NotificationRequestStatus): string {
    const translateKey = NotificationRequestStatusTranslateMap.get(status);
    let backgroundColor = 'rgba(25, 128, 56, 0.08)';
    switch (status) {
      case NotificationRequestStatus.SCHEDULED:
        backgroundColor = 'rgba(48, 86, 128, 0.08)';
        break;
      case NotificationRequestStatus.PROCESSING:
        backgroundColor = 'rgba(212, 125, 24, 0.08)';
        break;
    }
    return `<div style="border-radius: 16px; height: 32px; line-height: 32px; padding: 0 12px; width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private requestStatusStyle(status: NotificationRequestStatus): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038',
      cursor: 'pointer'
    };
    switch (status) {
      case NotificationRequestStatus.SCHEDULED:
        styleObj.color = '#305680';
        break;
      case NotificationRequestStatus.PROCESSING:
        styleObj.color = '#D47D18';
        break;
    }
    return styleObj;
  }
}
