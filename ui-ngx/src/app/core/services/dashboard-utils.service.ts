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

import { Injectable } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { TimeService } from '@core/services/time.service';
import {
  BreakpointId,
  breakpointIdIconMap,
  breakpointIdTranslationMap,
  BreakpointInfo,
  BreakpointLayoutInfo,
  BreakpointSystemId,
  Dashboard,
  DashboardConfiguration,
  DashboardLayout,
  DashboardLayoutId,
  DashboardLayoutsInfo,
  DashboardState,
  DashboardStateLayouts,
  GridSettings,
  LayoutType,
  WidgetLayout
} from '@shared/models/dashboard.models';
import {
  deepClean,
  deepClone,
  isDefined,
  isDefinedAndNotNull,
  isNotEmptyStr,
  isString,
  isUndefined
} from '@core/utils';
import {
  Datasource,
  datasourcesHasAggregation,
  datasourcesHasOnlyComparisonAggregation,
  DatasourceType,
  defaultLegendConfig,
  isValidWidgetFullFqn,
  TargetDevice,
  TargetDeviceType,
  Widget,
  WidgetConfig,
  WidgetConfigMode,
  WidgetSize,
  widgetType,
  WidgetTypeDescriptor, widgetTypeHasTimewindow
} from '@app/shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AliasFilterType, EntityAlias, EntityAliasFilter } from '@app/shared/models/alias.models';
import { EntityId } from '@app/shared/models/id/entity-id';
import { initModelFromDefaultTimewindow } from '@shared/models/time/time.models';
import { AlarmSearchStatus } from '@shared/models/alarm.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { BackgroundType, colorBackground, isBackgroundSettings } from '@shared/models/widget-settings.models';
import { MediaBreakpoints } from '@shared/models/constants';
import { TranslateService } from '@ngx-translate/core';
import { DashboardPageLayout } from '@home/components/dashboard-page/dashboard-page.models';
import { maxGridsterCol, maxGridsterRow } from '@home/models/dashboard-component.models';
import { findWidgetModelDefinition, widgetHasTimewindow } from '@shared/models/widget/widget-model.definition';

@Injectable({
  providedIn: 'root'
})
export class DashboardUtilsService {

  private systemBreakpoints: {[key in BreakpointSystemId]?: BreakpointInfo};

  constructor(private utils: UtilsService,
              private timeService: TimeService,
              private translate: TranslateService) {
  }

  public validateAndUpdateDashboard(dashboard: Dashboard): Dashboard {
    if (!dashboard.configuration) {
      dashboard.configuration = {
        entityAliases: {},
        filters: {},
      };
    }
    if (isUndefined(dashboard.configuration.widgets)) {
      dashboard.configuration.widgets = {};
    } else if (Array.isArray(dashboard.configuration.widgets)) {
      const widgetsMap: {[id: string]: Widget} = {};
      dashboard.configuration.widgets.forEach((widget) => {
        if (!widget.id) {
          widget.id = this.utils.guid();
        }
        widgetsMap[widget.id] = widget;
      });
      dashboard.configuration.widgets = widgetsMap;
    }
    for (const id of Object.keys(dashboard.configuration.widgets)) {
      const widget = dashboard.configuration.widgets[id];
      dashboard.configuration.widgets[id] = this.validateAndUpdateWidget(widget);
    }
    if (isUndefined(dashboard.configuration.states)) {
      dashboard.configuration.states = {
        default: this.createDefaultState(dashboard.title, true)
      };

      const mainLayout = dashboard.configuration.states.default.layouts.main;
      for (const id of Object.keys(dashboard.configuration.widgets)) {
        const widget = dashboard.configuration.widgets[id];
        mainLayout.widgets[id] = {
          sizeX: widget.sizeX,
          sizeY: widget.sizeY,
          row: widget.row,
          col: widget.col
        };
        if (isDefined(widget.config.mobileHeight)) {
          mainLayout.widgets[id].mobileHeight = widget.config.mobileHeight;
        }
        if (isDefined(widget.config.mobileOrder)) {
          mainLayout.widgets[id].mobileOrder = widget.config.mobileOrder;
        }
      }
    } else {
      const states = dashboard.configuration.states;
      let rootFound = false;
      for (const stateId of Object.keys(states)) {
        const state = states[stateId];
        if (isUndefined(state.root)) {
          state.root = false;
        } else if (state.root) {
          rootFound = true;
        }
        this.validateAndUpdateState(state);
      }
      if (!rootFound) {
        const firstStateId = Object.keys(states)[0];
        states[firstStateId].root = true;
      }
    }
    const datasourcesByAliasId: {[aliasId: string]: Array<Datasource>} = {};
    const targetDevicesByAliasId: {[aliasId: string]: Array<TargetDevice>} = {};
    for (const widgetId of Object.keys(dashboard.configuration.widgets)) {
      const widget = dashboard.configuration.widgets[widgetId];
      const datasources = widget.type === widgetType.alarm ? [widget.config.alarmSource] : widget.config.datasources;
      datasources.forEach((datasource) => {
        if (datasource.entityAliasId) {
          const aliasId = datasource.entityAliasId;
          let aliasDatasources = datasourcesByAliasId[aliasId];
          if (!aliasDatasources) {
            aliasDatasources = [];
            datasourcesByAliasId[aliasId] = aliasDatasources;
          }
          aliasDatasources.push(datasource);
        }
      });
      if (widget.config.targetDevice?.type === TargetDeviceType.entity && widget.config.targetDevice.entityAliasId) {
        const aliasId = widget.config.targetDevice.entityAliasId;
        let targetDevicesList = targetDevicesByAliasId[aliasId];
        if (!targetDevicesList) {
          targetDevicesList = [];
          targetDevicesByAliasId[aliasId] = targetDevicesList;
        }
        targetDevicesList.push(widget.config.targetDevice);
      }
    }

    dashboard.configuration = this.validateAndUpdateEntityAliases(dashboard.configuration, datasourcesByAliasId, targetDevicesByAliasId);
    if (!dashboard.configuration.filters) {
      dashboard.configuration.filters = {};
    }

    dashboard.configuration.timewindow = initModelFromDefaultTimewindow(dashboard.configuration.timewindow,
      false, false, this.timeService, true, true);
    if (isUndefined(dashboard.configuration.settings)) {
      dashboard.configuration.settings = {};
      dashboard.configuration.settings.stateControllerId = 'entity';
      dashboard.configuration.settings.showTitle = false;
      dashboard.configuration.settings.showDashboardsSelect = true;
      dashboard.configuration.settings.showEntitiesSelect = true;
      dashboard.configuration.settings.showDashboardTimewindow = true;
      dashboard.configuration.settings.showDashboardExport = true;
      dashboard.configuration.settings.toolbarAlwaysOpen = true;
    } else {
      if (isUndefined(dashboard.configuration.settings.stateControllerId)) {
        dashboard.configuration.settings.stateControllerId = 'entity';
      }
    }
    if (isDefined(dashboard.configuration.gridSettings)) {
      const gridSettings = dashboard.configuration.gridSettings;
      if (isDefined(gridSettings.showTitle)) {
        dashboard.configuration.settings.showTitle = gridSettings.showTitle;
        delete gridSettings.showTitle;
      }
      if (isDefined(gridSettings.titleColor)) {
        dashboard.configuration.settings.titleColor = gridSettings.titleColor;
        delete gridSettings.titleColor;
      }
      if (isDefined(gridSettings.showDevicesSelect)) {
        dashboard.configuration.settings.showEntitiesSelect = gridSettings.showDevicesSelect;
        delete gridSettings.showDevicesSelect;
      }
      if (isDefined(gridSettings.showEntitiesSelect)) {
        dashboard.configuration.settings.showEntitiesSelect = gridSettings.showEntitiesSelect;
        delete gridSettings.showEntitiesSelect;
      }
      if (isDefined(gridSettings.showDashboardTimewindow)) {
        dashboard.configuration.settings.showDashboardTimewindow = gridSettings.showDashboardTimewindow;
        delete gridSettings.showDashboardTimewindow;
      }
      if (isDefined(gridSettings.showDashboardExport)) {
        dashboard.configuration.settings.showDashboardExport = gridSettings.showDashboardExport;
        delete gridSettings.showDashboardExport;
      }
      dashboard.configuration.states.default.layouts.main.gridSettings = gridSettings;
      delete dashboard.configuration.gridSettings;
    }
    return dashboard;
  }

