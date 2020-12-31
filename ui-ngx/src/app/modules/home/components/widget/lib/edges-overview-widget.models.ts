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

import { NavTreeNode } from '@shared/components/nav-tree.component';
import { Datasource } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Edge } from "@shared/models/edge.models";
import { TranslateService } from "@ngx-translate/core";

export interface EntityNodeDatasource extends Datasource {
  nodeId: string;
}

export function edgeGroupsNodeText(translate: TranslateService, entityType: EntityType): string {
  const nodeIcon = materialIconByEntityType(entityType);
  const nodeText = textForEdgeGroupsType(translate, entityType);
  return nodeIcon + nodeText;
}

export function edgeNodeText(edge: Edge): string {
  const nodeIcon = materialIconByEntityType(edge.id.entityType);
  const nodeText = edge.name;
  return nodeIcon + nodeText;
}

export function materialIconByEntityType(entityType: EntityType): string {
  let materialIcon = 'insert_drive_file';
  switch (entityType) {
    case EntityType.DEVICE:
      materialIcon = 'devices_other';
      break;
    case EntityType.ASSET:
      materialIcon = 'domain';
      break;
    case EntityType.DASHBOARD:
      materialIcon = 'dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
    case EntityType.RULE_CHAIN:
      materialIcon = 'settings_ethernet';
      break;
  }
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function textForEdgeGroupsType(translate: TranslateService, entityType: EntityType): string {
  let textForEdgeGroupsType: string = '';
  switch (entityType) {
    case EntityType.DEVICE:
      textForEdgeGroupsType = 'device.devices';
      break;
    case EntityType.ASSET:
      textForEdgeGroupsType = 'asset.assets';
      break;
    case EntityType.DASHBOARD:
      textForEdgeGroupsType = 'dashboard.dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      textForEdgeGroupsType = 'entity-view.entity-views';
      break;
    case EntityType.RULE_CHAIN:
      textForEdgeGroupsType = 'rulechain.rulechains';
      break;
  }
  return translate.instant(textForEdgeGroupsType);
}

export const edgeGroupsTypes: EntityType[] = [
  EntityType.ASSET,
  EntityType.DEVICE,
  EntityType.ENTITY_VIEW,
  EntityType.DASHBOARD,
  EntityType.RULE_CHAIN
]

export interface EdgeOverviewNode extends NavTreeNode {
  data?: EdgeOverviewNodeData;
}

export type EdgeOverviewNodeData = EdgeGroupNodeData | EntityNodeData;

export interface EdgeGroupNodeData extends BaseEdgeOverviewNodeData {
  type: 'edgeGroup';
  entityType: EntityType;
  edge: Edge;
}

export interface EntityNodeData extends BaseEdgeOverviewNodeData {
  type: 'entity';
  entity: Edge;
}

export interface BaseEdgeOverviewNodeData {
  type: EdgeOverviewNodeType;
  internalId: string;
}

export type EdgeOverviewNodeType = 'entity' | 'edgeGroup';
