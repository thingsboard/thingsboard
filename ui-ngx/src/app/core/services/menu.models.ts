// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { EntityType } from '@shared/models/entity-type.models';
import { AuthState } from '@core/auth/auth.models';
import { Authority } from '@shared/models/authority.enum';
import { deepClone, isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import {
  CMItemLinkType,
  CMItemType,
  CustomMenuConfig,
  CustomMenuItem,
  HomeMenuItemType,
  isCustomMenuItem,
  isDefaultMenuItem,
  isHomeMenuItem,
  MenuItem
} from '@shared/models/custom-menu.models';
import { calculatedFieldsEntityTypeList } from '@shared/models/calculated-field.models';
import { alarmRuleEntityTypeList } from "@shared/models/alarm-rule.models";

export declare type MenuSectionType = 'link' | 'toggle';

export interface MenuReference {
  id: MenuId  | string;
  pages?: Array<MenuReference>;
}

export interface MenuSection extends MenuReference {
  id: MenuId | string;
  name: string;
  fullName?: string;
  type: MenuSectionType;
  path: string;
  queryParams?: {[k: string]: any};
  icon: string;
  outlinedIcon?: string;
  pages?: Array<MenuSection>;
  opened?: boolean;
  rootOnly?: boolean;
  isCustom?: boolean;
  isNew?: boolean;
  stateId?: string;
  childStateIds?: {[stateId: string]: boolean};
  customTranslate?: boolean;
  active?: boolean;
  homeDashboardId?: string;
  homeHideDashboardToolbar?: boolean;
}

export const sectionPath = (section: MenuSection): string => {
  if (section.isCustom) {
    return section.path + '/' + section.stateId;
  } else {
    return section.path;
  }
};

export interface HomeSection {
  name: string;
  places: Array<MenuSection>;
}

export enum MenuId {
  home = 'home',
  tenants = 'tenants',
  tenants_section = 'tenants_section',
  tenant_profiles = 'tenant_profiles',
  resources = 'resources',
  widget_library = 'widget_library',
  widget_types = 'widget_types',
  widgets_bundles = 'widgets_bundles',
  images = 'images',
  scada_symbols = 'scada_symbols',
  resources_library = 'resources_library',
  javascript_library = 'javascript_library',
  notifications_center = 'notifications_center',
  notification_inbox = 'notification_inbox',
  notification_sent = 'notification_sent',
  notification_recipients = 'notification_recipients',
  notification_templates = 'notification_templates',
  notification_rules = 'notification_rules',
  mobile_center = 'mobile_center',
  mobile_apps = 'mobile_apps',
  mobile_bundles = 'mobile_bundles',
  mobile_qr_code_widget = 'mobile_qr_code_widget',
  platform = 'platform',
  platform_section = 'platform_section',
  settings = 'settings',
  general = 'general',
  mail_server = 'mail_server',
  home_settings = 'home_settings',
  notification_settings = 'notification_settings',
  repository_settings = 'repository_settings',
  auto_commit_settings = 'auto_commit_settings',
  queues = 'queues',
  license_management = 'license_management',
  security_settings = 'security_settings',
  security_settings_general = 'security_settings_general',
  two_fa = 'two_fa',
  oauth2 = 'oauth2',
  domains = 'domains',
  clients = 'clients',
  audit_log = 'audit_log',
  monitor = 'monitor',
  alarms_center = 'alarms_center',
  alarms = 'alarms',
  alarm_rules = 'alarm_rules',
  dashboards = 'dashboards',
  entities = 'entities',
  devices = 'devices',
  assets = 'assets',
  entity_views = 'entity_views',
  gateways = 'gateways',
  profiles = 'profiles',
  device_profiles = 'device_profiles',
  asset_profiles = 'asset_profiles',
  customers_and_users = 'customers_and_users',
  customers = 'customers',
  data_processing = 'data_processing',
  calculated_fields = 'calculated_fields',
  rule_chains = 'rule_chains',
  edge_management = 'edge_management',
  edges = 'edges',
  edge_instances = 'edge_instances',
  rulechain_templates = 'rulechain_templates',
  agents = 'agents',
  agent_all = 'agent_all',
  agent_groups = 'agent_groups',
  agent_shared = 'agent_shared',
  edge_profiles = 'edge_profiles',
  edge_templates = 'edge_templates',
  features = 'features',
  otaUpdates = 'otaUpdates',
  version_control = 'version_control',
  api_usage = 'api_usage',
  white_labeling = 'white_labeling',
  white_labeling_general = 'white_labeling_general',
  login_white_labeling = 'login_white_labeling',
  mail_templates = 'mail_templates',
  custom_translation = 'custom_translation',
  custom_menu = 'custom_menu',
  dashboard_all = 'dashboard_all',
  dashboard_groups = 'dashboard_groups',
  dashboard_shared = 'dashboard_shared',
  ai_solution_creator = 'ai_solution_creator',
  device_all = 'device_all',
  device_groups = 'device_groups',
  device_shared = 'device_shared',
  asset_all = 'asset_all',
  asset_groups = 'asset_groups',
  asset_shared = 'asset_shared',
  entity_view_all = 'entity_view_all',
  entity_view_groups = 'entity_view_groups',
  entity_view_shared = 'entity_view_shared',
  customer_all = 'customer_all',
  customer_groups = 'customer_groups',
  customer_shared = 'customer_shared',
  customers_hierarchy = 'customers_hierarchy',
  users = 'users',
  user_all = 'user_all',
  user_groups = 'user_groups',
  integrations_center = 'integrations_center',
  integrations = 'integrations',
  converters = 'converters',
  edge_all = 'edge_all',
  edge_groups = 'edge_groups',
  edge_shared = 'edge_shared',
  integration_templates = 'integration_templates',
  converter_templates = 'converter_templates',
  scheduler = 'scheduler',
  roles = 'roles',
  self_registration = 'self_registration',
  task_manager = 'task_manager',
  trendz_settings = 'trendz_settings',
  secrets = 'secrets',
  ai_models = 'ai_models',
  iot_hub = 'iot_hub',
  reporting = 'reporting',
  report_templates = 'report_templates',
  report_scheduling = 'report_scheduling',
  reports = 'reports',
  trendz_analytics = 'trendz_analytics'
}

declare type MenuFilter = (_authState: AuthState, userPermissionsService: UserPermissionsService) => boolean;

export const menuSectionMap = new Map<MenuId, MenuSection>([
  [
    MenuId.home,
    {
      id: MenuId.home,
      name: 'home.home',
      type: 'link',
      path: '/home',
      icon: 'home',
      outlinedIcon: 'mdi:home-outline'
    }
  ],
  [
    MenuId.tenants_section,
    {
      id: MenuId.tenants_section,
      name: 'tenant.tenants',
      type: 'toggle',
      path: '/tenants',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline'
    }
  ],
  [
    MenuId.tenants,
    {
      id: MenuId.tenants,
      name: 'tenant.tenants',
      type: 'link',
      path: '/tenants',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline'
    }
  ],
  [
    MenuId.tenant_profiles,
    {
      id: MenuId.tenant_profiles,
      name: 'tenant-profile.tenant-profiles',
      type: 'link',
      path: '/tenantProfiles',
      icon: 'mdi:alpha-t-box',
      outlinedIcon: 'mdi:alpha-t-box-outline'
    }
  ],
  [
    MenuId.resources,
    {
      id: MenuId.resources,
      name: 'admin.resources',
      type: 'toggle',
      path: '/resources',
      icon: 'folder',
      outlinedIcon: 'mdi:folder-outline'
    }
  ],
  [
    MenuId.widget_library,
    {
      id: MenuId.widget_library,
      name: 'widget.widgets',
      type: 'link',
      path: '/resources/widgets-library',
      icon: 'now_widgets',
      outlinedIcon: 'mdi:widgets-outline'
    }
  ],
  [
    MenuId.widget_types,
    {
      id: MenuId.widget_types,
      name: 'widget.widgets',
      type: 'link',
      path: '/resources/widgets-library/widget-types',
      icon: 'now_widgets',
      outlinedIcon: 'mdi:widgets-outline'
    }
  ],
  [
    MenuId.widgets_bundles,
    {
      id: MenuId.widgets_bundles,
      name: 'widgets-bundle.widgets-bundles',
      type: 'link',
      path: '/resources/widgets-library/widgets-bundles',
      icon: 'now_widgets',
      outlinedIcon: 'mdi:widgets-outline'
    }
  ],
  [
    MenuId.images,
    {
      id: MenuId.images,
      name: 'image.images',
      type: 'link',
      path: '/resources/images',
      icon: 'filter',
      outlinedIcon: 'filter'
    }
  ],
  [
    MenuId.scada_symbols,
    {
      id: MenuId.scada_symbols,
      name: 'scada.symbols',
      type: 'link',
      path: '/resources/scada-symbols',
      icon: 'view_in_ar',
      outlinedIcon: 'view_in_ar'
    }
  ],
  [
    MenuId.resources_library,
    {
      id: MenuId.resources_library,
      name: 'resource.files',
      type: 'link',
      path: '/resources/resources-library',
      icon: 'mdi:rhombus-split',
      outlinedIcon: 'mdi:rhombus-split-outline'
    }
  ],
  [
    MenuId.javascript_library,
    {
      id: MenuId.javascript_library,
      name: 'javascript.scripts',
      type: 'link',
      path: '/resources/javascript-library',
      icon: 'mdi:language-javascript',
      outlinedIcon: 'mdi:language-javascript'
    }
  ],
  [
    MenuId.notifications_center,
    {
      id: MenuId.notifications_center,
      name: 'notification.notifications',
      type: 'link',
      path: '/notification',
      icon: 'mdi:message-badge',
      outlinedIcon: 'mdi:message-badge-outline'
    }
  ],
  [
    MenuId.notification_inbox,
    {
      id: MenuId.notification_inbox,
      name: 'notification.inbox',
      fullName: 'notification.notification-inbox',
      type: 'link',
      path: '/notification/inbox',
      icon: 'inbox',
      outlinedIcon: 'inbox'
    }
  ],
  [
    MenuId.notification_sent,
    {
      id: MenuId.notification_sent,
      name: 'notification.sent',
      fullName: 'notification.notification-sent',
      type: 'link',
      path: '/notification/sent',
      icon: 'outbox',
      outlinedIcon: 'outbox'
    }
  ],
  [
    MenuId.notification_recipients,
    {
      id: MenuId.notification_recipients,
      name: 'notification.recipients',
      fullName: 'notification.notification-recipients',
      type: 'link',
      path: '/notification/recipients',
      icon: 'contacts',
      outlinedIcon: 'mdi:contacts-outline'
    }
  ],
  [
    MenuId.notification_templates,
    {
      id: MenuId.notification_templates,
      name: 'notification.templates',
      fullName: 'notification.notification-templates',
      type: 'link',
      path: '/notification/templates',
      icon: 'mdi:message-draw',
      outlinedIcon: 'mdi:message-draw'
    }
  ],
  [
    MenuId.notification_rules,
    {
      id: MenuId.notification_rules,
      name: 'notification.rules',
      fullName: 'notification.notification-rules',
      type: 'link',
      path: '/notification/rules',
      icon: 'mdi:message-cog',
      outlinedIcon: 'mdi:message-cog-outline'
    }
  ],
  [
    MenuId.ai_models,
    {
      id: MenuId.ai_models,
      name: 'ai-models.ai-models',
      type: 'link',
      path: '/settings/ai-models',
      icon: 'auto_awesome',
      outlinedIcon: 'auto_awesome'
    }
  ],
  [
    MenuId.mobile_center,
    {
      id: MenuId.mobile_center,
      name: 'mobile.mobile-apps',
      type: 'link',
      path: '/mobile-center',
      icon: 'smartphone',
      outlinedIcon: 'smartphone'
    }
  ],
  [
    MenuId.mobile_apps,
    {
      id: MenuId.mobile_apps,
      name: 'mobile.applications',
      type: 'link',
      path: '/mobile-center/applications',
      icon: 'list',
      outlinedIcon: 'list'
    }
  ],
  [
    MenuId.mobile_bundles,
    {
      id: MenuId.mobile_bundles,
      name: 'mobile.bundles',
      type: 'link',
      path: '/mobile-center/bundles',
      icon: 'mdi:package',
      outlinedIcon: 'mdi:package'
    }
  ],
  [
    MenuId.mobile_qr_code_widget,
    {
      id: MenuId.mobile_qr_code_widget,
      name: 'mobile.qr-code-widget',
      fullName: 'mobile.qr-code-widget',
      type: 'link',
      path: '/mobile-center/qr-code-widget',
      icon: 'qr_code',
      outlinedIcon: 'qr_code'
    }
  ],
  [
    MenuId.platform,
    {
      id: MenuId.platform,
      name: 'admin.platform',
      type: 'link',
      path: '/settings',
      icon: 'miscellaneous_services',
      outlinedIcon: 'miscellaneous_services'
    }
  ],
  [
    MenuId.platform_section,
    {
      id: MenuId.platform_section,
      name: 'admin.platform',
      type: 'toggle',
      path: '/platform',
      icon: 'miscellaneous_services',
      outlinedIcon: 'miscellaneous_services'
    }
  ],
  [
    MenuId.settings,
    {
      id: MenuId.settings,
      name: 'admin.settings',
      type: 'link',
      path: '/settings',
      icon: 'settings',
      outlinedIcon: 'mdi:cog-outline'
    }
  ],
  [
    MenuId.general,
    {
      id: MenuId.general,
      name: 'admin.general',
      fullName: 'admin.general-settings',
      type: 'link',
      path: '/settings/general',
      icon: 'settings',
      outlinedIcon: 'mdi:cog-outline'
    }
  ],
  [
    MenuId.mail_server,
    {
      id: MenuId.mail_server,
      name: 'admin.outgoing-mail',
      type: 'link',
      path: '/settings/outgoing-mail',
      icon: 'mail',
      outlinedIcon: 'mail_outline'
    }
  ],
  [
    MenuId.home_settings,
    {
      id: MenuId.home_settings,
      name: 'admin.home',
      fullName: 'admin.home-settings',
      type: 'link',
      path: '/settings/home',
      icon: 'settings',
      outlinedIcon: 'mdi:cog-outline'
    }
  ],
  [
    MenuId.notification_settings,
    {
      id: MenuId.notification_settings,
      name: 'admin.notifications',
      fullName: 'admin.notifications-settings',
      type: 'link',
      path: '/settings/notifications',
      icon: 'mdi:message-badge',
      outlinedIcon: 'mdi:message-badge-outline'
    }
  ],
  [
    MenuId.repository_settings,
    {
      id: MenuId.repository_settings,
      name: 'admin.repository',
      fullName: 'admin.repository-settings',
      type: 'link',
      path: '/settings/repository',
      icon: 'manage_history',
      outlinedIcon: 'manage_history'
    }
  ],
  [
    MenuId.auto_commit_settings,
    {
      id: MenuId.auto_commit_settings,
      name: 'admin.auto-commit',
      fullName: 'admin.auto-commit-settings',
      type: 'link',
      path: '/settings/auto-commit',
      icon: 'settings_backup_restore',
      outlinedIcon: 'settings_backup_restore'
    }
  ],
  [
    MenuId.queues,
    {
      id: MenuId.queues,
      name: 'admin.queues',
      type: 'link',
      path: '/settings/queues',
      icon: 'swap_calls',
      outlinedIcon: 'swap_calls'
    }
  ],
  [
    MenuId.license_management,
    {
      id: MenuId.license_management,
      name: 'subscription.license-management',
      type: 'link',
      path: '/license',
      icon: 'mdi:file-cog',
      outlinedIcon: 'mdi:file-cog-outline'
    }
  ],
  [
    MenuId.security_settings,
    {
      id: MenuId.security_settings,
      name: 'security.security',
      type: 'toggle',
      path: '/security-settings',
      icon: 'security',
      outlinedIcon: 'security'
    }
  ],
  [
    MenuId.security_settings_general,
    {
      id: MenuId.security_settings_general,
      name: 'admin.general',
      fullName: 'security.general-settings',
      type: 'link',
      path: '/security-settings/general',
      icon: 'settings',
      outlinedIcon: 'mdi:cog-outline'
    }
  ],
  [
    MenuId.two_fa,
    {
      id: MenuId.two_fa,
      name: 'admin.2fa.2fa',
      type: 'link',
      path: '/security-settings/2fa',
      icon: 'mdi:two-factor-authentication',
      outlinedIcon: 'mdi:two-factor-authentication'
    }
  ],
  [
    MenuId.oauth2,
    {
      id: MenuId.oauth2,
      name: 'admin.oauth2.oauth2',
      type: 'link',
      path: '/security-settings/oauth2',
      icon: 'mdi:shield-account',
      outlinedIcon: 'mdi:shield-account-outline'
    }
  ],
  [
    MenuId.domains,
    {
      id: MenuId.domains,
      name: 'admin.oauth2.domains',
      type: 'link',
      path: '/security-settings/oauth2/domains',
      icon: 'domain',
      outlinedIcon: 'domain'
    }
  ],
  [
    MenuId.clients,
    {
      id: MenuId.clients,
      name: 'admin.oauth2.clients',
      type: 'link',
      path: '/security-settings/oauth2/clients',
      icon: 'public',
      outlinedIcon: 'public'
    }
  ],
  [
    MenuId.audit_log,
    {
      id: MenuId.audit_log,
      name: 'audit-log.audit-logs',
      type: 'link',
      path: '/security-settings/auditLogs',
      icon: 'track_changes',
      outlinedIcon: 'track_changes'
    }
  ],
  [
    MenuId.monitor,
    {
      id: MenuId.monitor,
      name: 'monitor.monitor',
      type: 'toggle',
      path: '/monitor',
      icon: 'mdi:view-dashboard',
      outlinedIcon: 'mdi:view-dashboard-outline'
    }
  ],
  [
    MenuId.alarms_center,
    {
      id: MenuId.alarms_center,
      name: 'alarm.alarms',
      type: 'link',
      path: '/alarms',
      icon: 'mdi:alert',
      outlinedIcon: 'mdi:alert-outline'
    }
  ],
  [
    MenuId.alarms,
    {
      id: MenuId.alarms,
      name: 'alarm.alarm-list',
      type: 'link',
      path: '/alarms/alarms',
      icon: 'mdi:alert',
      outlinedIcon: 'mdi:alert-outline'
    }
  ],
  [
    MenuId.alarm_rules,
    {
      id: MenuId.alarm_rules,
      name: 'alarm-rule.alarm-rules',
      type: 'link',
      path: '/alarms/alarm-rules',
      icon: 'tune',
      outlinedIcon: 'tune'
    }
  ],
  [
    MenuId.dashboards,
    {
      id: MenuId.dashboards,
      name: 'dashboard.dashboards',
      type: 'link',
      path: '/dashboards',
      icon: 'dashboards',
      outlinedIcon: 'mdi:view-dashboard-outline'
    }
  ],
  [
    MenuId.entities,
    {
      id: MenuId.entities,
      name: 'entity.devices-and-assets',
      type: 'toggle',
      path: '/entities',
      icon: 'category',
      outlinedIcon: 'mdi:shape-outline'
    }
  ],
  [
    MenuId.devices,
    {
      id: MenuId.devices,
      name: 'device.devices',
      type: 'link',
      path: '/entities/devices',
      icon: 'devices_other',
      outlinedIcon: 'devices_other'
    }
  ],
  [
    MenuId.assets,
    {
      id: MenuId.assets,
      name: 'asset.assets',
      type: 'link',
      path: '/entities/assets',
      icon: 'domain',
      outlinedIcon: 'domain'
    }
  ],
  [
    MenuId.entity_views,
    {
      id: MenuId.entity_views,
      name: 'entity-view.entity-views',
      type: 'link',
      path: '/entities/entityViews',
      icon: 'view_quilt',
      outlinedIcon: 'mdi:view-quilt-outline'
    }
  ],
  [
    MenuId.gateways,
    {
      id: MenuId.gateways,
      name: 'gateway.gateways',
      type: 'link',
      path: '/entities/gateways',
      icon: 'lan',
      outlinedIcon: 'lan'
    }
  ],
  [
    MenuId.profiles,
    {
      id: MenuId.profiles,
      name: 'profiles.profiles',
      type: 'toggle',
      path: '/profiles',
      icon: 'badge',
      outlinedIcon: 'badge'
    }
  ],
  [
    MenuId.device_profiles,
    {
      id: MenuId.device_profiles,
      name: 'device-profile.device-profiles',
      type: 'link',
      path: '/profiles/deviceProfiles',
      icon: 'mdi:alpha-d-box',
      outlinedIcon: 'mdi:alpha-d-box-outline'
    }
  ],
  [
    MenuId.asset_profiles,
    {
      id: MenuId.asset_profiles,
      name: 'asset-profile.asset-profiles',
      type: 'link',
      path: '/profiles/assetProfiles',
      icon: 'mdi:alpha-a-box',
      outlinedIcon: 'mdi:alpha-a-box-outline'
    }
  ],
  [
    MenuId.customers_and_users,
    {
      id: MenuId.customers_and_users,
      name: 'customer.customers-and-users',
      type: 'toggle',
      path: '/customersAndUsers',
      icon: 'mdi:account-multiple',
      outlinedIcon: 'mdi:account-multiple-outline'
    }
  ],
  [
    MenuId.customers,
    {
      id: MenuId.customers,
      name: 'customer.customers',
      type: 'link',
      path: '/customers',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline'
    }
  ],
  [
    MenuId.data_processing,
    {
      id: MenuId.data_processing,
      name: 'entity.data-processing',
      type: 'toggle',
      path: '/dataProcessing',
      icon: 'settings_ethernet',
      outlinedIcon: 'settings_ethernet'
    }
  ],
  [
    MenuId.calculated_fields,
    {
      id: MenuId.calculated_fields,
      name: 'entity.type-calculated-fields',
      type: 'link',
      path: '/calculatedFields',
      icon: 'mdi:function-variant',
      outlinedIcon: 'mdi:function-variant'
    }
  ],
  [
    MenuId.rule_chains,
    {
      id: MenuId.rule_chains,
      name: 'rulechain.rulechains',
      type: 'link',
      path: '/ruleChains',
      icon: 'settings_ethernet',
      outlinedIcon: 'settings_ethernet'
    }
  ],
  [
    MenuId.edge_management,
    {
      id: MenuId.edge_management,
      name: 'edge.management',
      type: 'toggle',
      path: '/edgeManagement',
      icon: 'settings_input_antenna',
      outlinedIcon: 'settings_input_antenna'
    }
  ],
  [
    MenuId.edges,
    {
      id: MenuId.edges,
      name: 'edge.edges',
      fullName: 'edge.edge-instances',
      type: 'link',
      path: '/edgeManagement/edges',
      icon: 'router',
      outlinedIcon: 'router'
    }
  ],
  [
    MenuId.edge_instances,
    {
      id: MenuId.edge_instances,
      name: 'edge.edge-instances',
      fullName: 'edge.edge-instances',
      type: 'link',
      path: '/edgeManagement/edges',
      icon: 'router',
      outlinedIcon: 'router'
    }
  ],
  [
    MenuId.rulechain_templates,
    {
      id: MenuId.rulechain_templates,
      name: 'edge.rulechain-templates',
      fullName: 'edge.edge-rulechain-templates',
      type: 'link',
      path: '/edgeManagement/templates/ruleChains',
      icon: 'settings_ethernet',
      outlinedIcon: 'settings_ethernet'
    }
  ],
  [
    MenuId.agents,
    {
      id: MenuId.agents,
      name: 'agent.agents',
      fullName: 'agent.agents',
      type: 'link',
      path: '/edgeManagement/agents',
      icon: 'memory',
      outlinedIcon: 'memory',
      isNew: true
    }
  ],
  [
    MenuId.agent_all,
    {
      id: MenuId.agent_all,
      name: 'agent.all',
      fullName: 'agent.all-agents',
      type: 'link',
      path: '/edgeManagement/agents/all',
      icon: 'memory',
      outlinedIcon: 'memory'
    }
  ],
  [
    MenuId.agent_groups,
    {
      id: MenuId.agent_groups,
      name: 'agent.groups',
      fullName: 'entity-group.agent-groups',
      type: 'link',
      path: '/edgeManagement/agents/groups',
      icon: 'memory',
      outlinedIcon: 'memory'
    }
  ],
  [
    MenuId.agent_shared,
    {
      id: MenuId.agent_shared,
      name: 'agent.shared',
      fullName: 'entity-group.shared-agent-groups',
      type: 'link',
      path: '/edgeManagement/agents/shared',
      icon: 'memory',
      outlinedIcon: 'memory',
      rootOnly: true
    }
  ],
  [
    MenuId.edge_profiles,
    {
      id: MenuId.edge_profiles,
      name: 'edge.profiles',
      type: 'link',
      path: '/edgeManagement/profiles',
      icon: 'badge',
      outlinedIcon: 'badge'
    }
  ],
  [
    MenuId.edge_templates,
    {
      id: MenuId.edge_templates,
      name: 'edge.templates',
      type: 'link',
      path: '/edgeManagement/templates',
      icon: 'dashboard',
      outlinedIcon: 'dashboard'
    }
  ],
  [
    MenuId.features,
    {
      id: MenuId.features,
      name: 'feature.advanced-features',
      type: 'toggle',
      path: '/features',
      icon: 'construction',
      outlinedIcon: 'construction'
    }
  ],
  [
    MenuId.otaUpdates,
    {
      id: MenuId.otaUpdates,
      name: 'ota-update.ota-updates',
      type: 'link',
      path: '/features/otaUpdates',
      icon: 'memory',
      outlinedIcon: 'memory'
    }
  ],
  [
    MenuId.version_control,
    {
      id: MenuId.version_control,
      name: 'version-control.version-control',
      type: 'link',
      path: '/features/vc',
      icon: 'history',
      outlinedIcon: 'history'
    }
  ],
  [
    MenuId.api_usage,
    {
      id: MenuId.api_usage,
      name: 'api-usage.api-usage',
      type: 'link',
      path: '/usage',
      icon: 'insert_chart',
      outlinedIcon: 'insert_chart_outlined'
    }
  ],
  [
    MenuId.white_labeling,
    {
      id: MenuId.white_labeling,
      name: 'white-labeling.white-labeling',
      type: 'link',
      path: '/white-labeling',
      icon: 'format_paint',
      outlinedIcon: 'format_paint'
    }
  ],
  [
    MenuId.white_labeling_general,
    {
      id: MenuId.white_labeling_general,
      name: 'white-labeling.general',
      fullName: 'white-labeling.white-labeling-general',
      type: 'link',
      path: '/white-labeling/whiteLabel',
      icon: 'format_paint',
      outlinedIcon: 'format_paint'
    }
  ],
  [
    MenuId.login_white_labeling,
    {
      id: MenuId.login_white_labeling,
      name: 'white-labeling.login',
      fullName: 'white-labeling.login-white-labeling',
      type: 'link',
      path: '/white-labeling/loginWhiteLabel',
      icon: 'format_paint',
      outlinedIcon: 'format_paint'
    }
  ],
  [
    MenuId.mail_templates,
    {
      id: MenuId.mail_templates,
      name: 'admin.mail-templates',
      type: 'link',
      path: '/white-labeling/mail-template',
      icon: 'format_shapes',
      outlinedIcon: 'format_shapes'
    }
  ],
  [
    MenuId.custom_translation,
    {
      id: MenuId.custom_translation,
      name: 'custom-translation.custom-translation',
      type: 'link',
      path: '/white-labeling/customTranslation',
      icon: 'language',
      outlinedIcon: 'language'
    }
  ],
  [
    MenuId.custom_menu,
    {
      id: MenuId.custom_menu,
      name: 'custom-menu.custom-menu',
      type: 'link',
      path: '/white-labeling/customMenu',
      icon: 'list',
      outlinedIcon: 'list'
    }
  ],
  [
    MenuId.dashboard_all,
    {
      id: MenuId.dashboard_all,
      name: 'dashboard.all',
      fullName: 'dashboard.all-dashboards',
      type: 'link',
      path: '/dashboards/all',
      icon: 'dashboards',
      outlinedIcon: 'mdi:view-dashboard-outline'
    }
  ],
  [
    MenuId.dashboard_groups,
    {
      id: MenuId.dashboard_groups,
      name: 'dashboard.groups',
      fullName: 'entity-group.dashboard-groups',
      type: 'link',
      path: '/dashboards/groups',
      icon: 'dashboard',
      outlinedIcon: 'mdi:view-dashboard-outline'
    }
  ],
  [
    MenuId.dashboard_shared,
    {
      id: MenuId.dashboard_shared,
      name: 'dashboard.shared',
      fullName: 'entity-group.shared-dashboard-groups',
      type: 'link',
      path: '/dashboards/shared',
      icon: 'dashboard',
      outlinedIcon: 'mdi:view-dashboard-outline',
      rootOnly: true
    }
  ],
  [
    MenuId.ai_solution_creator,
    {
      id: MenuId.ai_solution_creator,
      name: 'solution-creator.ai-solution-creator',
      type: 'link',
      path: '/ai-solution-creator',
      icon: 'mdi:creation',
      outlinedIcon: 'mdi:creation-outline',
      isNew: true,
    }
  ],
  [
    MenuId.device_all,
    {
      id: MenuId.device_all,
      name: 'device.all',
      fullName: 'device.all-devices',
      type: 'link',
      path: '/entities/devices/all',
      icon: 'devices_other',
      outlinedIcon: 'devices_other'
    }
  ],
  [
    MenuId.device_groups,
    {
      id: MenuId.device_groups,
      name: 'device.groups',
      fullName: 'entity-group.device-groups',
      type: 'link',
      path: '/entities/devices/groups',
      icon: 'devices_other',
      outlinedIcon: 'devices_other'
    }
  ],
  [
    MenuId.device_shared,
    {
      id: MenuId.device_shared,
      name: 'device.shared',
      fullName: 'entity-group.shared-device-groups',
      type: 'link',
      path: '/entities/devices/shared',
      icon: 'devices_other',
      outlinedIcon: 'devices_other',
      rootOnly: true
    }
  ],
  [
    MenuId.asset_all,
    {
      id: MenuId.asset_all,
      name: 'asset.all',
      fullName: 'asset.all-assets',
      type: 'link',
      path: '/entities/assets/all',
      icon: 'domain',
      outlinedIcon: 'domain'
    }
  ],
  [
    MenuId.asset_groups,
    {
      id: MenuId.asset_groups,
      name: 'asset.groups',
      fullName: 'entity-group.asset-groups',
      type: 'link',
      path: '/entities/assets/groups',
      icon: 'domain',
      outlinedIcon: 'domain'
    }
  ],
  [
    MenuId.asset_shared,
    {
      id: MenuId.asset_shared,
      name: 'asset.shared',
      fullName: 'entity-group.shared-asset-groups',
      type: 'link',
      path: '/entities/assets/shared',
      icon: 'domain',
      outlinedIcon: 'domain',
      rootOnly: true
    }
  ],
  [
    MenuId.entity_view_all,
    {
      id: MenuId.entity_view_all,
      name: 'entity-view.all',
      fullName: 'entity-view.all-entity-views',
      type: 'link',
      path: '/entities/entityViews/all',
      icon: 'view_quilt',
      outlinedIcon: 'mdi:view-quilt-outline'
    }
  ],
  [
    MenuId.entity_view_groups,
    {
      id: MenuId.entity_view_groups,
      name: 'entity-view.groups',
      fullName: 'entity-group.entity-view-groups',
      type: 'link',
      path: '/entities/entityViews/groups',
      icon: 'view_quilt',
      outlinedIcon: 'mdi:view-quilt-outline'
    }
  ],
  [
    MenuId.entity_view_shared,
    {
      id: MenuId.entity_view_shared,
      name: 'entity-view.shared',
      fullName: 'entity-group.shared-entity-view-groups',
      type: 'link',
      path: '/entities/entityViews/shared',
      icon: 'view_quilt',
      outlinedIcon: 'mdi:view-quilt-outline',
      rootOnly: true
    }
  ],
  [
    MenuId.customer_all,
    {
      id: MenuId.customer_all,
      name: 'customer.all',
      fullName: 'customer.all-customers',
      type: 'link',
      path: '/customers/all',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline'
    }
  ],
  [
    MenuId.customer_groups,
    {
      id: MenuId.customer_groups,
      name: 'customer.groups',
      fullName: 'entity-group.customer-groups',
      type: 'link',
      path: '/customers/groups',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline'
    }
  ],
  [
    MenuId.customer_shared,
    {
      id: MenuId.customer_shared,
      name: 'customer.shared',
      fullName: 'entity-group.shared-customer-groups',
      type: 'link',
      path: '/customers/shared',
      icon: 'supervisor_account',
      outlinedIcon: 'mdi:account-supervisor-outline',
      rootOnly: true
    }
  ],
  [
    MenuId.customers_hierarchy,
    {
      id: MenuId.customers_hierarchy,
      name: 'customer.hierarchy',
      fullName: 'customers-hierarchy.customers-hierarchy',
      type: 'link',
      path: '/customers/hierarchy',
      icon: 'sort',
      outlinedIcon: 'sort',
      rootOnly: true
    }
  ],
  [
    MenuId.users,
    {
      id: MenuId.users,
      name: 'user.users',
      type: 'link',
      path: '/users',
      icon: 'account_circle',
      outlinedIcon: 'mdi:account-circle-outline'
    }
  ],
  [
    MenuId.user_all,
    {
      id: MenuId.user_all,
      name: 'user.all',
      fullName: 'user.all-users',
      type: 'link',
      path: '/users/all',
      icon: 'account_circle',
      outlinedIcon: 'mdi:account-circle-outline'
    }
  ],
  [
    MenuId.user_groups,
    {
      id: MenuId.user_groups,
      name: 'user.groups',
      fullName: 'entity-group.user-groups',
      type: 'link',
      path: '/users/groups',
      icon: 'account_circle',
      outlinedIcon: 'mdi:account-circle-outline'
    }
  ],
  [
    MenuId.integrations_center,
    {
      id: MenuId.integrations_center,
      name: 'integration.integrations',
      type: 'toggle',
      path: '/integrationsCenter',
      icon: 'integration_instructions',
      outlinedIcon: 'mdi:code-block-tags'
    }
  ],
  [
    MenuId.integrations,
    {
      id: MenuId.integrations,
      name: 'integration.integrations',
      type: 'link',
      path: '/integrationsCenter/integrations',
      icon: 'input',
      outlinedIcon: 'input'
    }
  ],
  [
    MenuId.converters,
    {
      id: MenuId.converters,
      name: 'converter.converters',
      type: 'link',
      path: '/integrationsCenter/converters',
      icon: 'transform',
      outlinedIcon: 'transform'
    }
  ],
  [
    MenuId.edge_all,
    {
      id: MenuId.edge_all,
      name: 'edge.all',
      fullName: 'edge.all-edges',
      type: 'link',
      path: '/edgeManagement/edges/all',
      icon: 'router',
      outlinedIcon: 'router'
    }
  ],
  [
    MenuId.edge_groups,
    {
      id: MenuId.edge_groups,
      name: 'edge.groups',
      fullName: 'entity-group.edge-groups',
      type: 'link',
      path: '/edgeManagement/edges/groups',
      icon: 'router',
      outlinedIcon: 'router'
    }
  ],
  [
    MenuId.edge_shared,
    {
      id: MenuId.edge_shared,
      name: 'edge.shared',
      fullName: 'entity-group.shared-edge-groups',
      type: 'link',
      path: '/edgeManagement/edges/shared',
      icon: 'router',
      outlinedIcon: 'router',
      rootOnly: true
    }
  ],
  [
    MenuId.integration_templates,
    {
      id: MenuId.integration_templates,
      name: 'edge.integration-templates',
      fullName: 'edge.edge-integration-templates',
      type: 'link',
      path: '/edgeManagement/templates/integrations',
      icon: 'input',
      outlinedIcon: 'input'
    }
  ],
  [
    MenuId.converter_templates,
    {
      id: MenuId.converter_templates,
      name: 'edge.converter-templates',
      fullName: 'edge.edge-converter-templates',
      type: 'link',
      path: '/edgeManagement/templates/converters',
      icon: 'transform',
      outlinedIcon: 'transform'
    }
  ],
  [
    MenuId.scheduler,
    {
      id: MenuId.scheduler,
      name: 'scheduler.scheduler',
      type: 'link',
      path: '/features/scheduler',
      icon: 'schedule',
      outlinedIcon: 'schedule'
    }
  ],
  [
    MenuId.roles,
    {
      id: MenuId.roles,
      name: 'role.roles',
      type: 'link',
      path: '/roles',
      icon: 'security',
      outlinedIcon: 'security'
    }
  ],
  [
    MenuId.secrets,
    {
      id: MenuId.secrets,
      name: 'secret-storage.secrets-storage',
      type: 'link',
      path: '/security-settings/secrets',
      icon: 'mdi:key-variant',
      outlinedIcon: 'mdi:key-variant'
    }
  ],
  [
    MenuId.self_registration,
    {
      id: MenuId.self_registration,
      name: 'self-registration.self-registration',
      type: 'link',
      path: '/security-settings/selfRegistration',
      icon: 'group_add',
      outlinedIcon: 'group_add'
    }
  ],
  [
    MenuId.task_manager,
    {
      id: MenuId.task_manager,
      name: 'task.task-manager',
      type: 'link',
      path: '/features/taskManager',
      icon: 'mdi:invoice-text-clock',
      outlinedIcon: 'mdi:invoice-text-clock-outline'
    }
  ],
  [
    MenuId.reporting,
    {
      id: MenuId.reporting,
      name: 'report.reports',
      type: 'link',
      path: '/reporting',
      icon: 'mdi:chart-box-multiple',
      outlinedIcon: 'mdi:chart-box-multiple-outline'
    }
  ],
  [
    MenuId.report_templates,
    {
      id: MenuId.report_templates,
      name: 'report.templates',
      type: 'link',
      path: '/reporting/templates',
      icon: 'mdi:file-document-edit',
      outlinedIcon: 'mdi:file-document-edit-outline'
    }
  ],
  [
    MenuId.report_scheduling,
    {
      id: MenuId.report_scheduling,
      name: 'report.scheduling',
      type: 'link',
      path: '/reporting/scheduling',
      icon: 'mdi:file-clock',
      outlinedIcon: 'mdi:file-clock-outline'
    }
  ],
  [
    MenuId.reports,
    {
      id: MenuId.reports,
      name: 'report.reports',
      type: 'link',
      path: '/reporting/reports',
      icon: 'mdi:chart-box-multiple',
      outlinedIcon: 'mdi:chart-box-multiple-outline'
    }
  ],
  [
    MenuId.trendz_analytics,
    {
      id: MenuId.trendz_analytics,
      name: 'trendz-analytics.trendz-analytics',
      type: 'link',
      path: '/analytics',
      icon: 'trendz',
      outlinedIcon: 'trendz'
    }
  ],
  [
    MenuId.trendz_settings,
    {
      id: MenuId.trendz_settings,
      name: 'trendz-analytics.trendz',
      type: 'link',
      path: '/trendzSettings',
      icon: 'trendz',
      outlinedIcon: 'trendz'
    }
  ],
  [
    MenuId.iot_hub,
    {
      id: MenuId.iot_hub,
      name: 'iot-hub.iot-hub',
      type: 'link',
      path: '/iot-hub',
      icon: 'hub',
      outlinedIcon: 'mdi:hub-outline',
      isNew: true
    }
  ]
]);

const menuFilters = new Map<MenuId, MenuFilter>([
  [
    MenuId.alarms, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ALARM)
  ],
  [
    MenuId.alarm_rules, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          alarmRuleEntityTypeList.some(entityType => userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.READ_CALCULATED_FIELD, entityType))
  ],
  [
    MenuId.dashboard_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD)
  ],
  [
    MenuId.dashboard_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.DASHBOARD)
  ],
  [
    MenuId.dashboard_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.DASHBOARD)
  ],
  [
    MenuId.ai_solution_creator, (authState, userPermissionsService) =>
        authState.aiEnabled &&
        authState.authUser.authority === Authority.TENANT_ADMIN &&
        userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL)
  ],
  [
    MenuId.iot_hub, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasGenericPermission(Resource.ALL, Operation.ALL)
  ],
  [
    MenuId.device_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.DEVICE)
  ],
  [
    MenuId.device_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.DEVICE)
  ],
  [
    MenuId.device_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.DEVICE)
  ],
  [
    MenuId.asset_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ASSET)
  ],
  [
    MenuId.asset_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.ASSET)
  ],
  [
    MenuId.asset_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.ASSET)
  ],
  [
    MenuId.entity_view_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.ENTITY_VIEW)
  ],
  [
    MenuId.entity_view_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.ENTITY_VIEW)
  ],
  [
    MenuId.entity_view_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.ENTITY_VIEW)
  ],
  [
    MenuId.gateways, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE) &&
          (userPermissionsService.hasReadGenericPermission(Resource.DASHBOARD) ||
            userPermissionsService.hasReadGenericPermission(Resource.WIDGET_TYPE))
  ],
  [
    MenuId.device_profiles, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.DEVICE_PROFILE)
  ],
  [
    MenuId.asset_profiles, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.ASSET_PROFILE)
  ],
  [
    MenuId.customer_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.CUSTOMER)
  ],
  [
    MenuId.customer_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.customer_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.customers_hierarchy, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGroupsPermission(EntityType.CUSTOMER)
  ],
  [
    MenuId.user_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.USER)
  ],
  [
    MenuId.user_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.USER)
  ],
  [
    MenuId.integrations, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)
  ],
  [
    MenuId.converters, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN &&
          userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)
  ],
  [
    MenuId.calculated_fields, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN
          && calculatedFieldsEntityTypeList.some(entityType => userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.READ_CALCULATED_FIELD, entityType))
  ],
  [
    MenuId.rule_chains, (authState, userPermissionsService) =>
          authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)
  ],
  [
    MenuId.edge_all, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasReadGenericPermission(Resource.EDGE)
  ],
  [
    MenuId.edge_groups, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasGenericReadGroupsPermission(EntityType.EDGE)
  ],
  [
    MenuId.edge_shared, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && userPermissionsService.hasSharedReadGroupsPermission(EntityType.EDGE)
  ],
  [
    MenuId.agent_all, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.AGENT)
  ],
  [
    MenuId.agent_groups, (_authState, userPermissionsService) =>
          userPermissionsService.hasGenericReadGroupsPermission(EntityType.AGENT)
  ],
  [
    MenuId.agent_shared, (_authState, userPermissionsService) =>
          userPermissionsService.hasSharedReadGroupsPermission(EntityType.AGENT)
  ],
  [
    MenuId.edge_profiles, (_authState, userPermissionsService) =>
          userPermissionsService.hasReadGenericPermission(Resource.AGENT_PROFILE) ||
          userPermissionsService.hasReadGenericPermission(Resource.AGENT_APP_PROFILE)
  ],
  [
    MenuId.edge_templates, (authState, userPermissionsService) =>
          authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
          (userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN) ||
            userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION) ||
            userPermissionsService.hasReadGenericPermission(Resource.CONVERTER))
  ],
  [
    MenuId.rulechain_templates, (authState, userPermissionsService) =>
      authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
      userPermissionsService.hasReadGenericPermission(Resource.RULE_CHAIN)
  ],
  [
    MenuId.integration_templates, (authState, userPermissionsService) =>
      authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
      userPermissionsService.hasReadGenericPermission(Resource.INTEGRATION)
  ],
  [
    MenuId.converter_templates, (authState, userPermissionsService) =>
           authState.edgesSupportEnabled && authState.authUser.authority === Authority.TENANT_ADMIN &&
           userPermissionsService.hasReadGenericPermission(Resource.CONVERTER)
  ],
  [
    MenuId.otaUpdates, (authState, userPermissionsService) =>
           authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.OTA_PACKAGE)
  ],
  [
    MenuId.version_control, (authState, userPermissionsService) =>
           authState.authUser.authority === Authority.TENANT_ADMIN &&
           userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.widget_types, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN && userPermissionsService.hasReadGenericPermission(Resource.WIDGET_TYPE)
  ],
  [
    MenuId.widgets_bundles, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.WIDGETS_BUNDLE)
  ],
  [
    MenuId.resources_library, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)
  ],
  [
    MenuId.javascript_library, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.TB_RESOURCE)
  ],
  [
    MenuId.notification_sent, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_recipients, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_templates, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.notification_rules, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.NOTIFICATION)
  ],
  [
    MenuId.api_usage, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.API_USAGE_STATE) &&
            userPermissionsService.hasGenericPermission(Resource.API_USAGE_STATE, Operation.READ_TELEMETRY)
  ],
  [
    MenuId.white_labeling, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.white_labeling_general, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.login_white_labeling, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.mail_templates, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.custom_translation, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.custom_menu, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.home_settings, (authState, userPermissionsService) =>
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.mail_server, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.notification_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.repository_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.auto_commit_settings, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.VERSION_CONTROL)
  ],
  [
    MenuId.mobile_bundles, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP_BUNDLE)
  ],
  [
    MenuId.mobile_apps, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP)
  ],
  [
    MenuId.mobile_qr_code_widget, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.MOBILE_APP_SETTINGS)
  ],
  [
    MenuId.two_fa, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.secrets, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.SECRET)
  ],
  [
    MenuId.roles, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.ROLE)
  ],
  [
    MenuId.clients, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.OAUTH2_CLIENT)
  ],
  [
    MenuId.domains, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.DOMAIN)
  ],
  [
    MenuId.self_registration, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            authState.whiteLabelingAllowed && userPermissionsService.hasReadGenericPermission(Resource.WHITE_LABELING)
  ],
  [
    MenuId.audit_log, (_authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.AUDIT_LOG)
  ],
  [
    MenuId.task_manager, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.JOB)
  ],
  [
    MenuId.report_templates, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.REPORT_TEMPLATE)
  ],
  [
    MenuId.report_scheduling, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.REPORT_TEMPLATE) &&
            userPermissionsService.hasReadGenericPermission(Resource.SCHEDULER_EVENT)
  ],
  [
    MenuId.reports, (authState, userPermissionsService) =>
            userPermissionsService.hasReadGenericPermission(Resource.REPORT)
  ],
  [
    MenuId.license_management, (authState, userPermissionsService) =>
    authState.authUser.authority === Authority.SYS_ADMIN && authState.licenseVersion > 1
  ],
  [
    MenuId.ai_models, (authState, userPermissionsService) =>
            authState.authUser.authority === Authority.TENANT_ADMIN &&
            userPermissionsService.hasReadGenericPermission(Resource.AI_MODEL)
  ],
  [
    MenuId.trendz_settings, (authState) =>
            authState.authUser.authority === Authority.SYS_ADMIN
  ]
]);

