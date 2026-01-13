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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { AlarmId } from '@shared/models/id/alarm-id';
import { EntityId } from '@shared/models/id/entity-id';
import { TimePageLink } from '@shared/models/page/page-link';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityType } from '@shared/models/entity-type.models';
import { CustomerId } from '@shared/models/id/customer-id';
import { TableCellButtonActionDescriptor } from '@home/components/widget/lib/table-widget.models';
import { AlarmCommentId } from '@shared/models/id/alarm-comment-id';
import { UserId } from '@shared/models/id/user-id';
import { AlarmFilter } from '@shared/models/query/query.models';
import { HasTenantId } from '@shared/models/entity.models';
import { isNotEmptyStr } from '@core/utils';

export enum AlarmsMode {
  ALL,
  ENTITY
}

export enum AlarmSeverity {
  CRITICAL = 'CRITICAL',
  MAJOR = 'MAJOR',
  MINOR = 'MINOR',
  WARNING = 'WARNING',
  INDETERMINATE = 'INDETERMINATE'
}

export enum AlarmStatus {
  ACTIVE_UNACK = 'ACTIVE_UNACK',
  ACTIVE_ACK = 'ACTIVE_ACK',
  CLEARED_UNACK = 'CLEARED_UNACK',
  CLEARED_ACK = 'CLEARED_ACK'
}

export enum AlarmSearchStatus {
  ANY = 'ANY',
  ACTIVE = 'ACTIVE',
  CLEARED = 'CLEARED',
  ACK = 'ACK',
  UNACK = 'UNACK'
}

export const alarmSeverityTranslations = new Map<AlarmSeverity, string>(
  [
    [AlarmSeverity.CRITICAL, 'alarm.severity-critical'],
    [AlarmSeverity.MAJOR, 'alarm.severity-major'],
    [AlarmSeverity.MINOR, 'alarm.severity-minor'],
    [AlarmSeverity.WARNING, 'alarm.severity-warning'],
    [AlarmSeverity.INDETERMINATE, 'alarm.severity-indeterminate']
  ]
);

export const alarmStatusTranslations = new Map<AlarmStatus, string>(
  [
    [AlarmStatus.ACTIVE_UNACK, 'alarm.display-status.ACTIVE_UNACK'],
    [AlarmStatus.ACTIVE_ACK, 'alarm.display-status.ACTIVE_ACK'],
    [AlarmStatus.CLEARED_UNACK, 'alarm.display-status.CLEARED_UNACK'],
    [AlarmStatus.CLEARED_ACK, 'alarm.display-status.CLEARED_ACK'],
  ]
);

export const alarmSearchStatusTranslations = new Map<AlarmSearchStatus, string>(
  [
    [AlarmSearchStatus.ANY, 'alarm.search-status.ANY'],
    [AlarmSearchStatus.ACTIVE, 'alarm.search-status.ACTIVE'],
    [AlarmSearchStatus.CLEARED, 'alarm.search-status.CLEARED'],
    [AlarmSearchStatus.ACK, 'alarm.search-status.ACK'],
    [AlarmSearchStatus.UNACK, 'alarm.search-status.UNACK']
  ]
);

export const alarmSeverityColors = new Map<AlarmSeverity, string>(
  [
    [AlarmSeverity.CRITICAL, 'red'],
    [AlarmSeverity.MAJOR, 'orange'],
    [AlarmSeverity.MINOR, '#ffca3d'],
    [AlarmSeverity.WARNING, '#abab00'],
    [AlarmSeverity.INDETERMINATE, 'green']
  ]
);

export interface Alarm extends BaseData<AlarmId>, HasTenantId {
  tenantId: TenantId;
  customerId: CustomerId;
  assigneeId: UserId;
  type: string;
  originator: EntityId;
  severity: AlarmSeverity;
  status: AlarmStatus;
  acknowledged: boolean;
  cleared: boolean;
  startTs: number;
  endTs: number;
  ackTs: number;
  clearTs: number;
  assignTs: number;
  propagate: boolean;
  details?: any;
}

