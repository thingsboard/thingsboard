///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { NotificationTargetId } from '@shared/models/id/notification-target-id';
import { NotificationTemplateId } from '@shared/models/id/notification-template-id';
import { EntityId } from '@shared/models/id/entity-id';
import { NotificationRuleId } from '@shared/models/id/notification-rule-id';
import { AlarmSearchStatus, AlarmSeverity, AlarmStatus } from '@shared/models/alarm.models';
import { EntityType } from '@shared/models/entity-type.models';

export interface Notification {
  readonly id: NotificationId;
  readonly requestId: NotificationRequestId;
  readonly recipientId: UserId;
  readonly type: NotificationType;
  readonly subject: string;
  readonly text: string;
  readonly info: NotificationInfo;
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
}

export interface NotificationRequestStats {
  sent: Map<NotificationDeliveryMethod, any>;
  errors: { [key in NotificationDeliveryMethod]: {[errorKey in string]: string}};
  processedRecipients: Map<NotificationDeliveryMethod, Set<UserId>>;
}

export interface NotificationRequestConfig {
  sendingDelayInSec: number;
}

export interface NotificationSettings {
  deliveryMethodsConfigs: { [key in NotificationDeliveryMethod]: NotificationDeliveryMethodConfig };
}

export interface NotificationDeliveryMethodConfig extends Partial<SlackNotificationDeliveryMethodConfig>{
  enabled: boolean;
  method: NotificationDeliveryMethod;
}

interface SlackNotificationDeliveryMethodConfig {
  botToken: string;
}

export interface SlackConversation {
  id: string;
  name: string;
}

export interface NotificationRule extends Omit<BaseData<NotificationRuleId>, 'label'>{
  tenantId: TenantId;
  templateId: NotificationTemplateId;
  triggerType: TriggerType;
  triggerConfig: NotificationRuleTriggerConfig;
  recipientsConfig: NotificationRuleRecipientConfig;
  additionalConfig: {description: string};
}

export type NotificationRuleTriggerConfig = Partial<AlarmNotificationRuleTriggerConfig & DeviceInactivityNotificationRuleTriggerConfig &
  EntityActionNotificationRuleTriggerConfig & AlarmCommentNotificationRuleTriggerConfig & AlarmAssignmentNotificationRuleTriggerConfig &
  RuleEngineLifecycleEventNotificationRuleTriggerConfig & EntitiesLimitNotificationRuleTriggerConfig>;

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
  entityType: EntityType;
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

export interface NotificationRuleRecipientConfig {
  targets?: Array<string>;
  escalationTable?: {[key: number]: Array<string>};
}

export interface NonConfirmedNotificationEscalation {
  delayInSec: number;
  targets: Array<string>;
}

export interface NotificationTarget extends Omit<BaseData<NotificationTargetId>, 'label'>{
  tenantId: TenantId;
  configuration: NotificationTargetConfig;
}

