// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NavTreeNode } from '@shared/components/nav-tree.component';
import { Datasource } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { BaseData, HasId } from '@shared/models/base-data';

export interface EntityNodeDatasource extends Datasource {
  nodeId: string;
}

export interface EdgeOverviewNode extends NavTreeNode {
  data?: BaseEdgeNodeData;
}

export interface BaseEdgeNodeData {
  type: EdgeNodeType;
  group: BaseData<HasId>;
  groupType: EntityType;
}

export type EdgeNodeType = 'group' | 'groups';

export interface EntityGroupNodeData extends BaseEdgeNodeData {
  type: 'group';
}

export interface EntityGroupsNodeData extends BaseEdgeNodeData {
  type: 'groups';
  edge?: BaseData<HasId>;
}

export function edgeGroupsNodeText(translate: TranslateService, entityType: EntityType): string {
  const nodeIcon = materialIconByEntityType(entityType);
  const nodeText = textForEntityGroupsType(translate, entityType);
  return nodeIcon + nodeText;
}

export function entityGroupNodeText(entity: any): string {
  const nodeIcon = materialIconByEntityType(entity.type);
  const nodeText = entity.name;
  return nodeIcon + nodeText;
}

export function entityNodeText(entity: any): string {
  const nodeIcon = materialIconByEntityType(entity.id.entityType);
  const nodeText = entity.name;
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
    case EntityType.USER:
      materialIcon = 'account_circle';
      break;
    case EntityType.DASHBOARD:
      materialIcon = 'dashboards';
      break;
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
    case EntityType.SCHEDULER_EVENT:
      materialIcon = 'schedule';
      break;
    case EntityType.RULE_CHAIN:
      materialIcon = 'settings_ethernet';
      break;
    case EntityType.INTEGRATION:
      materialIcon = 'input';
      break;
  }
  return '<mat-icon class="node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}

export function textForEntityGroupsType(translate: TranslateService, entityType: EntityType): string {
  let textForEntityGroupsType: string = '';
  switch (entityType) {
    case EntityType.USER:
      return translate.instant('entity-group.user-groups');
    case EntityType.ASSET:
      return translate.instant('entity-group.asset-groups');
    case EntityType.DEVICE:
      return translate.instant('entity-group.device-groups');
    case EntityType.ENTITY_VIEW:
      return translate.instant('entity-group.entity-view-groups');
    case EntityType.DASHBOARD:
      return translate.instant('entity-group.dashboard-groups');
    case EntityType.SCHEDULER_EVENT:
      return translate.instant('entity.type-scheduler-events');
    case EntityType.RULE_CHAIN:
      return translate.instant('entity.type-rulechains');
    case EntityType.INTEGRATION:
      return translate.instant('entity.type-integrations');
  }
  return translate.instant(textForEntityGroupsType);
}