  public createSingleWidgetDashboard(widget: Widget): Dashboard {
    if (!widget.id) {
      widget.id = this.utils.guid();
    }
    let dashboard: Dashboard = {};
    dashboard = this.validateAndUpdateDashboard(dashboard);
    dashboard.configuration.widgets[widget.id] = widget;
    dashboard.configuration.states.default.layouts.main.widgets[widget.id] = {
      sizeX: widget.sizeX,
      sizeY: widget.sizeY,
      row: widget.row,
      col: widget.col,
    };
    return dashboard;
  }

  public validateAndUpdateWidget(widget: Widget): Widget {
    widget.config = this.validateAndUpdateWidgetConfig(widget.config, widget.type);
    widget = this.validateAndUpdateWidgetTypeFqn(widget);
    this.removeTimewindowConfigIfUnused(widget);
    if (isDefined((widget as any).title)) {
      delete (widget as any).title;
    }
    if (isDefined((widget as any).image)) {
      delete (widget as any).image;
    }
    if (isDefined((widget as any).description)) {
      delete (widget as any).description;
    }
    // Temp workaround
    if (['system.charts.state_chart',
         'system.charts.basic_timeseries',
         'system.charts.timeseries_bars_flot'].includes(widget.typeFullFqn)) {
      const widgetConfig = widget.config;
      const widgetSettings = widget.config.settings;
      if (isDefinedAndNotNull(widgetConfig.showLegend)) {
        widgetSettings.showLegend = widgetConfig.showLegend;
        delete widgetConfig.showLegend;
      } else if (isUndefined(widgetSettings.showLegend)) {
        widgetSettings.showLegend = true;
      }
      if (isDefinedAndNotNull(widgetConfig.legendConfig)) {
        widgetSettings.legendConfig = widgetConfig.legendConfig;
        delete widgetConfig.legendConfig;
      } else if (isUndefined(widgetSettings.legendConfig)) {
        widgetSettings.legendConfig = defaultLegendConfig(widget.type);
      }
    }
    return widget;
  }

  private validateAndUpdateWidgetTypeFqn(widget: Widget): Widget {
    const w = widget as any;
    if (!isValidWidgetFullFqn(widget.typeFullFqn)) {
      if (isDefinedAndNotNull(w.isSystemType) && isNotEmptyStr(w.bundleAlias) && isNotEmptyStr(w.typeAlias)) {
        widget.typeFullFqn = (w.isSystemType ? 'system' : 'tenant') + '.' + w.bundleAlias + '.' + w.typeAlias;
      }
    }
    if (isDefined(w.isSystemType)) {
      delete w.isSystemType;
    }
    if (isDefined(w.bundleAlias)) {
      delete w.bundleAlias;
    }
    if (isDefined(w.typeAlias)) {
      delete w.typeAlias;
    }
    // Temp workaround
    if (widget.typeFullFqn === 'system.charts.timeseries') {
      widget.typeFullFqn = 'system.charts.basic_timeseries';
    }
    return widget;
  }

