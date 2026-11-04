// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { TenantId } from './id/tenant-id';
import { BaseData, HasId } from '@shared/models/base-data';
import { getProviderHelpLink, OAuth2Client } from '@shared/models/oauth2.models';

export enum EntityType {
  TENANT = 'TENANT',
  TENANT_PROFILE = 'TENANT_PROFILE',
  CUSTOMER = 'CUSTOMER',
  USER = 'USER',
  DASHBOARD = 'DASHBOARD',
  ASSET = 'ASSET',
  DEVICE = 'DEVICE',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE = 'ASSET_PROFILE',
  ALARM = 'ALARM',
  ENTITY_GROUP = 'ENTITY_GROUP',
  CONVERTER = 'CONVERTER',
  INTEGRATION = 'INTEGRATION',
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
  SCHEDULER_EVENT = 'SCHEDULER_EVENT',
  BLOB_ENTITY = 'BLOB_ENTITY',
  REPORT_TEMPLATE = 'REPORT_TEMPLATE',
  REPORT = 'REPORT',
  ENTITY_VIEW = 'ENTITY_VIEW',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  ROLE = 'ROLE',
  GROUP_PERMISSION = 'GROUP_PERMISSION',
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  EDGE = 'EDGE',
  OTA_PACKAGE = 'OTA_PACKAGE',
  JOB = 'JOB',
  RPC = 'RPC',
  QUEUE = 'QUEUE',
  QUEUE_STATS = 'QUEUE_STATS',
  NOTIFICATION = 'NOTIFICATION',
  NOTIFICATION_REQUEST = 'NOTIFICATION_REQUEST',
  NOTIFICATION_RULE = 'NOTIFICATION_RULE',
  NOTIFICATION_TARGET = 'NOTIFICATION_TARGET',
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE',
  OAUTH2_CLIENT = 'OAUTH2_CLIENT',
  DOMAIN = 'DOMAIN',
  MOBILE_APP_BUNDLE = 'MOBILE_APP_BUNDLE',
  MOBILE_APP = 'MOBILE_APP',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  ADMIN_SETTINGS = 'ADMIN_SETTINGS',
  SECRET = 'SECRET',
  AI_MODEL = 'AI_MODEL',
  API_KEY = 'API_KEY',
  AGENT = 'AGENT',
  AGENT_PROFILE = 'AGENT_PROFILE',
  AGENT_APPLICATION = 'AGENT_APPLICATION',
  AGENT_APP_EVENT = 'AGENT_APP_EVENT',
  AGENT_APP_UNIT = 'AGENT_APP_UNIT',
  AGENT_APP_PROFILE = 'AGENT_APP_PROFILE',
  AGENT_BULK_ACTION = 'AGENT_BULK_ACTION',
}

export enum AliasEntityType {
  CURRENT_CUSTOMER = 'CURRENT_CUSTOMER',
  CURRENT_TENANT = 'CURRENT_TENANT',
  CURRENT_USER = 'CURRENT_USER',
  CURRENT_USER_OWNER = 'CURRENT_USER_OWNER'
}

export interface EntityTypeTranslation {
  type?: string;
  typePlural?: string;
  list?: string;
  nameStartsWith?: string;
  details?: string;
  add?: string;
  noEntities?: string;
  selectedEntities?: string;
  search?: string;
  selectGroupToAdd?: string;
  selectGroupToMove?: string;
  removeFromGroup?: string;
  group?: string;
  groupList?: string;
  groupNameStartsWith?: string;
}

export interface EntityTypeResource<T> {
  helpLinkId: string;
  helpLinkIdForEntity?(entity: T): string;
}

