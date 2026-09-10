// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
  negate?: boolean;
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
  entityURL?: string;
}
