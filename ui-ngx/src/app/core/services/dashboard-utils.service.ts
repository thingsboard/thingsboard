///
/// Copyright © 2016-2020 The Thingsboard Authors
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
  Dashboard,
  DashboardConfiguration,
  DashboardLayout,
  DashboardLayoutId,
  DashboardLayoutInfo,
  DashboardLayoutsInfo,
  DashboardState,
  DashboardStateLayouts,
  GridSettings,
  WidgetLayout
} from '@shared/models/dashboard.models';
import { isDefined, isString, isUndefined } from '@core/utils';
import { Datasource, DatasourceType, Widget } from '@app/shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AliasFilterType, EntityAlias, EntityAliasFilter } from '@app/shared/models/alias.models';
import { EntityId } from '@app/shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class DashboardUtilsService {

  constructor(private utils: UtilsService,
              private timeService: TimeService) {
  }

  public validateAndUpdateDashboard(dashboard: Dashboard): Dashboard {
    if (!dashboard.configuration) {
      dashboard.configuration = {};
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
    const targetDevicesByAliasId: {[aliasId: string]: Array<Array<string>>} = {};
    for (const widgetId of Object.keys(dashboard.configuration.widgets)) {
      const widget = dashboard.configuration.widgets[widgetId];
      widget.config.datasources.forEach((datasource) => {
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
      if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length) {
        const aliasId = widget.config.targetDeviceAliasIds[0];
        let targetDeviceAliasIdsList = targetDevicesByAliasId[aliasId];
        if (!targetDeviceAliasIdsList) {
          targetDeviceAliasIdsList = [];
          targetDevicesByAliasId[aliasId] = targetDeviceAliasIdsList;
        }
        targetDeviceAliasIdsList.push(widget.config.targetDeviceAliasIds);
      }
    }

    dashboard.configuration = this.validateAndUpdateEntityAliases(dashboard.configuration, datasourcesByAliasId, targetDevicesByAliasId);
    if (!dashboard.configuration.filters) {
      dashboard.configuration.filters = {};
    }

    if (isUndefined(dashboard.configuration.timewindow)) {
      dashboard.configuration.timewindow = this.timeService.defaultTimewindow();
    }
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
    if (!widget.config) {
      widget.config = {};
    }
    if (!widget.config.datasources) {
      widget.config.datasources = [];
    }
    widget.config.datasources.forEach((datasource) => {
      if (datasource.type === 'device') {
        datasource.type = DatasourceType.entity;
      }
      if (datasource.deviceAliasId) {
        datasource.entityAliasId = datasource.deviceAliasId;
        delete datasource.deviceAliasId;
      }
    });
    // Temp workaround
    if (widget.isSystemType  && widget.bundleAlias === 'charts' && widget.typeAlias === 'timeseries') {
      widget.typeAlias = 'basic_timeseries';
    }
    return widget;
  }

  public createDefaultLayoutData(): DashboardLayout {
    return {
      widgets: {},
      gridSettings: this.createDefaultGridSettings()
    };
  }

  private createDefaultGridSettings(): GridSettings {
    return {
      backgroundColor: '#eeeeee',
      color: 'rgba(0,0,0,0.870588)',
      columns: 24,
      margin: 10,
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
    if (layout.gridSettings.margins && layout.gridSettings.margins.length === 2) {
      layout.gridSettings.margin = layout.gridSettings.margins[0];
      delete layout.gridSettings.margins;
    }
    layout.gridSettings.margin = isDefined(layout.gridSettings.margin) ? layout.gridSettings.margin : 10;
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
          result[l] = {
            widgetIds: [],
            widgetLayouts: {},
            gridSettings: {}
          } as DashboardLayoutInfo;
          for (const id of Object.keys(layout.widgets)) {
            result[l].widgetIds.push(id);
          }
          result[l].widgetLayouts = layout.widgets;
          result[l].gridSettings = layout.gridSettings;
        }
      }
      return result;
    } else {
      return null;
    }
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

  public addWidgetToLayout(dashboard: Dashboard,
                           targetState: string,
                           targetLayout: DashboardLayoutId,
                           widget: Widget,
                           originalColumns?: number,
                           originalSize?: {sizeX: number, sizeY: number},
                           row?: number,
                           column?: number): void {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const state = states[targetState];
    const layout = state.layouts[targetLayout];
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
      mobileHeight: widget.config.mobileHeight
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
    layout.widgets[widget.id] = widgetLayout;
  }

  public removeWidgetFromLayout(dashboard: Dashboard,
                                targetState: string,
                                targetLayout: DashboardLayoutId,
                                widgetId: string) {
    const dashboardConfiguration = dashboard.configuration;
    const states = dashboardConfiguration.states;
    const state = states[targetState];
    const layout = state.layouts[targetLayout];
    delete layout.widgets[widgetId];
    this.removeUnusedWidgets(dashboard);
  }

  public isSingleLayoutDashboard(dashboard: Dashboard): {state: string, layout: DashboardLayoutId} {
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
    let maxRow = 0;
    for (const w of Object.keys(layout.widgets)) {
      const widget = layout.widgets[w];
      if (!widget.sizeX) {
        widget.sizeX = 1;
      }
      if (!widget.sizeY) {
        widget.sizeY = 1;
      }
      maxRow = Math.max(maxRow, widget.row + widget.sizeY);
    }
    const newMaxRow = Math.round(maxRow * ratio);
    for (const w of Object.keys(layout.widgets)) {
      const widget = layout.widgets[w];
      if (widget.row + widget.sizeY === maxRow) {
        widget.row = Math.round(widget.row * ratio);
        widget.sizeY = newMaxRow - widget.row;
      } else {
        widget.row = Math.round(widget.row * ratio);
        widget.sizeY = Math.round(widget.sizeY * ratio);
      }
      widget.sizeX = Math.round(widget.sizeX * ratio);
      widget.col = Math.round(widget.col * ratio);
      if (widget.col + widget.sizeX > columns) {
        widget.sizeX = columns - widget.col;
      }
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
          const layout = state.layouts[l];
          if (layout.widgets[widgetId]) {
            found = true;
            break;
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
                                         targetDevicesByAliasId: {[aliasId: string]: Array<Array<string>>}): DashboardConfiguration {
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
                                       targetDevicesByAliasId: {[aliasId: string]: Array<Array<string>>}): EntityAlias {
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
                                       targetDevicesByAliasId: {[aliasId: string]: Array<Array<string>>}): EntityAlias {
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
    return entityAlias;
  }

  private validateAliasId(aliasId: string,
                          datasourcesByAliasId: {[aliasId: string]: Array<Datasource>},
                          targetDevicesByAliasId: {[aliasId: string]: Array<Array<string>>}): string {
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
      const targetDeviceAliasIdsList = targetDevicesByAliasId[aliasId];
      if (targetDeviceAliasIdsList) {
        targetDeviceAliasIdsList.forEach(
          (targetDeviceAliasIds) => {
            targetDeviceAliasIds[0] = newAliasId;
          }
        );
      }
      return newAliasId;
    } else {
      return aliasId;
    }
  }

}