  public validateAndUpdateWidgetConfig(widgetConfig: WidgetConfig | undefined, type: widgetType): WidgetConfig {
    if (!widgetConfig) {
      widgetConfig = {};
    }
    widgetConfig.datasources = this.validateAndUpdateDatasources(widgetConfig.datasources);
    if (type === widgetType.latest) {
      if (datasourcesHasAggregation(widgetConfig.datasources)) {
        const onlyHistoryTimewindow = datasourcesHasOnlyComparisonAggregation(widgetConfig.datasources);
        widgetConfig.timewindow = initModelFromDefaultTimewindow(widgetConfig.timewindow, true,
          onlyHistoryTimewindow, this.timeService, false);
      }
    } else if (type === widgetType.rpc) {
      if (widgetConfig.targetDeviceAliasIds && widgetConfig.targetDeviceAliasIds.length) {
        widgetConfig.targetDevice = {
          type: TargetDeviceType.entity,
          entityAliasId: widgetConfig.targetDeviceAliasIds[0]
        };
        delete widgetConfig.targetDeviceAliasIds;
      } else if (!widgetConfig.targetDevice) {
        widgetConfig.targetDevice = {
          type: TargetDeviceType.device
        };
      }
    } else if (type === widgetType.alarm) {
      if (!widgetConfig.alarmFilterConfig) {
        widgetConfig.alarmFilterConfig = {};
        const alarmFilterConfig = widgetConfig.alarmFilterConfig;
        if (isDefined(widgetConfig.alarmStatusList) && widgetConfig.alarmStatusList.length) {
          alarmFilterConfig.statusList = widgetConfig.alarmStatusList;
        } else if (isDefined(widgetConfig.alarmSearchStatus) && widgetConfig.alarmSearchStatus !== AlarmSearchStatus.ANY) {
          alarmFilterConfig.statusList = [widgetConfig.alarmSearchStatus];
        } else {
          alarmFilterConfig.statusList = [];
        }
        if (isDefined(widgetConfig.alarmStatusList)) {
          delete widgetConfig.alarmStatusList;
        }
        if (isDefined(widgetConfig.alarmSearchStatus)) {
          delete widgetConfig.alarmSearchStatus;
        }
        if (isDefined(widgetConfig.alarmSeverityList)) {
          alarmFilterConfig.severityList = widgetConfig.alarmSeverityList;
          delete widgetConfig.alarmSeverityList;
        } else {
          alarmFilterConfig.severityList = [];
        }
        if (isDefined(widgetConfig.alarmTypeList)) {
          alarmFilterConfig.typeList = widgetConfig.alarmTypeList;
          delete widgetConfig.alarmTypeList;
        } else {
          alarmFilterConfig.typeList = [];
        }
        if (isDefined(widgetConfig.searchPropagatedAlarms)) {
          alarmFilterConfig.searchPropagatedAlarms = widgetConfig.searchPropagatedAlarms;
          delete widgetConfig.searchPropagatedAlarms;
        } else {
          alarmFilterConfig.searchPropagatedAlarms = true;
        }
      }
    }
    return deepClean(widgetConfig, {cleanKeys: ['_hash'], cleanOnlyKey: true});
  }

  private removeTimewindowConfigIfUnused(widget: Widget) {
    const hasTimewindow = widgetHasTimewindow(widget);
    if (!hasTimewindow || widget.config.useDashboardTimewindow) {
      delete widget.config.displayTimewindow;
      delete widget.config.timewindow;

      if (!hasTimewindow) {
        delete widget.config.useDashboardTimewindow;
      }
    }
  }

  public prepareWidgetForSaving(widget: Widget): Widget {
    this.removeTimewindowConfigIfUnused(widget);
    widget = deepClean(widget, {cleanKeys: ['_hash'], cleanOnlyKey: true});
    return widget;
  }

  public prepareWidgetForScadaLayout(widget: Widget, isScada: boolean): Widget {
    const config = widget.config;
    config.showTitle = false;
    config.dropShadow = false;
    config.resizable = true;
    config.preserveAspectRatio = isScada;
    config.padding = '0';
    config.margin = '0';
    config.backgroundColor = 'rgba(0,0,0,0)';
    const settings = config.settings || {};
    settings.padding = '0';
    const background = settings.background;
    if (isBackgroundSettings(background)) {
      background.type = BackgroundType.color;
      background.color = 'rgba(0,0,0,0)';
      background.overlay.enabled = false;
    } else {
      settings.background = colorBackground('rgba(0,0,0,0)');
    }
    config.settings = settings;
    return widget;
  }

  public validateAndUpdateDatasources(datasources?: Datasource[]): Datasource[] {
    if (!datasources) {
      datasources = [];
    }
    datasources.forEach((datasource) => {
      if (datasource.deviceAliasId) {
        datasource.type = DatasourceType.entity;
        datasource.entityAliasId = datasource.deviceAliasId;
        delete datasource.deviceAliasId;
      }
      if (datasource.deviceName) {
        datasource.entityName = datasource.deviceName;
        delete datasource.deviceName;
      }
      if (datasource.type === DatasourceType.entity && datasource.entityId) {
        datasource.name = datasource.entityName;
      }
      if (!datasource.dataKeys) {
        datasource.dataKeys = [];
      }
      datasource.dataKeys.forEach(dataKey => {
        if (isUndefined(dataKey.label)) {
          dataKey.label = dataKey.name;
        }
      });
    });
    return datasources;
  }

  public getWidgetDatasources(widget: Widget): Datasource[] {
    const widgetDefinition = findWidgetModelDefinition(widget);
    if (widgetDefinition) {
      return widgetDefinition.datasources(widget);
    }
    return this.validateAndUpdateDatasources(widget.config.datasources);
  }

  public createDefaultLayoutData(): DashboardLayout {
    return {
      widgets: {},
      gridSettings: this.createDefaultGridSettings()
    };
  }

  private createDefaultGridSettings(): GridSettings {
    return {
      layoutType: LayoutType.default,
      backgroundColor: '#eeeeee',
      columns: 24,
      margin: 10,
      outerMargin: true,
      backgroundSizeMode: '100%'
    };
  }

  public createDefaultLayouts(): DashboardStateLayouts {
    return {
      main: this.createDefaultLayoutData()
    };
  }

  public createDefaultState(name: string, root: boolean): DashboardState {
    return {
      name,
      root,
      layouts: this.createDefaultLayouts()
    };
  }

