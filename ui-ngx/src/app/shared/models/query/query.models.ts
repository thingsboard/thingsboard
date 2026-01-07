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

import { AliasFilterType, EntityFilters } from '@shared/models/alias.models';
import { EntityId } from '@shared/models/id/entity-id';
import { SortDirection } from '@angular/material/sort';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityInfo } from '@shared/models/entity.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKey, Datasource, DatasourceType } from '@shared/models/widget.models';
import { PageData } from '@shared/models/page/page-data';
import {
  guid,
  isArraysEqualIgnoreUndefined,
  isDefined,
  isDefinedAndNotNull,
  isEmpty,
  isEqual,
  isEqualIgnoreUndefined,
  isUndefinedOrNull
} from '@core/utils';
import { TranslateService } from '@ngx-translate/core';
import { AlarmInfo, AlarmSearchStatus, AlarmSeverity } from '../alarm.models';
import { DatePipe } from '@angular/common';
import { UserId } from '../id/user-id';
import { Direction } from '@shared/models/page/sort-order';
import { TbUnit } from '@shared/models/unit.models';

export enum EntityKeyType {
  ATTRIBUTE = 'ATTRIBUTE',
  CLIENT_ATTRIBUTE = 'CLIENT_ATTRIBUTE',
  SHARED_ATTRIBUTE = 'SHARED_ATTRIBUTE',
  SERVER_ATTRIBUTE = 'SERVER_ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES',
  ENTITY_FIELD = 'ENTITY_FIELD',
  ALARM_FIELD = 'ALARM_FIELD',
  CONSTANT = 'CONSTANT',
  COUNT = 'COUNT'
}

export const entityKeyTypeTranslationMap = new Map<EntityKeyType, string>(
  [
    [EntityKeyType.ATTRIBUTE, 'filter.key-type.attribute'],
    [EntityKeyType.TIME_SERIES, 'filter.key-type.timeseries'],
    [EntityKeyType.ENTITY_FIELD, 'filter.key-type.entity-field'],
    [EntityKeyType.CONSTANT, 'filter.key-type.constant'],
    [EntityKeyType.CLIENT_ATTRIBUTE, 'filter.key-type.client-attribute'],
    [EntityKeyType.SERVER_ATTRIBUTE, 'filter.key-type.server-attribute'],
    [EntityKeyType.SHARED_ATTRIBUTE, 'filter.key-type.shared-attribute']
  ]
);

export function entityKeyTypeToDataKeyType(entityKeyType: EntityKeyType): DataKeyType {
  switch (entityKeyType) {
    case EntityKeyType.ATTRIBUTE:
    case EntityKeyType.CLIENT_ATTRIBUTE:
    case EntityKeyType.SHARED_ATTRIBUTE:
    case EntityKeyType.SERVER_ATTRIBUTE:
      return DataKeyType.attribute;
    case EntityKeyType.TIME_SERIES:
      return DataKeyType.timeseries;
    case EntityKeyType.ENTITY_FIELD:
      return DataKeyType.entityField;
    case EntityKeyType.ALARM_FIELD:
      return DataKeyType.alarm;
    case EntityKeyType.COUNT:
      return DataKeyType.count;
  }
}

export function dataKeyTypeToEntityKeyType(dataKeyType: DataKeyType): EntityKeyType {
  switch (dataKeyType) {
    case DataKeyType.timeseries:
      return EntityKeyType.TIME_SERIES;
    case DataKeyType.attribute:
      return EntityKeyType.ATTRIBUTE;
    case DataKeyType.function:
      return EntityKeyType.ENTITY_FIELD;
    case DataKeyType.alarm:
      return EntityKeyType.ALARM_FIELD;
    case DataKeyType.entityField:
      return EntityKeyType.ENTITY_FIELD;
    case DataKeyType.count:
      return EntityKeyType.COUNT;
  }
}

export interface EntityKey {
  type: EntityKeyType;
  key: string;
}

export function dataKeyToEntityKey(dataKey: DataKey): EntityKey {
  return {
    key: dataKey.name,
    type: dataKeyTypeToEntityKeyType(dataKey.type)
  };
}

export enum EntityKeyValueType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  DATE_TIME = 'DATE_TIME'
}

export interface EntityKeyValueTypeData {
  name: string;
  icon: string;
}

