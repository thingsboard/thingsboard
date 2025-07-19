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
  IWidgetSubscription,
  SubscriptionEntityInfo,
  SubscriptionMessage,
  WidgetSubscriptionCallbacks,
  WidgetSubscriptionContext,
  WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import {
  DataKey,
  DataKeySettingsWithComparison,
  DataSet,
  DataSetHolder,
  Datasource,
  DatasourceData,
  datasourcesHasAggregation,
  DatasourceType,
  isDataKeySettingsWithComparison,
  LegendConfig,
  LegendData,
  LegendKey,
  LegendKeyData,
  TargetDevice,
  TargetDeviceType,
  targetDeviceValid,
  widgetType
} from '@app/shared/models/widget.models';
import { HttpErrorResponse } from '@angular/common/http';
import {
  calculateIntervalStartEndTime,
  calculateTsOffset,
  ComparisonDuration,
  createSubscriptionTimewindow,
  createTimewindowForComparison,
  isHistoryTypeTimewindow,
  SubscriptionTimewindow,
  Timewindow,
  timewindowTypeChanged,
  toHistoryTimewindow,
  WidgetTimewindow
} from '@app/shared/models/time/time.models';
import { forkJoin, Observable, of, ReplaySubject, Subject, throwError, timer } from 'rxjs';
import { CancelAnimationFrame } from '@core/services/raf.service';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import {
  createLabelFromPattern,
  deepClone,
  flatFormattedData,
  formattedDataFormDatasourceData,
  isDefinedAndNotNull,
  isEqual,
  isUndefined,
  parseHttpErrorMessage
} from '@core/utils';
import { EntityId } from '@app/shared/models/id/entity-id';
import moment_ from 'moment';
import { emptyPageData, PageData } from '@shared/models/page/page-data';
import { EntityDataListener } from '@core/api/entity-data.service';
import {
  AlarmData,
  AlarmDataPageLink,
  EntityData,
  EntityDataPageLink,
  entityDataToEntityInfo,
  EntityKeyType,
  KeyFilter,
  updateDatasourceFromEntityInfo
} from '@shared/models/query/query.models';
import { distinct, filter, map, switchMap, takeUntil } from 'rxjs/operators';
import { AlarmDataListener } from '@core/api/alarm-data.service';
import { RpcStatus } from '@shared/models/rpc.models';
import { EventEmitter } from '@angular/core';
import { NOT_SUPPORTED } from '@shared/models/telemetry/telemetry.models';
import { isNotEmptyTbUnits, TbUnit } from '@shared/models/unit.models';
import { ValueFormatProcessor } from '@shared/models/widget-settings.models';

const moment = moment_;


const calculateMin = (data: DataSet): number => {
  if (data.length > 0) {
    let result = Number(data[0][1]);
    for (let i = 1; i < data.length; i++) {
      result = Math.min(result, Number(data[i][1]));
    }
    return result;
  } else {
    return null;
  }
};

const calculateMax = (data: DataSet): number => {
  if (data.length > 0) {
    let result = Number(data[0][1]);
    for (let i = 1; i < data.length; i++) {
      result = Math.max(result, Number(data[i][1]));
    }
    return result;
  } else {
    return null;
  }
};

const calculateTotal = (data: DataSet): number => {
  if (data.length > 0) {
    let result = 0;
    data.forEach((dataRow) => {
      result += Number(dataRow[1]);
    });
    return result;
  } else {
    return null;
  }
};

const calculateAvg = (data: DataSet): number => {
  if (data.length > 0) {
    return calculateTotal(data) / data.length;
  } else {
    return null;
  }
};

const calculateLatest = (data: DataSet): number => {
  if (data.length > 0) {
    return Number(data[data.length - 1][1]);
  } else {
    return null;
  }
};

export class WidgetSubscription implements IWidgetSubscription {

  id: string;
  ctx: WidgetSubscriptionContext;
  type: widgetType;
  callbacks: WidgetSubscriptionCallbacks;

  timeWindow: WidgetTimewindow;
  originalTimewindow: Timewindow;
  timeWindowConfig: Timewindow;
  timezone: string;
  subscriptionTimewindow: SubscriptionTimewindow;
  useDashboardTimewindow: boolean;
  useTimewindow: boolean;
  onTimewindowChangeFunction: (timewindow: Timewindow) => Timewindow;
  tsOffset = 0;

  hasDataPageLink: boolean;
  singleEntity: boolean;
  pageSize: number;
  warnOnPageDataOverflow: boolean;
  ignoreDataUpdateOnIntervalTick: boolean;

  get firstDatasource(): Datasource {
    if (this.type === widgetType.alarm) {
      return this.alarmSource;
    } else if (this.datasources?.length) {
      return this.datasources[0];
    } else {
      return null;
    }
  }

  datasourcePages: PageData<Datasource>[];
  dataPages: PageData<Array<DatasourceData>>[];
  entityDataListeners: Array<EntityDataListener>;
  configuredDatasources: Array<Datasource>;

  data: Array<DatasourceData>;
  latestData: Array<DatasourceData>;
  datasources: Array<Datasource>;
  hiddenData: Array<DataSetHolder>;
  legendData: LegendData;
  legendConfig: LegendConfig;
  caulculateLegendData: boolean;
  displayLegend: boolean;
  stateData: boolean;
  datasourcesOptional: boolean;
  decimals: number;
  units: TbUnit;
  comparisonEnabled: boolean;
  timeForComparison: ComparisonDuration;
  comparisonCustomIntervalValue: number;
  comparisonTimeWindow: WidgetTimewindow;
  timewindowForComparison: SubscriptionTimewindow;

  alarms: PageData<AlarmData>;
  alarmSource: Datasource;
  alarmDataListener: AlarmDataListener;

  loadingData: boolean;

  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcDisabledReason: string;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse | Error;

  init$: Observable<IWidgetSubscription>;

  cafs: {[cafId: string]: CancelAnimationFrame} = {};
  hasResolvedData = false;

  targetEntityId?: EntityId;
  targetEntityName?: string;
  targetDevice: TargetDevice;
  targetDeviceId: string;
  executingSubjects: Array<Subject<void>>;

  subscribed = false;
  hasLatestData = false;
  widgetTimewindowChangedSubject: Subject<WidgetTimewindow> = new ReplaySubject<WidgetTimewindow>();

  widgetTimewindowChanged$ = this.widgetTimewindowChangedSubject.asObservable().pipe(
    distinct()
  );

  paginatedDataSubscriptionUpdated = new EventEmitter<void>();

