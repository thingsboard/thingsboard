///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { BaseData } from './base-data';
import { AuditLogId } from './id/audit-log-id';
import { CustomerId } from './id/customer-id';
import { EntityId } from './id/entity-id';
import { UserId } from './id/user-id';
import { TenantId } from './id/tenant-id';

export enum AuditLogMode {
  TENANT,
  ENTITY,
  USER,
  CUSTOMER
}

export enum ActionType {
  ADDED = 'ADDED',
  DELETED = 'DELETED',
  UPDATED = 'UPDATED',
  ATTRIBUTES_UPDATED = 'ATTRIBUTES_UPDATED',
  ATTRIBUTES_DELETED = 'ATTRIBUTES_DELETED',
  RPC_CALL = 'RPC_CALL',
  CREDENTIALS_UPDATED = 'CREDENTIALS_UPDATED',
  ASSIGNED_TO_CUSTOMER = 'ASSIGNED_TO_CUSTOMER',
  UNASSIGNED_FROM_CUSTOMER = 'UNASSIGNED_FROM_CUSTOMER',
  ACTIVATED = 'ACTIVATED',
  SUSPENDED = 'SUSPENDED',
  CREDENTIALS_READ = 'CREDENTIALS_READ',
  ATTRIBUTES_READ = 'ATTRIBUTES_READ',
  RELATION_ADD_OR_UPDATE = 'RELATION_ADD_OR_UPDATE',
  RELATION_DELETED = 'RELATION_DELETED',
  RELATIONS_DELETED = 'RELATIONS_DELETED',
  ALARM_ACK = 'ALARM_ACK',
  ALARM_CLEAR = 'ALARM_CLEAR',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  LOCKOUT = 'LOCKOUT'
}

export enum ActionStatus {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE'
}

export const actionTypeTranslations = new Map<ActionType, string>(
  [
    [ActionType.ADDED, 'audit-log.type-added'],
    [ActionType.DELETED, 'audit-log.type-deleted'],
    [ActionType.UPDATED, 'audit-log.type-updated'],
    [ActionType.ATTRIBUTES_UPDATED, 'audit-log.type-attributes-updated'],
    [ActionType.ATTRIBUTES_DELETED, 'audit-log.type-attributes-deleted'],
    [ActionType.RPC_CALL, 'audit-log.type-rpc-call'],
    [ActionType.CREDENTIALS_UPDATED, 'audit-log.type-credentials-updated'],
    [ActionType.ASSIGNED_TO_CUSTOMER, 'audit-log.type-assigned-to-customer'],
    [ActionType.UNASSIGNED_FROM_CUSTOMER, 'audit-log.type-unassigned-from-customer'],
    [ActionType.ACTIVATED, 'audit-log.type-activated'],
    [ActionType.SUSPENDED, 'audit-log.type-suspended'],
    [ActionType.CREDENTIALS_READ, 'audit-log.type-credentials-read'],
    [ActionType.ATTRIBUTES_READ, 'audit-log.type-attributes-read'],
    [ActionType.RELATION_ADD_OR_UPDATE, 'audit-log.type-relation-add-or-update'],
    [ActionType.RELATION_DELETED, 'audit-log.type-relation-delete'],
    [ActionType.RELATIONS_DELETED, 'audit-log.type-relations-delete'],
    [ActionType.ALARM_ACK, 'audit-log.type-alarm-ack'],
    [ActionType.ALARM_CLEAR, 'audit-log.type-alarm-clear'],
    [ActionType.LOGIN, 'audit-log.type-login'],
    [ActionType.LOGOUT, 'audit-log.type-logout'],
    [ActionType.LOCKOUT, 'audit-log.type-lockout']
  ]
);

export const actionStatusTranslations = new Map<ActionStatus, string>(
  [
    [ActionStatus.SUCCESS, 'audit-log.status-success'],
    [ActionStatus.FAILURE, 'audit-log.status-failure'],
  ]
);

export interface AuditLog extends BaseData<AuditLogId> {
  tenantId: TenantId;
  customerId: CustomerId;
  entityId: EntityId;
  entityName: string;
  userId: UserId;
  userName: string;
  actionType: ActionType;
  actionData: any;
  actionStatus: ActionStatus;
  actionFailureDetails: string;
}
