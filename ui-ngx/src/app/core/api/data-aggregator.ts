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

import { SubscriptionData, SubscriptionDataHolder } from '@app/shared/models/telemetry/telemetry.models';
import {
  AggregationType,
  calculateIntervalEndTime,
  calculateIntervalStartTime, getCurrentTime,
  QuickTimeInterval, SubscriptionTimewindow
} from '@shared/models/time/time.models';
import { UtilsService } from '@core/services/utils.service';
import { deepClone } from '@core/utils';
import Timeout = NodeJS.Timeout;

export declare type onAggregatedData = (data: SubscriptionData, detectChanges: boolean) => void;

interface AggData {
  count: number;
  sum: number;
  aggValue: any;
}

interface AggregationMap {
  [key: string]: Map<number, AggData>;
}

declare type AggFunction = (aggData: AggData, value?: any) => void;

const avg: AggFunction = (aggData: AggData, value?: any) => {
  aggData.count++;
  aggData.sum += value;
  aggData.aggValue = aggData.sum / aggData.count;
};

const min: AggFunction = (aggData: AggData, value?: any) => {
  aggData.aggValue = Math.min(aggData.aggValue, value);
};

const max: AggFunction = (aggData: AggData, value?: any) => {
  aggData.aggValue = Math.max(aggData.aggValue, value);
};

const sum: AggFunction = (aggData: AggData, value?: any) => {
  aggData.aggValue = aggData.aggValue + value;
};

const count: AggFunction = (aggData: AggData) => {
  aggData.count++;
  aggData.aggValue = aggData.count;
};

const none: AggFunction = (aggData: AggData, value?: any) => {
  aggData.aggValue = value;
};

export class DataAggregator {

  private dataBuffer: SubscriptionData = {};
  private data: SubscriptionData;
  private readonly lastPrevKvPairData: {[key: string]: [number, any]};

  private aggregationMap: AggregationMap;

  private dataReceived = false;
  private resetPending = false;
  private updatedData = false;

  private noAggregation = this.subsTw.aggregation.type === AggregationType.NONE;
  private aggregationTimeout = Math.max(this.subsTw.aggregation.interval, 1000);
  private readonly aggFunction: AggFunction;

  private intervalTimeoutHandle: Timeout;
  private intervalScheduledTime: number;

  private startTs = this.subsTw.startTs + this.subsTw.tsOffset;
  private endTs: number;
  private elapsed: number;

  constructor(private onDataCb: onAggregatedData,
              private tsKeyNames: string[],
              private subsTw: SubscriptionTimewindow,
              private utils: UtilsService,
              private ignoreDataUpdateOnIntervalTick: boolean) {
    this.tsKeyNames.forEach((key) => {
      this.dataBuffer[key] = [];
    });
    if (this.subsTw.aggregation.stateData) {
      this.lastPrevKvPairData = {};
    }
    switch (this.subsTw.aggregation.type) {
      case AggregationType.MIN:
        this.aggFunction = min;
        break;
      case AggregationType.MAX:
        this.aggFunction = max;
        break;
      case AggregationType.AVG:
        this.aggFunction = avg;
        break;
      case AggregationType.SUM:
        this.aggFunction = sum;
        break;
      case AggregationType.COUNT:
        this.aggFunction = count;
        break;
      case AggregationType.NONE:
        this.aggFunction = none;
        break;
      default:
        this.aggFunction = avg;
    }
  }

  public updateOnDataCb(newOnDataCb: onAggregatedData): onAggregatedData {
    const prevOnDataCb = this.onDataCb;
    this.onDataCb = newOnDataCb;
    return prevOnDataCb;
  }