export const defaultUserMenuMap = new Map<Authority, MenuReference[]>([
  [
    Authority.SYS_ADMIN,
    [
      {id: MenuId.home},
      {
        id: MenuId.tenants_section,
        pages: [
          {id: MenuId.tenants},
          {id: MenuId.tenant_profiles},
        ]
      },
      {
        id: MenuId.notifications_center,
        pages: [
          {id: MenuId.notification_inbox},
          {id: MenuId.notification_sent},
          {id: MenuId.notification_recipients},
          {id: MenuId.notification_templates},
          {id: MenuId.notification_rules}
        ]
      },
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.mail_templates},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.resources,
        pages: [
          {
            id: MenuId.widget_library,
            pages: [
              {id: MenuId.widget_types},
              {id: MenuId.widgets_bundles}
            ]
          },
          {id: MenuId.images},
          {id: MenuId.scada_symbols},
          {id: MenuId.javascript_library},
          {id: MenuId.resources_library}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {id: MenuId.security_settings_general},
          {id: MenuId.two_fa},
          {id: MenuId.secrets},
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          },
          {id: MenuId.audit_log}
        ]
      },
      {
        id: MenuId.platform_section,
        pages: [
          {
            id: MenuId.settings,
            pages: [
              {id: MenuId.general},
              {id: MenuId.mail_server},
              {id: MenuId.notification_settings},
              {id: MenuId.queues}
            ]
          },
          {id: MenuId.trendz_settings},
          {id: MenuId.license_management}
        ]
      },
      {
        id: MenuId.mobile_center,
        pages: [
          {id: MenuId.mobile_bundles},
          {id: MenuId.mobile_apps},
          {id: MenuId.mobile_qr_code_widget}
        ]
      }
    ]
  ],
  [
    Authority.TENANT_ADMIN,
    [
      {id: MenuId.home},
      {id: MenuId.iot_hub},
      {id: MenuId.ai_solution_creator},
      {
        id: MenuId.monitor,
        pages: [
          {
            id: MenuId.dashboards,
            pages: [
              {id: MenuId.dashboard_all},
              {id: MenuId.dashboard_groups},
              {id: MenuId.dashboard_shared}
            ]
          },
          {
            id: MenuId.alarms_center,
            pages: [
              {id: MenuId.alarms},
              {id: MenuId.alarm_rules}
            ]
          },
          {
            id: MenuId.notifications_center,
            pages: [
              {id: MenuId.notification_inbox},
              {id: MenuId.notification_sent},
              {id: MenuId.notification_recipients},
              {id: MenuId.notification_templates},
              {id: MenuId.notification_rules}
            ]
          },
          {
            id: MenuId.reporting,
            pages: [
              {id: MenuId.report_templates},
              {id: MenuId.report_scheduling},
              {id: MenuId.reports}
            ]
          }
        ]
      },
      {
        id: MenuId.entities,
        pages: [
          {
            id: MenuId.devices,
            pages: [
              {id: MenuId.device_all},
              {id: MenuId.device_groups},
              {id: MenuId.device_shared}
            ]
          },
          {id: MenuId.gateways},
          {
            id: MenuId.assets,
            pages: [
              {id: MenuId.asset_all},
              {id: MenuId.asset_groups},
              {id: MenuId.asset_shared}
            ]
          },
          {id: MenuId.device_profiles},
          {id: MenuId.asset_profiles},
          {
            id: MenuId.entity_views,
            pages: [
              {id: MenuId.entity_view_all},
              {id: MenuId.entity_view_groups},
              {id: MenuId.entity_view_shared}
            ]
          },
          {id: MenuId.otaUpdates}
        ]
      },
      {
        id: MenuId.customers_and_users,
        pages: [
          {
            id: MenuId.customers,
            pages: [
              {id: MenuId.customer_all},
              {id: MenuId.customer_groups},
              {id: MenuId.customer_shared},
              {id: MenuId.customers_hierarchy}
            ]
          },
          {
            id: MenuId.users,
            pages: [
              {id: MenuId.user_all},
              {id: MenuId.user_groups}
            ]
          },
          {id: MenuId.roles}
       ]
      },
      {
        id: MenuId.integrations_center,
        pages: [
          {id: MenuId.integrations},
          {id: MenuId.converters}
        ]
      },
      {
        id: MenuId.data_processing,
        pages: [
          {id: MenuId.calculated_fields},
          {id: MenuId.rule_chains}
        ]
      },
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.mail_templates},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.resources,
        pages: [
          {
            id: MenuId.widget_library,
            pages: [
              {id: MenuId.widget_types},
              {id: MenuId.widgets_bundles}
            ]
          },
          {id: MenuId.images},
          {id: MenuId.scada_symbols},
          {id: MenuId.javascript_library},
          {id: MenuId.resources_library}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {id: MenuId.two_fa},
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          },
          {id: MenuId.self_registration},
          {id: MenuId.secrets},
          {id: MenuId.audit_log}
        ]
      },
      {
        id: MenuId.platform_section,
        pages: [
          {id: MenuId.version_control},
          {id: MenuId.task_manager},
          {id: MenuId.scheduler},
          {
            id: MenuId.settings,
            pages: [
              {id: MenuId.home_settings},
              {id: MenuId.mail_server},
              {id: MenuId.notification_settings},
              {id: MenuId.repository_settings},
              {id: MenuId.auto_commit_settings},
              {id: MenuId.ai_models}
            ]
          },
          {id: MenuId.api_usage}
        ]
      },
      {
        id: MenuId.edge_management,
        pages: [
          {
            id: MenuId.agents,
            pages: [
              {id: MenuId.agent_all}
            ]
          },
          {
            id: MenuId.edges,
            pages: [
              {id: MenuId.edge_all},
              {id: MenuId.edge_groups},
              {id: MenuId.edge_shared},
            ]
          },
          {id: MenuId.edge_profiles},
          {id: MenuId.edge_templates}
        ]
      },
      {id: MenuId.trendz_analytics},
      {
        id: MenuId.mobile_center,
        pages: [
          {id: MenuId.mobile_bundles},
          {id: MenuId.mobile_apps},
          {id: MenuId.mobile_qr_code_widget}
        ]
      }
    ]
  ],
  [
    Authority.CUSTOMER_USER,
    [
      {id: MenuId.home},
      {
        id: MenuId.monitor,
        pages: [
          {
            id: MenuId.dashboards,
            pages: [
              {id: MenuId.dashboard_all},
              {id: MenuId.dashboard_groups},
              {id: MenuId.dashboard_shared}
            ]
          },
          {
            id: MenuId.alarms_center,
            pages: [
              {id: MenuId.alarms}
            ]
          },
          {
            id: MenuId.notifications_center,
            pages: [
              {id: MenuId.notification_inbox}
            ]
          },
          {
            id: MenuId.reporting,
            pages: [
              {id: MenuId.report_templates},
              {id: MenuId.report_scheduling},
              {id: MenuId.reports}
            ]
          }
        ]
      },
      {
        id: MenuId.entities,
        pages: [
          {
            id: MenuId.devices,
            pages: [
              {id: MenuId.device_all},
              {id: MenuId.device_groups},
              {id: MenuId.device_shared}
            ]
          },
          {
            id: MenuId.assets,
            pages: [
              {id: MenuId.asset_all},
              {id: MenuId.asset_groups},
              {id: MenuId.asset_shared}
            ]
          },
          {
            id: MenuId.entity_views,
            pages: [
              {id: MenuId.entity_view_all},
              {id: MenuId.entity_view_groups},
              {id: MenuId.entity_view_shared}
            ]
          }
        ]
      },
      {
        id: MenuId.customers_and_users,
        pages: [
          {
            id: MenuId.customers,
            pages: [
              {id: MenuId.customer_all},
              {id: MenuId.customer_groups},
              {id: MenuId.customer_shared},
              {id: MenuId.customers_hierarchy}
            ]
          },
          {
            id: MenuId.users,
            pages: [
              {id: MenuId.user_all},
              {id: MenuId.user_groups}
            ]
          },
          {id: MenuId.roles}
        ]
      },
      {
        id: MenuId.white_labeling,
        pages: [
          {id: MenuId.white_labeling_general},
          {id: MenuId.login_white_labeling},
          {id: MenuId.custom_translation},
          {id: MenuId.custom_menu}
        ]
      },
      {
        id: MenuId.resources,
        pages: [
          {id: MenuId.images},
          {id: MenuId.scada_symbols}
        ]
      },
      {
        id: MenuId.security_settings,
        pages: [
          {
            id: MenuId.oauth2,
            pages: [
              {id: MenuId.domains},
              {id: MenuId.clients}
            ]
          },
          {id: MenuId.audit_log}
        ]
      },
      {
        id: MenuId.platform_section,
        pages: [
          {id: MenuId.task_manager},
          {id: MenuId.scheduler},
          {
            id: MenuId.settings,
            pages: [
              {id: MenuId.home_settings}
            ]
          }
        ]
      },
      {
        id: MenuId.edge_instances,
        pages: [
          {id: MenuId.edge_all},
          {id: MenuId.edge_groups},
          {id: MenuId.edge_shared},
        ]
      },
      {id: MenuId.trendz_analytics}
    ]
  ]
]);

