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

import { DataSet, DataSetHolder, DatasourceType, widgetType } from '@shared/models/widget.models';
import { AggregationType, SubscriptionTimewindow } from '@shared/models/time/time.models';
import {
  EntityData,
  EntityDataPageLink,
  EntityFilter,
  EntityKey,
  EntityKeyType,
  entityKeyTypeToDataKeyType,
  entityPageDataChanged,
  KeyFilter,
  TsValue
} from '@shared/models/query/query.models';
import {
  DataKeyType,
  EntityCountCmd,
  EntityDataCmd,
  SubscriptionData,
  SubscriptionDataHolder,
  TelemetryService,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityDataListener, EntityDataLoadResult } from '@core/api/entity-data.service';
import { deepClone, isDefined, isDefinedAndNotNull, isObject, objectHashCode } from '@core/utils';
import { PageData } from '@shared/models/page/page-data';
import { DataAggregator } from '@core/api/data-aggregator';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityType } from '@shared/models/entity-type.models';
import { Observable, of, ReplaySubject, Subject } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import Timeout = NodeJS.Timeout;

declare type DataKeyFunction = (time: number, prevValue: any) => any;
declare type DataKeyPostFunction = (time: number, value: any, prevValue: any, timePrev: number, prevOrigValue: any) => any;
declare type DataUpdatedCb = (data: DataSetHolder, dataIndex: number, dataKeyIndex: number, detectChanges: boolean) => void;

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

export interface EntityDataSubscriptionOptions {
  datasourceType: DatasourceType;
  dataKeys: Array<SubscriptionDataKey>;
  type: widgetType;
  entityFilter?: EntityFilter;
  isPaginatedDataSubscription?: boolean;
  pageLink?: EntityDataPageLink;
  keyFilters?: Array<KeyFilter>;
  additionalKeyFilters?: Array<KeyFilter>;
  subscriptionTimewindow?: SubscriptionTimewindow;
}

export class EntityDataSubscription {

  private entityDataSubscriptionOptions = this.listener.subscriptionOptions;
  private datasourceType: DatasourceType = this.entityDataSubscriptionOptions.datasourceType;
  private history: boolean;
  private realtime: boolean;

  private subscriber: TelemetrySubscriber;
  private dataCommand: EntityDataCmd;
  private subsCommand: EntityDataCmd;
  private countCommand: EntityCountCmd;

  private attrFields: Array<EntityKey>;
  private tsFields: Array<EntityKey>;
  private latestValues: Array<EntityKey>;

  private entityDataResolveSubject: Subject<EntityDataLoadResult>;
  private pageData: PageData<EntityData>;
  private subsTw: SubscriptionTimewindow;
  private dataAggregators: Array<DataAggregator>;
  private dataKeys: {[key: string]: Array<SubscriptionDataKey> | SubscriptionDataKey} = {};
  private datasourceData: {[index: number]: {[key: string]: DataSetHolder}};
  private datasourceOrigData: {[index: number]: {[key: string]: DataSetHolder}};
  private entityIdToDataIndex: {[id: string]: number};

  private frequency: number;
  private tickScheduledTime = 0;
  private tickElapsed = 0;
  private timer: Timeout;

  private dataResolved = false;
  private started = false;

  constructor(private listener: EntityDataListener,
              private telemetryService: TelemetryService,
              private utils: UtilsService) {
    this.initializeSubscription();
  }

