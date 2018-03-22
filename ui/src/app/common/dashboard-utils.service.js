/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
function DashboardUtils(types, utils, timeService) {

    var service = {
        validateAndUpdateDashboard: validateAndUpdateDashboard,
        validateAndUpdateWidget: validateAndUpdateWidget,
        getRootStateId: getRootStateId,
        createSingleWidgetDashboard: createSingleWidgetDashboard,
        createSingleEntityFilter: createSingleEntityFilter,
        getStateLayoutsData: getStateLayoutsData,
        createDefaultState: createDefaultState,
        createDefaultLayoutData: createDefaultLayoutData,
        setLayouts: setLayouts,
        updateLayoutSettings: updateLayoutSettings,
        addWidgetToLayout: addWidgetToLayout,
        removeWidgetFromLayout: removeWidgetFromLayout,
        isSingleLayoutDashboard: isSingleLayoutDashboard,
        removeUnusedWidgets: removeUnusedWidgets,
        getWidgetsArray: getWidgetsArray
    };

    return service;

    function validateAndUpdateEntityAliases(configuration, datasourcesByAliasId, targetDevicesByAliasId) {
        var aliasId, entityAlias;
        if (angular.isUndefined(configuration.entityAliases)) {
            configuration.entityAliases = {};
            if (configuration.deviceAliases) {
                var deviceAliases = configuration.deviceAliases;
                for (aliasId in deviceAliases) {
                    var deviceAlias = deviceAliases[aliasId];
                    entityAlias = validateAndUpdateDeviceAlias(aliasId, deviceAlias, datasourcesByAliasId, targetDevicesByAliasId);
                    configuration.entityAliases[entityAlias.id] = entityAlias;
                }
                delete configuration.deviceAliases;
            }
        } else {
            var entityAliases = configuration.entityAliases;
            for (aliasId in entityAliases) {
                entityAlias = entityAliases[aliasId];
                entityAlias = validateAndUpdateEntityAlias(aliasId, entityAlias, datasourcesByAliasId, targetDevicesByAliasId);
                if (aliasId != entityAlias.id) {
                    delete entityAliases[aliasId];
                }
                entityAliases[entityAlias.id] = entityAlias;
            }
        }
        return configuration;
    }

    function validateAliasId(aliasId, datasourcesByAliasId, targetDevicesByAliasId) {
        if (!aliasId || !angular.isString(aliasId) || aliasId.length != 36) {
            var newAliasId = utils.guid();
            var aliasDatasources = datasourcesByAliasId[aliasId];
            if (aliasDatasources) {
                aliasDatasources.forEach(
                      function(datasource) {
                          datasource.entityAliasId = newAliasId;
                      }
                );
            }
            var targetDeviceAliasIdsList = targetDevicesByAliasId[aliasId];
            if (targetDeviceAliasIdsList) {
                targetDeviceAliasIdsList.forEach(
                    function(targetDeviceAliasIds) {
                        targetDeviceAliasIds[0] = newAliasId;
                    }
                );
            }
            return newAliasId;
        } else {
            return aliasId;
        }
    }

    function validateAndUpdateDeviceAlias(aliasId, deviceAlias, datasourcesByAliasId, targetDevicesByAliasId) {
        aliasId = validateAliasId(aliasId, datasourcesByAliasId, targetDevicesByAliasId);
        var alias = deviceAlias.alias;
        var entityAlias = {
            id: aliasId,
            alias: alias,
            filter: {
                type: null,
                entityType: types.entityType.device,
                resolveMultiple: false
            },
        }
        if (deviceAlias.deviceFilter) {
            entityAlias.filter.type =
                deviceAlias.deviceFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value;
            if (entityAlias.filter.type == types.aliasFilterType.entityList.value) {
                entityAlias.filter.entityList = deviceAlias.deviceFilter.deviceList;
            } else {
                entityAlias.filter.entityNameFilter = deviceAlias.deviceFilter.deviceNameFilter;
            }
        } else {
            entityAlias.filter.type = types.aliasFilterType.entityList.value;
            entityAlias.filter.entityList = [deviceAlias.deviceId];
        }
        return entityAlias;
    }

    function validateAndUpdateEntityAlias(aliasId, entityAlias, datasourcesByAliasId, targetDevicesByAliasId) {
        entityAlias.id = validateAliasId(aliasId, datasourcesByAliasId, targetDevicesByAliasId);
        if (!entityAlias.filter) {
            entityAlias.filter = {
                type: entityAlias.entityFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value,
                entityType: entityAlias.entityType,
                resolveMultiple: false
            }
            if (entityAlias.filter.type == types.aliasFilterType.entityList.value) {
                entityAlias.filter.entityList = entityAlias.entityFilter.entityList;
            } else {
                entityAlias.filter.entityNameFilter = entityAlias.entityFilter.entityNameFilter;
            }
            delete entityAlias.entityType;
            delete entityAlias.entityFilter;
        }
        return entityAlias;
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
        //TODO: Temp workaround
        if (widget.isSystemType  && widget.bundleAlias == 'charts' && widget.typeAlias == 'timeseries') {
            widget.typeAlias = 'basic_timeseries';
        }
        return widget;
    }

    function createDefaultLayoutData() {
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

    function createDefaultLayouts() {
        return {
            'main': createDefaultLayoutData()
        };
    }

    function createDefaultState(name, root) {
        return {
            name: name,
            root: root,
            layouts: createDefaultLayouts()
        }
    }

    function validateAndUpdateDashboard(dashboard) {
        if (!dashboard.configuration) {
            dashboard.configuration = {};
        }
        if (angular.isUndefined(dashboard.configuration.widgets)) {
            dashboard.configuration.widgets = {};
        } else if (angular.isArray(dashboard.configuration.widgets)) {
            var widgetsMap = {};
            dashboard.configuration.widgets.forEach(function (widget) {
                if (!widget.id) {
                    widget.id = utils.guid();
                }
                widgetsMap[widget.id] = widget;
            });
            dashboard.configuration.widgets = widgetsMap;
        }
        for (var id in dashboard.configuration.widgets) {
            var widget = dashboard.configuration.widgets[id];
            dashboard.configuration.widgets[id] = validateAndUpdateWidget(widget);
        }
        if (angular.isUndefined(dashboard.configuration.states)) {
            dashboard.configuration.states = {
                'default': createDefaultState(dashboard.title, true)
            };

            var mainLayout = dashboard.configuration.states['default'].layouts['main'];
            for (id in dashboard.configuration.widgets) {
                widget = dashboard.configuration.widgets[id];
                mainLayout.widgets[id] = {
                    sizeX: widget.sizeX,
                    sizeY: widget.sizeY,
                    row: widget.row,
                    col: widget.col,
                };
                if (angular.isDefined(widget.config.mobileHeight)) {
                    mainLayout.widgets[id].mobileHeight = widget.config.mobileHeight;
                }
                if (angular.isDefined(widget.config.mobileOrder)) {
                    mainLayout.widgets[id].mobileOrder = widget.config.mobileOrder;
                }
            }
        } else {
            var states = dashboard.configuration.states;
            var rootFound = false;
            for (var stateId in states) {
                var state = states[stateId];
                if (angular.isUndefined(state.root)) {
                    state.root = false;
                } else if (state.root) {
                    rootFound = true;
                }
            }
            if (!rootFound) {
                var firstStateId = Object.keys(states)[0];
                states[firstStateId].root = true;
            }
        }

        var datasourcesByAliasId = {};
        var targetDevicesByAliasId = {};
        for (var widgetId in dashboard.configuration.widgets) {
            widget = dashboard.configuration.widgets[widgetId];
            widget.config.datasources.forEach(function (datasource) {
               if (datasource.entityAliasId) {
                   var aliasId = datasource.entityAliasId;
                   var aliasDatasources = datasourcesByAliasId[aliasId];
                   if (!aliasDatasources) {
                       aliasDatasources = [];
                       datasourcesByAliasId[aliasId] = aliasDatasources;
                   }
                   aliasDatasources.push(datasource);
               }
            });
            if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length) {
                var aliasId = widget.config.targetDeviceAliasIds[0];
                var targetDeviceAliasIdsList = targetDevicesByAliasId[aliasId];
                if (!targetDeviceAliasIdsList) {
                    targetDeviceAliasIdsList = [];
                    targetDevicesByAliasId[aliasId] = targetDeviceAliasIdsList;
                }
                targetDeviceAliasIdsList.push(widget.config.targetDeviceAliasIds);
            }
        }

        dashboard.configuration = validateAndUpdateEntityAliases(dashboard.configuration, datasourcesByAliasId, targetDevicesByAliasId);

        if (angular.isUndefined(dashboard.configuration.timewindow)) {
            dashboard.configuration.timewindow = timeService.defaultTimewindow();
        }
        if (angular.isUndefined(dashboard.configuration.settings)) {
            dashboard.configuration.settings = {};
            dashboard.configuration.settings.stateControllerId = 'entity';
            dashboard.configuration.settings.showTitle = false;
            dashboard.configuration.settings.showDashboardsSelect = true;
            dashboard.configuration.settings.showEntitiesSelect = true;
            dashboard.configuration.settings.showDashboardTimewindow = true;
            dashboard.configuration.settings.showDashboardExport = true;
            dashboard.configuration.settings.toolbarAlwaysOpen = true;
        } else {
            if (angular.isUndefined(dashboard.configuration.settings.stateControllerId)) {
                dashboard.configuration.settings.stateControllerId = 'entity';
            }
        }
        if (angular.isDefined(dashboard.configuration.gridSettings)) {
            var gridSettings = dashboard.configuration.gridSettings;
            if (angular.isDefined(gridSettings.showTitle)) {
                dashboard.configuration.settings.showTitle = gridSettings.showTitle;
                delete gridSettings.showTitle;
            }
            if (angular.isDefined(gridSettings.titleColor)) {
                dashboard.configuration.settings.titleColor = gridSettings.titleColor;
                delete gridSettings.titleColor;
            }
            if (angular.isDefined(gridSettings.showDevicesSelect)) {
                dashboard.configuration.settings.showEntitiesSelect = gridSettings.showDevicesSelect;
                delete gridSettings.showDevicesSelect;
            }
            if (angular.isDefined(gridSettings.showEntitiesSelect)) {
                dashboard.configuration.settings.showEntitiesSelect = gridSettings.showEntitiesSelect;
                delete gridSettings.showEntitiesSelect;
            }
            if (angular.isDefined(gridSettings.showDashboardTimewindow)) {
                dashboard.configuration.settings.showDashboardTimewindow = gridSettings.showDashboardTimewindow;
                delete gridSettings.showDashboardTimewindow;
            }
            if (angular.isDefined(gridSettings.showDashboardExport)) {
                dashboard.configuration.settings.showDashboardExport = gridSettings.showDashboardExport;
                delete gridSettings.showDashboardExport;
            }
            dashboard.configuration.states['default'].layouts['main'].gridSettings = gridSettings;
            delete dashboard.configuration.gridSettings;
        }
        return dashboard;
    }

    function getRootStateId(states) {
        for (var stateId in states) {
            var state = states[stateId];
            if (state.root) {
                return stateId;
            }
        }
        return Object.keys(states)[0];
    }

    function createSingleWidgetDashboard(widget) {
        if (!widget.id) {
            widget.id = utils.guid();
        }
        var dashboard = {};
        dashboard = validateAndUpdateDashboard(dashboard);
        dashboard.configuration.widgets[widget.id] = widget;
        dashboard.configuration.states['default'].layouts['main'].widgets[widget.id] = {
            sizeX: widget.sizeX,
            sizeY: widget.sizeY,
            row: widget.row,
            col: widget.col,
        };
        return dashboard;
    }

    function createSingleEntityFilter(entityType, entityId) {
        return {
            type: types.aliasFilterType.singleEntity.value,
            singleEntity: { entityType: entityType, id: entityId },
            resolveMultiple: false
        };
    }

    function getStateLayoutsData(dashboard, targetState) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var state = states[targetState];
        if (state) {
            var allWidgets = dashboardConfiguration.widgets;
            var result = {};
            for (var l in state.layouts) {
                var layout = state.layouts[l];
                if (layout) {
                    result[l] = {
                        widgets: [],
                        widgetLayouts: {},
                        gridSettings: {}
                    }
                    for (var id in layout.widgets) {
                        result[l].widgets.push(allWidgets[id]);
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

    function setLayouts(dashboard, targetState, newLayouts) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var state = states[targetState];
        var addedCount = 0;
        var removedCount = 0;
        for (var l in state.layouts) {
            if (!newLayouts[l]) {
                removedCount++;
            }
        }
        for (l in newLayouts) {
            if (!state.layouts[l]) {
                addedCount++;
            }
        }
        state.layouts = newLayouts;
        var layoutsCount = Object.keys(state.layouts).length;
        var newColumns;
        if (addedCount) {
            for (l in state.layouts) {
                newColumns = state.layouts[l].gridSettings.columns * (layoutsCount - addedCount) / layoutsCount;
                state.layouts[l].gridSettings.columns = newColumns;
            }
        }
        if (removedCount) {
            for (l in state.layouts) {
                newColumns = state.layouts[l].gridSettings.columns * (layoutsCount + removedCount) / layoutsCount;
                state.layouts[l].gridSettings.columns = newColumns;
            }
        }
        removeUnusedWidgets(dashboard);
    }

    function updateLayoutSettings(layout, gridSettings) {
        var prevGridSettings = layout.gridSettings;
        var prevColumns = prevGridSettings ? prevGridSettings.columns : 24;
        var ratio = gridSettings.columns / prevColumns;
        layout.gridSettings = gridSettings;
        var maxRow = 0;
        for (var w in layout.widgets) {
            var widget = layout.widgets[w];
            maxRow = Math.max(maxRow, widget.row + widget.sizeY);
        }
        var newMaxRow = Math.round(maxRow * ratio);
        for (w in layout.widgets) {
            widget = layout.widgets[w];
            if (widget.row + widget.sizeY == maxRow) {
                widget.row = Math.round(widget.row * ratio);
                widget.sizeY = newMaxRow - widget.row;
            } else {
                widget.row = Math.round(widget.row * ratio);
                widget.sizeY = Math.round(widget.sizeY * ratio);
            }
            widget.sizeX = Math.round(widget.sizeX * ratio);
            widget.col = Math.round(widget.col * ratio);
            if (widget.col + widget.sizeX > gridSettings.columns) {
                widget.sizeX = gridSettings.columns - widget.col;
            }
        }
    }

    function addWidgetToLayout(dashboard, targetState, targetLayout, widget, originalColumns, originalSize, row, column) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var state = states[targetState];
        var layout = state.layouts[targetLayout];
        var layoutCount = Object.keys(state.layouts).length;
        if (!widget.id) {
            widget.id = utils.guid();
        }
        if (!dashboardConfiguration.widgets[widget.id]) {
            dashboardConfiguration.widgets[widget.id] = widget;
        }
        var widgetLayout = {
            sizeX: originalSize ? originalSize.sizeX : widget.sizeX,
            sizeY: originalSize ? originalSize.sizeY : widget.sizeY,
            mobileOrder: widget.config.mobileOrder,
            mobileHeight: widget.config.mobileHeight
        };

        if (angular.isUndefined(originalColumns)) {
            originalColumns = 24;
        }

        var gridSettings = layout.gridSettings;
        var columns = 24;
        if (gridSettings && gridSettings.columns) {
            columns = gridSettings.columns;
        }

        columns = columns * layoutCount;

        if (columns != originalColumns) {
            var ratio = columns / originalColumns;
            widgetLayout.sizeX *= ratio;
            widgetLayout.sizeY *= ratio;
        }

        if (row > -1 && column > - 1) {
            widgetLayout.row = row;
            widgetLayout.col = column;
        } else {
            row = 0;
            for (var w in layout.widgets) {
                var existingLayout = layout.widgets[w];
                var wRow = existingLayout.row ? existingLayout.row : 0;
                var wSizeY = existingLayout.sizeY ? existingLayout.sizeY : 1;
                var bottom = wRow + wSizeY;
                row = Math.max(row, bottom);
            }
            widgetLayout.row = row;
            widgetLayout.col = 0;
        }

        layout.widgets[widget.id] = widgetLayout;
    }

    function removeWidgetFromLayout(dashboard, targetState, targetLayout, widgetId) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var state = states[targetState];
        var layout = state.layouts[targetLayout];
        delete layout.widgets[widgetId];
        removeUnusedWidgets(dashboard);
    }

    function isSingleLayoutDashboard(dashboard) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var stateKeys = Object.keys(states);
        if (stateKeys.length === 1) {
            var state = states[stateKeys[0]];
            var layouts = state.layouts;
            var layoutKeys = Object.keys(layouts);
            if (layoutKeys.length === 1) {
                return {
                    state: stateKeys[0],
                    layout: layoutKeys[0]
                }
            }
        }
        return null;
    }

    function removeUnusedWidgets(dashboard) {
        var dashboardConfiguration = dashboard.configuration;
        var states = dashboardConfiguration.states;
        var widgets = dashboardConfiguration.widgets;
        for (var widgetId in widgets) {
            var found = false;
            for (var s in states) {
                var state = states[s];
                for (var l in state.layouts) {
                    var layout = state.layouts[l];
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

    function getWidgetsArray(dashboard) {
        var widgetsArray = [];
        var dashboardConfiguration = dashboard.configuration;
        var widgets = dashboardConfiguration.widgets;
        for (var widgetId in widgets) {
            var widget = widgets[widgetId];
            widgetsArray.push(widget);
        }
        return widgetsArray;
    }
}
