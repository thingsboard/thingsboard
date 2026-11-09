// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from '@shared/models/id/entity-id';

export interface ChatConfiguration {
  title: string;
}

export type AnswerRating = 'UP' | 'DOWN' | 'NONE';

export type AnswerProblem = 'WRONG' | 'NOT_WHAT_I_ASKED' | 'WRONG_CHANGE' | 'TOO_SLOW' | 'OTHER';

export const ANSWER_PROBLEMS: AnswerProblem[] = ['WRONG', 'NOT_WHAT_I_ASKED', 'WRONG_CHANGE', 'TOO_SLOW', 'OTHER'];

export interface AnswerFeedback {
  answer: string;
  rating?: AnswerRating;
  problem?: AnswerProblem;
  comment?: string;
}

export interface ChatUpdate {
  title?: string;
  pinned?: boolean;
  feedback?: AnswerFeedback;
}

export interface ChatInfo extends ChatConfiguration {
  id: string;
  createdTime: number;
  pinned?: boolean;
  status?: ChatStatus;
  statusReason?: string;
}

export type ChatStatus = 'RUNNING' | 'NEEDS_YOU' | 'FAILED';

export type ChatLevel = 'needs' | 'running' | 'error';

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
  review?: AiApprovalReview;
  time?: number;
}

export type AiReviewFieldKind = 'FACT' | 'CHANGE' | 'UNCHANGED';

export interface AiReviewField {
  kind: AiReviewFieldKind;
  id?: string;
  name?: string;
  current?: string;
  value?: string;
}

export interface AiApprovalReview {
  entityType: string;
  name: string;
  detail?: string;
  target?: string;
  fields: AiReviewField[];
  change?: 'CREATE' | 'UPDATE' | 'DELETE';
}

export interface AiAssistantPromptExample {
  label: string;
  message: string;
}

export interface AiAssistantPanelConfig {
  view?: AiAssistantViewConfig;
  initialPromptPlaceholder: string;
  followUpPromptPlaceholder?: string;
  title?: string;
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
  reason?: string;
}

export interface ToolExecutionResultEvent extends BaseChatEvent {
  event: 'toolExecutionResult';
  data: ToolExecutionResult;
}

export interface ErrorEvent extends BaseChatEvent {
  event: 'error';
  data: {
    message: string;
    code?: 'CREDITS_USED_UP' | 'RATE_LIMITED';
  };
}

export interface AiNotice {
  kind: 'info' | 'warn' | 'block';
  icon: string;
  text: string;
}

export interface ChatTitleGeneratedEvent extends BaseChatEvent {
  event: 'chatTitleGenerated';
  data: {
    title: string;
  };
}

export interface TurnResumedEvent extends BaseChatEvent {
  event: 'turnResumed';
  data: Record<string, never>;
}

export interface TurnStoppedEvent extends BaseChatEvent {
  event: 'turnStopped';
  data: Record<string, never>;
}

export interface ToolStarted extends BaseChatToolEventData {
  tool: string;
  arguments?: unknown;
}

export interface ToolStartedEvent extends BaseChatEvent {
  event: 'toolStarted';
  data: ToolStarted;
}

export interface ToolFinished extends BaseChatToolEventData {
  status: 'SUCCESS' | 'FAILURE';
  durationMs: number;
  result?: string;
  affectedEntities?: Array<EntityId>;
}

export interface ToolFinishedEvent extends BaseChatEvent {
  event: 'toolFinished';
  data: ToolFinished;
}

export type AiActionStatus = 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'STOPPED';

export interface AiAction {
  executionId: string;
  tool: string;
  arguments?: unknown;
  status: AiActionStatus;
  startedAt: number;
  durationMs?: number;
  result?: string;
  affectedEntities?: Array<EntityId>;
}

export interface ApprovalResult {
  executionId: string;
  approved: boolean;
  autoApprove?: boolean;
  reason?: string;
}

export enum ToolApprovalResult {
  APPROVED = 'APPROVED',
  DENIED = 'DENIED',
  TIMEOUT = 'TIMEOUT'
}

export interface MessageQueuedEvent extends BaseChatEvent {
  event: 'messageQueued';
  data: Record<string, never>;
}

export type ChatEvent = AssistantChatEvent | ToolExecutionRequestedEvent | ToolExecutionResultEvent | ErrorEvent | ChatTitleGeneratedEvent
  | TurnResumedEvent | TurnStoppedEvent | MessageQueuedEvent | ToolStartedEvent | ToolFinishedEvent;

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
  resources?: string[];
}

export interface AiResourceInfo {
  id: string;
  name: string;
  mediaType: string;
  size: number;
}

export interface AiResourceConfig {
  maxSize: number;
  maxPerMessage: number;
  mediaTypes: string[];
  extensions: string[];
}

export interface AiPromptMessage {
  message: string;
  resources: AiResourceInfo[];
}

export const AI_IMAGE_MEDIA_TYPES: readonly string[] = ['image/png', 'image/jpeg', 'image/webp'];

export const isAiImageResource = (resource: Pick<AiResourceInfo, 'mediaType'>): boolean =>
  AI_IMAGE_MEDIA_TYPES.includes(resource.mediaType);

export interface AiAssistantViewConfig {
  listView: AiAssistantViewType;
  listEntityId?: EntityId;
  entityView?: AiAssistantViewType;
}

