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
import angularStorage from 'angular-storage';

export default angular.module('thingsboard.itembuffer', [angularStorage])
    .factory('itembuffer', ItemBuffer)
    .factory('bufferStore', function(store) {
        var newStore = store.getNamespacedStore('tbBufferStore', null, null, false);
        return newStore;
    })
    .name;

/*@ngInject*/
function ItemBuffer(bufferStore, types) {

    const WIDGET_ITEM = "widget_item";

    var service = {
        prepareWidgetItem: prepareWidgetItem,
        copyWidget: copyWidget,
        hasWidget: hasWidget,
        pasteWidget: pasteWidget,
        addWidgetToDashboard: addWidgetToDashboard
    }

    return service;

    /**
     aliasesInfo {
        datasourceAliases: {
            datasourceIndex: {
                aliasName: "...",
                deviceFilter: "..."
            }
        }
        targetDeviceAliases: {
            targetDeviceAliasIndex: {
                aliasName: "...",
                deviceFilter: "..."
            }
        }
        ....
     }
    **/

    function getDeviceFilter(alias) {
        if (alias.deviceId) {
            return {
                useFilter: false,
                deviceNameFilter: '',
                deviceList: [alias.deviceId]
            };
        } else {
            return alias.deviceFilter;
        }
    }

    function prepareAliasInfo(deviceAlias) {
        return {
            aliasName: deviceAlias.alias,
            deviceFilter: getDeviceFilter(deviceAlias)
        };
    }

    function prepareWidgetItem(dashboard, widget) {
        var aliasesInfo = {
            datasourceAliases: {},
            targetDeviceAliases: {}
        };
        var originalColumns = 24;
        if (dashboard.configuration.gridSettings &&
            dashboard.configuration.gridSettings.columns) {
            originalColumns = dashboard.configuration.gridSettings.columns;
        }
        if (widget.config && dashboard.configuration
            && dashboard.configuration.deviceAliases) {
            var deviceAlias;
            if (widget.config.datasources) {
                for (var i=0;i<widget.config.datasources.length;i++) {
                    var datasource = widget.config.datasources[i];
                    if (datasource.type === types.datasourceType.device && datasource.deviceAliasId) {
                        deviceAlias = dashboard.configuration.deviceAliases[datasource.deviceAliasId];
                        if (deviceAlias) {
                            aliasesInfo.datasourceAliases[i] = prepareAliasInfo(deviceAlias);
                        }
                    }
                }
            }
            if (widget.config.targetDeviceAliasIds) {
                for (i=0;i<widget.config.targetDeviceAliasIds.length;i++) {
                    var targetDeviceAliasId = widget.config.targetDeviceAliasIds[i];
                    if (targetDeviceAliasId) {
                        deviceAlias = dashboard.configuration.deviceAliases[targetDeviceAliasId];
                        if (deviceAlias) {
                            aliasesInfo.targetDeviceAliases[i] = prepareAliasInfo(deviceAlias);
                        }
                    }
                }
            }
        }
        return {
            widget: widget,
            aliasesInfo: aliasesInfo,
            originalColumns: originalColumns
        }
    }

    function copyWidget(dashboard, widget) {
        var widgetItem = prepareWidgetItem(dashboard, widget);
        bufferStore.set(WIDGET_ITEM, angular.toJson(widgetItem));
    }

    function hasWidget() {
        return bufferStore.get(WIDGET_ITEM);
    }

    function pasteWidget(targetDashboard, position, onAliasesUpdate) {
        var widgetItemJson = bufferStore.get(WIDGET_ITEM);
        if (widgetItemJson) {
            var widgetItem = angular.fromJson(widgetItemJson);
            var widget = widgetItem.widget;
            var aliasesInfo = widgetItem.aliasesInfo;
            var originalColumns = widgetItem.originalColumns;
            var targetRow = -1;
            var targetColumn = -1;
            if (position) {
                targetRow = position.row;
                targetColumn = position.column;
            }
            addWidgetToDashboard(targetDashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns, targetRow, targetColumn);
        }
    }

    function addWidgetToDashboard(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns, row, column) {
        var theDashboard;
        if (dashboard) {
            theDashboard = dashboard;
        } else {
            theDashboard = {};
        }
        if (!theDashboard.configuration) {
            theDashboard.configuration = {};
        }
        if (!theDashboard.configuration.deviceAliases) {
            theDashboard.configuration.deviceAliases = {};
        }
        var newDeviceAliases = updateAliases(theDashboard, widget, aliasesInfo);

        if (!theDashboard.configuration.widgets) {
            theDashboard.configuration.widgets = [];
        }
        var targetColumns = 24;
        if (theDashboard.configuration.gridSettings &&
            theDashboard.configuration.gridSettings.columns) {
            targetColumns = theDashboard.configuration.gridSettings.columns;
        }
        if (targetColumns != originalColumns) {
            var ratio = targetColumns / originalColumns;
            widget.sizeX *= ratio;
            widget.sizeY *= ratio;
        }
        if (row > -1 && column > - 1) {
            widget.row = row;
            widget.col = column;
        } else {
            row = 0;
            for (var w in theDashboard.configuration.widgets) {
                var existingWidget = theDashboard.configuration.widgets[w];
                var wRow = existingWidget.row ? existingWidget.row : 0;
                var wSizeY = existingWidget.sizeY ? existingWidget.sizeY : 1;
                var bottom = wRow + wSizeY;
                row = Math.max(row, bottom);
            }
            widget.row = row;
            widget.col = 0;
        }
        var aliasesUpdated = !angular.equals(newDeviceAliases, theDashboard.configuration.deviceAliases);
        if (aliasesUpdated) {
            theDashboard.configuration.deviceAliases = newDeviceAliases;
            if (onAliasesUpdate) {
                onAliasesUpdate();
            }
        }
        theDashboard.configuration.widgets.push(widget);
        return theDashboard;
    }

    function updateAliases(dashboard, widget, aliasesInfo) {
        var deviceAliases = angular.copy(dashboard.configuration.deviceAliases);
        var aliasInfo;
        var newAliasId;
        for (var datasourceIndex in aliasesInfo.datasourceAliases) {
            aliasInfo = aliasesInfo.datasourceAliases[datasourceIndex];
            newAliasId = getDeviceAliasId(deviceAliases, aliasInfo);
            widget.config.datasources[datasourceIndex].deviceAliasId = newAliasId;
        }
        for (var targetDeviceAliasIndex in aliasesInfo.targetDeviceAliases) {
            aliasInfo = aliasesInfo.targetDeviceAliases[targetDeviceAliasIndex];
            newAliasId = getDeviceAliasId(deviceAliases, aliasInfo);
            widget.config.targetDeviceAliasIds[targetDeviceAliasIndex] = newAliasId;
        }
        return deviceAliases;
    }

    function isDeviceFiltersEqual(alias1, alias2) {
        var filter1 = getDeviceFilter(alias1);
        var filter2 = getDeviceFilter(alias2);
        return angular.equals(filter1, filter2);
    }

    function getDeviceAliasId(deviceAliases, aliasInfo) {
        var newAliasId;
        for (var aliasId in deviceAliases) {
            if (isDeviceFiltersEqual(deviceAliases[aliasId], aliasInfo)) {
                newAliasId = aliasId;
                break;
            }
        }
        if (!newAliasId) {
            var newAliasName = createDeviceAliasName(deviceAliases, aliasInfo.aliasName);
            newAliasId = 0;
            for (aliasId in deviceAliases) {
                newAliasId = Math.max(newAliasId, aliasId);
            }
            newAliasId++;
            deviceAliases[newAliasId] = {alias: newAliasName, deviceFilter: aliasInfo.deviceFilter};
        }
        return newAliasId;
    }

    function createDeviceAliasName(deviceAliases, alias) {
        var c = 0;
        var newAlias = angular.copy(alias);
        var unique = false;
        while (!unique) {
            unique = true;
            for (var devAliasId in deviceAliases) {
                var devAlias = deviceAliases[devAliasId];
                if (newAlias === devAlias.alias) {
                    c++;
                    newAlias = alias + c;
                    unique = false;
                }
            }
        }
        return newAlias;
    }


}