  public createSingleEntityFilter(entityId: EntityId): EntityAliasFilter {
    return {
      type: AliasFilterType.singleEntity,
      singleEntity: entityId,
      resolveMultiple: false
    };
  }

  public widgetConfigFromWidgetType(widgetTypeDescriptor: WidgetTypeDescriptor): WidgetConfig {
    const config: WidgetConfig = JSON.parse(widgetTypeDescriptor.defaultConfig);
    config.datasources = this.convertDatasourcesFromWidgetType(widgetTypeDescriptor, config, config.datasources);
    if (isDefinedAndNotNull(config.alarmSource)) {
      config.alarmSource = this.convertDatasourceFromWidgetType(widgetTypeDescriptor, config, config.alarmSource);
    }
    return config;
  }

  private convertDatasourcesFromWidgetType(widgetTypeDescriptor: WidgetTypeDescriptor,
                                           config: WidgetConfig, datasources?: Datasource[]): Datasource[] {
    const newDatasources: Datasource[] = [];
    if (datasources?.length) {
      newDatasources.push(this.convertDatasourceFromWidgetType(widgetTypeDescriptor, config, datasources[0]));
    }
    return newDatasources;
  }

  private convertDatasourceFromWidgetType(widgetTypeDescriptor: WidgetTypeDescriptor, config: WidgetConfig,
                                          datasource: Datasource): Datasource {
    const newDatasource = deepClone(datasource);
    if (newDatasource.type === DatasourceType.function) {
      newDatasource.type = DatasourceType.entity;
      newDatasource.name = '';
      if (widgetTypeDescriptor.hasBasicMode && config.configMode === WidgetConfigMode.basic) {
        newDatasource.type = DatasourceType.device;
      }
      const dataKeys = newDatasource.dataKeys;
      newDatasource.dataKeys = [];
      if (widgetTypeDescriptor.type === widgetType.alarm) {
        dataKeys.forEach(dataKey => {
          const newDataKey = deepClone(dataKey);
          newDataKey.funcBody = null;
          newDataKey.type = DataKeyType.alarm;
          newDatasource.dataKeys.push(newDataKey);
        });
      }
    }
    return newDatasource;
  }

  private validateAndUpdateState(state: DashboardState) {
    if (!state.layouts) {
      state.layouts = this.createDefaultLayouts();
    }
    for (const l of Object.keys(state.layouts)) {
      const layout = state.layouts[l as DashboardLayoutId];
      this.validateAndUpdateLayout(layout);
    }
  }

  private validateAndUpdateLayout(layout: DashboardLayout) {
    if (!layout.gridSettings) {
      layout.gridSettings = this.createDefaultGridSettings();
    }
    if ((layout.gridSettings as any).margins && (layout.gridSettings as any).margins.length === 2) {
      layout.gridSettings.margin = (layout.gridSettings as any).margins[0];
      delete (layout.gridSettings as any).margins;
    }
    layout.gridSettings.outerMargin = isDefined(layout.gridSettings.outerMargin) ? layout.gridSettings.outerMargin : true;
    layout.gridSettings.margin = isDefined(layout.gridSettings.margin) ? layout.gridSettings.margin : 10;
    if (!layout.gridSettings.layoutType) {
      layout.gridSettings.layoutType = LayoutType.default;
    }
  }

  public setLayouts(dashboard: Dashboard, targetState: string, newLayouts: DashboardStateLayouts) {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const state = states[targetState];
    let addedCount = 0;
    let removedCount = 0;
    for (const l of Object.keys(state.layouts)) {
      if (!newLayouts[l]) {
        removedCount++;
      }
    }
    for (const l of Object.keys(newLayouts)) {
      if (!state.layouts[l]) {
        addedCount++;
      }
    }
    state.layouts = newLayouts;
    const layoutsCount = Object.keys(state.layouts).length;
    let newColumns;
    if (addedCount) {
      for (const l of Object.keys(state.layouts)) {
        newColumns = state.layouts[l].gridSettings.columns * (layoutsCount - addedCount) / layoutsCount;
        if (newColumns > 0) {
          state.layouts[l].gridSettings.columns = newColumns;
        }
      }
    }
    if (removedCount) {
      for (const l of Object.keys(state.layouts)) {
        newColumns = state.layouts[l].gridSettings.columns * (layoutsCount + removedCount) / layoutsCount;
        if (newColumns > 0) {
          state.layouts[l].gridSettings.columns = newColumns;
        }
      }
    }
    this.removeUnusedWidgets(dashboard);
  }

  public isReferenceWidget(dashboard: Dashboard, widgetId: string): boolean {
    const states = dashboard.configuration.states;
    let foundWidgetRefs = 0;

    for (const state of Object.values(states)) {
      for (const layout of Object.values(state.layouts)) {
        if (layout.widgets[widgetId]) {
          foundWidgetRefs++;
        }
        if (layout.breakpoints) {
          for (const breakpoint of Object.values(layout.breakpoints)) {
            if (breakpoint.widgets[widgetId]) {
              foundWidgetRefs++;
            }
          }
        }
      }
    }

    return foundWidgetRefs > 1;
  }

  public getRootStateId(states: {[id: string]: DashboardState }): string {
    for (const stateId of Object.keys(states)) {
      const state = states[stateId];
      if (state.root) {
        return stateId;
      }
    }
    return Object.keys(states)[0];
  }

  public getStateLayoutsData(dashboard: Dashboard, targetState: string): DashboardLayoutsInfo {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const state = states[targetState];
    if (state) {
      const result: DashboardLayoutsInfo = {};
      for (const l of Object.keys(state.layouts)) {
        const layout: DashboardLayout = state.layouts[l];
        if (layout) {
          result[l]= {
            default: this.getBreakpointLayoutData(layout)
          };
          if (layout.breakpoints) {
            for (const breakpoint of Object.keys(layout.breakpoints)) {
              result[l][breakpoint] = this.getBreakpointLayoutData(layout.breakpoints[breakpoint]);
            }
          }
        }
      }
      return result;
    } else {
      return null;
    }
  }

