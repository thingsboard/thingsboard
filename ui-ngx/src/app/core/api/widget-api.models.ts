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

import { Observable } from 'rxjs';
import { EntityId } from '@app/shared/models/id/entity-id';
import {
  WidgetActionDescriptor,
  widgetType,
  LegendConfig,
  LegendData,
  Datasource,
  DatasourceData, DataSet, DatasourceType, KeyInfo
} from '@shared/models/widget.models';
import { TimeService } from '../services/time.service';
import { DeviceService } from '../http/device.service';
import { AlarmService } from '../http/alarm.service';
import { UtilsService } from '@core/services/utils.service';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { HttpErrorResponse } from '@angular/common/http';
import { DatasourceService } from '@core/api/datasource.service';

export interface TimewindowFunctions {
  onUpdateTimewindow: (startTimeMs: number, endTimeMs: number, interval: number) => void;
  onResetTimewindow: () => void;
}

export interface WidgetSubscriptionApi {
  createSubscription: (options: WidgetSubscriptionOptions, subscribe?: boolean) => Observable<IWidgetSubscription>;
  createSubscriptionFromInfo: (type: widgetType, subscriptionsInfo: Array<SubscriptionInfo>,
                               options: WidgetSubscriptionOptions, useDefaultComponents: boolean, subscribe: boolean)
    => Observable<IWidgetSubscription>;
  removeSubscription: (id: string) => void;
}

export interface RpcApi {
  sendOneWayCommand: (method: string, params?: any, timeout?: number) => Observable<any>;
  sendTwoWayCommand: (method: string, params?: any, timeout?: number) => Observable<any>;
}

export interface IWidgetUtils {
  formatValue: (value: any, dec?: number, units?: string, showZeroDecimals?: boolean) => string | undefined;
}

export interface WidgetActionsApi {
  actionDescriptorsBySourceId: {[sourceId: string]: Array<WidgetActionDescriptor>};
  getActionDescriptors: (actionSourceId: string) => Array<WidgetActionDescriptor>;
  handleWidgetAction: ($event: Event, descriptor: WidgetActionDescriptor,
                       entityId?: EntityId, entityName?: string, additionalParams?: any) => void;
  elementClick: ($event: Event) => void;
}

export interface AliasInfo {
  stateEntity?: boolean;
  currentEntity?: {
    id: string;
    entityType: EntityType;
    name?: string;
  };
  [key: string]: any | null;
  // TODO:
}

export interface IAliasController {
  entityAliasesChanged: Observable<Array<string>>;
  getAliasInfo(aliasId): Observable<AliasInfo>;
  resolveDatasources(datasources: Array<Datasource>): Observable<Array<Datasource>>;
  [key: string]: any | null;
  // TODO:
}

export interface StateObject {
  id?: string;
  params?: StateParams;
}

export interface StateParams {
  entityName?: string;
  targetEntityParamName?: string;
  entityId?: EntityId;
  [key: string]: any | null;
}

export interface IStateController {
  getStateParams: () => StateParams;
  openState: (id: string, params?: StateParams, openRightLayout?: boolean) => void;
  updateState: (id?: string, params?: StateParams, openRightLayout?: boolean) => void;
  // TODO:
}

export interface EntityInfo {
  entityId: EntityId;
  entityName: string;
}

export interface SubscriptionInfo {
  type: DatasourceType;
  name?: string;
  entityType?: EntityType;
  entityId?: string;
  entityIds?: Array<string>;
  entityName?: string;
  entityNamePrefix?: string;
  timeseries?: Array<KeyInfo>;
  attributes?: Array<KeyInfo>;
  functions?: Array<KeyInfo>;
  alarmFields?: Array<KeyInfo>;
  [key: string]: any;
}

export interface WidgetSubscriptionContext {
  timeService: TimeService;
  deviceService: DeviceService;
  alarmService: AlarmService;
  datasourceService: DatasourceService;
  utils: UtilsService;
  widgetUtils: IWidgetUtils;
  dashboardTimewindowApi: TimewindowFunctions;
  getServerTimeDiff: () => Observable<number>;
  aliasController: IAliasController;
  [key: string]: any;
  // TODO:
}

export interface WidgetSubscriptionCallbacks {
  onDataUpdated?: (subscription: IWidgetSubscription) => void;
  onDataUpdateError?: (subscription: IWidgetSubscription, e: any) => void;
  dataLoading?: (subscription: IWidgetSubscription) => void;
  legendDataUpdated?: (subscription: IWidgetSubscription) => void;
  timeWindowUpdated?: (subscription: IWidgetSubscription, timeWindowConfig: Timewindow) => void;
  rpcStateChanged?: (subscription: IWidgetSubscription) => void;
  onRpcSuccess?: (subscription: IWidgetSubscription) => void;
  onRpcFailed?: (subscription: IWidgetSubscription) => void;
  onRpcErrorCleared?: (subscription: IWidgetSubscription) => void;
}

export interface WidgetSubscriptionOptions {
  type: widgetType;
  stateData?: boolean;
  alarmSource?: Datasource;
  alarmSearchStatus?: AlarmSearchStatus;
  alarmsPollingInterval?: number;
  datasources?: Array<Datasource>;
  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timeWindowConfig?: Timewindow;
  dashboardTimewindow?: Timewindow;
  legendConfig?: LegendConfig;
  decimals?: number;
  units?: string;
  callbacks?: WidgetSubscriptionCallbacks;
  [key: string]: any;
  // TODO:
}

export interface IWidgetSubscription {

  id: string;
  init$: Observable<IWidgetSubscription>;
  ctx: WidgetSubscriptionContext;
  type: widgetType;
  callbacks: WidgetSubscriptionCallbacks;

  loadingData: boolean;
  useDashboardTimewindow: boolean;

  legendData: LegendData;
  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindow?: Timewindow;

  alarmSource?: Datasource;
  alarmSearchStatus?: AlarmSearchStatus;
  alarmsPollingInterval?: number;

  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;

  rpcEnabled?: boolean;
  executingRpcRequest?: boolean;
  rpcErrorText?: string;
  rpcRejection?: HttpErrorResponse;

  getFirstEntityInfo(): EntityInfo;

  onAliasesChanged(aliasIds: Array<string>): boolean;

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval: number): void;
  onResetTimewindow(): void;
  updateTimewindowConfig(newTimewindow: Timewindow): void;

  sendOneWayCommand(method: string, params?: any, timeout?: number): Observable<any>;
  sendTwoWayCommand(method: string, params?: any, timeout?: number): Observable<any>;
  clearRpcError(): void;

  subscribe(): void;

  destroy(): void;

  [key: string]: any;
  // TODO:
}
