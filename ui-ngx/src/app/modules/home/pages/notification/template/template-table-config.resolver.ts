// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  CellActionDescriptor,
  DateEntityTableColumn,
  defaultEntityTablePermissions,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, EntityTypeResource, entityTypeTranslations } from '@shared/models/entity-type.models';
import { Direction } from '@shared/models/page/sort-order';
import {
  notificationAiAssistantConfig,
  NotificationTemplate,
  NotificationTemplateTypeTranslateMap,
  singleNotificationTypeTemplate
} from '@shared/models/notification.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { NotificationService } from '@core/http/notification.service';
import { MatDialog } from '@angular/material/dialog';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification/template/template-notification-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot } from '@angular/router';
import { DatePipe } from '@angular/common';
import { Observable } from 'rxjs';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Authority } from '@shared/models/authority.enum';

@Injectable()
export class TemplateTableConfigResolver  {

  private readonly config: EntityTableConfig<NotificationTemplate> = new EntityTableConfig<NotificationTemplate>();

  constructor(private store: Store<AppState>,
              private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.NOTIFICATION_TEMPLATE;
    this.config.detailsPanelEnabled = false;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_TEMPLATE);
    this.config.entityResources = {} as EntityTypeResource<NotificationTemplate>;

    this.config.addEntity = () => this.notificationTemplateDialog(null, true);

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTemplates(pageLink);

    this.config.deleteEntityTitle = template => this.translate.instant('notification.delete-template-title', {templateName: template.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-template-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-templates-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-templates-text');
    this.config.deleteEntity = id => this.notificationService.deleteNotificationTemplate(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.entitySelectionEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.handleRowClick = ($event, template) => {
      this.editTemplate($event, template);
      return true;
    };

    this.config.columns.push(
      new DateEntityTableColumn<NotificationTemplate>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationTemplate>('notificationType', 'notification.type', '20%',
        (template) => this.translate.instant(NotificationTemplateTypeTranslateMap.get(template.notificationType).name)),
      new EntityTableColumn<NotificationTemplate>('name', 'notification.template', '80%')
    );

    this.config.aiAssistantConfig = notificationAiAssistantConfig(
      this.store, this.userPermissionsService, this.translate, AiAssistantViewType.NOTIFICATION_TEMPLATE_LIST);
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<NotificationTemplate> {
    const authority = getCurrentAuthUser(this.store).authority;
    if (authority === Authority.SYS_ADMIN) {
      this.config.deleteEnabled = (template) =>
        this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE) &&
          !singleNotificationTypeTemplate(template.notificationType);
    } else {
      defaultEntityTablePermissions(this.userPermissionsService, this.config);
    }
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationTemplate>> {
    return [
      {
        name: this.translate.instant('notification.copy-template'),
        icon: 'content_copy',
        isEnabled: (template) => {
          return this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE)
            && !singleNotificationTypeTemplate(template.notificationType)
        },
        onAction: ($event, entity) => this.editTemplate($event, entity, true)
      }
    ];
  }

  private editTemplate($event: Event, template: NotificationTemplate, isCopy = false) {
    $event?.stopPropagation();
    this.notificationTemplateDialog(template, false, isCopy).subscribe((res) => res ? this.config.updateData() : null);
  }

  private notificationTemplateDialog(template: NotificationTemplate, isAdd = false, isCopy = false): Observable<NotificationTemplate> {
    return this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData,
      NotificationTemplate>(TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isCopy,
        template,
        readonly: !this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE)
      }
    }).afterClosed();
  }
}
