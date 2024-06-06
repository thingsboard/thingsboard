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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { HasTenantId } from '@shared/models/entity.models';
import { AlarmRuleId } from '@shared/models/id/alarm-rule-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmSeverity } from '@shared/models/alarm.models';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { AlarmConditionSpec, AlarmScheduleType, CustomTimeSchedulerItem } from '@shared/models/device.models';

export interface AlarmRuleInfo extends Omit<BaseData<AlarmRuleId>, 'label'>, HasTenantId {
  tenantId: TenantId;
  alarmType: string;
  enabled: boolean;
  description: string;
}

export interface AlarmRule extends AlarmRuleInfo, ExportableEntity<AlarmRuleId> {
  configuration: AlarmRuleConfiguration;
}

export interface AlarmRuleConfiguration {
  sourceEntityFilters: AlarmRuleEntityFilter;
  arguments: AlarmRuleArguments;
  createRules: {[key in AlarmSeverity]: AlarmRuleCondition};
  clearRule: AlarmRuleCondition;
  propagate: boolean;
  propagateToOwner: boolean;
  propagateToTenant: boolean;
  propagateRelationTypes: Array<string>;
}

export type AlarmRuleArguments = {
  [key: string]: AlarmRuleArgument;
};

export enum AlarmRuleEntityFilterType {
  SINGLE_ENTITY = 'SINGLE_ENTITY',
  DEVICE_TYPE = 'DEVICE_TYPE',
  ASSET_TYPE = 'ASSET_TYPE',
  ENTITY_LIST = 'ENTITY_LIST',
  ALL_DEVICES = 'ALL_DEVICES',
  ALL_ASSETS = 'ALL_ASSETS'
}

export type AlarmRuleEntityFilter = AlarmRuleSingleEntityFilter | AlarmRuleDeviceTypeFilter | AlarmRuleAssetTypeFilter |
  AlarmRuleEntityListFilter | AlarmRuleAllDevicesFilter | AlarmRuleAllAssetsFilter;

interface AlarmRuleFilterTypes<T extends AlarmRuleEntityFilterType> {
  type: T;
}

export interface AlarmRuleSingleEntityFilter extends AlarmRuleFilterTypes<AlarmRuleEntityFilterType.SINGLE_ENTITY> {
  entityId: EntityId;
}

export interface AlarmRuleDeviceTypeFilter extends AlarmRuleFilterTypes<AlarmRuleEntityFilterType.DEVICE_TYPE> {
  deviceProfileIds: Array<DeviceProfileId>;
}

export interface AlarmRuleAssetTypeFilter extends AlarmRuleFilterTypes<AlarmRuleEntityFilterType.ASSET_TYPE> {
  assetProfileIds: Array<AssetProfileId>;
}

export interface AlarmRuleEntityListFilter extends AlarmRuleFilterTypes<AlarmRuleEntityFilterType.ENTITY_LIST> {
  entityType: EntityType;
  entityIds: Array<EntityId>;
}

export type AlarmRuleAllDevicesFilter = AlarmRuleFilterTypes<AlarmRuleEntityFilterType.ALL_DEVICES>;
export type AlarmRuleAllAssetsFilter = AlarmRuleFilterTypes<AlarmRuleEntityFilterType.ALL_ASSETS>;

export enum AlarmRuleArgumentType {
  FROM_MESSAGE = 'FROM_MESSAGE',
  CONSTANT = 'CONSTANT',
  ATTRIBUTE = 'ATTRIBUTE'
}

interface AlarmRuleArgumentTypes<T extends AlarmRuleArgumentType> {
  type: T;
  valueType: ArgumentValueType;
}
export type AlarmRuleArgument = AlarmRuleFromMessageArgument | AlarmRuleConstantArgument | AlarmRuleAttributeArgument;

export interface AlarmRuleFromMessageArgument extends AlarmRuleArgumentTypes<AlarmRuleArgumentType.FROM_MESSAGE> {
  key: AlarmConditionFilterKey;
}

export interface AlarmRuleConstantArgument extends AlarmRuleArgumentTypes<AlarmRuleArgumentType.CONSTANT> {
  description: string;
  value: string | number | boolean | Date;
}

export interface AlarmRuleAttributeArgument extends AlarmRuleArgumentTypes<AlarmRuleArgumentType.ATTRIBUTE> {
  attribute: string;
  defaultValue: string | number | boolean | Date;
  // sourceType: ValueSourceType;
  inherit: boolean;
}

