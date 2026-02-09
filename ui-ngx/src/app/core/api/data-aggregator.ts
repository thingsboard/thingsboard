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

import { AggKey, IndexedSubscriptionData, } from '@app/shared/models/telemetry/telemetry.models';
import {
  AggregationType,
  calculateAggIntervalWithSubscriptionTimeWindow,
  calculateIntervalComparisonEndTime,
  calculateIntervalEndTime,
  calculateIntervalStartEndTime,
  getCurrentTime,
  getTime,
  IntervalMath,
  SubscriptionTimewindow
} from '@shared/models/time/time.models';
import { UtilsService } from '@core/services/utils.service';
import { deepClone, isDefinedAndNotNull, isNumber, isNumeric } from '@core/utils';
import { DataEntry, DataSet, IndexedData } from '@shared/models/widget.models';
import BTree from 'sorted-btree';
import Timeout = NodeJS.Timeout;

export declare type onAggregatedData = (data: IndexedData, detectChanges: boolean) => void;

interface AggData {
  count: number;
  sum: number;
  aggValue: any;
  ts: number;
  interval: [number, number];
}

class AggDataMap {
  private map = new BTree<number, AggData>();
  private reusePair: [number, AggData] = [undefined, undefined];

  constructor(
    private subsTw: SubscriptionTimewindow,
    private endTs: number,
    private aggType: AggregationType
  ){};

  set(ts: number, data: AggData) {
    this.map.set(ts, data);
  }

  get(ts: number): AggData {
    return this.map.get(ts);
  }

  delete(ts: number) {
    this.map.delete(ts);
  }

  findDataForTs(ts: number): AggData | undefined {
    if (ts >= this.endTs) {
      this.updateLastInterval(ts + 1);
    }
    const pair = this.map.getPairOrNextLower(ts, this.reusePair);
    if (pair) {
      const data = pair[1];
      const interval = data.interval;
      if (ts < interval[1]) {
        return data;
      }
    }
  }

  calculateAggInterval(timestamp: number): [number, number] {
    return calculateAggIntervalWithSubscriptionTimeWindow(this.subsTw, this.endTs, timestamp, this.aggType);
  }

  updateLastInterval(endTs: number) {
    if (endTs > this.endTs) {
      this.endTs = endTs;
      const lastTs = this.map.maxKey();
      if (lastTs) {
        const data = this.map.get(lastTs);
        const interval = calculateAggIntervalWithSubscriptionTimeWindow(this.subsTw, endTs, data.ts, this.aggType);
        data.interval = interval;
        data.ts = interval[0] + Math.floor((interval[1] - interval[0]) / 2);
      }
    }
  }

  forEach(callback: (value: AggData, key: number, map: BTree<number, AggData>) => void, thisArg?: any) {
    this.map.forEach(callback, thisArg);
  }

  size(): number {
    return this.map.size;
  }
}

class AggregationMap {
  aggMap: {[id: number]: AggDataMap} = {};
}

declare type AggFunction = (aggData: AggData, value?: any) => void;

const avg: AggFunction = (aggData: AggData, value?: any) => {
  aggData.count++;
  if (isNumber(value)) {
    aggData.sum = aggData.aggValue * (aggData.count - 1) + value;
    aggData.aggValue = aggData.sum / aggData.count;
  } else {
    aggData.aggValue = value;
  }
};

const min: AggFunction = (aggData: AggData, value?: any) => {
  if (isNumber(value)) {
    aggData.aggValue = Math.min(aggData.aggValue, value);
  } else {
    aggData.aggValue = value;
  }
};

const max: AggFunction = (aggData: AggData, value?: any) => {
  if (isNumber(value)) {
    aggData.aggValue = Math.max(aggData.aggValue, value);
  } else {
    aggData.aggValue = value;
  }
};

const sum: AggFunction = (aggData: AggData, value?: any) => {
  if (isNumber(value)) {
    aggData.aggValue = aggData.aggValue + value;
  } else {
    aggData.aggValue = value;
  }
};

