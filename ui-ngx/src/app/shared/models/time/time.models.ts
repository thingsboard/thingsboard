///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { TimeService } from '@core/services/time.service';
import { deepClone, isDefined, isUndefined } from '@app/core/utils';
import * as moment_ from 'moment';
import * as momentTz from 'moment-timezone';

const moment = moment_;

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;
export const WEEK = 7 * DAY;
export const YEAR = DAY * 365;

export type ComparisonDuration = moment_.unitOfTime.DurationConstructor | 'previousInterval';

export enum TimewindowType {
  REALTIME,
  HISTORY
}

export enum RealtimeWindowType {
  LAST_INTERVAL,
  INTERVAL
}

export enum HistoryWindowType {
  LAST_INTERVAL,
  FIXED,
  INTERVAL
}

export interface IntervalWindow {
  interval?: number;
  timewindowMs?: number;
  quickInterval?: QuickTimeInterval;
}

export interface RealtimeWindow extends IntervalWindow{
  realtimeType?: RealtimeWindowType;
}

export interface FixedWindow {
  startTimeMs: number;
  endTimeMs: number;
}

export interface HistoryWindow extends IntervalWindow {
  historyType?: HistoryWindowType;
  fixedTimewindow?: FixedWindow;
}

export enum AggregationType {
  MIN = 'MIN',
  MAX = 'MAX',
  AVG = 'AVG',
  SUM = 'SUM',
  COUNT = 'COUNT',
  NONE = 'NONE'
}

export const aggregationTranslations = new Map<AggregationType, string>(
  [
    [AggregationType.MIN, 'aggregation.min'],
    [AggregationType.MAX, 'aggregation.max'],
    [AggregationType.AVG, 'aggregation.avg'],
    [AggregationType.SUM, 'aggregation.sum'],
    [AggregationType.COUNT, 'aggregation.count'],
    [AggregationType.NONE, 'aggregation.none'],
  ]
);

export interface Aggregation {
  interval?: number;
  type: AggregationType;
  limit: number;
}

export interface Timewindow {
  displayValue?: string;
  displayTimezoneAbbr?: string;
  hideInterval?: boolean;
  hideAggregation?: boolean;
  hideAggInterval?: boolean;
  hideTimezone?: boolean;
  selectedTab?: TimewindowType;
  realtime?: RealtimeWindow;
  history?: HistoryWindow;
  aggregation?: Aggregation;
  timezone?: string;
}

export interface SubscriptionAggregation extends Aggregation {
  interval?: number;
  timeWindow?: number;
  stateData?: boolean;
}

export interface SubscriptionTimewindow {
  startTs?: number;
  quickInterval?: QuickTimeInterval;
  timezone?: string;
  tsOffset?: number;
  realtimeWindowMs?: number;
  fixedWindow?: FixedWindow;
  aggregation?: SubscriptionAggregation;
  timeForComparison?: ComparisonDuration;
}

export interface WidgetTimewindow {
  minTime?: number;
  maxTime?: number;
  interval?: number;
  timezone?: string;
  stDiff?: number;
}

export enum QuickTimeInterval {
  YESTERDAY = 'YESTERDAY',
  DAY_BEFORE_YESTERDAY = 'DAY_BEFORE_YESTERDAY',
  THIS_DAY_LAST_WEEK = 'THIS_DAY_LAST_WEEK',
  PREVIOUS_WEEK = 'PREVIOUS_WEEK',
  PREVIOUS_WEEK_ISO = 'PREVIOUS_WEEK_ISO',
  PREVIOUS_MONTH = 'PREVIOUS_MONTH',
  PREVIOUS_YEAR = 'PREVIOUS_YEAR',
  CURRENT_HOUR = 'CURRENT_HOUR',
  CURRENT_DAY = 'CURRENT_DAY',
  CURRENT_DAY_SO_FAR = 'CURRENT_DAY_SO_FAR',
  CURRENT_WEEK = 'CURRENT_WEEK',
  CURRENT_WEEK_ISO = 'CURRENT_WEEK_ISO',
  CURRENT_WEEK_SO_FAR = 'CURRENT_WEEK_SO_FAR',
  CURRENT_WEEK_ISO_SO_FAR = 'CURRENT_WEEK_ISO_SO_FAR',
  CURRENT_MONTH = 'CURRENT_MONTH',
  CURRENT_MONTH_SO_FAR = 'CURRENT_MONTH_SO_FAR',
  CURRENT_YEAR = 'CURRENT_YEAR',
  CURRENT_YEAR_SO_FAR = 'CURRENT_YEAR_SO_FAR'
}