  private getBreakpointLayoutData(layout: DashboardLayout): BreakpointLayoutInfo {
    const result: BreakpointLayoutInfo = {
      widgetIds: [],
      widgetLayouts: {},
      gridSettings: {}
    };
    for (const id of Object.keys(layout.widgets)) {
      result.widgetIds.push(id);
    }
    result.widgetLayouts = layout.widgets;
    result.gridSettings = layout.gridSettings;
    return result;
  }

  public getWidgetsArray(dashboard: Dashboard): Array<Widget> {
    const widgetsArray: Array<Widget> = [];
    const dashboardConfiguration = dashboard.configuration;
    const widgets = dashboardConfiguration.widgets;
    for (const widgetId of Object.keys(widgets)) {
      const widget = widgets[widgetId];
      widgetsArray.push(widget);
    }
    return widgetsArray;
  }

  public isEmptyDashboard(dashboard: Dashboard): boolean {
    if (dashboard?.configuration?.widgets) {
      return Object.keys(dashboard?.configuration?.widgets).length === 0;
    } else {
      return true;
    }
  }

  public addWidgetToLayout(dashboard: Dashboard,
                           targetState: string,
                           targetLayout: DashboardLayoutId,
                           widget: Widget,
                           originalColumns?: number,
                           originalSize?: WidgetSize,
                           row?: number,
                           column?: number,
                           breakpoint = 'default'): void {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const state = states[targetState];
    let layout = state.layouts[targetLayout];
    if (breakpoint !== 'default' && layout.breakpoints?.[breakpoint]) {
      layout = layout.breakpoints[breakpoint];
    }
    const layoutCount = Object.keys(state.layouts).length;
    if (!widget.id) {
      widget.id = this.utils.guid();
    }
    if (!dashboardConfiguration.widgets[widget.id]) {
      dashboardConfiguration.widgets[widget.id] = widget;
    }
    const widgetLayout: WidgetLayout = {
      sizeX: originalSize ? originalSize.sizeX : widget.sizeX,
      sizeY: originalSize ? originalSize.sizeY : widget.sizeY,
      mobileOrder: widget.config.mobileOrder,
      mobileHeight: widget.config.mobileHeight,
      mobileHide: widget.config.mobileHide,
      desktopHide: widget.config.desktopHide,
      preserveAspectRatio: originalSize ? originalSize.preserveAspectRatio : widget.config.preserveAspectRatio,
      resizable: originalSize ? originalSize.resizable : widget.config.resizable
    };
    if (isUndefined(originalColumns)) {
      originalColumns = 24;
    }
    const gridSettings = layout.gridSettings;
    let columns = 24;
    if (gridSettings && gridSettings.columns) {
      columns = gridSettings.columns;
    }
    columns = columns * layoutCount;
    if (columns !== originalColumns) {
      const ratio = columns / originalColumns;
      widgetLayout.sizeX *= ratio;
      widgetLayout.sizeY *= ratio;
    }

    if (row > -1 && column > - 1) {
      widgetLayout.row = row;
      widgetLayout.col = column;
      if (this.hasWidgetCollision(widgetLayout.row, widgetLayout.col,
                                  widgetLayout.sizeX, widgetLayout.sizeY, Object.values(layout.widgets))) {
        this.widgetPossiblePosition(widgetLayout, layout);
      }
    } else {
      row = 0;
      for (const w of Object.keys(layout.widgets)) {
        const existingLayout = layout.widgets[w];
        const wRow = existingLayout.row ? existingLayout.row : 0;
        const wSizeY = existingLayout.sizeY ? existingLayout.sizeY : 1;
        const bottom = wRow + wSizeY;
        row = Math.max(row, bottom);
      }
      widgetLayout.row = row;
      widgetLayout.col = 0;
    }

    widgetLayout.sizeX = Math.floor(widgetLayout.sizeX);
    widgetLayout.sizeY = Math.floor(widgetLayout.sizeY);
    widgetLayout.row = Math.floor(widgetLayout.row);
    widgetLayout.col = Math.floor(widgetLayout.col);

    layout.widgets[widget.id] = widgetLayout;
  }

  private widgetPossiblePosition(widgetLayout: WidgetLayout, layout: DashboardLayout) {
    let bestRow = 0;
    let bestCol = 0;

    let maxCol = layout.gridSettings.minColumns || layout.gridSettings.columns || 0;
    let maxRow = 0;

    const widgetLayouts = Object.values(layout.widgets);

    widgetLayouts.forEach(widget => {
      maxCol = Math.max(maxCol, widget.col + widget.sizeX);
      maxRow = Math.max(maxRow, widget.row + widget.sizeY);
    })

    for (; bestRow < maxRow; bestRow++) {
      for (bestCol = 0; bestCol < maxCol; bestCol++) {
        if (!this.hasWidgetCollision(bestRow, bestCol, widgetLayout.sizeX, widgetLayout.sizeY, widgetLayouts)) {
          widgetLayout.row = bestRow;
          widgetLayout.col = bestCol;
          return;
        }
      }
    }
    const canAddToRows = maxGridsterRow >= maxRow + bestRow;
    const canAddToColumns = maxGridsterCol >= maxCol + bestCol;
    const addToRows = bestRow <= bestCol && canAddToRows;
    if (!addToRows && canAddToColumns) {
      widgetLayout.col = maxCol;
      widgetLayout.row = 0;
    } else if (canAddToRows) {
      widgetLayout.row = maxRow;
      widgetLayout.col = 0;
    }
  }

  private hasWidgetCollision(row: number, col: number, sizeX: number, sizeY: number, widgetLayouts: WidgetLayout[]) {
    const left = col;
    const right = col + sizeX;
    const top = row;
    const bottom = row + sizeY;

    for (const widget of widgetLayouts) {
      const left2 = widget.col;
      const right2 = widget.col + widget.sizeX;
      const top2 = widget.row;
      const bottom2 = widget.row + widget.sizeY;

      if (left < right2 && right > left2 && top < bottom2 && bottom > top2) {
        return true;
      }
    }
    return false;
  }

