// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityGroupId } from '@shared/models/id/entity-group-id';

export enum RoleType {
  GENERIC = 'GENERIC',
  GROUP = 'GROUP'
}

export const roleTypeTranslationMap = new Map<RoleType, string>(
  [
    [RoleType.GENERIC, 'role.display-type.GENERIC'],
    [RoleType.GROUP, 'role.display-type.GROUP'],
  ]
);

export enum Operation {
  ALL = 'ALL',
  CREATE = 'CREATE',
  READ = 'READ',
  WRITE = 'WRITE',
  DELETE = 'DELETE',
  RPC_CALL = 'RPC_CALL',
  READ_CREDENTIALS = 'READ_CREDENTIALS',
  WRITE_CREDENTIALS = 'WRITE_CREDENTIALS',
  READ_ATTRIBUTES = 'READ_ATTRIBUTES',
  WRITE_ATTRIBUTES = 'WRITE_ATTRIBUTES',
  READ_TELEMETRY = 'READ_TELEMETRY',
  WRITE_TELEMETRY = 'WRITE_TELEMETRY',
  ADD_TO_GROUP = 'ADD_TO_GROUP',
  REMOVE_FROM_GROUP = 'REMOVE_FROM_GROUP',
  CHANGE_OWNER = 'CHANGE_OWNER',
  IMPERSONATE = 'IMPERSONATE',
  CLAIM_DEVICES = 'CLAIM_DEVICES',
  SHARE_GROUP = 'SHARE_GROUP',
  ASSIGN_TO_TENANT = 'ASSIGN_TO_TENANT',
  READ_CALCULATED_FIELD = 'READ_CALCULATED_FIELD',
  WRITE_CALCULATED_FIELD = 'WRITE_CALCULATED_FIELD'
}

const operationTypeTranslations = new Map<Operation, string>();
for (const key of Object.keys(Operation)) {
  operationTypeTranslations.set(Operation[key], `permission.operation.display-type.${key}`);
}
export const operationTypeTranslationMap = operationTypeTranslations;

export enum Resource {
  ALL = 'ALL',
  PROFILE = 'PROFILE',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  ALARM = 'ALARM',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  ASSET = 'ASSET',
  CUSTOMER = 'CUSTOMER',
  DASHBOARD = 'DASHBOARD',
  ENTITY_VIEW = 'ENTITY_VIEW',
  TENANT = 'TENANT',
  TENANT_PROFILE = 'TENANT_PROFILE',
  RULE_CHAIN = 'RULE_CHAIN',
  USER = 'USER',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  CONVERTER = 'CONVERTER',
  INTEGRATION = 'INTEGRATION',
  SCHEDULER_EVENT = 'SCHEDULER_EVENT',
  BLOB_ENTITY = 'BLOB_ENTITY',
  REPORT_TEMPLATE = 'REPORT_TEMPLATE',
  REPORT = 'REPORT',
  CUSTOMER_GROUP = 'CUSTOMER_GROUP',
  DEVICE_GROUP = 'DEVICE_GROUP',
  ASSET_GROUP = 'ASSET_GROUP',
  USER_GROUP = 'USER_GROUP',
  ENTITY_VIEW_GROUP = 'ENTITY_VIEW_GROUP',
  DASHBOARD_GROUP = 'DASHBOARD_GROUP',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION',
  WHITE_LABELING = 'WHITE_LABELING',
  AUDIT_LOG = 'AUDIT_LOG',
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  EDGE = 'EDGE',
  EDGE_GROUP = 'EDGE_GROUP',
  OTA_PACKAGE = 'OTA_PACKAGE',
  QUEUE = 'QUEUE',
  QUEUE_STATS = 'QUEUE_STATS',
  VERSION_CONTROL = 'VERSION_CONTROL',
  NOTIFICATION = 'NOTIFICATION',
  MOBILE_APP_SETTINGS = 'MOBILE_APP_SETTINGS',
  MOBILE_APP_BUNDLE = 'MOBILE_APP_BUNDLE',
  MOBILE_APP = 'MOBILE_APP',
  OAUTH2_CLIENT = 'OAUTH2_CLIENT',
  DOMAIN = 'DOMAIN',
  SECRET = 'SECRET',
  JOB = 'JOB',
  AI_MODEL = 'AI_MODEL',
  AI = 'AI',
  API_KEY = 'API_KEY',
  AGENT = 'AGENT',
  AGENT_APP_PROFILE = 'AGENT_APP_PROFILE',
  AGENT_PROFILE = 'AGENT_PROFILE',
  AGENT_GROUP = 'AGENT_GROUP',
}

const resourceTypeTranslations = new Map<Resource, string>();
for (const key of Object.keys(Resource)) {
  resourceTypeTranslations.set(Resource[key], `permission.resource.display-type.${key}`);
}
export const resourceTypeTranslationMap = resourceTypeTranslations;

