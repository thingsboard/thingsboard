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
import { ComplexOperation, EntityKeyValueType, FilterPredicateType } from "@shared/models/query/query.models";
import { EntityType } from "@shared/models/entity-type.models";
import { Observable } from "rxjs";
import { CalculatedField, CalculatedFieldArgument } from "@shared/models/calculated-field.models";

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
  ComplexAlarmRuleFilterPredicate |
  NoDataAlarmRuleFilterPredicate;

export interface AlarmRuleValue<T> {
  dynamicValueArgument?: string;
  staticValue?: T
}

export interface StringAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.STRING;
  operation: AlarmRuleStringOperation;
  value: AlarmRuleValue<string>;
  ignoreCase: boolean;
}

export interface NumericAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.NUMERIC;
  operation: AlarmRuleNumericOperation;
  value: AlarmRuleValue<number>;
}

export interface BooleanAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.BOOLEAN;
  operation: AlarmRuleBooleanOperation;
  value: AlarmRuleValue<boolean>;
}

export interface NoDataAlarmRuleFilterPredicate {
  type: AlarmRuleFilterPredicateType.NO_DATA;
  unit: TimeUnit,
  operation: AlarmRuleStringOperation.NO_DATA | AlarmRuleNumericOperation.NO_DATA | AlarmRuleBooleanOperation.NO_DATA;
  duration: AlarmRuleValue<number>;
}

export interface BaseComplexFilterPredicate<T extends AlarmRuleFilterPredicate> {
  type: AlarmRuleFilterPredicateType.COMPLEX;
  operation: ComplexOperation;
  predicates: Array<T>;
}

export type ComplexAlarmRuleFilterPredicate = BaseComplexFilterPredicate<AlarmRuleFilterPredicate>;

export interface AlarmRuleFilterConfig {
  name?: Array<string>;
  entityType?: EntityType;
  entities?: Array<string>;
}

export const filterOperationTranslationMap = new Map<ComplexOperation, string>(
  [
    [ComplexOperation.AND, 'alarm-rule.filter-operation.and'],
    [ComplexOperation.OR, 'alarm-rule.filter-operation.or'],
  ]
);

export enum AlarmRuleFilterPredicateType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  COMPLEX = 'COMPLEX',
  NO_DATA = 'NO_DATA'
}

export enum AlarmRuleStringOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  IN = 'IN',
  NOT_IN = 'NOT_IN',
}

export const alarmRuleStringOperationTranslationMap = new Map<AlarmRuleStringOperation, string>(
  [
    [AlarmRuleStringOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleStringOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleStringOperation.STARTS_WITH, 'filter.operation.starts-with'],
    [AlarmRuleStringOperation.ENDS_WITH, 'filter.operation.ends-with'],
    [AlarmRuleStringOperation.CONTAINS, 'filter.operation.contains'],
    [AlarmRuleStringOperation.NOT_CONTAINS, 'filter.operation.not-contains'],
    [AlarmRuleStringOperation.IN, 'filter.operation.in'],
    [AlarmRuleStringOperation.NOT_IN, 'filter.operation.not-in'],
    [AlarmRuleStringOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export enum AlarmRuleNumericOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
  GREATER = 'GREATER',
  LESS = 'LESS',
  GREATER_OR_EQUAL = 'GREATER_OR_EQUAL',
  LESS_OR_EQUAL = 'LESS_OR_EQUAL'
}

export const alarmRuleNumericOperationTranslationMap = new Map<AlarmRuleNumericOperation, string>(
  [
    [AlarmRuleNumericOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleNumericOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleNumericOperation.GREATER, 'filter.operation.greater'],
    [AlarmRuleNumericOperation.LESS, 'filter.operation.less'],
    [AlarmRuleNumericOperation.GREATER_OR_EQUAL, 'filter.operation.greater-or-equal'],
    [AlarmRuleNumericOperation.LESS_OR_EQUAL, 'filter.operation.less-or-equal'],
    [AlarmRuleNumericOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export enum AlarmRuleBooleanOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  NO_DATA = 'NO_DATA',
}

export const alarmRuleBooleanOperationTranslationMap = new Map<AlarmRuleBooleanOperation, string>(
  [
    [AlarmRuleBooleanOperation.EQUAL, 'filter.operation.equal'],
    [AlarmRuleBooleanOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [AlarmRuleBooleanOperation.NO_DATA, 'alarm-rule.missing-for']
  ]
);

export const alarmRuleDefaultScript =
  '// Sample expression for an alarm rule: triggers when temperature is above 20 degree\n' +
  'return temperature > 20;'

export type AlarmRuleTestScriptFn = (calculatedField: CalculatedField, expression: string, argumentsObj?: Record<string, unknown>, closeAllOnSave?: boolean) => Observable<string>;

export function checkPredicates(predicates: any[], validSet: Set<string>): boolean {
  for (const predicate of predicates) {
    if (!predicate) continue;
    if (predicate?.value?.dynamicValueArgument) {
      if (!validSet.has(predicate.value.dynamicValueArgument)) {
        return false;
      }
    }
    if (predicate.type === 'COMPLEX' && Array.isArray(predicate.predicates)) {
      if (!checkPredicates(predicate.predicates, validSet)) {
        return false;
      }
    }
  }
  return true;
}

export function areFilterAndPredicateArgumentsValid(obj: any, args: Record<string, CalculatedFieldArgument>): boolean {
  const validSet = new Set(Object.keys(args));
  const filter = obj || [];
  if (filter.argument && !validSet.has(filter.argument)) {
    return false;
  }
  if (Array.isArray(filter.predicates)) {
    if (!checkPredicates(filter.predicates, validSet)) {
      return false;
    }
  }
  return true;
}

export function areFiltersAndPredicateArgumentsValid(obj: any, args: Record<string, CalculatedFieldArgument>): boolean {
  const validSet = new Set(Object.keys(args));
  const filters = obj || [];
    for (const filter of filters) {
      if (filter.argument && !validSet.has(filter.argument)) {
        return false;
      }
    }
  for (const filter of filters) {
    if (Array.isArray(filter.predicates)) {
      if (!checkPredicates(filter.predicates, validSet)) {
        return false;
      }
    }
  }
  return true;
}

export function isPredicateArgumentsValid(predicates: any, args: Record<string, CalculatedFieldArgument>): boolean {
  const validSet = new Set(Object.keys(args));
  if (Array.isArray(predicates)) {
    if (!checkPredicates(predicates, validSet)) {
      return false;
    }
  }
  return true;
}