const referencesToMenuIdList = (references: MenuReference[]): Array<MenuId | string> => {
  const result: Array<MenuId | string> = [];
  for (const ref of references) {
    result.push(ref.id);
    if (ref.pages?.length) {
      result.push(...referencesToMenuIdList(ref.pages));
    }
  }
  return result;
};

export const buildUserMenu = (authState: AuthState, userPermissionsService: UserPermissionsService,
                              customMenuConfig: CustomMenuConfig): Array<MenuSection> => {
  if (customMenuConfig?.items?.length) {
    const customStateIds: {[stateId: string]: boolean} = {};
    const allowedMenuIds = [...menuSectionMap.keys()];//referencesToMenuIdList(defaultUserMenuMap.get(authState.authUser.authority));
    return customMenuConfig.items.map(item =>
      menuItemToMenuSection(authState, userPermissionsService, allowedMenuIds, customStateIds, item)).filter(section => !!section);
  } else {
    const references = defaultUserMenuMap.get(authState.authUser.authority);
    return (references || []).map(ref =>
      referenceToMenuSection(authState, userPermissionsService, ref)).filter(section => !!section);
  }
};

export const buildUserHome = (currentMenuSections: MenuSection[]): Array<HomeSection> => {
  return (currentMenuSections || []).map(section =>
    menuSectionToHomeSection(section)).filter(section => !!section);
};

