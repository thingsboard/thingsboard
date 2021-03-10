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

import { BaseData } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { NavTreeNode, NodesCallback } from '@shared/components/nav-tree.component';
import { Datasource } from '@shared/models/widget.models';
import { isDefined, isUndefined } from '@core/utils';
import { EntityRelationsQuery, EntitySearchDirection, RelationTypeGroup } from '@shared/models/relation.models';
import { EntityType } from '@shared/models/entity-type.models';

export interface EntitiesHierarchyWidgetSettings {
  nodeRelationQueryFunction: string;
  nodeHasChildrenFunction: string;
  nodeOpenedFunction: string;
  nodeDisabledFunction: string;
  nodeIconFunction: string;
  nodeTextFunction: string;
  nodesSortFunction: string;
}

export interface HierarchyNodeContext {
  parentNodeCtx?: HierarchyNodeContext;
  entity: BaseData<EntityId>;
  childrenNodesLoaded?: boolean;
  level?: number;
  data: {[key: string]: any};
}

export interface HierarchyNavTreeNode extends NavTreeNode {
  data?: {
    datasource: HierarchyNodeDatasource;
    nodeCtx: HierarchyNodeContext;
    searchText?: string;
  };
}

export interface HierarchyNodeDatasource extends Datasource {
  nodeId: string;
}

export interface HierarchyNodeIconInfo {
  iconUrl?: string;
  materialIcon?: string;
}

export type NodeRelationQueryFunction = (nodeCtx: HierarchyNodeContext) => EntityRelationsQuery | 'default';
export type NodeTextFunction = (nodeCtx: HierarchyNodeContext) => string;
export type NodeDisabledFunction = (nodeCtx: HierarchyNodeContext) => boolean;
export type NodeIconFunction = (nodeCtx: HierarchyNodeContext) => HierarchyNodeIconInfo | 'default';
export type NodeOpenedFunction = (nodeCtx: HierarchyNodeContext) => boolean;
export type NodeHasChildrenFunction = (nodeCtx: HierarchyNodeContext) => boolean;
export type NodesSortFunction = (nodeCtx1: HierarchyNodeContext, nodeCtx2: HierarchyNodeContext) => number;

export function loadNodeCtxFunction<F extends (...args: any[]) => any>(functionBody: string, argNames: string, ...args: any[]): F {
  let nodeCtxFunction: F = null;
  if (isDefined(functionBody) && functionBody.length) {
    try {
      nodeCtxFunction = new Function(argNames, functionBody) as F;
      const res = nodeCtxFunction.apply(null, args);
      if (isUndefined(res)) {
        nodeCtxFunction = null;
      }
    } catch (e) {
      nodeCtxFunction = null;
    }
  }
  return nodeCtxFunction;
}

export function materialIconHtml(materialIcon: string): string {
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function iconUrlHtml(iconUrl: string): string {
  return '<div class="node-icon" style="background-image: url(' + iconUrl + ');">&nbsp;</div>';
}

export const defaultNodeRelationQueryFunction: NodeRelationQueryFunction = nodeCtx => {
  const entity = nodeCtx.entity;
  const query: EntityRelationsQuery = {
    parameters: {
      rootId: entity.id.id,
      rootType: entity.id.entityType as EntityType,
      direction: EntitySearchDirection.FROM,
      relationTypeGroup: RelationTypeGroup.COMMON,
      maxLevel: 1
    },
    filters: [
      {
        relationType: 'Contains',
        entityTypes: []
      }
    ]
  };
  return query;
};

export const defaultNodeIconFunction: NodeIconFunction = nodeCtx => {
  let materialIcon = 'insert_drive_file';
  const entity = nodeCtx.entity;
  if (entity && entity.id && entity.id.entityType) {
    switch (entity.id.entityType as EntityType | string) {
      case 'function':
        materialIcon = 'functions';
        break;
      case EntityType.DEVICE:
        materialIcon = 'devices_other';
        break;
      case EntityType.ASSET:
        materialIcon = 'domain';
        break;
      case EntityType.TENANT:
        materialIcon = 'supervisor_account';
        break;
      case EntityType.CUSTOMER:
        materialIcon = 'supervisor_account';
        break;
      case EntityType.USER:
        materialIcon = 'account_circle';
        break;
      case EntityType.DASHBOARD:
        materialIcon = 'dashboards';
        break;
      case EntityType.ALARM:
        materialIcon = 'notifications_active';
        break;
      case EntityType.ENTITY_VIEW:
        materialIcon = 'view_quilt';
        break;
    }
  }
  return {
    materialIcon
  };
};

export const defaultNodeOpenedFunction: NodeOpenedFunction = nodeCtx => {
  return nodeCtx.level <= 4;
};

export const defaultNodesSortFunction: NodesSortFunction = (nodeCtx1, nodeCtx2) => {
  let result = 0;
  if (!nodeCtx1.entity.id.entityType || !nodeCtx2.entity.id.entityType ) {
    if (nodeCtx1.entity.id.entityType) {
      result = 1;
    } else if (nodeCtx2.entity.id.entityType) {
      result = -1;
    }
  } else {
    result = nodeCtx1.entity.id.entityType.localeCompare(nodeCtx2.entity.id.entityType);
  }
  if (result === 0) {
    result = nodeCtx1.entity.name.localeCompare(nodeCtx2.entity.name);
  }
  return result;
};
