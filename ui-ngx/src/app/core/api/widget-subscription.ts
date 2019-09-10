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

import {
  EntityInfo,
  IWidgetSubscription,
  WidgetSubscriptionCallbacks,
  WidgetSubscriptionContext,
  WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import {
  DataSet,
  Datasource,
  DatasourceData,
  LegendConfig,
  LegendData,
  LegendKey,
  LegendKeyData,
  widgetType
} from '@app/shared/models/widget.models';
import { HttpErrorResponse } from '@angular/common/http';
import { Timewindow } from '@app/shared/models/time/time.models';
import { Observable, of, ReplaySubject, Subject } from 'rxjs';
import { CancelAnimationFrame } from '@core/services/raf.service';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmInfo, AlarmSearchStatus } from '@shared/models/alarm.models';
import { deepClone, isDefined } from '@core/utils';
import { AlarmSourceListener } from '@core/http/alarm.service';
import { DatasourceListener } from '@core/api/datasource.service';

export class WidgetSubscription implements IWidgetSubscription {

  id: string;
  ctx: WidgetSubscriptionContext;
  type: widgetType;
  callbacks: WidgetSubscriptionCallbacks;

  timeWindow: Timewindow;
  originalTimewindow: Timewindow;
  timeWindowConfig: Timewindow;
  subscriptionTimewindow: Timewindow;
  useDashboardTimewindow: boolean;

  data: Array<DatasourceData>;
  datasources: Array<Datasource>;
  datasourceListeners: Array<DatasourceListener>;
  hiddenData: Array<{ data: DataSet }>;
  legendData: LegendData;
  legendConfig: LegendConfig;
  caulculateLegendData: boolean;
  displayLegend: boolean;
  stateData: boolean;
  decimals: number;
  units: string;

  alarms: Array<AlarmInfo>;
  alarmSource: Datasource;
  alarmSearchStatus: AlarmSearchStatus;
  alarmsPollingInterval: number;
  alarmSourceListener: AlarmSourceListener;

  loadingData: boolean;

  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;

  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse;

  init$: Observable<IWidgetSubscription>;

  cafs: {[cafId: string]: CancelAnimationFrame} = {};

  targetDeviceAliasId: string;
  targetDeviceId: string;
  targetDeviceName: string;
  executingSubjects: Array<Subject<any>>;

