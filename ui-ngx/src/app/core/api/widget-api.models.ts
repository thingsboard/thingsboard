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

import { Observable } from 'rxjs';
import { EntityId } from '@app/shared/models/id/entity-id';
import {
  DataSet,
  Datasource,
  DatasourceData,
  DatasourceType,
  KeyInfo,
  LegendConfig,
  LegendData,
  TargetDevice,
  WidgetAction,
  WidgetActionDescriptor,
  widgetType
} from '@shared/models/widget.models';
import { TimeService } from '../services/time.service';
import { DeviceService } from '../http/device.service';
import { UtilsService } from '@core/services/utils.service';
import {
  ComparisonDuration,
  SubscriptionTimewindow,
  Timewindow,
  WidgetTimewindow
} from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import { EntityAliases } from '@shared/models/alias.models';
import { EntityInfo } from '@app/shared/models/entity.models';
import { IDashboardComponent } from '@home/models/dashboard-component.models';
import {
  AlarmData,
  AlarmDataPageLink,
  EntityData,
  EntityDataPageLink,
  EntityFilter,
  Filter,
  FilterInfo,
  Filters,
  KeyFilter
} from '@shared/models/query/query.models';
import { EntityDataService } from '@core/api/entity-data.service';
import { PageData } from '@shared/models/page/page-data';
import { TranslateService } from '@ngx-translate/core';
import { AlarmDataService } from '@core/api/alarm-data.service';
import { IDashboardController } from '@home/components/dashboard-page/dashboard-page.models';
import { PopoverPlacement } from '@shared/components/popover.models';
import { PersistentRpc } from '@shared/models/rpc.models';
import { EventEmitter } from '@angular/core';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { MatDialogRef } from '@angular/material/dialog';
import { TbUnit } from '@shared/models/unit.models';
import { UnitService } from '@core/services/unit.service';

export interface TimewindowFunctions {
  onUpdateTimewindow: (startTimeMs: number, endTimeMs: number, interval?: number) => void;
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
  sendOneWayCommand: (method: string, params?: any, timeout?: number, persistent?: boolean,
                      persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string) => Observable<any>;
  sendTwoWayCommand: (method: string, params?: any, timeout?: number, persistent?: boolean,
                      persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string) => Observable<any>;
  completedCommand: () => void;
}

export interface IWidgetUtils {
  formatValue: (value: any, dec?: number, units?: string, showZeroDecimals?: boolean) => string | undefined;
  getEntityDetailsPageURL: (id: string, entityType: EntityType) => string;
}

export interface PlaceMapItemActionData {
  action: WidgetAction | WidgetActionDescriptor;
  additionalParams?: any;
  afterPlaceItemCallback: ($event: Event, descriptor: WidgetAction, entityId?: EntityId, entityName?: string,
                           additionalParams?: any, entityLabel?: string) => void;
}

export interface WidgetActionsApi {
  actionDescriptorsBySourceId: {[sourceId: string]: Array<WidgetActionDescriptor>};
  getActionDescriptors: (actionSourceId: string) => Array<WidgetActionDescriptor>;
  handleWidgetAction: ($event: Event, descriptor: WidgetAction,
                       entityId?: EntityId, entityName?: string, additionalParams?: any, entityLabel?: string) => void;
  onWidgetAction: ($event: Event, action: WidgetAction) => void;
  elementClick: ($event: Event) => void;
  cardClick: ($event: Event) => void;
  click: ($event: Event) => void;
  getActiveEntityInfo: () => SubscriptionEntityInfo;
  openDashboardStateInSeparateDialog: (targetDashboardStateId: string, params?: StateParams, dialogTitle?: string,
                                       hideDashboardToolbar?: boolean, dialogWidth?: number, dialogHeight?: number) => MatDialogRef<any>;
  openDashboardStateInPopover: ($event: Event, targetDashboardStateId: string, params?: StateParams,
                                hideDashboardToolbar?: boolean, preferredPlacement?: PopoverPlacement,
                                hideOnClickOutside?: boolean, popoverWidth?: string,
                                popoverHeight?: string, popoverStyle?: { [klass: string]: any }) => void;
  placeMapItem: (action: PlaceMapItemActionData) => void;
}

export interface AliasInfo {
  alias?: string;
  stateEntity?: boolean;
  entityFilter?: EntityFilter;
  currentEntity?: EntityInfo;
  entityParamName?: string;
  resolveMultiple?: boolean;
}

