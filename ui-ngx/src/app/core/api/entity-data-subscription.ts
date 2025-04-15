///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import {
  ComparisonResultType,
  DataEntry,
  DataSet,
  DataSetHolder,
  DatasourceType,
  IndexedData,
  widgetType
} from '@shared/models/widget.models';
import {
  AggregationType,
  ComparisonDuration,
  createTimewindowForComparison,
  getCurrentTime,
  IntervalMath,
  SubscriptionTimewindow
} from '@shared/models/time/time.models';
import {
  AlarmFilter,
  ComparisonTsValue,
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
  AggKey,
  AlarmCountCmd,
  DataKeyType,
  EntityCountCmd,
  EntityDataCmd,
  IndexedSubscriptionData,
  IntervalType,
  NOT_SUPPORTED,
  SubscriptionData,
  SubscriptionDataEntry,
  TelemetrySubscriber
} from '@shared/models/telemetry/telemetry.models';
import { UtilsService } from '@core/services/utils.service';
import { EntityDataListener, EntityDataLoadResult } from '@core/api/entity-data.service';
import { deepClone, isDefined, isDefinedAndNotNull, isNumeric, isObject, objectHashCode } from '@core/utils';
import { PageData } from '@shared/models/page/page-data';
import { DataAggregator, onAggregatedData } from '@core/api/data-aggregator';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityType } from '@shared/models/entity-type.models';
import { firstValueFrom, from, Observable, of, ReplaySubject, Subject, Subscription } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import {
  CompiledTbFunction,
  compileTbFunction,
  isNotEmptyTbFunction,
  TbFunction
} from '@shared/models/js-function.models';
import { HttpClient } from '@angular/common/http';
import Timeout = NodeJS.Timeout;
import { finalize, switchMap } from 'rxjs/operators';

declare type DataKeyFunction = (time: number, prevValue: any) => any;
declare type DataKeyPostFunction = (time: number, value: any, prevValue: any, timePrev: number, prevOrigValue: any) => any;
declare type DataUpdatedCb = (data: DataSetHolder, dataIndex: number,
                              dataKeyIndex: number, detectChanges: boolean, isLatest: boolean) => void;

export interface SubscriptionDataKey {
  name: string;
  type: DataKeyType;
  aggregationType?: AggregationType;
  comparisonEnabled?: boolean;
  timeForComparison?: ComparisonDuration;
  comparisonCustomIntervalValue?: number;
  comparisonResultType?: ComparisonResultType;
  funcBody: TbFunction;
  func?: CompiledTbFunction<DataKeyFunction>;
  postFuncBody: TbFunction;
  postFunc?: CompiledTbFunction<DataKeyPostFunction>;
  index?: number;
  listIndex?: number;
  key?: string;
  lastUpdateTime?: number;
  latest?: boolean;
}

export interface EntityDataSubscriptionOptions {
  datasourceType: DatasourceType;
  dataKeys: Array<SubscriptionDataKey>;
  type: widgetType;
  entityFilter?: EntityFilter;
  alarmFilter?: AlarmFilter;
  isPaginatedDataSubscription?: boolean;
  ignoreDataUpdateOnIntervalTick?: boolean;
  pageLink?: EntityDataPageLink;
  keyFilters?: Array<KeyFilter>;
  additionalKeyFilters?: Array<KeyFilter>;
  subscriptionTimewindow?: SubscriptionTimewindow;
  latestTsOffset?: number;
}

export class EntityDataSubscription {

  constructor(private listener: EntityDataListener,
              private telemetryService: TelemetryWebsocketService,
              private utils: UtilsService,
              private http: HttpClient) {
  }

  private entityDataSubscriptionOptions = this.listener.subscriptionOptions;
  private datasourceType: DatasourceType = this.entityDataSubscriptionOptions.datasourceType;
  private history: boolean;
  private isFloatingTimewindow: boolean;
  private realtime: boolean;

  private subscriber: TelemetrySubscriber;
  private dataCommand: EntityDataCmd;
  private subsCommand: EntityDataCmd;
  private countCommand: EntityCountCmd;
  private alarmCountCommand: AlarmCountCmd;

  private attrFields: Array<EntityKey>;
  private tsFields: Array<EntityKey>;
  private latestValues: Array<EntityKey>;
  private aggTsValues: Array<AggKey>;
  private aggTsComparisonValues: Array<AggKey>;

  private subscribeSubscription: Subscription;
  private entityDataResolveSubject: Subject<EntityDataLoadResult>;
  private pageData: PageData<EntityData>;
  private prematureUpdates: Array<Array<EntityData>>;
  private data: Array<Array<DataSetHolder>>;
  private subsTw: SubscriptionTimewindow;
  private latestTsOffset: number;
  private dataAggregators: Array<DataAggregator>;
  private tsLatestDataAggregators: Array<DataAggregator>;
  private dataKeys: {[key: string]: Array<SubscriptionDataKey> | SubscriptionDataKey} = {};
  private dataKeysList: SubscriptionDataKey[] = [];
  private datasourceData: {[index: number]: {[key: string]: DataSetHolder}};
  private datasourceOrigData: {[index: number]: {[key: string]: DataSetHolder}};
  private entityIdToDataIndex: {[id: string]: number};

  private frequency: number;
  private latestFrequency: number;
  private tickScheduledTime = 0;
  private tickElapsed = 0;
  private timeseriesTimer: Timeout;
  private latestTimer: Timeout;

  private dataResolved = false;
  private started = false;

  private static convertValue(val: string): any {
    if (val && isNumeric(val) && Number(val).toString() === val) {
      return Number(val);
    }
    return val;
  }