export const entityKeyValueTypesMap = new Map<EntityKeyValueType, EntityKeyValueTypeData>(
  [
    [
      EntityKeyValueType.STRING,
      {
        name: 'filter.value-type.string',
        icon: 'mdi:format-text'
      }
    ],
    [
      EntityKeyValueType.NUMERIC,
      {
        name: 'filter.value-type.numeric',
        icon: 'mdi:numeric'
      }
    ],
    [
      EntityKeyValueType.BOOLEAN,
      {
        name: 'filter.value-type.boolean',
        icon: 'mdi:checkbox-marked-outline'
      }
    ],
    [
      EntityKeyValueType.DATE_TIME,
      {
        name: 'filter.value-type.date-time',
        icon: 'mdi:calendar-clock'
      }
    ]
  ]
);

export function entityKeyValueTypeToFilterPredicateType(valueType: EntityKeyValueType): FilterPredicateType {
  switch (valueType) {
    case EntityKeyValueType.STRING:
      return FilterPredicateType.STRING;
    case EntityKeyValueType.NUMERIC:
    case EntityKeyValueType.DATE_TIME:
      return FilterPredicateType.NUMERIC;
    case EntityKeyValueType.BOOLEAN:
      return FilterPredicateType.BOOLEAN;
  }
}

export function createDefaultFilterPredicateInfo(valueType: EntityKeyValueType, complex: boolean): KeyFilterPredicateInfo {
  const predicate = createDefaultFilterPredicate(valueType, complex);
  return {
    keyFilterPredicate: predicate,
    userInfo: createDefaultFilterPredicateUserInfo()
  };
}

export function createDefaultFilterPredicateUserInfo(): KeyFilterPredicateUserInfo {
  return {
    editable: true,
    label: '',
    autogeneratedLabel: true,
    order: 0,
    unit: ''
  };
}

export function createDefaultFilterPredicate(valueType: EntityKeyValueType, complex: boolean): KeyFilterPredicate {
  const predicate = {
    type: complex ? FilterPredicateType.COMPLEX : entityKeyValueTypeToFilterPredicateType(valueType)
  } as KeyFilterPredicate;
  switch (predicate.type) {
    case FilterPredicateType.STRING:
      predicate.operation = StringOperation.STARTS_WITH;
      predicate.value = {
        defaultValue: ''
      };
      predicate.ignoreCase = false;
      break;
    case FilterPredicateType.NUMERIC:
      predicate.operation = NumericOperation.EQUAL;
      predicate.value = {
        defaultValue: valueType === EntityKeyValueType.DATE_TIME ? Date.now() : 0
      };
      break;
    case FilterPredicateType.BOOLEAN:
      predicate.operation = BooleanOperation.EQUAL;
      predicate.value = {
        defaultValue: false
      };
      break;
    case FilterPredicateType.COMPLEX:
      predicate.operation = ComplexOperation.AND;
      predicate.predicates = [];
      break;
  }
  return predicate;
}

export function getDynamicSourcesForAllowUser(allow: boolean): DynamicValueSourceType[] {
  const dynamicValueSourceTypes = [DynamicValueSourceType.CURRENT_TENANT,
    DynamicValueSourceType.CURRENT_CUSTOMER];
  if (allow) {
    dynamicValueSourceTypes.push(DynamicValueSourceType.CURRENT_USER);
  } else {
    dynamicValueSourceTypes.push(DynamicValueSourceType.CURRENT_DEVICE);
  }
  return dynamicValueSourceTypes;
}

export enum FilterPredicateType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  COMPLEX = 'COMPLEX'
}

export enum StringOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  IN = 'IN',
  NOT_IN = 'NOT_IN'
}

export const stringOperationTranslationMap = new Map<StringOperation, string>(
  [
    [StringOperation.EQUAL, 'filter.operation.equal'],
    [StringOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [StringOperation.STARTS_WITH, 'filter.operation.starts-with'],
    [StringOperation.ENDS_WITH, 'filter.operation.ends-with'],
    [StringOperation.CONTAINS, 'filter.operation.contains'],
    [StringOperation.NOT_CONTAINS, 'filter.operation.not-contains'],
    [StringOperation.IN, 'filter.operation.in'],
    [StringOperation.NOT_IN, 'filter.operation.not-in']
  ]
);

export enum NumericOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  GREATER = 'GREATER',
  LESS = 'LESS',
  GREATER_OR_EQUAL = 'GREATER_OR_EQUAL',
  LESS_OR_EQUAL = 'LESS_OR_EQUAL'
}

