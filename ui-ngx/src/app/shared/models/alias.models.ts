// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitySearchDirection, RelationEntityTypeFilter } from '@shared/models/relation.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { guid, isEqual } from '@core/utils';

export enum AliasFilterType {
  singleEntity = 'singleEntity',
  entityList = 'entityList',
  entityName = 'entityName',
  entityType = 'entityType',
  stateEntity = 'stateEntity',
  assetType = 'assetType',
  deviceType = 'deviceType',
  edgeType = 'edgeType',
  entityViewType = 'entityViewType',
  apiUsageState = 'apiUsageState',
  relationsQuery = 'relationsQuery',
  assetSearchQuery = 'assetSearchQuery',
  deviceSearchQuery = 'deviceSearchQuery',
  edgeSearchQuery = 'edgeSearchQuery',
  entityViewSearchQuery = 'entityViewSearchQuery'
}

export const edgeAliasFilterTypes = new Array<string>(
  AliasFilterType.edgeType,
  AliasFilterType.edgeSearchQuery
);

export const aliasFilterTypeTranslationMap = new Map<AliasFilterType, string>(
  [
    [ AliasFilterType.singleEntity, 'alias.filter-type-single-entity' ],
    [ AliasFilterType.entityList, 'alias.filter-type-entity-list' ],
    [ AliasFilterType.entityName, 'alias.filter-type-entity-name' ],
    [ AliasFilterType.entityType, 'alias.filter-type-entity-type' ],
    [ AliasFilterType.stateEntity, 'alias.filter-type-state-entity' ],
    [ AliasFilterType.assetType, 'alias.filter-type-asset-type' ],
    [ AliasFilterType.deviceType, 'alias.filter-type-device-type' ],
    [ AliasFilterType.edgeType, 'alias.filter-type-edge-type' ],
    [ AliasFilterType.entityViewType, 'alias.filter-type-entity-view-type' ],
    [ AliasFilterType.apiUsageState, 'alias.filter-type-apiUsageState' ],
    [ AliasFilterType.relationsQuery, 'alias.filter-type-relations-query' ],
    [ AliasFilterType.assetSearchQuery, 'alias.filter-type-asset-search-query' ],
    [ AliasFilterType.deviceSearchQuery, 'alias.filter-type-device-search-query' ],
    [ AliasFilterType.edgeSearchQuery, 'alias.filter-type-edge-search-query' ],
    [ AliasFilterType.entityViewSearchQuery, 'alias.filter-type-entity-view-search-query' ]
  ]
);

export interface SingleEntityFilter {
  singleEntity?: EntityId;
}

export interface EntityListFilter {
  entityType?: EntityType;
  entityList?: string[];
}

export interface EntityNameFilter {
  entityType?: EntityType;
  entityNameFilter?: string;
}

export interface EntityTypeFilter {
  entityType?: EntityType;
}

export interface StateEntityFilter {
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
}

export interface AssetTypeFilter {
  /**
   * @deprecated
   */
  assetType?: string;
  assetTypes?: string[];
  assetNameFilter?: string;
}

export interface DeviceTypeFilter {
  /**
   * @deprecated
   */
  deviceType?: string;
  deviceTypes?: string[];
  deviceNameFilter?: string;
}

export interface EdgeTypeFilter {
  /**
   * @deprecated
   */
  edgeType?: string;
  edgeTypes?: string[];
  edgeNameFilter?: string;
}

export interface EntityViewFilter {
  /**
   * @deprecated
   */
  entityViewType?: string;
  entityViewTypes?: string[];
  entityViewNameFilter?: string;
}

export interface RelationsQueryFilter {
  rootStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  rootEntity?: EntityId;
  direction?: EntitySearchDirection;
  filters?: Array<RelationEntityTypeFilter>;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

export interface EntitySearchQueryFilter {
  rootStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  rootEntity?: EntityId;
  relationType?: string;
  direction?: EntitySearchDirection;
  maxLevel?: number;
  fetchLastLevelOnly?: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface ApiUsageStateFilter {

}

export interface AssetSearchQueryFilter extends EntitySearchQueryFilter {
  assetTypes?: string[];
}

export interface DeviceSearchQueryFilter extends EntitySearchQueryFilter {
  deviceTypes?: string[];
}

export interface EdgeSearchQueryFilter extends EntitySearchQueryFilter {
  edgeTypes?: string[];
}

export interface EntityViewSearchQueryFilter extends EntitySearchQueryFilter {
  entityViewTypes?: string[];
}

export type EntityFilters =
  SingleEntityFilter &
  EntityListFilter &
  EntityNameFilter &
  EntityTypeFilter &
  StateEntityFilter &
  AssetTypeFilter &
  DeviceTypeFilter &
  EdgeTypeFilter &
  EntityViewFilter &
  RelationsQueryFilter &
  AssetSearchQueryFilter &
  DeviceSearchQueryFilter &
  EntityViewSearchQueryFilter &
  EntitySearchQueryFilter &
  EdgeSearchQueryFilter;

export interface EntityAliasFilter extends EntityFilters {
  type?: AliasFilterType;
  resolveMultiple?: boolean;
}

export interface EntityAliasInfo {
  alias: string;
  filter: EntityAliasFilter;
  [key: string]: any;
}

export interface AliasesInfo {
  datasourceAliases: {[datasourceIndex: number]: EntityAliasInfo};
  targetDeviceAlias: EntityAliasInfo;
}

export interface EntityAlias extends EntityAliasInfo {
  id: string;
}

export interface EntityAliases {
  [id: string]: EntityAlias;
}

export interface EntityAliasFilterResult {
  stateEntity: boolean;
  entityFilter: EntityFilter;
  entityParamName?: string;
}

export const getEntityAliasId = (entityAliases: EntityAliases, aliasInfo: EntityAliasInfo): string => {
  let newAliasId: string;
  for (const aliasId of Object.keys(entityAliases)) {
    if (isEntityAliasEqual(entityAliases[aliasId], aliasInfo)) {
      newAliasId = aliasId;
      break;
    }
  }
  if (!newAliasId) {
    const newAliasName = createEntityAliasName(entityAliases, aliasInfo.alias);
    newAliasId = guid();
    entityAliases[newAliasId] = {id: newAliasId, alias: newAliasName, filter: aliasInfo.filter};
  }
  return newAliasId;
}

const isEntityAliasEqual = (alias1: EntityAliasInfo, alias2: EntityAliasInfo): boolean => {
  return isEqual(alias1.filter, alias2.filter);
}

const createEntityAliasName = (entityAliases: EntityAliases, alias: string): string => {
  let c = 0;
  let newAlias = alias;
  let unique = false;
  while (!unique) {
    unique = true;
    for (const entAliasId of Object.keys(entityAliases)) {
      const entAlias = entityAliases[entAliasId];
      if (newAlias === entAlias.alias) {
        c++;
        newAlias = alias + c;
        unique = false;
      }
    }
  }
  return newAlias;
}