export const menuItemToMenuSection = (authState: AuthState,
                                      userPermissionsService: UserPermissionsService,
                                      allowedMenuIds: Array<MenuId | string>,
                                      customStateIds: {[stateId: string]: boolean},
                                      item: MenuItem): MenuSection | undefined => {
  if (isDefaultMenuItem(item)) {
    if (!filterMenuReference(authState, userPermissionsService, allowedMenuIds, item)) {
      return undefined;
    }
    if (isDefinedAndNotNull(item.visible) && !item.visible) {
      return undefined;
    }
    const section = menuSectionMap.get(item.id);
    const result = deepClone(section);
    if (isNotEmptyStr(item.icon)) {
      result.icon = item.icon;
      result.outlinedIcon = item.icon;
    }
    if (isNotEmptyStr(item.name)) {
      result.name = item.name;
      result.customTranslate = true;
    }
    if (isHomeMenuItem(item)) {
      const type = item.homeType;
      switch (type) {
        case HomeMenuItemType.DEFAULT:
          break;
        case HomeMenuItemType.DASHBOARD:
          result.homeDashboardId = item.dashboardId;
          result.homeHideDashboardToolbar = item.hideDashboardToolbar;
          break;
      }
    }
    if (item.pages?.length) {
      result.pages = item.pages.map(page =>
        menuItemToMenuSection(authState, userPermissionsService, allowedMenuIds, customStateIds, page)).filter(page => !!page);
    }
    if ((result.type === 'toggle' || result.type === 'link' && Array.isArray(item.pages)) && !result.pages?.length) {
      return undefined;
    }
    return result;
  } else if (isCustomMenuItem(item)) {
    return buildCustomMenuSection(customStateIds, item);
  }
};

