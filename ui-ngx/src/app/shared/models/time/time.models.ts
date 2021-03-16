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
import { deepClone, isDefined, isDefinedAndNotNull, isUndefined } from '@app/core/utils';
import * as moment_ from 'moment';
import { Observable } from 'rxjs/internal/Observable';
import { from, of } from 'rxjs';
import { map, mergeMap, tap } from 'rxjs/operators';

const moment = moment_;

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;
export const WEEK = 7 * DAY;
export const YEAR = DAY * 365;

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
  hideInterval?: boolean;
  hideAggregation?: boolean;
  hideAggInterval?: boolean;
  selectedTab?: TimewindowType;
  realtime?: RealtimeWindow;
  history?: HistoryWindow;
  aggregation?: Aggregation;
}

export interface SubscriptionAggregation extends Aggregation {
  interval?: number;
  timeWindow?: number;
  stateData?: boolean;
}

export interface SubscriptionTimewindow {
  startTs?: number;
  quickInterval?: QuickTimeInterval;
  realtimeWindowMs?: number;
  fixedWindow?: FixedWindow;
  aggregation?: SubscriptionAggregation;
}

export interface WidgetTimewindow {
  minTime?: number;
  maxTime?: number;
  interval?: number;
  stDiff?: number;
}

export enum QuickTimeInterval {
  YESTERDAY = 'YESTERDAY',
  DAY_BEFORE_YESTERDAY = 'DAY_BEFORE_YESTERDAY',
  THIS_DAY_LAST_WEEK = 'THIS_DAY_LAST_WEEK',
  PREVIOUS_WEEK = 'PREVIOUS_WEEK',
  PREVIOUS_MONTH = 'PREVIOUS_MONTH',
  PREVIOUS_YEAR = 'PREVIOUS_YEAR',
  TODAY = 'TODAY',
  TODAY_SO_FAR = 'TODAY_SO_FAR',
  CURRENT_WEEK = 'CURRENT_WEEK',
  CURRENT_WEEK_SO_FAR = 'CURRENT_WEEK_SO_WAR',
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
  [QuickTimeInterval.PREVIOUS_MONTH, 'timeinterval.predefined.previous-month'],
  [QuickTimeInterval.PREVIOUS_YEAR, 'timeinterval.predefined.previous-year'],
  [QuickTimeInterval.TODAY, 'timeinterval.predefined.today'],
  [QuickTimeInterval.TODAY_SO_FAR, 'timeinterval.predefined.today-so-far'],
  [QuickTimeInterval.CURRENT_WEEK, 'timeinterval.predefined.current-week'],
  [QuickTimeInterval.CURRENT_WEEK_SO_FAR, 'timeinterval.predefined.current-week-so-far'],
  [QuickTimeInterval.CURRENT_MONTH, 'timeinterval.predefined.current-month'],
  [QuickTimeInterval.CURRENT_MONTH_SO_FAR, 'timeinterval.predefined.current-month-so-far'],
  [QuickTimeInterval.CURRENT_YEAR, 'timeinterval.predefined.current-year'],
  [QuickTimeInterval.CURRENT_YEAR_SO_FAR, 'timeinterval.predefined.current-year-so-far']
]);

export function historyInterval(timewindowMs: number): Timewindow {
  const timewindow: Timewindow = {
    selectedTab: TimewindowType.HISTORY,
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      timewindowMs
    }
  };
  return timewindow;
}

export function defaultTimewindow(timeService: TimeService): Timewindow {
  const currentTime = moment().valueOf();
  const timewindow: Timewindow = {
    displayValue: '',
    hideInterval: false,
    hideAggregation: false,
    hideAggInterval: false,
    selectedTab: TimewindowType.REALTIME,
    realtime: {
      realtimeType: RealtimeWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      quickInterval: QuickTimeInterval.TODAY
    },
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      fixedTimewindow: {
        startTimeMs: currentTime - DAY,
        endTimeMs: currentTime
      },
      quickInterval: QuickTimeInterval.TODAY
    },
    aggregation: {
      type: AggregationType.AVG,
      limit: Math.floor(timeService.getMaxDatapointsLimit() / 2)
    }
  };
  return timewindow;
}