  private initializeSubscription() {
    for (let i = 0; i < this.entityDataSubscriptionOptions.dataKeys.length; i++) {
      const dataKey = deepClone(this.entityDataSubscriptionOptions.dataKeys[i]);
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
      if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount ||
        this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
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
        dataKeysList.push(dataKey);
      } else {
        key = String(objectHashCode(dataKey));
        this.dataKeys[key] = dataKey;
      }
      dataKey.key = key;
    }
  }

  public unsubscribe() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount) {
      if (this.subscriber) {
        this.subscriber.unsubscribe();
        this.subscriber = null;
      }
    }
    if (this.dataAggregators) {
      this.dataAggregators.forEach((aggregator) => {
        aggregator.destroy();
      });
      this.dataAggregators = null;
    }
    this.pageData = null;
  }

  public subscribe(): Observable<EntityDataLoadResult> {
    this.entityDataResolveSubject = new ReplaySubject(1);
    if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      this.started = true;
      this.dataResolved = true;
      this.subsTw = this.entityDataSubscriptionOptions.subscriptionTimewindow;
      this.history = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
        isObject(this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow);
      this.realtime = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
        isDefinedAndNotNull(this.entityDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);
    }
    if (this.datasourceType === DatasourceType.entity) {
      const entityFields: Array<EntityKey> =
        this.entityDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.entityField).map(
          dataKey => ({ type: EntityKeyType.ENTITY_FIELD, key: dataKey.name })
        );
      if (!entityFields.find(key => key.key === 'name')) {
        entityFields.push({
          type: EntityKeyType.ENTITY_FIELD,
          key: 'name'
        });
      }
      if (!entityFields.find(key => key.key === 'label')) {
        entityFields.push({
          type: EntityKeyType.ENTITY_FIELD,
          key: 'label'
        });
      }
      if (!entityFields.find(key => key.key === 'additionalInfo')) {
        entityFields.push({
          type: EntityKeyType.ENTITY_FIELD,
          key: 'additionalInfo'
        });
      }

      this.attrFields = this.entityDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.attribute).map(
        dataKey => ({ type: EntityKeyType.ATTRIBUTE, key: dataKey.name })
      );

      this.tsFields = this.entityDataSubscriptionOptions.dataKeys.filter(dataKey => dataKey.type === DataKeyType.timeseries).map(
        dataKey => ({ type: EntityKeyType.TIME_SERIES, key: dataKey.name })
      );

      this.latestValues = this.attrFields.concat(this.tsFields);

      this.subscriber = new TelemetrySubscriber(this.telemetryService);
      this.dataCommand = new EntityDataCmd();

      let keyFilters = this.entityDataSubscriptionOptions.keyFilters;
      if (this.entityDataSubscriptionOptions.additionalKeyFilters) {
        if (keyFilters) {
          keyFilters = keyFilters.concat(this.entityDataSubscriptionOptions.additionalKeyFilters);
        } else {
          keyFilters = this.entityDataSubscriptionOptions.additionalKeyFilters;
        }
      }

      this.dataCommand.query = {
        entityFilter: this.entityDataSubscriptionOptions.entityFilter,
        pageLink: this.entityDataSubscriptionOptions.pageLink,
        keyFilters,
        entityFields,
        latestValues: this.latestValues
      };

      if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
        this.prepareSubscriptionCommands(this.dataCommand);
      }

      this.subscriber.subscriptionCommands.push(this.dataCommand);

      this.subscriber.entityData$.subscribe(
        (entityDataUpdate) => {
          if (entityDataUpdate.data) {
            this.onPageData(entityDataUpdate.data);
          } else if (entityDataUpdate.update) {
            this.onDataUpdate(entityDataUpdate.update);
          }
        }
      );

      this.subscriber.reconnect$.subscribe(() => {
        if (this.started) {
          const targetCommand = this.entityDataSubscriptionOptions.isPaginatedDataSubscription ? this.dataCommand : this.subsCommand;
          if (this.entityDataSubscriptionOptions.type === widgetType.timeseries &&
              !this.history && this.tsFields.length) {
            const newSubsTw: SubscriptionTimewindow = this.listener.updateRealtimeSubscription();
            this.subsTw = newSubsTw;
            targetCommand.tsCmd.startTs = this.subsTw.startTs;
            targetCommand.tsCmd.timeWindow = this.subsTw.aggregation.timeWindow;
            targetCommand.tsCmd.interval = this.subsTw.aggregation.interval;
            targetCommand.tsCmd.limit = this.subsTw.aggregation.limit;
            targetCommand.tsCmd.agg = this.subsTw.aggregation.type;
            targetCommand.tsCmd.fetchLatestPreviousPoint = this.subsTw.aggregation.stateData;
            this.dataAggregators.forEach((dataAggregator) => {
              dataAggregator.reset(newSubsTw.startTs,  newSubsTw.aggregation.timeWindow, newSubsTw.aggregation.interval);
            });
          }
          targetCommand.query = this.dataCommand.query;
          this.subscriber.subscriptionCommands = [targetCommand];
        } else {
          this.subscriber.subscriptionCommands = [this.dataCommand];
        }
      });

      this.subscriber.subscribe();
    } else if (this.datasourceType === DatasourceType.function) {
      const entityData: EntityData = {
        entityId: {
          id: NULL_UUID,
          entityType: EntityType.DEVICE
        },
        timeseries: {},
        latest: {}
      };
      const name = DatasourceType.function;
      entityData.latest[EntityKeyType.ENTITY_FIELD] = {
        name: {ts: Date.now(), value: name}
      };
      const pageData: PageData<EntityData> = {
        data: [entityData],
        hasNext: false,
        totalElements: 1,
        totalPages: 1
      };
      this.onPageData(pageData);
    } else if (this.datasourceType === DatasourceType.entityCount) {
      this.subscriber = new TelemetrySubscriber(this.telemetryService);
      this.countCommand = new EntityCountCmd();
      let keyFilters = this.entityDataSubscriptionOptions.keyFilters;
      if (this.entityDataSubscriptionOptions.additionalKeyFilters) {
        if (keyFilters) {
          keyFilters = keyFilters.concat(this.entityDataSubscriptionOptions.additionalKeyFilters);
        } else {
          keyFilters = this.entityDataSubscriptionOptions.additionalKeyFilters;
        }
      }
      this.countCommand.query = {
        entityFilter: this.entityDataSubscriptionOptions.entityFilter,
        keyFilters
      };
      this.subscriber.subscriptionCommands.push(this.countCommand);

      const entityId: EntityId = {
        id: NULL_UUID,
        entityType: null
      };

      const countKey = this.entityDataSubscriptionOptions.dataKeys[0];

      let dataReceived = false;

      this.subscriber.entityCount$.subscribe(
        (entityCountUpdate) => {
          if (!dataReceived) {
            const entityData: EntityData = {
              entityId,
              latest: {
                [EntityKeyType.ENTITY_FIELD]: {
                  name: {
                    ts: Date.now(),
                    value: DatasourceType.entityCount
                  }
                },
                [EntityKeyType.COUNT]: {
                  [countKey.name]: {
                    ts: Date.now(),
                    value: entityCountUpdate.count + ''
                  }
                }
              },
              timeseries: {}
            };
            const pageData: PageData<EntityData> = {
              data: [entityData],
              hasNext: false,
              totalElements: 1,
              totalPages: 1
            };
            this.onPageData(pageData);
            dataReceived = true;
          } else {
            const update: EntityData[] = [{
              entityId,
              latest: {
                [EntityKeyType.COUNT]: {
                  [countKey.name]: {
                    ts: Date.now(),
                    value: entityCountUpdate.count + ''
                  }
                }
              },
              timeseries: {}
            }];
            this.onDataUpdate(update);
          }
        }
      );
      this.subscriber.subscribe();
    }
    if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      return of(null);
    } else {
      return this.entityDataResolveSubject.asObservable();
    }
  }

  public start() {
    if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      return;
    }
    this.subsTw = this.entityDataSubscriptionOptions.subscriptionTimewindow;
    this.history = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
      isObject(this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow);
    this.realtime = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
      isDefinedAndNotNull(this.entityDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);

    this.prepareData();

    if (this.datasourceType === DatasourceType.entity) {
      this.subsCommand = new EntityDataCmd();
      this.subsCommand.cmdId = this.dataCommand.cmdId;
      this.prepareSubscriptionCommands(this.subsCommand);
      if (!this.subsCommand.isEmpty()) {
        this.subscriber.subscriptionCommands = [this.subsCommand];
        this.subscriber.update();
      }
    } else if (this.datasourceType === DatasourceType.function) {
      this.startFunction();
    }
    this.started = true;
  }

  private prepareSubscriptionCommands(cmd: EntityDataCmd) {
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      if (this.tsFields.length > 0) {
        if (this.history) {
          cmd.historyCmd = {
            keys: this.tsFields.map(key => key.key),
            startTs: this.subsTw.fixedWindow.startTimeMs,
            endTs: this.subsTw.fixedWindow.endTimeMs,
            interval: this.subsTw.aggregation.interval,
            limit: this.subsTw.aggregation.limit,
            agg: this.subsTw.aggregation.type,
            fetchLatestPreviousPoint: this.subsTw.aggregation.stateData
          };
        } else {
          cmd.tsCmd = {
            keys: this.tsFields.map(key => key.key),
            startTs: this.subsTw.startTs,
            timeWindow: this.subsTw.aggregation.timeWindow,
            interval: this.subsTw.aggregation.interval,
            limit: this.subsTw.aggregation.limit,
            agg: this.subsTw.aggregation.type,
            fetchLatestPreviousPoint: this.subsTw.aggregation.stateData
          };
        }
      }
    } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
      if (this.latestValues.length > 0) {
        cmd.latestCmd = {
          keys: this.latestValues
        };
      }
    }
  }

  private startFunction() {
    this.frequency = 1000;
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      this.frequency = Math.min(this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.interval, 5000);
    }
    this.tickScheduledTime = this.utils.currentPerfTime();
    if (this.history) {
      this.onTick(true);
    } else {
      this.timer = setTimeout(this.onTick.bind(this, true), 0);
    }
  }

  private prepareData() {
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }

    if (this.dataAggregators) {
      this.dataAggregators.forEach((aggregator) => {
        aggregator.destroy();
      });
    }
    this.dataAggregators = [];
    this.resetData();

    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      let tsKeyNames = [];
      if (this.datasourceType === DatasourceType.function) {
        for (const key of Object.keys(this.dataKeys)) {
          const dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
          dataKeysList.forEach((subscriptionDataKey) => {
            tsKeyNames.push(`${subscriptionDataKey.name}_${subscriptionDataKey.index}`);
          });
        }
      } else {
        tsKeyNames = this.tsFields ? this.tsFields.map(field => field.key) : [];
      }
      for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
        if (this.datasourceType === DatasourceType.function) {
          this.dataAggregators[dataIndex] = this.createRealtimeDataAggregator(this.subsTw, tsKeyNames,
            DataKeyType.function, dataIndex, this.notifyListener.bind(this));
        } else if (tsKeyNames.length) {
          this.dataAggregators[dataIndex] = this.createRealtimeDataAggregator(this.subsTw, tsKeyNames,
            DataKeyType.timeseries, dataIndex, this.notifyListener.bind(this));
        }
      }
    }
  }

  private resetData() {
    this.datasourceData = [];
    this.entityIdToDataIndex = {};
    for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
      const entityData = this.pageData.data[dataIndex];
      this.entityIdToDataIndex[entityData.entityId.id] = dataIndex;
      this.datasourceData[dataIndex] = {};
      for (const key of Object.keys(this.dataKeys)) {
        const dataKey = this.dataKeys[key];
        if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount ||
          this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
          const dataKeysList = dataKey as Array<SubscriptionDataKey>;
          for (let index = 0; index < dataKeysList.length; index++) {
            this.datasourceData[dataIndex][key + '_' + index] = {
              data: []
            };
          }
        } else {
          this.datasourceData[dataIndex][key] = {
            data: []
          };
        }
      }
    }
    this.datasourceOrigData = deepClone(this.datasourceData);
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      for (const key of Object.keys(this.dataKeys)) {
        const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        dataKeyList.forEach((dataKey) => {
          delete dataKey.lastUpdateTime;
        });
      }
    } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
      for (const key of Object.keys(this.dataKeys)) {
        delete (this.dataKeys[key] as SubscriptionDataKey).lastUpdateTime;
      }
    }
  }

  private onPageData(pageData: PageData<EntityData>) {
    const isInitialData = !this.pageData;
    if (!isInitialData && !this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      if (entityPageDataChanged(this.pageData, pageData)) {
        if (this.listener.initialPageDataChanged) {
          this.listener.initialPageDataChanged(pageData);
        }
        return;
      }
    }
    this.pageData = pageData;

    if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      this.prepareData();
    } else if (isInitialData) {
      this.resetData();
    }
    const data: Array<Array<DataSetHolder>> = [];
    for (let dataIndex = 0; dataIndex < pageData.data.length; dataIndex++) {
      const entityData = pageData.data[dataIndex];
      this.processEntityData(entityData, dataIndex, false,
        (data1, dataIndex1, dataKeyIndex) => {
          if (!data[dataIndex1]) {
            data[dataIndex1] = [];
          }
          data[dataIndex1][dataKeyIndex] = data1;
        }
      );
    }
    if (!this.dataResolved) {
      this.dataResolved = true;
      this.entityDataResolveSubject.next(
        {
          pageData,
          data,
          datasourceIndex: this.listener.configDatasourceIndex,
          pageLink: this.entityDataSubscriptionOptions.pageLink
        }
      );
      this.entityDataResolveSubject.complete();
    } else {
      if (isInitialData || this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
        this.listener.dataLoaded(pageData, data,
          this.listener.configDatasourceIndex, this.entityDataSubscriptionOptions.pageLink);
      }
      if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription && isInitialData) {
        if (this.datasourceType === DatasourceType.function) {
          this.startFunction();
        }
        this.entityDataResolveSubject.next(
          {
            pageData,
            data,
            datasourceIndex: this.listener.configDatasourceIndex,
            pageLink: this.entityDataSubscriptionOptions.pageLink
          }
        );
        this.entityDataResolveSubject.complete();
      }
    }
  }

  private onDataUpdate(update: Array<EntityData>) {
    for (const entityData of update) {
      const dataIndex = this.entityIdToDataIndex[entityData.entityId.id];
      if (isDefined(dataIndex) && dataIndex >= 0) {
        this.processEntityData(entityData, dataIndex, true, this.notifyListener.bind(this));
      }
    }
  }

  private notifyListener(data: DataSetHolder, dataIndex: number, dataKeyIndex: number, detectChanges: boolean) {
    this.listener.dataUpdated(data,
      this.listener.configDatasourceIndex,
        dataIndex, dataKeyIndex, detectChanges);
  }

  private processEntityData(entityData: EntityData, dataIndex: number, isUpdate: boolean,
                            dataUpdatedCb: DataUpdatedCb) {
    if (this.entityDataSubscriptionOptions.type === widgetType.latest && entityData.latest) {
      for (const type of Object.keys(entityData.latest)) {
        const subscriptionData = this.toSubscriptionData(entityData.latest[type], false);
        const dataKeyType = entityKeyTypeToDataKeyType(EntityKeyType[type]);
        this.onData(subscriptionData, dataKeyType, dataIndex, true, dataUpdatedCb);
      }
    }
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries && entityData.timeseries) {
      const subscriptionData = this.toSubscriptionData(entityData.timeseries, true);
      if (this.dataAggregators && this.dataAggregators[dataIndex]) {
        const dataAggregator = this.dataAggregators[dataIndex];
        let prevDataCb;
        if (!isUpdate) {
          prevDataCb = dataAggregator.updateOnDataCb((data, detectChanges) => {
            this.onData(data, this.datasourceType === DatasourceType.function ?
              DataKeyType.function : DataKeyType.timeseries, dataIndex, detectChanges, dataUpdatedCb);
          });
        }
        dataAggregator.onData({data: subscriptionData}, false, this.history, true);
        if (prevDataCb) {
          dataAggregator.updateOnDataCb(prevDataCb);
        }
      }
      if (!this.history && !isUpdate) {
        this.onData(subscriptionData, DataKeyType.timeseries, dataIndex, true, dataUpdatedCb);
      }
    }
  }

  private onData(sourceData: SubscriptionData, type: DataKeyType, dataIndex: number, detectChanges: boolean,
                 dataUpdatedCb: DataUpdatedCb) {
    for (const keyName of Object.keys(sourceData)) {
      const keyData = sourceData[keyName];
      const key = `${keyName}_${type}`;
      const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
      for (let keyIndex = 0; dataKeyList && keyIndex < dataKeyList.length; keyIndex++) {
        const datasourceKey = `${key}_${keyIndex}`;
        if (this.datasourceData[dataIndex][datasourceKey].data) {
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
            datasourceKeyData = this.datasourceData[dataIndex][datasourceKey].data;
            datasourceOrigKeyData = this.datasourceOrigData[dataIndex][datasourceKey].data;
          }
          if (datasourceKeyData.length > 0) {
            prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
            prevOrigSeries = datasourceOrigKeyData[datasourceOrigKeyData.length - 1];
          } else {
            prevSeries = [0, 0];
            prevOrigSeries = [0, 0];
          }
          this.datasourceOrigData[dataIndex][datasourceKey].data = [];
          if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
            keyData.forEach((keySeries) => {
              let series = keySeries;
              const time = series[0];
              this.datasourceOrigData[dataIndex][datasourceKey].data.push(series);
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
          } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
            if (keyData.length > 0) {
              let series = keyData[0];
              const time = series[0];
              this.datasourceOrigData[dataIndex][datasourceKey].data.push(series);
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
            this.datasourceData[dataIndex][datasourceKey].data = data;
            dataUpdatedCb(this.datasourceData[dataIndex][datasourceKey], dataIndex, dataKey.index, detectChanges);
          }
        }
      }
    }
  }

  private isNumeric(val: any): boolean {
    return (val - parseFloat( val ) + 1) >= 0;
  }

  private convertValue(val: string): any {
    if (val && this.isNumeric(val) && Number(val).toString() === val) {
      return Number(val);
    } else {
      return val;
    }
  }

  private toSubscriptionData(sourceData: {[key: string]: TsValue | TsValue[]}, isTs: boolean): SubscriptionData {
    const subsData: SubscriptionData = {};
    for (const keyName of Object.keys(sourceData)) {
      const values = sourceData[keyName];
      const dataSet: [number, any][] = [];
      if (isTs) {
        (values as TsValue[]).forEach((keySeries) => {
          dataSet.push([keySeries.ts, keySeries.value]);
        });
      } else {
        const tsValue = values as TsValue;
        dataSet.push([tsValue.ts, tsValue.value]);
      }
      subsData[keyName] = dataSet;
    }
    return subsData;
  }

  private createRealtimeDataAggregator(subsTw: SubscriptionTimewindow,
                                       tsKeyNames: Array<string>,
                                       dataKeyType: DataKeyType,
                                       dataIndex: number,
                                       dataUpdatedCb: DataUpdatedCb): DataAggregator {
    return new DataAggregator(
      (data, detectChanges) => {
        this.onData(data, dataKeyType, dataIndex, detectChanges, dataUpdatedCb);
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

  private generateSeries(dataKey: SubscriptionDataKey, index: number, startTime: number, endTime: number): [number, any][] {
    const data: [number, any][] = [];
    let prevSeries: [number, any];
    const datasourceDataKey = `${dataKey.key}_${index}`;
    const datasourceKeyData = this.datasourceData[0][datasourceDataKey].data;
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
    const datasourceKeyData = this.datasourceData[0][dataKey.key].data;
    if (datasourceKeyData.length > 0) {
      prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
    } else {
      prevSeries = [0, 0];
    }
    const time = Date.now();
    const value = dataKey.func(time, prevSeries[1]);
    const series: [number, any] = [time, value];
    this.datasourceData[0][dataKey.key].data = [series];
    this.listener.dataUpdated(this.datasourceData[0][dataKey.key],
      this.listener.configDatasourceIndex,
      0,
      dataKey.index, detectChanges);
  }

  private onTick(detectChanges: boolean) {
    const now = this.utils.currentPerfTime();
    this.tickElapsed += now - this.tickScheduledTime;
    this.tickScheduledTime = now;

    if (this.timer) {
      clearTimeout(this.timer);
    }
    let key: string;
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
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
                startTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.startTs;
                endTime = startTime + this.entityDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs + this.frequency;
                if (this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.type === AggregationType.NONE) {
                  const time = endTime - this.frequency * this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.limit;
                  startTime = Math.max(time, startTime);
                }
              }
            } else {
              startTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow.startTimeMs;
              endTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow.endTimeMs;
            }
          }
          generatedData.data[`${dataKey.name}_${dataKey.index}`] = this.generateSeries(dataKey, index, startTime, endTime);
        }
      }
      if (this.dataAggregators && this.dataAggregators.length) {
        this.dataAggregators[0].onData(generatedData, true, this.history, detectChanges);
      }
    } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
      for (key of Object.keys(this.dataKeys)) {
        this.generateLatest(this.dataKeys[key] as SubscriptionDataKey, detectChanges);
      }
    }

    if (!this.history) {
      this.timer = setTimeout(this.onTick.bind(this, true), this.frequency);
    }
  }

}