export const numericOperationTranslationMap = new Map<NumericOperation, string>(
  [
    [NumericOperation.EQUAL, 'filter.operation.equal'],
    [NumericOperation.NOT_EQUAL, 'filter.operation.not-equal'],
    [NumericOperation.GREATER, 'filter.operation.greater'],
    [NumericOperation.LESS, 'filter.operation.less'],
    [NumericOperation.GREATER_OR_EQUAL, 'filter.operation.greater-or-equal'],
    [NumericOperation.LESS_OR_EQUAL, 'filter.operation.less-or-equal']
  ]
);

export enum BooleanOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL'
}

export const booleanOperationTranslationMap = new Map<BooleanOperation, string>(
  [
    [BooleanOperation.EQUAL, 'filter.operation.equal'],
    [BooleanOperation.NOT_EQUAL, 'filter.operation.not-equal']
  ]
);

export enum ComplexOperation {
  AND = 'AND',
  OR = 'OR'
}

export const complexOperationTranslationMap = new Map<ComplexOperation, string>(
  [
    [ComplexOperation.AND, 'filter.operation.and'],
    [ComplexOperation.OR, 'filter.operation.or']
  ]
);

export enum DynamicValueSourceType {
  CURRENT_TENANT = 'CURRENT_TENANT',
  CURRENT_CUSTOMER = 'CURRENT_CUSTOMER',
  CURRENT_USER = 'CURRENT_USER',
  CURRENT_DEVICE = 'CURRENT_DEVICE'
}

export const dynamicValueSourceTypeTranslationMap = new Map<DynamicValueSourceType, string>(
  [
    [DynamicValueSourceType.CURRENT_TENANT, 'filter.current-tenant'],
    [DynamicValueSourceType.CURRENT_CUSTOMER, 'filter.current-customer'],
    [DynamicValueSourceType.CURRENT_USER, 'filter.current-user'],
    [DynamicValueSourceType.CURRENT_DEVICE, 'filter.current-device']
  ]
);

export const inheritModeForDynamicValueSourceType = [
  DynamicValueSourceType.CURRENT_CUSTOMER,
  DynamicValueSourceType.CURRENT_DEVICE];

export interface DynamicValue<T> {
  sourceType: DynamicValueSourceType;
  sourceAttribute: string;
  inherit?: boolean;
}

export interface FilterPredicateValue<T> {
  defaultValue: T;
  userValue?: T;
  dynamicValue?: DynamicValue<T>;
}

export interface StringFilterPredicate {
  type: FilterPredicateType.STRING;
  operation: StringOperation;
  value: FilterPredicateValue<string>;
  ignoreCase: boolean;
}

export interface NumericFilterPredicate {
  type: FilterPredicateType.NUMERIC;
  operation: NumericOperation;
  value: FilterPredicateValue<number>;
}

export interface BooleanFilterPredicate {
  type: FilterPredicateType.BOOLEAN;
  operation: BooleanOperation;
  value: FilterPredicateValue<boolean>;
}

export interface BaseComplexFilterPredicate<T extends KeyFilterPredicate | KeyFilterPredicateInfo> {
  type: FilterPredicateType.COMPLEX;
  operation: ComplexOperation;
  predicates: Array<T>;
}

export type ComplexFilterPredicate = BaseComplexFilterPredicate<KeyFilterPredicate>;

export type ComplexFilterPredicateInfo = BaseComplexFilterPredicate<KeyFilterPredicateInfo>;

export type KeyFilterPredicate = StringFilterPredicate |
  NumericFilterPredicate |
  BooleanFilterPredicate |
  ComplexFilterPredicate |
  ComplexFilterPredicateInfo;

export interface KeyFilterPredicateUserInfo {
  editable: boolean;
  label: string;
  autogeneratedLabel: boolean;
  order?: number;
  unit?: TbUnit;
}

export interface KeyFilterPredicateInfo {
  keyFilterPredicate: KeyFilterPredicate;
  userInfo: KeyFilterPredicateUserInfo;
}

export interface KeyFilter {
  key: EntityKey;
  valueType: EntityKeyValueType;
  value?: string | number | boolean;
  predicate: KeyFilterPredicate;
}

export interface KeyFilterInfo {
  key: EntityKey;
  valueType: EntityKeyValueType;
  value?: string | number | boolean;
  predicates: Array<KeyFilterPredicateInfo>;
}