export const QuickTimeIntervalTranslationMap = new Map<QuickTimeInterval, string>([
  [QuickTimeInterval.YESTERDAY, 'timeinterval.predefined.yesterday'],
  [QuickTimeInterval.DAY_BEFORE_YESTERDAY, 'timeinterval.predefined.day-before-yesterday'],
  [QuickTimeInterval.THIS_DAY_LAST_WEEK, 'timeinterval.predefined.this-day-last-week'],
  [QuickTimeInterval.PREVIOUS_WEEK, 'timeinterval.predefined.previous-week'],
  [QuickTimeInterval.PREVIOUS_WEEK_ISO, 'timeinterval.predefined.previous-week-iso'],
  [QuickTimeInterval.PREVIOUS_MONTH, 'timeinterval.predefined.previous-month'],
  [QuickTimeInterval.PREVIOUS_YEAR, 'timeinterval.predefined.previous-year'],
  [QuickTimeInterval.CURRENT_HOUR, 'timeinterval.predefined.current-hour'],
  [QuickTimeInterval.CURRENT_DAY, 'timeinterval.predefined.current-day'],
  [QuickTimeInterval.CURRENT_DAY_SO_FAR, 'timeinterval.predefined.current-day-so-far'],
  [QuickTimeInterval.CURRENT_WEEK, 'timeinterval.predefined.current-week'],
  [QuickTimeInterval.CURRENT_WEEK_ISO, 'timeinterval.predefined.current-week-iso'],
  [QuickTimeInterval.CURRENT_WEEK_SO_FAR, 'timeinterval.predefined.current-week-so-far'],
  [QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR, 'timeinterval.predefined.current-week-iso-so-far'],
  [QuickTimeInterval.CURRENT_MONTH, 'timeinterval.predefined.current-month'],
  [QuickTimeInterval.CURRENT_MONTH_SO_FAR, 'timeinterval.predefined.current-month-so-far'],
  [QuickTimeInterval.CURRENT_YEAR, 'timeinterval.predefined.current-year'],
  [QuickTimeInterval.CURRENT_YEAR_SO_FAR, 'timeinterval.predefined.current-year-so-far']
]);

export function historyInterval(timewindowMs: number): Timewindow {
  return {
    selectedTab: TimewindowType.HISTORY,
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      timewindowMs
    }
  };
}

export function defaultTimewindow(timeService: TimeService): Timewindow {
  const currentTime = moment().valueOf();
  return {
    displayValue: '',
    hideInterval: false,
    hideAggregation: false,
    hideAggInterval: false,
    hideTimezone: false,
    selectedTab: TimewindowType.REALTIME,
    realtime: {
      realtimeType: RealtimeWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      quickInterval: QuickTimeInterval.CURRENT_DAY
    },
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      fixedTimewindow: {
        startTimeMs: currentTime - DAY,
        endTimeMs: currentTime
      },
      quickInterval: QuickTimeInterval.CURRENT_DAY
    },
    aggregation: {
      type: AggregationType.AVG,
      limit: Math.floor(timeService.getMaxDatapointsLimit() / 2)
    }
  };
}

function getTimewindowType(timewindow: Timewindow): TimewindowType {
  if (isUndefined(timewindow.selectedTab)) {
    return isDefined(timewindow.realtime) ? TimewindowType.REALTIME : TimewindowType.HISTORY;
  } else {
    return timewindow.selectedTab;
  }
}

