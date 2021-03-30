///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export const CONTAINS_TYPE = 'Contains';
export const MANAGES_TYPE = 'Manages';

export const RelationTypes = [
  CONTAINS_TYPE,
  MANAGES_TYPE
];

export enum RelationTypeGroup {
  COMMON = 'COMMON',
  ALARM = 'ALARM',
  DASHBOARD = 'DASHBOARD',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
}

export enum EntitySearchDirection {
  FROM = 'FROM',
  TO = 'TO'
}

export const entitySearchDirectionTranslations = new Map<EntitySearchDirection, string>(
  [
    [EntitySearchDirection.FROM, 'relation.search-direction.FROM'],
    [EntitySearchDirection.TO, 'relation.search-direction.TO'],
  ]
);

export const directionTypeTranslations = new Map<EntitySearchDirection, string>(
  [
    [EntitySearchDirection.FROM, 'relation.direction-type.FROM'],
    [EntitySearchDirection.TO, 'relation.direction-type.TO'],
  ]
);

export interface RelationEntityTypeFilter {
  relationType: string;
  entityTypes: Array<EntityType>;
}

export interface RelationsSearchParameters {
  rootId: string;
  rootType: EntityType;
  direction: EntitySearchDirection;
  relationTypeGroup?: RelationTypeGroup;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

export interface EntityRelationsQuery {
  parameters: RelationsSearchParameters;
  filters: Array<RelationEntityTypeFilter>;
}

export interface EntitySearchQuery {
  parameters: RelationsSearchParameters;
  relationType: string;
}

export interface EntityRelation {
  from: EntityId;
  to: EntityId;
  type: string;
  typeGroup: RelationTypeGroup;
  additionalInfo?: any;
}

export interface EntityRelationInfo extends EntityRelation {
  fromName: string;
  toEntityTypeName?: string;
  toName: string;
  fromEntityTypeName?: string;
}