export interface FilterInfo {
  filter: string;
  editable: boolean;
  keyFilters: Array<KeyFilterInfo>;
}

export interface FiltersInfo {
  datasourceFilters: {[datasourceIndex: number]: FilterInfo};
}

export function keyFiltersToText(translate: TranslateService, datePipe: DatePipe, keyFilters: Array<KeyFilter>): string {
  const filtersText = keyFilters.map(keyFilter =>
      keyFilterToText(translate, datePipe, keyFilter,
        keyFilters.length > 1 ? ComplexOperation.AND : undefined));
  let result: string;
  if (filtersText.length > 1) {
    const andText = translate.instant('filter.operation.and');
    result = filtersText.join(' <span class="tb-filter-complex-operation">' + andText + '</span> ');
  } else {
    result = filtersText[0];
  }
  return result;
}

export function keyFilterToText(translate: TranslateService, datePipe: DatePipe, keyFilter: KeyFilter,
                                parentComplexOperation?: ComplexOperation): string {
  const keyFilterPredicate = keyFilter.predicate;
  return keyFilterPredicateToText(translate, datePipe, keyFilter, keyFilterPredicate, parentComplexOperation);
}

export function keyFilterPredicateToText(translate: TranslateService,
                                         datePipe: DatePipe,
                                         keyFilter: KeyFilter,
                                         keyFilterPredicate: KeyFilterPredicate,
                                         parentComplexOperation?: ComplexOperation): string {
  if (keyFilterPredicate.type === FilterPredicateType.COMPLEX) {
    const complexPredicate = keyFilterPredicate as ComplexFilterPredicate;
    const complexOperation = complexPredicate.operation;
    const complexPredicatesText =
      complexPredicate.predicates.map(predicate => keyFilterPredicateToText(translate, datePipe, keyFilter, predicate, complexOperation));
    if (complexPredicatesText.length > 1) {
      const operationText = translate.instant(complexOperationTranslationMap.get(complexOperation));
      let result = complexPredicatesText.join(' <span class="tb-filter-complex-operation">' + operationText + '</span> ');
      if (complexOperation === ComplexOperation.OR && parentComplexOperation && ComplexOperation.OR !== parentComplexOperation) {
        result = `<span class="tb-filter-bracket"><span class="tb-left-bracket">(</span>${result}<span class="tb-right-bracket">)</span></span>`;
      }
      return result;
    } else {
      return complexPredicatesText[0];
    }
  } else {
    return simpleKeyFilterPredicateToText(translate, datePipe, keyFilter, keyFilterPredicate);
  }
}

function simpleKeyFilterPredicateToText(translate: TranslateService,
                                        datePipe: DatePipe,
                                        keyFilter: KeyFilter,
                                        keyFilterPredicate: StringFilterPredicate |
                                                            NumericFilterPredicate |
                                                            BooleanFilterPredicate): string {
  const key = keyFilter.key.key;
  let operation: string;
  let value: string;
  const val = keyFilterPredicate.value;
  const dynamicValue = !!val.dynamicValue && !!val.dynamicValue.sourceType;
  if (dynamicValue) {
    value = '<span class="tb-filter-dynamic-value"><span class="tb-filter-dynamic-source">' +
    translate.instant(dynamicValueSourceTypeTranslationMap.get(val.dynamicValue.sourceType)) + '</span>';
    value += '.<span class="tb-filter-value">' + val.dynamicValue.sourceAttribute + '</span></span>';
  }
  switch (keyFilterPredicate.type) {
    case FilterPredicateType.STRING:
      operation = translate.instant(stringOperationTranslationMap.get(keyFilterPredicate.operation));
      if (keyFilterPredicate.ignoreCase) {
        operation += ' ' + translate.instant('filter.ignore-case');
      }
      if (!dynamicValue) {
        value = `'${keyFilterPredicate.value.defaultValue}'`;
      }
      break;
    case FilterPredicateType.NUMERIC:
      operation = translate.instant(numericOperationTranslationMap.get(keyFilterPredicate.operation));
      if (!dynamicValue) {
        if (keyFilter.valueType === EntityKeyValueType.DATE_TIME) {
          value = datePipe.transform(keyFilterPredicate.value.defaultValue, 'yyyy-MM-dd HH:mm');
        } else {
          value = keyFilterPredicate.value.defaultValue + '';
        }
      }
      break;
    case FilterPredicateType.BOOLEAN:
      operation = translate.instant(booleanOperationTranslationMap.get(keyFilterPredicate.operation));
      if (!dynamicValue) {
        value = translate.instant(keyFilterPredicate.value.defaultValue ? 'value.true' : 'value.false');
      }
      break;
  }
  if (!dynamicValue) {
    value = `<span class="tb-filter-value">${value}</span>`;
  }
  return `<span class="tb-filter-predicate"><span class="tb-filter-entity-key">${key}</span> <span class="tb-filter-simple-operation">${operation}</span> ${value}</span>`;
}

