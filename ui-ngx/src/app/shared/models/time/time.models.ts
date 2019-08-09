///
/// Copyright © 2016-2019 The Thingsboard Authors
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

export const SECOND = 1000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;

export enum TimewindowType {
  REALTIME,
  HISTORY
}

export enum HistoryWindowType {
  LAST_INTERVAL,
  FIXED
}

export class Timewindow {

  displayValue?: string;
  selectedTab?: TimewindowType;
  realtime?: IntervalWindow;
  history?: HistoryWindow;
  aggregation?: Aggregation;

  public static historyInterval(timewindowMs: number): Timewindow {
    const timewindow = new Timewindow();
    timewindow.history = new HistoryWindow();
    timewindow.history.timewindowMs = timewindowMs;
    return timewindow;
  }

  public static defaultTimewindow(timeService: TimeService): Timewindow {
    const currentTime = new Date().getTime();
    const timewindow = new Timewindow();
    timewindow.displayValue = '';
    timewindow.selectedTab = TimewindowType.REALTIME;
    timewindow.realtime = new IntervalWindow();
    timewindow.realtime.interval = SECOND;
    timewindow.realtime.timewindowMs = MINUTE;
    timewindow.history = new HistoryWindow();
    timewindow.history.historyType = HistoryWindowType.LAST_INTERVAL;
    timewindow.history.interval = SECOND;
    timewindow.history.timewindowMs = MINUTE;
    timewindow.history.fixedTimewindow = new FixedWindow();
    timewindow.history.fixedTimewindow.startTimeMs = currentTime - DAY;
    timewindow.history.fixedTimewindow.endTimeMs = currentTime;
    timewindow.aggregation = new Aggregation();
    timewindow.aggregation.type = AggregationType.AVG;
    timewindow.aggregation.limit = Math.floor(timeService.getMaxDatapointsLimit() / 2);
    return timewindow;
  }

  public static initModelFromDefaultTimewindow(value: Timewindow, timeService: TimeService): Timewindow {
    const model = Timewindow.defaultTimewindow(timeService);
    if (value) {
      if (value.realtime) {
        model.selectedTab = TimewindowType.REALTIME;
        if (typeof value.realtime.interval !== 'undefined') {
          model.realtime.interval = value.realtime.interval;
        }
        model.realtime.timewindowMs = value.realtime.timewindowMs;
      } else {
        model.selectedTab = TimewindowType.HISTORY;
        if (typeof value.history.interval !== 'undefined') {
          model.history.interval = value.history.interval;
        }
        if (typeof value.history.timewindowMs !== 'undefined') {
          model.history.historyType = HistoryWindowType.LAST_INTERVAL;
          model.history.timewindowMs = value.history.timewindowMs;
        } else {
          model.history.historyType = HistoryWindowType.FIXED;
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

  public clone(): Timewindow {
    const cloned = new Timewindow();
    cloned.displayValue = this.displayValue;
    cloned.selectedTab = this.selectedTab;
    cloned.realtime = this.realtime ? this.realtime.clone() : null;
    cloned.history = this.history ? this.history.clone() : null;
    cloned.aggregation = this.aggregation ? this.aggregation.clone() : null;
    return cloned;
  }

  public cloneSelectedTimewindow(): Timewindow {
    const cloned = new Timewindow();
    if (typeof this.selectedTab !== 'undefined') {
      if (this.selectedTab === TimewindowType.REALTIME) {
        cloned.realtime = this.realtime ? this.realtime.clone() : null;
      } else if (this.selectedTab === TimewindowType.HISTORY) {
        cloned.history = this.history ? this.history.cloneSelectedTimewindow() : null;
      }
    }
    cloned.aggregation = this.aggregation ? this.aggregation.clone() : null;
    return cloned;
  }

}

export class IntervalWindow {
  interval?: number;
  timewindowMs?: number;

  public clone(): IntervalWindow {
    const cloned = new IntervalWindow();
    cloned.interval = this.interval;
    cloned.timewindowMs = this.timewindowMs;
    return cloned;
  }
}

export class FixedWindow {
  startTimeMs: number;
  endTimeMs: number;

  public clone(): FixedWindow {
    const cloned = new FixedWindow();
    cloned.startTimeMs = this.startTimeMs;
    cloned.endTimeMs = this.endTimeMs;
    return cloned;
  }
}

export class HistoryWindow extends IntervalWindow {
  historyType?: HistoryWindowType;
  fixedTimewindow?: FixedWindow;

  public clone(): HistoryWindow {
    const cloned = new HistoryWindow();
    cloned.historyType = this.historyType;
    if (this.fixedTimewindow) {
      cloned.fixedTimewindow = this.fixedTimewindow.clone();
    }
    cloned.interval = this.interval;
    cloned.timewindowMs = this.timewindowMs;
    return cloned;
  }

  public cloneSelectedTimewindow(): HistoryWindow {
    const cloned = new HistoryWindow();
    if (typeof this.historyType !== 'undefined') {
      cloned.interval = this.interval;
      if (this.historyType === HistoryWindowType.LAST_INTERVAL) {
        cloned.timewindowMs = this.timewindowMs;
      } else if (this.historyType === HistoryWindowType.FIXED) {
        cloned.fixedTimewindow = this.fixedTimewindow ? this.fixedTimewindow.clone() : null;
      }
    }
    return cloned;
  }
}

export class Aggregation {
  type: AggregationType;
  limit: number;

  public clone(): Aggregation {
    const cloned = new Aggregation();
    cloned.type = this.type;
    cloned.limit = this.limit;
    return cloned;
  }
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