export enum AlarmCommentType {
  SYSTEM = 'SYSTEM',
  OTHER = 'OTHER'
}

export interface AlarmComment extends BaseData<AlarmCommentId> {
  alarmId: AlarmId;
  userId?: UserId;
  type: AlarmCommentType;
  comment: {
    text: string;
    edited?: boolean;
    editedOn?: number;
  };
}

export interface AlarmCommentInfo extends AlarmComment {
  firstName?: string;
  lastName?: string;
  email?: string;
}

export interface AlarmInfo extends Alarm {
  originatorName: string;
  originatorLabel: string;
  assignee: AlarmAssignee;
}

export interface AlarmAssignee {
  firstName: string;
  lastName: string;
  email: string;
}

export enum AlarmAssigneeOption {
  noAssignee = 'noAssignee',
  currentUser = 'currentUser'
}

export interface AlarmDataInfo extends AlarmInfo {
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
  [key: string]: any;
}

export const simulatedAlarm: AlarmInfo = {
  id: new AlarmId(NULL_UUID),
  tenantId: new TenantId(NULL_UUID),
  customerId: new CustomerId(NULL_UUID),
  assigneeId: new UserId(NULL_UUID),
  createdTime: new Date().getTime(),
  startTs: new Date().getTime(),
  endTs: 0,
  ackTs: 0,
  clearTs: 0,
  assignTs: 0,
  originatorName: 'Simulated',
  originatorLabel: 'Simulated',
  assignee: {
    firstName: '',
    lastName: '',
    email: 'test@example.com',
  },
  originator: {
    entityType: EntityType.DEVICE,
    id: '1'
  },
  type: 'TEMPERATURE',
  severity: AlarmSeverity.MAJOR,
  status: AlarmStatus.ACTIVE_UNACK,
  acknowledged: false,
  cleared: false,
  details: {
    message: 'Temperature is high!'
  },
  propagate: false
};

export interface AlarmField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export const alarmFields: {[fieldName: string]: AlarmField} = {
  createdTime: {
    keyName: 'createdTime',
    value: 'createdTime',
    name: 'alarm.created-time',
    time: true
  },
  startTime: {
    keyName: 'startTime',
    value: 'startTs',
    name: 'alarm.start-time',
    time: true
  },
  endTime: {
    keyName: 'endTime',
    value: 'endTs',
    name: 'alarm.end-time',
    time: true
  },
  ackTime: {
    keyName: 'ackTime',
    value: 'ackTs',
    name: 'alarm.ack-time',
    time: true
  },
  clearTime: {
    keyName: 'clearTime',
    value: 'clearTs',
    name: 'alarm.clear-time',
    time: true
  },
  assignTime: {
    keyName: 'assignTime',
    value: 'assignTs',
    name: 'alarm.assign-time',
    time: true
  },
  originator: {
    keyName: 'originator',
    value: 'originatorName',
    name: 'alarm.originator'
  },
  originatorLabel: {
    keyName: 'originatorLabel',
    value: 'originatorLabel',
    name: 'alarm.originator-label'
  },
  originatorType: {
    keyName: 'originatorType',
    value: 'originator.entityType',
    name: 'alarm.originator-type'
  },
  type: {
    keyName: 'type',
    value: 'type',
    name: 'alarm.type'
  },
  severity: {
    keyName: 'severity',
    value: 'severity',
    name: 'alarm.severity'
  },
  status: {
    keyName: 'status',
    value: 'status',
    name: 'alarm.status'
  },
  assignee: {
    keyName: 'assignee',
    value: 'assignee',
    name: 'alarm.assignee'
  }
};

export class AlarmQuery {

