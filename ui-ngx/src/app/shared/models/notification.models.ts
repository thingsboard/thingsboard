///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { NotificationId } from '@shared/models/id/notification-id';
import { NotificationRequestId } from '@shared/models/id/notification-request-id';
import { UserId } from '@shared/models/id/user-id';
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import { NotificationTemplateId } from '@shared/models/id/notification-template-id';
import { EntityId } from '@shared/models/id/entity-id';
import { NotificationRuleId } from '@shared/models/id/notification-rule-id';
import { AlarmSearchStatus, AlarmSeverity, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';
import { ApiFeature, ApiUsageStateValue } from '@shared/models/api-usage.models';
import { LimitedApi } from '@shared/models/limited-api.models';
import { HasTenantId } from '@shared/models/entity.models';

export interface Notification {
  readonly id: NotificationId;
  readonly requestId: NotificationRequestId;
  readonly recipientId: UserId;
  readonly type: NotificationType;
  readonly subject: string;
  readonly text: string;
  readonly info?: NotificationInfo;
  readonly status: NotificationStatus;
  readonly createdTime: number;
  readonly additionalConfig?: WebDeliveryMethodAdditionalConfig;
}

export interface NotificationInfo {
  description: string;
  type: string;
  alarmSeverity?: AlarmSeverity;
  alarmStatus?: AlarmStatus;
  alarmType?: string;
  stateEntityId?: EntityId;
  acknowledged?: boolean;
  cleared?: boolean;
}

export interface NotificationRequest extends Omit<BaseData<NotificationRequestId>, 'label'> {
  tenantId?: TenantId;
  targets: Array<string>;
  templateId?: NotificationTemplateId;
  template?: NotificationTemplate;
  info?: NotificationInfo;
  deliveryMethods: Array<NotificationDeliveryMethod>;
  originatorEntityId: EntityId;
  status: NotificationRequestStatus;
  stats: NotificationRequestStats;
  additionalConfig: NotificationRequestConfig;
}

export interface NotificationRequestInfo extends NotificationRequest {
  templateName: string;
  deliveryMethods: NotificationDeliveryMethod[];
}

export interface NotificationRequestPreview {
  totalRecipientsCount: number;
  recipientsCountByTarget: { [key in string]: number };
  processedTemplates: { [key in NotificationDeliveryMethod]: DeliveryMethodNotificationTemplate };
  recipientsPreview: Array<string>;
}

export interface NotificationRequestStats {
  sent: Map<NotificationDeliveryMethod, any>;
  errors: { [key in NotificationDeliveryMethod]: {[errorKey in string]: string}};
  totalErrors: number;
}

export interface NotificationRequestConfig {
  sendingDelayInSec: number;
}

export interface NotificationSettings {
  deliveryMethodsConfigs: { [key in NotificationDeliveryMethod]: NotificationDeliveryMethodConfig };
}

export interface NotificationDeliveryMethodConfig extends Partial<SlackNotificationDeliveryMethodConfig &
  MobileNotificationDeliveryMethodConfig>{
  enabled: boolean;
  method: NotificationDeliveryMethod;
}

interface SlackNotificationDeliveryMethodConfig {
  botToken: string;
}

interface MobileNotificationDeliveryMethodConfig {
  firebaseServiceAccountCredentials: string;
  firebaseServiceAccountCredentialsFileName: string;
}

export interface SlackConversation {
  id: string;
  title: string;
  name: string;
  wholeName: string;
  email: string;
  type: string;
}

export interface NotificationRule extends Omit<BaseData<NotificationRuleId>, 'label'>, HasTenantId, ExportableEntity<NotificationRuleId> {
  tenantId: TenantId;
  enabled: boolean;
  templateId: NotificationTemplateId;
  triggerType: TriggerType;
  triggerConfig: NotificationRuleTriggerConfig;
  recipientsConfig: NotificationRuleRecipientConfig;
  additionalConfig?: {description: string};
}

export type NotificationRuleTriggerConfig = Partial<AlarmNotificationRuleTriggerConfig & DeviceInactivityNotificationRuleTriggerConfig &
  EntityActionNotificationRuleTriggerConfig & AlarmCommentNotificationRuleTriggerConfig & AlarmAssignmentNotificationRuleTriggerConfig &
  RuleEngineLifecycleEventNotificationRuleTriggerConfig & EntitiesLimitNotificationRuleTriggerConfig &
  ApiUsageLimitNotificationRuleTriggerConfig & RateLimitsNotificationRuleTriggerConfig & ResourceUsageShortageNotificationRuleTriggerConfig>;

export interface AlarmNotificationRuleTriggerConfig {
  alarmTypes?: Array<string>;
  alarmSeverities?: Array<AlarmSeverity>;
  notifyOn: Array<AlarmAction>;
  clearRule?: ClearRule;
}

interface ClearRule {
  alarmStatuses: Array<AlarmSearchStatus>;
}

export interface DeviceInactivityNotificationRuleTriggerConfig {
  devices?: Array<string>;
  deviceProfiles?: Array<string>;
}

export interface EntityActionNotificationRuleTriggerConfig {
  entityTypes: EntityType[];
  created: boolean;
  updated: boolean;
  deleted: boolean;
}

export interface AlarmCommentNotificationRuleTriggerConfig {
  alarmTypes?: Array<string>;
  alarmSeverities?: Array<AlarmSeverity>;
  alarmStatuses?: Array<AlarmSearchStatus>;
  onlyUserComments?: boolean;
  notifyOnCommentUpdate?: boolean;
}

export interface AlarmAssignmentNotificationRuleTriggerConfig {
  alarmTypes?: Array<string>;
  alarmSeverities?: Array<AlarmSeverity>;
  alarmStatuses?: Array<AlarmSearchStatus>;
  notifyOn: Array<AlarmAssignmentAction>;
}

export interface RuleEngineLifecycleEventNotificationRuleTriggerConfig {
  ruleChains?: Array<string>;
  ruleChainEvents?: Array<ComponentLifecycleEvent>;
  onlyRuleChainLifecycleFailures: boolean;
  trackRuleNodeEvents: boolean;
  ruleNodeEvents: Array<any>;
  onlyRuleNodeLifecycleFailures: ComponentLifecycleEvent;
}

export interface EntitiesLimitNotificationRuleTriggerConfig {
  entityTypes: EntityType[];
  threshold: number;
}

export interface ResourceUsageShortageNotificationRuleTriggerConfig {
  cpuThreshold: number;
  ramThreshold: number;
  storageThreshold: number;
}

export interface ApiUsageLimitNotificationRuleTriggerConfig {
  apiFeatures: ApiFeature[];
  notifyOn: ApiUsageStateValue[];
}

export interface RateLimitsNotificationRuleTriggerConfig {
  apis: LimitedApi[];
}

export enum ComponentLifecycleEvent {
  STARTED = 'STARTED',
  UPDATED = 'UPDATED',
  STOPPED = 'STOPPED'
}

export const ComponentLifecycleEventTranslationMap = new Map<ComponentLifecycleEvent, string>([
  [ComponentLifecycleEvent.STARTED, 'event.started'],
  [ComponentLifecycleEvent.UPDATED, 'event.updated'],
  [ComponentLifecycleEvent.STOPPED, 'event.stopped']
]);

export enum AlarmAction {
  CREATED = 'CREATED',
  SEVERITY_CHANGED = 'SEVERITY_CHANGED',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  CLEARED = 'CLEARED'
}

export const AlarmActionTranslationMap = new Map<AlarmAction, string>([
  [AlarmAction.CREATED, 'notification.notify-alarm-action.created'],
  [AlarmAction.SEVERITY_CHANGED, 'notification.notify-alarm-action.severity-changed'],
  [AlarmAction.ACKNOWLEDGED, 'notification.notify-alarm-action.acknowledged'],
  [AlarmAction.CLEARED, 'notification.notify-alarm-action.cleared']
]);

export enum AlarmAssignmentAction {
  ASSIGNED = 'ASSIGNED',
  UNASSIGNED = 'UNASSIGNED'
}

export const AlarmAssignmentActionTranslationMap = new Map<AlarmAssignmentAction, string>([
  [AlarmAssignmentAction.ASSIGNED, 'notification.notify-alarm-action.assigned'],
  [AlarmAssignmentAction.UNASSIGNED, 'notification.notify-alarm-action.unassigned']
]);

export enum DeviceEvent {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export const DeviceEventTranslationMap = new Map<DeviceEvent, string>([
  [DeviceEvent.ACTIVE, 'notification.active'],
  [DeviceEvent.INACTIVE, 'notification.inactive']
]);

export interface NotificationRuleRecipientConfig {
  targets?: Array<string>;
  escalationTable?: {[key: number]: Array<string>};
}

export interface NonConfirmedNotificationEscalation {
  delayInSec: number;
  targets: Array<string>;
}

export interface NotificationTarget extends Omit<BaseData<NotificationTargetId>, 'label'>, HasTenantId,
  ExportableEntity<NotificationTargetId> {
  tenantId: TenantId;
  configuration: NotificationTargetConfig;
}

export interface NotificationTargetConfig extends Partial<PlatformUsersNotificationTargetConfig
  & SlackNotificationTargetConfig
  & MicrosoftTeamsNotificationTargetConfig> {
  description?: string;
  type: NotificationTargetType;
}
export interface PlatformUsersNotificationTargetConfig {
  usersFilter: UsersFilter;
}

export interface UsersFilter extends
  Partial<UserListFilter & CustomerUsersFilter & TenantAdministratorsFilter>{
  type: NotificationTargetConfigType;
}

interface UserListFilter {
  usersIds: Array<string>;
}

interface CustomerUsersFilter {
  customerId: string;
}

interface TenantAdministratorsFilter {
  tenantsIds?: Array<string>;
  tenantProfilesIds?: Array<string>;
}

export interface SlackNotificationTargetConfig {
  conversationType: SlackChanelType;
  conversation: SlackConversation;
}

export interface MicrosoftTeamsNotificationTargetConfig {
  webhookUrl: string;
  channelName: string;
  useOldApi?: boolean;
}
export enum NotificationTargetType {
  PLATFORM_USERS = 'PLATFORM_USERS',
  SLACK = 'SLACK',
  MICROSOFT_TEAMS = 'MICROSOFT_TEAMS'
}

export const NotificationTargetTypeTranslationMap = new Map<NotificationTargetType, string>([
  [NotificationTargetType.PLATFORM_USERS, 'notification.platform-users'],
  [NotificationTargetType.SLACK, 'notification.delivery-method.slack'],
  [NotificationTargetType.MICROSOFT_TEAMS, 'notification.delivery-method.microsoft-teams'],
]);

export interface NotificationTemplate extends Omit<BaseData<NotificationTemplateId>, 'label'>,
  HasTenantId, ExportableEntity<NotificationTemplateId> {
  tenantId: TenantId;
  notificationType: NotificationType;
  configuration: NotificationTemplateConfig;
}

interface NotificationTemplateConfig {
  deliveryMethodsTemplates: DeliveryMethodsTemplates;
}

export type DeliveryMethodsTemplates = {
  [key in NotificationDeliveryMethod]: DeliveryMethodNotificationTemplate
}

export interface DeliveryMethodNotificationTemplate extends
  Partial<WebDeliveryMethodNotificationTemplate
    & EmailDeliveryMethodNotificationTemplate
    & SlackDeliveryMethodNotificationTemplate
    & MicrosoftTeamsDeliveryMethodNotificationTemplate
    & MobileDeliveryMethodNotificationTemplate>{
  body: string;
  enabled: boolean;
  method: NotificationDeliveryMethod;
}

interface WebDeliveryMethodNotificationTemplate {
  subject: string;
  additionalConfig: WebDeliveryMethodAdditionalConfig;
}

interface WebDeliveryMethodAdditionalConfig {
  icon: {
    enabled: boolean;
    icon: string;
    color: string;
  };
  actionButtonConfig: NotificationButtonConfig;
}

interface NotificationButtonConfig {
  enabled: boolean;
  text: string;
  linkType: ActionButtonLinkType;
  link?: string;
  dashboardId?: string;
  dashboardState?: string;
  setEntityIdInState?: boolean;
}

interface EmailDeliveryMethodNotificationTemplate {
  subject: string;
}

interface SlackDeliveryMethodNotificationTemplate {
  conversationType: SlackChanelType;
  conversationId: string;
}

interface MicrosoftTeamsDeliveryMethodNotificationTemplate {
  subject?: string;
  button: NotificationButtonConfig;
  themeColor?: string;
}

interface MobileDeliveryMethodNotificationTemplate {
  subject: string;
}

export enum NotificationStatus {
  SENT = 'SENT',
  READ = 'READ'
}

export enum NotificationDeliveryMethod {
  WEB = 'WEB',
  MOBILE_APP = 'MOBILE_APP',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
  SLACK = 'SLACK',
  MICROSOFT_TEAMS = 'MICROSOFT_TEAMS'
}

export interface NotificationDeliveryMethodInfo {
  name: string;
  icon: string;
}

export const NotificationDeliveryMethodInfoMap = new Map<NotificationDeliveryMethod, NotificationDeliveryMethodInfo>([
  [NotificationDeliveryMethod.WEB,
    {
      name: 'notification.delivery-method.web',
      icon: 'mdi:bell-badge'
    }
  ],
  [NotificationDeliveryMethod.SMS,
    {
      name: 'notification.delivery-method.sms',
      icon: 'mdi:message-processing'
    }
  ],
  [NotificationDeliveryMethod.EMAIL,
    {
      name: 'notification.delivery-method.email',
      icon: 'mdi:email'
    }],
  [NotificationDeliveryMethod.SLACK,
    {
      name: 'notification.delivery-method.slack',
      icon: 'mdi:slack'
    }
  ],
  [NotificationDeliveryMethod.MOBILE_APP,
    {
      name: 'notification.delivery-method.mobile-app',
      icon: 'mdi:cellphone-text'
    }],
  [NotificationDeliveryMethod.MICROSOFT_TEAMS,
    {
      name: 'notification.delivery-method.microsoft-teams',
      icon: 'mdi:microsoft-teams'
    }]
]);

export enum NotificationRequestStatus {
  PROCESSING = 'PROCESSING',
  SCHEDULED = 'SCHEDULED',
  SENT = 'SENT'
}

export const NotificationRequestStatusTranslateMap = new Map<NotificationRequestStatus, string>([
  [NotificationRequestStatus.PROCESSING, 'notification.request-status.processing'],
  [NotificationRequestStatus.SCHEDULED, 'notification.request-status.scheduled'],
  [NotificationRequestStatus.SENT, 'notification.request-status.sent']
]);

export enum SlackChanelType {
  PUBLIC_CHANNEL = 'PUBLIC_CHANNEL',
  PRIVATE_CHANNEL = 'PRIVATE_CHANNEL',
  DIRECT= 'DIRECT'
}

export const SlackChanelTypesTranslateMap = new Map<SlackChanelType, string>([
  [SlackChanelType.DIRECT, 'notification.slack-chanel-types.direct'],
  [SlackChanelType.PUBLIC_CHANNEL, 'notification.slack-chanel-types.public-channel'],
  [SlackChanelType.PRIVATE_CHANNEL, 'notification.slack-chanel-types.private-channel']
]);

export enum NotificationTargetConfigType {
  ALL_USERS = 'ALL_USERS',
  TENANT_ADMINISTRATORS = 'TENANT_ADMINISTRATORS',
  CUSTOMER_USERS = 'CUSTOMER_USERS',
  USER_LIST = 'USER_LIST',
  ORIGINATOR_ENTITY_OWNER_USERS = 'ORIGINATOR_ENTITY_OWNER_USERS',
  AFFECTED_USER = 'AFFECTED_USER',
  SYSTEM_ADMINISTRATORS = 'SYSTEM_ADMINISTRATORS',
  AFFECTED_TENANT_ADMINISTRATORS = 'AFFECTED_TENANT_ADMINISTRATORS'
}

export interface NotificationTargetConfigTypeInfo {
  name: string;
  hint?: string;
}

export const NotificationTargetConfigTypeInfoMap = new Map<NotificationTargetConfigType, NotificationTargetConfigTypeInfo>([
  [NotificationTargetConfigType.ALL_USERS,
    {
      name: 'notification.recipient-type.all-users'
    }
  ],
  [NotificationTargetConfigType.TENANT_ADMINISTRATORS,
    {
      name: 'notification.recipient-type.tenant-administrators'
    }
  ],
  [NotificationTargetConfigType.CUSTOMER_USERS,
    {
      name: 'notification.recipient-type.customer-users'
    }
  ],
  [NotificationTargetConfigType.USER_LIST,
    {
      name: 'notification.recipient-type.user-list'
    }
  ],
  [NotificationTargetConfigType.ORIGINATOR_ENTITY_OWNER_USERS,
    {
      name: 'notification.recipient-type.users-entity-owner'
    }
  ],
  [NotificationTargetConfigType.AFFECTED_USER,
    {
      name: 'notification.recipient-type.affected-user'
    }
  ],
  [NotificationTargetConfigType.SYSTEM_ADMINISTRATORS,
    {
      name: 'notification.recipient-type.system-administrators'
    }
  ],
  [NotificationTargetConfigType.AFFECTED_TENANT_ADMINISTRATORS,
    {
      name: 'notification.recipient-type.affected-tenant-administrators'
    }
  ]
]);

export enum NotificationType {
  GENERAL = 'GENERAL',
  ALARM = 'ALARM',
  DEVICE_ACTIVITY = 'DEVICE_ACTIVITY',
  ENTITY_ACTION = 'ENTITY_ACTION',
  ALARM_COMMENT = 'ALARM_COMMENT',
  ALARM_ASSIGNMENT = 'ALARM_ASSIGNMENT',
  RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT = 'RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT',
  ENTITIES_LIMIT = 'ENTITIES_LIMIT',
  API_USAGE_LIMIT = 'API_USAGE_LIMIT',
  NEW_PLATFORM_VERSION = 'NEW_PLATFORM_VERSION',
  RULE_NODE = 'RULE_NODE',
  RATE_LIMITS = 'RATE_LIMITS',
  EDGE_CONNECTION = 'EDGE_CONNECTION',
  EDGE_COMMUNICATION_FAILURE = 'EDGE_COMMUNICATION_FAILURE',
  TASK_PROCESSING_FAILURE = 'TASK_PROCESSING_FAILURE',
  RESOURCES_SHORTAGE = 'RESOURCES_SHORTAGE'
}

export const NotificationTypeIcons = new Map<NotificationType, string | null>([
  [NotificationType.ALARM, 'warning'],
  [NotificationType.DEVICE_ACTIVITY, 'phonelink_off'],
  [NotificationType.ENTITY_ACTION, 'devices'],
  [NotificationType.ALARM_COMMENT, 'comment'],
  [NotificationType.ALARM_ASSIGNMENT, 'assignment_turned_in'],
  [NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, 'settings_ethernet'],
  [NotificationType.ENTITIES_LIMIT, 'data_thresholding'],
  [NotificationType.API_USAGE_LIMIT, 'insert_chart'],
  [NotificationType.TASK_PROCESSING_FAILURE, 'warning'],
  [NotificationType.RESOURCES_SHORTAGE, 'warning']
]);

export const AlarmSeverityNotificationColors = new Map<AlarmSeverity, string>(
  [
    [AlarmSeverity.CRITICAL, '#D12730'],
    [AlarmSeverity.MAJOR, '#FEAC0C'],
    [AlarmSeverity.MINOR, '#F2DA05'],
    [AlarmSeverity.WARNING, '#F66716'],
    [AlarmSeverity.INDETERMINATE, '#00000061']
  ]
);

export enum ActionButtonLinkType {
  LINK = 'LINK',
  DASHBOARD  = 'DASHBOARD'
}

export const ActionButtonLinkTypeTranslateMap = new Map<ActionButtonLinkType, string>([
  [ActionButtonLinkType.LINK, 'notification.link-type.link'],
  [ActionButtonLinkType.DASHBOARD, 'notification.link-type.dashboard']
]);

export interface NotificationTemplateTypeTranslate {
  name: string;
  helpId?: string;
}

export const NotificationTemplateTypeTranslateMap = new Map<NotificationType, NotificationTemplateTypeTranslate>([
  [NotificationType.GENERAL,
    {
      name: 'notification.template-type.general',
      helpId: 'notification/general'
    }
  ],
  [NotificationType.ALARM,
    {
      name: 'notification.template-type.alarm',
      helpId: 'notification/alarm'
    }
  ],
  [NotificationType.DEVICE_ACTIVITY,
    {
      name: 'notification.template-type.device-activity',
      helpId: 'notification/device_activity'
    }
  ],
  [NotificationType.ENTITY_ACTION,
    {
      name: 'notification.template-type.entity-action',
      helpId: 'notification/entity_action'
    }
  ],
  [NotificationType.ALARM_COMMENT,
    {
      name: 'notification.template-type.alarm-comment',
      helpId: 'notification/alarm_comment'
    }
  ],
  [NotificationType.ALARM_ASSIGNMENT,
    {
      name: 'notification.template-type.alarm-assignment',
      helpId: 'notification/alarm_assignment'
    }
  ],
  [NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT,
    {
      name: 'notification.template-type.rule-engine-lifecycle-event',
      helpId: 'notification/rule_engine_lifecycle_event'
    }
  ],
  [NotificationType.ENTITIES_LIMIT,
    {
      name: 'notification.template-type.entities-limit',
      helpId: 'notification/entities_limit'
    }
  ],
  [NotificationType.API_USAGE_LIMIT,
    {
      name: 'notification.template-type.api-usage-limit',
      helpId: 'notification/api_usage_limit'
    }
  ],
  [NotificationType.NEW_PLATFORM_VERSION,
    {
      name: 'notification.template-type.new-platform-version',
      helpId: 'notification/new_platform_version'
    }
  ],
  [NotificationType.RULE_NODE,
    {
      name: 'notification.template-type.rule-node',
      helpId: 'notification/rule_node'
    }
  ],
  [NotificationType.RATE_LIMITS,
    {
      name: 'notification.template-type.rate-limits',
      helpId: 'notification/rate_limits'
    }
  ],
  [NotificationType.EDGE_CONNECTION,
    {
      name: 'notification.template-type.edge-connection',
      helpId: 'notification/edge_connection'
    }
  ],
  [NotificationType.EDGE_COMMUNICATION_FAILURE,
    {
      name: 'notification.template-type.edge-communication-failure',
      helpId: 'notification/edge_communication_failure'
    }
  ],
  [NotificationType.TASK_PROCESSING_FAILURE,
    {
      name: 'notification.template-type.task-processing-failure',
      helpId: 'notification/task_processing_failure'
    }
  ],
  [NotificationType.RESOURCES_SHORTAGE,
    {
      name: 'notification.template-type.resources-shortage',
      helpId: 'notification/resources_shortage'
    }
  ]
]);

export enum TriggerType {
  ALARM = 'ALARM',
  DEVICE_ACTIVITY = 'DEVICE_ACTIVITY',
  ENTITY_ACTION = 'ENTITY_ACTION',
  ALARM_COMMENT = 'ALARM_COMMENT',
  ALARM_ASSIGNMENT = 'ALARM_ASSIGNMENT',
  RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT = 'RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT',
  ENTITIES_LIMIT = 'ENTITIES_LIMIT',
  API_USAGE_LIMIT = 'API_USAGE_LIMIT',
  NEW_PLATFORM_VERSION = 'NEW_PLATFORM_VERSION',
  RATE_LIMITS = 'RATE_LIMITS',
  EDGE_CONNECTION = 'EDGE_CONNECTION',
  EDGE_COMMUNICATION_FAILURE = 'EDGE_COMMUNICATION_FAILURE',
  TASK_PROCESSING_FAILURE = 'TASK_PROCESSING_FAILURE',
  RESOURCES_SHORTAGE = 'RESOURCES_SHORTAGE'
}

export const TriggerTypeTranslationMap = new Map<TriggerType, string>([
  [TriggerType.ALARM, 'notification.trigger.alarm'],
  [TriggerType.DEVICE_ACTIVITY, 'notification.trigger.device-activity'],
  [TriggerType.ENTITY_ACTION, 'notification.trigger.entity-action'],
  [TriggerType.ALARM_COMMENT, 'notification.trigger.alarm-comment'],
  [TriggerType.ALARM_ASSIGNMENT, 'notification.trigger.alarm-assignment'],
  [TriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, 'notification.trigger.rule-engine-lifecycle-event'],
  [TriggerType.ENTITIES_LIMIT, 'notification.trigger.entities-limit'],
  [TriggerType.API_USAGE_LIMIT, 'notification.trigger.api-usage-limit'],
  [TriggerType.NEW_PLATFORM_VERSION, 'notification.trigger.new-platform-version'],
  [TriggerType.RATE_LIMITS, 'notification.trigger.rate-limits'],
  [TriggerType.EDGE_CONNECTION, 'notification.trigger.edge-connection'],
  [TriggerType.EDGE_COMMUNICATION_FAILURE, 'notification.trigger.edge-communication-failure'],
  [TriggerType.TASK_PROCESSING_FAILURE, 'notification.trigger.task-processing-failure'],
  [TriggerType.RESOURCES_SHORTAGE, 'notification.trigger.resources-shortage']
]);

export interface NotificationUserSettings {
  prefs: {[key: string]: NotificationUserSetting};
}

export interface NotificationUserSetting {
  enabled: boolean;
  enabledDeliveryMethods: {[key: string]: boolean};
}
