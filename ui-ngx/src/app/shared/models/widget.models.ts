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

import {BaseData} from '@shared/models/base-data';
import {TenantId} from '@shared/models/id/tenant-id';
import {WidgetsBundleId} from '@shared/models/id/widgets-bundle-id';
import {WidgetTypeId} from '@shared/models/id/widget-type-id';
import { AliasEntityType, EntityType, EntityTypeTranslation } from '@shared/models/entity-type.models';
import { Timewindow } from '@shared/models/time/time.models';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm'
}

export interface WidgetTypeTemplate {
  bundleAlias: string;
  alias: string;
}

export interface WidgetTypeData {
  name: string;
  template: WidgetTypeTemplate;
}

export const widgetTypesData = new Map<widgetType, WidgetTypeData>(
  [
    [
      widgetType.timeseries,
      {
        name: 'widget.timeseries',
        template: {
          bundleAlias: 'charts',
          alias: 'basic_timeseries'
        }
      }
    ],
    [
      widgetType.latest,
      {
        name: 'widget.latest-values',
        template: {
          bundleAlias: 'cards',
          alias: 'attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        template: {
          bundleAlias: 'gpio_widgets',
          alias: 'basic_gpio_control'
        }
      }
    ],
    [
      widgetType.alarm,
      {
        name: 'widget.alarm',
        template: {
          bundleAlias: 'alarm_widgets',
          alias: 'alarms_table'
        }
      }
    ]
  ]
);

export interface WidgetResource {
  url: string;
}

export interface WidgetActionSource {
  name: string;
  value: string;
  multiple: boolean;
}

export const widgetActionSources: {[key: string]: WidgetActionSource} = {
    headerButton:
    {
      name: 'widget-action.header-button',
      value: 'headerButton',
      multiple: true,
    }
};

export interface WidgetTypeDescriptor {
  type: widgetType;
  resources: Array<WidgetResource>;
  templateHtml: string;
  templateCss: string;
  controllerScript: string;
  settingsSchema?: string;
  dataKeySettingsSchema?: string;
  defaultConfig: string;
  sizeX: number;
  sizeY: number;
}

export interface WidgetTypeParameters {
  useCustomDatasources?: boolean;
  maxDatasources?: number;
  maxDataKeys?: number;
  dataKeysOptional?: boolean;
  stateData?: boolean;
}

export interface WidgetControllerDescriptor {
  widgetTypeFunction?: any;
  settingsSchema?: string;
  dataKeySettingsSchema?: string;
  typeParameters?: WidgetTypeParameters;
  actionSources?: {[key: string]: WidgetActionSource};
}

export interface WidgetType extends BaseData<WidgetTypeId> {
  tenantId: TenantId;
  bundleAlias: string;
  alias: string;
  name: string;
  descriptor: WidgetTypeDescriptor;
}

export interface WidgetInfo extends WidgetTypeDescriptor, WidgetControllerDescriptor {
  widgetName: string;
  alias: string;
  typeSettingsSchema?: string;
  typeDataKeySettingsSchema?: string;
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
    '}\n'
};

export const ErrorWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Error loading widget',
  alias: 'error',
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
    '<div translate class="tb-widget-error-msg">widget.widget-type-load-error</div>',
  templateCss: '',
  controllerScript: 'self.onInit = function() {}',
  settingsSchema: '{}\n',
  dataKeySettingsSchema: '{}\n',
  defaultConfig: '{\n' +
    '"title": "Widget failed to load",\n' +
    '"datasources": [],\n' +
    '"settings": {}\n' +
    '}\n'
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

export enum LegendDirection {
  column = 'column',
  row = 'row'
}

export const legendDirectionTranslationMap = new Map<LegendDirection, string>(
  [
    [ LegendDirection.column, 'direction.column' ],
    [ LegendDirection.row, 'direction.row' ]
  ]
);

export enum LegendPosition {
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right'
}

export const legendPositionTranslationMap = new Map<LegendPosition, string>(
  [
    [ LegendPosition.top, 'position.top' ],
    [ LegendPosition.bottom, 'position.bottom' ],
    [ LegendPosition.left, 'position.left' ],
    [ LegendPosition.right, 'position.right' ]
  ]
);

export interface LegendConfig {
  position: LegendPosition;
  direction?: LegendDirection;
  showMin: boolean;
  showMax: boolean;
  showAvg: boolean;
  showTotal: boolean;
}

export interface DataKey {
  label: string;
  color: string;
  hidden?: boolean;
  [key: string]: any;
  // TODO:
}

export interface LegendKey {
  dataKey: DataKey;
  dataIndex: number;
}

export interface LegendKeyData {
  min: number;
  max: number;
  avg: number;
  total: number;
}

export interface LegendData {
  keys: Array<LegendKey>;
  data: Array<LegendKeyData>;
}

export interface WidgetConfigSettings {
  [key: string]: any;
  // TODO:
}

export enum WidgetActionType {
  openDashboardState = 'openDashboardState',
  updateDashboardState = 'updateDashboardState',
  openDashboard = 'openDashboard',
  custom = 'custom',
  customPretty = 'customPretty'
}

export const widgetActionTypeTranslationMap = new Map<WidgetActionType, string>(
  [
    [ WidgetActionType.openDashboardState, 'widget-action.open-dashboard-state' ],
    [ WidgetActionType.updateDashboardState, 'widget-action.update-dashboard-state' ],
    [ WidgetActionType.openDashboard, 'widget-action.open-dashboard' ],
    [ WidgetActionType.custom, 'widget-action.custom' ],
    [ WidgetActionType.customPretty, 'widget-action.custom-pretty' ]
  ]
);

export interface WidgetActionDescriptor {
  name: string;
  icon: string;
  displayName?: string;
  type: WidgetActionType;
  targetDashboardId?: string;
  targetDashboardStateId?: string;
  openRightLayout?: boolean;
  setEntityId?: boolean;
  stateEntityParamName?: string;
  customFunction?: string;
  customResources?: Array<WidgetResource>;
  customHtml?: string;
  customCss?: string;
}

export interface WidgetConfig {
  title?: string;
  titleIcon?: string;
  showTitle?: boolean;
  showTitleIcon?: boolean;
  iconColor?: string;
  iconSize?: number;
  dropShadow?: boolean;
  enableFullscreen?: boolean;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  showLegend?: boolean;
  legendConfig?: LegendConfig;
  timewindow?: Timewindow;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  widgetStyle?: {[klass: string]: any};
  titleStyle?: {[klass: string]: any};
  units?: string;
  decimals?: number;
  actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  settings?: WidgetConfigSettings;
  [key: string]: any;

  // TODO:
}

export interface Widget {
  id?: string;
  typeId: WidgetTypeId;
  isSystemType: boolean;
  bundleAlias: string;
  typeAlias: string;
  type: widgetType;
  title: string;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
}
