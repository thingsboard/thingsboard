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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { AlarmRuleDialogComponent } from "@home/components/alarm-rules/alarm-rule-dialog.component";
import { CreateCfAlarmRulesComponent } from "@home/components/alarm-rules/create-cf-alarm-rules.component";
import { CfAlarmRuleComponent } from "@home/components/alarm-rules/cf-alarm-rule.component";
import { CfAlarmRuleConditionComponent } from "@home/components/alarm-rules/cf-alarm-rule-condition.component";
import {
  CfAlarmRuleConditionDialogComponent
} from "@home/components/alarm-rules/cf-alarm-rule-condition-dialog.component";
import { CfAlarmScheduleComponent } from "@home/components/alarm-rules/cf-alarm-schedule.component";
import { CfAlarmScheduleDialogComponent } from "@home/components/alarm-rules/cf-alarm-schedule-dialog.component";
import {
  EntityDebugSettingsButtonComponent
} from "@home/components/entity/debug/entity-debug-settings-button.component";
import { AlarmRuleFilterTextComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-text.component";
import {
  CalculatedFieldArgumentsTableModule
} from "@home/components/calculated-fields/components/calculated-field-arguments/calculated-field-arguments-table.module";
import {
  AlarmRuleFilterPredicateListComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-list.component";
import {
  AlarmRuleFilterPredicateComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate.component";
import {
  AlarmRuleFilterPredicateValueComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-value.component";
import {
  AlarmRuleComplexFilterPredicateDialogComponent
} from "@home/components/alarm-rules/filter/alarm-rule-complex-filter-predicate-dialog.component";
import { AlarmRuleFilterListComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-list.component";
import { AlarmRuleFilterDialogComponent } from "@home/components/alarm-rules/filter/alarm-rule-filter-dialog.component";
import { AlarmRuleDetailsDialogComponent } from "@home/components/alarm-rules/alarm-rule-details-dialog.component";
import { AlarmRuleFilterConfigComponent } from "@home/components/alarm-rules/alarm-rule-filter-config.component";
import { AlarmRuleTableHeaderComponent } from "@home/components/alarm-rules/alarm-rule-table-header.component";
import {
  AlarmRuleFilterPredicateNoDataValueComponent
} from "@home/components/alarm-rules/filter/alarm-rule-filter-predicate-no-data-value.component";
import { AlarmRulesComponent } from '@home/components/alarm-rules/alarm-rules.component';

@NgModule({
  declarations: [
    AlarmRuleDialogComponent,
    CreateCfAlarmRulesComponent,
    CfAlarmRuleComponent,
    CfAlarmRuleConditionComponent,
    CfAlarmRuleConditionDialogComponent,
    CfAlarmScheduleComponent,
    CfAlarmScheduleDialogComponent,
    AlarmRuleFilterTextComponent,
    AlarmRuleFilterListComponent,
    AlarmRuleFilterDialogComponent,
    AlarmRuleFilterPredicateListComponent,
    AlarmRuleFilterPredicateComponent,
    AlarmRuleFilterPredicateValueComponent,
    AlarmRuleComplexFilterPredicateDialogComponent,
    AlarmRuleDetailsDialogComponent,
    AlarmRuleFilterConfigComponent,
    AlarmRuleTableHeaderComponent,
    AlarmRuleFilterPredicateNoDataValueComponent,
    AlarmRulesComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    EntityDebugSettingsButtonComponent,
    CalculatedFieldArgumentsTableModule
  ],
  exports: [
    AlarmRuleDialogComponent,
    AlarmRulesComponent
  ]
})
export class AlarmRuleModule { }
