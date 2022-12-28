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
import { DashboardId } from '@shared/models/id/dashboard-id';
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
  readonly type: string;
  readonly text: string;
  readonly info: NotificationInfo;
  readonly originatorType: NotificationOriginatorType;
  readonly status: NotificationStatus;
}

export interface NotificationInfo {
  description: string;
  dashboardId: DashboardId;
  originatorType: NotificationOriginatorType;
}

export interface NotificationRequest extends Omit<BaseData<NotificationRequestId>, 'label'> {
  tenantId?: TenantId;
  targets: Array<NotificationTargetId>;
  templateId: NotificationTemplateId;
  info?: NotificationInfo;
  deliveryMethods: Array<NotificationDeliveryMethod>;
  originatorEntityId: EntityId;
  originatorType: NotificationOriginatorType;
  status: NotificationRequestStatus;
  stats: NotificationRequestStats;
  additionalConfig: NotificationRequestConfig;
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
  deliveryMethodsConfigs: Map<NotificationDeliveryMethod, NotificationDeliveryMethodConfig>;
}

export interface NotificationDeliveryMethodConfig {
  enabled: boolean;
  method: NotificationDeliveryMethod;
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
  configuration: NotificationTargetConfig;
}

export interface NotificationTargetConfig extends
  Partial<SingleUserNotificationTargetConfig & UserListNotificationTargetConfig & CustomerUsersNotificationTargetConfig>{
  type: NotificationTargetConfigType;
}

interface SingleUserNotificationTargetConfig {
  userId: string;
}

interface UserListNotificationTargetConfig {
  usersIds: Array<string>;
}

interface CustomerUsersNotificationTargetConfig {
  customerId: string;
  getCustomerIdFromOriginatorEntity: boolean;
}

export interface NotificationTemplate extends Omit<BaseData<NotificationTemplateId>, 'label'>{
  tenantId: TenantId;
  notificationType: string;
  configuration: NotificationTemplateConfig;
}

interface NotificationTemplateConfig {
  defaultTextTemplate: string;
  templates: Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate>;
}

interface DeliveryMethodNotificationTemplate extends
  Partial<EmailDeliveryMethodNotificationTemplate & SlackDeliveryMethodNotificationTemplate>{
  body: string;
  method: NotificationDeliveryMethod;
}

interface EmailDeliveryMethodNotificationTemplate {
  subject: string;
}

interface SlackDeliveryMethodNotificationTemplate {
  conversationType: SlackChanelType;
  conversationId: string;
}

export enum NotificationOriginatorType {
  ADMIN = 'ADMIN',
  ALARM = 'ALARM',
  RULE_NODE = 'RULE_NODE'
}

export enum NotificationStatus {
  SENT = 'SENT',
  READ = 'READ'
}

export enum NotificationDeliveryMethod {
  WEBSOCKET = 'WEBSOCKET',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
  SLACK = 'SLACK'
}

export enum NotificationRequestStatus {
  PROCESSED = 'PROCESSED',
  SCHEDULED = 'SCHEDULED'
}

export enum SlackChanelType {
  USER= 'USER',
  PUBLIC_CHANNEL = 'PUBLIC_CHANNEL',
  PRIVATE_CHANNEL = 'PRIVATE_CHANNEL'
}

export enum NotificationTargetConfigType {
  SINGLE_USER = 'SINGLE_USER',
  USER_LIST = 'USER_LIST',
  CUSTOMER_USERS = 'CUSTOMER_USERS',
  ALL_USERS = 'ALL_USERS'
}
