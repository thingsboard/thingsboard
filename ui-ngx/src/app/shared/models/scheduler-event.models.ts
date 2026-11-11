// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { SchedulerEventId } from '@shared/models/id/scheduler-event-id';
import { EntityId } from '@shared/models/id/entity-id';
import * as moment_ from 'moment';
import { PageQueryParam } from '@shared/models/page/page-link';
import { schedulerCalendarView } from '@home/components/scheduler/scheduler-events.models';

export enum SchedulerRepeatType {
  DAILY = 'DAILY',
  EVERY_N_DAYS = 'EVERY_N_DAYS',
  WEEKLY = 'WEEKLY',
  EVERY_N_WEEKS = 'EVERY_N_WEEKS',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
  TIMER = 'TIMER'
}

export const schedulerRepeatTypeTranslationMap = new Map<SchedulerRepeatType, string>(
  [
    [SchedulerRepeatType.DAILY, 'scheduler.daily'],
    [SchedulerRepeatType.EVERY_N_DAYS, 'scheduler.every-n-days'],
    [SchedulerRepeatType.WEEKLY, 'scheduler.weekly'],
    [SchedulerRepeatType.EVERY_N_WEEKS, 'scheduler.every-n-weeks'],
    [SchedulerRepeatType.MONTHLY, 'scheduler.monthly'],
    [SchedulerRepeatType.YEARLY, 'scheduler.yearly'],
    [SchedulerRepeatType.TIMER, 'scheduler.timer']
  ]
);

export const schedulerRepeatTypeToUnitMap = new Map<SchedulerRepeatType, moment_.unitOfTime.Base>(
  [
    [SchedulerRepeatType.MONTHLY, 'month'],
    [SchedulerRepeatType.YEARLY, 'year'],
  ]
);

export enum SchedulerTimeUnit {
  HOURS = 'HOURS',
  MINUTES = 'MINUTES',
  SECONDS = 'SECONDS'
}

export const schedulerTimeUnitToUnitMap = new Map<SchedulerTimeUnit, moment_.unitOfTime.Base>(
  [
    [SchedulerTimeUnit.HOURS, 'hours'],
    [SchedulerTimeUnit.MINUTES, 'minutes'],
    [SchedulerTimeUnit.SECONDS, 'seconds'],
  ]
);

export const schedulerTimeUnitTranslationMap = new Map<SchedulerTimeUnit, string>(
  [
    [SchedulerTimeUnit.HOURS, 'scheduler.hours'],
    [SchedulerTimeUnit.MINUTES, 'scheduler.minutes'],
    [SchedulerTimeUnit.SECONDS, 'scheduler.seconds']
  ]
);

export const schedulerTimeUnitRepeatTranslationMap = new Map<SchedulerTimeUnit, string>(
  [
    [SchedulerTimeUnit.HOURS, 'scheduler.every-hour'],
    [SchedulerTimeUnit.MINUTES, 'scheduler.every-minute'],
    [SchedulerTimeUnit.SECONDS, 'scheduler.every-second']
  ]
);

export const schedulerWeekday: string[] =
  [
    'scheduler.sunday',
    'scheduler.monday',
    'scheduler.tuesday',
    'scheduler.wednesday',
    'scheduler.thursday',
    'scheduler.friday',
    'scheduler.saturday'
  ];

export interface SchedulerEventSchedule {
  timezone?: string;
  startTime?: number;
  repeat?: {
    type: SchedulerRepeatType;
    endsOn: number;
    repeatOn?: number[];
    days?: number;
    weeks?: number;
    repeatInterval?: number;
    timeUnit?: SchedulerTimeUnit;
  };
}

export interface SchedulerEventInfo extends Omit<BaseData<SchedulerEventId>, 'label'>, ExportableEntity<SchedulerEventId> {
  tenantId?: TenantId;
  timestamps?: number[];
  customerId?: CustomerId;
  originatorId?: EntityId;
  name: string;
  type: string;
  enabled?: boolean;
  schedule: SchedulerEventSchedule;
  additionalInfo?: any;
}

export interface SchedulerEventWithCustomerInfo extends SchedulerEventInfo {
  customerTitle: string;
  customerIsPublic: boolean;
  typeName?: string;
}

export interface SchedulerEventConfiguration {
  originatorId?: EntityId;
  msgType?: string;
  msgBody?: any;
  metadata?: any;
}

export interface SchedulerEvent extends SchedulerEventInfo {
  configuration: SchedulerEventConfiguration;
}

export type SchedulerEventMode  = 'list' | 'calendar';

export interface CalendarQueryParam extends PageQueryParam {
  mode?: SchedulerEventMode;
  calendarView?: schedulerCalendarView;
  calendarStart?: number;
}