export const entityTypeTranslations = new Map<EntityType | AliasEntityType, EntityTypeTranslation>(
  [
    [
      EntityType.TENANT,
      {
        type: 'entity.type-tenant',
        typePlural: 'entity.type-tenants',
        list: 'entity.list-of-tenants',
        nameStartsWith: 'entity.tenant-name-starts-with',
        details: 'tenant.tenant-details',
        add: 'tenant.add',
        noEntities: 'tenant.no-tenants-text',
        search: 'tenant.search',
        selectedEntities: 'tenant.selected-tenants'
      }
    ],
    [
      EntityType.TENANT_PROFILE,
      {
        type: 'entity.type-tenant-profile',
        typePlural: 'entity.type-tenant-profiles',
        list: 'entity.list-of-tenant-profiles',
        nameStartsWith: 'entity.tenant-profile-name-starts-with',
        details: 'tenant-profile.tenant-profile-details',
        add: 'tenant-profile.add',
        noEntities: 'tenant-profile.no-tenant-profiles-text',
        search: 'tenant-profile.search',
        selectedEntities: 'tenant-profile.selected-tenant-profiles'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        type: 'entity.type-customer',
        typePlural: 'entity.type-customers',
        list: 'entity.list-of-customers',
        nameStartsWith: 'entity.customer-name-starts-with',
        details: 'customer.customer-details',
        add: 'customer.add',
        noEntities: 'customer.no-customers-text',
        search: 'customer.search',
        selectedEntities: 'customer.selected-customers',
        selectGroupToAdd: 'customer.select-group-to-add',
        selectGroupToMove: 'customer.select-group-to-move',
        removeFromGroup: 'customer.remove-customers-from-group',
        group: 'customer.group',
        groupList: 'customer.list-of-groups',
        groupNameStartsWith: 'customer.group-name-starts-with'
      }
    ],
    [
      EntityType.USER,
      {
        type: 'entity.type-user',
        typePlural: 'entity.type-users',
        list: 'entity.list-of-users',
        nameStartsWith: 'entity.user-name-starts-with',
        details: 'user.user-details',
        add: 'user.add',
        noEntities: 'user.no-users-text',
        search: 'user.search',
        selectedEntities: 'user.selected-users',
        selectGroupToAdd: 'user.select-group-to-add',
        selectGroupToMove: 'user.select-group-to-move',
        removeFromGroup: 'user.remove-users-from-group',
        group: 'user.group',
        groupList: 'user.list-of-groups',
        groupNameStartsWith: 'user.group-name-starts-with'
      }
    ],
    [
      EntityType.DEVICE,
      {
        type: 'entity.type-device',
        typePlural: 'entity.type-devices',
        list: 'entity.list-of-devices',
        nameStartsWith: 'entity.device-name-starts-with',
        details: 'device.device-details',
        add: 'device.add',
        noEntities: 'device.no-devices-text',
        search: 'device.search',
        selectedEntities: 'device.selected-devices',
        selectGroupToAdd: 'device.select-group-to-add',
        selectGroupToMove: 'device.select-group-to-move',
        removeFromGroup: 'device.remove-devices-from-group',
        group: 'device.group',
        groupList: 'device.list-of-groups',
        groupNameStartsWith: 'device.group-name-starts-with'
      }
    ],
    [
      EntityType.DEVICE_PROFILE,
      {
        type: 'entity.type-device-profile',
        typePlural: 'entity.type-device-profiles',
        list: 'entity.list-of-device-profiles',
        nameStartsWith: 'entity.device-profile-name-starts-with',
        details: 'device-profile.device-profile-details',
        add: 'device-profile.add',
        noEntities: 'device-profile.no-device-profiles-text',
        search: 'device-profile.search',
        selectedEntities: 'device-profile.selected-device-profiles'
      }
    ],
    [
      EntityType.ASSET_PROFILE,
      {
        type: 'entity.type-asset-profile',
        typePlural: 'entity.type-asset-profiles',
        list: 'entity.list-of-asset-profiles',
        nameStartsWith: 'entity.asset-profile-name-starts-with',
        details: 'asset-profile.asset-profile-details',
        add: 'asset-profile.add',
        noEntities: 'asset-profile.no-asset-profiles-text',
        search: 'asset-profile.search',
        selectedEntities: 'asset-profile.selected-asset-profiles'
      }
    ],
    [
      EntityType.ASSET,
      {
        type: 'entity.type-asset',
        typePlural: 'entity.type-assets',
        list: 'entity.list-of-assets',
        nameStartsWith: 'entity.asset-name-starts-with',
        details: 'asset.asset-details',
        add: 'asset.add',
        noEntities: 'asset.no-assets-text',
        search: 'asset.search',
        selectedEntities: 'asset.selected-assets',
        selectGroupToAdd: 'asset.select-group-to-add',
        selectGroupToMove: 'asset.select-group-to-move',
        removeFromGroup: 'asset.remove-assets-from-group',
        group: 'asset.group',
        groupList: 'asset.list-of-groups',
        groupNameStartsWith: 'asset.group-name-starts-with'
      }
    ],
    [
      EntityType.ENTITY_VIEW,
      {
        type: 'entity.type-entity-view',
        typePlural: 'entity.type-entity-views',
        list: 'entity.list-of-entity-views',
        nameStartsWith: 'entity.entity-view-name-starts-with',
        details: 'entity-view.entity-view-details',
        add: 'entity-view.add',
        noEntities: 'entity-view.no-entity-views-text',
        search: 'entity-view.search',
        selectedEntities: 'entity-view.selected-entity-views',
        selectGroupToAdd: 'entity-view.select-group-to-add',
        selectGroupToMove: 'entity-view.select-group-to-move',
        removeFromGroup: 'entity-view.remove-entity-views-from-group',
        group: 'entity-view.group',
        groupList: 'entity-view.list-of-groups',
        groupNameStartsWith: 'entity-view.group-name-starts-with'
      }
    ],
    [
      EntityType.EDGE,
      {
        type: 'entity.type-edge',
        typePlural: 'entity.type-edges',
        list: 'entity.list-of-edges',
        nameStartsWith: 'entity.edge-name-starts-with',
        details: 'edge.edge-details',
        add: 'edge.add',
        noEntities: 'edge.no-edges-text',
        search: 'edge.search',
        selectedEntities: 'edge.selected-edges',
        selectGroupToAdd: 'edge.select-group-to-add',
        selectGroupToMove: 'edge.select-group-to-move',
        removeFromGroup: 'edge.remove-edges-from-group',
        group: 'edge.group',
        groupList: 'edge.list-of-groups',
        groupNameStartsWith: 'edge.group-name-starts-with'
      }
    ],
    [
      EntityType.RULE_CHAIN,
      {
        type: 'entity.type-rulechain',
        typePlural: 'entity.type-rulechains',
        list: 'entity.list-of-rulechains',
        nameStartsWith: 'entity.rulechain-name-starts-with',
        details: 'rulechain.rulechain-details',
        add: 'rulechain.add',
        noEntities: 'rulechain.no-rulechains-text',
        search: 'rulechain.search',
        selectedEntities: 'rulechain.selected-rulechains'
      }
    ],
    [
      EntityType.RULE_NODE,
      {
        type: 'entity.type-rulenode',
        typePlural: 'entity.type-rulenodes',
        list: 'entity.list-of-rulenodes',
        nameStartsWith: 'entity.rulenode-name-starts-with'
      }
    ],
    [
      EntityType.DASHBOARD,
      {
        type: 'entity.type-dashboard',
        typePlural: 'entity.type-dashboards',
        list: 'entity.list-of-dashboards',
        nameStartsWith: 'entity.dashboard-name-starts-with',
        details: 'dashboard.dashboard-details',
        add: 'dashboard.add',
        noEntities: 'dashboard.no-dashboards-text',
        search: 'dashboard.search',
        selectedEntities: 'dashboard.selected-dashboards',
        selectGroupToAdd: 'dashboard.select-group-to-add',
        selectGroupToMove: 'dashboard.select-group-to-move',
        removeFromGroup: 'dashboard.remove-dashboards-from-group',
        group: 'dashboard.group',
        groupList: 'dashboard.list-of-groups',
        groupNameStartsWith: 'dashboard.group-name-starts-with'
      }
    ],
    [
      EntityType.ALARM,
      {
        type: 'entity.type-alarm',
        typePlural: 'entity.type-alarms',
        list: 'entity.list-of-alarms',
        nameStartsWith: 'entity.alarm-name-starts-with',
        details: 'alarm.alarm-details',
        noEntities: 'alarm.no-alarms-prompt',
        search: 'alarm.search',
        selectedEntities: 'alarm.selected-alarms'
      }
    ],
    [
      EntityType.ENTITY_GROUP,
      {
        type: 'entity.type-entity-group',
        typePlural: 'entity.type-entity-groups',
        details: 'entity-group.entity-group-details',
        add: 'entity-group.add',
        noEntities: 'entity-group.no-entity-groups-text',
        search: 'entity-group.search',
        selectedEntities: 'entity-group.selected-entity-groups'
      }
    ],
    [
      EntityType.API_USAGE_STATE,
      {
        type: 'entity.type-api-usage-state'
      }
    ],
    [
      EntityType.WIDGET_TYPE,
      {
        type: 'entity.type-widget',
        typePlural: 'entity.type-widgets',
        list: 'entity.list-of-widgets',
        details: 'widget.details',
        add: 'dashboard.add-widget',
        noEntities: 'widget.no-widgets-text',
        search: 'widget.search-widgets',
        selectedEntities: 'widget.selected-widgets'
      }
    ],
    [
      EntityType.WIDGETS_BUNDLE,
      {
        type: 'entity.type-widgets-bundle',
        typePlural: 'entity.type-widgets-bundles',
        list: 'entity.list-of-widgets-bundles',
        details: 'widgets-bundle.widgets-bundle-details',
        add: 'widgets-bundle.add',
        noEntities: 'widgets-bundle.no-widgets-bundles-text',
        search: 'widgets-bundle.search',
        selectedEntities: 'widgets-bundle.selected-widgets-bundles'
      }
    ],
    [
      EntityType.CONVERTER,
      {
        type: 'entity.type-converter',
        typePlural: 'entity.type-converters',
        list: 'entity.list-of-converters',
        nameStartsWith: 'entity.converter-name-starts-with',
        details: 'converter.converter-details',
        add: 'converter.add',
        noEntities: 'converter.no-converters-text',
        search: 'converter.search',
        selectedEntities: 'converter.selected-converters'
      }
    ],
    [
      EntityType.INTEGRATION,
      {
        type: 'entity.type-integration',
        typePlural: 'entity.type-integrations',
        list: 'entity.list-of-integrations',
        nameStartsWith: 'entity.integration-name-starts-with',
        details: 'integration.integration-details',
        add: 'integration.add',
        noEntities: 'integration.no-integrations-text',
        search: 'integration.search',
        selectedEntities: 'integration.selected-integrations'
      }
    ],
    [
      EntityType.SCHEDULER_EVENT,
      {
        type: 'entity.type-scheduler-event',
        typePlural: 'entity.type-scheduler-events',
        list: 'entity.list-of-scheduler-events',
        nameStartsWith: 'entity.scheduler-event-name-starts-with'
      }
    ],
    [
      EntityType.BLOB_ENTITY,
      {
        type: 'entity.type-blob-entity',
        typePlural: 'entity.type-blob-entities',
        list: 'entity.list-of-blob-entities',
        nameStartsWith: 'entity.blob-entity-name-starts-with'
      }
    ],
    [
      EntityType.REPORT_TEMPLATE,
      {
        type: 'entity.type-report-template',
        typePlural: 'entity.type-report-templates',
        list: 'entity.list-of-report-templates',
        nameStartsWith: 'entity.report-template-name-starts-with',
        details: 'report-template.report-template-details',
        add: 'report-template.add',
        noEntities: 'report-template.no-report-templates-text',
        search: 'report-template.search',
        selectedEntities: 'report-template.selected-report-templates'
      }
    ],
    [
      EntityType.REPORT,
      {
        type: 'entity.type-report',
        typePlural: 'entity.type-reports',
        list: 'entity.list-of-reports',
        nameStartsWith: 'entity.report-name-starts-with',
        details: 'report.report-details',
        add: 'report.add',
        noEntities: 'report.no-reports-text',
        search: 'report.search',
        selectedEntities: 'report.selected-reports'
      }
    ],
    [
      EntityType.ROLE,
      {
        type: 'entity.type-role',
        typePlural: 'entity.type-roles',
        list: 'entity.list-of-roles',
        nameStartsWith: 'entity.role-name-starts-with',
        details: 'role.role-details',
        add: 'role.add',
        noEntities: 'role.no-roles-text',
        search: 'role.search',
        selectedEntities: 'role.selected-roles'
      }
    ],
    [
      EntityType.GROUP_PERMISSION,
      {
        type: 'entity.type-group-permission'
      }
    ],
    [
      AliasEntityType.CURRENT_CUSTOMER,
      {
        type: 'entity.type-current-customer',
        list: 'entity.type-current-customer'
      }
    ],
    [
      AliasEntityType.CURRENT_TENANT,
      {
        type: 'entity.type-current-tenant',
        list: 'entity.type-current-tenant'
      }
    ],
    [
      AliasEntityType.CURRENT_USER,
      {
        type: 'entity.type-current-user',
        list: 'entity.type-current-user'
      }
    ],
    [
      AliasEntityType.CURRENT_USER_OWNER,
      {
        type: 'entity.type-current-user-owner',
        list: 'entity.type-current-user-owner'
      }
    ],
    [
      EntityType.TB_RESOURCE,
      {
        type: 'entity.type-tb-resource',
        typePlural: 'entity.type-tb-resources',
        list: 'entity.list-of-tb-resources',
        details: 'resource.resource-library-details',
        add: 'resource.add',
        noEntities: 'resource.no-resource-text',
        search: 'resource.search',
        selectedEntities: 'resource.selected-resources'
      }
    ],
    [
      EntityType.OTA_PACKAGE,
      {
        type: 'entity.type-ota-package',
        typePlural: 'entity.type-ota-packages',
        list: 'entity.list-of-ota-packages',
        details: 'ota-update.ota-update-details',
        add: 'ota-update.add',
        noEntities: 'ota-update.no-packages-text',
        search: 'ota-update.search',
        selectedEntities: 'ota-update.selected-package'
      }
    ],
    [
      EntityType.JOB,
      {
        type: 'entity.type-task',
        typePlural: 'entity.type-tasks',
        list: 'entity.list-of-tasks',
        noEntities: 'task.no-tasks-prompt',
        search: 'task.search',
        selectedEntities: 'task.selected-tasks'
      }
    ],
    [
      EntityType.RPC,
      {
        type: 'entity.type-rpc'
      }
    ],
    [
      EntityType.QUEUE,
      {
        type: 'entity.type-queue',
        add: 'queue.add',
        search: 'queue.search',
        details: 'queue.details',
        selectedEntities: 'queue.selected-queues'
      }
    ],
    [
      EntityType.QUEUE_STATS,
      {
        type: 'entity.type-queue-stats',
        typePlural: 'entity.type-queues-stats',
        list: 'queue-statistics.list-of-queue-statistics',
        noEntities: 'queue-statistics.no-queue-statistics-text',
        selectedEntities: 'queue-statistics.selected-queue-statistics',
        nameStartsWith: 'queue-statistics.queue-statistics-starts-with',
      }
    ],
    [
      EntityType.NOTIFICATION,
      {
        type: 'entity.type-notification',
        noEntities: 'notification.no-inbox-notification',
        search: 'notification.search-notification',
        selectedEntities: 'notification.selected-notifications'
      }
    ],
    [
      EntityType.NOTIFICATION_REQUEST,
      {
        type: 'entity.type-notification-request',
        noEntities: 'notification.no-notification-request',
        selectedEntities: 'notification.selected-requests'
      }
    ],
    [
      EntityType.NOTIFICATION_RULE,
      {
        type: 'entity.type-notification-rule',
        typePlural: 'entity.type-notification-rules',
        list: 'entity.list-of-notification-rules',
        noEntities: 'notification.no-rules-notification',
        search: 'notification.search-rules',
        selectedEntities: 'notification.selected-rules',
        add: 'notification.add-rule'
      }
    ],
    [
      EntityType.NOTIFICATION_TARGET,
      {
        type: 'entity.type-notification-target',
        typePlural: 'entity.type-notification-targets',
        list: 'entity.list-of-notification-targets',
        noEntities: 'notification.no-recipients-notification',
        search: 'notification.search-recipients',
        selectedEntities: 'notification.selected-recipients',
        add: 'notification.add-recipients'
      }
    ],
    [
      EntityType.NOTIFICATION_TEMPLATE,
      {
        type: 'entity.type-notification-template',
        typePlural: 'entity.type-notification-templates',
        list: 'entity.list-of-notification-templates',
        noEntities: 'notification.no-notification-templates',
        search: 'notification.search-templates',
        selectedEntities: 'notification.selected-template',
        add: 'notification.add-template'
      }
    ],
    [
      EntityType.DOMAIN,
      {
        type: 'entity.type-domain',
        typePlural: 'entity.type-domains',
        list: 'entity.list-of-domains',
        details: 'admin.oauth2.domain-details',
        add: 'admin.oauth2.add-domain',
        noEntities: 'admin.oauth2.no-domains',
        search: 'admin.oauth2.search-domains'
      }
    ],
    [
      EntityType.OAUTH2_CLIENT,
      {
        type: 'entity.type-oauth2-client',
        typePlural: 'entity.type-oauth2-clients',
        list: 'entity.list-of-oauth2-clients',
        details: 'admin.oauth2.client-details',
        add: 'admin.oauth2.add-client',
        noEntities: 'admin.oauth2.no-oauth2-clients',
        search: 'admin.oauth2.search-oauth2-clients'
      }
    ],
    [
      EntityType.MOBILE_APP,
      {
        type: 'entity.type-mobile-app',
        typePlural: 'entity.type-mobile-apps',
        list: 'entity.list-of-mobile-apps',
        details: 'admin.oauth2.mobile-app-details',
        add: 'mobile.add-application',
        noEntities: 'mobile.no-application',
        search: 'mobile.search-application'
      }
    ],
    [
      EntityType.MOBILE_APP_BUNDLE,
      {
        type: 'entity.type-mobile-app-bundle',
        typePlural: 'entity.type-mobile-app-bundles',
        list: 'entity.list-of-mobile-app-bundles',
        add: 'mobile.add-bundle',
        noEntities: 'mobile.no-bundles',
        search: 'mobile.search-bundles'
      }
    ],
    [
      EntityType.CALCULATED_FIELD,
      {
        type: 'entity.type-calculated-field',
        typePlural: 'entity.type-calculated-fields',
        list: 'calculated-fields.list',
        add: 'calculated-fields.add',
        details: 'calculated-fields.calculated-field-details',
        noEntities: 'calculated-fields.no-found',
        search: 'action.search',
        selectedEntities: 'calculated-fields.selected-fields'
      }
    ],
    [
      EntityType.ADMIN_SETTINGS,
      {
        type: 'entity.type-admin-settings'
      }
    ],
    [
      EntityType.AI_MODEL,
      {
        type: 'entity.type-ai-model',
        typePlural: 'entity.type-ai-models',
        list: 'ai-models.list',
        add: 'ai-models.add',
        noEntities: 'ai-models.no-found',
        search: 'action.search',
        selectedEntities: 'ai-models.selected-fields'
      }
    ],
    [
      EntityType.SECRET,
      {
        type: 'entity.type-secret-storage',
        list: 'secret-storage.list',
        add: 'secret-storage.add',
        noEntities: 'secret-storage.no-found',
        search: 'secret-storage.search',
        selectedEntities: 'secret-storage.selected-fields'
      }
    ],
    [
      EntityType.API_KEY,
      {
        type: 'entity.type-api-key',
        typePlural: 'entity.type-api-keys',
        list: 'api-key.list',
        add: 'api-key.generate',
        noEntities: 'api-key.no-found',
        search: 'api-key.search',
        selectedEntities: 'api-key.selected-api-keys'
      }
    ],
    [
      EntityType.AGENT,
      {
        type: 'entity.type-agent',
        typePlural: 'entity.type-agents',
        list: 'entity.list-of-agents',
        nameStartsWith: 'entity.agent-name-starts-with',
        details: 'agent.agent-details',
        add: 'agent.add',
        noEntities: 'agent.no-agents-text',
        search: 'agent.search',
        selectedEntities: 'agent.selected-agents',
        group: 'agent.group',
        groupList: 'agent.list-of-groups',
        groupNameStartsWith: 'agent.group-name-starts-with'
      }
    ],
    [
      EntityType.AGENT_PROFILE,
      {
        type: 'entity.type-agent-profile',
        typePlural: 'entity.type-agent-profiles',
        list: 'entity.list-of-agent-profiles',
        nameStartsWith: 'entity.agent-profile-name-starts-with',
        details: 'agent.profile-details',
        add: 'agent.add-profile',
        noEntities: 'agent.no-agent-profiles-text',
        search: 'agent.search-profiles',
        selectedEntities: 'agent.selected-agent-profiles'
      }
    ],
    [
      EntityType.AGENT_APPLICATION,
      {
        type: 'entity.type-agent-application',
        typePlural: 'entity.type-agent-applications',
        list: 'entity.list-of-agent-applications',
        nameStartsWith: 'entity.agent-application-name-starts-with',
        details: 'agent.application-details',
        add: 'agent.add-application',
        noEntities: 'agent.no-applications-text',
        search: 'agent.search-applications',
        selectedEntities: 'agent.selected-applications'
      }
    ],
    [
      EntityType.AGENT_APP_PROFILE,
      {
        type: 'entity.type-agent-app-profile',
        typePlural: 'entity.type-agent-app-profiles',
        list: 'entity.list-of-agent-app-profiles',
        nameStartsWith: 'entity.agent-app-profile-name-starts-with',
        details: 'agent.app-profile-details',
        add: 'agent.add-app-profile',
        noEntities: 'agent.no-app-profiles-text',
        search: 'agent.search-app-profiles',
        selectedEntities: 'agent.selected-app-profiles'
      }
    ],
    [
      EntityType.AGENT_APP_EVENT,
      {
        type: 'entity.type-agent-app-event',
        typePlural: 'entity.type-agent-app-events',
        noEntities: 'agent.no-events-text',
        search: 'agent.search-events'
      }
    ],
    [
      EntityType.AGENT_APP_UNIT,
      {
        type: 'entity.type-agent-app-unit',
        typePlural: 'entity.type-agent-app-units'
      }
    ],
    [
      EntityType.AGENT_BULK_ACTION,
      {
        type: 'entity.type-agent-bulk-action',
        typePlural: 'entity.type-agent-bulk-actions'
      }
    ],
  ]
);