export interface StateEntityInfo {
  entityParamName: string;
  entityId: EntityId;
}

export interface IAliasController {
  entityAliasesChanged: Observable<Array<string>>;
  entityAliasResolved: Observable<string>;
  filtersChanged: Observable<Array<string>>;
  getAliasInfo(aliasId: string): Observable<AliasInfo>;
  getEntityAliasId(aliasName: string): string;
  getInstantAliasInfo(aliasId: string): AliasInfo;
  resolveSingleEntityInfo(aliasId: string): Observable<EntityInfo>;
  resolveSingleEntityInfoForDeviceId(deviceId: string): Observable<EntityInfo>;
  resolveSingleEntityInfoForTargetDevice(targetDevice: TargetDevice): Observable<EntityInfo>;
  resolveDatasources(datasources: Array<Datasource>, singleEntity?: boolean, pageSize?: number): Observable<Array<Datasource>>;
  resolveAlarmSource(alarmSource: Datasource): Observable<Datasource>;
  getEntityAliases(): EntityAliases;
  getFilters(): Filters;
  getUserFilters(): Filters;
  getFilterInfo(filterId: string): FilterInfo;
  getKeyFilters(filterId: string): Array<KeyFilter>;
  updateCurrentAliasEntity(aliasId: string, currentEntity: EntityInfo): void;
  updateUserFilter(filter: Filter): void;
  updateEntityAliases(entityAliases: EntityAliases): void;
  updateFilters(filters: Filters): void;
  updateAliases(aliasIds?: Array<string>): void;
  dashboardStateChanged(): void;
}

export interface StateObject {
  id?: string;
  params?: StateParams;
}

export interface StateParams {
  entityName?: string;
  entityLabel?: string;
  targetEntityParamName?: string;
  entityId?: EntityId;
  [key: string]: any | null;
}

export type StateControllerHolder = () => IStateController;

export interface IStateController {
  dashboardCtrl: IDashboardController;
  getStateParams(): StateParams;
  stateChanged(): Observable<string>;
  stateId(): Observable<string>;
  getStateParamsByStateId(stateId: string): StateParams;
  openState(id: string, params?: StateParams, openRightLayout?: boolean): void;
  updateState(id?: string, params?: StateParams, openRightLayout?: boolean): void;
  resetState(): void;
  openRightLayout(): void;
  preserveState(): void;
  cleanupPreservedStates(): void;
  navigatePrevState(index: number, params?: StateParams): void;
  getStateId(): string;
  getStateIndex(): number;
  getStateIdAtIndex(index: number): string;
  getEntityId(entityParamName: string): EntityId;
  getCurrentStateName(): string;
  reInit(): void;
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

  deviceId?: string;
  deviceName?: string;
  deviceNamePrefix?: string;
  deviceIds?: Array<string>;
  pageSize?: number;
}

export class WidgetSubscriptionContext {

  constructor(private dashboard: IDashboardComponent) {}

  get aliasController(): IAliasController {
    return this.dashboard.aliasController;
  }

  dashboardTimewindowApi: TimewindowFunctions = {
    onResetTimewindow: this.dashboard.onResetTimewindow.bind(this.dashboard),
    onUpdateTimewindow: this.dashboard.onUpdateTimewindow.bind(this.dashboard)
  };

  timeService: TimeService;
  deviceService: DeviceService;
  translate: TranslateService;
  entityDataService: EntityDataService;
  alarmDataService: AlarmDataService;
  utils: UtilsService;
  dashboardUtils: DashboardUtilsService;
  raf: RafService;
  unitService: UnitService;
  widgetUtils: IWidgetUtils;
  getServerTimeDiff: () => Observable<number>;
}

export type SubscriptionMessageSeverity = 'info' | 'warn' | 'error' | 'success';

export interface SubscriptionMessage {
  severity: SubscriptionMessageSeverity;
  message: string;
}

export interface WidgetSubscriptionCallbacks {
  onDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  onLatestDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  onDataUpdateError?: (subscription: IWidgetSubscription, e: any) => void;
  onLatestDataUpdateError?: (subscription: IWidgetSubscription, e: any) => void;
  onSubscriptionMessage?: (subscription: IWidgetSubscription, message: SubscriptionMessage) => void;
  onInitialPageDataChanged?: (subscription: IWidgetSubscription, nextPageData: PageData<EntityData>) => void;
  forceReInit?: () => void;
  dataLoading?: (subscription: IWidgetSubscription) => void;
  legendDataUpdated?: (subscription: IWidgetSubscription, detectChanges: boolean) => void;
  timeWindowUpdated?: (subscription: IWidgetSubscription, timeWindowConfig: Timewindow) => void;
  rpcStateChanged?: (subscription: IWidgetSubscription) => void;
  onRpcSuccess?: (subscription: IWidgetSubscription) => void;
  onRpcFailed?: (subscription: IWidgetSubscription) => void;
  onRpcErrorCleared?: (subscription: IWidgetSubscription) => void;
}

