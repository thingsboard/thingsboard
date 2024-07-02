///
/// Copyright © 2016-2024 The Thingsboard Authors
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
import { Resolve } from '@angular/router';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { AlarmRuleInfo } from '@shared/models/alarm-rule.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { AlarmRuleComponent } from '@home/pages/alarm/alarm-rule.component';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class AlarmRulesTableConfigResolver implements Resolve<EntityTableConfig<AlarmRuleInfo>> {

  private readonly config: EntityTableConfig<AlarmRuleInfo> = new EntityTableConfig<AlarmRuleInfo>();

  constructor(private translate: TranslateService) {
    this.config.entityType = EntityType.ALARM_RULE;
    this.config.entityComponent = AlarmRuleComponent;
    // this.config.entityTabsComponent = AlarmRuleTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.ALARM_RULE);
    this.config.entityResources = entityTypeResources.get(EntityType.ALARM_RULE);
    this.config.tableTitle = this.translate.instant('alarm-rule.alarm-rules');
  }

  resolve(): EntityTableConfig<AlarmRuleInfo> {
    return this.config;
  }

}