  public removeWidgetFromLayout(dashboard: Dashboard,
                                targetState: string,
                                targetLayout: DashboardLayoutId,
                                widgetId: string,
                                breakpoint: BreakpointId) {
    const layout = this.getDashboardLayoutConfig(dashboard.configuration.states[targetState].layouts[targetLayout], breakpoint);
    delete layout.widgets[widgetId];
    this.removeUnusedWidgets(dashboard);
  }

  public isSingleLayoutDashboard(dashboard: Dashboard): {state: string; layout: DashboardLayoutId} {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const stateKeys = Object.keys(states);
    if (stateKeys.length === 1) {
      const state = states[stateKeys[0]];
      const layouts = state.layouts;
      const layoutKeys = Object.keys(layouts);
      if (layoutKeys.length === 1) {
        return {
          state: stateKeys[0],
          layout: layoutKeys[0] as DashboardLayoutId
        };
      }
    }
    return null;
  }

  public updateLayoutSettings(layout: DashboardLayout, gridSettings: GridSettings) {
    const prevGridSettings = layout.gridSettings;
    let prevColumns = prevGridSettings ? prevGridSettings.columns : 24;
    if (!prevColumns) {
      prevColumns = 24;
    }
    const columns = gridSettings.columns || 24;
    const ratio = columns / prevColumns;
    layout.gridSettings = gridSettings;
    for (const w of Object.keys(layout.widgets)) {
      const widget = layout.widgets[w];
      if (!widget.sizeX) {
        widget.sizeX = 1;
      }
      if (!widget.sizeY) {
        widget.sizeY = 1;
      }
    }
    const widgets: WidgetLayout[] = [];
    for (const w of Object.keys(layout.widgets)) {
      const widget = layout.widgets[w];
      widget.row = Math.round(widget.row * ratio);
      widget.col = Math.round(widget.col * ratio);
      widget.sizeX = Math.max(1, Math.round(widget.sizeX * ratio));
      widget.sizeY = Math.max(1, Math.round(widget.sizeY * ratio));
      widgets.push(widget);
    }
    widgets.sort((w1, w2) => {
      let res = w1.col - w2.col;
      if (res === 0) {
        res = w1.row - w2.row;
      }
      return res;
    });
    for (const widget of widgets) {
      for (const widget2 of widgets) {
        if (widget !== widget2) {
          const left = widget.col;
          const right = widget.col + widget.sizeX;
          const top = widget.row;
          const bottom = widget.row + widget.sizeY;
          const left2 = widget2.col;
          const right2 = widget2.col + widget2.sizeX;
          const top2 = widget2.row;
          const bottom2 = widget2.row + widget2.sizeY;
          if (left < right2 && right > left2 &&
            top < bottom2 && bottom > top2 ) {
            let horizontalOverlapFixed = false;
            if (right - left2 === 1) {
              if (widget.sizeX > 1) {
                widget.sizeX--;
                horizontalOverlapFixed = true;
              } else if (widget2.sizeX > 1) {
                widget2.col++;
                widget2.sizeX--;
                horizontalOverlapFixed = true;
              }
            }
            if (!horizontalOverlapFixed && (bottom - top2) === 1) {
              if (widget.sizeY > 1) {
                widget.sizeY--;
              } else if (widget2.sizeY > 1) {
                widget2.row++;
                widget2.sizeY--;
              }
            }
          }
        }
      }
    }
  }

  public moveWidgets(layout: DashboardLayout, cols: number, rows: number) {
    cols = isDefinedAndNotNull(cols) ? Math.round(cols) : 0;
    rows = isDefinedAndNotNull(rows) ? Math.round(rows) : 0;
    if (cols < 0 || rows < 0) {
      let widgetMinCol = Infinity;
      let widgetMinRow = Infinity;
      for (const w of Object.keys(layout.widgets)) {
        const widget = layout.widgets[w];
        widgetMinCol = Math.min(widgetMinCol, widget.col);
        widgetMinRow = Math.min(widgetMinRow, widget.row);
      }
      if ((cols + widgetMinCol) < 0 ){
        cols = -widgetMinCol;
      }
      if ((rows + widgetMinRow) < 0 ){
        rows = -widgetMinRow;
      }
    }
    for (const w of Object.keys(layout.widgets)) {
      const widget = layout.widgets[w];
      widget.col += cols;
      widget.row += rows;
    }
  }

  public removeUnusedWidgets(dashboard: Dashboard) {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const widgets = dashboardConfiguration.widgets;
    for (const widgetId of Object.keys(widgets)) {
      let found = false;
      for (const s of Object.keys(states)) {
        const state = states[s];
        for (const l of Object.keys(state.layouts)) {
          const layout: DashboardLayout = state.layouts[l];
          if (layout.widgets[widgetId]) {
            found = true;
            break;
          }
          if (layout.breakpoints) {
            for (const breakpoint of Object.keys(layout.breakpoints)) {
              if (layout.breakpoints[breakpoint].widgets[widgetId]) {
                found = true;
                break;
              }
            }
          }
        }
      }
      if (!found) {
        delete dashboardConfiguration.widgets[widgetId];
      }
    }
  }

  private validateAndUpdateEntityAliases(configuration: DashboardConfiguration,
                                         datasourcesByAliasId: {[aliasId: string]: Array<Datasource>},
                                         targetDevicesByAliasId: {[aliasId: string]: Array<TargetDevice>}): DashboardConfiguration {
    let entityAlias: EntityAlias;
    if (isUndefined(configuration.entityAliases)) {
      configuration.entityAliases = {};
      if (configuration.deviceAliases) {
        const deviceAliases = configuration.deviceAliases;
        for (const aliasId of Object.keys(deviceAliases)) {
          const deviceAlias = deviceAliases[aliasId];
          entityAlias = this.validateAndUpdateDeviceAlias(aliasId, deviceAlias, datasourcesByAliasId, targetDevicesByAliasId);
          configuration.entityAliases[entityAlias.id] = entityAlias;
        }
        delete configuration.deviceAliases;
      }
    } else {
      const entityAliases = configuration.entityAliases;
      for (const aliasId of Object.keys(entityAliases)) {
        entityAlias = entityAliases[aliasId];
        entityAlias = this.validateAndUpdateEntityAlias(aliasId, entityAlias, datasourcesByAliasId, targetDevicesByAliasId);
        if (aliasId !== entityAlias.id) {
          delete entityAliases[aliasId];
        }
        entityAliases[entityAlias.id] = entityAlias;
      }
    }
    return configuration;
  }

