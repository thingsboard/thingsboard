// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
  FROM_ENTITY_GROUP = 'FROM_ENTITY_GROUP'
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

export const ALL_RELATIONS_DIRECTION = 'ALL';

export type RelationsTableDirection = EntitySearchDirection | typeof ALL_RELATIONS_DIRECTION;

export const relationDirectionTranslations = new Map<EntitySearchDirection, string>(
  [
    [EntitySearchDirection.FROM, 'relation.outbound'],
    [EntitySearchDirection.TO, 'relation.inbound'],
  ]
);

export const relationsTableDirectionTranslations = new Map<RelationsTableDirection, string>(
  [
    [ALL_RELATIONS_DIRECTION, 'relation.all'],
    ...relationDirectionTranslations
  ]
);

export const relationsTableEntityTypeHeaderTranslations = new Map<RelationsTableDirection, string>(
  [
    [ALL_RELATIONS_DIRECTION, 'relation.entity-type'],
    [EntitySearchDirection.FROM, 'relation.to-entity-type'],
    [EntitySearchDirection.TO, 'relation.from-entity-type'],
  ]
);

export const relationsTableEntityNameHeaderTranslations = new Map<RelationsTableDirection, string>(
  [
    [ALL_RELATIONS_DIRECTION, 'relation.entity-name'],
    [EntitySearchDirection.FROM, 'relation.to-entity-name'],
    [EntitySearchDirection.TO, 'relation.from-entity-name'],
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
  toName: string;
  entityURL?: string;
  direction?: EntitySearchDirection;
  directionName?: string;
  entityTypeName?: string;
  entityName?: string;
}