  public reset(subsTw: SubscriptionTimewindow) {
    if (this.intervalTimeoutHandle) {
      clearTimeout(this.intervalTimeoutHandle);
      this.intervalTimeoutHandle = null;
    }
    this.subsTw = subsTw;
    this.intervalScheduledTime = this.utils.currentPerfTime();
    this.startTs = this.subsTw.startTs + this.subsTw.tsOffset;
    if (this.subsTw.quickInterval) {
      this.endTs = calculateIntervalEndTime(this.subsTw.quickInterval, null, this.subsTw.timezone) + this.subsTw.tsOffset;
    } else {
      this.endTs = this.startTs + this.subsTw.aggregation.timeWindow;
    }
    this.elapsed = 0;
    this.aggregationTimeout = Math.max(this.subsTw.aggregation.interval, 1000);
    this.resetPending = true;
    this.updatedData = false;
    this.intervalTimeoutHandle = setTimeout(this.onInterval.bind(this), this.aggregationTimeout);
  }

  public destroy() {
    if (this.intervalTimeoutHandle) {
      clearTimeout(this.intervalTimeoutHandle);
      this.intervalTimeoutHandle = null;
    }
    this.aggregationMap = null;
  }

  public onData(data: SubscriptionDataHolder, update: boolean, history: boolean, detectChanges: boolean) {
    this.updatedData = true;
    if (!this.dataReceived || this.resetPending) {
      let updateIntervalScheduledTime = true;
      if (!this.dataReceived) {
        this.elapsed = 0;
        this.dataReceived = true;
        if (this.subsTw.quickInterval) {
          this.endTs = calculateIntervalEndTime(this.subsTw.quickInterval, null, this.subsTw.timezone) + this.subsTw.tsOffset;
        } else {
          this.endTs = this.startTs + this.subsTw.aggregation.timeWindow;
        }
      }
      if (this.resetPending) {
        this.resetPending = false;
        updateIntervalScheduledTime = false;
      }
      if (update) {
        this.aggregationMap = {};
        this.updateAggregatedData(data.data);
      } else {
        this.aggregationMap = this.processAggregatedData(data.data);
      }
      if (updateIntervalScheduledTime) {
        this.intervalScheduledTime = this.utils.currentPerfTime();
      }
      this.onInterval(history, detectChanges);
    } else {
      this.updateAggregatedData(data.data);
      if (history) {
        this.intervalScheduledTime = this.utils.currentPerfTime();
        this.onInterval(history, detectChanges);
      }
    }
  }

  private onInterval(history?: boolean, detectChanges?: boolean) {
    const now = this.utils.currentPerfTime();
    this.elapsed += now - this.intervalScheduledTime;
    this.intervalScheduledTime = now;
    if (this.intervalTimeoutHandle) {
      clearTimeout(this.intervalTimeoutHandle);
      this.intervalTimeoutHandle = null;
    }
    if (!history) {
      const delta = Math.floor(this.elapsed / this.subsTw.aggregation.interval);
      if (delta || !this.data) {
        const tickTs = delta * this.subsTw.aggregation.interval;
        if (this.subsTw.quickInterval) {
          const currentDate = getCurrentTime(this.subsTw.timezone);
          this.startTs = calculateIntervalStartTime(this.subsTw.quickInterval, currentDate) + this.subsTw.tsOffset;
          this.endTs = calculateIntervalEndTime(this.subsTw.quickInterval, currentDate) + this.subsTw.tsOffset;
        } else {
          this.startTs += tickTs;
          this.endTs += tickTs;
        }
        this.data = this.updateData();
        this.elapsed = this.elapsed - delta * this.subsTw.aggregation.interval;
      }
    } else {
      this.data = this.updateData();
    }
    if (this.onDataCb && (!this.ignoreDataUpdateOnIntervalTick || this.updatedData)) {
      this.onDataCb(this.data, detectChanges);
      this.updatedData = false;
    }
    if (!history) {
      this.intervalTimeoutHandle = setTimeout(this.onInterval.bind(this), this.aggregationTimeout);
    }
  }