const referenceToMenuSection = (authState: AuthState, userPermissionsService: UserPermissionsService,
                                reference: MenuReference): MenuSection | undefined => {
  if (filterMenuReference(authState, userPermissionsService, [], reference)) {
    const section = menuSectionMap.get(MenuId[reference.id]);
    if (section) {
      const result = deepClone(section);
      if (reference.pages?.length) {
        result.pages = reference.pages.map(page =>
          referenceToMenuSection(authState, userPermissionsService, page)).filter(page => !!page);
      }
      return result;
    } else {
      return undefined;
    }
  } else {
    return undefined;
  }
};

const filterMenuReference = (authState: AuthState, userPermissionsService: UserPermissionsService,
                             allowedMenuIds: Array<MenuId | string>,
                             reference: MenuReference): boolean => {
  if (allowedMenuIds?.length && !allowedMenuIds.includes(reference.id)) {
    return false;
  }
  if (authState.authUser.authority === Authority.SYS_ADMIN && reference.id !== MenuId.license_management) {
    return true;
  }
  const filter = menuFilters.get(MenuId[reference.id]);
  if (filter) {
    if (!filter(authState, userPermissionsService)) {
      return false;
    }
  }
  if (reference.pages?.length) {
    if (reference.pages.every(page => !filterMenuReference(authState, userPermissionsService, allowedMenuIds, page))) {
      return false;
    }
  }
  return true;
};