export function initModelFromDefaultTimewindow(value: Timewindow, timeService: TimeService): Timewindow {
  const model = defaultTimewindow(timeService);
  if (value) {
    model.hideInterval = value.hideInterval;
    model.hideAggregation = value.hideAggregation;
    model.hideAggInterval = value.hideAggInterval;
    if (isUndefined(value.selectedTab)) {
      if (value.realtime) {
        model.selectedTab = TimewindowType.REALTIME;
      } else {
        model.selectedTab = TimewindowType.HISTORY;
      }
    } else {
      model.selectedTab = value.selectedTab;
    }
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
  const historyTimewindow: Timewindow = {
    hideInterval: timewindow.hideInterval || false,
    hideAggregation: timewindow.hideAggregation || false,
    hideAggInterval: timewindow.hideAggInterval || false,
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
    }
  };
  return historyTimewindow;
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
    }
  };
  let aggTimewindow = 0;
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
  let selectedTab = timewindow.selectedTab;
  if (isUndefined(selectedTab)) {
    selectedTab = isDefined(timewindow.realtime) ? TimewindowType.REALTIME : TimewindowType.HISTORY;
  }
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
      subscriptionTimewindow.realtimeWindowMs = getSubscriptionRealtimeWindowFromTimeInterval(timewindow.realtime.quickInterval);
      subscriptionTimewindow.quickInterval = timewindow.realtime.quickInterval;
    } else {
      subscriptionTimewindow.realtimeWindowMs = timewindow.realtime.timewindowMs;
    }
    subscriptionTimewindow.aggregation.interval =
      timeService.boundIntervalToTimewindow(subscriptionTimewindow.realtimeWindowMs, timewindow.realtime.interval,
        subscriptionTimewindow.aggregation.type);
    subscriptionTimewindow.startTs = Date.now() + stDiff - subscriptionTimewindow.realtimeWindowMs;
    const startDiff = subscriptionTimewindow.startTs % subscriptionTimewindow.aggregation.interval;
    aggTimewindow = subscriptionTimewindow.realtimeWindowMs;
    if (startDiff && realtimeType !== RealtimeWindowType.INTERVAL) {
      subscriptionTimewindow.startTs -= startDiff;
      aggTimewindow += subscriptionTimewindow.aggregation.interval;
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
      const currentTime = Date.now();
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: currentTime - timewindow.history.timewindowMs,
        endTimeMs: currentTime
      };
      aggTimewindow = timewindow.history.timewindowMs;
    } else if (historyType === HistoryWindowType.INTERVAL) {
      const currentDate = moment();
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: calculateIntervalStartTime(timewindow.history.quickInterval, null, currentDate),
        endTimeMs: calculateIntervalEndTime(timewindow.history.quickInterval, null, currentDate)
      };
      aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
    } else {
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: timewindow.history.fixedTimewindow.startTimeMs,
        endTimeMs: timewindow.history.fixedTimewindow.endTimeMs
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

function getSubscriptionRealtimeWindowFromTimeInterval(interval: QuickTimeInterval): number {
  const currentDate = moment();
  switch (interval) {
    case QuickTimeInterval.TODAY:
    case QuickTimeInterval.TODAY_SO_FAR:
      return currentDate.diff(currentDate.clone().startOf('day'));
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      return currentDate.diff(currentDate.clone().startOf('week'));
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return currentDate.diff(currentDate.clone().startOf('month'));
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return currentDate.diff(currentDate.clone().startOf('year'));
  }
}

export function calculateIntervalEndTime(interval: QuickTimeInterval, endTs = 0, nowDate?: moment_.Moment): number {
  const currentDate = isDefinedAndNotNull(nowDate) ? nowDate.clone() : moment();
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
      currentDate.subtract(1, 'days');
      return currentDate.endOf('day').valueOf();
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
      currentDate.subtract(2, 'days');
      return currentDate.endOf('day').valueOf();
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.endOf('day').valueOf();
    case QuickTimeInterval.PREVIOUS_WEEK:
      currentDate.subtract(1, 'weeks');
      return currentDate.endOf('week').valueOf();
    case QuickTimeInterval.PREVIOUS_MONTH:
      currentDate.subtract(1, 'months');
      return currentDate.endOf('month').valueOf();
    case QuickTimeInterval.PREVIOUS_YEAR:
      currentDate.subtract(1, 'years');
      return currentDate.endOf('year').valueOf();
    case QuickTimeInterval.TODAY:
      return currentDate.endOf('day').valueOf();
    case QuickTimeInterval.CURRENT_WEEK:
      return currentDate.endOf('week').valueOf();
    case QuickTimeInterval.CURRENT_MONTH:
      return currentDate.endOf('month').valueOf();
    case QuickTimeInterval.CURRENT_YEAR:
      return currentDate.endOf('year').valueOf();
    case QuickTimeInterval.TODAY_SO_FAR:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return currentDate.valueOf();
    default:
      return endTs;
  }
}

export function calculateIntervalStartTime(interval: QuickTimeInterval, startTS = 0, nowDate?: moment_.Moment): number {
  const currentDate = isDefinedAndNotNull(nowDate) ? nowDate.clone() : moment();
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
      currentDate.subtract(1, 'days');
      return  currentDate.startOf('day').valueOf();
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
      currentDate.subtract(2, 'days');
      return currentDate.startOf('day').valueOf();
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
      currentDate.subtract(1, 'weeks');
      return  currentDate.startOf('day').valueOf();
    case QuickTimeInterval.PREVIOUS_WEEK:
      currentDate.subtract(1, 'weeks');
      return  currentDate.startOf('week').valueOf();
    case QuickTimeInterval.PREVIOUS_MONTH:
      currentDate.subtract(1, 'months');
      return  currentDate.startOf('month').valueOf();
    case QuickTimeInterval.PREVIOUS_YEAR:
      currentDate.subtract(1, 'years');
      return  currentDate.startOf('year').valueOf();
    case QuickTimeInterval.TODAY:
    case QuickTimeInterval.TODAY_SO_FAR:
      return currentDate.startOf('day').valueOf();
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
      return currentDate.startOf('week').valueOf();
    case QuickTimeInterval.CURRENT_MONTH:
    case QuickTimeInterval.CURRENT_MONTH_SO_FAR:
      return currentDate.startOf('month').valueOf();
    case QuickTimeInterval.CURRENT_YEAR:
    case QuickTimeInterval.CURRENT_YEAR_SO_FAR:
      return currentDate.startOf('year').valueOf();
    default:
      return startTS;
  }
}