const count: AggFunction = (aggData: AggData) => {
  aggData.count++;
  aggData.aggValue = aggData.count;
};

const none: AggFunction = (aggData: AggData, value?: any) => {
  aggData.aggValue = value;
};

const MAX_INTERVAL_TIMEOUT = Math.pow(2,31)-1;

export class DataAggregator {

  constructor(private onDataCb: onAggregatedData,
              private tsKeys: AggKey[],
              private isLatestDataAgg: boolean,
              private subsTw: SubscriptionTimewindow,
              private utils: UtilsService,
              private ignoreDataUpdateOnIntervalTick: boolean) {
    this.tsKeys.forEach((key) => {
      if (!this.dataBuffer[key.id]) {
        this.dataBuffer[key.id] = [];
      }
    });
    if (this.subsTw.aggregation.stateData) {
      this.lastPrevKvPairData = {};
    }
  }

  private dataBuffer: IndexedData = [];
  private data: IndexedData;
  private readonly lastPrevKvPairData: {[id: number]: DataEntry};

  private aggregationMap: AggregationMap;

  private dataReceived = false;
  private resetPending = false;
  private updatedData = false;

  private aggregationTimeout = this.isLatestDataAgg ? 1000 : Math.max(IntervalMath.numberValue(this.subsTw.aggregation.interval), 1000);

  private intervalTimeoutHandle: Timeout;
  private intervalScheduledTime: number;

  private startTs: number;
  private endTs: number;
  private elapsed: number;

  private static convertValue(val: string, noAggregation: boolean): any {
    if (val && isNumeric(val) && (!noAggregation || noAggregation && Number(val).toString() === val)) {
      return Number(val);
    }
    return val;
  }