export const entityTypeResources = new Map<EntityType, EntityTypeResource<BaseData<HasId>>>(
  [
    [
      EntityType.TENANT,
      {
        helpLinkId: 'tenants'
      }
    ],
    [
      EntityType.TENANT_PROFILE,
      {
        helpLinkId: 'tenantProfiles'
      }
    ],
    [
      EntityType.CUSTOMER,
      {
        helpLinkId: 'customers'
      }
    ],
    [
      EntityType.USER,
      {
        helpLinkId: 'users'
      }
    ],
    [
      EntityType.DEVICE,
      {
        helpLinkId: 'devices'
      }
    ],
    [
      EntityType.DEVICE_PROFILE,
      {
        helpLinkId: 'deviceProfiles'
      }
    ],
    [
      EntityType.ASSET_PROFILE,
      {
        helpLinkId: 'assetProfiles'
      }
    ],
    [
      EntityType.ASSET,
      {
        helpLinkId: 'assets'
      }
    ],
    [
      EntityType.EDGE,
      {
        helpLinkId: 'edges'
      }
    ],
    [
      EntityType.ENTITY_VIEW,
      {
        helpLinkId: 'entityViews'
      }
    ],
    [
      EntityType.EDGE,
      {
        helpLinkId: 'edges'
      }
    ],
    [
      EntityType.RULE_CHAIN,
      {
        helpLinkId: 'rulechains'
      }
    ],
    [
      EntityType.DASHBOARD,
      {
        helpLinkId: 'dashboards'
      }
    ],
    [
      EntityType.WIDGET_TYPE,
      {
        helpLinkId: 'widgetTypes'
      }
    ],
    [
      EntityType.WIDGETS_BUNDLE,
      {
        helpLinkId: 'widgetsBundles'
      }
    ],
    [
      EntityType.ROLE,
      {
        helpLinkId: 'roles'
      }
    ],
    [
      EntityType.ENTITY_GROUP,
      {
        helpLinkId: 'entityGroups'
      }
    ],
    [
      EntityType.TB_RESOURCE,
      {
        helpLinkId: 'lwm2mResourceLibrary'
      }
    ],
    [
      EntityType.OTA_PACKAGE,
      {
        helpLinkId: 'otaUpdates'
      }
    ],
    [
      EntityType.QUEUE,
      {
        helpLinkId: 'queue'
      }
    ],
    [
      EntityType.OAUTH2_CLIENT,
      {
        helpLinkId: 'oauth2Settings',
        helpLinkIdForEntity: (entity: OAuth2Client) => getProviderHelpLink(entity.additionalInfo.providerName)
      }
    ],
    [
      EntityType.DOMAIN,
      {
        helpLinkId: 'domains'
      }
    ],
    [
      EntityType.MOBILE_APP,
      {
        helpLinkId: 'mobileApplication'
      }
    ],
    [
      EntityType.MOBILE_APP_BUNDLE,
      {
        helpLinkId: 'mobileBundle'
      }
    ],
    [
      EntityType.SECRET,
      {
        helpLinkId: 'secretStorage'
      }
    ],
    [
      EntityType.AI_MODEL,
      {
        helpLinkId: 'aiModels'
      }
    ],
    [
      EntityType.CALCULATED_FIELD,
      {
        helpLinkId: 'calculatedField'
      }
    ],
    [
      EntityType.REPORT_TEMPLATE,
      {
        helpLinkId: 'reportTemplates'
      }
    ],
    [
      EntityType.REPORT,
      {
        helpLinkId: 'reports'
      }
    ],
    [
      EntityType.API_KEY,
      {
        helpLinkId: 'apiKeys'
      }
    ],
    [
      EntityType.AGENT,
      {
        helpLinkId: 'agents'
      }
    ],
    [
      EntityType.AGENT_PROFILE,
      {
        helpLinkId: 'agentProfiles'
      }
    ],
    [
      EntityType.AGENT_APPLICATION,
      {
        helpLinkId: 'agentApplications'
      }
    ],
    [
      EntityType.AGENT_APP_PROFILE,
      {
        helpLinkId: 'agentAppProfiles'
      }
    ],
  ]
);