  constructor(subscriptionContext: WidgetSubscriptionContext, public options: WidgetSubscriptionOptions) {
    const subscriptionSubject = new ReplaySubject<IWidgetSubscription>();
    this.init$ = subscriptionSubject.asObservable();
    this.ctx = subscriptionContext;
    this.type = options.type;
    this.id = this.ctx.utils.guid();
    this.callbacks = options.callbacks;

    if (this.type === widgetType.rpc) {
      this.callbacks.rpcStateChanged = this.callbacks.rpcStateChanged || (() => {});
      this.callbacks.onRpcSuccess = this.callbacks.onRpcSuccess || (() => {});
      this.callbacks.onRpcFailed = this.callbacks.onRpcFailed || (() => {});
      this.callbacks.onRpcErrorCleared = this.callbacks.onRpcErrorCleared || (() => {});

      this.targetDevice = options.targetDevice;
      if (!targetDeviceValid(this.targetDevice)) {
        if (options.targetDeviceAliasIds && options.targetDeviceAliasIds.length) {
          this.targetDevice = {
            type: TargetDeviceType.entity,
            entityAliasId: options.targetDeviceAliasIds[0]
          };
        } else if (options.targetDeviceIds && options.targetDeviceIds.length) {
          this.targetDevice = {
            type: TargetDeviceType.device,
            entityAliasId: options.targetDeviceIds[0]
          };
        }
      }
      this.targetEntityId = null;
      this.targetEntityName = null;

      this.rpcRejection = null;
      this.rpcErrorText = null;
      this.rpcEnabled = false;
      this.executingRpcRequest = false;
      this.executingSubjects = [];
      this.initRpc().subscribe(() => {
        subscriptionSubject.next(this);
        subscriptionSubject.complete();
      });
    } else if (this.type === widgetType.alarm) {
      this.callbacks.onDataUpdated = this.callbacks.onDataUpdated || (() => {});
      this.callbacks.onDataUpdateError = this.callbacks.onDataUpdateError || (() => {});
      this.callbacks.onSubscriptionMessage = this.callbacks.onSubscriptionMessage || (() => {});
      this.callbacks.dataLoading = this.callbacks.dataLoading || (() => {});
      this.callbacks.timeWindowUpdated = this.callbacks.timeWindowUpdated || (() => {});
      this.alarmSource = options.alarmSource;
      this.datasourcesOptional = options.datasourcesOptional;
      this.alarmDataListener = null;
      this.alarms = emptyPageData();
      this.originalTimewindow = null;
      this.timeWindow = {};
      this.useDashboardTimewindow = options.useDashboardTimewindow;
      this.useTimewindow = true;
      this.onTimewindowChangeFunction = options.onTimewindowChangeFunction || ((timewindow) => timewindow);
      if (this.useDashboardTimewindow) {
        this.timeWindowConfig = deepClone(options.dashboardTimewindow);
      } else {
        this.timeWindowConfig = deepClone(options.timeWindowConfig);
      }
      this.subscriptionTimewindow = null;
      this.loadingData = false;
      this.displayLegend = false;
      this.initAlarmSubscription().subscribe({
        next:() => {
          subscriptionSubject.next(this);
          subscriptionSubject.complete();
        },
        error: () => {
          subscriptionSubject.error(null);
        }}
      );
    } else {
      this.callbacks.onDataUpdated = this.callbacks.onDataUpdated || (() => {});
      this.callbacks.onLatestDataUpdated = this.callbacks.onLatestDataUpdated || (() => {});
      this.callbacks.onDataUpdateError = this.callbacks.onDataUpdateError || (() => {});
      this.callbacks.onLatestDataUpdateError = this.callbacks.onLatestDataUpdateError || (() => {});
      this.callbacks.onSubscriptionMessage = this.callbacks.onSubscriptionMessage || (() => {});
      this.callbacks.onInitialPageDataChanged = this.callbacks.onInitialPageDataChanged || (() => {});
      this.callbacks.forceReInit = this.callbacks.forceReInit || (() => {});
      this.callbacks.dataLoading = this.callbacks.dataLoading || (() => {});
      this.callbacks.legendDataUpdated = this.callbacks.legendDataUpdated || (() => {});
      this.callbacks.timeWindowUpdated = this.callbacks.timeWindowUpdated || (() => {});

      this.configuredDatasources = this.ctx.dashboardUtils.validateAndUpdateDatasources(options.datasources);
      this.datasourcesOptional = options.datasourcesOptional;
      this.entityDataListeners = [];
      this.hasDataPageLink = options.hasDataPageLink;
      this.singleEntity = options.singleEntity;
      this.pageSize = options.pageSize;
      this.warnOnPageDataOverflow = options.warnOnPageDataOverflow;
      this.ignoreDataUpdateOnIntervalTick = options.ignoreDataUpdateOnIntervalTick;
      this.datasourcePages = [];
      this.datasources = [];
      this.dataPages = [];
      this.data = [];
      this.latestData = [];
      this.hiddenData = [];
      this.originalTimewindow = null;
      this.timeWindow = {};
      this.useDashboardTimewindow = options.useDashboardTimewindow;
      this.onTimewindowChangeFunction = options.onTimewindowChangeFunction || ((timewindow) => timewindow);
      this.stateData = options.stateData;
      this.useTimewindow = this.type === widgetType.timeseries || datasourcesHasAggregation(this.configuredDatasources);
      if (this.useDashboardTimewindow) {
        this.timeWindowConfig = deepClone(options.dashboardTimewindow);
      } else {
        this.timeWindowConfig = deepClone(options.timeWindowConfig);
      }
      if (this.type === widgetType.latest) {
        this.timezone = this.useTimewindow ? this.timeWindowConfig.timezone : options.dashboardTimewindow.timezone;
        this.updateTsOffset();
      }

      this.subscriptionTimewindow = null;
      this.comparisonEnabled = options.comparisonEnabled && isHistoryTypeTimewindow(this.timeWindowConfig);
      if (this.comparisonEnabled) {
        this.timeForComparison = options.timeForComparison;
        this.comparisonCustomIntervalValue = options.comparisonCustomIntervalValue;

        this.comparisonTimeWindow = {};
        this.timewindowForComparison = null;
      }

      this.units = options.units || '';
      this.decimals = isDefinedAndNotNull(options.decimals) ? options.decimals : 2;

      this.loadingData = false;

      if (options.legendConfig) {
        this.legendConfig = options.legendConfig;
        this.legendData = {
          keys: [],
          data: []
        };
        this.displayLegend = true;
      } else {
        this.displayLegend = false;
      }
      this.caulculateLegendData = this.displayLegend &&
        this.type === widgetType.timeseries &&
        (this.legendConfig.showMin === true ||
          this.legendConfig.showMax === true ||
          this.legendConfig.showAvg === true ||
          this.legendConfig.showTotal === true ||
          this.legendConfig.showLatest === true);
      this.initDataSubscription().subscribe({
        next:() => {
          subscriptionSubject.next(this);
          subscriptionSubject.complete();
        },
        error: () => {
          subscriptionSubject.error(null);
        }}
      );
    }
 }

  private initRpc(): Observable<any> {
    const initRpcSubject = new ReplaySubject<void>();
    this.ctx.aliasController.resolveSingleEntityInfoForTargetDevice(this.targetDevice).subscribe({
      next: (entityInfo) => {
        if (entityInfo?.id) {
          this.targetEntityId = {
            id: entityInfo.id,
            entityType: entityInfo.entityType
          };
          this.targetEntityName = entityInfo.name;
        }
        if (entityInfo?.entityType === EntityType.DEVICE) {
          this.targetDeviceId = entityInfo.id;
        }
        if (this.targetDeviceId) {
          this.rpcEnabled = true;
        } else {
          this.rpcEnabled = this.ctx.utils.widgetEditMode;
          if (!this.rpcEnabled) {
            if (this.targetEntityId) {
              const entityType =
                this.ctx.translate.instant(entityTypeTranslations.get(this.targetEntityId.entityType).type);
              this.rpcDisabledReason =
                this.ctx.translate.instant('rpc.error.invalid-target-entity', {entityType});
            } else {
              this.rpcDisabledReason = this.ctx.translate.instant('rpc.error.target-device-is-not-set');
            }
          }
        }
        this.hasResolvedData = true;
        this.callbacks.rpcStateChanged(this);
        initRpcSubject.next();
        initRpcSubject.complete();
      },
      error: () => {
        this.rpcEnabled = false;
        this.rpcDisabledReason = this.ctx.translate.instant('rpc.error.failed-to-resolve-target-device');
        this.callbacks.rpcStateChanged(this);
        initRpcSubject.next();
        initRpcSubject.complete();
      }
    });
    return initRpcSubject.asObservable();
  }

