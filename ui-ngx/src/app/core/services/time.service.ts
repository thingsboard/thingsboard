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

import { Injectable } from '@angular/core';
import {
  AggregationType,
  DAY,
  defaultTimeIntervals,
  defaultTimewindow,
  SECOND,
  Timewindow
} from '@shared/models/time/time.models';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptions } from '@core/http/http-utils';
import { map } from 'rxjs/operators';
import { isDefined } from '@core/utils';

export interface TimeInterval {
  name: string;
  translateParams: { [key: string]: any };
  value: number;
}

const MIN_INTERVAL = SECOND;
const MAX_INTERVAL = 365 * 20 * DAY;

const MIN_LIMIT = 7;

const MAX_DATAPOINTS_LIMIT = 500;

@Injectable({
  providedIn: 'root'
})
export class TimeService {

  private maxDatapointsLimit = MAX_DATAPOINTS_LIMIT;

  constructor(
    private http: HttpClient
  ) {
  }

  public loadMaxDatapointsLimit(): Observable<number> {
    return this.http.get<number>('/api/dashboard/maxDatapointsLimit',
      defaultHttpOptions(true)).pipe(
      map((limit) => {
        this.maxDatapointsLimit = limit;
        if (!this.maxDatapointsLimit || this.maxDatapointsLimit <= MIN_LIMIT) {
          this.maxDatapointsLimit = MIN_LIMIT + 1;
        }
        return this.maxDatapointsLimit;
      })
    );
  }

  public matchesExistingInterval(min: number, max: number, intervalMs: number): boolean {
    const intervals = this.getIntervals(min, max);
    return intervals.findIndex(interval => interval.value === intervalMs) > -1;
  }

  public getIntervals(min: number, max: number): Array<TimeInterval> {
    min = this.boundMinInterval(min);
    max = this.boundMaxInterval(max);
    return defaultTimeIntervals.filter((interval) => interval.value >= min && interval.value <= max);
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

  public boundToPredefinedInterval(min: number, max: number, intervalMs: number): number {
    const intervals = this.getIntervals(min, max);
    let minDelta = MAX_INTERVAL;
    const boundedInterval = intervalMs || min;
    if (!intervals.length) {
      return boundedInterval;
    }
    let matchedInterval: TimeInterval = intervals[0];
    intervals.forEach((interval) => {
      const delta = Math.abs(interval.value - boundedInterval);
      if (delta < minDelta) {
        matchedInterval = interval;
        minDelta = delta;
      }
    });
    return matchedInterval.value;
  }

  public boundIntervalToTimewindow(timewindow: number, intervalMs: number, aggType: AggregationType): number {
    if (aggType === AggregationType.NONE) {
      return SECOND;
    } else {
      const min = this.minIntervalLimit(timewindow);
      const max = this.maxIntervalLimit(timewindow);
      if (intervalMs) {
        return this.toBound(intervalMs, min, max, intervalMs);
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

  public defaultTimewindow(): Timewindow {
    return defaultTimewindow(this);
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

}
