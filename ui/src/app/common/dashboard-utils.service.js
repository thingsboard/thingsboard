/*
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default angular.module('thingsboard.dashboardUtils', [])
    .factory('dashboardUtils', DashboardUtils)
    .name;

/*@ngInject*/
function DashboardUtils(types, timeService) {

    var service = {
        validateAndUpdateDashboard: validateAndUpdateDashboard
    };

    return service;

    function validateAndUpdateEntityAliases(configuration) {
        if (angular.isUndefined(configuration.entityAliases)) {
            configuration.entityAliases = {};
            if (configuration.deviceAliases) {
                var deviceAliases = configuration.deviceAliases;
                for (var aliasId in deviceAliases) {
                    var deviceAlias = deviceAliases[aliasId];
                    var alias = deviceAlias.alias;
                    var entityFilter = {
                        useFilter: false,
                        entityNameFilter: '',
                        entityList: []
                    }
                    if (deviceAlias.deviceFilter) {
                        entityFilter.useFilter = deviceAlias.deviceFilter.useFilter;
                        entityFilter.entityNameFilter = deviceAlias.deviceFilter.deviceNameFilter;
                        entityFilter.entityList = deviceAlias.deviceFilter.deviceList;
                    } else if (deviceAlias.deviceId) {
                        entityFilter.entityList = [deviceAlias.deviceId];
                    }
                    var entityAlias = {
                        id: aliasId,
                        alias: alias,
                        entityType: types.entityType.device,
                        entityFilter: entityFilter
                    };
                    configuration.entityAliases[aliasId] = entityAlias;
                }
                delete configuration.deviceAliases;
            }
        }
        return configuration;
    }

    function validateAndUpdateWidget(widget) {
        if (!widget.config) {
            widget.config = {};
        }
        if (!widget.config.datasources) {
            widget.config.datasources = [];
        }
        widget.config.datasources.forEach(function(datasource) {
             if (datasource.type === 'device') {
                 datasource.type = types.datasourceType.entity;
             }
             if (datasource.deviceAliasId) {
                 datasource.entityAliasId = datasource.deviceAliasId;
                 delete datasource.deviceAliasId;
             }
        });
    }

    function validateAndUpdateDashboard(dashboard) {
        if (!dashboard.configuration) {
            dashboard.configuration = {
                widgets: [],
                entityAliases: {}
            };
        }
        if (angular.isUndefined(dashboard.configuration.widgets)) {
            dashboard.configuration.widgets = [];
        }
        dashboard.configuration.widgets.forEach(function(widget) {
            validateAndUpdateWidget(widget);
        });
        if (angular.isUndefined(dashboard.configuration.timewindow)) {
            dashboard.configuration.timewindow = timeService.defaultTimewindow();
        }
        if (angular.isDefined(dashboard.configuration.gridSettings)) {
            if (angular.isDefined(dashboard.configuration.gridSettings.showDevicesSelect)) {
                dashboard.configuration.gridSettings.showEntitiesSelect = dashboard.configuration.gridSettings.showDevicesSelect;
                delete dashboard.configuration.gridSettings.showDevicesSelect;
            }
        }
        dashboard.configuration = validateAndUpdateEntityAliases(dashboard.configuration);
        return dashboard;
    }
}
