// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { CalculatedFieldAlarmRule, CalculatedFieldsQuery } from "@shared/models/calculated-field.models";
import { AlarmRulesTableConfig } from "@home/components/alarm-rules/alarm-rules-table-config";

@Component({
    selector: 'tb-alarm-rule-table-header',
    templateUrl: './alarm-rule-table-header.component.html',
    styleUrls: ['./alarm-rule-table-header.component.scss'],
    standalone: false
})
export class AlarmRuleTableHeaderComponent extends EntityTableHeaderComponent<CalculatedFieldAlarmRule> {

  get alarmRuleTableConfig(): AlarmRulesTableConfig {
    return this.entitiesTableConfig as AlarmRulesTableConfig;
  }

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  alarmRuleFilterChanged(alarmRuleFilterConfig: CalculatedFieldsQuery) {
    this.alarmRuleTableConfig.alarmRuleFilterConfig = alarmRuleFilterConfig;
    this.alarmRuleTableConfig.getTable().resetSortAndFilter(true, true);
  }
}
