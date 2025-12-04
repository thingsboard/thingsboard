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

import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { AggregationType, ComparisonDuration, Timewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DataKeyType } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AlarmFilter,
  AlarmFilterConfig,
  EntityDataPageLink,
  EntityFilter,
  KeyFilter
} from '@shared/models/query/query.models';
import { PopoverPlacement } from '@shared/components/popover.models';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, DestroyRef, Directive, EventEmitter, inject, Inject, OnInit, Type } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { Observable } from 'rxjs';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { isNotEmptyStr, mergeDeep, mergeDeepIgnoreArray } from '@core/utils';
import { WidgetConfigComponentData } from '@home/models/widget-component.models';
import { ComponentStyle, Font, TimewindowStyle, ValueFormatProcessor } from '@shared/models/widget-settings.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { EntityInfoData, HasTenantId, HasVersion } from '@shared/models/entity.models';
import {
  DataKeysCallbacks,
  DataKeySettingsFunction
} from '@home/components/widget/lib/settings/common/key/data-keys.component.models';
import { WidgetConfigCallbacks } from '@home/components/widget/config/widget-config.component.models';
import { TbFunction } from '@shared/models/js-function.models';
import { FormProperty, jsonFormSchemaToFormProperties } from '@shared/models/dynamic-form.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbUnit } from '@shared/models/unit.models';
import { ImageResourceInfo } from '@shared/models/resource.models';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm',
  static = 'static'
}

export interface WidgetTypeTemplate {
  fullFqn: string;
}

export interface WidgetTypeData {
  name: string;
  icon: string;
  configHelpLinkId: string;
  template: WidgetTypeTemplate;
}

export const widgetTitleAutocompleteValues = ['entityName', 'entityLabel'];