  private static calculateComparisonValue(key: SubscriptionDataKey, comparisonTsValue: ComparisonTsValue): DataSet {
    let timestamp: number;
    let value: any;
    switch (key.comparisonResultType) {
      case ComparisonResultType.PREVIOUS_VALUE:
        timestamp = comparisonTsValue.previous.ts;
        value = comparisonTsValue.previous.value;
        break;
      case ComparisonResultType.DELTA_ABSOLUTE:
      case ComparisonResultType.DELTA_PERCENT:
        timestamp = comparisonTsValue.previous.ts;
        const currentVal = EntityDataSubscription.convertValue(comparisonTsValue.current.value);
        const prevVal = EntityDataSubscription.convertValue(comparisonTsValue.previous.value);
        if (isNumeric(currentVal) && isNumeric(prevVal)) {
          if (key.comparisonResultType === ComparisonResultType.DELTA_ABSOLUTE) {
            value = currentVal - prevVal;
          } else {
            if(prevVal === 0){
              value = 100;
            } else {
              value = (currentVal - prevVal) / prevVal * 100;
            }
          }
        } else {
          value = '';
        }
        break;
    }
    return [[timestamp, value]];
  }

  private async initializeSubscription() {
    for (let i = 0; i < this.entityDataSubscriptionOptions.dataKeys.length; i++) {
      const dataKey = deepClone(this.entityDataSubscriptionOptions.dataKeys[i]);
      this.dataKeysList.push(dataKey);
      dataKey.index = i;
      if (this.datasourceType === DatasourceType.function) {
        if (!dataKey.func) {
          dataKey.func = await firstValueFrom(compileTbFunction(this.http, dataKey.funcBody, 'time', 'prevValue'));
        }
      } else {
        if (isNotEmptyTbFunction(dataKey.postFuncBody) && !dataKey.postFunc) {
          try {
            dataKey.postFunc = await firstValueFrom(compileTbFunction(this.http, dataKey.postFuncBody, 'time', 'value', 'prevValue', 'timePrev', 'prevOrigValue'));
          } catch (e) {/**/}
        }
      }
      let key: string;
      if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount ||
        this.datasourceType === DatasourceType.alarmCount ||
        this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
        if (this.datasourceType === DatasourceType.function) {
          key = `${dataKey.name}_${dataKey.index}_${dataKey.type}${dataKey.latest ? '_latest' : ''}`;
        } else {
          const keyIndexSuffix = dataKey.aggregationType && dataKey.aggregationType !== AggregationType.NONE ? `_${dataKey.index}` : '';
          key = `${dataKey.name}_${dataKey.type}${keyIndexSuffix}${dataKey.latest ? '_latest' : ''}`;
        }
        let dataKeysList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        if (!dataKeysList) {
          dataKeysList = [];
          this.dataKeys[key] = dataKeysList;
        }
        dataKeysList.push(dataKey);
        dataKey.listIndex = dataKeysList.length - 1;
      } else {
        key = String(objectHashCode(dataKey));
        this.dataKeys[key] = dataKey;
      }
      dataKey.key = key;
    }
  }

  public unsubscribe() {
    if (this.subscribeSubscription) {
      this.subscribeSubscription.unsubscribe();
      this.subscribeSubscription = null;
    }
    if (this.timeseriesTimer) {
      clearTimeout(this.timeseriesTimer);
      this.timeseriesTimer = null;
    }
    if (this.latestTimer) {
      clearTimeout(this.latestTimer);
      this.latestTimer = null;
    }
    if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount
      || this.datasourceType === DatasourceType.alarmCount) {
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
    if (this.tsLatestDataAggregators) {
      this.tsLatestDataAggregators.forEach((aggregator) => {
        aggregator.destroy();
      });
      this.tsLatestDataAggregators = null;
    }
    this.pageData = null;
  }

  public subscribe(): Observable<EntityDataLoadResult> {
    this.entityDataResolveSubject = new ReplaySubject(1);
    const subscribeSubject = new ReplaySubject<void>(1);
    this.subscribeSubscription = from(this.initializeSubscription()).pipe(
      finalize(() => {
        subscribeSubject.next();
        subscribeSubject.complete();
      })
    ).subscribe(
      () => {
        if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
          this.started = true;
          this.dataResolved = true;
          this.prepareSubscriptionTimewindow();
        }
        if (this.datasourceType === DatasourceType.entity) {
          const entityFields: Array<EntityKey> =
            this.dataKeysList.filter(dataKey => dataKey.type === DataKeyType.entityField).map(
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

          this.attrFields = this.dataKeysList.filter(dataKey => dataKey.type === DataKeyType.attribute).map(
            dataKey => ({ type: EntityKeyType.ATTRIBUTE, key: dataKey.name })
          );

          this.tsFields = this.dataKeysList.
          filter(dataKey => dataKey.type === DataKeyType.timeseries &&
            (!dataKey.aggregationType || dataKey.aggregationType === AggregationType.NONE) && !dataKey.latest).map(
            dataKey => ({ type: EntityKeyType.TIME_SERIES, key: dataKey.name })
          );

          if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
            const latestTsFields = this.dataKeysList.
            filter(dataKey => dataKey.type === DataKeyType.timeseries && dataKey.latest &&
              (!dataKey.aggregationType || dataKey.aggregationType === AggregationType.NONE)).map(
              dataKey => ({ type: EntityKeyType.TIME_SERIES, key: dataKey.name })
            );
            this.latestValues = this.attrFields.concat(latestTsFields);
          } else {
            this.latestValues = this.attrFields.concat(this.tsFields);
          }

          this.aggTsValues = this.dataKeysList.
          filter(dataKey => dataKey.type === DataKeyType.timeseries &&
            dataKey.aggregationType && dataKey.aggregationType !== AggregationType.NONE && !dataKey.comparisonEnabled).map(
            dataKey => ({ id: dataKey.index, key: dataKey.name, agg: dataKey.aggregationType })
          );

          this.aggTsComparisonValues = this.dataKeysList.
          filter(dataKey => dataKey.type === DataKeyType.timeseries &&
            dataKey.aggregationType && dataKey.aggregationType !== AggregationType.NONE && dataKey.comparisonEnabled).map(
            dataKey => ({ id: dataKey.index, key: dataKey.name, agg: dataKey.aggregationType,
              previousValueOnly: dataKey.comparisonResultType === ComparisonResultType.PREVIOUS_VALUE })
          );

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
            if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
              this.subscriber.setTsOffset(this.subsTw.tsOffset);
            } else {
              this.subscriber.setTsOffset(this.latestTsOffset);
            }
          }

          this.subscriber.subscriptionCommands.push(this.dataCommand);

          this.subscriber.entityData$.subscribe(
            (entityDataUpdate) => {
              if (entityDataUpdate.data) {
                this.onPageData(entityDataUpdate.data);
                if (this.prematureUpdates) {
                  for (const update of this.prematureUpdates) {
                    this.onDataUpdate(update);
                  }
                  this.prematureUpdates = null;
                }
              } else if (entityDataUpdate.update) {
                if (!this.pageData) {
                  if (!this.prematureUpdates) {
                    this.prematureUpdates = [];
                  }
                  this.prematureUpdates.push(entityDataUpdate.update);
                } else {
                  this.onDataUpdate(entityDataUpdate.update);
                }
              }
            }
          );

          this.subscriber.reconnect$.subscribe(() => {
            if (this.started) {
              const targetCommand = this.entityDataSubscriptionOptions.isPaginatedDataSubscription ? this.dataCommand : this.subsCommand;
              if (!this.history && (this.entityDataSubscriptionOptions.type === widgetType.timeseries && this.tsFields.length ||
                this.aggTsValues.length > 0 && !this.isFloatingTimewindow)) {
                const newSubsTw = this.listener.updateRealtimeSubscription();
                this.subsTw = newSubsTw;
                if (this.entityDataSubscriptionOptions.type === widgetType.timeseries && this.tsFields.length) {
                  targetCommand.tsCmd.startTs = this.subsTw.startTs;
                  targetCommand.tsCmd.timeWindow = this.subsTw.aggregation.timeWindow;
                  if (typeof this.subsTw.aggregation.interval === 'number') {
                    targetCommand.tsCmd.interval = this.subsTw.aggregation.interval;
                    targetCommand.tsCmd.intervalType = IntervalType.MILLISECONDS;
                  } else {
                    targetCommand.tsCmd.intervalType = this.subsTw.aggregation.interval;
                  }
                  targetCommand.tsCmd.timeZoneId = this.subsTw.timezone;
                  targetCommand.tsCmd.limit = this.subsTw.aggregation.limit;
                  targetCommand.tsCmd.agg = this.subsTw.aggregation.type;
                  targetCommand.tsCmd.fetchLatestPreviousPoint = this.subsTw.aggregation.stateData;
                  this.dataAggregators.forEach((dataAggregator) => {
                    dataAggregator.reset(newSubsTw);
                  });
                }
                if (this.aggTsValues.length > 0 && !this.isFloatingTimewindow) {
                  targetCommand.aggTsCmd.startTs = this.subsTw.startTs;
                  targetCommand.aggTsCmd.timeWindow = this.subsTw.aggregation.timeWindow;
                  this.tsLatestDataAggregators.forEach((dataAggregator) => {
                    dataAggregator.reset(newSubsTw);
                  });
                }
              }
              if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
                this.subscriber.setTsOffset(this.subsTw.tsOffset);
              } else {
                this.subscriber.setTsOffset(this.latestTsOffset);
              }
              targetCommand.query = this.dataCommand.query;
              this.subscriber.subscriptionCommands = [targetCommand];
            } else {
              this.subscriber.subscriptionCommands = [this.dataCommand];
            }
          });
          this.subscriber.subscribe();
        } else if (this.datasourceType === DatasourceType.function) {
          let tsOffset = 0;
          if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
            tsOffset = this.entityDataSubscriptionOptions.latestTsOffset;
          } else if (this.entityDataSubscriptionOptions.subscriptionTimewindow) {
            tsOffset = this.entityDataSubscriptionOptions.subscriptionTimewindow.tsOffset;
          }

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
            name: {ts: Date.now() + tsOffset, value: name}
          };
          const pageData: PageData<EntityData> = {
            data: [entityData],
            hasNext: false,
            totalElements: 1,
            totalPages: 1
          };
          this.onPageData(pageData);
        } else if (this.datasourceType === DatasourceType.entityCount) {
          this.latestTsOffset = this.entityDataSubscriptionOptions.latestTsOffset;
          this.subscriber = new TelemetrySubscriber(this.telemetryService);
          this.subscriber.setTsOffset(this.latestTsOffset);
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

          const countKey = this.dataKeysList[0];

          let dataReceived = false;

          this.subscriber.entityCount$.subscribe(
            (entityCountUpdate) => {
              if (!dataReceived) {
                const entityData: EntityData = {
                  entityId,
                  latest: {
                    [EntityKeyType.ENTITY_FIELD]: {
                      name: {
                        ts: Date.now() + this.latestTsOffset,
                        value: DatasourceType.entityCount
                      }
                    },
                    [EntityKeyType.COUNT]: {
                      [countKey.name]: {
                        ts: Date.now() + this.latestTsOffset,
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
                        ts: Date.now() + this.latestTsOffset,
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
        } else if (this.datasourceType === DatasourceType.alarmCount) {
          this.latestTsOffset = this.entityDataSubscriptionOptions.latestTsOffset;
          this.subscriber = new TelemetrySubscriber(this.telemetryService);
          this.subscriber.setTsOffset(this.latestTsOffset);
          this.alarmCountCommand = new AlarmCountCmd();
          let keyFilters = this.entityDataSubscriptionOptions.keyFilters;
          if (this.entityDataSubscriptionOptions.additionalKeyFilters) {
            if (keyFilters) {
              keyFilters = keyFilters.concat(this.entityDataSubscriptionOptions.additionalKeyFilters);
            } else {
              keyFilters = this.entityDataSubscriptionOptions.additionalKeyFilters;
            }
          }
          this.alarmCountCommand.query = {
            entityFilter: this.entityDataSubscriptionOptions.entityFilter,
            keyFilters
          };
          if (this.entityDataSubscriptionOptions.alarmFilter) {
            this.alarmCountCommand.query = {...this.alarmCountCommand.query, ...this.entityDataSubscriptionOptions.alarmFilter};
          }
          this.subscriber.subscriptionCommands.push(this.alarmCountCommand);

          const entityId: EntityId = {
            id: NULL_UUID,
            entityType: null
          };

          const countKey = this.dataKeysList[0];

          let dataReceived = false;

          this.subscriber.alarmCount$.subscribe(
            (alarmCountUpdate) => {
              if (!dataReceived) {
                const entityData: EntityData = {
                  entityId,
                  latest: {
                    [EntityKeyType.ENTITY_FIELD]: {
                      name: {
                        ts: Date.now() + this.latestTsOffset,
                        value: DatasourceType.alarmCount
                      }
                    },
                    [EntityKeyType.COUNT]: {
                      [countKey.name]: {
                        ts: Date.now() + this.latestTsOffset,
                        value: alarmCountUpdate.count + ''
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
                        ts: Date.now() + this.latestTsOffset,
                        value: alarmCountUpdate.count + ''
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
      }
    );
    return subscribeSubject.pipe(
      switchMap(() => {
        if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
          return of(null);
        } else {
          return this.entityDataResolveSubject.asObservable();
        }
      })
    );
  }

  public start() {
    if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
      return;
    }
    this.prepareSubscriptionTimewindow();

    this.prepareData(true);

    if (this.datasourceType === DatasourceType.entity) {
      this.subsCommand = new EntityDataCmd();
      this.subsCommand.cmdId = this.dataCommand.cmdId;
      this.prepareSubscriptionCommands(this.subsCommand);
      let latestTsOffsetChanged = false;
      if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
        this.subscriber.setTsOffset(this.subsTw.tsOffset);
      } else {
        latestTsOffsetChanged = this.subscriber.setTsOffset(this.latestTsOffset);
      }
      if (latestTsOffsetChanged) {
        if (this.listener.forceReInit) {
          this.listener.forceReInit();
        }
      } else if (!this.subsCommand.isEmpty()) {
        this.subscriber.subscriptionCommands = [this.subsCommand];
        this.subscriber.update();
      }
    } else if (this.datasourceType === DatasourceType.entityCount || this.datasourceType === DatasourceType.alarmCount) {
      if (this.subscriber.setTsOffset(this.latestTsOffset)) {
        if (this.listener.forceReInit) {
          this.listener.forceReInit();
        }
      }
    } else if (this.datasourceType === DatasourceType.function) {
      this.startFunction();
    }
    this.started = true;
  }

  private prepareSubscriptionTimewindow() {
    this.subsTw = this.entityDataSubscriptionOptions.subscriptionTimewindow;
    this.latestTsOffset = this.entityDataSubscriptionOptions.latestTsOffset;
    this.history = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
      isObject(this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow);
    this.realtime = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
      isDefinedAndNotNull(this.entityDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs);
    this.isFloatingTimewindow = this.entityDataSubscriptionOptions.subscriptionTimewindow &&
      !this.entityDataSubscriptionOptions.subscriptionTimewindow.quickInterval && !this.history;
  }

  private prepareSubscriptionCommands(cmd: EntityDataCmd) {
    let latestValuesKeys: EntityKey[] = [];
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      if (this.tsFields.length > 0) {
        if (this.history) {
          cmd.historyCmd = {
            keys: [... new Set(this.tsFields.map(key => key.key))],
            startTs: this.subsTw.fixedWindow.startTimeMs,
            endTs: this.subsTw.fixedWindow.endTimeMs,
            interval: 0,
            intervalType: IntervalType.MILLISECONDS,
            limit: this.subsTw.aggregation.limit,
            timeZoneId: this.subsTw.timezone,
            agg: this.subsTw.aggregation.type,
            fetchLatestPreviousPoint: this.subsTw.aggregation.stateData
          };
          if (typeof this.subsTw.aggregation.interval === 'number') {
            cmd.historyCmd.interval = this.subsTw.aggregation.interval;
          } else {
            cmd.historyCmd.intervalType = this.subsTw.aggregation.interval;
          }
        } else {
          cmd.tsCmd = {
            keys: [... new Set(this.tsFields.map(key => key.key))],
            startTs: this.subsTw.startTs,
            timeWindow: this.subsTw.aggregation.timeWindow,
            interval: 0,
            intervalType: IntervalType.MILLISECONDS,
            limit: this.subsTw.aggregation.limit,
            timeZoneId: this.subsTw.timezone,
            agg: this.subsTw.aggregation.type,
            fetchLatestPreviousPoint: this.subsTw.aggregation.stateData
          };
          if (typeof this.subsTw.aggregation.interval === 'number') {
            cmd.tsCmd.interval = this.subsTw.aggregation.interval;
          } else {
            cmd.tsCmd.intervalType = this.subsTw.aggregation.interval;
          }
        }
      }
      latestValuesKeys = this.latestValues;
    } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
      latestValuesKeys = this.latestValues;
    }
    if (this.history && (this.aggTsValues.length > 0 || this.aggTsComparisonValues.length > 0)) {
      for (const aggTsComparison of this.aggTsComparisonValues) {
        const subscriptionDataKey = this.dataKeyByIndex(aggTsComparison.id);
        const timewindowForComparison =
          createTimewindowForComparison(this.subsTw, subscriptionDataKey.timeForComparison,
            subscriptionDataKey.comparisonCustomIntervalValue);
        aggTsComparison.previousStartTs = timewindowForComparison.fixedWindow.startTimeMs;
        aggTsComparison.previousEndTs = timewindowForComparison.fixedWindow.endTimeMs;
      }
      cmd.aggHistoryCmd = {
        keys: [...this.aggTsValues, ...this.aggTsComparisonValues],
        startTs: this.subsTw.fixedWindow.startTimeMs,
        endTs: this.subsTw.fixedWindow.endTimeMs
      };
    } else if (!this.isFloatingTimewindow && this.aggTsValues.length > 0) {
      cmd.aggTsCmd = {
        keys: this.aggTsValues,
        startTs: this.subsTw.startTs,
        timeWindow: this.subsTw.aggregation.timeWindow
      };
      if (latestValuesKeys.length > 0) {
        const tsKeys = this.aggTsValues.map(key => key.key);
        latestValuesKeys = latestValuesKeys.filter(latestKey => latestKey.type !== EntityKeyType.TIME_SERIES
          || !tsKeys.includes(latestKey.key));
      }
    }
    if (latestValuesKeys.length > 0) {
      cmd.latestCmd = {
        keys: latestValuesKeys
      };
    }
  }

  private startFunction() {
    this.frequency = 1000;
    this.latestFrequency = 1000;
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      this.frequency =
        Math.min(IntervalMath.numberValue(this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.interval), 5000);
    }
    this.tickScheduledTime = this.utils.currentPerfTime();
    this.generateData(true);
  }

  private prepareData(isUpdate: boolean) {
    if (this.timeseriesTimer) {
      clearTimeout(this.timeseriesTimer);
      this.timeseriesTimer = null;
    }
    if (this.latestTimer) {
      clearTimeout(this.latestTimer);
      this.latestTimer = null;
    }

    if (this.dataAggregators) {
      this.dataAggregators.forEach((aggregator) => {
        aggregator.destroy();
      });
    }
    this.dataAggregators = [];
    if (this.tsLatestDataAggregators) {
      this.tsLatestDataAggregators.forEach((aggregator) => {
        aggregator.destroy();
      });
    }
    this.tsLatestDataAggregators = [];
    this.resetData();

    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      let tsKeyIds: number[];
      if (this.datasourceType === DatasourceType.function) {
        tsKeyIds = this.dataKeysList.filter(key => !key.latest).map(key => key.index);
      } else {
        tsKeyIds = this.dataKeysList.
            filter(dataKey => dataKey.type === DataKeyType.timeseries &&
              (!dataKey.aggregationType || dataKey.aggregationType === AggregationType.NONE) && !dataKey.latest).map(
              dataKey => dataKey.index
            );
      }
      const aggKeys: AggKey[] = tsKeyIds.map(key => ({id: key, key: key + '', agg: this.subsTw.aggregation.type}));
      if (aggKeys.length) {
        for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
          this.dataAggregators[dataIndex] = this.createRealtimeDataAggregator(this.subsTw, aggKeys,
            false, dataIndex, this.notifyListener.bind(this));
        }
      }
    }
    if (this.aggTsValues && this.aggTsValues.length) {
      if (!this.isFloatingTimewindow) {
        const aggLatestTimewindow = deepClone(this.subsTw);
        aggLatestTimewindow.aggregation.stateData = false;
        aggLatestTimewindow.aggregation.interval = aggLatestTimewindow.aggregation.timeWindow;
        for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
          this.tsLatestDataAggregators[dataIndex] = this.createRealtimeDataAggregator(aggLatestTimewindow, this.aggTsValues,
            true, dataIndex, this.notifyListener.bind(this));
        }
      } else {
        this.reportNotSupported(this.aggTsValues, isUpdate);
      }
    }
    if (!this.history && this.aggTsComparisonValues && this.aggTsComparisonValues.length) {
      this.reportNotSupported(this.aggTsComparisonValues, isUpdate);
    }
  }

  private reportNotSupported(keys: AggKey[], isUpdate: boolean) {
    const indexedData: IndexedData = [];
    for (const key of keys) {
      indexedData[key.id] = [[0, NOT_SUPPORTED, [0,0]]];
    }
    for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
      this.onIndexedData(indexedData, dataIndex, true,
        this.entityDataSubscriptionOptions.type === widgetType.timeseries,
        (data, dataIndex1, dataKeyIndex, detectChanges, isLatest) => {
          if (!this.data[dataIndex1]) {
            this.data[dataIndex1] = [];
          }
          this.data[dataIndex1][dataKeyIndex] = data;
          if (isUpdate) {
            this.notifyListener(data, dataIndex1, dataKeyIndex, detectChanges, isLatest);
          }
        });
    }
  }

  private resetData() {
    this.data = [];
    this.datasourceData = [];
    this.entityIdToDataIndex = {};
    for (let dataIndex = 0; dataIndex < this.pageData.data.length; dataIndex++) {
      const entityData = this.pageData.data[dataIndex];
      this.entityIdToDataIndex[entityData.entityId.id] = dataIndex;
      this.datasourceData[dataIndex] = {};
      for (const key of Object.keys(this.dataKeys)) {
        const dataKey = this.dataKeys[key];
        if (this.datasourceType === DatasourceType.entity || this.datasourceType === DatasourceType.entityCount
          || this.datasourceType === DatasourceType.alarmCount ||
          this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
          const dataKeysList = dataKey as Array<SubscriptionDataKey>;
          for (let index = 0; index < dataKeysList.length; index++) {
            const datasourceKey = `${key}_${index}`;
            this.datasourceData[dataIndex][datasourceKey] = {
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
      this.prepareData(false);
    } else if (isInitialData) {
      this.resetData();
    }
    for (let dataIndex = 0; dataIndex < pageData.data.length; dataIndex++) {
      const entityData = pageData.data[dataIndex];
      this.processEntityData(entityData, dataIndex, false,
        (data1, dataIndex1, dataKeyIndex) => {
          if (!this.data[dataIndex1]) {
            this.data[dataIndex1] = [];
          }
          this.data[dataIndex1][dataKeyIndex] = data1;
        }
      );
    }
    if (!this.dataResolved) {
      this.dataResolved = true;
      this.entityDataResolveSubject.next(
        {
          pageData,
          data: this.data,
          datasourceIndex: this.listener.configDatasourceIndex,
          pageLink: this.entityDataSubscriptionOptions.pageLink
        }
      );
      this.entityDataResolveSubject.complete();
    } else {
      if (isInitialData || this.entityDataSubscriptionOptions.isPaginatedDataSubscription) {
        this.listener.dataLoaded(pageData, this.data,
          this.listener.configDatasourceIndex, this.entityDataSubscriptionOptions.pageLink);
      }
      if (this.entityDataSubscriptionOptions.isPaginatedDataSubscription && isInitialData) {
        if (this.datasourceType === DatasourceType.function) {
          this.startFunction();
        }
        this.entityDataResolveSubject.next(
          {
            pageData,
            data: this.data,
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

  private notifyListener(data: DataSetHolder, dataIndex: number, dataKeyIndex: number, detectChanges: boolean, isLatest: boolean) {
    this.listener.dataUpdated(data,
      this.listener.configDatasourceIndex,
        dataIndex, dataKeyIndex, detectChanges, isLatest);
  }

  private processEntityData(entityData: EntityData, dataIndex: number, isUpdate: boolean,
                            dataUpdatedCb: DataUpdatedCb) {
    if (this.entityDataSubscriptionOptions.type === widgetType.latest ||
        this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      if (entityData.aggLatest) {
        const aggData: IndexedSubscriptionData = [];
        for (const idStr of Object.keys(entityData.aggLatest)) {
          const id = Number(idStr);
          const dataKey = this.dataKeyByIndex(id);
          const aggLatestData = entityData.aggLatest[id];
          if (dataKey.comparisonEnabled) {
            const keyData = EntityDataSubscription.calculateComparisonValue(dataKey, aggLatestData);
            this.onKeyData(keyData, dataKey.name, id, dataKey.type, dataIndex, true,
              this.entityDataSubscriptionOptions.type === widgetType.timeseries, true, dataUpdatedCb);
          } else {
            aggData[id] = [[aggLatestData.current.ts, aggLatestData.current.value, aggLatestData.current.count]];
          }
        }
        if (Object.keys(aggData).length > 0 && this.tsLatestDataAggregators && this.tsLatestDataAggregators[dataIndex]) {
          const dataAggregator = this.tsLatestDataAggregators[dataIndex];
          let prevDataCb: onAggregatedData;
          if (!isUpdate) {
            prevDataCb = dataAggregator.updateOnDataCb((data, detectChanges) => {
              this.onIndexedData(data, dataIndex, detectChanges,
                this.entityDataSubscriptionOptions.type === widgetType.timeseries, dataUpdatedCb);
            });
          }
          dataAggregator.onData(aggData, false, this.history, true);
          if (prevDataCb) {
            dataAggregator.updateOnDataCb(prevDataCb);
          }
        }
      }
      if (entityData.latest) {
        for (const type of Object.keys(entityData.latest)) {
          const subscriptionData = this.toSubscriptionData(entityData.latest[type], false);
          const dataKeyType = entityKeyTypeToDataKeyType(EntityKeyType[type]);
          if (isUpdate && EntityKeyType[type] === EntityKeyType.TIME_SERIES) {
            const keys: string[] = Object.keys(subscriptionData);
            const latestTsKeys = this.latestValues.filter(key => key.type === EntityKeyType.TIME_SERIES && keys.includes(key.key));
            if (latestTsKeys.length) {
              const latestTsSubsciptionData: SubscriptionData = {};
              for (const latestTsKey of latestTsKeys) {
                latestTsSubsciptionData[latestTsKey.key] = subscriptionData[latestTsKey.key];
              }
              this.onData(latestTsSubsciptionData, dataKeyType, dataIndex, true,
                this.entityDataSubscriptionOptions.type === widgetType.timeseries, dataUpdatedCb);
            }
            const aggTsKeys = this.aggTsValues.filter(key => keys.includes(key.key));
            if (!this.history && aggTsKeys.length && this.tsLatestDataAggregators && this.tsLatestDataAggregators[dataIndex]) {
              const dataAggregator = this.tsLatestDataAggregators[dataIndex];
              const indexedData: IndexedSubscriptionData = [];
              for (const aggKey of aggTsKeys) {
                indexedData[aggKey.id] = subscriptionData[aggKey.key];
              }
              dataAggregator.onData(indexedData, true, false, true);
            }
          } else {
            this.onData(subscriptionData, dataKeyType, dataIndex, true,
              this.entityDataSubscriptionOptions.type === widgetType.timeseries, dataUpdatedCb);
          }
        }
      }
    }
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries && entityData.timeseries) {
      const subscriptionData = this.toSubscriptionData(entityData.timeseries, true);
      if (this.dataAggregators && this.dataAggregators[dataIndex]) {
        const dataAggregator = this.dataAggregators[dataIndex];
        const keyNames = Object.keys(subscriptionData);
        const dataKeys = this.timeseriesDataKeysByKeyNames(keyNames);
        const indexedData: IndexedSubscriptionData = [];
        for (const dataKey of dataKeys) {
          indexedData[dataKey.index] = subscriptionData[dataKey.name];
        }
        let prevDataCb: onAggregatedData;
        if (!isUpdate) {
          prevDataCb = dataAggregator.updateOnDataCb((data, detectChanges) => {
            this.onIndexedData(data, dataIndex, detectChanges, false, dataUpdatedCb);
          });
        }
        dataAggregator.onData(indexedData, false, this.history, true);
        if (prevDataCb) {
          dataAggregator.updateOnDataCb(prevDataCb);
        }
      } else if (!this.history && !isUpdate) {
        this.onData(subscriptionData, DataKeyType.timeseries, dataIndex, true, false, dataUpdatedCb);
      }
    }
  }

  private onData(sourceData: SubscriptionData, type: DataKeyType, dataIndex: number, detectChanges: boolean,
                 isTsLatest: boolean, dataUpdatedCb: DataUpdatedCb) {
    for (const key of Object.keys(sourceData)) {
      const keyData = sourceData[key];
      this.onKeyData(keyData.map(entry => [entry[0], entry[1], [entry[0], entry[0]]]), key, 0, type,
        dataIndex, detectChanges, isTsLatest, false, dataUpdatedCb);
    }
  }

  private onIndexedData(sourceData: IndexedData,  dataIndex: number, detectChanges: boolean,
                        isTsLatest: boolean, dataUpdatedCb: DataUpdatedCb) {
    for (const indexStr of Object.keys(sourceData)) {
      const id = Number(indexStr);
      const dataKey = this.dataKeyByIndex(id);
      const isAggLatest = dataKey.aggregationType && dataKey.aggregationType !== AggregationType.NONE;
      const keyData = sourceData[id];
      let keyName = dataKey.name;
      if (dataKey.type === DataKeyType.function) {
        keyName += `_${dataKey.index}`;
      }
      this.onKeyData(keyData, keyName, id, dataKey.type,
        dataIndex, detectChanges, isTsLatest, isAggLatest, dataUpdatedCb);
    }
  }

  private onKeyData(keyData: DataSet, keyName: string, id: number, type: DataKeyType,
                    dataIndex: number, detectChanges: boolean,
                    isTsLatest: boolean, isAggLatest: boolean, dataUpdatedCb: DataUpdatedCb) {
    const keyIdSuffix = isAggLatest ? `_${id}` : '';
    const key = `${keyName}_${type}${keyIdSuffix}${isTsLatest ? '_latest' : ''}`;
    const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
    for (let keyIndex = 0; dataKeyList && keyIndex < dataKeyList.length; keyIndex++) {
      const datasourceKey = `${key}_${keyIndex}`;
      if (this.datasourceData[dataIndex][datasourceKey].data) {
        const dataKey = dataKeyList[keyIndex];
        const data: DataSet = [];
        let prevSeries: DataEntry;
        let prevOrigSeries: DataEntry;
        let datasourceKeyData: DataSet;
        let datasourceOrigKeyData: DataSet;
        let update = false;
        if (this.realtime && !isTsLatest) {
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
          prevSeries = [0, 0, [0, 0]];
          prevOrigSeries = [0, 0, [0, 0]];
        }
        this.datasourceOrigData[dataIndex][datasourceKey].data = [];
        if (this.entityDataSubscriptionOptions.type === widgetType.timeseries && !isTsLatest) {
          keyData.forEach((keySeries) => {
            let series = keySeries;
            const time = series[0];
            this.datasourceOrigData[dataIndex][datasourceKey].data.push([series[0], series[1], series[2]]);
            let value = EntityDataSubscription.convertValue(series[1]);
            if (dataKey.postFunc) {
              value = dataKey.postFunc.execute(time, value, prevSeries[1], prevOrigSeries[0], prevOrigSeries[1]);
            }
            prevOrigSeries = [series[0], series[1], series[2]];
            series = [series[0], value, series[2]];
            data.push([series[0], series[1], series[2]]);
            prevSeries = [series[0], series[1], series[2]];
          });
          update = true;
        } else if (this.entityDataSubscriptionOptions.type === widgetType.latest || isTsLatest) {
          if (keyData.length > 0) {
            let series = keyData[0];
            const time = series[0];
            this.datasourceOrigData[dataIndex][datasourceKey].data.push([series[0], series[1], series[2]]);
            let value = EntityDataSubscription.convertValue(series[1]);
            if (dataKey.postFunc) {
              value = dataKey.postFunc.execute(time, value, prevSeries[1], prevOrigSeries[0], prevOrigSeries[1]);
            }
            series = [time, value, series[2]];
            data.push([series[0], series[1], series[2]]);
          }
          update = true;
        }
        if (update) {
          this.datasourceData[dataIndex][datasourceKey].data = data;
          dataUpdatedCb(this.datasourceData[dataIndex][datasourceKey], dataIndex, dataKey.index, detectChanges, isTsLatest);
        }
      }
    }
  }

  private toSubscriptionData(sourceData: {[key: string]: TsValue | TsValue[]}, isTs: boolean): SubscriptionData {
    const subsData: SubscriptionData = {};
    for (const keyName of Object.keys(sourceData)) {
      const values = sourceData[keyName];
      const dataSet: [number, any, number?][] = [];
      if (isTs) {
        (values as TsValue[]).forEach((keySeries) => {
          dataSet.push([keySeries.ts, keySeries.value, keySeries.count]);
        });
      } else {
        const tsValue = values as TsValue;
        dataSet.push([tsValue.ts, tsValue.value, tsValue.count]);
      }
      subsData[keyName] = dataSet;
    }
    return subsData;
  }

  private createRealtimeDataAggregator(subsTw: SubscriptionTimewindow,
                                       tsKeys: Array<AggKey>,
                                       isLatestDataAgg: boolean,
                                       dataIndex: number,
                                       dataUpdatedCb: DataUpdatedCb): DataAggregator {
    return new DataAggregator(
      (data, detectChanges) => {
        this.onIndexedData(data, dataIndex, detectChanges,
          isLatestDataAgg && (this.entityDataSubscriptionOptions.type === widgetType.timeseries), dataUpdatedCb);
      },
      tsKeys,
      isLatestDataAgg,
      subsTw,
      this.utils,
      this.entityDataSubscriptionOptions.ignoreDataUpdateOnIntervalTick || isLatestDataAgg
    );
  }

  private dataKeyByIndex(index: number): SubscriptionDataKey {
    return this.dataKeysList.find(key => key.index === index);
  }

  private timeseriesDataKeysByKeyNames(keyNames: string[]): SubscriptionDataKey[] {
    const result: SubscriptionDataKey[] = [];
    for (const keyName of keyNames) {
      const key = `${keyName}_${DataKeyType.timeseries}`;
      const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
      result.push(...dataKeyList);
    }
    return result;
  }

  private generateSeries(dataKey: SubscriptionDataKey, startTime: number, endTime: number): SubscriptionDataEntry[] {
    const data: SubscriptionDataEntry[] = [];
    let prevSeries: SubscriptionDataEntry;
    const datasourceDataKey = `${dataKey.key}_${dataKey.listIndex}`;
    const datasourceKeyData = this.datasourceData[0][datasourceDataKey].data;
    if (datasourceKeyData.length > 0) {
      const prevDataEntry = datasourceKeyData[datasourceKeyData.length - 1];
      prevSeries = [prevDataEntry[0], prevDataEntry[1]];
    } else {
      prevSeries = [0, 0];
    }
    for (let time = startTime; time <= endTime && (this.timeseriesTimer || this.history); time += this.frequency) {
      const value = dataKey.func.execute(time, prevSeries[1]);
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
    let prevSeries: DataEntry;
    const datasourceKey = dataKey.latest ? `${dataKey.key}_${dataKey.listIndex}` : dataKey.key;
    const datasourceKeyData = this.datasourceData[0][datasourceKey].data;
    if (datasourceKeyData.length > 0) {
      prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
    } else {
      prevSeries = [0, 0];
    }
    const time = Date.now() + this.latestTsOffset;
    const value = dataKey.func.execute(time, prevSeries[1]);
    const series: [number, any] = [time, value];
    this.datasourceData[0][datasourceKey].data = [series];
    this.listener.dataUpdated(this.datasourceData[0][datasourceKey],
      this.listener.configDatasourceIndex,
      0,
      dataKey.index, detectChanges, dataKey.latest);
  }

  private generateData(detectChanges: boolean) {
    let key: string;
    let tsDataKeys: SubscriptionDataKey[] = [];
    let latestDataKeys: SubscriptionDataKey[] = [];
    if (this.entityDataSubscriptionOptions.type === widgetType.timeseries) {
      for (key of Object.keys(this.dataKeys)) {
        const dataKeyList = this.dataKeys[key] as Array<SubscriptionDataKey>;
        tsDataKeys = tsDataKeys.concat(dataKeyList.filter(dataKey => !dataKey.latest));
        latestDataKeys = latestDataKeys.concat(dataKeyList.filter(dataKey => dataKey.latest));
      }
    } else if (this.entityDataSubscriptionOptions.type === widgetType.latest) {
      for (key of Object.keys(this.dataKeys)) {
        latestDataKeys.push(this.dataKeys[key] as SubscriptionDataKey);
      }
    }
    if (tsDataKeys.length) {
      if (this.history) {
        this.onTimeseriesTick(tsDataKeys, true);
      } else {
        this.timeseriesTimer = setTimeout(this.onTimeseriesTick.bind(this, tsDataKeys, true), 0);
      }
    }
    if (latestDataKeys.length) {
      this.onLatestTick(latestDataKeys, detectChanges);
    }
  }

  private onTimeseriesTick(tsDataKeys: SubscriptionDataKey[], detectChanges: boolean) {
    const now = this.utils.currentPerfTime();
    this.tickElapsed += now - this.tickScheduledTime;
    this.tickScheduledTime = now;
    if (this.timeseriesTimer) {
      clearTimeout(this.timeseriesTimer);
    }
    let startTime: number;
    let endTime: number;
    let delta: number;
    const generatedData: IndexedSubscriptionData = [];
    if (!this.history) {
      delta = Math.floor(this.tickElapsed / this.frequency);
    }
    const deltaElapsed = this.history ? this.frequency : delta * this.frequency;
    this.tickElapsed = this.tickElapsed - deltaElapsed;
    for (let index = 0; index < tsDataKeys.length && (this.timeseriesTimer || this.history); index ++) {
      const dataKey = tsDataKeys[index];
      if (!startTime) {
        if (this.realtime) {
          if (dataKey.lastUpdateTime) {
            startTime = dataKey.lastUpdateTime + this.frequency;
            endTime = dataKey.lastUpdateTime + deltaElapsed;
          } else {
            startTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.startTs +
              this.entityDataSubscriptionOptions.subscriptionTimewindow.tsOffset;
            endTime = startTime + this.entityDataSubscriptionOptions.subscriptionTimewindow.realtimeWindowMs + this.frequency;
            if (this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.type === AggregationType.NONE) {
              const time = endTime - this.frequency * this.entityDataSubscriptionOptions.subscriptionTimewindow.aggregation.limit;
              startTime = Math.max(time, startTime);
            }
          }
        } else {
          startTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow.startTimeMs +
            this.entityDataSubscriptionOptions.subscriptionTimewindow.tsOffset;
          endTime = this.entityDataSubscriptionOptions.subscriptionTimewindow.fixedWindow.endTimeMs +
            this.entityDataSubscriptionOptions.subscriptionTimewindow.tsOffset;
        }
        if (this.entityDataSubscriptionOptions.subscriptionTimewindow.quickInterval) {
          const currentTime = getCurrentTime().valueOf() + this.entityDataSubscriptionOptions.subscriptionTimewindow.tsOffset;
          endTime = Math.min(currentTime, endTime);
        }
      }
      generatedData[dataKey.index] = this.generateSeries(dataKey, startTime, endTime);
    }
    if (this.dataAggregators && this.dataAggregators.length) {
      this.dataAggregators[0].onData(generatedData, true, this.history, detectChanges);
    }

    if (!this.history) {
      this.timeseriesTimer = setTimeout(this.onTimeseriesTick.bind(this, tsDataKeys, true), this.frequency);
    }
  }

  private onLatestTick(latestDataKeys: SubscriptionDataKey[], detectChanges: boolean) {
    if (this.latestTimer) {
      clearTimeout(this.latestTimer);
    }
    latestDataKeys.forEach(dataKey => {
      this.generateLatest(dataKey, detectChanges);
    });
    this.latestTimer = setTimeout(this.onLatestTick.bind(this, latestDataKeys, true), this.latestFrequency);
  }

}
