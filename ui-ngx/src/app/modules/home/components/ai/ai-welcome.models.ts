// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import { AiAssistantPanelConfig, AiAssistantPromptExample, AiAssistantViewType, AiChatView } from '@shared/models/ai-chat.models';

export interface AiWelcome {
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
  [AiAssistantViewType.DEVICE_LIST]: {key: 'devices', examples: ['inactive']},
  [AiAssistantViewType.DEVICE]: {key: 'device', entity: true, examples: ['telemetry', 'alarm', 'dashboard']},
  [AiAssistantViewType.DEVICE_GROUP_LIST]: {key: 'device-groups', owned: true},
  [AiAssistantViewType.DEVICE_GROUP]: {key: 'device-group', entity: true, examples: ['status']},
  [AiAssistantViewType.ALARM_RULE_LIST]: {key: 'alarm-rules'},
  [AiAssistantViewType.ALARM_RULE]: {key: 'alarm-rule', entity: true, examples: ['explain']},
  [AiAssistantViewType.ALARM_LIST]: {key: 'alarms'},
  [AiAssistantViewType.CALCULATED_FIELD_LIST]: {key: 'calculated-fields'},
  [AiAssistantViewType.CALCULATED_FIELD]: {key: 'calculated-field', entity: true, examples: ['explain']},
  [AiAssistantViewType.ASSET_LIST]: {key: 'assets', examples: ['find']},
  [AiAssistantViewType.ASSET]: {key: 'asset', entity: true, examples: ['telemetry', 'metric', 'dashboard']},
  [AiAssistantViewType.ASSET_GROUP_LIST]: {key: 'asset-groups', owned: true, examples: ['find']},
  [AiAssistantViewType.ASSET_GROUP]: {key: 'asset-group', entity: true, examples: ['status']},
  [AiAssistantViewType.ENTITY_VIEW_LIST]: {key: 'entity-views', examples: ['find']},
  [AiAssistantViewType.ENTITY_VIEW]: {key: 'entity-view', entity: true, examples: ['telemetry', 'source']},
  [AiAssistantViewType.DEVICE_PROFILE_LIST]: {key: 'device-profiles', examples: ['find']},
  [AiAssistantViewType.DEVICE_PROFILE]: {key: 'device-profile', entity: true, examples: ['devices', 'alarm', 'metric']},
  [AiAssistantViewType.ASSET_PROFILE_LIST]: {key: 'asset-profiles', examples: ['find']},
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
  [AiAssistantViewType.OTA_UPDATES]: {key: 'ota-updates'},
  [AiAssistantViewType.WHITE_LABELING]: {key: 'white-labeling'},
  [AiAssistantViewType.LOGIN_WHITE_LABELING]: {key: 'login-white-labeling'},
  [AiAssistantViewType.MAIL_TEMPLATES]: {key: 'mail-templates'},
  [AiAssistantViewType.CUSTOM_TRANSLATION]: {key: 'custom-translation'},
  [AiAssistantViewType.CUSTOM_MENU]: {key: 'custom-menu'},
  [AiAssistantViewType.IMAGES]: {key: 'images'},
  [AiAssistantViewType.JAVASCRIPT_LIBRARY]: {key: 'javascript-library'},
  [AiAssistantViewType.RESOURCES_LIBRARY]: {key: 'resources-library'},
  [AiAssistantViewType.TWO_FACTOR_AUTH]: {key: 'two-factor-auth'},
  [AiAssistantViewType.OAUTH2_DOMAINS]: {key: 'oauth2-domains'},
  [AiAssistantViewType.OAUTH2_CLIENTS]: {key: 'oauth2-clients'},
  [AiAssistantViewType.SELF_REGISTRATION]: {key: 'self-registration'},
  [AiAssistantViewType.SECRETS]: {key: 'secrets'},
  [AiAssistantViewType.AUDIT_LOGS]: {key: 'audit-logs'},
  [AiAssistantViewType.VERSION_CONTROL]: {key: 'version-control'},
  [AiAssistantViewType.TASK_MANAGER]: {key: 'task-manager'},
  [AiAssistantViewType.SCHEDULER]: {key: 'scheduler'},
  [AiAssistantViewType.HOME_SETTINGS]: {key: 'home-settings'},
  [AiAssistantViewType.MAIL_SERVER_SETTINGS]: {key: 'mail-server-settings', examples: ['setup']},
  [AiAssistantViewType.NOTIFICATION_SETTINGS]: {key: 'notification-settings', examples: ['slack', 'sms']},
  [AiAssistantViewType.REPOSITORY_SETTINGS]: {key: 'repository-settings'},
  [AiAssistantViewType.AUTO_COMMIT_SETTINGS]: {key: 'auto-commit-settings'},
  [AiAssistantViewType.AI_MODELS]: {key: 'ai-models'},
  [AiAssistantViewType.API_USAGE]: {key: 'api-usage'},
  [AiAssistantViewType.MOBILE_BUNDLES]: {key: 'mobile-bundles'},
  [AiAssistantViewType.MOBILE_APPS]: {key: 'mobile-apps'},
  [AiAssistantViewType.MOBILE_QR_CODE]: {key: 'mobile-qr-code'},
  [AiAssistantViewType.TRENDZ]: {key: 'trendz'}
};

export function aiWelcome(view: AiChatView | undefined, name: string | null, config: AiAssistantPanelConfig | null,
                          translate: TranslateService): AiWelcome {
  const configExamples = config?.promptExamples ?? [];
  const kind = view ? WELCOMES[view.type] : undefined;
  if (!kind) {
    return {title: config?.title, line: config?.subTitle, examples: configExamples, suggestion: config?.suggestion ?? shortExample(configExamples)};
  }
  const base = 'ai-assistant.welcome.' + kind.key;
  const own = (kind.examples ?? []).map(example => ({
    label: translate.instant(`${base}.example.${example}.label`),
    message: translate.instant(`${base}.example.${example}.message`)
  }));
  const examples = kind.entity && own.length ? own : [...own, ...configExamples.filter(item => !own.some(mine => mine.message === item.message))];
  return {
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