export const widgetTypesData = new Map<widgetType, WidgetTypeData>(
  [
    [
      widgetType.timeseries,
      {
        name: 'widget.timeseries',
        icon: 'timeline',
        configHelpLinkId: 'widgetsConfigTimeseries',
        template: {
          fullFqn: 'system.time_series_chart'
        }
      }
    ],
    [
      widgetType.latest,
      {
        name: 'widget.latest',
        icon: 'track_changes',
        configHelpLinkId: 'widgetsConfigLatest',
        template: {
          fullFqn: 'system.cards.attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        icon: 'mdi:developer-board',
        configHelpLinkId: 'widgetsConfigRpc',
        template: {
          fullFqn: 'system.gpio_widgets.basic_gpio_control'
        }
      }
    ],
    [
      widgetType.alarm,
      {
        name: 'widget.alarm',
        icon: 'error',
        configHelpLinkId: 'widgetsConfigAlarm',
        template: {
          fullFqn: 'system.alarm_widgets.alarms_table'
        }
      }
    ],
    [
      widgetType.static,
      {
        name: 'widget.static',
        icon: 'font_download',
        configHelpLinkId: 'widgetsConfigStatic',
        template: {
          fullFqn: 'system.cards.html_card'
        }
      }
    ]
  ]
);

export interface WidgetResource {
  url: string;
  isModule?: boolean;
}

export interface WidgetActionSource {
  name: string;
  value: string;
  multiple: boolean;
  hasShowCondition?: boolean;
}

export const widgetActionSources: {[acionSourceId: string]: WidgetActionSource} = {
    headerButton:
    {
      name: 'widget-action.header-button',
      value: 'headerButton',
      multiple: true,
      hasShowCondition: true
    }
};

export interface WidgetTypeDescriptor {
  type: widgetType;
  resources: Array<WidgetResource>;
  templateHtml: string;
  templateCss: string;
  controllerScript: TbFunction;
  settingsForm?: FormProperty[];
  dataKeySettingsForm?: FormProperty[];
  latestDataKeySettingsForm?: FormProperty[];
  settingsDirective?: string;
  dataKeySettingsDirective?: string;
  latestDataKeySettingsDirective?: string;
  hasBasicMode?: boolean;
  basicModeDirective?: string;
  defaultConfig: string;
  sizeX: number;
  sizeY: number;
}

export interface WidgetTypeParameters {
  useCustomDatasources?: boolean;
  maxDatasources?: number;
  maxDataKeys?: number;
  datasourcesOptional?: boolean;
  dataKeysOptional?: boolean;
  stateData?: boolean;
  hasDataPageLink?: boolean;
  singleEntity?: boolean;
  hasAdditionalLatestDataKeys?: boolean;
  warnOnPageDataOverflow?: boolean;
  ignoreDataUpdateOnIntervalTick?: boolean;
  processNoDataByWidget?: boolean;
  previewWidth?: string;
  previewHeight?: string;
  embedTitlePanel?: boolean;
  embedActionsPanel?: boolean;
  overflowVisible?: boolean;
  hideDataTab?: boolean;
  hideDataSettings?: boolean;
  defaultDataKeysFunction?: (configComponent: any, configData: any) => DataKey[];
  defaultLatestDataKeysFunction?: (configComponent: any, configData: any) => DataKey[];
  dataKeySettingsFunction?: DataKeySettingsFunction;
  displayRpcMessageToast?: boolean;
  targetDeviceOptional?: boolean;
  supportsUnitConversion?: boolean;
  additionalWidgetActionTypes?: WidgetActionType[];
}

export interface WidgetControllerDescriptor {
  widgetTypeFunction?: any;
  settingsForm?: FormProperty[];
  dataKeySettingsForm?: FormProperty[];
  latestDataKeySettingsForm?: FormProperty[];
  typeParameters?: WidgetTypeParameters;
  actionSources?: {[actionSourceId: string]: WidgetActionSource};
}

export interface BaseWidgetType extends BaseData<WidgetTypeId>, HasTenantId, HasVersion, ExportableEntity<WidgetTypeId> {
  tenantId: TenantId;
  fqn: string;
  name: string;
  deprecated: boolean;
  scada: boolean;
}

export const fullWidgetTypeFqn = (type: BaseWidgetType): string =>
  ((!type.tenantId || type.tenantId?.id === NULL_UUID) ? 'system' : 'tenant') + '.' + type.fqn;

export const widgetTypeFqn = (fullFqn: string): string => {
  if (isNotEmptyStr(fullFqn)) {
    const parts = fullFqn.split('.');
    if (parts.length > 1) {
      const scopeQualifier = parts[0];
      if (['system', 'tenant'].includes(scopeQualifier)) {
        return fullFqn.substring(scopeQualifier.length + 1);
      }
    }
  }
  return fullFqn;
};

export const isValidWidgetFullFqn = (fullFqn: string): boolean => {
  if (isNotEmptyStr(fullFqn)) {
    const parts = fullFqn.split('.');
    if (parts.length > 1) {
      const scopeQualifier = parts[0];
      return ['system', 'tenant'].includes(scopeQualifier);
    }
  }
  return false;
};


export const migrateWidgetTypeToDynamicForms = <T extends WidgetType>(widgetType: T): T => {
  const descriptor = widgetType.descriptor;
  if ((descriptor as any).settingsSchema) {
    if (!descriptor.settingsForm?.length) {
      descriptor.settingsForm = jsonFormSchemaToFormProperties((descriptor as any).settingsSchema);
    }
    delete (descriptor as any).settingsSchema;
  }
  if ((descriptor as any).dataKeySettingsSchema) {
    if (!descriptor.dataKeySettingsForm?.length) {
      descriptor.dataKeySettingsForm = jsonFormSchemaToFormProperties((descriptor as any).dataKeySettingsSchema);
    }
    delete (descriptor as any).dataKeySettingsSchema;
  }
  if ((descriptor as any).latestDataKeySettingsSchema) {
    if (!descriptor.latestDataKeySettingsForm?.length) {
      descriptor.latestDataKeySettingsForm = jsonFormSchemaToFormProperties((descriptor as any).latestDataKeySettingsSchema);
    }
    delete (descriptor as any).latestDataKeySettingsSchema;
  }
  return widgetType;
}

export interface WidgetType extends BaseWidgetType {
  descriptor: WidgetTypeDescriptor;
}

export interface WidgetTypeInfo extends BaseWidgetType {
  image: string;
  description: string;
  tags: string[];
  widgetType: widgetType;
  bundles?: EntityInfoData[];
}

export interface WidgetTypeDetails extends WidgetType, ExportableEntity<WidgetTypeId> {
  image: string;
  description: string;
  tags: string[];
  resources?: Array<any>;
}

export enum DeprecatedFilter {
  ALL = 'ALL',
  ACTUAL = 'ACTUAL',
  DEPRECATED = 'DEPRECATED'
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

export const legendPositions = Object.keys(LegendPosition) as LegendPosition[];

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
  sortDataKeys: boolean;
  showMin: boolean;
  showMax: boolean;
  showAvg: boolean;
  showTotal: boolean;
  showLatest: boolean;
  valueFormat: ValueFormatProcessor;
}

export const defaultLegendConfig = (wType: widgetType): LegendConfig => ({
  direction: LegendDirection.column,
  position: LegendPosition.bottom,
  sortDataKeys: false,
  showMin: false,
  showMax: false,
  showAvg: wType === widgetType.timeseries,
  showTotal: false,
  showLatest: false,
  valueFormat: null
});

export enum ComparisonResultType {
  PREVIOUS_VALUE = 'PREVIOUS_VALUE',
  DELTA_ABSOLUTE = 'DELTA_ABSOLUTE',
  DELTA_PERCENT = 'DELTA_PERCENT'
}

export const comparisonResultTypeTranslationMap = new Map<ComparisonResultType, string>(
  [
    [ComparisonResultType.PREVIOUS_VALUE, 'datakey.delta-calculation-result-previous-value'],
    [ComparisonResultType.DELTA_ABSOLUTE, 'datakey.delta-calculation-result-delta-absolute'],
    [ComparisonResultType.DELTA_PERCENT, 'datakey.delta-calculation-result-delta-percent']
  ]
);

export interface KeyInfo {
  name: string;
  aggregationType?: AggregationType;
  comparisonEnabled?: boolean;
  timeForComparison?: ComparisonDuration;
  comparisonCustomIntervalValue?: number;
  comparisonResultType?: ComparisonResultType;
  label?: string;
  color?: string;
  funcBody?: TbFunction;
  postFuncBody?: TbFunction;
  units?: TbUnit;
  decimals?: number;
}

export const dataKeyAggregationTypeHintTranslationMap = new Map<AggregationType, string>(
  [
    [AggregationType.MIN, 'datakey.aggregation-type-min-hint'],
    [AggregationType.MAX, 'datakey.aggregation-type-max-hint'],
    [AggregationType.AVG, 'datakey.aggregation-type-avg-hint'],
    [AggregationType.SUM, 'datakey.aggregation-type-sum-hint'],
    [AggregationType.COUNT, 'datakey.aggregation-type-count-hint'],
    [AggregationType.NONE, 'datakey.aggregation-type-none-hint'],
  ]
);


export interface DataKey extends KeyInfo {
  type: DataKeyType;
  pattern?: string;
  settings?: any;
  usePostProcessing?: boolean;
  hidden?: boolean;
  inLegend?: boolean;
  isAdditional?: boolean;
  origDataKeyIndex?: number;
}

export type CellClickColumnInfo = Pick<DataKey, 'name' | 'label'>;

export enum DataKeyConfigMode {
  general = 'general',
  advanced = 'advanced'
}

export enum DatasourceType {
  function = 'function',
  device = 'device',
  entity = 'entity',
  entityCount = 'entityCount',
  alarmCount = 'alarmCount'
}

export const datasourceTypeTranslationMap = new Map<DatasourceType, string>(
  [
    [ DatasourceType.function, 'function.function' ],
    [ DatasourceType.device, 'device.device' ],
    [ DatasourceType.entity, 'entity.entity' ],
    [ DatasourceType.entityCount, 'entity.entities-count' ],
    [ DatasourceType.alarmCount, 'entity.alarms-count' ]
  ]
);

export interface Datasource {
  type?: DatasourceType | any;
  name?: string;
  aliasName?: string;
  dataKeys?: Array<DataKey>;
  latestDataKeys?: Array<DataKey>;
  entityType?: EntityType;
  entityId?: string;
  entityName?: string;
  deviceId?: string;
  entityAliasId?: string;
  filterId?: string;
  unresolvedStateEntity?: boolean;
  dataReceived?: boolean;
  entity?: BaseData<EntityId>;
  entityLabel?: string;
  entityDescription?: string;
  generated?: boolean;
  isAdditional?: boolean;
  origDatasourceIndex?: number;
  pageLink?: EntityDataPageLink;
  keyFilters?: Array<KeyFilter>;
  entityFilter?: EntityFilter;
  alarmFilterConfig?: AlarmFilterConfig;
  alarmFilter?: AlarmFilter;
  dataKeyStartIndex?: number;
  latestDataKeyStartIndex?: number;
  [key: string]: any;
}

export const datasourceValid = (datasource: Datasource): boolean => {
  const type: DatasourceType = datasource?.type;
  if (type) {
    switch (type) {
      case DatasourceType.function:
      case DatasourceType.alarmCount:
        return true;
      case DatasourceType.device:
        return !!datasource.deviceId;
      case DatasourceType.entity:
      case DatasourceType.entityCount:
        return !!datasource.entityAliasId;
    }
  }
  return false;
};

export enum TargetDeviceType {
  device = 'device',
  entity = 'entity'
}

export interface TargetDevice {
  type?: TargetDeviceType;
  deviceId?: string;
  entityAliasId?: string;
}

export const targetDeviceValid = (targetDevice?: TargetDevice): boolean =>
  !!targetDevice && !!targetDevice.type &&
    ((targetDevice.type === TargetDeviceType.device && !!targetDevice.deviceId) ||
      (targetDevice.type === TargetDeviceType.entity && !!targetDevice.entityAliasId));

export const widgetTypeHasTimewindow = (type: widgetType): boolean => {
  return type === widgetType.timeseries || type === widgetType.alarm;
}

export const widgetTypeCanHaveTimewindow = (type: widgetType): boolean => {
  return widgetTypeHasTimewindow(type) || type === widgetType.latest;
}

export const datasourcesHasAggregation = (datasources?: Array<Datasource>): boolean => {
  if (datasources) {
    const foundDatasource = datasources.find(datasource => {
      const found = datasource.dataKeys && datasource.dataKeys.find(key => key?.type === DataKeyType.timeseries &&
        key?.aggregationType && key.aggregationType !== AggregationType.NONE);
      return !!found;
    });
    if (foundDatasource) {
      return true;
    }
  }
  return false;
};

export const datasourcesHasOnlyComparisonAggregation = (datasources?: Array<Datasource>): boolean => {
  if (!datasourcesHasAggregation(datasources)) {
    return false;
  }
  if (datasources) {
    const foundDatasource = datasources.find(datasource => {
      const found = datasource.dataKeys && datasource.dataKeys.find(key => key?.type === DataKeyType.timeseries &&
        key?.aggregationType && key.aggregationType !== AggregationType.NONE && !key.comparisonEnabled);
      return !!found;
    });
    if (foundDatasource) {
      return false;
    }
  }
  return true;
};

export interface FormattedData<D extends Datasource = Datasource> {
  $datasource: D;
  entityName: string;
  deviceName: string;
  entityId: string;
  entityType: EntityType;
  entityLabel: string;
  entityDescription: string;
  aliasName: string;
  dsIndex: number;
  dsName: string;
  deviceType: string;
  [key: string]: any;
}

export interface ReplaceInfo {
  variable: string;
  valDec?: number;
  dataKeyName: string;
}

export type DataEntry = [number, any, [number, number]?];

export type DataSet = DataEntry[];

export interface IndexedData {
  [id: number]: DataSet;
}

export interface DataSetHolder {
  data: DataSet;
}

export interface DatasourceData extends DataSetHolder {
  datasource: Datasource;
  dataKey: DataKey;
}

export interface LegendKey {
  dataKey: DataKey;
  dataIndex: number;
  valueFormat: ValueFormatProcessor;
}

export interface LegendKeyData {
  min: string;
  max: string;
  avg: string;
  total: string;
  latest: string;
  hidden: boolean;
}

export interface LegendData {
  keys: Array<LegendKey>;
  data: Array<LegendKeyData>;
}

export enum WidgetHeaderActionButtonType {
  basic = 'basic',
  raised = 'raised',
  stroked = 'stroked',
  flat = 'flat',
  icon = 'icon',
  miniFab = 'miniFab'
}

export const WidgetHeaderActionButtonTypes = Object.keys(WidgetHeaderActionButtonType) as WidgetHeaderActionButtonType[];

export const widgetHeaderActionButtonTypeTranslationMap = new Map<WidgetHeaderActionButtonType, string>([
  [WidgetHeaderActionButtonType.basic, 'widget-config.header-button.button-type-basic'],
  [WidgetHeaderActionButtonType.raised, 'widget-config.header-button.button-type-raised'],
  [WidgetHeaderActionButtonType.stroked, 'widget-config.header-button.button-type-stroked'],
  [WidgetHeaderActionButtonType.flat, 'widget-config.header-button.button-type-flat'],
  [WidgetHeaderActionButtonType.icon, 'widget-config.header-button.button-type-icon'],
  [WidgetHeaderActionButtonType.miniFab, 'widget-config.header-button.button-type-mini-fab']
]);

export enum WidgetActionType {
  doNothing = 'doNothing',
  openDashboardState = 'openDashboardState',
  updateDashboardState = 'updateDashboardState',
  openDashboard = 'openDashboard',
  custom = 'custom',
  customPretty = 'customPretty',
  mobileAction = 'mobileAction',
  openURL = 'openURL',
  placeMapItem = 'placeMapItem'
}

export enum WidgetMobileActionType {
  takePictureFromGallery = 'takePictureFromGallery',
  takePhoto = 'takePhoto',
  mapDirection = 'mapDirection',
  mapLocation = 'mapLocation',
  scanQrCode = 'scanQrCode',
  makePhoneCall = 'makePhoneCall',
  getLocation = 'getLocation',
  takeScreenshot = 'takeScreenshot',
  deviceProvision = 'deviceProvision',
}

export interface ActionConfig {
  title: string,
  formControlName: string,
  functionName: string,
  functionArgs: string[],
  helpId?: string
}

export enum ProvisionType {
  auto = 'auto',
  wiFi = 'wiFi',
  ble = 'ble',
  softAp = 'softAp'
}

export const provisionTypeTranslationMap = new Map<ProvisionType, string>(
  [
    [ ProvisionType.auto, 'widget-action.mobile.auto' ],
    [ ProvisionType.wiFi, 'widget-action.mobile.wi-fi' ],
    [ ProvisionType.ble, 'widget-action.mobile.ble' ],
    [ ProvisionType.softAp, 'widget-action.mobile.soft-ap' ],
  ]
);

export enum MapItemType {
  marker = 'marker',
  polygon = 'polygon',
  rectangle = 'rectangle',
  circle = 'circle',
  polyline = 'polyline'
}

export const widgetActionTypes = Object.keys(WidgetActionType)
  .filter(value => value !== WidgetActionType.placeMapItem) as WidgetActionType[];

export const widgetActionTypeTranslationMap = new Map<WidgetActionType, string>(
  [
    [ WidgetActionType.doNothing, 'widget-action.do-nothing' ],
    [ WidgetActionType.openDashboardState, 'widget-action.open-dashboard-state' ],
    [ WidgetActionType.updateDashboardState, 'widget-action.update-dashboard-state' ],
    [ WidgetActionType.openDashboard, 'widget-action.open-dashboard' ],
    [ WidgetActionType.custom, 'widget-action.custom' ],
    [ WidgetActionType.customPretty, 'widget-action.custom-pretty' ],
    [ WidgetActionType.mobileAction, 'widget-action.mobile-action' ],
    [ WidgetActionType.openURL, 'widget-action.open-URL' ],
    [ WidgetActionType.placeMapItem, 'widget-action.place-map-item' ],
  ]
);

export const widgetMobileActionTypeTranslationMap = new Map<WidgetMobileActionType, string>(
  [
    [ WidgetMobileActionType.takePictureFromGallery, 'widget-action.mobile.take-picture-from-gallery' ],
    [ WidgetMobileActionType.takePhoto, 'widget-action.mobile.take-photo' ],
    [ WidgetMobileActionType.mapDirection, 'widget-action.mobile.map-direction' ],
    [ WidgetMobileActionType.mapLocation, 'widget-action.mobile.map-location' ],
    [ WidgetMobileActionType.scanQrCode, 'widget-action.mobile.scan-qr-code' ],
    [ WidgetMobileActionType.makePhoneCall, 'widget-action.mobile.make-phone-call' ],
    [ WidgetMobileActionType.getLocation, 'widget-action.mobile.get-location' ],
    [ WidgetMobileActionType.takeScreenshot, 'widget-action.mobile.take-screenshot' ],
    [ WidgetMobileActionType.deviceProvision, 'widget-action.mobile.device-provision' ]
  ]
);

export const mapItemTypeTranslationMap = new Map<MapItemType, string>(
  [
    [ MapItemType.marker, 'widget-action.map-item.marker' ],
    [ MapItemType.polygon, 'widget-action.map-item.polygon' ],
    [ MapItemType.rectangle, 'widget-action.map-item.rectangle' ],
    [ MapItemType.circle, 'widget-action.map-item.circle' ],
    [ MapItemType.polyline, 'widget-action.map-item.polyline' ]
  ]
)

export interface MobileLaunchResult {
  launched: boolean;
}

export interface MobileImageResult {
  imageUrl: string;
  imageInfo?: ImageResourceInfo;
}

export interface MobileQrCodeResult {
  code: string;
  format: string;
}

export interface MobileLocationResult {
  latitude: number;
  longitude: number;
}

export interface MobileDeviceProvisionResult {
  deviceName: string;
}

export type MobileActionResult = MobileLaunchResult &
                                 MobileImageResult &
                                 MobileQrCodeResult &
                                 MobileLocationResult &
                                 MobileDeviceProvisionResult;

export interface WidgetMobileActionResult<T extends MobileActionResult> {
  result?: T;
  hasResult: boolean;
  error?: string;
  hasError: boolean;
}

export interface ProvisionSuccessDescriptor {
  handleProvisionSuccessFunction: TbFunction;
  provisionType?: string;
}

export interface ProcessImageDescriptor {
  processImageFunction: TbFunction;
  saveToGallery?: boolean;
}

export interface ProcessLaunchResultDescriptor {
  processLaunchResultFunction?: TbFunction;
}

export interface LaunchMapDescriptor extends ProcessLaunchResultDescriptor {
  getLocationFunction: TbFunction;
}

export interface ScanQrCodeDescriptor {
  processQrCodeFunction: TbFunction;
}

export interface MakePhoneCallDescriptor extends ProcessLaunchResultDescriptor {
  getPhoneNumberFunction: TbFunction;
}

export interface GetLocationDescriptor {
  processLocationFunction: TbFunction;
}

export type WidgetMobileActionDescriptors = ProcessImageDescriptor &
                                            LaunchMapDescriptor &
                                            ScanQrCodeDescriptor &
                                            MakePhoneCallDescriptor &
                                            GetLocationDescriptor &
                                            ProvisionSuccessDescriptor;

export interface WidgetMobileActionDescriptor extends WidgetMobileActionDescriptors {
  type: WidgetMobileActionType;
  handleErrorFunction?: TbFunction;
  handleEmptyResultFunction?: TbFunction;
  handleNonMobileFallbackFunction?: TbFunction;
}

export interface CustomActionDescriptor {
  customFunction?: TbFunction;
  customResources?: Array<WidgetResource>;
  customHtml?: string;
  customCss?: string;
  customImports?: Type<any>[];
}

export interface WidgetAction extends CustomActionDescriptor {
  name?: string;
  type: WidgetActionType;
  targetDashboardId?: string;
  targetDashboardStateId?: string;
  openRightLayout?: boolean;
  openNewBrowserTab?: boolean;
  openInPopover?: boolean;
  popoverHideDashboardToolbar?: boolean;
  popoverPreferredPlacement?: PopoverPlacement;
  popoverHideOnClickOutside?: boolean;
  popoverWidth?: string;
  popoverHeight?: string;
  popoverStyle?: { [klass: string]: any };
  openInSeparateDialog?: boolean;
  dialogTitle?: string;
  dialogHideDashboardToolbar?: boolean;
  dialogWidth?: number;
  dialogHeight?: number;
  setEntityId?: boolean;
  stateEntityParamName?: string;
  mobileAction?: WidgetMobileActionDescriptor;
  url?: string;
  mapItemType?: MapItemType;
  mapItemTooltips?: MapItemTooltips;
}

export interface MapItemTooltips {
  placeMarker?: string;
  firstVertex?: string;
  continueLine?: string;
  finishPoly?: string;
  startRect?: string;
  finishRect?: string;
  startCircle?: string;
  finishCircle?: string;
  startPolyline?: string;
  finishPolyline?: string;
}

export const mapItemTooltipsTranslation: Required<MapItemTooltips> = Object.freeze({
  placeMarker: 'widgets.maps.data-layer.marker.place-marker-hint',
  firstVertex: 'widgets.maps.data-layer.polygon.polygon-place-first-point-hint',
  continueLine: 'widgets.maps.data-layer.polygon.continue-polygon-hint',
  finishPoly: 'widgets.maps.data-layer.polygon.finish-polygon-hint',
  startRect: 'widgets.maps.data-layer.polygon.rectangle-place-first-point-hint',
  finishRect: 'widgets.maps.data-layer.polygon.finish-rectangle-hint',
  startCircle: 'widgets.maps.data-layer.circle.place-circle-center-hint',
  finishCircle: 'widgets.maps.data-layer.circle.finish-circle-hint',
  startPolyline: 'widgets.maps.data-layer.polyline.polyline-place-first-point-hint',
  finishPolyline: 'widgets.maps.data-layer.polyline.finish-polyline-hint'
})

export interface WidgetActionDescriptor extends WidgetAction {
  id: string;
  name: string;
  buttonType?: WidgetHeaderActionButtonType;
  showIcon?: boolean;
  icon: string;
  buttonColor?: string;
  buttonFillColor?: string;
  buttonBorderColor?: string;
  customButtonStyle?: {[key: string]: string};
  displayName?: string;
  useShowWidgetActionFunction?: boolean;
  showWidgetActionFunction?: TbFunction;
  columnIndex?: number;
}

export const actionDescriptorToAction = (descriptor: WidgetActionDescriptor): WidgetAction => {
  const result: WidgetActionDescriptor = {...descriptor};
  delete result.id;
  delete result.name;
  delete result.buttonType;
  delete result.showIcon;
  delete result.icon;
  delete result.buttonColor;
  delete result.buttonFillColor;
  delete result.buttonBorderColor;
  delete result.customButtonStyle;
  delete result.displayName;
  delete result.useShowWidgetActionFunction;
  delete result.showWidgetActionFunction;
  delete result.columnIndex;
  return result;
};

export const defaultWidgetAction = (setEntityId = true): WidgetAction => ({
    type: WidgetActionType.updateDashboardState,
    targetDashboardStateId: null,
    openRightLayout: false,
    setEntityId,
    stateEntityParamName: null
  });

export interface WidgetComparisonSettings {
  comparisonEnabled?: boolean;
  timeForComparison?: ComparisonDuration;
  comparisonCustomIntervalValue?: number;
}

export interface DataKeyComparisonSettings {
  showValuesForComparison: boolean;
  comparisonValuesLabel: string;
  color: string;
}

export interface DataKeySettingsWithComparison {
  comparisonSettings?: DataKeyComparisonSettings;
}

export const isDataKeySettingsWithComparison = (settings: any): settings is DataKeySettingsWithComparison =>
  'comparisonSettings' in settings;

export interface WidgetSettings {
  [key: string]: any;
}

export enum WidgetConfigMode {
  basic = 'basic',
  advanced = 'advanced'
}

export interface WidgetConfig {
  configMode?: WidgetConfigMode;
  title?: string;
  titleFont?: Font;
  titleColor?: string;
  titleIcon?: string;
  showTitle?: boolean;
  showTitleIcon?: boolean;
  iconColor?: string;
  iconSize?: string;
  titleTooltip?: string;
  dropShadow?: boolean;
  enableFullscreen?: boolean;
  useDashboardTimewindow?: boolean;
  displayTimewindow?: boolean;
  timewindow?: Timewindow;
  timewindowStyle?: TimewindowStyle;
  resizable?: boolean;
  preserveAspectRatio?: boolean;
  desktopHide?: boolean;
  mobileHide?: boolean;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  borderRadius?: string;
  widgetStyle?: ComponentStyle;
  widgetCss?: string;
  titleStyle?: ComponentStyle;
  units?: TbUnit;
  decimals?: number;
  noDataDisplayMessage?: string;
  pageSize?: number;
  actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  settings?: WidgetSettings;
  alarmSource?: Datasource;
  alarmFilterConfig?: AlarmFilterConfig;
  datasources?: Array<Datasource>;
  targetDevice?: TargetDevice;
  [key: string]: any;
}

export interface BaseWidgetInfo {
  id?: string;
  typeFullFqn: string;
  type: widgetType;
}

export interface Widget extends BaseWidgetInfo, ExportableEntity<WidgetTypeId> {
  typeId?: WidgetTypeId;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
}

export interface WidgetInfo extends BaseWidgetInfo {
  title: string;
  image?: string;
  description?: string;
  deprecated?: boolean;
}

export interface DynamicFormData {
  settingsForm?: FormProperty[];
  model?: any;
  settingsDirective?: string;
}

export interface WidgetPosition {
  row: number;
  column: number;
}

export interface WidgetSize {
  sizeX: number;
  sizeY: number;
  preserveAspectRatio: boolean;
  resizable: boolean;
}

export interface IWidgetSettingsComponent {
  aliasController: IAliasController;
  callbacks: WidgetConfigCallbacks;
  dataKeyCallbacks: DataKeysCallbacks;
  functionsOnly: boolean;
  dashboard: Dashboard;
  widget: Widget;
  widgetConfig: WidgetConfigComponentData;
  functionScopeVariables: string[];
  settings: WidgetSettings;
  settingsChanged: Observable<WidgetSettings>;
  validateSettings(): boolean;
  [key: string]: any;
}

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class WidgetSettingsComponent extends PageComponent implements
  IWidgetSettingsComponent, OnInit, AfterViewInit {

  aliasController: IAliasController;

  callbacks: WidgetConfigCallbacks;

  dataKeyCallbacks: DataKeysCallbacks;

  functionsOnly: boolean;

  dashboard: Dashboard;

  widget: Widget;

  widgetConfigValue: WidgetConfigComponentData;

  set widgetConfig(value: WidgetConfigComponentData) {
    this.widgetConfigValue = value;
    this.onWidgetConfigSet(value);
  }

  get widgetConfig(): WidgetConfigComponentData {
    return this.widgetConfigValue;
  }

  functionScopeVariables: string[];

  settingsValue: WidgetSettings;

  private settingsSet = false;

  set settings(value: WidgetSettings) {
    if (!value) {
      this.settingsValue = mergeDeep({}, this.defaultSettings());
    } else {
      this.settingsValue = mergeDeepIgnoreArray({}, this.defaultSettings(), value);
    }
    if (!this.settingsSet) {
      this.settingsSet = true;
      this.setupSettings(this.settingsValue);
    } else {
      this.updateSettings(this.settingsValue);
    }
  }

  get settings(): WidgetSettings {
    return this.settingsValue;
  }

  settingsChangedEmitter = new EventEmitter<WidgetSettings>();
  settingsChanged = this.settingsChangedEmitter.asObservable();

  protected destroyRef = inject(DestroyRef);

  protected constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    if (!this.validateSettings()) {
      setTimeout(() => {
        this.onSettingsChanged(this.prepareOutputSettings(this.settingsForm().getRawValue()));
      }, 0);
    }
  }

  public validateSettings(): boolean {
    return this.settingsForm().valid;
  }

  protected setupSettings(settings: WidgetSettings) {
    this.onSettingsSet(this.prepareInputSettings(settings));
    this.updateValidators(false);
    for (const trigger of this.validatorTriggers()) {
      const path = trigger.split('.');
      let control: AbstractControl = this.settingsForm();
      for (const part of path) {
        control = control.get(part);
      }
      control.valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.settingsForm().valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.onSettingsChanged(this.prepareOutputSettings(this.settingsForm().getRawValue()));
    });
  }

  protected updateSettings(settings: WidgetSettings) {
    settings = this.prepareInputSettings(settings);
    this.settingsForm().reset(settings, {emitEvent: false});
    this.doUpdateSettings(this.settingsForm(), settings);
    this.updateValidators(false);
  }

  protected updateValidators(emitEvent: boolean, trigger?: string) {
  }

  protected validatorTriggers(): string[] {
    return [];
  }

  protected onSettingsChanged(updated: WidgetSettings) {
    this.settingsValue = updated;
    this.settingsChangedEmitter.emit(this.settingsValue);
  }

  protected doUpdateSettings(settingsForm: UntypedFormGroup, settings: WidgetSettings) {
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return settings;
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings;
  }

  protected abstract settingsForm(): UntypedFormGroup;

  protected abstract onSettingsSet(settings: WidgetSettings);

  protected defaultSettings(): WidgetSettings {
    return {};
  }

  protected onWidgetConfigSet(widgetConfig: WidgetConfigComponentData) {
  }
}