export function keyFilterInfosToKeyFilters(keyFilterInfos: Array<KeyFilterInfo>): Array<KeyFilter> {
  if (!keyFilterInfos) {
    return [];
  }
  const keyFilters: Array<KeyFilter> = [];
  for (const keyFilterInfo of keyFilterInfos) {
    const key = keyFilterInfo.key;
    for (const predicate of keyFilterInfo.predicates) {
      const keyFilter: KeyFilter = {
        key,
        valueType: keyFilterInfo.valueType,
        value: keyFilterInfo.value,
        predicate: keyFilterPredicateInfoToKeyFilterPredicate(predicate)
      };
      keyFilters.push(keyFilter);
    }
  }
  return keyFilters;
}

export function keyFiltersToKeyFilterInfos(keyFilters: Array<KeyFilter>): Array<KeyFilterInfo> {
  const keyFilterInfos: Array<KeyFilterInfo> = [];
  const keyFilterInfoMap: {[infoKey: string]: KeyFilterInfo} = {};
  if (keyFilters) {
    for (const keyFilter of keyFilters) {
      const key = keyFilter.key;
      const infoKey = key.key + key.type + keyFilter.valueType;
      let keyFilterInfo = keyFilterInfoMap[infoKey];
      if (!keyFilterInfo) {
        keyFilterInfo = {
          key,
          valueType: keyFilter.valueType,
          value: keyFilter.value,
          predicates: []
        };
        keyFilterInfoMap[infoKey] = keyFilterInfo;
        keyFilterInfos.push(keyFilterInfo);
      }
      if (keyFilter.predicate) {
        keyFilterInfo.predicates.push(keyFilterPredicateToKeyFilterPredicateInfo(keyFilter.predicate));
      }
    }
  }
  return keyFilterInfos;
}

export function filterInfoToKeyFilters(filter: FilterInfo): Array<KeyFilter> {
  const keyFilterInfos = filter.keyFilters;
  const keyFilters: Array<KeyFilter> = [];
  for (const keyFilterInfo of keyFilterInfos) {
    const key = keyFilterInfo.key;
    for (const predicate of keyFilterInfo.predicates) {
      const keyFilter: KeyFilter = {
        key,
        valueType: keyFilterInfo.valueType,
        value: keyFilterInfo.value,
        predicate: keyFilterPredicateInfoToKeyFilterPredicate(predicate)
      };
      keyFilters.push(keyFilter);
    }
  }
  return keyFilters;
}

export function keyFilterPredicateInfoToKeyFilterPredicate(keyFilterPredicateInfo: KeyFilterPredicateInfo): KeyFilterPredicate {
  let keyFilterPredicate = keyFilterPredicateInfo.keyFilterPredicate;
  if (keyFilterPredicate.type === FilterPredicateType.COMPLEX) {
    const complexInfo = keyFilterPredicate as ComplexFilterPredicateInfo;
    const predicates = complexInfo.predicates.map((predicateInfo => keyFilterPredicateInfoToKeyFilterPredicate(predicateInfo)));
    keyFilterPredicate = {
      type: FilterPredicateType.COMPLEX,
      operation: complexInfo.operation,
      predicates
    } as ComplexFilterPredicate;
  }
  return keyFilterPredicate;
}

export function keyFilterPredicateToKeyFilterPredicateInfo(keyFilterPredicate: KeyFilterPredicate): KeyFilterPredicateInfo {
  const keyFilterPredicateInfo: KeyFilterPredicateInfo = {
    keyFilterPredicate: null,
    userInfo: null
  };
  if (keyFilterPredicate.type === FilterPredicateType.COMPLEX) {
    const complexPredicate = keyFilterPredicate as ComplexFilterPredicate;
    const predicateInfos = complexPredicate.predicates.map(
      predicate => keyFilterPredicateToKeyFilterPredicateInfo(predicate));
    keyFilterPredicateInfo.keyFilterPredicate = {
      predicates: predicateInfos,
      operation: complexPredicate.operation,
      type: FilterPredicateType.COMPLEX
    } as ComplexFilterPredicateInfo;
  } else {
    keyFilterPredicateInfo.keyFilterPredicate = keyFilterPredicate;
  }
  return keyFilterPredicateInfo;
}