  private initAlarmSubscription(): Observable<any> {
    const initAlarmSubscriptionSubject = new ReplaySubject<void>(1);
    this.loadStDiff().subscribe(() => {
      if (!this.ctx.aliasController) {
        this.hasResolvedData = true;
        this.configureAlarmsData();
        initAlarmSubscriptionSubject.next();
        initAlarmSubscriptionSubject.complete();
      } else {
        this.ctx.aliasController.resolveAlarmSource(this.alarmSource).subscribe(
          {
            next: (alarmSource) => {
              this.alarmSource = alarmSource;
              if (alarmSource) {
                this.hasResolvedData = true;
              }
              this.configureAlarmsData();
              initAlarmSubscriptionSubject.next();
              initAlarmSubscriptionSubject.complete();
            },
            error: (err) => {
              initAlarmSubscriptionSubject.error(err);
            }
          }
        );
      }
    });
    return initAlarmSubscriptionSubject.asObservable();
  }

  private configureAlarmsData() {
    this.notifyDataLoaded();
  }

  private initDataSubscription(): Observable<any> {
    this.notifyDataLoading();
    const initDataSubscriptionSubject = new ReplaySubject<void>(1);
    this.loadStDiff().subscribe(() => {
      if (!this.ctx.aliasController) {
        this.configuredDatasources = deepClone(this.configuredDatasources);
        this.hasResolvedData = true;
        this.prepareDataSubscriptions().subscribe(
          () => {
            initDataSubscriptionSubject.next();
            initDataSubscriptionSubject.complete();
          }
        );
      } else {
        this.ctx.aliasController.resolveDatasources(this.configuredDatasources, this.singleEntity, this.pageSize).subscribe(
          {
            next: (datasources) => {
              this.configuredDatasources = datasources;
              this.prepareDataSubscriptions().subscribe(
                () => {
                  initDataSubscriptionSubject.next();
                  initDataSubscriptionSubject.complete();
                }
              );
            },
            error: (err) => {
              this.notifyDataLoaded();
              initDataSubscriptionSubject.error(err);
            }
          }
        );
      }
    });
    return initDataSubscriptionSubject.asObservable();
  }

  private prepareDataSubscriptions(): Observable<any> {
    if (this.hasDataPageLink || !this.configuredDatasources || !this.configuredDatasources.length) {
      this.hasResolvedData = true;
      this.notifyDataLoaded();
      return of(null);
    }
    if (this.comparisonEnabled) {
      const additionalDatasources: Datasource[] = [];
      this.configuredDatasources.forEach((datasource, datasourceIndex) => {
        const additionalDataKeys: DataKey[] = [];
        datasource.dataKeys.forEach((dataKey, dataKeyIndex) => {
          if (isDataKeySettingsWithComparison(dataKey.settings) && dataKey.settings.comparisonSettings.showValuesForComparison) {
            const additionalDataKey = deepClone(dataKey);
            additionalDataKey.isAdditional = true;
            additionalDataKey.origDataKeyIndex = dataKeyIndex;
            additionalDataKeys.push(additionalDataKey);
          }
        });
        if (additionalDataKeys.length) {
          const additionalDatasource: Datasource = deepClone(datasource);
          additionalDatasource.dataKeys = additionalDataKeys;
          additionalDatasource.isAdditional = true;
          additionalDatasource.origDatasourceIndex = datasourceIndex;
          additionalDatasources.push(additionalDatasource);
        }
      });
      this.configuredDatasources = this.configuredDatasources.concat(additionalDatasources);
    }
    const resolveResultObservables = this.configuredDatasources.map((datasource, index) => {
      const listener: EntityDataListener = {
        subscriptionType: this.type,
        useTimewindow: this.useTimewindow,
        configDatasource: datasource,
        configDatasourceIndex: index,
        dataLoaded: (pageData, data1, datasourceIndex, pageLink) => {
          this.dataLoaded(pageData, data1, datasourceIndex, pageLink, true);
        },
        initialPageDataChanged: this.initialPageDataChanged.bind(this),
        forceReInit: this.forceReInit.bind(this),
        dataUpdated: this.dataUpdated.bind(this),
        updateRealtimeSubscription: () => this.updateRealtimeSubscription(),
        setRealtimeSubscription: (subscriptionTimewindow) => {
          this.updateRealtimeSubscription(deepClone(subscriptionTimewindow));
        }
      };
      this.entityDataListeners.push(listener);
      return this.ctx.entityDataService.prepareSubscription(listener, this.ignoreDataUpdateOnIntervalTick);
    });
    return forkJoin(resolveResultObservables).pipe(
      map((resolveResults) => {
        resolveResults.forEach((resolveResult) => {
          if (resolveResult) {
            this.dataLoaded(resolveResult.pageData, resolveResult.data, resolveResult.datasourceIndex, resolveResult.pageLink, false);
          }
        });
        this.configureLoadedData();
        this.hasResolvedData = this.datasources.length > 0 || this.datasourcesOptional;
        this.updateDataTimewindow();
        this.notifyDataLoaded();
        this.onDataUpdated(true);
        if (this.hasLatestData) {
          this.onLatestDataUpdated(true);
        }
      })
    );
  }

  private resetData() {
    this.data.length = 0;
    this.latestData.length = 0;
    this.hiddenData.length = 0;
    if (this.displayLegend) {
      this.legendData.keys.length = 0;
      this.legendData.data.length = 0;
    }
    this.onDataUpdated();
    if (this.hasLatestData) {
      this.onLatestDataUpdated();
    }
  }

  getFirstEntityInfo(): SubscriptionEntityInfo {
    let entityId: EntityId;
    let entityName: string;
    let entityLabel: string;
    let entityDescription: string;
    if (this.type === widgetType.rpc) {
      if (this.targetEntityId) {
        entityId = this.targetEntityId;
        entityName = this.targetEntityName;
        entityLabel = this.targetEntityName;
      }
    } else if (this.type === widgetType.alarm) {
      if (this.alarmSource && this.alarmSource.entityType && this.alarmSource.entityId) {
        entityId = {
          entityType: this.alarmSource.entityType,
          id: this.alarmSource.entityId
        };
        entityName = this.alarmSource.entityName;
        entityLabel = this.alarmSource.entityLabel;
        entityDescription = this.alarmSource.entityDescription;
      } else if (this.alarms && this.alarms.data.length) {
        const data = this.alarms.data[0];
        entityId = data.originator;
        entityName = data.originatorName;
        entityLabel = data.originatorLabel;
        if (data.latest && data.latest[EntityKeyType.ENTITY_FIELD]) {
          const entityFields = data.latest[EntityKeyType.ENTITY_FIELD];
          const additionalInfoValue = entityFields.additionalInfo;
          if (additionalInfoValue) {
            const additionalInfo = additionalInfoValue.value;
            if (additionalInfo && additionalInfo.length) {
              try {
                const additionalInfoJson = JSON.parse(additionalInfo);
                if (additionalInfoJson && additionalInfoJson.description) {
                  entityDescription = additionalInfoJson.description;
                }
              } catch (e) {/**/}
            }
          }
        }
      }
    } else {
      for (const datasource of this.datasources) {
        if (datasource && datasource.entityType && datasource.entityId) {
          entityId = {
            entityType: datasource.entityType,
            id: datasource.entityId
          };
          entityName = datasource.entityName;
          entityLabel = datasource.entityLabel;
          entityDescription = datasource.entityDescription;
          break;
        }
      }
    }
    if (entityId) {
      return {
        entityId,
        entityName,
        entityLabel,
        entityDescription
      };
    } else {
      return null;
    }
  }

  onAliasesChanged(aliasIds: Array<string>): boolean {
    if (this.type === widgetType.rpc) {
      return this.checkRpcTarget(aliasIds);
    } else if (this.type === widgetType.alarm) {
      return this.checkAlarmSource(aliasIds);
    } else {
      return this.checkSubscriptions(aliasIds);
    }
  }

  onFiltersChanged(filterIds: Array<string>): boolean {
    if (this.type !== widgetType.rpc) {
      if (this.type === widgetType.alarm) {
        return this.checkAlarmSourceFilters(filterIds);
      } else {
        return this.checkSubscriptionsFilters(filterIds);
      }
    }
    return false;
  }

