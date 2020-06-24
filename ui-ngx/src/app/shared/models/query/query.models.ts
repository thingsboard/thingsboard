///
/// Copyright © 2016-2020 The Thingsboard Authors
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

export enum EntityKeyType {
  ATTRIBUTE = 'ATTRIBUTE',
  CLIENT_ATTRIBUTE = 'CLIENT_ATTRIBUTE',
  SHARED_ATTRIBUTE = 'SHARED_ATTRIBUTE',
  SERVER_ATTRIBUTE = 'SERVER_ATTRIBUTE',
  TIME_SERIES = 'TIME_SERIES',
  ENTITY_FIELD = 'ENTITY_FIELD'
}

export function entityKeyTypeToDataKeyType(entityKeyType: EntityKeyType): DataKeyType {
  switch (entityKeyType) {
    case EntityKeyType.ATTRIBUTE:
    case EntityKeyType.CLIENT_ATTRIBUTE:
    case EntityKeyType.SHARED_ATTRIBUTE:
    case EntityKeyType.SERVER_ATTRIBUTE:
      return DataKeyType.attribute
    case EntityKeyType.TIME_SERIES:
      return DataKeyType.timeseries;
    case EntityKeyType.ENTITY_FIELD:
      return DataKeyType.entityField;
  }
}

export interface EntityKey {
  type: EntityKeyType;
  key: string;
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
  NOT_CONTAIN = 'NOT_CONTAIN'
}

export enum NumericOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL',
  GREATER = 'GREATER',
  LESS = 'LESS',
  GREATER_OR_EQUAL = 'GREATER_OR_EQUAL',
  LESS_OR_EQUAL = 'LESS_OR_EQUAL'
}

export enum BooleanOperation {
  EQUAL = 'EQUAL',
  NOT_EQUAL = 'NOT_EQUAL'
}

export enum ComplexOperation {
  AND = 'AND',
  OR = 'OR'
}

export interface StringFilterPredicate {
  operation: StringOperation;
  value: string;
  ignoreCase: boolean;
}

export interface NumericFilterPredicate {
  operation: NumericOperation;
  value: number;
}

export interface BooleanFilterPredicate {
  operation: BooleanOperation;
  value: boolean;
}

export interface ComplexFilterPredicate {
  operation: ComplexOperation;
  predicates: Array<KeyFilterPredicate>;
}

export type KeyFilterPredicates = StringFilterPredicate &
  NumericFilterPredicate &
  BooleanFilterPredicate &
  ComplexFilterPredicate;

export interface KeyFilterPredicate extends KeyFilterPredicates {
  type?: FilterPredicateType;
}

export interface KeyFilter {
  key: EntityKey;
  predicate: KeyFilterPredicate;
}

export interface EntityFilter extends EntityFilters {
  type?: AliasFilterType;
}

export enum Direction {
  ASC = 'ASC',
  DESC = 'DESC'
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
  }
}

export const defaultEntityDataPageLink: EntityDataPageLink = createDefaultEntityDataPageLink(1024);

export interface EntityCountQuery {
  entityFilter: EntityFilter;
}

export interface EntityDataQuery extends EntityCountQuery {
  pageLink: EntityDataPageLink;
  entityFields?: Array<EntityKey>;
  latestValues?: Array<EntityKey>;
  keyFilters?: Array<KeyFilter>;
}

export interface TsValue {
  ts: number;
  value: string;
}

export interface EntityData {
  entityId: EntityId;
  latest: {[entityKeyType: string]: {[key: string]: TsValue}};
  timeseries: {[key: string]: Array<TsValue>};
}

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
    entityInfo.entityDescription = 'TODO: Not implemented';
  }
  return entityInfo;
}