export function isFilterEditable(filter: FilterInfo): boolean {
  if (filter.editable) {
    return filter.keyFilters.some(value => isKeyFilterInfoEditable(value));
  } else {
    return false;
  }
}

export function isKeyFilterInfoEditable(keyFilterInfo: KeyFilterInfo): boolean {
  return keyFilterInfo.predicates.some(value => isPredicateInfoEditable(value));
}

export function isPredicateInfoEditable(predicateInfo: KeyFilterPredicateInfo): boolean {
  if (predicateInfo.keyFilterPredicate.type === FilterPredicateType.COMPLEX) {
    const complexFilterPredicateInfo: ComplexFilterPredicateInfo = predicateInfo.keyFilterPredicate as ComplexFilterPredicateInfo;
    return complexFilterPredicateInfo.predicates.some(value => isPredicateInfoEditable(value));
  } else {
    return predicateInfo.userInfo.editable;
  }
}

export interface UserFilterInputInfo {
  label: string;
  valueType: EntityKeyValueType;
  info: KeyFilterPredicateInfo;
  unit: TbUnit;
}

export function filterToUserFilterInfoList(filter: Filter, translate: TranslateService): Array<UserFilterInputInfo> {
  const result = filter.keyFilters.map((keyFilterInfo => keyFilterInfoToUserFilterInfoList(keyFilterInfo, translate)));
  let userInputs: Array<UserFilterInputInfo> = [].concat.apply([], result);
  userInputs = userInputs.sort((input1, input2) => {
    const order1 = isDefined(input1.info.userInfo.order) ? input1.info.userInfo.order : 0;
    const order2 = isDefined(input2.info.userInfo.order) ? input2.info.userInfo.order : 0;
    return order1 - order2;
  });
  return userInputs;
}

export function keyFilterInfoToUserFilterInfoList(keyFilterInfo: KeyFilterInfo, translate: TranslateService): Array<UserFilterInputInfo> {
  const result = keyFilterInfo.predicates.map((predicateInfo => predicateInfoToUserFilterInfoList(keyFilterInfo.key,
    keyFilterInfo.valueType, predicateInfo, translate)));
  return [].concat.apply([], result);
}

export function predicateInfoToUserFilterInfoList(key: EntityKey,
                                                  valueType: EntityKeyValueType,
                                                  predicateInfo: KeyFilterPredicateInfo,
                                                  translate: TranslateService): Array<UserFilterInputInfo> {
  if (predicateInfo.keyFilterPredicate.type === FilterPredicateType.COMPLEX) {
    const complexFilterPredicateInfo: ComplexFilterPredicateInfo = predicateInfo.keyFilterPredicate as ComplexFilterPredicateInfo;
    const result = complexFilterPredicateInfo.predicates.map((predicateInfo1 =>
      predicateInfoToUserFilterInfoList(key, valueType, predicateInfo1, translate)));
    return [].concat.apply([], result);
  } else {
    if (predicateInfo.userInfo.editable) {
      const userInput: UserFilterInputInfo = {
        info: predicateInfo,
        label: predicateInfo.userInfo.label,
        unit: predicateInfo.userInfo.unit,
        valueType
      };
      if (predicateInfo.userInfo.autogeneratedLabel) {
        userInput.label = generateUserFilterValueLabel(key.key, valueType,
          predicateInfo.keyFilterPredicate.operation, translate);
      }
      return [userInput];
    } else {
      return [];
    }
  }
}

export function generateUserFilterValueLabel(key: string, valueType: EntityKeyValueType,
                                             operation: StringOperation | BooleanOperation | NumericOperation,
                                             translate: TranslateService) {
  let label = key;
  let operationTranslationKey: string;
  switch (valueType) {
    case EntityKeyValueType.STRING:
      operationTranslationKey = stringOperationTranslationMap.get(operation as StringOperation);
      break;
    case EntityKeyValueType.NUMERIC:
    case EntityKeyValueType.DATE_TIME:
      operationTranslationKey = numericOperationTranslationMap.get(operation as NumericOperation);
      break;
    case EntityKeyValueType.BOOLEAN:
      operationTranslationKey = booleanOperationTranslationMap.get(operation as BooleanOperation);
      break;
  }
  label += ' ' + translate.instant(operationTranslationKey);
  return label;
}