export function initModelFromDefaultTimewindow(value: Timewindow, timeService: TimeService): Timewindow {
  const model = defaultTimewindow(timeService);
  if (value) {
    model.hideInterval = value.hideInterval;
    model.hideAggregation = value.hideAggregation;
    model.hideAggInterval = value.hideAggInterval;
    model.hideTimezone = value.hideTimezone;
    model.selectedTab = getTimewindowType(value);
    if (model.selectedTab === TimewindowType.REALTIME) {
      if (isDefined(value.realtime.interval)) {
        model.realtime.interval = value.realtime.interval;
      }
      if (isUndefined(value.realtime.realtimeType)) {
        if (isDefined(value.realtime.quickInterval)) {
          model.realtime.realtimeType = RealtimeWindowType.INTERVAL;
        } else {
          model.realtime.realtimeType = RealtimeWindowType.LAST_INTERVAL;
        }
      } else {
        model.realtime.realtimeType = value.realtime.realtimeType;
      }
      if (model.realtime.realtimeType === RealtimeWindowType.INTERVAL) {
        model.realtime.quickInterval = value.realtime.quickInterval;
      } else {
        model.realtime.timewindowMs = value.realtime.timewindowMs;
      }
    } else {
      if (isDefined(value.history.interval)) {
        model.history.interval = value.history.interval;
      }
      if (isUndefined(value.history.historyType)) {
        if (isDefined(value.history.timewindowMs)) {
          model.history.historyType = HistoryWindowType.LAST_INTERVAL;
        } else if (isDefined(value.history.quickInterval)) {
          model.history.historyType = HistoryWindowType.INTERVAL;
        } else {
          model.history.historyType = HistoryWindowType.FIXED;
        }
      } else {
        model.history.historyType = value.history.historyType;
      }
      if (model.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        model.history.timewindowMs = value.history.timewindowMs;
      } else if (model.history.historyType === HistoryWindowType.INTERVAL) {
        model.history.quickInterval = value.history.quickInterval;
      } else {
        model.history.fixedTimewindow.startTimeMs = value.history.fixedTimewindow.startTimeMs;
        model.history.fixedTimewindow.endTimeMs = value.history.fixedTimewindow.endTimeMs;
      }
    }
    if (value.aggregation) {
      if (value.aggregation.type) {
        model.aggregation.type = value.aggregation.type;
      }
      model.aggregation.limit = value.aggregation.limit || Math.floor(timeService.getMaxDatapointsLimit() / 2);
    }
    model.timezone = value.timezone;
  }
  return model;
}

export function toHistoryTimewindow(timewindow: Timewindow, startTimeMs: number, endTimeMs: number,
                                    interval: number, timeService: TimeService): Timewindow {
  if (timewindow.history) {
    interval = isDefined(interval) ? interval : timewindow.history.interval;
  } else if (timewindow.realtime) {
    interval = timewindow.realtime.interval;
  }  else {
    interval = 0;
  }
  let aggType: AggregationType;
  let limit: number;
  if (timewindow.aggregation) {
    aggType = timewindow.aggregation.type || AggregationType.AVG;
    limit = timewindow.aggregation.limit || timeService.getMaxDatapointsLimit();
  } else {
    aggType = AggregationType.AVG;
    limit = timeService.getMaxDatapointsLimit();
  }
  return {
    hideInterval: timewindow.hideInterval || false,
    hideAggregation: timewindow.hideAggregation || false,
    hideAggInterval: timewindow.hideAggInterval || false,
    hideTimezone: timewindow.hideTimezone || false,
    selectedTab: TimewindowType.HISTORY,
    history: {
      historyType: HistoryWindowType.FIXED,
      fixedTimewindow: {
        startTimeMs,
        endTimeMs
      },
      interval: timeService.boundIntervalToTimewindow(endTimeMs - startTimeMs, interval, AggregationType.AVG)
    },
    aggregation: {
      type: aggType,
      limit
    },
    timezone: timewindow.timezone
  };
}

export function timewindowTypeChanged(newTimewindow: Timewindow, oldTimewindow: Timewindow): boolean {
  if (!newTimewindow || !oldTimewindow) {
    return false;
  }
  const newType = getTimewindowType(newTimewindow);
  const oldType = getTimewindowType(oldTimewindow);
  return newType !== oldType;
}