  private validateAndUpdateDeviceAlias(aliasId: string,
                                       deviceAlias: any,
                                       datasourcesByAliasId: {[aliasId: string]: Array<Datasource>},
                                       targetDevicesByAliasId: {[aliasId: string]: Array<TargetDevice>}): EntityAlias {
    aliasId = this.validateAliasId(aliasId, datasourcesByAliasId, targetDevicesByAliasId);
    const alias = deviceAlias.alias;
    const entityAlias: EntityAlias = {
      id: aliasId,
      alias,
      filter: {
        type: null,
        entityType: EntityType.DEVICE,
        resolveMultiple: false
      },
    };
    if (deviceAlias.deviceFilter) {
      entityAlias.filter.type =
        deviceAlias.deviceFilter.useFilter ? AliasFilterType.entityName : AliasFilterType.entityList;
      if (entityAlias.filter.type === AliasFilterType.entityList) {
        entityAlias.filter.entityList = deviceAlias.deviceFilter.deviceList;
      } else {
        entityAlias.filter.entityNameFilter = deviceAlias.deviceFilter.deviceNameFilter;
      }
    } else {
      entityAlias.filter.type = AliasFilterType.entityList;
      entityAlias.filter.entityList = [deviceAlias.deviceId];
    }
    return entityAlias;
  }

  private validateAndUpdateEntityAlias(aliasId: string, entityAlias: EntityAlias,
                                       datasourcesByAliasId: {[aliasId: string]: Array<Datasource>},
                                       targetDevicesByAliasId: {[aliasId: string]: Array<TargetDevice>}): EntityAlias {
    entityAlias.id = this.validateAliasId(aliasId, datasourcesByAliasId, targetDevicesByAliasId);
    if (!entityAlias.filter) {
      entityAlias.filter = {
        type: entityAlias.entityFilter.useFilter ? AliasFilterType.entityName : AliasFilterType.entityList,
        entityType: entityAlias.entityType,
        resolveMultiple: false
      };
      if (entityAlias.filter.type === AliasFilterType.entityList) {
        entityAlias.filter.entityList = entityAlias.entityFilter.entityList;
      } else {
        entityAlias.filter.entityNameFilter = entityAlias.entityFilter.entityNameFilter;
      }
      delete entityAlias.entityType;
      delete entityAlias.entityFilter;
    }
    entityAlias = this.validateAndUpdateEntityAliasSingleTypeFilters(entityAlias);
    return entityAlias;
  }

  private validateAndUpdateEntityAliasSingleTypeFilters(entityAlias: EntityAlias): EntityAlias {
    if (entityAlias.filter.type === AliasFilterType.deviceType) {
      if (entityAlias.filter.deviceType) {
        if (!entityAlias.filter.deviceTypes) {
          entityAlias.filter.deviceTypes = [];
        }
        entityAlias.filter.deviceTypes.push(entityAlias.filter.deviceType);
        delete entityAlias.filter.deviceType;
      }
    }
    if (entityAlias.filter.type === AliasFilterType.assetType) {
      if (entityAlias.filter.assetType) {
        if (!entityAlias.filter.assetTypes) {
          entityAlias.filter.assetTypes = [];
        }
        entityAlias.filter.assetTypes.push(entityAlias.filter.assetType);
        delete entityAlias.filter.assetType;
      }
    }
    if (entityAlias.filter.type === AliasFilterType.entityViewType) {
      if (entityAlias.filter.entityViewType) {
        if (!entityAlias.filter.entityViewTypes) {
          entityAlias.filter.entityViewTypes = [];
        }
        entityAlias.filter.entityViewTypes.push(entityAlias.filter.entityViewType);
        delete entityAlias.filter.entityViewType;
      }
    }
    if (entityAlias.filter.type === AliasFilterType.edgeType) {
      if (entityAlias.filter.edgeType) {
        if (!entityAlias.filter.edgeTypes) {
          entityAlias.filter.edgeTypes = [];
        }
        entityAlias.filter.edgeTypes.push(entityAlias.filter.edgeType);
        delete entityAlias.filter.edgeType;
      }
    }
    return entityAlias;
  }

  private validateAliasId(aliasId: string,
                          datasourcesByAliasId: {[aliasId: string]: Array<Datasource>},
                          targetDevicesByAliasId: {[aliasId: string]: Array<TargetDevice>}): string {
    if (!aliasId || !isString(aliasId) || aliasId.length !== 36) {
      const newAliasId = this.utils.guid();
      const aliasDatasources = datasourcesByAliasId[aliasId];
      if (aliasDatasources) {
        aliasDatasources.forEach(
          (datasource) => {
            datasource.entityAliasId = newAliasId;
          }
        );
      }
      const targetDevicesList = targetDevicesByAliasId[aliasId];
      if (targetDevicesList) {
        targetDevicesList.forEach(
          (targetDevice) => {
            targetDevice.entityAliasId = newAliasId;
          }
        );
      }
      return newAliasId;
    } else {
      return aliasId;
    }
  }

