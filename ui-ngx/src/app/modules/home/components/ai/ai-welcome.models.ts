// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { AiAssistantPanelConfig, AiAssistantPromptExample, AiAssistantViewType, AiChatView } from '@shared/models/ai-chat.models';

export type AiWelcomePlace = 'dashboard' | 'list' | 'page';

export interface AiWelcome {
  place: AiWelcomePlace;
  title?: string;
  line?: string;
  examples: AiAssistantPromptExample[];
  suggestion: string | null;
}

interface AiWelcomeKind {
  key: string;
  entity?: boolean;
  owned?: boolean;
  examples?: string[];
}

const WELCOMES: Partial<Record<AiAssistantViewType, AiWelcomeKind>> = {
  [AiAssistantViewType.DASHBOARD_LIST]: {key: 'dashboards', examples: ['build', 'find']},
  [AiAssistantViewType.DASHBOARD]: {key: 'dashboard', entity: true},
  [AiAssistantViewType.REPORT_TEMPLATE_LIST]: {key: 'report-templates', examples: ['fleet', 'export']},
  [AiAssistantViewType.REPORT_TEMPLATE]: {key: 'report-template', entity: true},
  [AiAssistantViewType.SCHEDULED_REPORTS]: {key: 'scheduled-reports', examples: ['weekly', 'next']},
  [AiAssistantViewType.REPORTS]: {key: 'reports', examples: ['latest', 'now']},
  [AiAssistantViewType.WIDGET_TYPE_LIST]: {key: 'widget-types', examples: ['tank', 'heatmap']},
  [AiAssistantViewType.WIDGET_TYPE]: {key: 'widget-type', entity: true},
  [AiAssistantViewType.NEW_WIDGET_TYPE]: {key: 'widget-type-new'},
  [AiAssistantViewType.WIDGETS_BUNDLE_LIST]: {key: 'widgets-bundles', examples: ['custom']},
  [AiAssistantViewType.WIDGETS_BUNDLE]: {key: 'widgets-bundle', entity: true, examples: ['add', 'order']},
  [AiAssistantViewType.DEVICE_LIST]: {key: 'devices', examples: ['inactive']},
  [AiAssistantViewType.DEVICE]: {key: 'device', entity: true, examples: ['telemetry', 'alarm', 'dashboard']},
  [AiAssistantViewType.DEVICE_GROUP_LIST]: {key: 'device-groups', owned: true},
  [AiAssistantViewType.DEVICE_GROUP]: {key: 'device-group', entity: true, examples: ['status']},
  [AiAssistantViewType.ALARM_RULE_LIST]: {key: 'alarm-rules'},
  [AiAssistantViewType.ALARM_RULE]: {key: 'alarm-rule', entity: true, examples: ['explain']},
  [AiAssistantViewType.ALARM_LIST]: {key: 'alarms', examples: ['attention', 'triage']},
  [AiAssistantViewType.ALARM]: {key: 'alarm', entity: true, examples: ['explain', 'acknowledge']},
  [AiAssistantViewType.CALCULATED_FIELD_LIST]: {key: 'calculated-fields'},
  [AiAssistantViewType.CALCULATED_FIELD]: {key: 'calculated-field', entity: true, examples: ['explain']},
  [AiAssistantViewType.ASSET_LIST]: {key: 'assets', examples: ['find', 'hierarchy']},
  [AiAssistantViewType.ASSET]: {key: 'asset', entity: true, examples: ['telemetry', 'metric', 'dashboard']},
  [AiAssistantViewType.ASSET_GROUP_LIST]: {key: 'asset-groups', owned: true, examples: ['find']},
  [AiAssistantViewType.ASSET_GROUP]: {key: 'asset-group', entity: true, examples: ['status']},
  [AiAssistantViewType.ENTITY_VIEW_LIST]: {key: 'entity-views', examples: ['find', 'create']},
  [AiAssistantViewType.ENTITY_VIEW]: {key: 'entity-view', entity: true, examples: ['telemetry', 'source']},
  [AiAssistantViewType.DEVICE_PROFILE_LIST]: {key: 'device-profiles', examples: ['find']},
  [AiAssistantViewType.DEVICE_PROFILE]: {key: 'device-profile', entity: true, examples: ['devices', 'alarm', 'metric']},
  [AiAssistantViewType.ASSET_PROFILE_LIST]: {key: 'asset-profiles', examples: ['find', 'create']},
  [AiAssistantViewType.ASSET_PROFILE]: {key: 'asset-profile', entity: true, examples: ['assets', 'alarm']},
  [AiAssistantViewType.CUSTOMER_LIST]: {key: 'customers', examples: ['create', 'find']},
  [AiAssistantViewType.CUSTOMER]: {key: 'customer', entity: true, examples: ['user', 'share', 'devices']},
  [AiAssistantViewType.CUSTOMER_GROUP_LIST]: {key: 'customer-groups', owned: true, examples: ['create']},
  [AiAssistantViewType.CUSTOMER_GROUP]: {key: 'customer-group', entity: true, examples: ['list']},
  [AiAssistantViewType.USER_LIST]: {key: 'users', examples: ['invite', 'find']},
  [AiAssistantViewType.USER]: {key: 'user', entity: true, examples: ['access', 'notifications']},
  [AiAssistantViewType.USER_GROUP_LIST]: {key: 'user-groups', owned: true, examples: ['create']},
  [AiAssistantViewType.USER_GROUP]: {key: 'user-group', entity: true, examples: ['access', 'add']},
  [AiAssistantViewType.ROLE_LIST]: {key: 'roles', examples: ['create', 'explain']},
  [AiAssistantViewType.ROLE]: {key: 'role', entity: true, examples: ['explain']},
  [AiAssistantViewType.RULE_CHAIN_LIST]: {key: 'rule-chains', examples: ['alarm', 'notify']},
  [AiAssistantViewType.RULE_CHAIN]: {key: 'rule-chains', examples: ['alarm', 'notify']},
  [AiAssistantViewType.INTEGRATION_LIST]: {key: 'integrations', examples: ['connect']},
  [AiAssistantViewType.INTEGRATION]: {key: 'integration', entity: true, examples: ['connect']},
  [AiAssistantViewType.CONVERTER_LIST]: {key: 'converters', examples: ['connect']},
  [AiAssistantViewType.CONVERTER]: {key: 'converter', entity: true, examples: ['connect']},
  [AiAssistantViewType.EDGE_LIST]: {key: 'edges', examples: ['offline', 'create']},
  [AiAssistantViewType.EDGE]: {key: 'edge', entity: true, examples: ['status', 'assign', 'install']},
  [AiAssistantViewType.EDGE_GROUP_LIST]: {key: 'edge-groups', owned: true, examples: ['find']},
  [AiAssistantViewType.EDGE_GROUP]: {key: 'edge-group', entity: true, examples: ['status']},
  [AiAssistantViewType.EDGE_SCHEDULER]: {key: 'edge-scheduler', examples: ['assign']},
  [AiAssistantViewType.RULE_CHAIN_TEMPLATE_LIST]: {key: 'rule-chain-templates', examples: ['root', 'hub']},
  [AiAssistantViewType.RULE_CHAIN_TEMPLATE]: {key: 'rule-chain-template', entity: true, examples: ['edges', 'auto']},
  [AiAssistantViewType.INTEGRATION_TEMPLATE_LIST]: {key: 'integration-templates', examples: ['edges']},
  [AiAssistantViewType.INTEGRATION_TEMPLATE]: {key: 'integration-template', entity: true, examples: ['edges']},
  [AiAssistantViewType.CONVERTER_TEMPLATE_LIST]: {key: 'converter-templates', examples: ['explain']},
  [AiAssistantViewType.CONVERTER_TEMPLATE]: {key: 'converter-template', entity: true, examples: ['explain']},
  [AiAssistantViewType.AGENT_LIST]: {key: 'agents', examples: ['status', 'deploy']},
  [AiAssistantViewType.AGENT]: {key: 'agent', entity: true, examples: ['apps', 'edge', 'upgrade']},
  [AiAssistantViewType.AGENT_GROUP_LIST]: {key: 'agent-groups', owned: true, examples: ['find']},
  [AiAssistantViewType.AGENT_GROUP]: {key: 'agent-group', entity: true, examples: ['status']},
  [AiAssistantViewType.AGENT_APPLICATION_LIST]: {key: 'agent-applications', examples: ['status', 'install']},
  [AiAssistantViewType.AGENT_APPLICATION]: {key: 'agent-application', entity: true, examples: ['logs', 'restart']},
  [AiAssistantViewType.AGENT_EVENTS]: {key: 'agent-events', examples: ['failed']},
  [AiAssistantViewType.AGENT_PROFILE_LIST]: {key: 'agent-profiles', examples: ['create', 'default']},
  [AiAssistantViewType.AGENT_PROFILE]: {key: 'agent-profile', entity: true, examples: ['apps', 'bulk']},
  [AiAssistantViewType.AGENT_APP_PROFILE_LIST]: {key: 'agent-app-profiles', examples: ['edge', 'gateway']},
  [AiAssistantViewType.AGENT_APP_PROFILE]: {key: 'agent-app-profile', entity: true, examples: ['explain']},
  [AiAssistantViewType.AGENT_BULK_ACTION]: {key: 'agent-bulk-action', examples: ['progress']},
  [AiAssistantViewType.NOTIFICATION_TEMPLATE_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_RULE_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_RECIPIENT_LIST]: {key: 'notifications'},
  [AiAssistantViewType.SENT_NOTIFICATION_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_INBOX]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_TEMPLATE]: {key: 'notification-template', entity: true, examples: ['explain', 'slack']},
  [AiAssistantViewType.NOTIFICATION_RULE]: {key: 'notification-rule', entity: true, examples: ['explain', 'escalate']},
  [AiAssistantViewType.NOTIFICATION_RECIPIENT]: {key: 'notification-recipient', entity: true, examples: ['rules']},
  [AiAssistantViewType.IOT_HUB]: {key: 'iot-hub', examples: ['popular', 'alarm', 'installed']},
  [AiAssistantViewType.DASHBOARD_GROUP_LIST]: {key: 'dashboard-groups', owned: true, examples: ['share']},
  [AiAssistantViewType.DASHBOARD_GROUP]: {key: 'dashboard-group', entity: true, examples: ['access']},
  [AiAssistantViewType.ENTITY_VIEW_GROUP_LIST]: {key: 'entity-view-groups', owned: true},
  [AiAssistantViewType.ENTITY_VIEW_GROUP]: {key: 'entity-view-group', entity: true, examples: ['list']},
  [AiAssistantViewType.CUSTOMER_HIERARCHY]: {key: 'customer-hierarchy', examples: ['move']},
  [AiAssistantViewType.GATEWAYS]: {key: 'gateways', examples: ['modbus', 'status']},
  [AiAssistantViewType.OTA_UPDATES]: {key: 'ota-updates', examples: ['rollout', 'add']},
  [AiAssistantViewType.WHITE_LABELING]: {key: 'white-labeling', examples: ['brand']},
  [AiAssistantViewType.LOGIN_WHITE_LABELING]: {key: 'login-white-labeling', examples: ['match']},
  [AiAssistantViewType.MAIL_TEMPLATES]: {key: 'mail-templates', examples: ['name', 'activation']},
  [AiAssistantViewType.CUSTOM_TRANSLATION]: {key: 'custom-translation', examples: ['rename', 'find']},
  [AiAssistantViewType.CUSTOM_MENU]: {key: 'custom-menu', examples: ['simple', 'link']},
  [AiAssistantViewType.IMAGES]: {key: 'images', examples: ['icon']},
  [AiAssistantViewType.SCADA_SYMBOL_LIST]: {key: 'scada-symbols', examples: ['find', 'connectors']},
  [AiAssistantViewType.SCADA_SYMBOL]: {key: 'scada-symbol', entity: true, examples: ['explain', 'values']},
  [AiAssistantViewType.JAVASCRIPT_LIBRARY]: {key: 'javascript-library', examples: ['module']},
  [AiAssistantViewType.RESOURCES_LIBRARY]: {key: 'resources-library'},
  [AiAssistantViewType.TWO_FACTOR_AUTH]: {key: 'two-factor-auth', examples: ['review', 'methods', 'enforce']},
  [AiAssistantViewType.OAUTH2_DOMAINS]: {key: 'oauth2-domains', examples: ['missing', 'hide']},
  [AiAssistantViewType.OAUTH2_CLIENTS]: {key: 'oauth2-clients', examples: ['review']},
  [AiAssistantViewType.SELF_REGISTRATION]: {key: 'self-registration', examples: ['review']},
  [AiAssistantViewType.SECRETS]: {key: 'secrets', examples: ['create']},
  [AiAssistantViewType.AUDIT_LOGS]: {key: 'audit-logs', examples: ['changes', 'logins']},
  [AiAssistantViewType.VERSION_CONTROL]: {key: 'version-control', examples: ['save', 'history']},
  [AiAssistantViewType.TASK_MANAGER]: {key: 'task-manager', examples: ['failed']},
  [AiAssistantViewType.SCHEDULER]: {key: 'scheduler', examples: ['night', 'week']},
  [AiAssistantViewType.HOME_SETTINGS]: {key: 'home-settings', examples: ['set']},
  [AiAssistantViewType.MAIL_SERVER_SETTINGS]: {key: 'mail-server-settings', examples: ['setup', 'check']},
  [AiAssistantViewType.NOTIFICATION_SETTINGS]: {key: 'notification-settings', examples: ['slack', 'sms']},
  [AiAssistantViewType.REPOSITORY_SETTINGS]: {key: 'repository-settings', examples: ['connect']},
  [AiAssistantViewType.AUTO_COMMIT_SETTINGS]: {key: 'auto-commit-settings', examples: ['devices']},
  [AiAssistantViewType.AI_MODELS]: {key: 'ai-models', examples: ['add']},
  [AiAssistantViewType.API_USAGE]: {key: 'api-usage', examples: ['limits']},
  [AiAssistantViewType.MOBILE_BUNDLES]: {key: 'mobile-bundles', examples: ['dashboard', 'review']},
  [AiAssistantViewType.MOBILE_APPS]: {key: 'mobile-apps', examples: ['release', 'register']},
  [AiAssistantViewType.MOBILE_QR_CODE]: {key: 'mobile-qr-code', examples: ['home']},
  [AiAssistantViewType.ACCOUNT_PROFILE]: {key: 'account-profile', examples: ['home', 'units']},
  [AiAssistantViewType.ACCOUNT_SECURITY]: {key: 'account-security', examples: ['review', 'keys']},
  [AiAssistantViewType.ACCOUNT_NOTIFICATIONS]: {key: 'account-notifications', examples: ['quiet', 'review']},
  [AiAssistantViewType.TRENDZ]: {key: 'trendz'}
};