export const resourceByEntityType = new Map<EntityType, Resource>(
  [
    [EntityType.ALARM, Resource.ALARM],
    [EntityType.DEVICE, Resource.DEVICE],
    [EntityType.DEVICE_PROFILE, Resource.DEVICE_PROFILE],
    [EntityType.ASSET_PROFILE, Resource.ASSET_PROFILE],
    [EntityType.ASSET, Resource.ASSET],
    [EntityType.CUSTOMER, Resource.CUSTOMER],
    [EntityType.DASHBOARD, Resource.DASHBOARD],
    [EntityType.ENTITY_VIEW, Resource.ENTITY_VIEW],
    [EntityType.TENANT, Resource.TENANT],
    [EntityType.TENANT_PROFILE, Resource.TENANT_PROFILE],
    [EntityType.RULE_CHAIN, Resource.RULE_CHAIN],
    [EntityType.USER, Resource.USER],
    [EntityType.WIDGETS_BUNDLE, Resource.WIDGETS_BUNDLE],
    [EntityType.WIDGET_TYPE, Resource.WIDGET_TYPE],
    [EntityType.CONVERTER, Resource.CONVERTER],
    [EntityType.INTEGRATION, Resource.INTEGRATION],
    [EntityType.SCHEDULER_EVENT, Resource.SCHEDULER_EVENT],
    [EntityType.BLOB_ENTITY, Resource.BLOB_ENTITY],
    [EntityType.REPORT_TEMPLATE, Resource.REPORT_TEMPLATE],
    [EntityType.REPORT, Resource.REPORT],
    [EntityType.ROLE, Resource.ROLE],
    [EntityType.GROUP_PERMISSION, Resource.GROUP_PERMISSION],
    [EntityType.TB_RESOURCE, Resource.TB_RESOURCE],
    [EntityType.EDGE, Resource.EDGE],
    [EntityType.OTA_PACKAGE, Resource.OTA_PACKAGE],
    [EntityType.QUEUE, Resource.QUEUE],
    [EntityType.QUEUE_STATS, Resource.QUEUE_STATS],
    [EntityType.MOBILE_APP, Resource.MOBILE_APP],
    [EntityType.MOBILE_APP_BUNDLE, Resource.MOBILE_APP_BUNDLE],
    [EntityType.DOMAIN, Resource.DOMAIN],
    [EntityType.OAUTH2_CLIENT, Resource.OAUTH2_CLIENT],
    [EntityType.NOTIFICATION_TARGET, Resource.NOTIFICATION],
    [EntityType.NOTIFICATION_RULE, Resource.NOTIFICATION],
    [EntityType.NOTIFICATION_TEMPLATE, Resource.NOTIFICATION],
    [EntityType.SECRET, Resource.SECRET],
    [EntityType.JOB, Resource.JOB],
    [EntityType.AI_MODEL, Resource.AI_MODEL],
    [EntityType.API_KEY, Resource.API_KEY],
    [EntityType.AGENT, Resource.AGENT],
    [EntityType.AGENT_APP_PROFILE, Resource.AGENT_APP_PROFILE],
    [EntityType.AGENT_PROFILE, Resource.AGENT_PROFILE],
  ]
);

export const groupResourceByGroupType = new Map<EntityType, Resource>(
  [
    [EntityType.CUSTOMER, Resource.CUSTOMER_GROUP],
    [EntityType.DEVICE, Resource.DEVICE_GROUP],
    [EntityType.ASSET, Resource.ASSET_GROUP],
    [EntityType.USER, Resource.USER_GROUP],
    [EntityType.ENTITY_VIEW, Resource.ENTITY_VIEW_GROUP],
    [EntityType.DASHBOARD, Resource.DASHBOARD_GROUP],
    [EntityType.EDGE, Resource.EDGE_GROUP],
    [EntityType.AGENT, Resource.AGENT_GROUP],
  ]
);

export const sharableGroupTypes = new Set<EntityType>(
  [
    EntityType.CUSTOMER,
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD,
    EntityType.EDGE,
    EntityType.AGENT
  ]
);

export const publicGroupTypes = new Set<EntityType>(
  [
    EntityType.ASSET,
    EntityType.DEVICE,
    EntityType.ENTITY_VIEW,
    EntityType.DASHBOARD,
    EntityType.EDGE
  ]
);

export interface MergedGroupPermissionInfo {
  entityType: EntityType;
  operations: Operation[];
}

export interface MergedGroupTypePermissionInfo {
  entityGroupIds: EntityGroupId[];
  hasGenericRead: boolean;
}

export interface MergedUserPermissions {
  genericPermissions: {[resource: string]: Operation[]};
  groupPermissions: {[entityGroupId: string]: MergedGroupPermissionInfo};
  readGroupPermissions: {[entityType: string]: MergedGroupTypePermissionInfo};
}

export interface AllowedPermissionsInfo {
  operationsByResource: {[resource: string]: Operation[]};
  allowedForGroupRoleOperations: Operation[];
  allowedForGroupOwnerOnlyOperations: Operation[];
  allowedForGroupOwnerOnlyGroupOperations: Operation[];
  allowedResources: Resource[];
  userPermissions: MergedUserPermissions;
  userOwnerId: EntityId;
}