const buildCustomMenuSection = (stateIds: {[stateId: string]: boolean}, customMenuItem: CustomMenuItem): MenuSection | undefined => {
  if (isDefinedAndNotNull(customMenuItem.visible) && !customMenuItem.visible) {
    return undefined;
  }
  if (customMenuItem.menuItemType === CMItemType.SECTION &&
      !customMenuItem.pages?.length) {
    return undefined;
  }
  const stateId = getCustomMenuStateId(customMenuItem.name, stateIds);
  const customMenuSection = {
    id: stateId,
    isCustom: true,
    customTranslate: true,
    stateId,
    name: customMenuItem.name,
    icon: customMenuItem.icon,
    outlinedIcon: customMenuItem.icon
  } as MenuSection;
  if (customMenuItem.menuItemType === CMItemType.SECTION) {
    customMenuSection.type = 'toggle';
    const pages: MenuSection[] = [];
    const childStateIds: {[stateId: string]: boolean} = {};
    for (const customMenuChildItem of customMenuItem.pages) {
      if (isDefinedAndNotNull(customMenuChildItem.visible) && !customMenuChildItem.visible) {
        continue;
      }
      const childStateId = getCustomMenuStateId(customMenuChildItem.name, stateIds);
      const customMenuChildSection: MenuSection = {
        id: childStateId,
        isCustom: true,
        customTranslate: true,
        stateId: childStateId,
        name: customMenuChildItem.name,
        type: 'link',
        icon: customMenuChildItem.icon,
        outlinedIcon: customMenuChildItem.icon,
        path: '/iframeView/child'
      };
      customMenuChildSection.queryParams = {
        ...customMenuItemQueryParams(customMenuItem, stateId),
        ...customMenuItemQueryParams(customMenuChildItem, childStateId, true)
      };
      pages.push(customMenuChildSection);
      childStateIds[childStateId] = true;
    }
    if (!pages.length) {
      return undefined;
    }
    customMenuSection.pages = pages;
    customMenuSection.childStateIds = childStateIds;
  } else {
    customMenuSection.path = '/iframeView';
    customMenuSection.type = 'link';
    customMenuSection.queryParams = customMenuItemQueryParams(customMenuItem, stateId);
  }
  return customMenuSection;
};

