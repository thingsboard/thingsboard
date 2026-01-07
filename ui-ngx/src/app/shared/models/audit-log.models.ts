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

import { BaseData } from './base-data';
import { AuditLogId } from './id/audit-log-id';
import { CustomerId } from './id/customer-id';
import { EntityId } from './id/entity-id';
import { UserId } from './id/user-id';
import { TenantId } from './id/tenant-id';
import { isArraysEqualIgnoreUndefined } from "@core/utils";

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
  ALARM_ASSIGNED = 'ALARM_ASSIGNED',
  ALARM_DELETE = 'ALARM_DELETE',
  ALARM_UNASSIGNED = 'ALARM_UNASSIGNED',
  ADDED_COMMENT = 'ADDED_COMMENT',
  UPDATED_COMMENT = 'UPDATED_COMMENT',
  DELETED_COMMENT = 'DELETED_COMMENT',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  LOCKOUT = 'LOCKOUT',
  ASSIGNED_FROM_TENANT = 'ASSIGNED_FROM_TENANT',
  ASSIGNED_TO_TENANT = 'ASSIGNED_TO_TENANT',
  PROVISION_SUCCESS = 'PROVISION_SUCCESS',
  PROVISION_FAILURE = 'PROVISION_FAILURE',
  TIMESERIES_UPDATED = 'TIMESERIES_UPDATED',
  TIMESERIES_DELETED = 'TIMESERIES_DELETED',
  ASSIGNED_TO_EDGE = 'ASSIGNED_TO_EDGE',
  UNASSIGNED_FROM_EDGE = 'UNASSIGNED_FROM_EDGE',
  SMS_SENT = 'SMS_SENT'
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
    [ActionType.ALARM_DELETE, 'audit-log.type-alarm-delete'],
    [ActionType.ALARM_ASSIGNED, 'audit-log.type-alarm-assign'],
    [ActionType.ALARM_UNASSIGNED, 'audit-log.type-alarm-unassign'],
    [ActionType.ADDED_COMMENT, 'audit-log.type-added-comment'],
    [ActionType.UPDATED_COMMENT, 'audit-log.type-updated-comment'],
    [ActionType.DELETED_COMMENT, 'audit-log.type-deleted-comment'],
    [ActionType.LOGIN, 'audit-log.type-login'],
    [ActionType.LOGOUT, 'audit-log.type-logout'],
    [ActionType.LOCKOUT, 'audit-log.type-lockout'],
    [ActionType.ASSIGNED_FROM_TENANT, 'audit-log.type-assigned-from-tenant'],
    [ActionType.ASSIGNED_TO_TENANT, 'audit-log.type-assigned-to-tenant'],
    [ActionType.PROVISION_SUCCESS, 'audit-log.type-provision-success'],
    [ActionType.PROVISION_FAILURE, 'audit-log.type-provision-failure'],
    [ActionType.TIMESERIES_UPDATED, 'audit-log.type-timeseries-updated'],
    [ActionType.TIMESERIES_DELETED, 'audit-log.type-timeseries-deleted'],
    [ActionType.ASSIGNED_TO_EDGE, 'audit-log.type-assigned-to-edge'],
    [ActionType.UNASSIGNED_FROM_EDGE, 'audit-log.type-unassigned-from-edge'],
    [ActionType.SMS_SENT, 'audit-log.type-sms-sent'],
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

export interface AuditLogFilter {
  actionTypes: string[];
}

export const auditLogFilterEquals = (filter1?: AuditLogFilter, filter2?: AuditLogFilter): boolean => {
  return isArraysEqualIgnoreUndefined(filter1.actionTypes, filter2.actionTypes);
};
