// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  SchedulerEventSchedule,
  SchedulerRepeatType,
  schedulerTimeUnitRepeatTranslationMap,
  schedulerWeekday
} from '@shared/models/scheduler-event.models';
import { TranslateService } from '@ngx-translate/core';
import _moment from 'moment';

export enum schedulerCalendarView {
  month = 'month',
  week = 'week',
  day = 'day',
  listYear = 'listYear',
  listMonth = 'listMonth',
  listWeek = 'listWeek',
  listDay = 'listDay',
  agendaWeek = 'agendaWeek',
  agendaDay = 'agendaDay'
}

export const schedulerCalendarViewValueMap = new Map<schedulerCalendarView, string>(
  [
    [schedulerCalendarView.month, 'dayGridMonth'],
    [schedulerCalendarView.week, 'dayGridWeek'],
    [schedulerCalendarView.day, 'dayGridDay'],
    [schedulerCalendarView.listYear, 'listYear'],
    [schedulerCalendarView.listMonth, 'listMonth'],
    [schedulerCalendarView.listWeek, 'listWeek'],
    [schedulerCalendarView.listDay, 'listDay'],
    [schedulerCalendarView.agendaWeek, 'timeGridWeek'],
    [schedulerCalendarView.agendaDay, 'timeGridDay']
  ]
);

export const schedulerCalendarViewTranslationMap = new Map<schedulerCalendarView, string>(
  [
    [schedulerCalendarView.month, 'scheduler.month'],
    [schedulerCalendarView.week, 'scheduler.week'],
    [schedulerCalendarView.day, 'scheduler.day'],
    [schedulerCalendarView.listYear, 'scheduler.list-year'],
    [schedulerCalendarView.listMonth, 'scheduler.list-month'],
    [schedulerCalendarView.listWeek, 'scheduler.list-week'],
    [schedulerCalendarView.listDay, 'scheduler.list-day'],
    [schedulerCalendarView.agendaWeek, 'scheduler.agenda-week'],
    [schedulerCalendarView.agendaDay, 'scheduler.agenda-day']
  ]
);

export const scheduleWeekDays: Array<SchedulerWeekDay> = [
  { label: 'scheduler.sunday-label', tooltip: 'scheduler.repeat-on-sunday' },
  { label: 'scheduler.monday-label', tooltip: 'scheduler.repeat-on-monday' },
  { label: 'scheduler.tuesday-label', tooltip: 'scheduler.repeat-on-tuesday' },
  { label: 'scheduler.wednesday-label', tooltip: 'scheduler.repeat-on-wednesday' },
  { label: 'scheduler.thursday-label', tooltip: 'scheduler.repeat-on-thursday' },
  { label: 'scheduler.friday-label', tooltip: 'scheduler.repeat-on-friday' },
  { label: 'scheduler.saturday-label', tooltip: 'scheduler.repeat-on-saturday' }
];

export interface CustomSchedulerEventType {
  name: string;
  value: string;
  originator: boolean;
  msgType: boolean;
  metadata: boolean;
  template: string;
}

export interface SchedulerEventsWidgetSettings {
  title: string;
  displayCreatedTime: boolean;
  displayType: boolean;
  displayCustomer: boolean;
  displaySchedule?: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  pageStepIncrement: number;
  pageStepCount: number;
  defaultSortOrder: string;
  noDataDisplayMessage: string;
  enabledViews: 'both' | 'list' | 'calendar';
  forceDefaultEventType: string;
  customEventTypes: CustomSchedulerEventType[];
}

export interface SchedulerWeekDay {
  label: string;
  tooltip: string;
}

export const scheduleInfo = (
  schedule: SchedulerEventSchedule,
  translate: TranslateService,
  startTime: _moment.Moment = _moment(schedule.startTime)
): string => {
  let info = '';
  if (!schedule.repeat) {
    const start = startTime.local().format('MMM DD, YYYY, hh:mma');
    info += start;
    return info;
  } else {
    info += startTime.local().format('hh:mma');
    info += '<br/>';
    info += translate.instant('scheduler.starting-from') + ' ' + startTime.local().format('MMM DD, YYYY') + ', ';
    if (schedule.repeat.type === SchedulerRepeatType.DAILY) {
      info += translate.instant('scheduler.daily') + ', ';
    } else if (schedule.repeat.type === SchedulerRepeatType.EVERY_N_DAYS) {
      info += translate.instant('scheduler.every-n-days-text', {days: schedule.repeat.days}) + ', ';
    } else if (schedule.repeat.type === SchedulerRepeatType.MONTHLY) {
      info += translate.instant('scheduler.monthly') + ', ';
    } else if (schedule.repeat.type === SchedulerRepeatType.EVERY_N_WEEKS) {
      info += translate.instant('scheduler.every-n-weeks-text', {weeks: schedule.repeat.weeks}) + ', ';
    } else if (schedule.repeat.type === SchedulerRepeatType.YEARLY) {
      info += translate.instant('scheduler.yearly') + ', ';
    } else if (schedule.repeat.type === SchedulerRepeatType.TIMER) {
      const repeatInterval = translate.instant(schedulerTimeUnitRepeatTranslationMap.get(schedule.repeat.timeUnit),
        {count: schedule.repeat.repeatInterval});
      info += repeatInterval + ', ';
    } else {
      info += translate.instant('scheduler.weekly') + ' ' + translate.instant('scheduler.on') + ' ';
      schedule.repeat.repeatOn.forEach((day) => {
        info += translate.instant(schedulerWeekday[day]) + ', ';
      });
    }
    info += translate.instant('scheduler.until') + ' ';
    info += _moment.utc(schedule.repeat.endsOn).local().format('MMM DD, YYYY');
    return info;
  }
}