export interface Filter extends FilterInfo {
  id: string;
}

export interface Filters {
  [id: string]: Filter;
}

export interface EntityFilter extends EntityFilters {
  type?: AliasFilterType;
}

export interface EntityDataSortOrder {
  key: EntityKey;
  direction: Direction;
}

export interface EntityDataPageLink {
  pageSize: number;
  page: number;
  textSearch?: string;
  sortOrder?: EntityDataSortOrder;
  dynamic?: boolean;
}

export interface AlarmFilter {
  startTs?: number;
  endTs?: number;
  timeWindow?: number;
  typeList?: Array<string>;
  statusList?: Array<AlarmSearchStatus>;
  severityList?: Array<AlarmSeverity>;
  searchPropagatedAlarms?: boolean;
  assigneeId?: UserId;
}

export interface AlarmFilterConfig extends AlarmFilter {
  assignedToCurrentUser?: boolean;
}

export const alarmFilterConfigEquals = (filter1?: AlarmFilterConfig, filter2?: AlarmFilterConfig): boolean => {
  if (filter1 === filter2) {
    return true;
  }
  if ((isUndefinedOrNull(filter1) || isEmpty(filter1)) && (isUndefinedOrNull(filter2) || isEmpty(filter2))) {
    return true;
  } else if (isDefinedAndNotNull(filter1) && isDefinedAndNotNull(filter2)) {
    if (!isArraysEqualIgnoreUndefined(filter1.typeList, filter2.typeList)) {
      return false;
    }
    if (!isArraysEqualIgnoreUndefined(filter1.statusList, filter2.statusList)) {
      return false;
    }
    if (!isArraysEqualIgnoreUndefined(filter1.severityList, filter2.severityList)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.assigneeId, filter2.assigneeId)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.searchPropagatedAlarms, filter2.searchPropagatedAlarms)) {
      return false;
    }
    if (!isEqualIgnoreUndefined(filter1.assignedToCurrentUser, filter2.assignedToCurrentUser)) {
      return false;
    }
    return true;
  }
  return false;
};

export type AlarmCountQuery = EntityCountQuery & AlarmFilter;

export type AlarmDataPageLink = EntityDataPageLink & AlarmFilter;

export function entityDataPageLinkSortDirection(pageLink: EntityDataPageLink): SortDirection {
  if (pageLink.sortOrder) {
    return (pageLink.sortOrder.direction + '').toLowerCase() as SortDirection;
  } else {
    return '' as SortDirection;
  }
}

export function createDefaultEntityDataPageLink(pageSize: number): EntityDataPageLink {
  return {
    pageSize,
    page: 0,
    sortOrder: {
      key: {
        type: EntityKeyType.ENTITY_FIELD,
        key: 'createdTime'
      },
      direction: Direction.DESC
    }
  };
}

export const singleEntityDataPageLink: EntityDataPageLink = createDefaultEntityDataPageLink(1);

export const singleEntityFilterFromDeviceId = (deviceId: string): EntityFilter => ({
  type: AliasFilterType.singleEntity,
  singleEntity: {
    entityType: EntityType.DEVICE,
    id: deviceId
  }
});

export interface EntityCountQuery {
  entityFilter: EntityFilter;
  keyFilters?: Array<KeyFilter>;
}

export interface AbstractDataQuery<T extends EntityDataPageLink> extends EntityCountQuery {
  pageLink: T;
  entityFields?: Array<EntityKey>;
  latestValues?: Array<EntityKey>;
}

export interface EntityDataQuery extends AbstractDataQuery<EntityDataPageLink> {
}

export interface AlarmDataQuery extends AbstractDataQuery<AlarmDataPageLink> {
  alarmFields?: Array<EntityKey>;
}

export interface TsValue {
  ts: number;
  value: string;
  count?: number;
}

export interface ComparisonTsValue {
  current?: TsValue;
  previous?: TsValue;
}

export interface EntityData {
  entityId: EntityId;
  latest: {[entityKeyType: string]: {[key: string]: TsValue}};
  timeseries: {[key: string]: Array<TsValue>};
  aggLatest?: {[id: number]: ComparisonTsValue};
}