export interface NotificationTargetConfig extends Partial<PlatformUsersNotificationTargetConfig & SlackNotificationTargetConfig> {
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
export enum NotificationTargetType {
  PLATFORM_USERS = 'PLATFORM_USERS',
  SLACK = 'SLACK'
}

export const NotificationTargetTypeTranslationMap = new Map<NotificationTargetType, string>([
  [NotificationTargetType.PLATFORM_USERS, 'notification.platform-users'],
  [NotificationTargetType.SLACK, 'notification.delivery-method.slack']
]);

export interface NotificationTemplate extends Omit<BaseData<NotificationTemplateId>, 'label'>{
  tenantId: TenantId;
  notificationType: NotificationType;
  configuration: NotificationTemplateConfig;
}

interface NotificationTemplateConfig {
  deliveryMethodsTemplates: {
    [key in NotificationDeliveryMethod]: DeliveryMethodNotificationTemplate
  };
}

export interface DeliveryMethodNotificationTemplate extends
  Partial<WebDeliveryMethodNotificationTemplate & EmailDeliveryMethodNotificationTemplate & SlackDeliveryMethodNotificationTemplate>{
  body?: string;
  enabled: boolean;
  method: NotificationDeliveryMethod;
}

interface WebDeliveryMethodNotificationTemplate {
  subject?: string;
  additionalConfig: WebDeliveryMethodAdditionalConfig;
}

interface WebDeliveryMethodAdditionalConfig {
  icon: {
    enabled: boolean;
    icon: string;
    color: string;
  };
  actionButtonConfig: {
    enabled: boolean;
    text: string;
    linkType: ActionButtonLinkType;
    link?: string;
    dashboardId?: string;
    dashboardState?: string;
    setEntityIdInState?: boolean;
  };
}

interface EmailDeliveryMethodNotificationTemplate {
  subject: string;
}

interface SlackDeliveryMethodNotificationTemplate {
  conversationType: SlackChanelType;
  conversationId: string;
}

export enum NotificationStatus {
  SENT = 'SENT',
  READ = 'READ'
}

export enum NotificationDeliveryMethod {
  WEB = 'WEB',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
  SLACK = 'SLACK'
}

export const NotificationDeliveryMethodTranslateMap = new Map<NotificationDeliveryMethod, string>([
  [NotificationDeliveryMethod.WEB, 'notification.delivery-method.web'],
  [NotificationDeliveryMethod.SMS, 'notification.delivery-method.sms'],
  [NotificationDeliveryMethod.EMAIL, 'notification.delivery-method.email'],
  [NotificationDeliveryMethod.SLACK, 'notification.delivery-method.slack']
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
  AFFECTED_USER = 'AFFECTED_USER'
}

interface NotificationTargetConfigTypeInfo {
  name: string;
  hint?: string;
}

export const NotificationTargetConfigTypeInfoMap = new Map<NotificationTargetConfigType, NotificationTargetConfigTypeInfo>([
  [NotificationTargetConfigType.ALL_USERS,
    {
      name: 'notification.target-type.all-users'
    }
  ],
  [NotificationTargetConfigType.TENANT_ADMINISTRATORS,
    {
      name: 'notification.target-type.tenant-administrators'
    }
  ],
  [NotificationTargetConfigType.CUSTOMER_USERS,
    {
      name: 'notification.target-type.customer-users'
    }
  ],
  [NotificationTargetConfigType.USER_LIST,
    {
      name: 'notification.target-type.user-list'
    }
  ],
  [NotificationTargetConfigType.ORIGINATOR_ENTITY_OWNER_USERS,
    {
      name: 'notification.target-type.users-entity-owner',
      hint: 'notification.target-type.users-entity-owner-hint'
    }
  ],
  [NotificationTargetConfigType.AFFECTED_USER,
    {
      name: 'notification.target-type.affected-user',
      hint: 'notification.target-type.affected-user-hint'
    }
  ]
]);

export enum NotificationType {
  GENERAL = 'GENERAL',
  ALARM = 'ALARM',
  DEVICE_INACTIVITY = 'DEVICE_INACTIVITY',
  ENTITY_ACTION = 'ENTITY_ACTION',
  ALARM_COMMENT = 'ALARM_COMMENT',
  ALARM_ASSIGNMENT = 'ALARM_ASSIGNMENT',
  RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT = 'RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT',
  ENTITIES_LIMIT = 'ENTITIES_LIMIT'
}

export const NotificationTypeIcons = new Map<NotificationType, string | null>([
  [NotificationType.ALARM, 'warning'],
  [NotificationType.DEVICE_INACTIVITY, 'phonelink_off'],
  [NotificationType.ENTITY_ACTION, 'devices'],
  [NotificationType.ALARM_COMMENT, 'comment'],
  [NotificationType.ALARM_ASSIGNMENT, 'assignment_turned_in'],
  [NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, 'settings_ethernet'],
  [NotificationType.ENTITIES_LIMIT, 'data_thresholding'],
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

interface NotificationTemplateTypeTranslate {
  name: string;
  hint?: string;
}

export const NotificationTemplateTypeTranslateMap = new Map<NotificationType, NotificationTemplateTypeTranslate>([
  [NotificationType.GENERAL,
    {
      name: 'notification.template-type.general',
      hint: 'notification.template-hint.general'
    }
  ],
  [NotificationType.ALARM,
    {
      name: 'notification.template-type.alarm',
      hint: 'notification.template-hint.alarm'
    }
  ],
  [NotificationType.DEVICE_INACTIVITY,
    {
      name: 'notification.template-type.device-inactivity',
      hint: 'notification.template-hint.device-inactivity'
    }
  ],
  [NotificationType.ENTITY_ACTION,
    {
      name: 'notification.template-type.entity-action',
      hint: 'notification.template-hint.entity-action'
    }
  ],
  [NotificationType.ALARM_COMMENT,
    {
      name: 'notification.template-type.alarm-comment',
      hint: 'notification.template-hint.alarm-comment'
    }
  ],
  [NotificationType.ALARM_ASSIGNMENT,
    {
      name: 'notification.template-type.alarm-assignment',
      hint: 'notification.template-hint.alarm-assignment'
    }
  ],
  [NotificationType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT,
    {
      name: 'notification.template-type.rule-engine-lifecycle-event',
      hint: 'notification.template-hint.rule-engine-lifecycle-event'
    }
  ],
  [NotificationType.ENTITIES_LIMIT,
    {
      name: 'notification.template-type.entities-limit',
      hint: 'notification.template-hint.entities-limit'
    }]
]);

export enum TriggerType {
  ALARM = 'ALARM',
  DEVICE_INACTIVITY = 'DEVICE_INACTIVITY',
  ENTITY_ACTION = 'ENTITY_ACTION',
  ALARM_COMMENT = 'ALARM_COMMENT',
  ALARM_ASSIGNMENT = 'ALARM_ASSIGNMENT',
  RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT = 'RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT',
  ENTITIES_LIMIT = 'ENTITIES_LIMIT'
}

export const TriggerTypeTranslationMap = new Map<TriggerType, string>([
  [TriggerType.ALARM, 'notification.trigger.alarm'],
  [TriggerType.DEVICE_INACTIVITY, 'notification.trigger.device-inactivity'],
  [TriggerType.ENTITY_ACTION, 'notification.trigger.entity-action'],
  [TriggerType.ALARM_COMMENT, 'notification.trigger.alarm-comment'],
  [TriggerType.ALARM_ASSIGNMENT, 'notification.trigger.alarm-assignment'],
  [TriggerType.RULE_ENGINE_COMPONENT_LIFECYCLE_EVENT, 'notification.trigger.rule-engine-lifecycle-event'],
  [TriggerType.ENTITIES_LIMIT, 'notification.trigger.entities-limit'],
]);