  constructor(subscriptionContext: WidgetSubscriptionContext, options: WidgetSubscriptionOptions) {
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

      this.targetDeviceAliasIds = options.targetDeviceAliasIds;
      this.targetDeviceIds = options.targetDeviceIds;

      this.targetDeviceAliasId = null;
      this.targetDeviceId = null;

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
      this.callbacks.dataLoading = this.callbacks.dataLoading || (() => {});
      this.callbacks.timeWindowUpdated = this.callbacks.timeWindowUpdated || (() => {});
      this.alarmSource = options.alarmSource;
      this.alarmSearchStatus = isDefined(options.alarmSearchStatus) ?
        options.alarmSearchStatus : AlarmSearchStatus.ANY;
      this.alarmsPollingInterval = isDefined(options.alarmsPollingInterval) ?
        options.alarmsPollingInterval : 5000;
      this.alarmSourceListener = null;
      this.alarms = [];
      this.originalTimewindow = null;
      this.timeWindow = {};
      this.useDashboardTimewindow = options.useDashboardTimewindow;
      if (this.useDashboardTimewindow) {
        this.timeWindowConfig = deepClone(options.dashboardTimewindow);
      } else {
        this.timeWindowConfig = deepClone(options.timeWindowConfig);
      }
      this.subscriptionTimewindow = null;
      this.loadingData = false;
      this.displayLegend = false;
      this.initAlarmSubscription().subscribe(() => {
        subscriptionSubject.next(this);
        subscriptionSubject.complete();
      },
      () => {
        subscriptionSubject.error(null);
      });
    } else {
      this.callbacks.onDataUpdated = this.callbacks.onDataUpdated || (() => {});
      this.callbacks.onDataUpdateError = this.callbacks.onDataUpdateError || (() => {});
      this.callbacks.dataLoading = this.callbacks.dataLoading || (() => {});
      this.callbacks.legendDataUpdated = this.callbacks.legendDataUpdated || (() => {});
      this.callbacks.timeWindowUpdated = this.callbacks.timeWindowUpdated || (() => {});

      this.datasources = this.ctx.utils.validateDatasources(options.datasources);
      this.datasourceListeners = [];
      this.data = [];
      this.hiddenData = [];
      this.originalTimewindow = null;
      this.timeWindow = {};
      this.useDashboardTimewindow = options.useDashboardTimewindow;
      this.stateData = options.stateData;
      if (this.useDashboardTimewindow) {
        this.timeWindowConfig = deepClone(options.dashboardTimewindow);
      } else {
        this.timeWindowConfig = deepClone(options.timeWindowConfig);
      }

      this.subscriptionTimewindow = null;

      this.units = options.units || '';
      this.decimals = isDefined(options.decimals) ? options.decimals : 2;

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
          this.legendConfig.showTotal === true);
      this.initDataSubscription().subscribe(() => {
          subscriptionSubject.next(this);
          subscriptionSubject.complete();
        },
        () => {
          subscriptionSubject.error(null);
        });
    }
 }

  private initRpc(): Observable<any> {
    const initRpcSubject = new ReplaySubject();
    if (this.targetDeviceAliasIds && this.targetDeviceAliasIds.length > 0) {
      this.targetDeviceAliasId = this.targetDeviceAliasIds[0];
      this.ctx.aliasController.getAliasInfo(this.targetDeviceAliasId).subscribe(
        (aliasInfo) => {
          if (aliasInfo.currentEntity && aliasInfo.currentEntity.entityType === EntityType.DEVICE) {
            this.targetDeviceId = aliasInfo.currentEntity.id;
            this.targetDeviceName = aliasInfo.currentEntity.name;
            if (this.targetDeviceId) {
              this.rpcEnabled = true;
            } else {
              this.rpcEnabled = this.ctx.utils.widgetEditMode ? true : false;
            }
            this.callbacks.rpcStateChanged(this);
            initRpcSubject.next();
            initRpcSubject.complete();
          } else {
            this.rpcEnabled = false;
            this.callbacks.rpcStateChanged(this);
            initRpcSubject.next();
            initRpcSubject.complete();
          }
        },
        () => {
          this.rpcEnabled = false;
          this.callbacks.rpcStateChanged(this);
          initRpcSubject.next();
          initRpcSubject.complete();
        }
      );
    } else {
      if (this.targetDeviceIds && this.targetDeviceIds.length > 0) {
        this.targetDeviceId = this.targetDeviceIds[0];
      }
      if (this.targetDeviceId) {
        this.rpcEnabled = true;
      } else {
        this.rpcEnabled = this.ctx.utils.widgetEditMode ? true : false;
      }
      this.callbacks.rpcStateChanged(this);
      initRpcSubject.next();
      initRpcSubject.complete();
    }
    return initRpcSubject.asObservable();
  }

  private initAlarmSubscription(): Observable<any> {
    // TODO:
    return of(null);
  }

  private initDataSubscription(): Observable<any> {
    const initDataSubscriptionSubject = new ReplaySubject(1);
    this.loadStDiff().subscribe(() => {
      if (!this.ctx.aliasController) {
        this.configureData();
        initDataSubscriptionSubject.next();
        initDataSubscriptionSubject.complete();
      } else {
        this.ctx.aliasController.resolveDatasources(this.datasources).subscribe(
          (datasources) => {
            this.datasources = datasources;
            this.configureData();
            initDataSubscriptionSubject.next();
            initDataSubscriptionSubject.complete();
          },
          () => {
            initDataSubscriptionSubject.error(null);
          }
        );
      }
    });
    return initDataSubscriptionSubject.asObservable();
  }

  private configureData() {
    let dataIndex = 0;
    this.datasources.forEach((datasource) => {
      datasource.dataKeys.forEach((dataKey) => {
        dataKey.hidden = false;
        dataKey.pattern = dataKey.label;
        const datasourceData: DatasourceData = {
          datasource,
          dataKey,
          data: []
        };
        this.data.push(datasourceData);
        this.hiddenData.push({data: []});
        if (this.displayLegend) {
          const legendKey: LegendKey = {
            dataKey,
            dataIndex: dataIndex++
          };
          this.legendData.keys.push(legendKey);
          const legendKeyData: LegendKeyData = {
            min: null,
            max: null,
            avg: null,
            total: null,
            hidden: false
          };
          this.legendData.data.push(legendKeyData);
        }
      });
    });
    if (this.displayLegend) {
      this.legendData.keys = this.legendData.keys.sort((key1, key2) => key1.dataKey.label.localeCompare(key2.dataKey.label));
      // TODO:
    }
    if (this.type === widgetType.timeseries) {
      if (this.useDashboardTimewindow) {
        // TODO:
      } else {
        // TODO:
      }
    }
  }

  getFirstEntityInfo(): EntityInfo {
    return undefined;
  }

  updateTimewindowConfig(newTimewindow: Timewindow): void {
  }

  onResetTimewindow(): void {
  }

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval: number): void {
  }

  sendOneWayCommand(method: string, params?: any, timeout?: number): Observable<any> {
    return undefined;
  }

  sendTwoWayCommand(method: string, params?: any, timeout?: number): Observable<any> {
    return undefined;
  }

  clearRpcError(): void {
  }

  subscribe(): void {
    // TODO:
    this.notifyDataLoaded();
  }

  destroy(): void {
  }

  private notifyDataLoading() {
    this.loadingData = true;
    this.callbacks.dataLoading(this);
  }

  private notifyDataLoaded() {
    this.loadingData = false;
    this.callbacks.dataLoading(this);
  }

  onAliasesChanged(aliasIds: Array<string>): boolean {
    return false;
  }

  private loadStDiff(): Observable<any> {
    const loadSubject = new ReplaySubject(1);
    if (this.ctx.getServerTimeDiff && this.timeWindow) {
      this.ctx.getServerTimeDiff().subscribe(
        (stDiff) => {
          this.timeWindow.stDiff = stDiff;
          loadSubject.next();
          loadSubject.complete();
        },
        () => {
          this.timeWindow.stDiff = 0;
          loadSubject.next();
          loadSubject.complete();
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
