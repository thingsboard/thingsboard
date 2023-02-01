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
import { NotificationRule, TriggerTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { RuleTableHeaderComponent } from '@home/pages/notification-center/rule-table/rule-table-header.component';
import {
  RuleNotificationDialogComponent,
  RuleNotificationDialogData
} from '@home/pages/notification-center/rule-table/rule-notification-dialog.component';

export class RuleTableConfig extends EntityTableConfig<NotificationRule> {

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog) {
    super();
    this.loadDataOnInit = false;
    this.entityTranslations = {
      noEntities: 'notification.no-rules-notification',
      search: 'notification.search-rules'
    };
    this.entityResources = {} as EntityTypeResource<NotificationRule>;

    this.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRules(pageLink);

    this.deleteEntityTitle = rule => this.translate.instant('notification.delete-rule-title', {ruleName: rule.name});
    this.deleteEntityContent = () => this.translate.instant('notification.delete-rule-text');
    this.deleteEntity = id => this.notificationService.deleteNotificationRule(id.id);

    this.cellActionDescriptors = this.configureCellActions();
    this.headerComponent = RuleTableHeaderComponent;
    this.onEntityAction = action => this.onTargetAction(action);

    this.defaultSortOrder = {property: 'name', direction: Direction.ASC};

    this.columns.push(
      new EntityTableColumn<NotificationRule>('name', 'notification.rule-name', '30%'),
      new EntityTableColumn<NotificationRule>('templateName', 'notification.template', '20%'),
      new EntityTableColumn<NotificationRule>('triggerType', 'notification.trigger.trigger', '20%',
        (rule) => this.translate.instant(TriggerTypeTranslationMap.get(rule.triggerType)) || '',
        () => ({}), true),
      new EntityTableColumn<NotificationRule>('additionalConfig.description', 'notification.description', '30%',
        (target) => target.additionalConfig.description || '',
        () => ({}), false)
    );
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRule>> {
    return [{
      name: this.translate.instant('notification.copy-rule'),
      icon: 'content_copy',
      isEnabled: () => true,
      onAction: ($event, entity) => this.editRule($event, entity, false, true)
    }, {
      name: this.translate.instant('notification.edit-rule'),
      icon: 'edit',
      isEnabled: () => true,
      onAction: ($event, entity) => this.editRule($event, entity)
    }];
  }

  private editRule($event: Event, rule: NotificationRule, isAdd = false, isCopy = false) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialog.open<RuleNotificationDialogComponent, RuleNotificationDialogData,
      NotificationRule>(RuleNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isCopy,
        rule
      }
    }).afterClosed()
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private onTargetAction(action: EntityAction<NotificationRule>): boolean {
    switch (action.action) {
      case 'add':
        this.editRule(action.event, action.entity, true);
        return true;
    }
    return false;
  }
}
