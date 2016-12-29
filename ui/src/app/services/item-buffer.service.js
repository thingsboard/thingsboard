/*
 * Copyright © 2016 The Thingsboard Authors
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
function ItemBuffer(bufferStore) {

    const WIDGET_ITEM = "widget_item";

    var service = {
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
                deviceId: "..."
            }
        }
        targetDeviceAliases: {
            targetDeviceAliasIndex: {
                aliasName: "...",
                deviceId: "..."
            }
        }
        ....
     }
    **/

    function copyWidget(widget, aliasesInfo, originalColumns) {
        var widgetItem = {
            widget: widget,
            aliasesInfo: aliasesInfo,
            originalColumns: originalColumns
        }
        bufferStore.set(WIDGET_ITEM, angular.toJson(widgetItem));
    }

    function hasWidget() {
        return bufferStore.get(WIDGET_ITEM);
    }

    function pasteWidget(targetDasgboard, position) {
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
            addWidgetToDashboard(targetDasgboard, widget, aliasesInfo, originalColumns, targetRow, targetColumn);
        }
    }

    function addWidgetToDashboard(dashboard, widget, aliasesInfo, originalColumns, row, column) {
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
        updateAliases(theDashboard, widget, aliasesInfo);

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
        theDashboard.configuration.widgets.push(widget);
        return theDashboard;
    }

    function updateAliases(dashboard, widget, aliasesInfo) {
        var deviceAliases = dashboard.configuration.deviceAliases;
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
    }

    function getDeviceAliasId(deviceAliases, aliasInfo) {
        var newAliasId;
        for (var aliasId in deviceAliases) {
            if (deviceAliases[aliasId].deviceId === aliasInfo.deviceId) {
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
            deviceAliases[newAliasId] = {alias: newAliasName, deviceId: aliasInfo.deviceId};
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