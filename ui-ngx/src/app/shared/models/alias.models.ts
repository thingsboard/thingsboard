// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitySearchDirection, RelationEntityTypeFilter } from '@shared/models/relation.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { guid, isEqual } from '@core/utils';

export enum AliasFilterType {
  singleEntity = 'singleEntity',
  entityGroup = 'entityGroup',
  entityList = 'entityList',
  entityName = 'entityName',
  entityType = 'entityType',
  entityGroupList = 'entityGroupList',
  entityGroupName = 'entityGroupName',
  entitiesByGroupName = 'entitiesByGroupName',
  stateEntity = 'stateEntity',
  stateEntityOwner = 'stateEntityOwner',
  assetType = 'assetType',
  deviceType = 'deviceType',
  entityViewType = 'entityViewType',
  edgeType = 'edgeType',
  apiUsageState = 'apiUsageState',
  relationsQuery = 'relationsQuery',
  assetSearchQuery = 'assetSearchQuery',
  deviceSearchQuery = 'deviceSearchQuery',
  entityViewSearchQuery = 'entityViewSearchQuery',
  edgeSearchQuery = 'edgeSearchQuery',
  schedulerEvent = 'schedulerEvent'
}

export const edgeAliasFilterTypes = new Array<string>(
  AliasFilterType.edgeType,
  AliasFilterType.edgeSearchQuery
);

export const aliasFilterTypeTranslationMap = new Map<AliasFilterType, string>(
  [
    [ AliasFilterType.singleEntity, 'alias.filter-type-single-entity' ],
    [ AliasFilterType.entityGroup, 'alias.filter-type-entity-group' ],
    [ AliasFilterType.entityList, 'alias.filter-type-entity-list' ],
    [ AliasFilterType.entityName, 'alias.filter-type-entity-name' ],
    [ AliasFilterType.entityType, 'alias.filter-type-entity-type' ],
    [ AliasFilterType.entityGroupList, 'alias.filter-type-entity-group-list' ],
    [ AliasFilterType.entityGroupName, 'alias.filter-type-entity-group-name' ],
    [ AliasFilterType.entitiesByGroupName, 'alias.filter-type-entities-by-group-name' ],
    [ AliasFilterType.stateEntity, 'alias.filter-type-state-entity' ],
    [ AliasFilterType.stateEntityOwner, 'alias.filter-type-state-entity-owner' ],
    [ AliasFilterType.assetType, 'alias.filter-type-asset-type' ],
    [ AliasFilterType.deviceType, 'alias.filter-type-device-type' ],
    [ AliasFilterType.entityViewType, 'alias.filter-type-entity-view-type' ],
    [ AliasFilterType.edgeType, 'alias.filter-type-edge-type' ],
    [ AliasFilterType.apiUsageState, 'alias.filter-type-apiUsageState' ],
    [ AliasFilterType.relationsQuery, 'alias.filter-type-relations-query' ],
    [ AliasFilterType.assetSearchQuery, 'alias.filter-type-asset-search-query' ],
    [ AliasFilterType.deviceSearchQuery, 'alias.filter-type-device-search-query' ],
    [ AliasFilterType.entityViewSearchQuery, 'alias.filter-type-entity-view-search-query' ],
    [ AliasFilterType.edgeSearchQuery, 'alias.filter-type-edge-search-query' ],
    [ AliasFilterType.schedulerEvent, 'alias.filter-type-scheduler-event' ]
  ]
);

const reportAliasFilterTypeTranslationMap = new Map(aliasFilterTypeTranslationMap);
reportAliasFilterTypeTranslationMap.set(AliasFilterType.stateEntity, 'alias.filter-type-state-entity-originator');
reportAliasFilterTypeTranslationMap.set(AliasFilterType.stateEntityOwner, 'alias.filter-type-state-entity-owner-originator');
export { reportAliasFilterTypeTranslationMap };

const subReportAliasFilterTypeTranslationMap = new Map(aliasFilterTypeTranslationMap);
subReportAliasFilterTypeTranslationMap.set(AliasFilterType.stateEntity, 'alias.filter-type-state-entity-master-report');
subReportAliasFilterTypeTranslationMap.set(AliasFilterType.stateEntityOwner, 'alias.filter-type-state-entity-owner-master-report');
export { subReportAliasFilterTypeTranslationMap };

export interface SingleEntityFilter {
  singleEntity?: EntityId;
}

export interface EntityGroupFilter {
  groupStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateGroupType?: EntityType;
  defaultStateEntityGroup?: string;
  groupType?: EntityType;
  entityGroup?: string;
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

export interface EntityGroupListFilter {
  groupType?: EntityType;
  entityGroupList?: string[];
}

export interface EntityGroupNameFilter {
  groupType?: EntityType;
  entityGroupNameFilter?: string;
}

export interface EntitiesByGroupNameFilter {
  groupStateEntity?: boolean;
  stateEntityParamName?: string;
  groupType?: EntityType;
  ownerId?: EntityId;
  entityGroupNameFilter?: string;
}

export interface StateEntityFilter {
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
}

export interface StateEntityOwnerFilter {
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

export interface EntityViewSearchQueryFilter extends EntitySearchQueryFilter {
  entityViewTypes?: string[];
}

export interface EdgeSearchQueryFilter extends EntitySearchQueryFilter {
  edgeTypes?: string[];
}

export interface SchedulerEventFilter {
  originatorStateEntity?: boolean;
  stateEntityParamName?: string;
  defaultStateEntity?: EntityId;
  originator?: EntityId;
  eventType?: string;
}

export type EntityFilters =
  SingleEntityFilter &
  EntityGroupFilter &
  EntityListFilter &
  EntityNameFilter &
  EntityTypeFilter &
  EntityGroupListFilter &
  EntityGroupNameFilter &
  EntitiesByGroupNameFilter &
  StateEntityFilter &
  StateEntityOwnerFilter &
  AssetTypeFilter &
  DeviceTypeFilter &
  EntityViewFilter &
  EdgeTypeFilter &
  RelationsQueryFilter &
  AssetSearchQueryFilter &
  DeviceSearchQueryFilter &
  EntityViewSearchQueryFilter &
  EntitySearchQueryFilter &
  EdgeSearchQueryFilter &
  SchedulerEventFilter;

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