export function calculateTsOffset(timezone?: string): number {
  if (timezone) {
    const tz = getTimezone(timezone);
    const localOffset = moment().utcOffset();
    return (tz.utcOffset() - localOffset) * 60 * 1000;
  } else {
    return 0;
  }
}

export function isHistoryTypeTimewindow(timewindow: Timewindow): boolean {
  return getTimewindowType(timewindow) === TimewindowType.HISTORY;
}

export function createSubscriptionTimewindow(timewindow: Timewindow, stDiff: number, stateData: boolean,
                                             timeService: TimeService): SubscriptionTimewindow {
  const subscriptionTimewindow: SubscriptionTimewindow = {
    fixedWindow: null,
    realtimeWindowMs: null,
    aggregation: {
      interval: SECOND,
      limit: timeService.getMaxDatapointsLimit(),
      type: AggregationType.AVG
    },
    timezone: timewindow.timezone,
    tsOffset: calculateTsOffset(timewindow.timezone)
  };
  let aggTimewindow;
  if (stateData) {
    subscriptionTimewindow.aggregation.type = AggregationType.NONE;
    subscriptionTimewindow.aggregation.stateData = true;
  }
  if (isDefined(timewindow.aggregation) && !stateData) {
    subscriptionTimewindow.aggregation = {
      type: timewindow.aggregation.type || AggregationType.AVG,
      limit: timewindow.aggregation.limit || timeService.getMaxDatapointsLimit()
    };
  }
  const selectedTab = getTimewindowType(timewindow);
  if (selectedTab === TimewindowType.REALTIME) {
    let realtimeType = timewindow.realtime.realtimeType;
    if (isUndefined(realtimeType)) {
      if (isDefined(timewindow.realtime.quickInterval)) {
        realtimeType = RealtimeWindowType.INTERVAL;
      } else {
        realtimeType = RealtimeWindowType.LAST_INTERVAL;
      }
    }
    if (realtimeType === RealtimeWindowType.INTERVAL) {
      subscriptionTimewindow.realtimeWindowMs =
        getSubscriptionRealtimeWindowFromTimeInterval(timewindow.realtime.quickInterval, timewindow.timezone);
      subscriptionTimewindow.quickInterval = timewindow.realtime.quickInterval;
      const currentDate = getCurrentTime(timewindow.timezone);
      subscriptionTimewindow.startTs = calculateIntervalStartTime(timewindow.realtime.quickInterval, currentDate).valueOf();
    } else {
      subscriptionTimewindow.realtimeWindowMs = timewindow.realtime.timewindowMs;
      const currentDate = getCurrentTime(timewindow.timezone);
      subscriptionTimewindow.startTs = currentDate.valueOf() + stDiff - subscriptionTimewindow.realtimeWindowMs;
    }
    subscriptionTimewindow.aggregation.interval =
      timeService.boundIntervalToTimewindow(subscriptionTimewindow.realtimeWindowMs, timewindow.realtime.interval,
        subscriptionTimewindow.aggregation.type);
    aggTimewindow = subscriptionTimewindow.realtimeWindowMs;
    if (realtimeType !== RealtimeWindowType.INTERVAL) {
      const startDiff = subscriptionTimewindow.startTs % subscriptionTimewindow.aggregation.interval;
      if (startDiff) {
        subscriptionTimewindow.startTs -= startDiff;
        aggTimewindow += subscriptionTimewindow.aggregation.interval;
      }
    }
  } else {
    let historyType = timewindow.history.historyType;
    if (isUndefined(historyType)) {
      if (isDefined(timewindow.history.timewindowMs)) {
        historyType = HistoryWindowType.LAST_INTERVAL;
      } else if (isDefined(timewindow.history.quickInterval)) {
        historyType = HistoryWindowType.INTERVAL;
      } else {
        historyType = HistoryWindowType.FIXED;
      }
    }
    if (historyType === HistoryWindowType.LAST_INTERVAL) {
      const currentDate = getCurrentTime(timewindow.timezone);
      const currentTime = currentDate.valueOf();
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: currentTime - timewindow.history.timewindowMs,
        endTimeMs: currentTime
      };
      aggTimewindow = timewindow.history.timewindowMs;
    } else if (historyType === HistoryWindowType.INTERVAL) {
      const startEndTime = calculateIntervalStartEndTime(timewindow.history.quickInterval, timewindow.timezone);
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: startEndTime[0],
        endTimeMs: startEndTime[1]
      };
      aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
      subscriptionTimewindow.quickInterval = timewindow.history.quickInterval;
    } else {
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: timewindow.history.fixedTimewindow.startTimeMs - subscriptionTimewindow.tsOffset,
        endTimeMs: timewindow.history.fixedTimewindow.endTimeMs - subscriptionTimewindow.tsOffset
      };
      aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
    }
    subscriptionTimewindow.startTs = subscriptionTimewindow.fixedWindow.startTimeMs;
    subscriptionTimewindow.aggregation.interval =
      timeService.boundIntervalToTimewindow(aggTimewindow, timewindow.history.interval, subscriptionTimewindow.aggregation.type);
  }
  const aggregation = subscriptionTimewindow.aggregation;
  aggregation.timeWindow = aggTimewindow;
  if (aggregation.type !== AggregationType.NONE) {
    aggregation.limit = Math.ceil(aggTimewindow / subscriptionTimewindow.aggregation.interval);
  }
  return subscriptionTimewindow;
}

