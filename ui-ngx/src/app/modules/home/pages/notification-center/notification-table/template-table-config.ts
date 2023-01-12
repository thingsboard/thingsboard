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
import { NotificationTemplate } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import {
  TemplateNotificationDialogComponent,
  TemplateNotificationDialogData
} from '@home/pages/notification-center/template-dialog/template-notification-dialog.component';
import { TranslateService } from '@ngx-translate/core';
import {
  TemplateTableHeaderComponent
} from '@home/pages/notification-center/template-dialog/template-table-header.component';

export class TemplateTableConfig extends EntityTableConfig<NotificationTemplate> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
    this.loadDataOnInit = false;
    this.entityTranslations = {
      noEntities: 'notification.no-notification-templates',
      search: 'notification.search-templates'
    };
    this.entityResources = {} as EntityTypeResource<NotificationTemplate>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationTemplates(pageLink);

    this.deleteEntityTitle = template => this.translate.instant('notification.delete-template-title', {templateName: template.name});
    this.deleteEntityContent = () => this.translate.instant('notification.delete-template-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationTemplate(id.id);

    this.cellActionDescriptors = this.configureCellActions();

    this.headerComponent = TemplateTableHeaderComponent;
    this.onEntityAction = action => this.onTemplateAction(action);

    this.defaultSortOrder = {property: 'notificationType', direction: Direction.ASC};

    this.columns.push(
      new EntityTableColumn<NotificationTemplate>('notificationType', 'notification.type', '15%'),
      new EntityTableColumn<NotificationTemplate>('name', 'notification.template', '25%'),
      new EntityTableColumn<NotificationTemplate>('configuration.notificationSubject', 'notification.subject', '25%',
        (template) => template.configuration.notificationSubject, () => ({}), false),
      new EntityTableColumn<NotificationTemplate>('configuration.defaultTextTemplate', 'notification.message', '35%',
        (template) => template.configuration.defaultTextTemplate, () => ({}), false)
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationTemplate>> {
    return [];
  }

  private editTemplate($event: Event, template: NotificationTemplate, isAdd = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<TemplateNotificationDialogComponent, TemplateNotificationDialogData,
      NotificationTemplate>(TemplateNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        template
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.updateData();
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
