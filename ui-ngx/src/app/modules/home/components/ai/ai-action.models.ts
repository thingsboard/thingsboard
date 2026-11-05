// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
export type { AiAction, AiActionStatus } from '@shared/models/ai-chat.models';

export type AiSpinKind = 'think' | 'gen' | 'install' | 'listen' | 'stop';

export interface AiActionWords {
  key: string;
  params?: Record<string, string>;
  kind: AiSpinKind;
}

export type AiWorkMoment = 'reading-request' | 'going-through-results' | 'applying-change' | 'reading-what-to-change';

export interface AiWorkState {
  moment: AiWorkMoment;
  since: number;
}

const HIDDEN_TOOLS = new Set(['read_current_time']);

const TOOL_WORDS: Record<string, string> = {
  read_entity: 'read-entity',
  read_timeseries_keys: 'read-telemetry-keys',
  read_attributes: 'read-attributes',
  discover_attribute_keys: 'read-attributes',
  save_attributes: 'save-attributes',
  delete_attributes: 'delete-attributes',
  read_entity_relations: 'read-relations',
  read_device_credentials: 'read-credentials',
  save_device: 'save-device',
  save_calculated_field: 'save-calculated-field',
  save_alarm_rule: 'save-alarm-rule',
  create_dashboard: 'create-dashboard',
  update_dashboard: 'update-dashboard',
  add_dashboard_state: 'add-dashboard-state',
  update_dashboard_state: 'update-dashboard-state',
  add_dashboard_entity_alias: 'add-entity-alias',
  update_dashboard_entity_alias: 'update-entity-alias',
  read_dashboard_entity_alias: 'read-entity-alias',
  add_dashboard_filter: 'add-dashboard-filter',
  update_dashboard_filter: 'update-dashboard-filter',
  read_dashboard_filter: 'read-dashboard-filter',
  read_dashboard_overview: 'read-dashboard',
  add_dashboard_widgets: 'add-widgets',
  update_dashboard_widget: 'update-widget',
  place_dashboard_widgets: 'place-widgets',
  read_dashboard_widget: 'read-widget',
  read_dashboard_html_page: 'read-dashboard-page',
  save_notification_target: 'save-notification-target',
  save_notification_template: 'save-notification-template',
  save_notification_rule: 'save-notification-rule',
  search_iot_hub_devices: 'search-iot-hub',
  read_transport_endpoints: 'read-connection-settings',
  read_device_connect_commands: 'read-connection-settings',
  search_web: 'search-web'
};

const GENERATING_TOOLS: Record<string, string> = {
  set_dashboard_html_page: 'lay-out-dashboard',
  reprocess_calculated_field: 'recalculate-history',
  research_device_connectivity: 'research-device'
};

const SEARCHED_TYPES: Record<string, string> = {
  DEVICE: 'search-devices',
  ASSET: 'search-assets',
  DASHBOARD: 'search-dashboards',
  CUSTOMER: 'search-customers',
  USER: 'search-users'
};

const DELETED_TARGETS: Record<string, string> = {
  CALCULATED_FIELD: 'delete-calculated-field',
  ALARM_RULE: 'delete-alarm-rule',
  NOTIFICATION_RULE: 'delete-notification-rule',
  NOTIFICATION_TARGET: 'delete-notification-target',
  NOTIFICATION_TEMPLATE: 'delete-notification-template',
  DASHBOARD: 'delete-dashboard',
  DASHBOARD_WIDGET: 'delete-widget',
  DASHBOARD_STATE: 'delete-dashboard-state',
  DASHBOARD_ENTITY_ALIAS: 'delete-entity-alias',
  DASHBOARD_FILTER: 'delete-dashboard-filter',
  DEVICE: 'delete-device'
};

const ACTION_PREFIX = 'ai-assistant.action.';

export const isHiddenAiAction = (tool: string): boolean => HIDDEN_TOOLS.has(tool);

export function aiActionWords(tool: string, args: unknown): AiActionWords {
  const values = (args && typeof args === 'object' ? args : {}) as Record<string, any>;
  if (GENERATING_TOOLS[tool]) {
    return {key: ACTION_PREFIX + GENERATING_TOOLS[tool], kind: 'gen'};
  }
  switch (tool) {
    case 'search_entities':
      return {key: ACTION_PREFIX + searchedWords(values), kind: 'think'};
    case 'read_timeseries':
      return {key: ACTION_PREFIX + telemetryWords(values), kind: 'think'};
    case 'delete':
      return {key: ACTION_PREFIX + (DELETED_TARGETS[values.targetType] ?? 'delete-other'), kind: 'think'};
    case 'fetch_web_page': {
      const site = siteOf(values.url);
      return site ? {key: ACTION_PREFIX + 'read-site', params: {site}, kind: 'think'} : {key: ACTION_PREFIX + 'read-page', kind: 'think'};
    }
  }
  return {key: ACTION_PREFIX + (TOOL_WORDS[tool] ?? 'other'), kind: 'think'};
}

export function aiActionDuration(ms: number): string {
  if (ms < 1000) {
    return `${Math.max(Math.round(ms / 100), 1) / 10} s`;
  }
  const seconds = Math.round(ms / 1000);
  if (seconds < 60) {
    return `${seconds} s`;
  }
  const rest = seconds % 60;
  return rest ? `${Math.floor(seconds / 60)} min ${rest} s` : `${seconds / 60} min`;
}

export function aiWorkClock(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}

export function aiActionDetail(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '';
  }
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return typeof parsed === 'object' && parsed !== null ? JSON.stringify(parsed, null, 2) : value;
    } catch {
      return value;
    }
  }
  return JSON.stringify(value, null, 2);
}

function searchedWords(values: Record<string, any>): string {
  const filter = values.entityFilter ?? values;
  const types = new Set<string>();
  const add = (type: unknown) => typeof type === 'string' && types.add(type);
  add(filter?.entityType);
  add(filter?.singleEntity?.entityType);
  if (filter?.deviceType || filter?.deviceTypes || filter?.filterType === 'deviceType') {
    types.add('DEVICE');
  }
  if (filter?.assetType || filter?.assetTypes || filter?.filterType === 'assetType') {
    types.add('ASSET');
  }
  (filter?.filters ?? []).forEach((relationFilter: any) => (relationFilter?.entityTypes ?? []).forEach(add));
  if (types.size === 1) {
    return SEARCHED_TYPES[[...types][0]] ?? 'search-tenant';
  }
  return 'search-tenant';
}

function telemetryWords(values: Record<string, any>): string {
  switch (values.mode) {
    case 'LATEST':
      return 'read-latest-values';
    case 'KEYS':
      return 'read-telemetry-keys';
    default:
      return values.mode ? 'read-history' : 'read-latest-values';
  }
}

function siteOf(url: unknown): string | undefined {
  if (typeof url !== 'string') {
    return undefined;
  }
  try {
    return new URL(url).hostname.replace(/^www\./, '') || undefined;
  } catch {
    return undefined;
  }
}
