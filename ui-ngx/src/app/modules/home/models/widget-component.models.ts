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

import { ExceptionData } from '@shared/models/error.models';
import { IDashboardComponent } from '@home/models/dashboard-component.models';
import { WidgetActionDescriptor, WidgetConfig, WidgetConfigSettings, widgetType } from '@shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import {
  EntityInfo,
  IWidgetSubscription,
  SubscriptionInfo,
  WidgetSubscriptionOptions,
  IStateController,
  IAliasController,
  TimewindowFunctions,
  WidgetSubscriptionApi,
  RpcApi,
  WidgetActionsApi,
  IWidgetUtils
} from '@core/api/widget-api.models';
import { Observable } from 'rxjs';
import { EntityId } from '@shared/models/id/entity-id';

export interface IWidgetAction {
  name: string;
  icon: string;
  onAction: ($event: Event) => void;
}

export interface WidgetHeaderAction extends IWidgetAction {
  displayName: string;
  descriptor: WidgetActionDescriptor;
}

export interface WidgetAction extends IWidgetAction {
  show: boolean;
}

export interface IDynamicWidgetComponent {
  widgetContext: WidgetContext;
  widgetErrorData: ExceptionData;
  loadingData: boolean;
  [key: string]: any;
}

export interface WidgetContext {
  inited?: boolean;
  $container?: any;
  $containerParent?: any;
  width?: number;
  height?: number;
  $scope?: IDynamicWidgetComponent;
  hideTitlePanel?: boolean;
  isEdit?: boolean;
  isMobile?: boolean;
  dashboard?: IDashboardComponent;
  widgetConfig?: WidgetConfig;
  settings?: WidgetConfigSettings;
  units?: string;
  decimals?: number;
  subscriptions?: {[id: string]: IWidgetSubscription};
  defaultSubscription?: IWidgetSubscription;
  dashboardTimewindow?: Timewindow;
  timewindowFunctions?: TimewindowFunctions;
  subscriptionApi?: WidgetSubscriptionApi;
  controlApi?: RpcApi;
  utils?: IWidgetUtils;
  actionsApi?: WidgetActionsApi;
  stateController?: IStateController;
  aliasController?: IAliasController;
  activeEntityInfo?: EntityInfo;
  widgetTitleTemplate?: string;
  widgetTitle?: string;
  customHeaderActions?: Array<WidgetHeaderAction>;
  widgetActions?: Array<WidgetAction>;
}