export function aiWelcome(view: AiChatView | undefined, name: string | null, config: AiAssistantPanelConfig | null,
                          translate: TranslateService): AiWelcome {
  const configExamples = config?.promptExamples ?? [];
  const kind = view ? WELCOMES[view.type] : undefined;
  if (!kind) {
    return {place: 'page', title: config?.title, line: config?.subTitle, examples: configExamples, suggestion: config?.suggestion ?? shortExample(configExamples)};
  }
  const base = 'ai-assistant.welcome.' + (config?.welcomeKey ?? kind.key);
  const own = (kind.examples ?? []).map(example => ({
    label: translate.instant(`${base}.example.${example}.label`),
    message: translate.instant(`${base}.example.${example}.message`)
  }));
  const examples = kind.entity && own.length ? own : [...own, ...configExamples.filter(item => !own.some(mine => mine.message === item.message))];
  return {
    place: view.type === AiAssistantViewType.DASHBOARD ? 'dashboard' : kind.entity ? 'page' : 'list',
    title: translate.instant(`${base}.${titleKey(kind, view, name)}`, {name}),
    line: translate.instant(`${base}.line`),
    examples,
    suggestion: translate.instant(`${base}.suggestion`)
  };
}

function titleKey(kind: AiWelcomeKind, view: AiChatView, name: string | null): string {
  if (kind.entity) {
    return name ? 'title' : 'title-unnamed';
  }
  return kind.owned && view.entityId?.entityType === EntityType.CUSTOMER ? 'title-customer' : 'title';
}

function shortExample(examples: AiAssistantPromptExample[]): string | null {
  const example = examples[0]?.message?.trim();
  return example && example.length <= 80 && !example.includes('...') ? example : null;
}
