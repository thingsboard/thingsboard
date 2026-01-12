///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { NotificationRule, TriggerTypeTranslationMap } from '@shared/models/notification.models';
import { NotificationService } from '@core/http/notification.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import {
  RuleNotificationDialogComponent,
  RuleNotificationDialogData
} from '@home/pages/notification/rule/rule-notification-dialog.component';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Injectable } from '@angular/core';
import { DatePipe } from '@angular/common';
import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { Observable } from 'rxjs';

@Injectable()
export class RuleTableConfigResolver  {

  private readonly config: EntityTableConfig<NotificationRule> = new EntityTableConfig<NotificationRule>();

  constructor(private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private customTranslate: CustomTranslatePipe) {

    this.config.entityType = EntityType.NOTIFICATION_RULE;
    this.config.detailsPanelEnabled = false;
    this.config.addAsTextButton = true;
    this.config.rowPointer = true;

    this.config.entityTranslations = entityTypeTranslations.get(EntityType.NOTIFICATION_RULE);
    this.config.entityResources = {} as EntityTypeResource<NotificationRule>;

    this.config.addEntity = () => this.notificationRuleDialog(null, true);

    this.config.entitiesFetchFunction = pageLink => this.notificationService.getNotificationRules(pageLink);

    this.config.deleteEntityTitle = rule => this.translate.instant('notification.delete-rule-title', {ruleName: rule.name});
    this.config.deleteEntityContent = () => this.translate.instant('notification.delete-rule-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('notification.delete-rules-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('notification.delete-rules-text');
    this.config.deleteEntity = id => this.notificationService.deleteNotificationRule(id.id);

    this.config.cellActionDescriptors = this.configureCellActions();

    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.handleRowClick = ($event, rule) => {
      this.editRule($event, rule);
      return true;
    };

    this.config.columns.push(
      new DateEntityTableColumn<NotificationRule>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<NotificationRule>('name', 'notification.rule-name', '30%'),
      new EntityTableColumn<NotificationRule>('templateName', 'notification.template', '20%'),
      new EntityTableColumn<NotificationRule>('triggerType', 'notification.trigger.trigger', '20%',
        (rule) => this.translate.instant(TriggerTypeTranslationMap.get(rule.triggerType)) || '',
        () => ({}), true),
      new EntityTableColumn<NotificationRule>('additionalConfig.description', 'notification.description', '30%',
        (target) => this.customTranslate.transform(target.additionalConfig?.description || ''),
        () => ({}), false)
    );
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<NotificationRule> {
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRule>> {
    return [{
      name: '',
      nameFunction: (entity) =>
        this.translate.instant(entity.enabled ? 'notification.rule-disable' : 'notification.rule-enable'),
      icon: 'mdi:toggle-switch',
      isEnabled: () => true,
      iconFunction: (entity) => entity.enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
      onAction: ($event, entity) => this.toggleEnableMode($event, entity)
    },
    {
      name: this.translate.instant('notification.copy-rule'),
      icon: 'content_copy',
      isEnabled: () => true,
      onAction: ($event, entity) => this.editRule($event, entity, true)
    }];
  }

  private editRule($event: Event, rule: NotificationRule, isCopy = false): void{
    $event?.stopPropagation();
    this.notificationRuleDialog(rule, false, isCopy).subscribe(res => res ? this.config.updateData() : null);
  }

  private notificationRuleDialog(rule: NotificationRule, isAdd = false, isCopy = false): Observable<NotificationRule> {
    return this.dialog.open<RuleNotificationDialogComponent, RuleNotificationDialogData,
      NotificationRule>(RuleNotificationDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        isCopy,
        rule
      }
    }).afterClosed();
  }

  private toggleEnableMode($event: Event, rule: NotificationRule): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifyRule: NotificationRule = {
      ...rule,
      enabled: !rule.enabled
    };

    this.notificationService.saveNotificationRule(modifyRule, {ignoreLoading: true})
      .subscribe((notificationRule) => {
        rule.enabled = notificationRule.enabled;
        this.config.getTable().detectChanges();
      });
  }
}