  replaceReferenceWithWidgetCopy(widget: Widget,
                                 dashboard: Dashboard,
                                 targetState: string,
                                 targetLayout: DashboardLayoutId,
                                 breakpointId: BreakpointId,
                                 isRemoveWidget: boolean): Widget {

    const newWidget = deepClone(widget);
    newWidget.id = this.utils.guid();

    const originalColumns = this.getOriginalColumns(dashboard, targetState, targetLayout, breakpointId);
    const originalSize = this.getOriginalSize(dashboard, targetState, targetLayout, widget, breakpointId);

    const layout = this.getDashboardLayoutConfig(dashboard.configuration.states[targetState].layouts[targetLayout], breakpointId);
    const widgetLayout = layout.widgets[widget.id];
    const targetRow = widgetLayout.row;
    const targetColumn = widgetLayout.col;

    if (isRemoveWidget) {
      this.removeWidgetFromLayout(dashboard, targetState, targetLayout, widget.id, breakpointId);
    }

    this.addWidgetToLayout(dashboard, targetState, targetLayout, newWidget, originalColumns, originalSize,
      targetRow, targetColumn, breakpointId);

    return newWidget;
  }

  getDashboardLayoutConfig(layout: DashboardLayout, breakpointId: BreakpointId): DashboardLayout {
    if (breakpointId !== 'default' && layout.breakpoints) {
      return layout.breakpoints[breakpointId];
    }
    return layout;
  }

  getOriginalColumns(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId, breakpointId: BreakpointId): number {
    let originalColumns = 24;
    let gridSettings = null;
    const state = dashboard.configuration.states[sourceState];
    const layoutCount = Object.keys(state.layouts).length;
    if (state) {
      const layout = this.getDashboardLayoutConfig(state.layouts[sourceLayout], breakpointId);
      if (layout) {
        gridSettings = layout.gridSettings;
      }
    }
    if (gridSettings && gridSettings.columns) {
      originalColumns = gridSettings.columns;
    }
    originalColumns = originalColumns * layoutCount;
    return originalColumns;
  }

  getOriginalSize(dashboard: Dashboard, sourceState: string, sourceLayout: DashboardLayoutId,
                  widget: Widget, breakpointId: BreakpointId): WidgetSize {
    const layout = this.getDashboardLayoutConfig(dashboard.configuration.states[sourceState].layouts[sourceLayout], breakpointId);
    const widgetLayout = layout.widgets[widget.id];
    return {
      sizeX: widgetLayout.sizeX,
      sizeY: widgetLayout.sizeY,
      preserveAspectRatio: widgetLayout.preserveAspectRatio,
      resizable: widgetLayout.resizable
    };
  }

  private loadSystemBreakpoints() {
    this.systemBreakpoints = {};
    const dashboardMediaBreakpointIds: BreakpointSystemId[] = ['xs', 'sm', 'md', 'lg', 'xl'];
    dashboardMediaBreakpointIds.forEach(breakpoint => {
      const value = MediaBreakpoints[breakpoint];
      const minWidth = value.match(/min-width:\s*(\d+)px/)?.[1];
      const maxWidth = value.match(/max-width:\s*(\d+)px/)?.[1];
      this.systemBreakpoints[breakpoint] = ({
        id: breakpoint,
        minWidth: minWidth ? Number(minWidth) : undefined,
        maxWidth: maxWidth ? Number(maxWidth) : undefined,
        value
      });
    });
  }

  getListBreakpoint(): BreakpointInfo[] {
    if(!this.systemBreakpoints) {
      this.loadSystemBreakpoints();
    }
    const breakpointsList = Object.values(this.systemBreakpoints);
    breakpointsList.unshift({id: 'default'});
    return breakpointsList;
  }

  getBreakpoints(): string[] {
    if(!this.systemBreakpoints) {
      this.loadSystemBreakpoints();
    }
    return Object.values(this.systemBreakpoints).map(item => item.value);
  }

  getBreakpointInfoByValue(breakpointValue: string): BreakpointInfo {
    if(!this.systemBreakpoints) {
      this.loadSystemBreakpoints();
    }
    return Object.values(this.systemBreakpoints).find(item => item.value === breakpointValue);
  }

  getBreakpointInfoById(breakpointId: BreakpointId): BreakpointInfo {
    if(!this.systemBreakpoints) {
      this.loadSystemBreakpoints();
    }
    return this.systemBreakpoints[breakpointId];
  }

  getBreakpointName(breakpointId: BreakpointId): string {
    if (breakpointIdTranslationMap.has(breakpointId)) {
      return this.translate.instant(breakpointIdTranslationMap.get(breakpointId));
    }
    return breakpointId;
  }

  getBreakpointIcon(breakpointId: BreakpointId): string {
    if (breakpointIdIconMap.has(breakpointId)) {
      return breakpointIdIconMap.get(breakpointId);
    }
    return 'desktop_windows';
  }

  getBreakpointSizeDescription(breakpointId: BreakpointId): string {
    const currentData = this.getBreakpointInfoById(breakpointId);
    const minStr = isDefined(currentData?.minWidth) ? `min ${currentData.minWidth}px` : '';
    const maxStr = isDefined(currentData?.maxWidth) ? `max ${currentData.maxWidth}px` : '';
    return minStr && maxStr ? `${minStr} - ${maxStr}` : `${minStr}${maxStr}`;
  }

  updatedLayoutForBreakpoint(layout: DashboardPageLayout, breakpointId: BreakpointId) {
    let selectBreakpointId: BreakpointId = 'default';
    if (layout.layoutCtx.layoutData[breakpointId]) {
      selectBreakpointId = breakpointId;
    }
    layout.layoutCtx.breakpoint = selectBreakpointId;
    const layoutInfo = layout.layoutCtx.layoutData[selectBreakpointId];
    if (layoutInfo.gridSettings) {
      layout.layoutCtx.gridSettings = layoutInfo.gridSettings;
    }
    layout.layoutCtx.widgets.setWidgetIds(layoutInfo.widgetIds);
    layout.layoutCtx.widgetLayouts = layoutInfo.widgetLayouts;
    if (layout.show && layout.layoutCtx.ctrl) {
      layout.layoutCtx.ctrl.reload();
    }
    layout.layoutCtx.ignoreLoading = true;
  }
}