  private onDataUpdated(detectChanges?: boolean) {
    if (this.cafs.dataUpdated) {
      this.cafs.dataUpdated();
      this.cafs.dataUpdated = null;
    }
    this.cafs.dataUpdated = this.ctx.raf.raf(() => {
      try {
        this.callbacks.onDataUpdated(this, detectChanges);
      } catch (e) {
        this.callbacks.onDataUpdateError(this, e);
      }
    });
  }

  private onLatestDataUpdated(detectChanges?: boolean) {
    if (this.cafs.latestDataUpdated) {
      this.cafs.latestDataUpdated();
      this.cafs.latestDataUpdated = null;
    }
    this.cafs.latestDataUpdated = this.ctx.raf.raf(() => {
      try {
        this.callbacks.onLatestDataUpdated(this, detectChanges);
      } catch (e) {
        this.callbacks.onLatestDataUpdateError(this, e);
      }
    });
  }

  private onSubscriptionMessage(message: SubscriptionMessage) {
    if (this.cafs.message) {
      this.cafs.message();
      this.cafs.message = null;
    }
    this.cafs.message = this.ctx.raf.raf(() => {
      this.callbacks.onSubscriptionMessage(this, message);
    });
  }

  onDashboardTimewindowChanged(newDashboardTimewindow: Timewindow) {
    let doUpdate = false;
    let isTimewindowTypeChanged = false;
    if (this.useTimewindow) {
      if (this.useDashboardTimewindow) {
        if (this.type === widgetType.latest) {
          if (newDashboardTimewindow && this.timezone !== newDashboardTimewindow.timezone) {
            this.timezone = newDashboardTimewindow.timezone;
            doUpdate = this.updateTsOffset();
          }
        }
        if (!isEqual(this.timeWindowConfig, newDashboardTimewindow) && newDashboardTimewindow) {
          isTimewindowTypeChanged = timewindowTypeChanged(this.timeWindowConfig, newDashboardTimewindow);
          this.timeWindowConfig = deepClone(newDashboardTimewindow);
          doUpdate = true;
        }
      }
    } else if (this.type === widgetType.latest) {
      if (newDashboardTimewindow && this.timezone !== newDashboardTimewindow.timezone) {
        this.timezone = newDashboardTimewindow.timezone;
        doUpdate = this.updateTsOffset();
      }
    }
    if (doUpdate) {
      this.update(isTimewindowTypeChanged);
    }
  }

  updateDataVisibility(index: number): void {
    if (this.displayLegend) {
      const hidden = this.legendData.keys.find(key => key.dataIndex === index).dataKey.hidden;
      if (hidden) {
        this.hiddenData[index].data = this.data[index].data;
        this.data[index].data = [];
      } else {
        this.data[index].data = this.hiddenData[index].data;
        this.hiddenData[index].data = [];
      }
      this.onDataUpdated();
    }
  }

  updateTimewindowConfig(newTimewindow: Timewindow): void {
    if (!this.useDashboardTimewindow) {
      if (this.type === widgetType.latest) {
        if (newTimewindow && this.timezone !== newTimewindow.timezone) {
          this.timezone = newTimewindow.timezone;
          this.updateTsOffset();
        }
      }
      const isTimewindowTypeChanged = timewindowTypeChanged(this.timeWindowConfig, newTimewindow);
      this.timeWindowConfig = newTimewindow;
      this.update(isTimewindowTypeChanged);
    }
  }