const customMenuItemQueryParams = (item: CustomMenuItem, stateId: string, child = false):  {[k: string]: any} => {
  const queryParams: {[k: string]: any} = {};
  if (child) {
    queryParams.childStateId = stateId;
  } else {
    queryParams.stateId = stateId;
  }
  if (item.linkType === CMItemLinkType.URL) {
    if (child) {
      queryParams.childIframeUrl = item.url;
      queryParams.childSetAccessToken = item.setAccessToken;
    } else {
      queryParams.iframeUrl = item.url;
      queryParams.setAccessToken = item.setAccessToken;
    }
  } else if (item.linkType === CMItemLinkType.DASHBOARD) {
    if (child) {
      queryParams.childDashboardId = item.dashboardId;
      queryParams.childHideDashboardToolbar = item.hideDashboardToolbar;
    } else {
      queryParams.dashboardId = item.dashboardId;
      queryParams.hideDashboardToolbar = item.hideDashboardToolbar;
    }
  }
  return queryParams;
};

const getCustomMenuStateId = (name: string, stateIds: {[stateId: string]: boolean}): string => {
  const origName = (' ' + name).slice(1);
  let stateId = origName;
  let inc = 1;
  while (stateIds[stateId]) {
    stateId = origName + inc;
    inc++;
  }
  stateIds[stateId] = true;
  return stateId;
};

const menuSectionToHomeSection = (section: MenuSection): HomeSection => {
  if (section.id !== MenuId.home) {
    if (section.type === 'link') {
      return {
        name: section.name,
        places: [ section ]
      }
    } else if (section.type === 'toggle' && section.pages?.length) {
      return {
        name: section.name,
        places: section.pages
      };
    }
  }
}