export const baseDetailsPageByEntityType = new Map<EntityType, string>([
  [EntityType.TENANT, '/tenants'],
  [EntityType.TENANT_PROFILE, '/tenantProfiles'],
  [EntityType.CUSTOMER, '/customers/all'],
  [EntityType.USER, '/users/all'],
  [EntityType.DASHBOARD, '/dashboards/all'],
  [EntityType.ASSET, '/entities/assets/all'],
  [EntityType.DEVICE, '/entities/devices/all'],
  [EntityType.DEVICE_PROFILE, '/profiles/deviceProfiles'],
  [EntityType.ASSET_PROFILE, '/profiles/assetProfiles'],
  [EntityType.CONVERTER, '/integrationsCenter/converters'],
  [EntityType.INTEGRATION, '/integrationsCenter/integrations'],
  [EntityType.RULE_CHAIN, '/ruleChains'],
  [EntityType.EDGE, '/edgeManagement/edges/all'],
  [EntityType.ENTITY_VIEW, '/entities/entityViews/all'],
  [EntityType.ROLE, '/security-settings/roles'],
  [EntityType.TB_RESOURCE, '/resources/resources-library'],
  [EntityType.OTA_PACKAGE, '/features/otaUpdates'],
  [EntityType.QUEUE, '/settings/queues'],
  [EntityType.WIDGETS_BUNDLE, '/resources/widgets-library/widgets-bundles/details'],
  [EntityType.WIDGET_TYPE, '/resources/widgets-library/widget-types/details'],
  [EntityType.OAUTH2_CLIENT, '/security-settings/oauth2/clients/details'],
  [EntityType.DOMAIN, '/security-settings/oauth2/clients/details'],
  [EntityType.MOBILE_APP, '/mobile-center/applications'],
  [EntityType.REPORT_TEMPLATE, '/reporting/templates'],
  [EntityType.AGENT, '/edgeManagement/agents/all'],
  [EntityType.AGENT_PROFILE, '/edgeManagement/profiles/agent'],
  [EntityType.AGENT_APP_PROFILE, '/edgeManagement/profiles/application'],
  [EntityType.CALCULATED_FIELD, '/calculatedFields'],
]);

export const groupUrlPrefixByEntityType = new Map<EntityType, string>([
  [EntityType.CUSTOMER, '/customers/groups'],
  [EntityType.USER, '/users/groups'],
  [EntityType.DASHBOARD, '/dashboards/groups'],
  [EntityType.ASSET, '/entities/assets/groups'],
  [EntityType.DEVICE, '/entities/devices/groups'],
  [EntityType.EDGE, '/edgeManagement/edges/groups'],
  [EntityType.ENTITY_VIEW, '/entities/entityViews/groups'],
  [EntityType.AGENT, '/edgeManagement/agents/groups'],
]);

export interface EntitySubtype {
  tenantId: TenantId;
  entityType: EntityType;
  type: string;
}