function getSubscriptionRealtimeWindowFromTimeInterval(interval: QuickTimeInterval, tz?: string): number {
  let currentDate;
  switch (interval) {
    case QuickTimeInterval.CURRENT_HOUR:
      return HOUR;
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return DAY;
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return WEEK;
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      currentDate = getCurrentTime(tz);
      return currentDate.endOf('month').diff(currentDate.clone().startOf('month'));
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      currentDate = getCurrentTime(tz);
      return currentDate.endOf('year').diff(currentDate.clone().startOf('year'));
  }
}

export function calculateIntervalStartEndTime(interval: QuickTimeInterval, tz?: string): [number, number] {
  const startEndTs: [number, number] = [0, 0];
  const currentDate = getCurrentTime(tz);
  const startDate = calculateIntervalStartTime(interval, currentDate);
  startEndTs[0] = startDate.valueOf();
  const endDate = calculateIntervalEndTime(interval, startDate, tz);
  startEndTs[1] = endDate.valueOf();
  return startEndTs;
}

export function calculateIntervalStartTime(interval: QuickTimeInterval, currentDate: moment_.Moment): moment_.Moment {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
      currentDate.subtract(1, 'days');
      return currentDate.startOf('day');
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
      currentDate.subtract(2, 'days');
      return currentDate.startOf('day');
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('day');
    case QuickTimeInterval.PREVIOUS_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('week');
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
      currentDate.subtract(1, 'weeks');
      return currentDate.startOf('isoWeek');
    case QuickTimeInterval.PREVIOUS_MONTH:
      currentDate.subtract(1, 'months');
      return currentDate.startOf('month');
    case QuickTimeInterval.PREVIOUS_YEAR:
      currentDate.subtract(1, 'years');
      return currentDate.startOf('year');
    case QuickTimeInterval.CURRENT_HOUR:
      return currentDate.startOf('hour');
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return currentDate.startOf('day');
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      return currentDate.startOf('week');
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return currentDate.startOf('isoWeek');
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return currentDate.startOf('month');
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return currentDate.startOf('year');
  }
}

export function calculateIntervalEndTime(interval: QuickTimeInterval, startDate: moment_.Moment, tz?: string): number {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
    case QuickTimeInterval.CURRENT_DAY:
      return startDate.add(1, 'day').valueOf();
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
      return startDate.add(1, 'week').valueOf();
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
      return startDate.add(1, 'month').valueOf();
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
      return startDate.add(1, 'year').valueOf();
    case QuickTimeInterval.CURRENT_HOUR:
      return startDate.add(1, 'hour').valueOf();
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return getCurrentTime(tz).valueOf();
  }
}

export function quickTimeIntervalPeriod(interval: QuickTimeInterval): number {
  switch (interval) {
    case QuickTimeInterval.CURRENT_HOUR:
      return HOUR;
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return DAY;
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return WEEK;
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return DAY * 30;
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return YEAR;
  }
}

