// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TranslateService } from '@ngx-translate/core';
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
  examples?: string[];
}

const WELCOMES: Partial<Record<AiAssistantViewType, AiWelcomeKind>> = {
  [AiAssistantViewType.DASHBOARD_LIST]: {key: 'dashboards', examples: ['build', 'find']},
  [AiAssistantViewType.DASHBOARD]: {key: 'dashboard', entity: true},
  [AiAssistantViewType.DEVICE_LIST]: {key: 'devices', examples: ['inactive']},
  [AiAssistantViewType.DEVICE]: {key: 'device', entity: true, examples: ['telemetry', 'alarm', 'dashboard']},
  [AiAssistantViewType.DEVICE_GROUP_LIST]: {key: 'device-groups'},
  [AiAssistantViewType.DEVICE_GROUP]: {key: 'device-group', entity: true, examples: ['status']},
  [AiAssistantViewType.ALARM_RULE_LIST]: {key: 'alarm-rules'},
  [AiAssistantViewType.ALARM_RULE]: {key: 'alarm-rule', entity: true, examples: ['explain']},
  [AiAssistantViewType.ALARM_LIST]: {key: 'alarms'},
  [AiAssistantViewType.CALCULATED_FIELD_LIST]: {key: 'calculated-fields'},
  [AiAssistantViewType.CALCULATED_FIELD]: {key: 'calculated-field', entity: true, examples: ['explain']},
  [AiAssistantViewType.NOTIFICATION_TEMPLATE_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_RULE_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_RECIPIENT_LIST]: {key: 'notifications'},
  [AiAssistantViewType.SENT_NOTIFICATION_LIST]: {key: 'notifications'},
  [AiAssistantViewType.NOTIFICATION_INBOX]: {key: 'notifications'}
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
    title: kind.entity && name ? translate.instant(`${base}.title`, {name}) : translate.instant(`${base}.${kind.entity ? 'title-unnamed' : 'title'}`),
    line: translate.instant(`${base}.line`),
    examples,
    suggestion: translate.instant(`${base}.suggestion`)
  };
}

function shortExample(examples: AiAssistantPromptExample[]): string | null {
  const example = examples[0]?.message?.trim();
  return example && example.length <= 80 && !example.includes('...') ? example : null;
}
