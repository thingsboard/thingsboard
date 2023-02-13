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

import { Injectable } from '@angular/core';
import { Resolve, Router } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DialogService } from '@core/services/dialog.service';
import { MatDialog } from '@angular/material/dialog';
import { ImportExportService } from '@home/components/import-export/import-export.service';
import { HomeDialogsService } from '@home/dialogs/home-dialogs.service';
import { AlarmRuleComponent } from '@home/pages/alarm/alarm-rule.component';
import { AlarmRuleTabsComponent } from '@home/pages/alarm/alarm-rule-tabs.component';
import { AlarmRule } from '@shared/models/alarm-rule.models';
import { emptyPageData } from '@shared/models/page/page-data';
import { of } from 'rxjs';

@Injectable()
export class AlarmRulesTableConfigResolver implements Resolve<EntityTableConfig<AlarmRule>> {

  private readonly config: EntityTableConfig<AlarmRule> = new EntityTableConfig<AlarmRule>();

  constructor(private importExport: ImportExportService,
              private homeDialogs: HomeDialogsService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService,
              private router: Router,
              private dialog: MatDialog) {

    this.config.entityType = EntityType.ALARM_RULE;
    this.config.entityComponent = AlarmRuleComponent;
    this.config.entityTabsComponent = AlarmRuleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ALARM_RULE);
    this.config.entityResources = entityTypeResources.get(EntityType.ALARM_RULE);

    this.config.hideDetailsTabsOnEdit = false;

    this.config.columns.push(
      new DateEntityTableColumn<AlarmRule>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<AlarmRule>('name', 'alarm-rule.name', '50%')
    );

    this.config.deleteEntityTitle = alarmRule => this.translate.instant('alarm-rule.delete-alarm-rule-title',
      { alarmRuleName: alarmRule.name });
    this.config.deleteEntityContent = () => this.translate.instant('alarm-rule.delete-alarm-rule-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('alarm-rule.delete-alarm-rules-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('alarm-rule.delete-alarm-rules-text');

    this.config.entitiesFetchFunction = pageLink => of(emptyPageData<AlarmRule>()); // TODO
    this.config.loadEntity = id => of(null); // TODO
    this.config.saveEntity = alarmRule => of(alarmRule); // TODO
    this.config.deleteEntity = id => of(null); // TODO
    this.config.onEntityAction = action => this.onAlarmRuleAction(action);
  }

  resolve(): EntityTableConfig<AlarmRule> {
    this.config.tableTitle = this.translate.instant('alarm-rule.alarm-rules');

    return this.config;
  }

  private openAlarmRule($event: Event, alarmRule: AlarmRule) {
    if ($event) {
      $event.stopPropagation();
    }
    const url = this.router.createUrlTree(['alarm', 'rules', alarmRule.id.id]);
    this.router.navigateByUrl(url);
  }

  onAlarmRuleAction(action: EntityAction<AlarmRule>): boolean {
    switch (action.action) {
      case 'open':
        this.openAlarmRule(action.event, action.entity);
        return true;
    }
    return false;
  }

}