export function calculateIntervalComparisonStartTime(interval: QuickTimeInterval,
                                                     startDate: moment_.Moment): moment_.Moment {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.CURRENT_DAY:
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      startDate.subtract(1, 'days');
      return startDate.startOf('day');
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('day');
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('week');
    case QuickTimeInterval.PREVIOUS_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      startDate.subtract(1, 'weeks');
      return startDate.startOf('isoWeek');
    case QuickTimeInterval.PREVIOUS_MONTH:
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      startDate.subtract(1, 'months');
      return startDate.startOf('month');
    case QuickTimeInterval.PREVIOUS_YEAR:
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      startDate.subtract(1, 'years');
      return startDate.startOf('year');
    case QuickTimeInterval.CURRENT_HOUR:
      startDate.subtract(1, 'hour');
      return startDate.startOf('hour');
  }
}

export function calculateIntervalComparisonEndTime(interval: QuickTimeInterval,
                                                   comparisonStartDate: moment_.Moment,
                                                   endDate: moment_.Moment): number {
  switch (interval) {
    case QuickTimeInterval.CURRENT_DAY_SO_FAR:
      return endDate.subtract(1, 'days').valueOf();
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_ISO_SO_FAR:
      return endDate.subtract(1, 'week').valueOf();
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return endDate.subtract(1, 'month').valueOf();
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return endDate.subtract(1, 'year').valueOf();
    default:
      return calculateIntervalEndTime(interval, comparisonStartDate);
  }
}

export function createTimewindowForComparison(subscriptionTimewindow: SubscriptionTimewindow,
                                              timeUnit: ComparisonDuration): SubscriptionTimewindow {
  const timewindowForComparison: SubscriptionTimewindow = {
    fixedWindow: null,
    realtimeWindowMs: null,
    aggregation: subscriptionTimewindow.aggregation,
    tsOffset: subscriptionTimewindow.tsOffset
  };

  if (subscriptionTimewindow.fixedWindow) {
    let startTimeMs;
    let endTimeMs;
    if (timeUnit === 'previousInterval') {
      if (subscriptionTimewindow.quickInterval) {
        const startDate = moment(subscriptionTimewindow.fixedWindow.startTimeMs);
        const endDate = moment(subscriptionTimewindow.fixedWindow.endTimeMs);
        if (subscriptionTimewindow.timezone) {
          startDate.tz(subscriptionTimewindow.timezone);
          endDate.tz(subscriptionTimewindow.timezone);
        }
        const comparisonStartDate = calculateIntervalComparisonStartTime(subscriptionTimewindow.quickInterval, startDate);
        startTimeMs = comparisonStartDate.valueOf();
        endTimeMs = calculateIntervalComparisonEndTime(subscriptionTimewindow.quickInterval, comparisonStartDate, endDate);
      } else {
        const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
        endTimeMs = subscriptionTimewindow.fixedWindow.startTimeMs;
        startTimeMs = endTimeMs - timeInterval;
      }
    } else {
      const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
      endTimeMs = moment(subscriptionTimewindow.fixedWindow.endTimeMs).subtract(1, timeUnit).valueOf();
      startTimeMs = endTimeMs - timeInterval;
    }
    timewindowForComparison.startTs = startTimeMs;
    timewindowForComparison.fixedWindow = {
      startTimeMs: timewindowForComparison.startTs,
      endTimeMs
    };
  }

  return timewindowForComparison;
}

export function cloneSelectedTimewindow(timewindow: Timewindow): Timewindow {
  const cloned: Timewindow = {};
  cloned.hideInterval = timewindow.hideInterval || false;
  cloned.hideAggregation = timewindow.hideAggregation || false;
  cloned.hideAggInterval = timewindow.hideAggInterval || false;
  cloned.hideTimezone = timewindow.hideTimezone || false;
  if (isDefined(timewindow.selectedTab)) {
    cloned.selectedTab = timewindow.selectedTab;
    if (timewindow.selectedTab === TimewindowType.REALTIME) {
      cloned.realtime = deepClone(timewindow.realtime);
    } else if (timewindow.selectedTab === TimewindowType.HISTORY) {
      cloned.history = deepClone(timewindow.history);
    }
  }
  cloned.aggregation = deepClone(timewindow.aggregation);
  cloned.timezone = timewindow.timezone;
  return cloned;
}

