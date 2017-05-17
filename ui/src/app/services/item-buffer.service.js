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
function ItemBuffer(bufferStore, types, dashboardUtils) {

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
                entityType: "...",
                entityFilter: "..."
            }
        }
        targetDeviceAliases: {
            targetDeviceAliasIndex: {
                aliasName: "...",
                entityType: "...",
                entityFilter: "..."
            }
        }
        ....
     }
    **/

    function prepareAliasInfo(entityAlias) {
        return {
            aliasName: entityAlias.alias,
            entityType: entityAlias.entityType,
            entityFilter: entityAlias.entityFilter
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
            && dashboard.configuration.entityAliases) {
            var entityAlias;
            if (widget.config.datasources) {
                for (var i=0;i<widget.config.datasources.length;i++) {
                    var datasource = widget.config.datasources[i];
                    if (datasource.type === types.datasourceType.entity && datasource.entityAliasId) {
                        entityAlias = dashboard.configuration.entityAliases[datasource.entityAliasId];
                        if (entityAlias) {
                            aliasesInfo.datasourceAliases[i] = prepareAliasInfo(entityAlias);
                        }
                    }
                }
            }
            if (widget.config.targetDeviceAliasIds) {
                for (i=0;i<widget.config.targetDeviceAliasIds.length;i++) {
                    var targetDeviceAliasId = widget.config.targetDeviceAliasIds[i];
                    if (targetDeviceAliasId) {
                        entityAlias = dashboard.configuration.entityAliases[targetDeviceAliasId];
                        if (entityAlias) {
                            aliasesInfo.targetDeviceAliases[i] = prepareAliasInfo(entityAlias);
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

        theDashboard = dashboardUtils.validateAndUpdateDashboard(theDashboard);

        var newEntityAliases = updateAliases(theDashboard, widget, aliasesInfo);

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
        var aliasesUpdated = !angular.equals(newEntityAliases, theDashboard.configuration.entityAliases);
        if (aliasesUpdated) {
            theDashboard.configuration.entityAliases = newEntityAliases;
            if (onAliasesUpdate) {
                onAliasesUpdate();
            }
        }
        theDashboard.configuration.widgets.push(widget);
        return theDashboard;
    }

    function updateAliases(dashboard, widget, aliasesInfo) {
        var entityAliases = angular.copy(dashboard.configuration.entityAliases);
        var aliasInfo;
        var newAliasId;
        for (var datasourceIndex in aliasesInfo.datasourceAliases) {
            aliasInfo = aliasesInfo.datasourceAliases[datasourceIndex];
            newAliasId = getEntityAliasId(entityAliases, aliasInfo);
            widget.config.datasources[datasourceIndex].entityAliasId = newAliasId;
        }
        for (var targetDeviceAliasIndex in aliasesInfo.targetDeviceAliases) {
            aliasInfo = aliasesInfo.targetDeviceAliases[targetDeviceAliasIndex];
            newAliasId = getEntityAliasId(entityAliases, aliasInfo);
            widget.config.targetDeviceAliasIds[targetDeviceAliasIndex] = newAliasId;
        }
        return entityAliases;
    }

    function isEntityAliasEqual(alias1, alias2) {
        return alias1.entityType === alias2.entityType &&
            angular.equals(alias1.entityFilter, alias2.entityFilter);
    }

    function getEntityAliasId(entityAliases, aliasInfo) {
        var newAliasId;
        for (var aliasId in entityAliases) {
            if (isEntityAliasEqual(entityAliases[aliasId], aliasInfo)) {
                newAliasId = aliasId;
                break;
            }
        }
        if (!newAliasId) {
            var newAliasName = createEntityAliasName(entityAliases, aliasInfo.aliasName);
            newAliasId = 0;
            for (aliasId in entityAliases) {
                newAliasId = Math.max(newAliasId, aliasId);
            }
            newAliasId++;
            entityAliases[newAliasId] = {alias: newAliasName, entityType: aliasInfo.entityType, entityFilter: aliasInfo.entityFilter};
        }
        return newAliasId;
    }

    function createEntityAliasName(entityAliases, alias) {
        var c = 0;
        var newAlias = angular.copy(alias);
        var unique = false;
        while (!unique) {
            unique = true;
            for (var entAliasId in entityAliases) {
                var entAlias = entityAliases[entAliasId];
                if (newAlias === entAlias.alias) {
                    c++;
                    newAlias = alias + c;
                    unique = false;
                }
            }
        }
        return newAlias;
    }


}