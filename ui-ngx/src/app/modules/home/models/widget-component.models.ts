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
  WidgetControllerDescriptor,
  WidgetType,
  widgetType,
  WidgetTypeDescriptor,
  WidgetTypeParameters,
  Widget
} from '@shared/models/widget.models';
import { Timewindow, WidgetTimewindow } from '@shared/models/time/time.models';
import {
  IAliasController,
  IStateController,
  IWidgetSubscription,
  IWidgetUtils,
  RpcApi, SubscriptionEntityInfo, SubscriptionInfo,
  TimewindowFunctions,
  WidgetActionsApi,
  WidgetSubscriptionApi, WidgetSubscriptionContext, WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import { ChangeDetectorRef, ComponentFactory, Injector, NgZone, Type } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetLayout } from '@shared/models/dashboard.models';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@app/core/http/asset.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { isDefined, formatValue } from '@core/utils';
import { Observable, of, ReplaySubject } from 'rxjs';
import { WidgetSubscription } from '@core/api/widget-subscription';

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

export interface IDashboardWidget {
  updateWidgetParams();
}

export class WidgetContext {

  constructor(public dashboard: IDashboardComponent,
              private dashboardWidget: IDashboardWidget,
              private widget: Widget) {}

  get stateController(): IStateController {
    return this.dashboard.stateController;
  }

  get aliasController(): IAliasController {
    return this.dashboard.aliasController;
  }

  get dashboardTimewindow(): Timewindow {
    return this.dashboard.dashboardTimewindow;
  }

  get widgetConfig(): WidgetConfig {
    return this.widget.config;
  }

  get settings(): any {
    return this.widget.config.settings;
  }

  get units(): string {
    return this.widget.config.units || '';
  }

  get decimals(): number {
    return isDefined(this.widget.config.decimals) ? this.widget.config.decimals : 2;
  }

  set changeDetector(cd: ChangeDetectorRef) {
    this._changeDetector = cd;
  }

  private _changeDetector: ChangeDetectorRef;

  detectChanges(updateWidgetParams: boolean = false) {
    if (!this.destroyed) {
      if (updateWidgetParams) {
        this.dashboardWidget.updateWidgetParams();
      }
      this._changeDetector.detectChanges();
    }
  }

  updateWidgetParams() {
    if (!this.destroyed) {
      setTimeout(() => {
        this.dashboardWidget.updateWidgetParams();
      }, 0);
    }
  }

  inited = false;
  destroyed = false;

  subscriptions: {[id: string]: IWidgetSubscription} = {};
  defaultSubscription: IWidgetSubscription = null;

  timewindowFunctions: TimewindowFunctions = {
    onUpdateTimewindow: (startTimeMs, endTimeMs, interval) => {
      if (this.defaultSubscription) {
        this.defaultSubscription.onUpdateTimewindow(startTimeMs, endTimeMs, interval);
      }
    },
    onResetTimewindow: () => {
      if (this.defaultSubscription) {
        this.defaultSubscription.onResetTimewindow();
      }
    }
  };

  controlApi: RpcApi = {
    sendOneWayCommand: (method, params, timeout) => {
      if (this.defaultSubscription) {
        return this.defaultSubscription.sendOneWayCommand(method, params, timeout);
      } else {
        return of(null);
      }
    },
    sendTwoWayCommand: (method, params, timeout) => {
      if (this.defaultSubscription) {
        return this.defaultSubscription.sendTwoWayCommand(method, params, timeout);
      } else {
        return of(null);
      }
    }
  };

  utils: IWidgetUtils = {
    formatValue
  };

  $container: JQuery<any>;
  $containerParent: JQuery<any>;
  width: number;
  height: number;
  $scope: IDynamicWidgetComponent;
  isEdit: boolean;
  isMobile: boolean;

  subscriptionApi?: WidgetSubscriptionApi;

  actionsApi?: WidgetActionsApi;
  activeEntityInfo?: SubscriptionEntityInfo;

  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindow?: WidgetTimewindow;

  hideTitlePanel = false;

  widgetTitleTemplate?: string;
  widgetTitle?: string;
  customHeaderActions?: Array<WidgetHeaderAction>;
  widgetActions?: Array<WidgetAction>;

  servicesMap?: Map<string, Type<any>>;

  $injector?: Injector;

  ngZone?: NgZone;
}

export interface IDynamicWidgetComponent {
  readonly ctx: WidgetContext;
  readonly errorMessages: string[];
  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse;
  raf: RafService;
  [key: string]: any;
}

export interface WidgetInfo extends WidgetTypeDescriptor, WidgetControllerDescriptor {
  widgetName: string;
  alias: string;
  typeSettingsSchema?: string | any;
  typeDataKeySettingsSchema?: string | any;
  componentFactory?: ComponentFactory<IDynamicWidgetComponent>;
}

export interface WidgetConfigComponentData {
  config: WidgetConfig;
  layout: WidgetLayout;
  widgetType: widgetType;
  typeParameters: WidgetTypeParameters;
  actionSources: {[actionSourceId: string]: WidgetActionSource};
  isDataEnabled: boolean;
  settingsSchema: any;
  dataKeySettingsSchema: any;
}

export const MissingWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Widget type not found',
  alias: 'undefined',
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
    '<div class="tb-widget-error-msg" innerHTML="{{\'widget.widget-type-not-found\' | translate }}"></div>' +
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
  actionSources?: () => {[actionSourceId: string]: WidgetActionSource};

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

export function toWidgetType(widgetInfo: WidgetInfo, id: WidgetTypeId, tenantId: TenantId, bundleAlias: string): WidgetType {
  const descriptor: WidgetTypeDescriptor = {
    type: widgetInfo.type,
    sizeX: widgetInfo.sizeX,
    sizeY: widgetInfo.sizeY,
    resources: widgetInfo.resources,
    templateHtml: widgetInfo.templateHtml,
    templateCss: widgetInfo.templateCss,
    controllerScript: widgetInfo.controllerScript,
    settingsSchema: widgetInfo.settingsSchema,
    dataKeySettingsSchema: widgetInfo.dataKeySettingsSchema,
    defaultConfig: widgetInfo.defaultConfig
  };
  return {
    id,
    tenantId,
    bundleAlias,
    alias: widgetInfo.alias,
    name: widgetInfo.widgetName,
    descriptor
  };
}
