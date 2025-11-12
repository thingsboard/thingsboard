///
/// Copyright © 2016-2025 The Thingsboard Authors
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


import { CustomTimeSchedulerItem } from "@shared/models/device.models";
import { DashboardId } from "@shared/models/id/dashboard-id";
import { TimeUnit } from "@shared/models/time/time.models";
import {
  BooleanOperation,
  ComplexOperation,
  EntityKeyValueType,
  FilterPredicateType,
  NumericOperation,
  StringOperation
} from "@shared/models/query/query.models";

export enum AlarmRuleScheduleType {
  ANY_TIME = 'ANY_TIME',
  SPECIFIC_TIME = 'SPECIFIC_TIME',
  CUSTOM = 'CUSTOM'
}

export const AlarmRuleScheduleTypeTranslationMap = new Map<AlarmRuleScheduleType, string>(
  [
    [AlarmRuleScheduleType.ANY_TIME, 'alarm-rule.schedule.any-time'],
    [AlarmRuleScheduleType.SPECIFIC_TIME, 'alarm-rule.schedule.specific-time'],
    [AlarmRuleScheduleType.CUSTOM, 'alarm-rule.schedule.custom']
  ]
);

export enum AlarmRuleConditionType {
  SIMPLE = 'SIMPLE',
  DURATION = 'DURATION',
  REPEATING = 'REPEATING'
}

export const AlarmRuleConditionTypeTranslationMap = new Map<AlarmRuleConditionType, string>(
  [
    [AlarmRuleConditionType.SIMPLE, 'alarm-rule.conditions.simple'],
    [AlarmRuleConditionType.DURATION, 'alarm-rule.conditions.duration'],
    [AlarmRuleConditionType.REPEATING, 'alarm-rule.conditions.repeating']
  ]
);

export enum AlarmRuleExpressionType {
  SIMPLE = 'SIMPLE',
  TBEL = 'TBEL',
}

export const FilterPredicateTypeTranslationMap = new Map<FilterPredicateType, string>(
  [
    [FilterPredicateType.STRING, 'alarm-rule.filter-predicate-type.string'],
    [FilterPredicateType.NUMERIC, 'alarm-rule.filter-predicate-type.numeric'],
    [FilterPredicateType.BOOLEAN, 'alarm-rule.filter-predicate-type.boolean'],
    [FilterPredicateType.COMPLEX, 'alarm-rule.filter-predicate-type.complex']
  ]
);

export interface AlarmRule {
  condition: AlarmRuleCondition;
  alarmDetails?: string;
  dashboardId?: DashboardId;
}

export interface AlarmRuleCondition {
  type: AlarmRuleConditionType;
  expression: AlarmRuleExpression;
  schedule?: AlarmRuleSchedule;
  unit?: TimeUnit;
  value?: AlarmRuleValue<number>;
  count?: AlarmRuleValue<number>;
}

export interface AlarmRuleExpression {
  type: AlarmRuleExpressionType;
  expression?: string;
  filters?: Array<AlarmRuleFilter>;
  operation?: ComplexOperation;
}

export interface AlarmRuleSchedule {
  staticValue?: {
    type?: AlarmRuleScheduleType;
    timezone?: string;
    daysOfWeek?: number[];
    startsOn?: number;
    endsOn?: number;
    items?: CustomTimeSchedulerItem[];
  };
  dynamicValueArgument?: string;
}

export interface AlarmRuleFilter {
  argument: string;
  valueType: EntityKeyValueType;
  operation: ComplexOperation;
  predicates: AlarmRuleFilterPredicate[];
}

export interface AlarmRulePredicateInfo {
  keyFilterPredicate: AlarmRuleFilterPredicate;
}

export type AlarmRuleFilterPredicate = StringAlarmRuleFilterPredicate |
  NumericAlarmRuleFilterPredicate |
  BooleanAlarmRuleFilterPredicate |
  ComplexAlarmRuleFilterPredicate;

export interface AlarmRuleValue<T> {
  dynamicValueArgument?: string;
  staticValue?: T
}

export interface StringAlarmRuleFilterPredicate {
  type: FilterPredicateType.STRING;
  operation: StringOperation;
  value: AlarmRuleValue<string>;
  ignoreCase: boolean;
}

export interface NumericAlarmRuleFilterPredicate {
  type: FilterPredicateType.NUMERIC;
  operation: NumericOperation;
  value: AlarmRuleValue<number>;
}

export interface BooleanAlarmRuleFilterPredicate {
  type: FilterPredicateType.BOOLEAN;
  operation: BooleanOperation;
  value: AlarmRuleValue<boolean>;
}

export interface BaseComplexFilterPredicate<T extends AlarmRuleFilterPredicate> {
  type: FilterPredicateType.COMPLEX;
  operation: ComplexOperation;
  predicates: Array<T>;
}

export type ComplexAlarmRuleFilterPredicate = BaseComplexFilterPredicate<AlarmRuleFilterPredicate>;
