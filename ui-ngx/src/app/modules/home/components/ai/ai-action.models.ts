// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AiQuestion } from '@shared/models/solution-creator.models';

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
  create_solution: 'create-solution',
  install_solution: 'install-solution',
  read_entity: 'read-entity',
  read_timeseries_keys: 'read-telemetry-keys',
  read_attributes: 'read-attributes',
  discover_attribute_keys: 'read-attributes',
  save_attributes: 'save-attributes',
  delete_attributes: 'delete-attributes',
  read_entity_relations: 'read-relations',
  read_device_credentials: 'read-credentials',
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
  update_dashboard_widget: 'update-widget',
  place_dashboard_widgets: 'place-widgets',
  read_dashboard_widget: 'read-widget',
  read_dashboard_html_page: 'read-dashboard-page',
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

const SAVED_KINDS: Record<string, [string, string]> = {
  save_device: ['device', ''],
  save_calculated_field: ['calculated-field', ''],
  save_alarm_rule: ['alarm-rule', 'alarmRule'],
  save_notification_target: ['notification-target', 'target'],
  save_notification_template: ['notification-template', 'template'],
  save_notification_rule: ['notification-rule', 'rule']
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

export const SUGGEST_PROMPTS_TOOL = 'suggest_next_prompts';

export const ASK_USER_TOOL = 'ask_user';

export function aiQuestionsOf(args: unknown): AiQuestion[] {
  const questions = (args as {questions?: unknown})?.questions;
  if (!Array.isArray(questions)) {
    return [];
  }
  return questions
    .filter(question => typeof question?.question === 'string' && question.question.trim())
    .slice(0, 3)
    .map((question, index) => ({
      id: typeof question.id === 'string' && question.id ? question.id : 'q' + index,
      question: question.question.trim(),
      why: typeof question.why === 'string' ? question.why : undefined,
      options: (Array.isArray(question.options) ? question.options : [])
        .filter(option => typeof option?.label === 'string' && option.label.trim())
        .slice(0, 6)
        .map(option => ({label: option.label.trim(), hint: typeof option.hint === 'string' ? option.hint : undefined})),
      recommended: Array.isArray(question.recommended) ? question.recommended.filter(label => typeof label === 'string') : undefined,
      multiple: question.multiple === true
    }));
}

const MAX_PROMPTS = 4;

export interface AiPrompt {
  text: string;
  icon?: string;
}

export function aiPromptsOf(args: unknown): AiPrompt[] {
  const prompts = (args as {prompts?: unknown})?.prompts;
  if (!Array.isArray(prompts)) {
    return [];
  }
  return prompts
    .filter(prompt => typeof prompt?.text === 'string' && prompt.text.trim())
    .map(prompt => ({text: prompt.text.trim(), icon: typeof prompt.icon === 'string' && /^[a-z0-9_]+$/.test(prompt.icon) ? prompt.icon : undefined}))
    .slice(0, MAX_PROMPTS);
}

export function aiActionWords(tool: string, args: unknown): AiActionWords {
  const values = (args && typeof args === 'object' ? args : {}) as Record<string, any>;
  if (GENERATING_TOOLS[tool]) {
    return {key: ACTION_PREFIX + GENERATING_TOOLS[tool], kind: 'gen'};
  }
  if (SAVED_KINDS[tool]) {
    const [kind, nested] = SAVED_KINDS[tool];
    return {key: ACTION_PREFIX + (hasId(nested ? values[nested] : values) ? 'update-' : 'create-') + kind, kind: 'think'};
  }
  switch (tool) {
    case 'add_dashboard_widgets':
      return {key: ACTION_PREFIX + (Array.isArray(values.widgets) && values.widgets.length === 1 ? 'add-widget' : 'add-widgets'), kind: 'think'};
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
  const seconds = Math.round(ms / 1000);
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

function hasId(value: unknown): boolean {
  let object = value;
  if (typeof value === 'string') {
    try {
      object = JSON.parse(value);
    } catch {
      return false;
    }
  }
  const id = (object as {id?: unknown})?.id;
  return !!id && (typeof id === 'string' || typeof (id as {id?: unknown}).id === 'string');
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

export type AiEntityPalette = 'device' | 'path' | 'chain' | 'rule' | 'view' | 'people' | 'solution';

export interface AiEntityKind {
  icon: string;
  palette: AiEntityPalette;
}

const ENTITY_KINDS: Record<string, AiEntityKind> = {
  DEVICE: {icon: 'memory', palette: 'device'},
  DEVICE_PROFILE: {icon: 'category', palette: 'device'},
  ASSET: {icon: 'domain', palette: 'device'},
  ASSET_PROFILE: {icon: 'category', palette: 'device'},
  INTEGRATION: {icon: 'input', palette: 'path'},
  CONVERTER: {icon: 'transform', palette: 'path'},
  CALCULATED_FIELD: {icon: 'functions', palette: 'path'},
  RULE_CHAIN: {icon: 'account_tree', palette: 'chain'},
  ALARM_RULE: {icon: 'rule', palette: 'rule'},
  ALARM: {icon: 'notification_important', palette: 'rule'},
  NOTIFICATION_RULE: {icon: 'notifications_active', palette: 'rule'},
  NOTIFICATION_TEMPLATE: {icon: 'description', palette: 'path'},
  NOTIFICATION_TARGET: {icon: 'group', palette: 'people'},
  DASHBOARD: {icon: 'dashboard', palette: 'view'},
  DASHBOARD_WIDGET: {icon: 'widgets', palette: 'view'},
  DASHBOARD_STATE: {icon: 'tab', palette: 'view'},
  DASHBOARD_ENTITY_ALIAS: {icon: 'label', palette: 'view'},
  DASHBOARD_FILTER: {icon: 'filter_list', palette: 'view'},
  WIDGET_TYPE: {icon: 'widgets', palette: 'view'},
  CUSTOMER: {icon: 'supervisor_account', palette: 'people'},
  USER: {icon: 'account_circle', palette: 'people'},
  TENANT: {icon: 'business', palette: 'people'}
};

export function aiEntityKind(entityType: string, alarmRule = false): AiEntityKind {
  return ENTITY_KINDS[alarmRule ? 'ALARM_RULE' : entityType] ?? {icon: 'category', palette: 'solution'};
}

export function aiEntityIcon(entityType: string, alarmRule = false): string {
  return aiEntityKind(entityType, alarmRule).icon;
}