export interface TimeInterval {
  name: string;
  translateParams: {[key: string]: any};
  value: number;
}

export const defaultTimeIntervals = new Array<TimeInterval>(
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 1},
    value: SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 5},
    value: 5 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 10},
    value: 10 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 15},
    value: 15 * SECOND
  },
  {
    name: 'timeinterval.seconds-interval',
    translateParams: {seconds: 30},
    value: 30 * SECOND
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 1},
    value: MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 2},
    value: 2 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 5},
    value: 5 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 10},
    value: 10 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 15},
    value: 15 * MINUTE
  },
  {
    name: 'timeinterval.minutes-interval',
    translateParams: {minutes: 30},
    value: 30 * MINUTE
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 1},
    value: HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 2},
    value: 2 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 5},
    value: 5 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 10},
    value: 10 * HOUR
  },
  {
    name: 'timeinterval.hours-interval',
    translateParams: {hours: 12},
    value: 12 * HOUR
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 1},
    value: DAY
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 7},
    value: 7 * DAY
  },
  {
    name: 'timeinterval.days-interval',
    translateParams: {days: 30},
    value: 30 * DAY
  }
);

export enum TimeUnit {
  SECONDS = 'SECONDS',
  MINUTES = 'MINUTES',
  HOURS = 'HOURS',
  DAYS = 'DAYS'
}

export const timeUnitTranslationMap = new Map<TimeUnit, string>(
  [
    [TimeUnit.SECONDS, 'timeunit.seconds'],
    [TimeUnit.MINUTES, 'timeunit.minutes'],
    [TimeUnit.HOURS, 'timeunit.hours'],
    [TimeUnit.DAYS, 'timeunit.days']
  ]
);

export interface TimezoneInfo {
  id: string;
  name: string;
  offset: string;
  nOffset: number;
  abbr: string;
}

let timezones: TimezoneInfo[] = null;
let defaultTimezone: string = null;

export function getTimezones(): TimezoneInfo[] {
  if (!timezones) {
    timezones = momentTz.tz.names().map((zoneName) => {
      const tz = momentTz.tz(zoneName);
      return {
        id: zoneName,
        name: zoneName.replace(/_/g, ' '),
        offset: `UTC${tz.format('Z')}`,
        nOffset: tz.utcOffset(),
        abbr: tz.zoneAbbr()
      };
    });
  }
  return timezones;
}

export function getTimezoneInfo(timezoneId: string, defaultTimezoneId?: string, userTimezoneByDefault?: boolean): TimezoneInfo {
  const timezoneList = getTimezones();
  let foundTimezone = timezoneId ? timezoneList.find(timezoneInfo => timezoneInfo.id === timezoneId) : null;
  if (!foundTimezone) {
    if (userTimezoneByDefault) {
      const userTimezone = getDefaultTimezone();
      foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === userTimezone);
    } else if (defaultTimezoneId) {
      foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === defaultTimezoneId);
    }
  }
  return foundTimezone;
}

export function getDefaultTimezoneInfo(): TimezoneInfo {
  const userTimezone = getDefaultTimezone();
  return getTimezoneInfo(userTimezone);
}

export function getDefaultTimezone(): string {
  if (!defaultTimezone) {
    defaultTimezone = momentTz.tz.guess();
  }
  return defaultTimezone;
}

export function getCurrentTime(tz?: string): moment_.Moment {
  if (tz) {
    return moment().tz(tz);
  } else {
    return moment();
  }
}

export function getTime(ts: number, tz?: string): moment_.Moment {
  if (tz) {
    return moment(ts).tz(tz);
  } else {
    return moment(ts);
  }
}

export function getTimezone(tz: string): moment_.Moment {
    return moment.tz(tz);
}

export function getCurrentTimeForComparison(timeForComparison: moment_.unitOfTime.DurationConstructor, tz?: string): moment_.Moment {
  return getCurrentTime(tz).subtract(1, timeForComparison);
}