export function quickTimeIntervalPeriod(interval: QuickTimeInterval): number {
  switch (interval) {
    case QuickTimeInterval.YESTERDAY:
    case QuickTimeInterval.DAY_BEFORE_YESTERDAY:
    case QuickTimeInterval.THIS_DAY_LAST_WEEK:
    case QuickTimeInterval.TODAY:
    case QuickTimeInterval.TODAY_SO_FAR:
      return DAY;
    case QuickTimeInterval.PREVIOUS_WEEK:
    case QuickTimeInterval.CURRENT_WEEK:
    case QuickTimeInterval.CURRENT_WEEK_SO_FAR:
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

export function createTimewindowForComparison(subscriptionTimewindow: SubscriptionTimewindow,
                                              timeUnit: moment_.unitOfTime.DurationConstructor): SubscriptionTimewindow {
  const timewindowForComparison: SubscriptionTimewindow = {
    fixedWindow: null,
    realtimeWindowMs: null,
    aggregation: subscriptionTimewindow.aggregation
  };

  if (subscriptionTimewindow.realtimeWindowMs) {
    timewindowForComparison.startTs = moment(subscriptionTimewindow.startTs).subtract(1, timeUnit).valueOf();
    timewindowForComparison.realtimeWindowMs = subscriptionTimewindow.realtimeWindowMs;
  } else if (subscriptionTimewindow.fixedWindow) {
    const timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
    const endTimeMs = moment(subscriptionTimewindow.fixedWindow.endTimeMs).subtract(1, timeUnit).valueOf();

    timewindowForComparison.startTs = endTimeMs - timeInterval;
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
  if (isDefined(timewindow.selectedTab)) {
    cloned.selectedTab = timewindow.selectedTab;
    if (timewindow.selectedTab === TimewindowType.REALTIME) {
      cloned.realtime = deepClone(timewindow.realtime);
    } else if (timewindow.selectedTab === TimewindowType.HISTORY) {
      cloned.history = deepClone(timewindow.history);
    }
  }
  cloned.aggregation = deepClone(timewindow.aggregation);
  return cloned;
}

export function cloneSelectedHistoryTimewindow(historyWindow: HistoryWindow): HistoryWindow {
  const cloned: HistoryWindow = {};
  if (isDefined(historyWindow.historyType)) {
    cloned.historyType = historyWindow.historyType;
    cloned.interval = historyWindow.interval;
    if (historyWindow.historyType === HistoryWindowType.LAST_INTERVAL) {
      cloned.timewindowMs = historyWindow.timewindowMs;
    } else if (historyWindow.historyType === HistoryWindowType.INTERVAL) {
      cloned.quickInterval = historyWindow.quickInterval;
    } else if (historyWindow.historyType === HistoryWindowType.FIXED) {
      cloned.fixedTimewindow = deepClone(historyWindow.fixedTimewindow);
    }
  }
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
}

let timezones: TimezoneInfo[] = null;
let defaultTimezone: string = null;

export function getTimezones(): Observable<TimezoneInfo[]> {
  if (timezones) {
    return of(timezones);
  } else {
    return from(import('moment-timezone')).pipe(
      map((monentTz) => {
        return monentTz.tz.names().map((zoneName) => {
          const tz = monentTz.tz(zoneName);
          return {
            id: zoneName,
            name: zoneName.replace(/_/g, ' '),
            offset: `UTC${tz.format('Z')}`,
            nOffset: tz.utcOffset()
          };
        });
      }),
      tap((zones) => {
        timezones = zones;
      })
    );
  }
}

export function getTimezoneInfo(timezoneId: string, defaultTimezoneId?: string, userTimezoneByDefault?: boolean): Observable<TimezoneInfo> {
  return getTimezones().pipe(
    mergeMap((timezoneList) => {
      let foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === timezoneId);
      if (!foundTimezone) {
        if (userTimezoneByDefault) {
          return getDefaultTimezone().pipe(
            map((userTimezone) => {
              return timezoneList.find(timezoneInfo => timezoneInfo.id === userTimezone);
            })
          );
        } else if (defaultTimezoneId) {
          foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === defaultTimezoneId);
        }
      }
      return of(foundTimezone);
    })
  );
}

export function getDefaultTimezone(): Observable<string> {
  if (defaultTimezone) {
    return of(defaultTimezone);
  } else {
    return from(import('moment-timezone')).pipe(
      map((monentTz) => {
        return monentTz.tz.guess();
      }),
      tap((zone) => {
        defaultTimezone = zone;
      })
    );
  }
}
