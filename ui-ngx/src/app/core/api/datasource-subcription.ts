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

import { DataSet, DataSetHolder, DatasourceType, widgetType } from '@shared/models/widget.models';
import {
  AttributesSubscriptionCmd,
  DataKeyType,
  GetHistoryCmd,
  SubscriptionData,
  SubscriptionDataHolder,
  SubscriptionUpdateMsg,
  TelemetryService,
  TelemetrySubscriber,
  TimeseriesSubscriptionCmd
} from '@shared/models/telemetry/telemetry.models';
import { DatasourceListener } from './datasource.service';
import { AggregationType, SubscriptionTimewindow, YEAR } from '@shared/models/time/time.models';
import { deepClone, isDefinedAndNotNull, isObject, objectHashCode } from '@core/utils';
import { UtilsService } from '@core/services/utils.service';
import { EntityType } from '@shared/models/entity-type.models';
import { DataAggregator } from '@core/api/data-aggregator';
import Timeout = NodeJS.Timeout;

declare type DataKeyFunction = (time: number, prevValue: any) => any;

declare type DataKeyPostFunction = (time: number, value: any, prevValue: any, timePrev: number, prevOrigValue: any) => any;

export interface SubscriptionDataKey {
  name: string;
  type: DataKeyType;
  funcBody: string;
  func?: DataKeyFunction;
  postFuncBody: string;
  postFunc?: DataKeyPostFunction;
  index?: number;
  key?: string;
  lastUpdateTime?: number;
}

export interface DatasourceSubscriptionOptions {
  datasourceType: DatasourceType;
  dataKeys: Array<SubscriptionDataKey>;
  type: widgetType;
  entityType?: EntityType;
  entityId?: string;
  subscriptionTimewindow?: SubscriptionTimewindow;
}

export class DatasourceSubscription {

  private listeners: Array<DatasourceListener> = [];
  private datasourceType: DatasourceType = this.datasourceSubscriptionOptions.datasourceType;

  private history = this.datasourceSubscriptionOptions.subscriptionTimewindow &&
    isObject(this.datasourceSubscriptionOptions.subscriptionTimewindow.fixedWindow);

  private realtime = this.datasourceSubscriptionOptions.subscriptionTimewindow &&
    isDefinedAndNotNull(this.datasourceSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);

  private subscribers = new Array<TelemetrySubscriber>();

  private dataAggregator: DataAggregator;

  private dataKeys: {[key: string]: Array<SubscriptionDataKey> | SubscriptionDataKey} = {};
  private datasourceData: {[key: string]: DataSetHolder} = {};
  private datasourceOrigData: {[key: string]: DataSetHolder} = {};

  private frequency: number;
  private tickScheduledTime = 0;
  private tickElapsed = 0;
  private timer: Timeout;

  constructor(private datasourceSubscriptionOptions: DatasourceSubscriptionOptions,
              private telemetryService: TelemetryService,
              private utils: UtilsService) {
    this.initializeSubscription();
  }

  private initializeSubscription() {
    for (let i = 0; i < this.datasourceSubscriptionOptions.dataKeys.length; i++) {
      const dataKey = deepClone(this.datasourceSubscriptionOptions.dataKeys[i]);
      dataKey.index = i;
      if (this.datasourceType === DatasourceType.function) {
        if (!dataKey.func) {
          dataKey.func = new Function('time', 'prevValue', dataKey.funcBody) as DataKeyFunction;
        }
      } else {
        if (dataKey.postFuncBody && !dataKey.postFunc) {
          dataKey.postFunc = new Function('time', 'value', 'prevValue', 'timePrev', 'prevOrigValue',
            dataKey.postFuncBody) as DataKeyPostFunction;
        }
      }
      let key: string;
      if (this.datasourceType === DatasourceType.entity || this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
        if (this.datasourceType === DatasourceType.function) {
          key = `${dataKey.name}_${dataKey.index}_${dataKey.type}`;
        } else {
          key = `${dataKey.name}_${dataKey.type}`;
        }
        let dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        if (!dataKeysList) {
          dataKeysList = [];
          this.dataKeys[key] = dataKeysList;
        }
        const index = dataKeysList.push(dataKey) - 1;
        this.datasourceData[key + '_' + index] = {
          data: []
        };
      } else {
        key = String(objectHashCode(dataKey));
        this.datasourceData[key] = {
          data: []
        };
        this.dataKeys[key] = dataKey;
      }
      dataKey.key = key;
    }
    this.datasourceOrigData = deepClone(this.datasourceData);
    if (this.datasourceType === DatasourceType.function) {
      this.frequency = 1000;
      if (this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
        this.frequency = Math.min(this.datasourceSubscriptionOptions.subscriptionTimewindow.aggregation.interval, 5000);
      }
    }
  }

