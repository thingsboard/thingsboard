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
  NotificationRule,
  TriggerTypeTranslationMap
} from '@shared/models/notification.models';
import { AiAssistantViewType } from '@shared/models/ai-chat.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
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
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';

@Injectable()
export class RuleTableConfigResolver  {

  private readonly config: EntityTableConfig<NotificationRule> = new EntityTableConfig<NotificationRule>();

  constructor(private store: Store<AppState>,
              private notificationService: NotificationService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private customTranslate: CustomTranslatePipe,
              private userPermissionsService: UserPermissionsService) {

    this.config.entityType = EntityType.NOTIFICATION_RULE;
    this.config.detailsPanelEnabled = false;
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

    this.config.entitySelectionEnabled = () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE);

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

    this.config.aiAssistantConfig = notificationAiAssistantConfig(
      this.store, this.userPermissionsService, this.translate, AiAssistantViewType.NOTIFICATION_RULE_LIST);
  }

  resolve(_route: ActivatedRouteSnapshot): EntityTableConfig<NotificationRule> {
    defaultEntityTablePermissions(this.userPermissionsService, this.config);
    return this.config;
  }

  private configureCellActions(): Array<CellActionDescriptor<NotificationRule>> {
    return [{
      name: '',
      nameFunction: (entity) =>
        this.translate.instant(entity.enabled ? 'notification.rule-disable' : 'notification.rule-enable'),
      icon: 'mdi:toggle-switch',
      isEnabled: () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE),
      iconFunction: (entity) => entity.enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
      onAction: ($event, entity) => this.toggleEnableMode($event, entity)
    },
    {
      name: this.translate.instant('notification.copy-rule'),
      icon: 'content_copy',
      isEnabled: () => this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE),
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
        rule,
        readonly: !this.userPermissionsService.hasGenericPermission(Resource.NOTIFICATION, Operation.WRITE)
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
