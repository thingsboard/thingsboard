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

import { TimeService } from '@core/services/time.service';
import { deepClone, isDefined, isUndefined } from '@app/core/utils';
import * as moment_ from 'moment';
import { Observable } from 'rxjs/internal/Observable';
import { from, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';

const moment = moment_;

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;
export const YEAR = DAY * 365;

export enum TimewindowType {
  REALTIME,
  HISTORY
}

export enum HistoryWindowType {
  LAST_INTERVAL,
  FIXED
}

export interface IntervalWindow {
  interval?: number;
  timewindowMs?: number;
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
  realtime?: IntervalWindow;
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
      interval: SECOND,
      timewindowMs: MINUTE
    },
    history: {
      historyType: HistoryWindowType.LAST_INTERVAL,
      interval: SECOND,
      timewindowMs: MINUTE,
      fixedTimewindow: {
        startTimeMs: currentTime - DAY,
        endTimeMs: currentTime
      }
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
      model.realtime.timewindowMs = value.realtime.timewindowMs;
    } else {
      if (isDefined(value.history.interval)) {
        model.history.interval = value.history.interval;
      }
      if (isUndefined(value.history.historyType)) {
        if (isDefined(value.history.timewindowMs)) {
          model.history.historyType = HistoryWindowType.LAST_INTERVAL;
        } else {
          model.history.historyType = HistoryWindowType.FIXED;
        }
      } else {
        model.history.historyType = value.history.historyType;
      }
      if (model.history.historyType === HistoryWindowType.LAST_INTERVAL) {
        model.history.timewindowMs = value.history.timewindowMs;
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
    subscriptionTimewindow.realtimeWindowMs = timewindow.realtime.timewindowMs;
    subscriptionTimewindow.aggregation.interval =
      timeService.boundIntervalToTimewindow(subscriptionTimewindow.realtimeWindowMs, timewindow.realtime.interval,
        subscriptionTimewindow.aggregation.type);
    subscriptionTimewindow.startTs = Date.now() + stDiff - subscriptionTimewindow.realtimeWindowMs;
    const startDiff = subscriptionTimewindow.startTs % subscriptionTimewindow.aggregation.interval;
    aggTimewindow = subscriptionTimewindow.realtimeWindowMs;
    if (startDiff) {
      subscriptionTimewindow.startTs -= startDiff;
      aggTimewindow += subscriptionTimewindow.aggregation.interval;
    }
  } else {
    let historyType = timewindow.history.historyType;
    if (isUndefined(historyType)) {
      historyType = isDefined(timewindow.history.timewindowMs) ? HistoryWindowType.LAST_INTERVAL : HistoryWindowType.FIXED;
    }
    if (historyType === HistoryWindowType.LAST_INTERVAL) {
      const currentTime = Date.now();
      subscriptionTimewindow.fixedWindow = {
        startTimeMs: currentTime - timewindow.history.timewindowMs,
        endTimeMs: currentTime
      };
      aggTimewindow = timewindow.history.timewindowMs;
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
    value: 1 * SECOND
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
    value: 1 * MINUTE
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
    value: 1 * HOUR
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
    value: 1 * DAY
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

export function getTimezoneInfo(timezoneId: string, defaultTimezoneId?: string): Observable<TimezoneInfo> {
  return getTimezones().pipe(
    map((timezoneList) => {
      let foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === timezoneId);
      if (!foundTimezone && defaultTimezoneId) {
        foundTimezone = timezoneList.find(timezoneInfo => timezoneInfo.id === defaultTimezoneId);
      }
      return foundTimezone;
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