export interface WidgetSubscriptionOptions {
  type?: widgetType;
  stateData?: boolean;
  alarmSource?: Datasource;
  datasources?: Array<Datasource>;
  datasourcesOptional?: boolean;
  hasDataPageLink?: boolean;
  singleEntity?: boolean;
  pageSize?: number;
  warnOnPageDataOverflow?: boolean;
  ignoreDataUpdateOnIntervalTick?: boolean;
  targetDeviceAliasIds?: Array<string>;
  targetDeviceIds?: Array<string>;
  targetDevice?: TargetDevice;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timeWindowConfig?: Timewindow;
  dashboardTimewindow?: Timewindow;
  onTimewindowChangeFunction?: (timewindow: Timewindow) => Timewindow;
  legendConfig?: LegendConfig;
  comparisonEnabled?: boolean;
  timeForComparison?: ComparisonDuration;
  comparisonCustomIntervalValue?: number;
  decimals?: number;
  units?: TbUnit;
  callbacks?: WidgetSubscriptionCallbacks;
}

export interface SubscriptionEntityInfo {
  entityId: EntityId;
  entityName: string;
  entityLabel: string;
  entityDescription: string;
}

export interface IWidgetSubscription {

  options: WidgetSubscriptionOptions;
  id: string;
  init$: Observable<IWidgetSubscription>;
  ctx: WidgetSubscriptionContext;
  type: widgetType;
  callbacks: WidgetSubscriptionCallbacks;

  loadingData: boolean;
  useDashboardTimewindow: boolean;

  legendData: LegendData;

  readonly firstDatasource?: Datasource;

  datasourcePages?: PageData<Datasource>[];
  dataPages?: PageData<Array<DatasourceData>>[];
  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  latestData?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindowConfig?: Timewindow;
  timeWindow?: WidgetTimewindow;
  subscriptionTimewindow: SubscriptionTimewindow;
  onTimewindowChangeFunction?: (timewindow: Timewindow) => Timewindow;
  widgetTimewindowChanged$: Observable<WidgetTimewindow>;
  comparisonEnabled?: boolean;
  comparisonTimeWindow?: WidgetTimewindow;

  persistentRequests?: PageData<PersistentRpc>;

  alarms?: PageData<AlarmData>;
  alarmSource?: Datasource;

  targetEntityId?: EntityId;

  rpcEnabled?: boolean;
  executingRpcRequest?: boolean;
  rpcErrorText?: string;
  rpcRejection?: HttpErrorResponse | Error;

  getFirstEntityInfo(): SubscriptionEntityInfo;

  onAliasesChanged(aliasIds: Array<string>): boolean;

  onFiltersChanged(filterIds: Array<string>): boolean;

  onDashboardTimewindowChanged(dashboardTimewindow: Timewindow): void;

  updateDataVisibility(index: number): void;

  onUpdateTimewindow(startTimeMs: number, endTimeMs: number, interval?: number): void;
  onResetTimewindow(): void;
  updateTimewindowConfig(newTimewindow: Timewindow): void;

  sendOneWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any>;
  sendTwoWayCommand(method: string, params?: any, timeout?: number, persistent?: boolean,
                    persistentPollingInterval?: number, retries?: number, additionalInfo?: any, requestUUID?: string): Observable<any>;
  clearRpcError(): void;

  subscribe(): void;

  subscribeAllForPaginatedData(pageLink: EntityDataPageLink,
                               keyFilters: KeyFilter[]): void;

  subscribeForPaginatedData(datasourceIndex: number,
                            pageLink: EntityDataPageLink,
                            keyFilters: KeyFilter[]): Observable<any>;

  paginatedDataSubscriptionUpdated: EventEmitter<void>;

  subscribeForAlarms(pageLink: AlarmDataPageLink,
                     keyFilters: KeyFilter[]): void;

  isDataResolved(): boolean;

  destroy(): void;

  update(): void;

  [key: string]: any;
}