  private updateData(): SubscriptionData {
    this.tsKeyNames.forEach((key) => {
      this.dataBuffer[key] = [];
    });
    for (const key of Object.keys(this.aggregationMap)) {
      const aggKeyData = this.aggregationMap[key];
      let keyData = this.dataBuffer[key];
      aggKeyData.forEach((aggData, aggTimestamp) => {
        if (aggTimestamp <= this.startTs) {
          if (this.subsTw.aggregation.stateData &&
            (!this.lastPrevKvPairData[key] || this.lastPrevKvPairData[key][0] < aggTimestamp)) {
            this.lastPrevKvPairData[key] = [aggTimestamp, aggData.aggValue];
          }
          aggKeyData.delete(aggTimestamp);
          this.updatedData = true;
        } else if (aggTimestamp <= this.endTs) {
          const kvPair: [number, any] = [aggTimestamp, aggData.aggValue];
          keyData.push(kvPair);
        }
      });
      keyData.sort((set1, set2) => set1[0] - set2[0]);
      if (this.subsTw.aggregation.stateData) {
        this.updateStateBounds(keyData, deepClone(this.lastPrevKvPairData[key]));
      }
      if (keyData.length > this.subsTw.aggregation.limit) {
        keyData = keyData.slice(keyData.length - this.subsTw.aggregation.limit);
      }
      this.dataBuffer[key] = keyData;
    }
    return this.dataBuffer;
  }

  private updateStateBounds(keyData: [number, any][], lastPrevKvPair: [number, any]) {
    if (lastPrevKvPair) {
      lastPrevKvPair[0] = this.startTs;
    }
    let firstKvPair;
    if (!keyData.length) {
      if (lastPrevKvPair) {
        firstKvPair = lastPrevKvPair;
        keyData.push(firstKvPair);
      }
    } else {
      firstKvPair = keyData[0];
    }
    if (firstKvPair && firstKvPair[0] > this.startTs) {
      if (lastPrevKvPair) {
        keyData.unshift(lastPrevKvPair);
      }
    }
    if (keyData.length) {
      let lastKvPair = keyData[keyData.length - 1];
      if (lastKvPair[0] < this.endTs) {
        lastKvPair = deepClone(lastKvPair);
        lastKvPair[0] = this.endTs;
        keyData.push(lastKvPair);
      }
    }
  }

  private processAggregatedData(data: SubscriptionData): AggregationMap {
    const isCount = this.subsTw.aggregation.type === AggregationType.COUNT;
    const aggregationMap: AggregationMap = {};
    for (const key of Object.keys(data)) {
      let aggKeyData = aggregationMap[key];
      if (!aggKeyData) {
        aggKeyData = new Map<number, AggData>();
        aggregationMap[key] = aggKeyData;
      }
      const keyData = data[key];
      keyData.forEach((kvPair) => {
        const timestamp = kvPair[0];
        const value = this.convertValue(kvPair[1]);
        const aggKey = timestamp;
        const aggData = {
          count: isCount ? value : 1,
          sum: value,
          aggValue: value
        };
        aggKeyData.set(aggKey, aggData);
      });
    }
    return aggregationMap;
  }

  private updateAggregatedData(data: SubscriptionData) {
    const isCount = this.subsTw.aggregation.type === AggregationType.COUNT;
    for (const key of Object.keys(data)) {
      let aggKeyData = this.aggregationMap[key];
      if (!aggKeyData) {
        aggKeyData = new Map<number, AggData>();
        this.aggregationMap[key] = aggKeyData;
      }
      const keyData = data[key];
      keyData.forEach((kvPair) => {
        const timestamp = kvPair[0];
        const value = this.convertValue(kvPair[1]);
        const aggTimestamp = this.noAggregation ? timestamp : (this.startTs +
          Math.floor((timestamp - this.startTs) / this.subsTw.aggregation.interval) *
          this.subsTw.aggregation.interval + this.subsTw.aggregation.interval / 2);
        let aggData = aggKeyData.get(aggTimestamp);
        if (!aggData) {
          aggData = {
            count: 1,
            sum: value,
            aggValue: isCount ? 1 : value
          };
          aggKeyData.set(aggTimestamp, aggData);
        } else {
          this.aggFunction(aggData, value);
        }
      });
    }
  }

  private isNumeric(val: any): boolean {
    return (val - parseFloat( val ) + 1) >= 0;
  }

  private convertValue(val: string): any {
    if (!this.noAggregation || val && this.isNumeric(val)) {
      return Number(val);
    } else {
      return val;
    }
  }

}

