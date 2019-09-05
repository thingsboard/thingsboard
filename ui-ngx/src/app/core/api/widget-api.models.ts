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
import { WidgetActionDescriptor, widgetType } from '@shared/models/widget.models';
import { TimeService } from '../services/time.service';
import { DeviceService } from '../http/device.service';
import { AlarmService } from '../http/alarm.service';
import { UtilsService } from '@core/services/utils.service';

export interface TimewindowFunctions {
  onUpdateTimewindow: (startTimeMs: number, endTimeMs: number, interval: number) => void;
  onResetTimewindow: () => void;
}

export interface WidgetSubscriptionApi {
  createSubscription: (options: WidgetSubscriptionOptions, subscribe: boolean) => Observable<IWidgetSubscription>;
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

export interface IAliasController {
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
  [key: string]: any;
  // TODO:
}

export interface WidgetSubscriptionContext {
  timeService: TimeService;
  deviceService: DeviceService;
  alarmService: AlarmService;
  utils: UtilsService;
  widgetUtils: IWidgetUtils;
  dashboardTimewindowApi: TimewindowFunctions;
  getServerTimeDiff: Observable<number>;
  aliasController: IAliasController;
  [key: string]: any;
  // TODO:
}

export interface WidgetSubscriptionOptions {
  [key: string]: any;
  // TODO:
}

export interface IWidgetSubscription {

  onUpdateTimewindow: (startTimeMs: number, endTimeMs: number, interval: number) => void;
  onResetTimewindow: () => void;

  sendOneWayCommand: (method: string, params?: any, timeout?: number) => Observable<any>;
  sendTwoWayCommand: (method: string, params?: any, timeout?: number) => Observable<any>;

  clearRpcError: () => void;

  getFirstEntityInfo: () => EntityInfo;

  destroy(): void;

  [key: string]: any;
  // TODO:
}
