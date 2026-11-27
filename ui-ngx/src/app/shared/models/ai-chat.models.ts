// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export interface ChatConfiguration {
  title: string;
  autoApprove?: boolean;
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
  autoApprove?: boolean;
  fileEdit?: FileEdit;
}

export interface FileEdit {
  executionId: string;
  content: string;
}

export interface ChatInfo extends ChatConfiguration {
  id: string;
  createdTime: number;
  pinned?: boolean;
  pinnedTime?: number;
  prompt?: string;
  status?: ChatStatus;
  statusReason?: string;
  updatedTime?: number;
}

export type ChatStatus = 'RUNNING' | 'NEEDS_YOU' | 'NO_DATA' | 'FAILED';

export type ChatLevel = 'needs' | 'nodata' | 'running' | 'error';

export interface BaseChatEvent {
  event: string;
}

export interface AiReading {
  key: string;
  values: string[];
}

export interface AiEntityReference {
  name: string;
  entityType: string;
  id: string;
}

export interface AssistantChatEvent extends BaseChatEvent {
  event: 'assistantMessage',
  data: {
    message: string;
    references?: AiEntityReference[];
    severities?: string[];
    readings?: AiReading[];
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
  autoApproved?: boolean;
  title?: string;
  approveLabel?: string;
  denyLabel?: string;
  executing?: string;
  icon?: string;
  review?: AiApprovalReview;
  time?: number;
  tool?: string;
  arguments?: unknown;
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
  verb?: string;
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
  suggestion?: string;
  welcomeKey?: string;
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
  result?: string;
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
  startedAt?: number;
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
  edited?: boolean;
  progress?: string;
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

export interface AssistantDeltaChatEvent extends BaseChatEvent {
  event: 'assistantDelta';
  data: {
    text: string;
  };
}

export interface ThinkingChatEvent extends BaseChatEvent {
  event: 'thinking';
  data: {headline?: string; tool?: string; arguments?: unknown};
}

export interface LimitReachedChatEvent extends BaseChatEvent {
  event: 'limitReached';
  data: AiPlatformLimit;
}

export interface AiPlatformLimit {
  errorCode: number;
  message?: string;
  entityType?: EntityType;
  limit?: number;
  subscriptionErrorCode?: 'LIMIT_REACHED' | 'FEATURE_DISABLED';
  subscriptionEntry?: string;
  subscriptionValue?: number;
}

export type ChatEvent = AssistantChatEvent | AssistantDeltaChatEvent | ToolExecutionRequestedEvent | ToolExecutionResultEvent | ErrorEvent | ChatTitleGeneratedEvent
  | TurnResumedEvent | TurnStoppedEvent | MessageQueuedEvent | ToolStartedEvent | ToolFinishedEvent | ThinkingChatEvent | LimitReachedChatEvent;

export enum AiAssistantViewType {
  PAGE = 'PAGE',
  IOT_HUB = 'IOT_HUB',
  DASHBOARD = 'DASHBOARD',
  DASHBOARD_LIST = 'DASHBOARD_LIST',
  DASHBOARD_GROUP_LIST = 'DASHBOARD_GROUP_LIST',
  DASHBOARD_GROUP = 'DASHBOARD_GROUP',
  REPORT_TEMPLATE_LIST = 'REPORT_TEMPLATE_LIST',
  REPORT_TEMPLATE = 'REPORT_TEMPLATE',
  SCHEDULED_REPORTS = 'SCHEDULED_REPORTS',
  REPORTS = 'REPORTS',
  WIDGET_TYPE_LIST = 'WIDGET_TYPE_LIST',
  WIDGET_TYPE = 'WIDGET_TYPE',
  NEW_WIDGET_TYPE = 'NEW_WIDGET_TYPE',
  WIDGETS_BUNDLE_LIST = 'WIDGETS_BUNDLE_LIST',
  WIDGETS_BUNDLE = 'WIDGETS_BUNDLE',
  SCADA_SYMBOL_LIST = 'SCADA_SYMBOL_LIST',
  SCADA_SYMBOL = 'SCADA_SYMBOL',
  ALARM_RULE = 'ALARM_RULE',
  ALARM_RULE_LIST = 'ALARM_RULE_LIST',
  ALARM_LIST = 'ALARM_LIST',
  ALARM = 'ALARM',
  CALCULATED_FIELD = 'CALCULATED_FIELD',
  CALCULATED_FIELD_LIST = 'CALCULATED_FIELD_LIST',
  DEVICE = 'DEVICE',
  DEVICE_LIST = 'DEVICE_LIST',
  DEVICE_GROUP = 'DEVICE_GROUP',
  DEVICE_GROUP_LIST = 'DEVICE_GROUP_LIST',
  ASSET_LIST = 'ASSET_LIST',
  ASSET = 'ASSET',
  ASSET_GROUP_LIST = 'ASSET_GROUP_LIST',
  ASSET_GROUP = 'ASSET_GROUP',
  ENTITY_VIEW_LIST = 'ENTITY_VIEW_LIST',
  ENTITY_VIEW = 'ENTITY_VIEW',
  ENTITY_VIEW_GROUP_LIST = 'ENTITY_VIEW_GROUP_LIST',
  ENTITY_VIEW_GROUP = 'ENTITY_VIEW_GROUP',
  DEVICE_PROFILE_LIST = 'DEVICE_PROFILE_LIST',
  DEVICE_PROFILE = 'DEVICE_PROFILE',
  ASSET_PROFILE_LIST = 'ASSET_PROFILE_LIST',
  ASSET_PROFILE = 'ASSET_PROFILE',
  CUSTOMER_LIST = 'CUSTOMER_LIST',
  CUSTOMER = 'CUSTOMER',
  CUSTOMER_GROUP_LIST = 'CUSTOMER_GROUP_LIST',
  CUSTOMER_GROUP = 'CUSTOMER_GROUP',
  USER_LIST = 'USER_LIST',
  USER = 'USER',
  USER_GROUP_LIST = 'USER_GROUP_LIST',
  USER_GROUP = 'USER_GROUP',
  ROLE_LIST = 'ROLE_LIST',
  ROLE = 'ROLE',
  RULE_CHAIN_LIST = 'RULE_CHAIN_LIST',
  RULE_CHAIN = 'RULE_CHAIN',
  INTEGRATION_LIST = 'INTEGRATION_LIST',
  INTEGRATION = 'INTEGRATION',
  CONVERTER_LIST = 'CONVERTER_LIST',
  CONVERTER = 'CONVERTER',
  EDGE_LIST = 'EDGE_LIST',
  EDGE = 'EDGE',
  EDGE_GROUP_LIST = 'EDGE_GROUP_LIST',
  EDGE_GROUP = 'EDGE_GROUP',
  EDGE_SCHEDULER = 'EDGE_SCHEDULER',
  RULE_CHAIN_TEMPLATE_LIST = 'RULE_CHAIN_TEMPLATE_LIST',
  RULE_CHAIN_TEMPLATE = 'RULE_CHAIN_TEMPLATE',
  INTEGRATION_TEMPLATE_LIST = 'INTEGRATION_TEMPLATE_LIST',
  INTEGRATION_TEMPLATE = 'INTEGRATION_TEMPLATE',
  CONVERTER_TEMPLATE_LIST = 'CONVERTER_TEMPLATE_LIST',
  CONVERTER_TEMPLATE = 'CONVERTER_TEMPLATE',
  AGENT_LIST = 'AGENT_LIST',
  AGENT = 'AGENT',
  AGENT_GROUP_LIST = 'AGENT_GROUP_LIST',
  AGENT_GROUP = 'AGENT_GROUP',
  AGENT_APPLICATION_LIST = 'AGENT_APPLICATION_LIST',
  AGENT_APPLICATION = 'AGENT_APPLICATION',
  AGENT_EVENTS = 'AGENT_EVENTS',
  AGENT_PROFILE_LIST = 'AGENT_PROFILE_LIST',
  AGENT_PROFILE = 'AGENT_PROFILE',
  AGENT_APP_PROFILE_LIST = 'AGENT_APP_PROFILE_LIST',
  AGENT_APP_PROFILE = 'AGENT_APP_PROFILE',
  AGENT_BULK_ACTION = 'AGENT_BULK_ACTION',
  NOTIFICATION_TEMPLATE_LIST = 'NOTIFICATION_TEMPLATE_LIST',
  NOTIFICATION_RULE_LIST = 'NOTIFICATION_RULE_LIST',
  NOTIFICATION_RECIPIENT_LIST = 'NOTIFICATION_RECIPIENT_LIST',
  SENT_NOTIFICATION_LIST = 'SENT_NOTIFICATION_LIST',
  NOTIFICATION_INBOX = 'NOTIFICATION_INBOX',
  NOTIFICATION_TEMPLATE = 'NOTIFICATION_TEMPLATE',
  NOTIFICATION_RULE = 'NOTIFICATION_RULE',
  NOTIFICATION_RECIPIENT = 'NOTIFICATION_RECIPIENT',
  CUSTOMER_HIERARCHY = 'CUSTOMER_HIERARCHY',
  GATEWAYS = 'GATEWAYS',
  OTA_UPDATES = 'OTA_UPDATES',
  WHITE_LABELING = 'WHITE_LABELING',
  LOGIN_WHITE_LABELING = 'LOGIN_WHITE_LABELING',
  MAIL_TEMPLATES = 'MAIL_TEMPLATES',
  CUSTOM_TRANSLATION = 'CUSTOM_TRANSLATION',
  CUSTOM_MENU = 'CUSTOM_MENU',
  IMAGES = 'IMAGES',
  JAVASCRIPT_LIBRARY = 'JAVASCRIPT_LIBRARY',
  RESOURCES_LIBRARY = 'RESOURCES_LIBRARY',
  TWO_FACTOR_AUTH = 'TWO_FACTOR_AUTH',
  OAUTH2_DOMAINS = 'OAUTH2_DOMAINS',
  OAUTH2_CLIENTS = 'OAUTH2_CLIENTS',
  SELF_REGISTRATION = 'SELF_REGISTRATION',
  SECRETS = 'SECRETS',
  AUDIT_LOGS = 'AUDIT_LOGS',
  VERSION_CONTROL = 'VERSION_CONTROL',
  TASK_MANAGER = 'TASK_MANAGER',
  SCHEDULER = 'SCHEDULER',
  HOME_SETTINGS = 'HOME_SETTINGS',
  MAIL_SERVER_SETTINGS = 'MAIL_SERVER_SETTINGS',
  NOTIFICATION_SETTINGS = 'NOTIFICATION_SETTINGS',
  REPOSITORY_SETTINGS = 'REPOSITORY_SETTINGS',
  AUTO_COMMIT_SETTINGS = 'AUTO_COMMIT_SETTINGS',
  AI_MODELS = 'AI_MODELS',
  API_USAGE = 'API_USAGE',
  MOBILE_BUNDLES = 'MOBILE_BUNDLES',
  MOBILE_APPS = 'MOBILE_APPS',
  MOBILE_QR_CODE = 'MOBILE_QR_CODE',
  ACCOUNT_PROFILE = 'ACCOUNT_PROFILE',
  ACCOUNT_SECURITY = 'ACCOUNT_SECURITY',
  ACCOUNT_NOTIFICATIONS = 'ACCOUNT_NOTIFICATIONS',
  TRENDZ = 'TRENDZ'
}

const AI_PAGE_VIEWS: Array<[string, AiAssistantViewType]> = [
  ['/iot-hub', AiAssistantViewType.IOT_HUB],
  ['/customers/hierarchy', AiAssistantViewType.CUSTOMER_HIERARCHY],
  ['/entities/gateways', AiAssistantViewType.GATEWAYS],
  ['/features/otaUpdates', AiAssistantViewType.OTA_UPDATES],
  ['/white-labeling/whiteLabel', AiAssistantViewType.WHITE_LABELING],
  ['/white-labeling/loginWhiteLabel', AiAssistantViewType.LOGIN_WHITE_LABELING],
  ['/white-labeling/mail-template', AiAssistantViewType.MAIL_TEMPLATES],
  ['/white-labeling/customTranslation', AiAssistantViewType.CUSTOM_TRANSLATION],
  ['/white-labeling/customMenu', AiAssistantViewType.CUSTOM_MENU],
  ['/resources/images', AiAssistantViewType.IMAGES],
  ['/resources/scada-symbols', AiAssistantViewType.SCADA_SYMBOL_LIST],
  ['/resources/javascript-library', AiAssistantViewType.JAVASCRIPT_LIBRARY],
  ['/resources/resources-library', AiAssistantViewType.RESOURCES_LIBRARY],
  ['/security-settings/2fa', AiAssistantViewType.TWO_FACTOR_AUTH],
  ['/security-settings/oauth2/domains', AiAssistantViewType.OAUTH2_DOMAINS],
  ['/security-settings/oauth2/clients', AiAssistantViewType.OAUTH2_CLIENTS],
  ['/security-settings/selfRegistration', AiAssistantViewType.SELF_REGISTRATION],
  ['/security-settings/secrets', AiAssistantViewType.SECRETS],
  ['/security-settings/auditLogs', AiAssistantViewType.AUDIT_LOGS],
  ['/features/vc', AiAssistantViewType.VERSION_CONTROL],
  ['/features/taskManager', AiAssistantViewType.TASK_MANAGER],
  ['/features/scheduler', AiAssistantViewType.SCHEDULER],
  ['/reporting/scheduling', AiAssistantViewType.SCHEDULED_REPORTS],
  ['/reporting/reports', AiAssistantViewType.REPORTS],
  ['/settings/home', AiAssistantViewType.HOME_SETTINGS],
  ['/settings/outgoing-mail', AiAssistantViewType.MAIL_SERVER_SETTINGS],
  ['/settings/notifications', AiAssistantViewType.NOTIFICATION_SETTINGS],
  ['/settings/repository', AiAssistantViewType.REPOSITORY_SETTINGS],
  ['/settings/auto-commit', AiAssistantViewType.AUTO_COMMIT_SETTINGS],
  ['/settings/ai-models', AiAssistantViewType.AI_MODELS],
  ['/usage', AiAssistantViewType.API_USAGE],
  ['/mobile-center/bundles', AiAssistantViewType.MOBILE_BUNDLES],
  ['/mobile-center/applications', AiAssistantViewType.MOBILE_APPS],
  ['/mobile-center/qr-code-widget', AiAssistantViewType.MOBILE_QR_CODE],
  ['/account/profile', AiAssistantViewType.ACCOUNT_PROFILE],
  ['/account/security', AiAssistantViewType.ACCOUNT_SECURITY],
  ['/account/notificationSettings', AiAssistantViewType.ACCOUNT_NOTIFICATIONS],
  ['/analytics', AiAssistantViewType.TRENDZ]
];

const AI_PAGE_VIEW_PATTERNS: Array<[RegExp, AiAssistantViewType]> = [
  [/^\/resources\/scada-symbols\/(system|tenant)\/[^/]+$/, AiAssistantViewType.SCADA_SYMBOL],
  [/^\/edgeManagement\/.*\/[0-9a-f-]{36}\/scheduler$/, AiAssistantViewType.EDGE_SCHEDULER],
  [/^\/edgeManagement\/agents\/.*\/[0-9a-f-]{36}\/events$/, AiAssistantViewType.AGENT_EVENTS],
  [/^\/edgeManagement\/profiles\/.*\/bulk\/[0-9a-f-]{36}$/, AiAssistantViewType.AGENT_BULK_ACTION]
];

const AI_TITLED_VIEWS = new Set<string>([AiAssistantViewType.PAGE, ...AI_PAGE_VIEWS.map(([, type]) => type),
  ...AI_PAGE_VIEW_PATTERNS.map(([, type]) => type)]);

export const aiPageView = (path: string): AiAssistantViewType =>
  AI_PAGE_VIEW_PATTERNS.find(([pattern]) => pattern.test(path))?.[1]
  ?? AI_PAGE_VIEWS.find(([prefix]) => path === prefix || path.startsWith(prefix + '/'))?.[1]
  ?? AiAssistantViewType.PAGE;

export const isAiTitledView = (type: string): boolean => AI_TITLED_VIEWS.has(type);

export interface AiChatView {
  type: AiAssistantViewType;
  entityId?: EntityId;
  dashboardState?: string;
  title?: string;
  path?: string;
}

export interface AiChatClientContext {
  view: AiChatView;
  timeZone?: string;
  os?: string;
}

export function aiClientOs(): string | undefined {
  const nav = navigator as Navigator & {userAgentData?: {platform?: string}};
  const agent = `${nav.userAgentData?.platform ?? ''} ${navigator.userAgent}`;
  return /Android/i.test(agent) ? 'android' : /iPhone|iPad|iPod/i.test(agent) ? 'ios' : /CrOS|Chrome OS/i.test(agent) ? 'chromeos'
    : /Win/i.test(agent) ? 'windows' : /Mac/i.test(agent) ? 'macos' : /Linux|X11/i.test(agent) ? 'linux' : undefined;
}

export interface SendChatMessageRequest {
  message: string;
  clientContext?: AiChatClientContext;
  resources?: string[];
  secrets?: Record<string, string>;
  editorDraft?: unknown;
}

export interface AiResourceFacts {
  pages?: number;
  rows?: number;
  columns?: number;
  lines?: number;
}

export interface AiResourceInfo {
  id: string;
  name: string;
  mediaType: string;
  size: number;
  facts?: AiResourceFacts;
}

export interface AiResourceConfig {
  maxSize: number;
  maxPerMessage: number;
  mediaTypes: string[];
  extensions: string[];
}

export interface AiLinesQuote {
  name: string;
  icon: string;
  lang: string;
  from: number;
  to: number;
  text: string;
}

export interface AiPromptMessage {
  message: string;
  resources: AiResourceInfo[];
  quote?: AiLinesQuote;
  secrets?: Record<string, string>;
}

const QUOTE_PATTERN = /^About lines? (\d+)(?:–(\d+))? of (.+?):\n```([\w+-]*)\n([\s\S]*?)\n```\n*/;

export function aiQuoteText(quote: AiLinesQuote, message: string): string {
  const lines = quote.from === quote.to ? `line ${quote.from}` : `lines ${quote.from}–${quote.to}`;
  return `About ${lines} of ${quote.name}:\n\`\`\`${quote.lang}\n${quote.text}\n\`\`\`\n\n${message}`;
}

export function aiQuoteOf(content: string | undefined): {quote: Omit<AiLinesQuote, 'icon'>; message: string} | null {
  const match = content ? QUOTE_PATTERN.exec(content) : null;
  if (!match) {
    return null;
  }
  const from = Number(match[1]);
  return {
    quote: {from, to: match[2] ? Number(match[2]) : from, name: match[3], lang: match[4], text: match[5]},
    message: content.slice(match[0].length)
  };
}

export const AI_IMAGE_MEDIA_TYPES: readonly string[] = ['image/png', 'image/jpeg', 'image/webp'];

export const isAiImageResource = (resource: Pick<AiResourceInfo, 'mediaType'>): boolean =>
  AI_IMAGE_MEDIA_TYPES.includes(resource.mediaType);

export interface AiAssistantViewConfig {
  listView: AiAssistantViewType;
  listEntityId?: EntityId;
  entityView?: AiAssistantViewType;
}

