///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  RULE_CHAIN = 'RULE_CHAIN',
  RULE_NODE = 'RULE_NODE',
  EDGE = 'EDGE',
  ENTITY_VIEW = 'ENTITY_VIEW',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  WIDGET_TYPE = 'WIDGET_TYPE',
  API_USAGE_STATE = 'API_USAGE_STATE',
  TB_RESOURCE = 'TB_RESOURCE',
  OTA_PACKAGE = 'OTA_PACKAGE',
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
  AI_MODEL = 'AI_MODEL',
  API_KEY = 'API_KEY',
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
        selectedEntities: 'customer.selected-customers'
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
        selectedEntities: 'user.selected-users'
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
        selectedEntities: 'device.selected-devices'
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
        selectedEntities: 'asset.selected-assets'
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
        selectedEntities: 'edge.selected-edges'
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
        selectedEntities: 'entity-view.selected-entity-views'
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
        selectedEntities: 'dashboard.selected-dashboards'
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
        add: 'action.add',
        noEntities: 'calculated-fields.no-found',
        search: 'action.search',
        selectedEntities: 'calculated-fields.selected-fields'
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
      EntityType.AI_MODEL,
      {
        helpLinkId: 'aiModels'
      }
    ],
    [
      EntityType.API_KEY,
      {
        helpLinkId: 'apiKeys'
      }
    ],
  ]
);

export const baseDetailsPageByEntityType = new Map<EntityType, string>([
  [EntityType.TENANT, '/tenants'],
  [EntityType.TENANT_PROFILE, '/tenantProfiles'],
  [EntityType.CUSTOMER, '/customers'],
  [EntityType.USER, '/users'],
  [EntityType.DASHBOARD, '/dashboards'],
  [EntityType.ASSET, '/entities/assets'],
  [EntityType.DEVICE, '/entities/devices'],
  [EntityType.DEVICE_PROFILE, '/profiles/deviceProfiles'],
  [EntityType.ASSET_PROFILE, '/profiles/assetProfiles'],
  [EntityType.RULE_CHAIN, '/ruleChains'],
  [EntityType.EDGE, '/edgeManagement/instances'],
  [EntityType.ENTITY_VIEW, '/entities/entityViews'],
  [EntityType.TB_RESOURCE, '/resources/resources-library'],
  [EntityType.OTA_PACKAGE, '/features/otaUpdates'],
  [EntityType.QUEUE, '/settings/queues'],
  [EntityType.WIDGETS_BUNDLE, '/resources/widgets-library/widgets-bundles/details'],
  [EntityType.WIDGET_TYPE, '/resources/widgets-library/widget-types/details'],
  [EntityType.OAUTH2_CLIENT, '/security-settings/oauth2/clients/details'],
  [EntityType.DOMAIN, '/security-settings/oauth2/clients/details'],
  [EntityType.MOBILE_APP, '/mobile-center/applications']
]);

export interface EntitySubtype {
  tenantId: TenantId;
  entityType: EntityType;
  type: string;
}
