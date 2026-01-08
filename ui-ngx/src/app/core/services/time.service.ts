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

import { Injectable } from '@angular/core';
import {
  AggregationType,
  DAY,
  defaultTimeIntervals,
  defaultTimewindow,
  getDefaultTimezoneInfo,
  Interval,
  IntervalMath,
  SECOND,
  TimeInterval,
  Timewindow,
  TimezoneInfo
} from '@shared/models/time/time.models';
import { HttpClient } from '@angular/common/http';
import { deepClone, isDefined } from '@core/utils';
import { TranslateService } from '@ngx-translate/core';

const MIN_INTERVAL = SECOND;
const MAX_INTERVAL = 365 * 20 * DAY;

const MIN_LIMIT = 1;

const MAX_DATAPOINTS_LIMIT = 500;

@Injectable({
  providedIn: 'root'
})
export class TimeService {

  private maxDatapointsLimit = MAX_DATAPOINTS_LIMIT;

  private localBrowserTimezoneInfoPlaceholder: TimezoneInfo;

  constructor(
    private http: HttpClient,
    private translate: TranslateService
  ) {}

  public setMaxDatapointsLimit(limit: number) {
    this.maxDatapointsLimit = limit;
    if (!this.maxDatapointsLimit || this.maxDatapointsLimit <= MIN_LIMIT) {
      this.maxDatapointsLimit = MIN_LIMIT + 1;
    }
  }

  public matchesExistingInterval(min: number, max: number, interval: Interval, useCalendarIntervals = false): boolean {
    const intervals = this.getIntervals(min, max, useCalendarIntervals);
    return intervals.findIndex(timeInterval => timeInterval.value === interval) > -1;
  }

  public getIntervals(min: number, max: number, useCalendarIntervals = false): Array<TimeInterval> {
    min = this.boundMinInterval(min);
    max = this.boundMaxInterval(max);
    return defaultTimeIntervals.filter((interval) => (useCalendarIntervals || typeof interval.value === 'number') &&
      IntervalMath.numberValue(interval.value) >= min && IntervalMath.numberValue(interval.value) <= max);
  }

  public boundMinInterval(min: number): number {
    if (isDefined(min)) {
      min = Math.ceil(min / 1000) * 1000;
    }
    return this.toBound(min, MIN_INTERVAL, MAX_INTERVAL, MIN_INTERVAL);
  }

  public boundMaxInterval(max: number): number {
    if (isDefined(max)) {
      max = Math.floor(max / 1000) * 1000;
    }
    return this.toBound(max, MIN_INTERVAL, MAX_INTERVAL, MAX_INTERVAL);
  }

  public boundToPredefinedInterval(min: number, max: number, interval: Interval, useCalendarIntervals = false): Interval {
    const intervals = this.getIntervals(min, max, useCalendarIntervals);
    let minDelta = MAX_INTERVAL;
    const boundedInterval = interval || min;
    if (!intervals.length) {
      return boundedInterval;
    }
    const found = intervals.find(timeInterval => timeInterval.value === boundedInterval);
    if (found) {
      return found.value;
    } else {
      let matchedInterval: TimeInterval = intervals[0];
      intervals.forEach((timeInterval) => {
        const delta = Math.abs(IntervalMath.numberValue(timeInterval.value) - IntervalMath.numberValue(boundedInterval));
        if (delta <= minDelta) {
          matchedInterval = timeInterval;
          minDelta = delta;
        }
      });
      return matchedInterval.value;
    }
  }

  public boundIntervalToTimewindow(timewindow: number, interval: Interval, aggType: AggregationType): Interval {
    if (aggType === AggregationType.NONE) {
      return SECOND;
    } else {
      const min = this.minIntervalLimit(timewindow);
      const max = this.maxIntervalLimit(timewindow);
      if (interval) {
        return this.toIntervalBound(interval, min, max, interval);
      } else {
        return this.boundToPredefinedInterval(min, max, this.avgInterval(timewindow));
      }
    }
  }

  public getMaxDatapointsLimit(): number {
    return this.maxDatapointsLimit;
  }

  public getMinDatapointsLimit(): number {
    return MIN_LIMIT;
  }

  public avgInterval(timewindow: number): number {
    const avg = timewindow / 200;
    return this.boundMinInterval(avg);
  }

  public minIntervalLimit(timewindowMs: number): number {
    const min = timewindowMs / 500;
    return this.boundMinInterval(min);
  }

  public maxIntervalLimit(timewindowMs: number): number {
    const max = timewindowMs / MIN_LIMIT;
    return this.boundMaxInterval(max);
  }

  public defaultTimewindow(isDashboard = false): Timewindow {
    return defaultTimewindow(this, isDashboard);
  }

  private toBound(value: number, min: number, max: number, defValue: number): number {
    if (isDefined(value)) {
      value = Math.max(value, min);
      value = Math.min(value, max);
      return value;
    } else {
      return defValue;
    }
  }

  private toIntervalBound(value: Interval, min: number, max: number, defValue: Interval): Interval {
    if (isDefined(value)) {
      value = IntervalMath.max(value, min);
      value = IntervalMath.min(value, max);
      return value;
    } else {
      return defValue;
    }
  }

  public getLocalBrowserTimezoneInfoPlaceholder(): TimezoneInfo {
    if (!this.localBrowserTimezoneInfoPlaceholder) {
      this.localBrowserTimezoneInfoPlaceholder = deepClone(getDefaultTimezoneInfo());
      this.localBrowserTimezoneInfoPlaceholder.id = null;
      this.localBrowserTimezoneInfoPlaceholder.name = this.translate.instant('timezone.browser-time');
    }
    return this.localBrowserTimezoneInfoPlaceholder;
  }
}