  onResetTimewindow(): void {
    if (this.useDashboardTimewindow) {
      this.ctx.dashboardTimewindowApi.onResetTimewindow();
    } else {
      if (this.originalTimewindow) {
        const isTimewindowTypeChanged = timewindowTypeChanged(this.timeWindowConfig, this.originalTimewindow);
        this.timeWindowConfig = deepClone(this.originalTimewindow);
        this.originalTimewindow = null;
        this.callbacks.timeWindowUpdated(this, this.timeWindowConfig);
        this.update(isTimewindowTypeChanged);
      }
    }
  }

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number): void {
    if (this.useDashboardTimewindow) {
      this.ctx.dashboardTimewindowApi.onUpdateTimewindow(startTimeMs, endTimeMs);
    } else {
      if (!this.originalTimewindow) {
        this.originalTimewindow = deepClone(this.timeWindowConfig);
      }
      this.timeWindowConfig = toHistoryTimewindow(this.timeWindowConfig, startTimeMs, endTimeMs, interval, this.ctx.timeService);
      this.callbacks.timeWindowUpdated(this, this.timeWindowConfig);
      const isTimewindowTypeChanged = timewindowTypeChanged(this.timeWindowConfig, this.originalTimewindow);
      this.update(isTimewindowTypeChanged);
    }
  }

  sendOneWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any> {
    return this.sendCommand(true, method, params, timeout, persistent, persistentPollingInterval, retries, additionalInfo, requestUUID);
  }

  sendTwoWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any> {
    return this.sendCommand(false, method, params, timeout, persistent, persistentPollingInterval, retries, additionalInfo, requestUUID);
  }

  clearRpcError(): void {
    this.rpcRejection = null;
    this.rpcErrorText = null;
    this.callbacks.onRpcErrorCleared(this);
  }

  completedCommand(): void {
    this.executingSubjects.forEach(subject => {
      subject.next();
      subject.complete();
    });
  }

  sendCommand(oneWayElseTwoWay: boolean, method: string, params?: any, timeout?: number,
              persistent?: boolean, persistentPollingInterval?: number, retries?: number,
              additionalInfo?: any, requestUUID?: string): Observable<any> {
    if (!this.rpcEnabled) {
      this.rpcErrorText = this.rpcDisabledReason;
      this.rpcRejection = new Error(this.rpcErrorText);
      return throwError(() => this.rpcRejection);
    } else {
      if (this.rpcRejection && (!(this.rpcRejection as any).status || (this.rpcRejection as HttpErrorResponse).status !== 504)) {
        this.rpcRejection = null;
        this.rpcErrorText = null;
        this.callbacks.onRpcErrorCleared(this);
      }
      const requestBody: any = {
        method,
        params,
        persistent,
        retries,
        additionalInfo,
        requestUUID
      };
      if (timeout && timeout > 0) {
        requestBody.timeout = timeout;
      }
      const rpcSubject: Subject<any | void> = oneWayElseTwoWay ? new Subject<void>() : new Subject<any>();
      this.executingRpcRequest = true;
      this.callbacks.rpcStateChanged(this);
      if (this.ctx.utils.widgetEditMode) {
        setTimeout(() => {
          this.executingRpcRequest = false;
          this.callbacks.rpcStateChanged(this);
          if (oneWayElseTwoWay) {
            (rpcSubject as Subject<void>).next();
            rpcSubject.complete();
          } else {
            rpcSubject.next(requestBody);
            rpcSubject.complete();
          }
        }, 500);
      } else {
        this.executingSubjects.push(rpcSubject);
        (oneWayElseTwoWay ? this.ctx.deviceService.sendOneWayRpcCommand(this.targetDeviceId, requestBody) :
          this.ctx.deviceService.sendTwoWayRpcCommand(this.targetDeviceId, requestBody)).pipe(
            switchMap((response) => {
              if (persistent && persistentPollingInterval > 0) {
                const pollingInterval = Math.max(persistentPollingInterval, 1000);
                const initialTimeout = timeout ? Math.min(timeout + 1000, pollingInterval) : pollingInterval;
                return timer(initialTimeout, pollingInterval).pipe(
                  switchMap(() => this.ctx.deviceService.getPersistedRpc(response.rpcId, true)),
                  filter(persistentRespons =>
                    persistentRespons.status !== RpcStatus.DELIVERED && persistentRespons.status !== RpcStatus.QUEUED),
                  switchMap(persistentResponse => {
                    if ([RpcStatus.TIMEOUT, RpcStatus.EXPIRED].includes(persistentResponse.status)) {
                      return throwError(() => ({status: 504}));
                    } else if (persistentResponse.status === RpcStatus.FAILED) {
                      return throwError(() => ({status: 502, statusText: persistentResponse.response.error}));
                    } else {
                      return of(persistentResponse.response);
                    }
                  }),
                  takeUntil(rpcSubject)
                );
              }
              return of(response);
            })
        )
        .subscribe({
          next: (responseBody) => {
            this.rpcRejection = null;
            this.rpcErrorText = null;
            const index = this.executingSubjects.indexOf(rpcSubject);
            if (index >= 0) {
              this.executingSubjects.splice( index, 1 );
            }
            this.executingRpcRequest = this.executingSubjects.length > 0;
            this.callbacks.onRpcSuccess(this);
            rpcSubject.next(responseBody);
            rpcSubject.complete();
          },
          error: (rejection: HttpErrorResponse) => {
            const index = this.executingSubjects.indexOf(rpcSubject);
            if (index >= 0) {
              this.executingSubjects.splice( index, 1 );
            }
            this.executingRpcRequest = this.executingSubjects.length > 0;
            this.callbacks.rpcStateChanged(this);
            if (!this.executingRpcRequest || rejection.status === 504) {
              this.rpcRejection = rejection;
              if (rejection.status === 504) {
                this.rpcErrorText = this.ctx.translate.instant('rpc.error.request-timeout');
              } else {
                this.rpcErrorText =  this.ctx.translate.instant('rpc.error.rpc-http-error',
                  {status: rejection.status, statusText: rejection.statusText});
                const error = parseHttpErrorMessage(rejection, this.ctx.translate);
                if (error) {
                  this.rpcErrorText += '</br>';
                  this.rpcErrorText += error.message;
                }
              }
              this.callbacks.onRpcFailed(this);
            }
            rpcSubject.error(rejection);
          }
        });
      }
      return rpcSubject.asObservable();
    }
  }

  update(isTimewindowTypeChanged = false) {
    if (this.type !== widgetType.rpc) {
      this.widgetTimewindowChangedSubject.next(this.timeWindowConfig);
      if (this.type === widgetType.alarm) {
        this.updateAlarmDataSubscription();
      } else {
        if (this.type === widgetType.timeseries && this.options.comparisonEnabled && isTimewindowTypeChanged) {
          this.forceReInit();
        } else {
          if (this.hasDataPageLink) {
            this.updateDataSubscriptions();
          } else {
            this.notifyDataLoading();
            this.dataSubscribe();
          }
        }
      }
    }
  }

  subscribe(): void {
    if (!this.subscribed) {
      this.subscribed = true;
      if (this.cafs.subscribe) {
        this.cafs.subscribe();
        this.cafs.subscribe = null;
      }
      this.cafs.subscribe = this.ctx.raf.raf(() => {
        this.doSubscribe();
      });
    }
  }

  subscribeAllForPaginatedData(pageLink: EntityDataPageLink,
                               keyFilters: KeyFilter[]): Observable<any> {
    const observables: Observable<any>[] = [];
    this.configuredDatasources.forEach((_datasource, datasourceIndex) => {
      observables.push(this.subscribeForPaginatedData(datasourceIndex, pageLink, keyFilters));
    });
    if (observables.length) {
      return forkJoin(observables);
    } else {
      return of(null);
    }
  }

  stopSubscription(datasourceIndex: number) {
    const entityDataListener = this.entityDataListeners[datasourceIndex];
    if (entityDataListener) {
      this.ctx.entityDataService.stopSubscription(entityDataListener);
    }
  }

  subscribeForPaginatedData(datasourceIndex: number,
                            pageLink: EntityDataPageLink,
                            keyFilters: KeyFilter[]): Observable<any> {
    let entityDataListener = this.entityDataListeners[datasourceIndex];
    if (entityDataListener) {
      this.ctx.entityDataService.stopSubscription(entityDataListener);
    }
    const datasource = this.configuredDatasources[datasourceIndex];
    if (datasource) {
      if (this.useTimewindow && this.timeWindowConfig) {
        this.updateRealtimeSubscription();
      }
      entityDataListener = {
        subscriptionType: this.type,
        useTimewindow: this.useTimewindow,
        configDatasource: datasource,
        configDatasourceIndex: datasourceIndex,
        subscriptionTimewindow: this.subscriptionTimewindow,
        latestTsOffset: this.tsOffset,
        dataLoaded: (pageData, data1, datasourceIndex1, pageLink1) => {
          this.dataLoaded(pageData, data1, datasourceIndex1, pageLink1, true);
        },
        dataUpdated: this.dataUpdated.bind(this),
        updateRealtimeSubscription: () => this.updateRealtimeSubscription(),
        setRealtimeSubscription: (subscriptionTimewindow) => {
          this.updateRealtimeSubscription(deepClone(subscriptionTimewindow));
        }
      };
      this.entityDataListeners[datasourceIndex] = entityDataListener;
      return this.ctx.entityDataService.subscribeForPaginatedData(entityDataListener, pageLink, keyFilters,
                                                                  this.ignoreDataUpdateOnIntervalTick);
    } else {
      return of(null);
    }
  }

  subscribeForAlarms(pageLink: AlarmDataPageLink,
                     keyFilters: KeyFilter[]) {
    if (this.alarmDataListener) {
      this.ctx.alarmDataService.stopSubscription(this.alarmDataListener);
    }

    if (this.timeWindowConfig) {
      this.updateRealtimeSubscription();
    }

    this.alarmDataListener = {
      subscriptionTimewindow: this.subscriptionTimewindow,
      alarmSource: this.alarmSource,
      alarmsLoaded: this.alarmsLoaded.bind(this),
      alarmsUpdated: this.alarmsUpdated.bind(this)
    };

    this.alarms = emptyPageData();

    this.ctx.alarmDataService.subscribeForAlarms(this.alarmDataListener, pageLink, keyFilters);

    let forceUpdate = false;
    if (this.alarmSource.unresolvedStateEntity) {
      forceUpdate = true;
    }
    if (forceUpdate) {
      this.onDataUpdated();
    }
  }

  private doSubscribe() {
    if (this.type !== widgetType.rpc && this.type !== widgetType.alarm) {
      this.dataSubscribe();
    }
  }

  private updateDataTimewindow() {
    if (!this.hasDataPageLink) {
      if (this.useTimewindow && this.timeWindowConfig) {
        this.updateRealtimeSubscription();
        if (this.comparisonEnabled) {
          this.updateSubscriptionForComparison();
        }
      }
    }
  }

  private dataSubscribe() {
    this.updateDataTimewindow();
    if (!this.hasDataPageLink) {
      if (this.useTimewindow && this.timeWindowConfig && this.subscriptionTimewindow.fixedWindow) {
          this.onDataUpdated();
      }
      const forceUpdate = !this.datasources.length;
      const notifyDataLoaded = !this.entityDataListeners.filter((listener) => !!listener.subscription).length;
      this.entityDataListeners.forEach((listener) => {
        if (this.comparisonEnabled && listener.configDatasource.isAdditional) {
          listener.subscriptionTimewindow = this.timewindowForComparison;
        } else {
          listener.subscriptionTimewindow = this.subscriptionTimewindow;
          listener.latestTsOffset = this.tsOffset;
        }
        this.ctx.entityDataService.startSubscription(listener);
      });
      if (forceUpdate) {
        this.onDataUpdated();
      }
      if (notifyDataLoaded) {
        this.notifyDataLoaded();
      }
    }
  }

  unsubscribe() {
    if (this.type !== widgetType.rpc) {
      if (this.type === widgetType.alarm) {
        this.alarmsUnsubscribe();
      } else {
        this.entityDataListeners.forEach((listener) => {
          if (listener != null) {
            this.ctx.entityDataService.stopSubscription(listener);
          }
        });
        this.entityDataListeners.length = 0;
        this.resetData();
      }
    }
    this.subscribed = false;
  }

  private alarmsUnsubscribe() {
    if (this.alarmDataListener) {
      this.ctx.alarmDataService.stopSubscription(this.alarmDataListener);
      this.alarmDataListener = null;
    }
  }

  private checkRpcTarget(aliasIds: Array<string>): boolean {
    return this.targetDevice?.type === TargetDeviceType.entity &&
          aliasIds.indexOf(this.targetDevice.entityAliasId) > -1;
  }

  private checkAlarmSource(aliasIds: Array<string>): boolean {
    if (this.options.alarmSource && this.options.alarmSource.entityAliasId) {
      if (aliasIds.indexOf(this.options.alarmSource.entityAliasId) > -1) {
        this.updateAlarmSubscription();
      }
    }
    return false;
  }

  private checkAlarmSourceFilters(filterIds: Array<string>): boolean {
    if (this.options.alarmSource && this.options.alarmSource.filterId) {
      if (filterIds.indexOf(this.options.alarmSource.filterId) > -1) {
        this.updateAlarmSubscription();
      }
    }
    return false;
  }

  private updateAlarmSubscription() {
    this.alarmSource = this.options.alarmSource;
    if (!this.ctx.aliasController) {
      this.hasResolvedData = true;
      this.configureAlarmsData();
      this.updateAlarmDataSubscription();
    } else {
      this.ctx.aliasController.resolveAlarmSource(this.alarmSource).subscribe(
        {
          next: (alarmSource) => {
            this.alarmSource = alarmSource;
            if (alarmSource) {
              this.hasResolvedData = true;
            }
            this.configureAlarmsData();
            this.updateAlarmDataSubscription();
          },
          error: () => {
            this.notifyDataLoaded();
          }
        }
      );
    }
  }

  private updateAlarmDataSubscription() {
    if (this.alarmDataListener) {
      const pageLink = this.alarmDataListener.alarmDataSubscriptionOptions.pageLink;
      const keyFilters = this.alarmDataListener.alarmDataSubscriptionOptions.additionalKeyFilters;
      this.subscribeForAlarms(pageLink, keyFilters);
    }
  }

  private checkSubscriptions(aliasIds: Array<string>): boolean {
    let subscriptionsChanged = false;
    const datasources = this.options.datasources;
    if (datasources) {
      for (const datasource of datasources) {
        if (datasource.entityAliasId) {
          if (aliasIds.indexOf(datasource.entityAliasId) > -1) {
            subscriptionsChanged = true;
            break;
          }
        }
      }
    }
    if (subscriptionsChanged && this.hasDataPageLink) {
      subscriptionsChanged = false;
      this.updateDataSubscriptions();
    }
    return subscriptionsChanged;
  }

  private checkSubscriptionsFilters(filterIds: Array<string>): boolean {
    let subscriptionsChanged = false;
    const datasources = this.options.datasources;
    if (datasources) {
      for (const datasource of datasources) {
        if (datasource.filterId) {
          if (filterIds.indexOf(datasource.filterId) > -1) {
            subscriptionsChanged = true;
            break;
          }
        }
      }
    }
    if (subscriptionsChanged && this.hasDataPageLink) {
      subscriptionsChanged = false;
      this.updateDataSubscriptions();
    }
    return subscriptionsChanged;
  }

  private updateDataSubscriptions() {
    this.configuredDatasources = this.ctx.dashboardUtils.validateAndUpdateDatasources(this.options.datasources);
    if (!this.ctx.aliasController) {
      this.configuredDatasources = deepClone(this.configuredDatasources);
      this.hasResolvedData = true;
      this.prepareDataSubscriptions().subscribe(
        () => {
          this.updatePaginatedDataSubscriptions();
        }
      );
    } else {
      this.ctx.aliasController.resolveDatasources(this.configuredDatasources, this.singleEntity, this.pageSize).subscribe(
        {
          next: (datasources) => {
            this.configuredDatasources = datasources;
            this.prepareDataSubscriptions().subscribe(
              () => {
                this.updatePaginatedDataSubscriptions();
              }
            );
          },
          error: () => {
            this.notifyDataLoaded();
          }
        }
      );
    }
  }

  private updatePaginatedDataSubscriptions() {
    for (let datasourceIndex = 0; datasourceIndex < this.entityDataListeners.length; datasourceIndex++) {
      this.stopSubscription(datasourceIndex);
    }
    this.paginatedDataSubscriptionUpdated.emit();
    for (let datasourceIndex = 0; datasourceIndex < this.entityDataListeners.length; datasourceIndex++) {
      const entityDataListener = this.entityDataListeners[datasourceIndex];
      if (entityDataListener) {
        const pageLink = entityDataListener.subscriptionOptions.pageLink;
        const keyFilters = entityDataListener.subscriptionOptions.additionalKeyFilters;
        this.subscribeForPaginatedData(datasourceIndex, pageLink, keyFilters);
      }
    }
  }

  isDataResolved(): boolean {
    return this.hasResolvedData;
  }

  destroy(): void {
    this.unsubscribe();
    this.widgetTimewindowChangedSubject.complete();
    for (const cafId of Object.keys(this.cafs)) {
      if (this.cafs[cafId]) {
        this.cafs[cafId]();
        this.cafs[cafId] = null;
      }
    }
  }

  private notifyDataLoading() {
    this.loadingData = true;
    this.callbacks.dataLoading(this);
  }

  private notifyDataLoaded() {
    this.loadingData = false;
    this.callbacks.dataLoading(this);
  }

  private updateTimewindow() {
    this.timeWindow.interval = this.subscriptionTimewindow.aggregation.interval || 1000;
    this.timeWindow.timezone = this.subscriptionTimewindow.timezone;
    this.timeWindow.tsOffset = this.subscriptionTimewindow.tsOffset;
    if (this.subscriptionTimewindow.realtimeWindowMs) {
      if (this.subscriptionTimewindow.quickInterval) {
        const startEndTime = calculateIntervalStartEndTime(this.subscriptionTimewindow.quickInterval, this.subscriptionTimewindow.timezone);
        this.timeWindow.maxTime = startEndTime[1] + this.subscriptionTimewindow.tsOffset;
        this.timeWindow.minTime = startEndTime[0] + this.subscriptionTimewindow.tsOffset;
      } else {
        const now = moment().valueOf() + this.subscriptionTimewindow.tsOffset + this.timeWindow.stDiff;
        if (!this.timeWindow.maxTime || Math.abs(now - this.timeWindow.maxTime) > 500) {
          this.timeWindow.maxTime = now;
          this.timeWindow.maxTime -= this.timeWindow.maxTime % 1000;
          this.timeWindow.minTime = this.timeWindow.maxTime - this.subscriptionTimewindow.realtimeWindowMs;
        }
      }
    } else if (this.subscriptionTimewindow.fixedWindow) {
      this.timeWindow.maxTime = this.subscriptionTimewindow.fixedWindow.endTimeMs + this.subscriptionTimewindow.tsOffset;
      this.timeWindow.minTime = this.subscriptionTimewindow.fixedWindow.startTimeMs + this.subscriptionTimewindow.tsOffset;
    }
  }

  private updateTsOffset(): boolean {
    const newOffset = calculateTsOffset(this.timezone);
    if (this.tsOffset !== newOffset) {
      this.tsOffset = newOffset;
      return true;
    }
    return false;
  }

  private updateRealtimeSubscription(subscriptionTimewindow?: SubscriptionTimewindow): SubscriptionTimewindow {
    if (subscriptionTimewindow) {
      this.subscriptionTimewindow = subscriptionTimewindow;
    } else {
      this.subscriptionTimewindow =
        createSubscriptionTimewindow(this.timeWindowConfig, this.timeWindow.stDiff,
          this.stateData, this.ctx.timeService);
    }
    this.updateTimewindow();
    return this.subscriptionTimewindow;
  }

  private updateComparisonTimewindow() {
    this.comparisonTimeWindow.interval = this.timewindowForComparison.aggregation.interval || 1000;
    this.comparisonTimeWindow.timezone = this.timewindowForComparison.timezone;
    if (this.timewindowForComparison.fixedWindow) {
      this.comparisonTimeWindow.maxTime = this.timewindowForComparison.fixedWindow.endTimeMs + this.timewindowForComparison.tsOffset;
      this.comparisonTimeWindow.minTime = this.timewindowForComparison.fixedWindow.startTimeMs + this.timewindowForComparison.tsOffset;
    }
  }

  private updateSubscriptionForComparison(): SubscriptionTimewindow {
    this.timewindowForComparison = createTimewindowForComparison(this.subscriptionTimewindow, this.timeForComparison,
      this.comparisonCustomIntervalValue);
    this.updateComparisonTimewindow();
    return this.timewindowForComparison;
  }

  private initialPageDataChanged(nextPageData: PageData<EntityData>) {
    this.callbacks.onInitialPageDataChanged(this, nextPageData);
  }

  private forceReInit() {
    this.callbacks.forceReInit();
  }

  private dataLoaded(pageData: PageData<EntityData>,
                     data: Array<Array<DataSetHolder>>,
                     datasourceIndex: number, _pageLink: EntityDataPageLink, isUpdate: boolean) {
    const datasource = this.configuredDatasources[datasourceIndex];
    datasource.dataReceived = true;
    const datasources = pageData.data.map((entityData, index) =>
      this.entityDataToDatasource(datasource, entityData, index)
    );
    this.datasourcePages[datasourceIndex] = {
      data: datasources,
      hasNext: pageData.hasNext,
      totalElements: pageData.totalElements,
      totalPages: pageData.totalPages
    };
    const datasourceData = datasources.map((datasourceElement, index) =>
      this.entityDataToDatasourceData(datasourceElement, data[index])
    );
    this.dataPages[datasourceIndex] = {
      data: datasourceData,
      hasNext: pageData.hasNext,
      totalElements: pageData.totalElements,
      totalPages: pageData.totalPages
    };
    if (datasource.type === DatasourceType.entity &&
        pageData.hasNext && !this.singleEntity) {
      if (this.warnOnPageDataOverflow) {
        const message = this.ctx.translate.instant('widget.data-overflow',
          {count: pageData.data.length, total: pageData.totalElements});
        this.onSubscriptionMessage({
          severity: 'warn',
          message
        });
      }
    }
    if (isUpdate) {
      this.configureLoadedData();
      this.onDataUpdated(true);
      if (this.hasLatestData) {
        this.onLatestDataUpdated(true);
      }
    }
  }

  private configureLoadedData() {
    this.datasources.length = 0;
    this.data.length = 0;
    this.latestData.length = 0;
    this.hiddenData.length = 0;
    this.hasLatestData = false;
    if (this.displayLegend) {
      this.legendData.keys.length = 0;
      this.legendData.data.length = 0;
    }

    let dataKeyIndex = 0;
    let latestDataKeyIndex = 0;
    this.configuredDatasources.forEach((configuredDatasource, datasourceIndex) => {
        configuredDatasource.dataKeyStartIndex = dataKeyIndex;
        configuredDatasource.latestDataKeyStartIndex = latestDataKeyIndex;
        const datasourcesPage = this.datasourcePages[datasourceIndex];
        const datasourceDataPage = this.dataPages[datasourceIndex];
        if (datasourcesPage) {
          datasourcesPage.data.forEach((datasource, currentDatasourceIndex) => {
            datasource.dataKeys.forEach((dataKey, currentDataKeyIndex) => {
              const datasourceData = datasourceDataPage.data[currentDatasourceIndex][currentDataKeyIndex];
              this.data.push(datasourceData);
              this.hiddenData.push({data: []});
              if (this.displayLegend) {
                const decimals = isDefinedAndNotNull(dataKey.decimals) ? dataKey.decimals : this.decimals;
                const units = isNotEmptyTbUnits(dataKey.units) ? dataKey.units : this.units;
                const valueFormat = ValueFormatProcessor.fromSettings(this.ctx.unitService, {decimals, units})
                const legendKey: LegendKey = {
                  dataKey,
                  dataIndex: dataKeyIndex,
                  valueFormat
                };
                this.legendData.keys.push(legendKey);
                const legendKeyData: LegendKeyData = {
                  min: null,
                  max: null,
                  avg: null,
                  total: null,
                  latest: null,
                  hidden: false
                };
                this.legendData.data.push(legendKeyData);
              }
              dataKeyIndex++;
            });
            if (datasource.latestDataKeys && datasource.latestDataKeys.length) {
              this.hasLatestData = true;
              datasource.latestDataKeys.forEach((_dataKey, currentLatestDataKeyIndex) => {
                const currentDataKeyIndex = datasource.dataKeys.length + currentLatestDataKeyIndex;
                const datasourceData = datasourceDataPage.data[currentDatasourceIndex][currentDataKeyIndex];
                this.latestData.push(datasourceData);
                latestDataKeyIndex++;
              });
            }
            this.datasources.push(datasource);
          });
        }
      }
    );
    let index = 0;
    this.datasources.forEach((datasource) => {
      datasource.dataKeys.forEach((dataKey) => {
        if (datasource.generated || datasource.isAdditional) {
          dataKey.color = this.ctx.utils.getMaterialColor(index);
        }
        index++;
      });
      // if (datasource.latestDataKeys) {
      //   datasource.latestDataKeys.forEach((dataKey) => {
      //     if (datasource.generated || datasource.isAdditional) {
      //       // dataKey.color = this.ctx.utils.getMaterialColor(index);
      //     }
      //     // index++;
      //   });
      // }
    });
    if (this.comparisonEnabled) {
      this.datasourcePages.forEach(datasourcePage => {
        datasourcePage.data.forEach((datasource, dIndex) => {
          if (datasource.isAdditional) {
            const origDatasource = this.datasourcePages[datasource.origDatasourceIndex].data[dIndex];
            datasource.dataKeys.forEach((dataKey) => {
              const settings: DataKeySettingsWithComparison = dataKey.settings;
              if (settings.comparisonSettings.color) {
                dataKey.color = dataKey.settings.comparisonSettings.color;
              }
              const origDataKey = origDatasource.dataKeys[dataKey.origDataKeyIndex];
              (origDataKey.settings as DataKeySettingsWithComparison).comparisonSettings.color = dataKey.color;
            });
          }
        });
      });
    }
    if (this.caulculateLegendData) {
      this.data.forEach((dataSetHolder, keyIndex) => {
        this.updateLegend(keyIndex, dataSetHolder.data, false);
      });
      this.callbacks.legendDataUpdated(this, true);
    }
  }

  private entityDataToDatasourceData(datasource: Datasource, data: Array<DataSetHolder>): Array<DatasourceData> {
    let datasourceDataArray: Array<DatasourceData> = [];
    datasourceDataArray = datasourceDataArray.concat(datasource.dataKeys.map((dataKey, keyIndex) => {
      dataKey.hidden = !!dataKey.settings?.hideDataByDefault;
      dataKey.inLegend = dataKey.settings?.showInLegend ||
        (isUndefined(dataKey.settings?.showInLegend) && !dataKey.settings?.removeFromLegend);
      dataKey.label = this.ctx.utils.customTranslation(dataKey.label, dataKey.label);
      const datasourceData: DatasourceData = {
        datasource,
        dataKey,
        data: []
      };
      if (data && data[keyIndex] && data[keyIndex].data) {
        datasourceData.data = data[keyIndex].data;
      }
      return datasourceData;
    }));
    if (datasource.latestDataKeys) {
      datasourceDataArray = datasourceDataArray.concat(datasource.latestDataKeys.map((dataKey, latestKeyIndex) => {
        dataKey.label = this.ctx.utils.customTranslation(dataKey.label, dataKey.label);
        const datasourceData: DatasourceData = {
          datasource,
          dataKey,
          data: []
        };
        const keyIndex = datasource.dataKeys.length + latestKeyIndex;
        if (data && data[keyIndex] && data[keyIndex].data) {
          datasourceData.data = data[keyIndex].data;
        }
        return datasourceData;
      }));
    }

    const formattedDataArray = formattedDataFormDatasourceData(datasourceDataArray);
    const formattedData = flatFormattedData(formattedDataArray);

    datasource.dataKeys.forEach((dataKey) => {
      if (this.comparisonEnabled && dataKey.isAdditional && isDataKeySettingsWithComparison(dataKey.settings) &&
        dataKey.settings.comparisonSettings.comparisonValuesLabel) {
        dataKey.label = createLabelFromPattern(dataKey.settings.comparisonSettings.comparisonValuesLabel, formattedData);
      } else {
        if (this.comparisonEnabled && dataKey.isAdditional) {
          dataKey.label = dataKey.label + ' ' + this.ctx.translate.instant('legend.comparison-time-ago.' + this.timeForComparison);
        }
        dataKey.pattern = dataKey.label;
        dataKey.label = createLabelFromPattern(dataKey.pattern, formattedData);
      }
    });

    return datasourceDataArray;
  }

  private entityDataToDatasource(configDatasource: Datasource, entityData: EntityData, index: number): Datasource {
    const newDatasource = deepClone(configDatasource);
    const entityInfo = entityDataToEntityInfo(entityData);
    updateDatasourceFromEntityInfo(newDatasource, entityInfo);
    newDatasource.generated = index > 0;
    return newDatasource;
  }

  private dataUpdated(data: DataSetHolder, datasourceIndex: number, dataIndex: number, dataKeyIndex: number,
                      detectChanges: boolean, isLatest: boolean) {
    if (isLatest) {
      this.processLatestDataUpdated(data, datasourceIndex, dataIndex, dataKeyIndex, detectChanges);
    } else {
      this.processDataUpdated(data, datasourceIndex, dataIndex, dataKeyIndex, detectChanges);
    }
  }

  private processDataUpdated(data: DataSetHolder, datasourceIndex: number, dataIndex: number,
                             dataKeyIndex: number, detectChanges: boolean) {
    const configuredDatasource = this.configuredDatasources[datasourceIndex];
    const startIndex = configuredDatasource.dataKeyStartIndex;
    const dataKeysCount = configuredDatasource.dataKeys.length;
    const index = startIndex + dataIndex * dataKeysCount + dataKeyIndex;
    let update = true;
    let currentData: DataSetHolder;
    if (this.displayLegend && this.legendData.keys.find(key => key.dataIndex === index).dataKey.hidden) {
      currentData = this.hiddenData[index];
    } else {
      currentData = this.data[index];
    }
    if (this.type === widgetType.latest) {
      const prevData = currentData.data;
      if (!data.data.length && !prevData.length) {
        update = false;
      } else if (prevData && prevData[0] && prevData[0].length > 1 && data.data.length > 0) {
        const prevTs = prevData[0][0];
        const prevValue = prevData[0][1];
        if (prevTs === data.data[0][0] && prevValue === data.data[0][1] && data.data[0][1] !== NOT_SUPPORTED) {
          update = false;
        }
      }
    }
    if (update) {
      if (this.subscriptionTimewindow && this.subscriptionTimewindow.realtimeWindowMs) {
        this.updateTimewindow();
        if (this.timewindowForComparison && this.timewindowForComparison.realtimeWindowMs) {
          this.updateComparisonTimewindow();
        }
      }
      currentData.data = data.data;
      if (this.caulculateLegendData) {
        this.updateLegend(index, data.data, detectChanges);
      }
      this.notifyDataLoaded();
      this.onDataUpdated(detectChanges);
    } else if (this.loadingData) {
      this.notifyDataLoaded();
    }
  }

  private processLatestDataUpdated(data: DataSetHolder, datasourceIndex: number, dataIndex: number,
                                   dataKeyIndex: number, detectChanges: boolean) {
    const configuredDatasource = this.configuredDatasources[datasourceIndex];
    const startIndex = configuredDatasource.latestDataKeyStartIndex;
    const dataKeysCount = configuredDatasource.latestDataKeys.length;
    const index = startIndex + dataIndex * dataKeysCount + dataKeyIndex - configuredDatasource.dataKeys.length;
    let update = true;
    const currentData = this.latestData[index];
    const prevData = currentData.data;
    if (!data.data.length) {
      update = false;
    } else if (prevData && prevData[0] && prevData[0].length > 1 && data.data.length > 0) {
      const prevTs = prevData[0][0];
      const prevValue = prevData[0][1];
      if (prevTs === data.data[0][0] && prevValue === data.data[0][1]) {
        update = false;
      }
    }
    if (update) {
      currentData.data = data.data;
      this.onLatestDataUpdated(detectChanges);
    }
  }

  private alarmsLoaded(alarms: PageData<AlarmData>, allowedEntities: number, totalEntities: number) {
    this.alarms = alarms;
    if (totalEntities > allowedEntities) {
      const message = this.ctx.translate.instant('widget.alarm-data-overflow',
        { allowedEntities, totalEntities });
      this.onSubscriptionMessage({
        severity: 'warn',
        message
      });
    }
    if (this.subscriptionTimewindow && this.subscriptionTimewindow.realtimeWindowMs) {
      this.updateTimewindow();
    }
    this.onDataUpdated();
  }

  private alarmsUpdated(_updated: Array<AlarmData>, alarms: PageData<AlarmData>) {
    this.alarmsLoaded(alarms, 0, 0);
  }

  private updateLegend(dataIndex: number, data: DataSet, detectChanges: boolean) {
    const valueFormat = this.legendData.keys.find(key => key.dataIndex === dataIndex).valueFormat;
    const legendKeyData = this.legendData.data[dataIndex];
    if (this.legendConfig.showMin) {
      legendKeyData.min = valueFormat.format(calculateMin(data));
    }
    if (this.legendConfig.showMax) {
      legendKeyData.max = valueFormat.format(calculateMax(data));
    }
    if (this.legendConfig.showAvg) {
      legendKeyData.avg = valueFormat.format(calculateAvg(data));
    }
    if (this.legendConfig.showTotal) {
      legendKeyData.total = valueFormat.format(calculateTotal(data));
    }
    if (this.legendConfig.showLatest) {
      legendKeyData.latest = valueFormat.format(calculateLatest(data));
    }
    this.callbacks.legendDataUpdated(this, detectChanges !== false);
  }

  private loadStDiff(): Observable<any> {
    const loadSubject = new ReplaySubject<void>(1);
    if (this.ctx.getServerTimeDiff && this.timeWindow) {
      this.ctx.getServerTimeDiff().subscribe(
        {
          next: (stDiff) => {
            this.timeWindow.stDiff = stDiff;
            loadSubject.next();
            loadSubject.complete();
          },
          error: () => {
            this.timeWindow.stDiff = 0;
            loadSubject.next();
            loadSubject.complete();
          }
        }
      );
    } else {
      if (this.timeWindow) {
        this.timeWindow.stDiff = 0;
      }
      loadSubject.next();
      loadSubject.complete();
    }
    return loadSubject.asObservable();
  }
}