  affectedEntityId: EntityId;
  pageLink: TimePageLink;
  searchStatus: AlarmSearchStatus;
  status: AlarmStatus;
  fetchOriginator: boolean;
  assigneeId?: UserId;

  constructor(entityId: EntityId, pageLink: TimePageLink,
              searchStatus: AlarmSearchStatus, status: AlarmStatus,
              fetchOriginator: boolean, assigneeId?: UserId) {
    this.affectedEntityId = entityId;
    this.pageLink = pageLink;
    this.searchStatus = searchStatus;
    this.status = status;
    this.fetchOriginator = fetchOriginator;
    this.assigneeId = assigneeId;
  }

  public toQuery(): string {
    let query = this.affectedEntityId ? `/${this.affectedEntityId.entityType}/${this.affectedEntityId.id}` : '';
    query += this.pageLink.toQuery();
    if (this.searchStatus) {
      query += `&searchStatus=${this.searchStatus}`;
    } else if (this.status) {
      query += `&status=${this.status}`;
    }
    if (typeof this.fetchOriginator !== 'undefined' && this.fetchOriginator !== null) {
      query += `&fetchOriginator=${this.fetchOriginator}`;
    }
    if (typeof this.assigneeId !== 'undefined' && this.assigneeId !== null) {
      query += `&assigneeId=${this.assigneeId.id}`;
    }
    return query;
  }

}

export class AlarmQueryV2 {

  affectedEntityId: EntityId;
  pageLink: TimePageLink;
  typeList: string[];
  statusList: AlarmSearchStatus[];
  severityList: AlarmSeverity[];
  assigneeId?: UserId;

  constructor(entityId: EntityId, pageLink: TimePageLink,
              alarmFilter: AlarmFilter) {
    this.affectedEntityId = entityId;
    this.pageLink = pageLink;
    this.typeList = alarmFilter.typeList;
    this.statusList = alarmFilter.statusList;
    this.severityList = alarmFilter.severityList;
    this.assigneeId = alarmFilter.assigneeId;
  }

  public toQuery(): string {
    let query = this.affectedEntityId ? `/${this.affectedEntityId.entityType}/${this.affectedEntityId.id}` : '';
    query += this.pageLink.toQuery();
    if (this.typeList && this.typeList.length) {
      query += `&typeList=${this.typeList.map(type => encodeURIComponent(type)).join(',')}`;
    }
    if (this.statusList && this.statusList.length) {
      query += `&statusList=${this.statusList.join(',')}`;
    }
    if (this.severityList && this.severityList.length) {
      query += `&severityList=${this.severityList.join(',')}`;
    }
    if (typeof this.assigneeId !== 'undefined' && this.assigneeId !== null) {
      query += `&assigneeId=${this.assigneeId.id}`;
    }
    return query;
  }

}

export const getUserDisplayName = (alarmAssignee: AlarmAssignee |  AlarmCommentInfo) => {
  let displayName = '';
  if (isNotEmptyStr(alarmAssignee.firstName) || isNotEmptyStr(alarmAssignee.lastName)) {
    if (alarmAssignee.firstName) {
      displayName += alarmAssignee.firstName;
    }
    if (alarmAssignee.lastName) {
      if (displayName.length > 0) {
        displayName += ' ';
      }
      displayName += alarmAssignee.lastName;
    }
  } else {
    displayName = alarmAssignee.email;
  }
  return displayName;
};

export const getUserInitials = (alarmAssignee: AlarmAssignee): string => {
  let initials = '';
  if (isNotEmptyStr(alarmAssignee.firstName) || isNotEmptyStr(alarmAssignee.lastName)) {
    if (alarmAssignee.firstName) {
      initials += alarmAssignee.firstName.charAt(0);
    }
    if (alarmAssignee.lastName) {
      initials += alarmAssignee.lastName.charAt(0);
    }
  } else {
    initials += alarmAssignee.email.charAt(0);
  }
  return initials.toUpperCase();
};
