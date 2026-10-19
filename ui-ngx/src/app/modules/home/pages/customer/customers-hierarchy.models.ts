// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Customer } from '@shared/models/customer.model';
import { EntityGroupInfo } from '@shared/models/entity-group.models';
import { EntityType } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { NavTreeNode } from '@shared/components/nav-tree.component';
import { Edge } from '@shared/models/edge.models';
import { EntityId } from '@shared/models/id/entity-id';

export type CustomersHierarchyViewMode = 'groups' | 'group' | 'scheduler';

export type CustomersHierarchyNodeType = 'group' | 'groups' | 'customer' | 'edge' | 'edgeEntities' | 'edgeGroup';

export interface BaseCustomersHierarchyNodeData {
  type: CustomersHierarchyNodeType;
  parentEntityGroupId: string;
  internalId: string;
}

export interface EntityGroupNodeData extends BaseCustomersHierarchyNodeData {
  type: 'group';
  entity: EntityGroupInfo;
}

export interface EntityGroupsNodeData extends BaseCustomersHierarchyNodeData {
  type: 'groups';
  groupsType: EntityType;
  customer: Customer;
}

export interface CustomerNodeData extends BaseCustomersHierarchyNodeData {
  type: 'customer';
  entity: Customer;
}

export interface EdgeNodeCustomerData {
  customerGroupId?: string;
  customerId?: string;
}

interface BaseEdgeNodeData {
  edge: Edge;
  edgeId?: Edge;
  customerData?: EdgeNodeCustomerData;
}

export interface EdgeNodeData extends BaseCustomersHierarchyNodeData, BaseEdgeNodeData {
  type: 'edge';
}

export interface EdgeEntityGroupsNodeData extends BaseCustomersHierarchyNodeData, BaseEdgeNodeData {
  type: 'edgeEntities';
  entityType: EntityType;
}

export interface EdgeEntityGroupNodeData extends BaseCustomersHierarchyNodeData, BaseEdgeNodeData {
  type: 'edgeGroup';
  entityGroup: EntityGroupInfo;
}

export type CustomersHierarchyNodeData = EntityGroupNodeData | EntityGroupsNodeData | CustomerNodeData |
                                         EdgeNodeData | EdgeEntityGroupsNodeData | EdgeEntityGroupNodeData;

export interface CustomersHierarchyNode extends NavTreeNode {
  data?: CustomersHierarchyNodeData;
}

export function entityGroupNodeText(entityGroup: EntityGroupInfo): string {
  const nodeIcon = materialIconByEntityType(entityGroup.type);
  return nodeIcon + entityGroup.name;
}

export function customerNodeText(customer: Customer): string {
  const nodeIcon = materialIconByEntityType(EntityType.CUSTOMER);
  return nodeIcon + customer.title;
}

export function edgeNodeText(edge: Edge): string {
  const nodeIcon = materialIconByEntityType(EntityType.EDGE);
  return nodeIcon + edge.name;
}

export function entityGroupsNodeText(translate: TranslateService, groupType: EntityType) {
  const nodeIcon = materialIconByEntityType(groupType);
  const nodeText = textForGroupType(translate, groupType);
  return nodeIcon + nodeText;
}

export function entitiesNodeText(translate: TranslateService, entityType: EntityType, name: string): string {
  const nodeIcon = materialIconByEntityType(entityType);
  const nodeText = translate.instant(name);
  return nodeIcon + nodeText;
}

function textForGroupType(translate: TranslateService, groupType: EntityType): string {
  switch (groupType) {
    case EntityType.USER:
      return translate.instant('entity-group.user-groups');
    case EntityType.CUSTOMER:
      return translate.instant('entity-group.customer-groups');
    case EntityType.ASSET:
      return translate.instant('entity-group.asset-groups');
    case EntityType.DEVICE:
      return translate.instant('entity-group.device-groups');
    case EntityType.ENTITY_VIEW:
      return translate.instant('entity-group.entity-view-groups');
    case EntityType.EDGE:
      return translate.instant('entity-group.edge-groups');
    case EntityType.DASHBOARD:
      return translate.instant('entity-group.dashboard-groups');
  }
  return '';
}

function materialIconByEntityType (entityType: EntityType): string {
  let materialIcon = 'insert_drive_file';
  switch (entityType) {
    case EntityType.DEVICE:
      materialIcon = 'devices_other';
      break;
    case EntityType.ASSET:
      materialIcon = 'domain';
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
    case EntityType.ENTITY_VIEW:
      materialIcon = 'view_quilt';
      break;
    case EntityType.EDGE:
      materialIcon = 'router';
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
  return '<mat-icon class="mat-icon node-icon material-icons" role="img" aria-hidden="false">' + materialIcon + '</mat-icon>';
}