export interface AlarmData extends AlarmInfo {
  entityId: string;
  latest: {[entityKeyType: string]: {[key: string]: TsValue}};
}

export function entityPageDataChanged(prevPageData: PageData<EntityData>, nextPageData: PageData<EntityData>): boolean {
  const prevIds = prevPageData.data.map((entityData) => entityData.entityId.id);
  const nextIds = nextPageData.data.map((entityData) => entityData.entityId.id);
  return !isEqual(prevIds, nextIds);
}

export const entityInfoFields: EntityKey[] = [
  {
    type: EntityKeyType.ENTITY_FIELD,
    key: 'name'
  },
  {
    type: EntityKeyType.ENTITY_FIELD,
    key: 'label'
  },
  {
    type: EntityKeyType.ENTITY_FIELD,
    key: 'additionalInfo'
  }
];

export function entityDataToEntityInfo(entityData: EntityData): EntityInfo {
  const entityInfo: EntityInfo = {
    id: entityData.entityId.id,
    entityType: entityData.entityId.entityType as EntityType
  };
  if (entityData.latest && entityData.latest[EntityKeyType.ENTITY_FIELD]) {
    const fields = entityData.latest[EntityKeyType.ENTITY_FIELD];
    if (fields.name) {
      entityInfo.name = fields.name.value;
    } else {
      entityInfo.name = '';
    }
    if (fields.label) {
      entityInfo.label = fields.label.value;
    } else {
      entityInfo.label = '';
    }
    entityInfo.entityDescription = '';
    if (fields.additionalInfo) {
      const additionalInfo = fields.additionalInfo.value;
      if (additionalInfo && additionalInfo.length) {
        try {
          const additionalInfoJson = JSON.parse(additionalInfo);
          if (additionalInfoJson && additionalInfoJson.description) {
            entityInfo.entityDescription = additionalInfoJson.description;
          }
        } catch (e) {/**/}
      }
    }
    if (fields.queueName && fields.serviceId) {
      entityInfo.name = fields.queueName.value + '_' + fields.serviceId.value;
    }
  }
  return entityInfo;
}

export function updateDatasourceFromEntityInfo(datasource: Datasource, entity: EntityInfo, createFilter = false) {
  datasource.entity = {
    id: {
      entityType: entity.entityType,
      id: entity.id
    }
  };
  datasource.entityId = entity.id;
  datasource.entityType = entity.entityType;
  if (datasource.type === DatasourceType.entity || datasource.type === DatasourceType.entityCount
    || datasource.type === DatasourceType.alarmCount) {
    if (datasource.type === DatasourceType.entity) {
      datasource.entityName = entity.name;
      datasource.entityLabel = entity.label;
      datasource.name = entity.name;
      datasource.entityDescription = entity.entityDescription;
      datasource.entity.label = entity.label;
      datasource.entity.name = entity.name;
    }
    if (createFilter) {
      datasource.entityFilter = {
        type: AliasFilterType.singleEntity,
        singleEntity: {
          id: entity.id,
          entityType: entity.entityType
        }
      };
    }
  }
}

export const getFilterId = (filters: Filters, filterInfo: FilterInfo): string => {
  let newFilterId: string;
  for (const filterId of Object.keys(filters)) {
    if (isFilterEqual(filters[filterId], filterInfo)) {
      newFilterId = filterId;
      break;
    }
  }
  if (!newFilterId) {
    const newFilterName = createFilterName(filters, filterInfo.filter);
    newFilterId = guid();
    filters[newFilterId] = {id: newFilterId, filter: newFilterName,
      keyFilters: filterInfo.keyFilters, editable: filterInfo.editable};
  }
  return newFilterId;
}

const isFilterEqual = (filter1: FilterInfo, filter2: FilterInfo): boolean => {
  return isEqual(filter1.keyFilters, filter2.keyFilters);
}

const createFilterName = (filters: Filters, filter: string): string => {
  let c = 0;
  let newFilter = filter;
  let unique = false;
  while (!unique) {
    unique = true;
    for (const entFilterId of Object.keys(filters)) {
      const entFilter = filters[entFilterId];
      if (newFilter === entFilter.filter) {
        c++;
        newFilter = filter + c;
        unique = false;
      }
    }
  }
  return newFilter;
}
