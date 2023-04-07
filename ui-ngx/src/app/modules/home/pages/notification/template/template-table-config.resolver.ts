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
import { NotificationTemplate, NotificationTemplateTypeTranslateMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification/template/template-notification-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import { TemplateTableHeaderComponent } from '@home/pages/notification/template/template-table-header.component';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { DatePipe } from '@angular/common';

@Injectable()
export class TemplateTableConfigResolver implements Resolve<EntityTableConfig<NotificationTemplate>> {

  private readonly config: EntityTableConfig<NotificationTemplate> = new EntityTableConfig<NotificationTemplate>();

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe) {

    this.config.entityType = EntityType.NOTIFICATION_TEMPLATE;
    this.config.detailsPanelEnabled = false;
    this.config.addEnabled = false;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_TEMPLATE);
    this.config.entityResources = {} as EntityTypeResource<NotificationTemplate>;

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTemplates(pageLink);

    this.config.deleteEntityTitle = template => this.translate.instant('notification.delete-template-title', {templateName: template.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-template-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-templates-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-templates-text');
    this.config.deleteEntity = id => this.notificationService.deleteNotificationTemplate(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.headerComponent = TemplateTableHeaderComponent;
    this.config.onEntityAction = action => this.onTemplateAction(action);

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
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<NotificationTemplate> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationTemplate>> {
    return [
      {
        name: this.translate.instant('notification.copy-template'),
        icon: 'content_copy',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editTemplate($event, entity, false, true)
      }
    ];
  }

  private editTemplate($event: Event, template: NotificationTemplate, isAdd = false, isCopy = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData,
      NotificationTemplate>(TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isCopy,
        template
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.config.updateData();
        }
      });
  }

  private onTemplateAction(action: EntityAction<NotificationTemplate>): boolean {
    switch (action.action) {
      case 'add':
        this.editTemplate(action.event, action.entity, true);
        return true;
    }
    return false;
  }

}
