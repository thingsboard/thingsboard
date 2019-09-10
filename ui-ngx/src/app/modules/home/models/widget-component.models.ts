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
import {
  DataSet,
  Datasource, DatasourceData,
  WidgetActionDescriptor,
  WidgetActionSource,
  WidgetConfig,
  WidgetConfigSettings,
  WidgetControllerDescriptor,
  WidgetType,
  widgetType,
  WidgetTypeDescriptor,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { Timewindow } from '@shared/models/time/time.models';
import {
  EntityInfo,
  IAliasController,
  IStateController,
  IWidgetSubscription,
  IWidgetUtils,
  RpcApi,
  TimewindowFunctions,
  WidgetActionsApi,
  WidgetSubscriptionApi
} from '@core/api/widget-api.models';
import { ComponentFactory } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

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

  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindow?: Timewindow;
}

export interface IDynamicWidgetComponent {
  widgetContext: WidgetContext;
  errorMessages: string[];
  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse;
  [key: string]: any;
}

export interface WidgetInfo extends WidgetTypeDescriptor, WidgetControllerDescriptor {
  widgetName: string;
  alias: string;
  typeSettingsSchema?: string;
  typeDataKeySettingsSchema?: string;
  componentFactory?: ComponentFactory<IDynamicWidgetComponent>;
}

export const MissingWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Widget type not found',
  alias: 'undefined',
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
    '<div translate class="tb-widget-error-msg">widget.widget-type-not-found</div>' +
    '</div>',
  templateCss: '',
  controllerScript: 'self.onInit = function() {}',
  settingsSchema: '{}\n',
  dataKeySettingsSchema: '{}\n',
  defaultConfig: '{\n' +
    '"title": "Widget type not found",\n' +
    '"datasources": [],\n' +
    '"settings": {}\n' +
    '}\n',
  typeParameters: {}
};

export const ErrorWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Error loading widget',
  alias: 'error',
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
                   '<div translate class="tb-widget-error-msg">widget.widget-type-load-error</div>' +
                   '<div *ngFor="let error of errorMessages" class="tb-widget-error-msg">{{ error }}</div>' +
                '</div>',
  templateCss: '',
  controllerScript: 'self.onInit = function() {}',
  settingsSchema: '{}\n',
  dataKeySettingsSchema: '{}\n',
  defaultConfig: '{\n' +
    '"title": "Widget failed to load",\n' +
    '"datasources": [],\n' +
    '"settings": {}\n' +
    '}\n',
  typeParameters: {}
};

export interface WidgetTypeInstance {
  getSettingsSchema?: () => string;
  getDataKeySettingsSchema?: () => string;
  typeParameters?: () => WidgetTypeParameters;
  useCustomDatasources?: () => boolean;
  actionSources?: () => {[key: string]: WidgetActionSource};

  onInit?: () => void;
  onDataUpdated?: () => void;
  onResize?: () => void;
  onEditModeChanged?: () => void;
  onMobileModeChanged?: () => void;
  onDestroy?: () => void;
}

export function toWidgetInfo(widgetTypeEntity: WidgetType): WidgetInfo {
  return {
    widgetName: widgetTypeEntity.name,
    alias: widgetTypeEntity.alias,
    type: widgetTypeEntity.descriptor.type,
    sizeX: widgetTypeEntity.descriptor.sizeX,
    sizeY: widgetTypeEntity.descriptor.sizeY,
    resources: widgetTypeEntity.descriptor.resources,
    templateHtml: widgetTypeEntity.descriptor.templateHtml,
    templateCss: widgetTypeEntity.descriptor.templateCss,
    controllerScript: widgetTypeEntity.descriptor.controllerScript,
    settingsSchema: widgetTypeEntity.descriptor.settingsSchema,
    dataKeySettingsSchema: widgetTypeEntity.descriptor.dataKeySettingsSchema,
    defaultConfig: widgetTypeEntity.descriptor.defaultConfig
  };
}