export interface AlarmRuleCondition {
  alarmCondition: AlarmCondition;
  schedule: AlarmSchedule;
  alarmDetails: string;
  dashboardId: DashboardId;
}

interface AlarmCondition {
  conditionFilter: AlarmConditionFilter;
  spec: AlarmConditionSpec;
}

enum AlarmConditionFilterType {
  SIMPLE = 'COMPLEX',
  COMPLEX = 'COMPLEX'
}

export type AlarmConditionFilter = SimpleAlarmConditionFilter | ComplexAlarmConditionFilter;

interface AlarmConditionFilterTypes<T extends AlarmConditionFilterType> {
  type: T;
}

export interface SimpleAlarmConditionFilter extends AlarmConditionFilterTypes<AlarmConditionFilterType.SIMPLE> {
  leftArgId: string;
  rightArgId: string;
  operation: ArgumentOperation;
  ignoreCase?: boolean;
}

export interface ComplexAlarmConditionFilter extends AlarmConditionFilterTypes<AlarmConditionFilterType.COMPLEX> {
  conditions: AlarmConditionFilter[];
  operation: ComplexOperation;
}

enum ComplexOperation {
  AND = 'AND',
  OR = 'OR'
}

type AlarmSchedule = AnyTimeSchedule | SpecificTimeSchedule | CustomTimeSchedule;

export interface AlarmScheduleTypes<T extends AlarmScheduleType> {
  type: T;
  argumentId?: string;
}

export type AnyTimeSchedule = AlarmScheduleTypes<AlarmScheduleType.ANY_TIME>;

export interface SpecificTimeSchedule extends AlarmScheduleTypes<AlarmScheduleType.SPECIFIC_TIME> {
  timezone: string;
  daysOfWeek: number[];
  startsOn: number;
  endsOn: number;
}

export interface CustomTimeSchedule extends AlarmScheduleTypes<AlarmScheduleType.CUSTOM> {
  timezone: string;
  items?: CustomTimeSchedulerItem[];
}

// export enum ValueSourceType {
//   CURRENT_TENANT = 'CURRENT_TENANT',
//   CURRENT_CUSTOMER = 'CURRENT_CUSTOMER',
//   CURRENT_ENTITY = 'CURRENT_ENTITY'
// }

interface AlarmConditionFilterKey {
  key: string;
  type: AlarmConditionKeyType;
}

enum AlarmConditionKeyType {
  ATTRIBUTE = 'ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES',
  CONSTANT = 'CONSTANT'
}

enum ArgumentValueType {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  DATE_TIME = 'DATE_TIME',
  BOOLEAN = 'BOOLEAN'
}

enum ArgumentOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  IN = 'IN',
  NOT_IN = 'NOT_IN',
  GREATER = 'GREATER',
  GREATER_OR_EQUAL = 'GREATER_OR_EQUAL',
  LESS = 'LESS',
  LESS_OR_EQUAL = 'LESS_OR_EQUAL'
}

export const AllowArgumentOperationForArgumentType = new Map<ArgumentValueType, Array<ArgumentOperation>>([[
    ArgumentValueType.STRING, [
      ArgumentOperation.EQUAL,
      ArgumentOperation.NOT_EQUAL,
      ArgumentOperation.STARTS_WITH,
      ArgumentOperation.ENDS_WITH,
      ArgumentOperation.CONTAINS,
      ArgumentOperation.NOT_CONTAINS,
      ArgumentOperation.IN,
      ArgumentOperation.NOT_IN
    ]], [
    ArgumentValueType.NUMERIC, [
      ArgumentOperation.EQUAL,
      ArgumentOperation.NOT_EQUAL,
      ArgumentOperation.GREATER,
      ArgumentOperation.GREATER_OR_EQUAL,
      ArgumentOperation.LESS,
      ArgumentOperation.LESS_OR_EQUAL
    ]], [
    ArgumentValueType.DATE_TIME, [
      ArgumentOperation.EQUAL,
      ArgumentOperation.NOT_EQUAL,
      ArgumentOperation.GREATER,
      ArgumentOperation.GREATER_OR_EQUAL,
      ArgumentOperation.LESS,
      ArgumentOperation.LESS_OR_EQUAL
    ]], [
    ArgumentValueType.BOOLEAN, [
      ArgumentOperation.EQUAL,
      ArgumentOperation.NOT_EQUAL,
    ]]
  ]
);
