// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import {
  notificationAiAssistantConfig,
  NotificationDeliveryMethodInfoMap,
  NotificationRequest,
  NotificationRequestInfo,
  NotificationRequestStats,
  NotificationRequestStatus,
  NotificationRequestStatusTranslateMap,
  NotificationTemplate
} from '@shared/models/notification.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import { DatePipe } from '@angular/common';
import { EntityAction } from '@home/models/entity/entity-component.models';
import {
  RequestNotificationDialogData,
  SentNotificationDialogComponent
} from '@home/pages/notification/sent/sent-notification-dialog.componet';
import { PageLink } from '@shared/models/page/page-link';
import {
  NotificationRequestErrorDialogData,
  SentErrorDialogComponent
} from '@home/pages/notification/sent/sent-error-dialog.component';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class SentTableConfigResolver  {

  private readonly config: EntityTableConfig<NotificationRequest, PageLink, NotificationRequestInfo> =
    new EntityTableConfig<NotificationRequest, PageLink, NotificationRequestInfo>();

  constructor(private store: Store<AppState>,
              private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.NOTIFICATION_REQUEST;
    this.config.detailsPanelEnabled = false;
    this.config.addEnabled = false;
    this.config.searchEnabled = false;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_REQUEST);
    this.config.entityResources = {} as EntityTypeResource<NotificationRequest>;

    this.config.deleteEntityTitle = () => this.translate.instant('notification.delete-request-title');
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-request-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-requests-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-requests-text');

    this.config.deleteEntity = id => this.notificationService.deleteNotificationRequest(id.id);
    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRequests(pageLink);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.onEntityAction = action => this.onRequestAction(action);

    this.config.deleteEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);
    this.config.entitySelectionEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

    this.config.handleRowClick = (event, entity) => {
      if ((event.target as HTMLElement).getElementsByClassName('stats').length || (event.target as HTMLElement).className === 'stats') {
        this.openStatsErrorDialog(event, entity);
      }
      return true;
    };

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<NotificationRequestInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationRequestInfo>('status', 'notification.status', '15%',
        request => `<span style="display: flex;">${this.requestStatus(request.status)}${this.requestStats(request.stats)}</span>`,
          request => this.requestStatusStyle(request.status)),
      new EntityTableColumn<NotificationRequest>('deliveryMethods', 'notification.delivery-method.delivery-method', '15%',
        (request) => request.deliveryMethods
          .map((deliveryMethod) => this.translate.instant(NotificationDeliveryMethodInfoMap.get(deliveryMethod).name)).join(', '),
        () => ({}), false),
      new EntityTableColumn<NotificationRequest>('templateName', 'notification.template', '70%')
    );

    this.config.aiAssistantConfig = notificationAiAssistantConfig(
      this.store, this.userPermissionsService, this.translate, AiAssistantViewType.SENT_NOTIFICATION_LIST);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<NotificationRequest, PageLink, NotificationRequestInfo> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRequestInfo>> {
    return [{
      name: this.translate.instant('notification.notify-again'),
      icon: 'mdi:repeat-variant',
      isEnabled: (request) => request.status !== NotificationRequestStatus.SCHEDULED &&
        this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE),
      onAction: ($event, entity) => this.createRequest($event, entity)
    }];
  }

  private createRequest($event: Event, request: NotificationRequest, isAdd = false, updateData = true) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SentNotificationDialogComponent, RequestNotificationDialogData,
      NotificationTemplate>(SentNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        request
      }
    }).afterClosed().subscribe((res) => {
      if (res && updateData) {
        this.config.updateData();
      }
    });
  }

  private onRequestAction(action: EntityAction<NotificationRequest>): boolean {
    switch (action.action) {
      case 'add':
        this.createRequest(action.event, action.entity, true);
        return true;
      case 'add-without-update':
        this.createRequest(action.event, action.entity, true, false);
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
    return `<div style="border-radius: 12px; height: 24px; line-height: 24px; padding: 0 10px;
                        width: fit-content; background-color: ${backgroundColor}">
                ${this.translate.instant(translateKey)}
            </div>`;
  }

  private requestStats(stats: NotificationRequestStats): string {
    if (!stats?.errors) {
      return '';
    }
    const countError = stats.totalErrors;
    if (countError === 0) {
      return '';
    }
    return `<div style="border-radius: 12px; height: 24px; line-height: 24px; padding: 0 10px; width: max-content; cursor: pointer;
                        background-color: #D12730; color: #fff; font-weight: 500; margin-left: 8px" class="stats">
                ${this.translate.instant('notification.fails', {count: countError})} >
            </div>`;
  }

  private requestStatusStyle(status: NotificationRequestStatus): object {
    const styleObj = {
      fontSize: '14px',
      color: '#198038'
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

  private openStatsErrorDialog($event: Event, notificationRequest: NotificationRequest) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<SentErrorDialogComponent, NotificationRequestErrorDialogData,
      void>(SentErrorDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        notificationRequest
      }
    }).afterClosed().subscribe(() => {});
  }
}
