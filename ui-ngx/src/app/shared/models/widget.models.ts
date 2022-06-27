///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import { BaseData } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { Timewindow } from '@shared/models/time/time.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AlarmSearchStatus, AlarmSeverity } from '@shared/models/alarm.models';
import { DataKeyType } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import * as moment_ from 'moment';
import { EntityDataPageLink, EntityFilter, KeyFilter } from '@shared/models/query/query.models';
import { PopoverPlacement } from '@shared/components/popover.models';
import { PageComponent } from '@shared/components/page.component';
import { AfterViewInit, Directive, EventEmitter, Inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { AbstractControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { Dashboard } from '@shared/models/dashboard.models';
import { IAliasController } from '@core/api/widget-api.models';
import { isEmptyStr } from '@core/utils';

export enum widgetType {
  timeseries = 'timeseries',
  latest = 'latest',
  rpc = 'rpc',
  alarm = 'alarm',
  static = 'static'
}

export interface WidgetTypeTemplate {
  bundleAlias: string;
  alias: string;
}

export interface WidgetTypeData {
  name: string;
  icon: string;
  isMdiIcon?: boolean;
  configHelpLinkId: string;
  template: WidgetTypeTemplate;
}

export const widgetTypesData = new Map<widgetType, WidgetTypeData>(
  [
    [
      widgetType.timeseries,
      {
        name: 'widget.timeseries',
        icon: 'timeline',
        configHelpLinkId: 'widgetsConfigTimeseries',
        template: {
          bundleAlias: 'charts',
          alias: 'basic_timeseries'
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
          bundleAlias: 'cards',
          alias: 'attributes_card'
        }
      }
    ],
    [
      widgetType.rpc,
      {
        name: 'widget.rpc',
        icon: 'mdi:developer-board',
        configHelpLinkId: 'widgetsConfigRpc',
        isMdiIcon: true,
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
        icon: 'error',
        configHelpLinkId: 'widgetsConfigAlarm',
        template: {
          bundleAlias: 'alarm_widgets',
          alias: 'alarms_table'
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
          bundleAlias: 'cards',
          alias: 'html_card'
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
  controllerScript: string;
  settingsSchema?: string | any;
  dataKeySettingsSchema?: string | any;
  latestDataKeySettingsSchema?: string | any;
  settingsDirective?: string;
  dataKeySettingsDirective?: string;
  latestDataKeySettingsDirective?: string;
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

}

export interface WidgetControllerDescriptor {
  widgetTypeFunction?: any;
  settingsSchema?: string | any;
  dataKeySettingsSchema?: string | any;
  latestDataKeySettingsSchema?: string | any;
  typeParameters?: WidgetTypeParameters;
  actionSources?: {[actionSourceId: string]: WidgetActionSource};
}

export interface BaseWidgetType extends BaseData<WidgetTypeId> {
  tenantId: TenantId;
  bundleAlias: string;
  alias: string;
  name: string;
}

export interface WidgetType extends BaseWidgetType {
  descriptor: WidgetTypeDescriptor;
}

export interface WidgetTypeInfo extends BaseWidgetType {
  image: string;
  description: string;
  widgetType: widgetType;
}

export interface WidgetTypeDetails extends WidgetType {
  image: string;
  description: string;
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
  sortDataKeys: boolean;
  showMin: boolean;
  showMax: boolean;
  showAvg: boolean;
  showTotal: boolean;
  showLatest: boolean;
}

export function defaultLegendConfig(wType: widgetType): LegendConfig {
  return {
    direction: LegendDirection.column,
    position: LegendPosition.bottom,
    sortDataKeys: false,
    showMin: false,
    showMax: false,
    showAvg: wType === widgetType.timeseries,
    showTotal: false,
    showLatest: false
  };
}

export interface KeyInfo {
  name: string;
  label?: string;
  color?: string;
  funcBody?: string;
  postFuncBody?: string;
  units?: string;
  decimals?: number;
}

export interface DataKey extends KeyInfo {
  type: DataKeyType;
  pattern?: string;
  settings?: any;
  usePostProcessing?: boolean;
  hidden?: boolean;
  inLegend?: boolean;
  isAdditional?: boolean;
  origDataKeyIndex?: number;
  _hash?: number;
}

export enum DatasourceType {
  function = 'function',
  entity = 'entity',
  entityCount = 'entityCount'
}

export const datasourceTypeTranslationMap = new Map<DatasourceType, string>(
  [
    [ DatasourceType.function, 'function.function' ],
    [ DatasourceType.entity, 'entity.entity' ],
    [ DatasourceType.entityCount, 'entity.entities-count' ]
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
  dataKeyStartIndex?: number;
  latestDataKeyStartIndex?: number;
  [key: string]: any;
}

export interface FormattedData {
  $datasource: Datasource;
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

export type DataSet = [number, any][];

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

export enum WidgetActionType {
  openDashboardState = 'openDashboardState',
  updateDashboardState = 'updateDashboardState',
  openDashboard = 'openDashboard',
  custom = 'custom',
  customPretty = 'customPretty',
  mobileAction = 'mobileAction'
}

export enum WidgetMobileActionType {
  takePictureFromGallery = 'takePictureFromGallery',
  takePhoto = 'takePhoto',
  mapDirection = 'mapDirection',
  mapLocation = 'mapLocation',
  scanQrCode = 'scanQrCode',
  makePhoneCall = 'makePhoneCall',
  getLocation = 'getLocation',
  takeScreenshot = 'takeScreenshot'
}

export const widgetActionTypeTranslationMap = new Map<WidgetActionType, string>(
  [
    [ WidgetActionType.openDashboardState, 'widget-action.open-dashboard-state' ],
    [ WidgetActionType.updateDashboardState, 'widget-action.update-dashboard-state' ],
    [ WidgetActionType.openDashboard, 'widget-action.open-dashboard' ],
    [ WidgetActionType.custom, 'widget-action.custom' ],
    [ WidgetActionType.customPretty, 'widget-action.custom-pretty' ],
    [ WidgetActionType.mobileAction, 'widget-action.mobile-action' ]
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
    [ WidgetMobileActionType.takeScreenshot, 'widget-action.mobile.take-screenshot' ]
  ]
);

export interface MobileLaunchResult {
  launched: boolean;
}

export interface MobileImageResult {
  imageUrl: string;
}

export interface MobileQrCodeResult {
  code: string;
  format: string;
}

export interface MobileLocationResult {
  latitude: number;
  longitude: number;
}

export type MobileActionResult = MobileLaunchResult &
                                 MobileImageResult &
                                 MobileQrCodeResult &
                                 MobileLocationResult;

export interface WidgetMobileActionResult<T extends MobileActionResult> {
  result?: T;
  hasResult: boolean;
  error?: string;
  hasError: boolean;
}

export interface ProcessImageDescriptor {
  processImageFunction: string;
}

export interface ProcessLaunchResultDescriptor {
  processLaunchResultFunction?: string;
}

export interface LaunchMapDescriptor extends ProcessLaunchResultDescriptor {
  getLocationFunction: string;
}

export interface ScanQrCodeDescriptor {
  processQrCodeFunction: string;
}

export interface MakePhoneCallDescriptor extends ProcessLaunchResultDescriptor {
  getPhoneNumberFunction: string;
}

export interface GetLocationDescriptor {
  processLocationFunction: string;
}

export type WidgetMobileActionDescriptors = ProcessImageDescriptor &
                                            LaunchMapDescriptor &
                                            ScanQrCodeDescriptor &
                                            MakePhoneCallDescriptor &
                                            GetLocationDescriptor;

export interface WidgetMobileActionDescriptor extends WidgetMobileActionDescriptors {
  type: WidgetMobileActionType;
  handleErrorFunction?: string;
  handleEmptyResultFunction?: string;
}

export interface CustomActionDescriptor {
  customFunction?: string;
  customResources?: Array<WidgetResource>;
  customHtml?: string;
  customCss?: string;
}

export interface WidgetActionDescriptor extends CustomActionDescriptor {
  id: string;
  name: string;
  icon: string;
  displayName?: string;
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
  useShowWidgetActionFunction?: boolean;
  showWidgetActionFunction?: string;
}

export interface WidgetComparisonSettings {
  comparisonEnabled?: boolean;
  timeForComparison?: moment_.unitOfTime.DurationConstructor;
  comparisonCustomIntervalValue?: number;
}

export interface WidgetSettings {
  [key: string]: any;
}

export interface WidgetConfig {
  title?: string;
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
  showLegend?: boolean;
  legendConfig?: LegendConfig;
  timewindow?: Timewindow;
  mobileHide?: boolean;
  mobileHeight?: number;
  mobileOrder?: number;
  color?: string;
  backgroundColor?: string;
  padding?: string;
  margin?: string;
  widgetStyle?: {[klass: string]: any};
  widgetCss?: string;
  titleStyle?: {[klass: string]: any};
  units?: string;
  decimals?: number;
  noDataDisplayMessage?: string;
  pageSize?: number;
  actions?: {[actionSourceId: string]: Array<WidgetActionDescriptor>};
  settings?: WidgetSettings;
  alarmSource?: Datasource;
  alarmStatusList?: AlarmSearchStatus[];
  alarmSeverityList?: AlarmSeverity[];
  alarmTypeList?: string[];
  searchPropagatedAlarms?: boolean;
  datasources?: Array<Datasource>;
  targetDeviceAliasIds?: Array<string>;
  [key: string]: any;
}

export interface Widget extends WidgetInfo{
  typeId?: WidgetTypeId;
  sizeX: number;
  sizeY: number;
  row: number;
  col: number;
  config: WidgetConfig;
}

export interface WidgetInfo {
  id?: string;
  isSystemType: boolean;
  bundleAlias: string;
  typeAlias: string;
  type: widgetType;
  title: string;
  image?: string;
  description?: string;
}

export interface GroupInfo {
  formIndex: number;
  GroupTitle: string;
}

export interface JsonSchema {
  type: string;
  title?: string;
  properties: {[key: string]: any};
  required?: string[];
}

export interface JsonSettingsSchema {
  schema?: JsonSchema;
  form?: any[];
  groupInfoes?: GroupInfo[];
}

export interface WidgetPosition {
  row: number;
  column: number;
}

export interface WidgetSize {
  sizeX: number;
  sizeY: number;
}

export interface IWidgetSettingsComponent {
  aliasController: IAliasController;
  dashboard: Dashboard;
  widget: Widget;
  functionScopeVariables: string[];
  settings: WidgetSettings;
  settingsChanged: Observable<WidgetSettings>;
  validate();
  [key: string]: any;
}

function removeEmptyWidgetSettings(settings: WidgetSettings): WidgetSettings {
  if (settings) {
    const keys = Object.keys(settings);
    for (const key of keys) {
      const val = settings[key];
      if (val === null || isEmptyStr(val)) {
        delete settings[key];
      }
    }
  }
  return settings;
}

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class WidgetSettingsComponent extends PageComponent implements
  IWidgetSettingsComponent, OnInit, AfterViewInit {

  aliasController: IAliasController;

  dashboard: Dashboard;

  widget: Widget;

  functionScopeVariables: string[];

  settingsValue: WidgetSettings;

  private settingsSet = false;

  set settings(value: WidgetSettings) {
    if (!value) {
      this.settingsValue = this.defaultSettings();
    } else {
      this.settingsValue = {...this.defaultSettings(), ...value};
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

  protected constructor(@Inject(Store) protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (!this.validateSettings()) {
        this.settingsChangedEmitter.emit(null);
      }
    }, 0);
  }

  validate() {
    this.onValidate();
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
      control.valueChanges.subscribe(() => {
        this.updateValidators(true, trigger);
      });
    }
    this.settingsForm().valueChanges.subscribe((updated: any) => {
      this.onSettingsChanged(this.prepareOutputSettings(updated));
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
    this.settingsValue = removeEmptyWidgetSettings(updated);
    if (this.validateSettings()) {
      this.settingsChangedEmitter.emit(this.settingsValue);
    } else {
      this.settingsChangedEmitter.emit(null);
    }
  }

  protected doUpdateSettings(settingsForm: FormGroup, settings: WidgetSettings) {
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return settings;
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings;
  }

  protected validateSettings(): boolean {
    return this.settingsForm().valid;
  }

  protected onValidate() {}

  protected abstract settingsForm(): FormGroup;

  protected abstract onSettingsSet(settings: WidgetSettings);

  protected defaultSettings(): WidgetSettings {
    return {};
  }

}
