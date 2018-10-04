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
import angularStorage from 'angular-storage';

export default angular.module('thingsboard.itembuffer', [angularStorage])
    .factory('itembuffer', ItemBuffer)
    .factory('bufferStore', function(store) {
        var newStore = store.getNamespacedStore('tbBufferStore', null, null, false);
        return newStore;
    })
    .name;

/*@ngInject*/
function ItemBuffer($q, bufferStore, types, utils, dashboardUtils, ruleChainService) {

    const WIDGET_ITEM = "widget_item";
    const WIDGET_REFERENCE = "widget_reference";
    const RULE_NODES = "rule_nodes";

    var service = {
        prepareWidgetItem: prepareWidgetItem,
        copyWidget: copyWidget,
        copyWidgetReference: copyWidgetReference,
        hasWidget: hasWidget,
        canPasteWidgetReference: canPasteWidgetReference,
        pasteWidget: pasteWidget,
        pasteWidgetReference: pasteWidgetReference,
        addWidgetToDashboard: addWidgetToDashboard,
        copyRuleNodes: copyRuleNodes,
        hasRuleNodes: hasRuleNodes,
        pasteRuleNodes: pasteRuleNodes
    }

    return service;

    /**
     aliasesInfo {
        datasourceAliases: {
            datasourceIndex: {
                alias: "...",
                filter: "..."
            }
        }
        targetDeviceAliases: {
            targetDeviceAliasIndex: {
                alias: "...",
                filter: "..."
            }
        }
        ....
     }
    **/

    function prepareAliasInfo(entityAlias) {
        return {
            alias: entityAlias.alias,
            filter: entityAlias.filter
        };
    }

    function getOriginalColumns(dashboard, sourceState, sourceLayout) {
        var originalColumns = 24;
        var gridSettings = null;
        var state = dashboard.configuration.states[sourceState];
        var layoutCount = Object.keys(state.layouts).length;
        if (state) {
            var layout = state.layouts[sourceLayout];
            if (layout) {
                gridSettings = layout.gridSettings;

            }
        }
        if (gridSettings &&
            gridSettings.columns) {
            originalColumns = gridSettings.columns;
        }
        originalColumns = originalColumns * layoutCount;
        return originalColumns;
    }

    function getOriginalSize(dashboard, sourceState, sourceLayout, widget) {
        var layout = dashboard.configuration.states[sourceState].layouts[sourceLayout];
        var widgetLayout = layout.widgets[widget.id];
        return {
            sizeX: widgetLayout.sizeX,
            sizeY: widgetLayout.sizeY
        }
    }

    function prepareWidgetItem(dashboard, sourceState, sourceLayout, widget) {
        var aliasesInfo = {
            datasourceAliases: {},
            targetDeviceAliases: {}
        };
        var originalColumns = getOriginalColumns(dashboard, sourceState, sourceLayout);
        var originalSize = getOriginalSize(dashboard, sourceState, sourceLayout, widget);
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
            originalSize: originalSize,
            originalColumns: originalColumns
        };
    }

    function prepareWidgetReference(dashboard, sourceState, sourceLayout, widget) {
        var originalColumns = getOriginalColumns(dashboard, sourceState, sourceLayout);
        var originalSize = getOriginalSize(dashboard, sourceState, sourceLayout, widget);

        return {
            dashboardId: dashboard.id.id,
            sourceState: sourceState,
            sourceLayout: sourceLayout,
            widgetId: widget.id,
            originalSize: originalSize,
            originalColumns: originalColumns
        };
    }

    function copyRuleNodes(nodes, connections) {
        var ruleNodes = {
            nodes: [],
            connections: []
        };
        var top = -1, left = -1, bottom = -1, right = -1;
        for (var i=0;i<nodes.length;i++) {
            var origNode = nodes[i];
            var node = {
                additionalInfo: origNode.additionalInfo,
                configuration: origNode.configuration,
                debugMode: origNode.debugMode,
                x: origNode.x,
                y: origNode.y,
                name: origNode.name,
                componentClazz: origNode.component.clazz,
            };
            if (origNode.targetRuleChainId) {
                node.targetRuleChainId = origNode.targetRuleChainId;
            }
            if (origNode.error) {
                node.error = origNode.error;
            }
            ruleNodes.nodes.push(node);
            if (i==0) {
                top = node.y;
                left = node.x;
                bottom = node.y + 50;
                right = node.x + 170;
            } else {
                top = Math.min(top, node.y);
                left = Math.min(left, node.x);
                bottom = Math.max(bottom, node.y + 50);
                right = Math.max(right, node.x + 170);
            }
        }
        ruleNodes.originX = left + (right-left)/2;
        ruleNodes.originY = top + (bottom-top)/2;
        for (i=0;i<connections.length;i++) {
            var connection = connections[i];
            ruleNodes.connections.push(connection);
        }
        bufferStore.set(RULE_NODES, angular.toJson(ruleNodes));
    }

    function hasRuleNodes() {
        return bufferStore.get(RULE_NODES);
    }

    function pasteRuleNodes(x, y) {
        var ruleNodesJson = bufferStore.get(RULE_NODES);
        if (ruleNodesJson) {
            var ruleNodes = angular.fromJson(ruleNodesJson);
            var deltaX = x - ruleNodes.originX;
            var deltaY = y - ruleNodes.originY;
            for (var i=0;i<ruleNodes.nodes.length;i++) {
                var node = ruleNodes.nodes[i];
                var component = ruleChainService.getRuleNodeComponentByClazz(node.componentClazz);
                if (component) {
                    var icon = types.ruleNodeType[component.type].icon;
                    var iconUrl = null;
                    if (component.configurationDescriptor.nodeDefinition.icon) {
                        icon = component.configurationDescriptor.nodeDefinition.icon;
                    }
                    if (component.configurationDescriptor.nodeDefinition.iconUrl) {
                        iconUrl = component.configurationDescriptor.nodeDefinition.iconUrl;
                    }
                    delete node.componentClazz;
                    node.component = component;
                    node.nodeClass = types.ruleNodeType[component.type].nodeClass;
                    node.icon = icon;
                    node.iconUrl = iconUrl;
                    node.connectors = [];
                    node.x = Math.round(node.x + deltaX);
                    node.y = Math.round(node.y + deltaY);
                } else {
                    return null;
                }
            }
            return ruleNodes;
        }
        return null;
    }

    function copyWidget(dashboard, sourceState, sourceLayout, widget) {
        var widgetItem = prepareWidgetItem(dashboard, sourceState, sourceLayout, widget);
        bufferStore.set(WIDGET_ITEM, angular.toJson(widgetItem));
    }

    function copyWidgetReference(dashboard, sourceState, sourceLayout, widget) {
        var widgetReference = prepareWidgetReference(dashboard, sourceState, sourceLayout, widget);
        bufferStore.set(WIDGET_REFERENCE, angular.toJson(widgetReference));
    }

    function hasWidget() {
        return bufferStore.get(WIDGET_ITEM);
    }

    function canPasteWidgetReference(dashboard, state, layout) {
        var widgetReferenceJson = bufferStore.get(WIDGET_REFERENCE);
        if (widgetReferenceJson) {
            var widgetReference = angular.fromJson(widgetReferenceJson);
            if (widgetReference.dashboardId === dashboard.id.id) {
                if ((widgetReference.sourceState != state || widgetReference.sourceLayout != layout)
                    && dashboard.configuration.widgets[widgetReference.widgetId]) {
                    return true;
                }
            }
        }
        return false;
    }

    function pasteWidgetReference(targetDashboard, targetState, targetLayout, position) {
        var deferred = $q.defer();
        var widgetReferenceJson = bufferStore.get(WIDGET_REFERENCE);
        if (widgetReferenceJson) {
            var widgetReference = angular.fromJson(widgetReferenceJson);
            var widget = targetDashboard.configuration.widgets[widgetReference.widgetId];
            if (widget) {
                var originalColumns = widgetReference.originalColumns;
                var originalSize = widgetReference.originalSize;
                var targetRow = -1;
                var targetColumn = -1;
                if (position) {
                    targetRow = position.row;
                    targetColumn = position.column;
                }
                addWidgetToDashboard(targetDashboard, targetState, targetLayout, widget, null,
                    null, originalColumns, originalSize, targetRow, targetColumn).then(
                    function () {
                        deferred.resolve(widget);
                    }
                );
            } else {
                deferred.reject();
            }
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function pasteWidget(targetDashboard, targetState, targetLayout, position, onAliasesUpdateFunction) {
        var deferred = $q.defer();
        var widgetItemJson = bufferStore.get(WIDGET_ITEM);
        if (widgetItemJson) {
            var widgetItem = angular.fromJson(widgetItemJson);
            var widget = widgetItem.widget;
            var aliasesInfo = widgetItem.aliasesInfo;
            var originalColumns = widgetItem.originalColumns;
            var originalSize = widgetItem.originalSize;
            var targetRow = -1;
            var targetColumn = -1;
            if (position) {
                targetRow = position.row;
                targetColumn = position.column;
            }
            widget.id = utils.guid();
            addWidgetToDashboard(targetDashboard, targetState, targetLayout, widget, aliasesInfo,
                onAliasesUpdateFunction, originalColumns, originalSize, targetRow, targetColumn).then(
                    function () {
                        deferred.resolve(widget);
                    }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function addWidgetToDashboard(dashboard, targetState, targetLayout, widget, aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, row, column) {
        var deferred = $q.defer();
        var theDashboard;
        if (dashboard) {
            theDashboard = dashboard;
        } else {
            theDashboard = {};
        }

        theDashboard = dashboardUtils.validateAndUpdateDashboard(theDashboard);

        var callAliasUpdateFunction = false;
        if (aliasesInfo) {
            var newEntityAliases = updateAliases(theDashboard, widget, aliasesInfo);
            var aliasesUpdated = !angular.equals(newEntityAliases, theDashboard.configuration.entityAliases);
            if (aliasesUpdated) {
                theDashboard.configuration.entityAliases = newEntityAliases;
                if (onAliasesUpdateFunction) {
                    callAliasUpdateFunction = true;
                }
            }
        }
        dashboardUtils.addWidgetToLayout(theDashboard, targetState, targetLayout, widget, originalColumns, originalSize, row, column);
        if (callAliasUpdateFunction) {
            onAliasesUpdateFunction();
        }
        deferred.resolve(theDashboard);
        return deferred.promise;
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
        return angular.equals(alias1.filter, alias2.filter);
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
            var newAliasName = createEntityAliasName(entityAliases, aliasInfo.alias);
            newAliasId = utils.guid();
            entityAliases[newAliasId] = {id: newAliasId, alias: newAliasName, filter: aliasInfo.filter};
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