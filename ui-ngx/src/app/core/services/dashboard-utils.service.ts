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

import { Injectable } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { TimeService } from '@core/services/time.service';
import {
  Dashboard,
  DashboardLayout,
  DashboardStateLayouts,
  DashboardState,
  DashboardConfiguration
} from '@shared/models/dashboard.models';
import { isUndefined, isDefined, isString } from '@core/utils';
import { DatasourceType, Widget, Datasource } from '@app/shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { EntityAlias, AliasFilterType } from '@app/shared/models/alias.models';

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
    // TODO: Temp workaround
    if (widget.isSystemType  && widget.bundleAlias === 'charts' && widget.typeAlias === 'timeseries') {
      widget.typeAlias = 'basic_timeseries';
    }
    return widget;
  }

  public createDefaultLayoutData(): DashboardLayout {
    return {
      widgets: {},
      gridSettings: {
        backgroundColor: '#eeeeee',
        color: 'rgba(0,0,0,0.870588)',
        columns: 24,
        margins: [10, 10],
        backgroundSizeMode: '100%'
      }
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