  public addListener(listener: DatasourceListener) {
    this.listeners.push(listener);
    if (this.history) {
      this.start();
    }
  }

  public hasListeners(): boolean {
    return this.listeners.length > 0;
  }

  public removeListener(listener: DatasourceListener) {
    this.listeners.splice(this.listeners.indexOf(listener), 1);
  }

  public syncListener(listener: DatasourceListener) {
    let key: string;
    let dataKey: SubscriptionDataKey;
    if (this.datasourceType === DatasourceType.entity || this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
      for (key of Object.keys(this.dataKeys)) {
        const dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        for (let i = 0; i < dataKeysList.length; i++) {
          dataKey = dataKeysList[i];
          const datasourceKey = `${key}_${i}`;
          listener.dataUpdated(this.datasourceData[datasourceKey],
            listener.datasourceIndex,
            dataKey.index, false);
        }
      }
    } else {
      for (key of Object.keys(this.dataKeys)) {
        dataKey = this.dataKeys[key] as SubscriptionDataKey;
        listener.dataUpdated(this.datasourceData[key],
          listener.datasourceIndex,
          dataKey.index, false);
      }
    }
  }

  public unsubscribe() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    if (this.datasourceType === DatasourceType.entity) {
      this.subscribers.forEach(
        (subscriber) => {
          subscriber.unsubscribe();
        }
      );
      this.subscribers.length = 0;
    }
    if (this.dataAggregator) {
      this.dataAggregator.destroy();
      this.dataAggregator = null;
    }
  }

  public start() {
    if (this.history && !this.hasListeners()) {
      return;
    }
    let subsTw = this.datasourceSubscriptionOptions.subscriptionTimewindow;
    const tsKeyNames: string[] = [];
    const attrKeyNames: string[] = [];
    let dataKey: SubscriptionDataKey;
    if (this.datasourceType === DatasourceType.entity) {

      let tsKeys = '';
      let attrKeys = '';

      for (const key of Object.keys(this.dataKeys)) {
        const dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        dataKey = dataKeysList[0];
        if (dataKey.type === DataKeyType.timeseries) {
          tsKeyNames.push(dataKey.name);
        } else if (dataKey.type === DataKeyType.attribute) {
          attrKeyNames.push(dataKey.name);
        }
      }
      tsKeys = tsKeyNames.join(',');
      attrKeys = attrKeyNames.join(',');
      if (tsKeys.length > 0) {
        if (this.history) {
          const historyCommand = new GetHistoryCmd();
          historyCommand.entityType = this.datasourceSubscriptionOptions.entityType;
          historyCommand.entityId = this.datasourceSubscriptionOptions.entityId;
          historyCommand.keys = tsKeys;
          historyCommand.startTs = subsTw.fixedWindow.startTimeMs;
          historyCommand.endTs = subsTw.fixedWindow.endTimeMs;
          historyCommand.interval = subsTw.aggregation.interval;
          historyCommand.limit = subsTw.aggregation.limit;
          historyCommand.agg = subsTw.aggregation.type;

          const subscriber = new TelemetrySubscriber(this.telemetryService);
          subscriber.subscriptionCommands.push(historyCommand);

          let firstStateHistoryCommand: GetHistoryCmd;
          if (subsTw.aggregation.stateData) {
            firstStateHistoryCommand = this.createFirstStateHistoryCommand(subsTw.fixedWindow.startTimeMs, tsKeys);
            subscriber.subscriptionCommands.push(firstStateHistoryCommand);
          }
          let data: SubscriptionUpdateMsg;
          let firstStateData: SubscriptionUpdateMsg;

          subscriber.data$.subscribe(
            (subscriptionUpdate) => {
              if (subsTw.aggregation.stateData && firstStateHistoryCommand
                && firstStateHistoryCommand.cmdId === subscriptionUpdate.subscriptionId) {
                if (data) {
                  this.onStateHistoryData(subscriptionUpdate, data, subsTw.aggregation.limit,
                    subsTw.fixedWindow.startTimeMs, subsTw.fixedWindow.endTimeMs,
                    (newData) => {
                      this.onData(newData.data, DataKeyType.timeseries, true);
                    }
                  );
                } else {
                  firstStateData = data;
                }
              } else {
                if (subsTw.aggregation.stateData) {
                  if (firstStateData) {
                    this.onStateHistoryData(firstStateData, subscriptionUpdate, subsTw.aggregation.limit,
                      subsTw.fixedWindow.startTimeMs, subsTw.fixedWindow.endTimeMs,
                      (newData) => {
                        this.onData(newData.data, DataKeyType.timeseries, true);
                      });
                  } else {
                    data = subscriptionUpdate;
                  }
                } else {
                  for (const key of Object.keys(subscriptionUpdate.data)) {
                    const keyData = subscriptionUpdate.data[key];
                    keyData.sort((set1, set2) => set1[0] - set2[0]);
                  }
                  this.onData(subscriptionUpdate.data, DataKeyType.timeseries, true);
                }
              }
            }
          );
          subscriber.subscribe();
          this.subscribers.push(subscriber);
        } else {
          const subscriptionCommand = new TimeseriesSubscriptionCmd();
          subscriptionCommand.entityType = this.datasourceSubscriptionOptions.entityType;
          subscriptionCommand.entityId = this.datasourceSubscriptionOptions.entityId;
          subscriptionCommand.keys = tsKeys;

          const subscriber = new TelemetrySubscriber(this.telemetryService);
          subscriber.subscriptionCommands.push(subscriptionCommand);

          if (this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
            this.updateRealtimeSubscriptionCommand(subscriptionCommand, subsTw);

            let firstStateSubscriptionCommand: GetHistoryCmd;
            if (subsTw.aggregation.stateData) {
              firstStateSubscriptionCommand = this.createFirstStateHistoryCommand(subsTw.startTs, tsKeys);
              subscriber.subscriptionCommands.push(firstStateSubscriptionCommand);
            }
            this.dataAggregator = this.createRealtimeDataAggregator(subsTw, tsKeyNames, DataKeyType.timeseries);

            let data: SubscriptionUpdateMsg;
            let firstStateData: SubscriptionUpdateMsg;
            let stateDataReceived: boolean;

            subscriber.data$.subscribe(
              (subscriptionUpdate) => {
                if (subsTw.aggregation.stateData &&
                  firstStateSubscriptionCommand && firstStateSubscriptionCommand.cmdId === subscriptionUpdate.subscriptionId) {
                  if (data) {
                    this.onStateHistoryData(subscriptionUpdate, data, subsTw.aggregation.limit,
                      subsTw.startTs, subsTw.startTs + subsTw.aggregation.timeWindow,
                      (newData) => {
                        this.dataAggregator.onData(newData, false, false, true);
                      });
                    stateDataReceived = true;
                  } else {
                    firstStateData = data;
                  }
                } else {
                  if (subsTw.aggregation.stateData && !stateDataReceived) {
                    if (firstStateData) {
                      this.onStateHistoryData(firstStateData, subscriptionUpdate, subsTw.aggregation.limit,
                        subsTw.startTs, subsTw.startTs + subsTw.aggregation.timeWindow,
                        (newData) => {
                          this.dataAggregator.onData(newData, false, false, true);
                        });
                      stateDataReceived = true;
                    } else {
                      data = subscriptionUpdate;
                    }
                  } else {
                    this.dataAggregator.onData(subscriptionUpdate, false, false, true);
                  }
                }
              }
            );
            subscriber.reconnect$.subscribe(() => {
              let newSubsTw: SubscriptionTimewindow = null;
              this.listeners.forEach((listener) => {
                if (!newSubsTw) {
                  newSubsTw = listener.updateRealtimeSubscription();
                } else {
                  listener.setRealtimeSubscription(newSubsTw);
                }
              });
              subsTw = newSubsTw;
              firstStateData = null;
              data = null;
              stateDataReceived = false;
              this.updateRealtimeSubscriptionCommand(subscriptionCommand, subsTw);
              if (subsTw.aggregation.stateData) {
                this.updateFirstStateHistoryCommand(firstStateSubscriptionCommand, subsTw.startTs);
              }
              this.dataAggregator.reset(newSubsTw.startTs,  newSubsTw.aggregation.timeWindow, newSubsTw.aggregation.interval);
            });
          } else {
            subscriber.data$.subscribe(
              (subscriptionUpdate) => {
                if (subscriptionUpdate.data) {
                  this.onData(subscriptionUpdate.data, DataKeyType.timeseries, true);
                }
              }
            );
          }

          subscriber.subscribe();
          this.subscribers.push(subscriber);

        }
      }

      if (attrKeys.length) {
        const attrsSubscriptionCommand = new AttributesSubscriptionCmd();
        attrsSubscriptionCommand.entityType = this.datasourceSubscriptionOptions.entityType;
        attrsSubscriptionCommand.entityId = this.datasourceSubscriptionOptions.entityId;
        attrsSubscriptionCommand.keys = attrKeys;

        const subscriber = new TelemetrySubscriber(this.telemetryService);
        subscriber.subscriptionCommands.push(attrsSubscriptionCommand);
        subscriber.data$.subscribe(
          (subscriptionUpdate) => {
            if (subscriptionUpdate.data) {
              this.onData(subscriptionUpdate.data, DataKeyType.attribute, true);
            }
          }
        );

        subscriber.subscribe();
        this.subscribers.push(subscriber);
      }
    } else if (this.datasourceType === DatasourceType.function) {
      if (this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
        for (const key of Object.keys(this.dataKeys)) {
          const dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
          dataKeysList.forEach((subscriptionDataKey) => {
            tsKeyNames.push(`${subscriptionDataKey.name}_${subscriptionDataKey.index}`);
          });
        }
        this.dataAggregator = this.createRealtimeDataAggregator(subsTw, tsKeyNames, DataKeyType.function);
      }
      this.tickScheduledTime = this.utils.currentPerfTime();
      if (this.history) {
        this.onTick(true);
      } else {
        this.timer = setTimeout(this.onTick.bind(this, true), 0);
      }
    }
  }

  private createFirstStateHistoryCommand(startTs: number, tsKeys: string): GetHistoryCmd {
    const command = new GetHistoryCmd();
    command.entityType = this.datasourceSubscriptionOptions.entityType;
    command.entityId = this.datasourceSubscriptionOptions.entityId;
    command.keys = tsKeys;
    command.startTs = startTs - YEAR;
    command.endTs = startTs;
    command.interval = 1000;
    command.limit = 1;
    command.agg = AggregationType.NONE;
    return command;
  }

  private updateFirstStateHistoryCommand(stateHistoryCommand: GetHistoryCmd, startTs: number) {
    stateHistoryCommand.startTs = startTs - YEAR;
    stateHistoryCommand.endTs = startTs;
  }

  private onStateHistoryData(firstStateData: SubscriptionUpdateMsg, data: SubscriptionUpdateMsg,
                             limit: number, startTs: number, endTs: number, onData: (data: SubscriptionUpdateMsg) => void) {
    for (const key of Object.keys(data.data)) {
      const keyData = data.data[key];
      keyData.sort((set1, set2) => set1[0] - set2[0]);
      if (keyData.length < limit) {
        let firstStateKeyData = firstStateData.data[key];
        if (firstStateKeyData.length) {
          const firstStateDataTsKv = firstStateKeyData[0];
          firstStateDataTsKv[0] = startTs;
          firstStateKeyData = [
            [ startTs, firstStateKeyData[0][1] ]
          ];
          keyData.unshift(firstStateDataTsKv);
        }
      }
      if (keyData.length) {
        const lastTsKv = deepClone(keyData[keyData.length - 1]);
        lastTsKv[0] = endTs;
        keyData.push(lastTsKv);
      }
    }
    onData(data);
  }

  private createRealtimeDataAggregator(subsTw: SubscriptionTimewindow,
                                       tsKeyNames: Array<string>, dataKeyType: DataKeyType): DataAggregator {
    return new DataAggregator(
      (data, detectChanges) => {
        this.onData(data, dataKeyType, detectChanges);
      },
      tsKeyNames,
      subsTw.startTs,
      subsTw.aggregation.limit,
      subsTw.aggregation.type,
      subsTw.aggregation.timeWindow,
      subsTw.aggregation.interval,
      subsTw.aggregation.stateData,
      this.utils
    );
  }

  private updateRealtimeSubscriptionCommand(subscriptionCommand: TimeseriesSubscriptionCmd, subsTw: SubscriptionTimewindow) {
    subscriptionCommand.startTs = subsTw.startTs;
    subscriptionCommand.timeWindow = subsTw.aggregation.timeWindow;
    subscriptionCommand.interval = subsTw.aggregation.interval;
    subscriptionCommand.limit = subsTw.aggregation.limit;
    subscriptionCommand.agg = subsTw.aggregation.type;
  }

  private generateSeries(dataKey: SubscriptionDataKey, index: number, startTime: number, endTime: number): [number, any][] {
    const data: [number, any][] = [];
    let prevSeries: [number, any];
    const datasourceDataKey = `${dataKey.key}_${index}`;
    const datasourceKeyData = this.datasourceData[datasourceDataKey].data;
    if (datasourceKeyData.length > 0) {
      prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
    } else {
      prevSeries = [0, 0];
    }
    for (let time = startTime; time <= endTime && (this.timer || this.history); time += this.frequency) {
      const value = dataKey.func(time, prevSeries[1]);
      const series: [number, any] = [time, value];
      data.push(series);
      prevSeries = series;
    }
    if (data.length > 0) {
      dataKey.lastUpdateTime = data[data.length - 1][0];
    }
    return data;
  }

  private generateLatest(dataKey: SubscriptionDataKey, detectChanges: boolean) {
    let prevSeries: [number, any];
    const datasourceKeyData = this.datasourceData[dataKey.key].data;
    if (datasourceKeyData.length > 0) {
      prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
    } else {
      prevSeries = [0, 0];
    }
    const time = Date.now();
    const value = dataKey.func(time, prevSeries[1]);
    const series: [number, any] = [time, value];
    this.datasourceData[dataKey.key].data = [series];
    this.listeners.forEach(
      (listener) => {
        listener.dataUpdated(this.datasourceData[dataKey.key],
          listener.datasourceIndex,
          dataKey.index, detectChanges);
      }
    );
  }

  private onTick(detectChanges: boolean) {
    const now = this.utils.currentPerfTime();
    this.tickElapsed += now - this.tickScheduledTime;
    this.tickScheduledTime = now;

    if (this.timer) {
      clearTimeout(this.timer);
    }
    let key: string;
    if (this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
      let startTime: number;
      let endTime: number;
      let delta: number;
      const generatedData: SubscriptionDataHolder = {
        data: {}
      };
      if (!this.history) {
        delta = Math.floor(this.tickElapsed / this.frequency);
      }
      const deltaElapsed = this.history ? this.frequency : delta * this.frequency;
      this.tickElapsed = this.tickElapsed - deltaElapsed;
      for (key of Object.keys(this.dataKeys)) {
        const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        for (let index = 0; index < dataKeyList.length && (this.timer || this.history); index ++) {
          const dataKey = dataKeyList[index];
          if (!startTime) {
            if (this.realtime) {
              if (dataKey.lastUpdateTime) {
                startTime = dataKey.lastUpdateTime + this.frequency;
                endTime = dataKey.lastUpdateTime + deltaElapsed;
              } else {
                startTime = this.datasourceSubscriptionOptions.subscriptionTimewindow.startTs;
                endTime = startTime + this.datasourceSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs + this.frequency;
                if (this.datasourceSubscriptionOptions.subscriptionTimewindow.aggregation.type === AggregationType.NONE) {
                  const time = endTime - this.frequency * this.datasourceSubscriptionOptions.subscriptionTimewindow.aggregation.limit;
                  startTime = Math.max(time, startTime);
                }
              }
            } else {
              startTime = this.datasourceSubscriptionOptions.subscriptionTimewindow.fixedWindow.startTimeMs;
              endTime = this.datasourceSubscriptionOptions.subscriptionTimewindow.fixedWindow.endTimeMs;
            }
          }
          const data = this.generateSeries(dataKey, index, startTime, endTime);
          generatedData.data[`${dataKey.name}_${dataKey.index}`] = data;
        }
      }
      if (this.dataAggregator) {
        this.dataAggregator.onData(generatedData, true, this.history, detectChanges);
      }
    } else if (this.datasourceSubscriptionOptions.type === widgetType.latest) {
      for (key of Object.keys(this.dataKeys)) {
        this.generateLatest(this.dataKeys[key] as SubscriptionDataKey, detectChanges);
      }
    }

    if (!this.history) {
      this.timer = setTimeout(this.onTick.bind(this, true), this.frequency);
    }
  }

  private onData(sourceData: SubscriptionData, type: DataKeyType, detectChanges: boolean) {
    for (const keyName of Object.keys(sourceData)) {
      const keyData = sourceData[keyName];
      const key = `${keyName}_${type}`;
      const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
      for (let keyIndex = 0; dataKeyList && keyIndex < dataKeyList.length; keyIndex++) {
        const datasourceKey = `${key}_${keyIndex}`;
        if (this.datasourceData[datasourceKey].data) {
          const dataKey = dataKeyList[keyIndex];
          const data: DataSet = [];
          let prevSeries: [number, any];
          let prevOrigSeries: [number, any];
          let datasourceKeyData: DataSet;
          let datasourceOrigKeyData: DataSet;
          let update = false;
          if (this.realtime) {
            datasourceKeyData = [];
            datasourceOrigKeyData = [];
          } else {
            datasourceKeyData = this.datasourceData[datasourceKey].data;
            datasourceOrigKeyData = this.datasourceOrigData[datasourceKey].data;
          }
          if (datasourceKeyData.length > 0) {
            prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
            prevOrigSeries = datasourceOrigKeyData[datasourceOrigKeyData.length - 1];
          } else {
            prevSeries = [0, 0];
            prevOrigSeries = [0, 0];
          }
          this.datasourceOrigData[datasourceKey].data = [];
          if (this.datasourceSubscriptionOptions.type === widgetType.timeseries) {
            keyData.forEach((keySeries) => {
              let series = keySeries;
              const time = series[0];
              this.datasourceOrigData[datasourceKey].data.push(series);
              let value = this.convertValue(series[1]);
              if (dataKey.postFunc) {
                value = dataKey.postFunc(time, value, prevSeries[1], prevOrigSeries[0], prevOrigSeries[1]);
              }
              prevOrigSeries = series;
              series = [time, value];
              data.push(series);
              prevSeries = series;
            });
            update = true;
          } else if (this.datasourceSubscriptionOptions.type === widgetType.latest) {
            if (keyData.length > 0) {
              let series = keyData[0];
              const time = series[0];
              this.datasourceOrigData[datasourceKey].data.push(series);
              let value = this.convertValue(series[1]);
              if (dataKey.postFunc) {
                value = dataKey.postFunc(time, value, prevSeries[1], prevOrigSeries[0], prevOrigSeries[1]);
              }
              series = [time, value];
              data.push(series);
            }
            update = true;
          }
          if (update) {
            this.datasourceData[datasourceKey].data = data;
            this.listeners.forEach((listener) => {
              listener.dataUpdated(this.datasourceData[datasourceKey],
                listener.datasourceIndex,
                dataKey.index, detectChanges);
            });
          }
        }
      }
    }
  }

  private isNumeric(val: any): boolean {
    return (val - parseFloat( val ) + 1) >= 0;
  }

  private convertValue(val: string): any {
    if (val && this.isNumeric(val)) {
      return Number(val);
    } else {
      return val;
    }
  }

}
