///
/// Copyright © 2016-2022 The Thingsboard Authors
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
  readonly additionalConfig?: PushDeliveryMethodAdditionalConfig;
}

export interface NotificationInfo {
  description: string;
  type: string;
}

export interface NotificationRequest extends Omit<BaseData<NotificationRequestId>, 'label'> {
  tenantId?: TenantId;
  targets: Array<string>;
  templateId: NotificationTemplateId;
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
  errors: Map<NotificationDeliveryMethod, Map<string, string>>;
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
  deliveryMethods: Array<NotificationDeliveryMethod>;
  configuration: NotificationRuleConfig;
}

export interface NotificationRuleConfig {
  initialNotificationTargetId: NotificationTargetId;
  escalationConfig: NotificationEscalationConfig;
}

export interface NotificationEscalationConfig {
  escalations: Array<NonConfirmedNotificationEscalation>;
}

export interface NonConfirmedNotificationEscalation {
  delayInSec: number;
  notificationTargetId: NotificationTargetId;
}

export interface NotificationTarget extends Omit<BaseData<NotificationTargetId>, 'label'>{
  tenantId: TenantId;
  type: NotificationTargetType;
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
  Partial<UserListNotificationTargetConfig & CustomerUsersNotificationTargetConfig>{
  type: NotificationTargetConfigType;
}

interface UserListNotificationTargetConfig {
  usersIds: Array<string>;
}

interface CustomerUsersNotificationTargetConfig {
  customerId: string;
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
  [NotificationTargetType.SLACK, 'notification.slack']
]);

export interface NotificationTemplate extends Omit<BaseData<NotificationTemplateId>, 'label'>{
  tenantId: TenantId;
  notificationType: NotificationType;
  configuration: NotificationTemplateConfig;
}

interface NotificationTemplateConfig {
  defaultTextTemplate: string;
  notificationSubject: string;
  deliveryMethodsTemplates: {
    [key in NotificationDeliveryMethod]: DeliveryMethodNotificationTemplate
  };
}

export interface DeliveryMethodNotificationTemplate extends
  Partial<PushDeliveryMethodNotificationTemplate & EmailDeliveryMethodNotificationTemplate & SlackDeliveryMethodNotificationTemplate>{
  body?: string;
  enabled: boolean;
  method: NotificationDeliveryMethod;
}

interface PushDeliveryMethodNotificationTemplate {
  subject?: string;
  additionalConfig: PushDeliveryMethodAdditionalConfig;
}

interface PushDeliveryMethodAdditionalConfig {
  icon: {
    enabled: boolean;
    icon: string;
    color: string;
  };
  actionButtonConfig: {
    enabled: boolean;
    text: string;
    color: string;
    link: string;
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
  PUSH = 'PUSH',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
  SLACK = 'SLACK'
}

export const NotificationDeliveryMethodTranslateMap = new Map<NotificationDeliveryMethod, string>([
  [NotificationDeliveryMethod.PUSH, 'notification.delivery-method-type.push'],
  [NotificationDeliveryMethod.SMS, 'notification.delivery-method-type.sms'],
  [NotificationDeliveryMethod.EMAIL, 'notification.delivery-method-type.email'],
  [NotificationDeliveryMethod.SLACK, 'notification.delivery-method-type.slack'],
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
  DIRECT= 'DIRECT',
  PUBLIC_CHANNEL = 'PUBLIC_CHANNEL',
  PRIVATE_CHANNEL = 'PRIVATE_CHANNEL'
}

export const SlackChanelTypesTranslateMap = new Map<SlackChanelType, string>([
  [SlackChanelType.DIRECT, 'notification.slack-chanel-types.direct'],
  [SlackChanelType.PUBLIC_CHANNEL, 'notification.slack-chanel-types.public-channel'],
  [SlackChanelType.PRIVATE_CHANNEL, 'notification.slack-chanel-types.private-channel']
]);

export enum NotificationTargetConfigType {
  ALL_USERS = 'ALL_USERS',
  USER_LIST = 'USER_LIST',
  CUSTOMER_USERS = 'CUSTOMER_USERS',
  ORIGINATOR_ENTITY_OWNER_USERS = 'ORIGINATOR_ENTITY_OWNER_USERS'
}

export const NotificationTargetConfigTypeTranslateMap = new Map<NotificationTargetConfigType, string>([
  [NotificationTargetConfigType.ALL_USERS, 'notification.target-type.all-users'],
  [NotificationTargetConfigType.USER_LIST, 'notification.target-type.user-list'],
  [NotificationTargetConfigType.CUSTOMER_USERS, 'notification.target-type.customer-users'],
  [NotificationTargetConfigType.ORIGINATOR_ENTITY_OWNER_USERS, 'notification.target-type.originator-entity-owner-users']
]);

export enum NotificationType {
  GENERAL = 'GENERAL',
  ALARM = 'ALARM',
  DEVICE_INACTIVITY = 'DEVICE_INACTIVITY',
  ENTITY_ACTION = 'ENTITY_ACTION'
}

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
  ]
]);
