// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from '@shared/models/id/entity-id';

export interface ChatConfiguration {
  title: string;
}

export interface ChatInfo extends ChatConfiguration {
  id: string;
  createdTime: number;
}

export interface BaseChatEvent {
  event: string;
}

export interface AssistantChatEvent extends BaseChatEvent {
  event: 'assistantMessage',
  data: {
    message: string;
  }
}

export interface ToolExecutionRequestedEvent extends BaseChatEvent {
  event: 'toolExecutionRequested';
  data: ToolExecutionRequested;
}

export interface ToolExecutionRequested extends BaseChatToolEventData {
  needsApproval: boolean;
  destructive: boolean;
  message: string;
  title?: string;
  approveLabel?: string;
  denyLabel?: string;
  executing?: string;
  icon?: string;
}

export interface AiAssistantPromptExample {
  label: string;
  message: string;
}

export interface AiAssistantPanelConfig {
  view?: AiAssistantViewConfig;
  initialPromptPlaceholder: string;
  followUpPromptPlaceholder?: string;
  subTitle?: string;
  pageUrl?: string;
  showButton?: boolean;
  fill?: boolean;
  promptExamples?: AiAssistantPromptExample[];
}

export interface BaseChatToolEventData {
  executionId: string;
}

export interface ToolExecutionResult extends BaseChatToolEventData {
  approvalStatus: ToolApprovalResult;
  executionStatus: 'SUCCESS' | 'FAILURE';
  message: string;
  icon?: string;
  affectedEntities?: Array<EntityId>;
}

export interface ToolExecutionResultEvent extends BaseChatEvent {
  event: 'toolExecutionResult';
  data: ToolExecutionResult;
}

export interface ErrorEvent extends BaseChatEvent {
  event: 'error';
  data: {
    message: string;
  };
}

export interface ChatTitleGeneratedEvent extends BaseChatEvent {
  event: 'chatTitleGenerated';
  data: {
    title: string;
  };
}

export interface ApprovalResult {
  executionId: string;
  approved: boolean;
  autoApprove?: boolean;
}

export enum ToolApprovalResult {
  APPROVED = 'APPROVED',
  DENIED = 'DENIED',
  TIMEOUT = 'TIMEOUT'
}

export type ChatEvent = AssistantChatEvent | ToolExecutionRequestedEvent | ToolExecutionResultEvent | ErrorEvent | ChatTitleGeneratedEvent;

export enum AiAssistantViewType {
  DASHBOARD = 'DASHBOARD',
  DASHBOARD_LIST = 'DASHBOARD_LIST',
  ALARM_RULE = 'ALARM_RULE',
  ALARM_RULE_LIST = 'ALARM_RULE_LIST',
  ALARM_LIST = 'ALARM_LIST',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  CALCULATED_FIELD_LIST = 'CALCULATED_FIELD_LIST',
  DEVICE = 'DEVICE',
  DEVICE_LIST = 'DEVICE_LIST',
  DEVICE_GROUP = 'DEVICE_GROUP',
  DEVICE_GROUP_LIST = 'DEVICE_GROUP_LIST',
  NOTIFICATION_TEMPLATE_LIST = 'NOTIFICATION_TEMPLATE_LIST',
  NOTIFICATION_RULE_LIST = 'NOTIFICATION_RULE_LIST',
  NOTIFICATION_RECIPIENT_LIST = 'NOTIFICATION_RECIPIENT_LIST',
  SENT_NOTIFICATION_LIST = 'SENT_NOTIFICATION_LIST',
  NOTIFICATION_INBOX = 'NOTIFICATION_INBOX'
}

export interface AiChatView {
  type: AiAssistantViewType;
  entityId?: EntityId;
  dashboardState?: string;
}

export interface AiChatClientContext {
  view: AiChatView;
  timeZone?: string;
}

export interface SendChatMessageRequest {
  message: string;
  clientContext?: AiChatClientContext;
}

export interface AiAssistantViewConfig {
  listView: AiAssistantViewType;
  listEntityId?: EntityId;
  entityView?: AiAssistantViewType;
}