  private static getAggFunction(aggType: AggregationType): AggFunction {
    switch (aggType) {
      case AggregationType.MIN:
        return min;
      case AggregationType.MAX:
        return max;
      case AggregationType.AVG:
        return avg;
      case AggregationType.SUM:
        return sum;
      case AggregationType.COUNT:
        return count;
      case AggregationType.NONE:
        return none;
      default:
        return avg;
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
    this.calculateStartEndTs();
    this.elapsed = 0;
    this.aggregationTimeout = this.isLatestDataAgg ? 1000 : Math.max(IntervalMath.numberValue(this.subsTw.aggregation.interval), 1000);
    this.resetPending = true;
    this.updatedData = false;
    this.intervalTimeoutHandle = setTimeout(this.onInterval.bind(this), Math.min(this.aggregationTimeout, MAX_INTERVAL_TIMEOUT));
  }

  public destroy() {
    if (this.intervalTimeoutHandle) {
      clearTimeout(this.intervalTimeoutHandle);
      this.intervalTimeoutHandle = null;
    }
    this.aggregationMap = null;
  }

  public onData(data: IndexedSubscriptionData, update: boolean, history: boolean, detectChanges: boolean) {
    this.updatedData = true;
    if (!this.dataReceived || this.resetPending) {
      let updateIntervalScheduledTime = true;
      if (!this.dataReceived) {
        this.elapsed = 0;
        this.dataReceived = true;
        this.calculateStartEndTs();
      }
      if (this.resetPending) {
        this.resetPending = false;
        updateIntervalScheduledTime = false;
      }
      if (update) {
        this.aggregationMap = new AggregationMap();
        this.updateAggregatedData(data);
      } else {
        this.aggregationMap = this.processAggregatedData(data);
      }
      if (updateIntervalScheduledTime) {
        this.intervalScheduledTime = this.utils.currentPerfTime();
      }
      this.onInterval(history, detectChanges);
    } else {
      this.updateAggregatedData(data);
      if (history) {
        this.intervalScheduledTime = this.utils.currentPerfTime();
        this.onInterval(history, detectChanges);
      } else {
        this.onInterval(false, detectChanges, true);
      }
    }
  }

  private calculateStartEndTs() {
    this.startTs = this.subsTw.startTs + this.subsTw.tsOffset;
    if (this.subsTw.quickInterval) {
      if (this.subsTw.timeForComparison === 'previousInterval') {
        const startDate = getTime(this.subsTw.startTs, this.subsTw.timezone);
        const currentDate = getCurrentTime(this.subsTw.timezone);
        this.endTs = calculateIntervalComparisonEndTime(this.subsTw.quickInterval, startDate, currentDate) + this.subsTw.tsOffset;
      } else {
        const startDate = getTime(this.subsTw.startTs, this.subsTw.timezone);
        this.endTs = calculateIntervalEndTime(this.subsTw.quickInterval, startDate, this.subsTw.timezone) + this.subsTw.tsOffset;
      }
    } else {
      this.endTs = this.startTs + this.subsTw.aggregation.timeWindow;
    }
  }

  private onInterval(history?: boolean, detectChanges?: boolean, dataChanged?: boolean) {
    const now = this.utils.currentPerfTime();
    this.elapsed += now - this.intervalScheduledTime;
    this.intervalScheduledTime = now;
    if (this.intervalTimeoutHandle) {
      clearTimeout(this.intervalTimeoutHandle);
      this.intervalTimeoutHandle = null;
    }
    const intervalTimeout = dataChanged ? this.aggregationTimeout - this.elapsed : this.aggregationTimeout;
    if (!history) {
      const delta = Math.floor(this.elapsed / this.aggregationTimeout);
      if (delta || !this.data || dataChanged) {
        const tickTs = delta * this.aggregationTimeout;
        if (this.subsTw.quickInterval) {
          const startEndTime = calculateIntervalStartEndTime(this.subsTw.quickInterval, this.subsTw.timezone);
          this.startTs = startEndTime[0] + this.subsTw.tsOffset;
          this.endTs = startEndTime[1] + this.subsTw.tsOffset;
        } else {
          this.startTs += tickTs;
          this.endTs += tickTs;
        }
        if (this.subsTw.aggregation.type !== AggregationType.NONE) {
          this.updateLastInterval();
        }
        this.data = this.updateData();
        this.elapsed = this.elapsed - delta * this.aggregationTimeout;
      }
    } else {
      this.data = this.updateData();
    }
    if (this.onDataCb && (!this.ignoreDataUpdateOnIntervalTick || this.updatedData)) {
      this.onDataCb(this.data, detectChanges);
      this.updatedData = false;
    }
    if (!history) {
      this.intervalTimeoutHandle = setTimeout(this.onInterval.bind(this), Math.min(intervalTimeout, MAX_INTERVAL_TIMEOUT));
    }
  }

  private updateData(): IndexedData {
    this.dataBuffer = [];
    this.tsKeys.forEach((key) => {
      if (!this.dataBuffer[key.id]) {
        this.dataBuffer[key.id] = [];
      }
    });
    for (const idStr of Object.keys(this.aggregationMap.aggMap)) {
      const id = Number(idStr);
      const aggKeyData = this.aggregationMap.aggMap[id];
      const aggKey = this.aggKeyById(id);
      const noAggregation = aggKey.agg === AggregationType.NONE;
      let keyData = this.dataBuffer[id];
      const deletedKeys: number[] = [];
      aggKeyData.forEach((aggData, aggStartTs) => {
        if (aggStartTs < this.startTs) {
          if (this.subsTw.aggregation.stateData &&
            (!this.lastPrevKvPairData[id] || this.lastPrevKvPairData[id][0] < aggData.ts)) {
            this.lastPrevKvPairData[id] = [aggData.ts, aggData.aggValue, aggData.interval];
          }
          deletedKeys.push(aggStartTs);
          this.updatedData = true;
        } else if (aggData.ts < this.endTs || noAggregation) {
          const kvPair: DataEntry = [aggData.ts, aggData.aggValue, aggData.interval];
          keyData.push(kvPair);
        }
      });
      deletedKeys.forEach(ts => aggKeyData.delete(ts));
      keyData.sort((set1, set2) => set1[0] - set2[0]);
      if (this.subsTw.aggregation.stateData) {
        this.updateStateBounds(keyData, deepClone(this.lastPrevKvPairData[id]));
      }
      if (keyData.length > this.subsTw.aggregation.limit) {
        keyData = keyData.slice(keyData.length - this.subsTw.aggregation.limit);
      }
      this.dataBuffer[id] = keyData;
    }
    return this.dataBuffer;
  }

  private updateStateBounds(keyData: DataSet, lastPrevKvPair: DataEntry) {
    if (lastPrevKvPair) {
      lastPrevKvPair[0] = this.startTs;
      lastPrevKvPair[2] = [this.startTs, this.startTs];
    }
    let firstKvPair: DataEntry;
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
        lastKvPair[2] = [this.endTs, this.endTs];
        keyData.push(lastKvPair);
      }
    }
  }

  private processAggregatedData(data: IndexedSubscriptionData): AggregationMap {
    const aggregationMap = new AggregationMap();
    for (const idStr of Object.keys(data)) {
      const id = Number(idStr);
      const aggKey = this.aggKeyById(id);
      const aggType = aggKey.agg;
      const isCount = aggType === AggregationType.COUNT;
      const noAggregation = aggType === AggregationType.NONE;
      let aggKeyData = aggregationMap.aggMap[id];
      if (!aggKeyData) {
        aggKeyData = new AggDataMap(this.subsTw, this.endTs, aggType);
        aggregationMap.aggMap[id] = aggKeyData;
      }
      const keyData = data[id];
      keyData.forEach((kvPair) => {
        const timestamp = kvPair[0];
        const value = DataAggregator.convertValue(kvPair[1], noAggregation);
        let interval: [number, number] = [timestamp, timestamp];
        if (!noAggregation) {
          interval = aggKeyData.calculateAggInterval(timestamp);
        }
        const ts = interval[0] + Math.floor((interval[1] - interval[0]) / 2);
        const aggData: AggData = {
          count: isCount ? value : isDefinedAndNotNull(kvPair[2]) ? kvPair[2] : 1,
          sum: value,
          aggValue: value,
          ts,
          interval
        };
        aggKeyData.set(interval[0], aggData);
      });
    }
    return aggregationMap;
  }

  private updateAggregatedData(data: IndexedSubscriptionData) {
    for (const idStr of Object.keys(data)) {
      const id = Number(idStr);
      const aggKey = this.aggKeyById(id);
      const aggType = aggKey.agg;
      const isCount = aggType === AggregationType.COUNT;
      const noAggregation = aggType === AggregationType.NONE;
      let aggKeyData = this.aggregationMap.aggMap[id];
      if (!aggKeyData) {
        aggKeyData = new AggDataMap(this.subsTw, this.endTs, aggType);
        this.aggregationMap.aggMap[id] = aggKeyData;
      }
      const keyData = data[id];
      keyData.forEach((kvPair) => {
        const timestamp = kvPair[0];
        const value = DataAggregator.convertValue(kvPair[1], noAggregation);
        let aggData = aggKeyData.findDataForTs(timestamp);
        if (!aggData) {
          let interval: [number, number] = [timestamp, timestamp];
          if (!noAggregation) {
            interval = aggKeyData.calculateAggInterval(timestamp);
          }
          const ts = interval[0] + Math.floor((interval[1] - interval[0]) / 2);
          aggData = {
            count: isDefinedAndNotNull(kvPair[2]) ? kvPair[2] : 1,
            sum: value,
            aggValue: isCount ? 1 : value,
            ts,
            interval
          };
          aggKeyData.set(interval[0], aggData);
        } else {
          DataAggregator.getAggFunction(aggType)(aggData, value);
        }
      });
    }
  }

  private updateLastInterval() {
    for (const idStr of Object.keys(this.aggregationMap.aggMap)) {
      const id = Number(idStr);
      const aggKeyData = this.aggregationMap.aggMap[id];
      aggKeyData.updateLastInterval(this.endTs);
    }
  }

  private aggKeyById(id: number): AggKey {
    return this.tsKeys.find(key => key.id === id);
  }

}
