/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import './rulechain.scss';

import 'tooltipster/dist/css/tooltipster.bundle.min.css';
import 'tooltipster/dist/js/tooltipster.bundle.min.js';
import 'tooltipster/dist/css/plugins/tooltipster/sideTip/themes/tooltipster-sideTip-shadow.min.css';

/* eslint-disable import/no-unresolved, import/default */

import addRuleNodeTemplate from './add-rulenode.tpl.html';
import addRuleNodeLinkTemplate from './add-link.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function RuleChainController($state, $scope, $compile, $q, $mdUtil, $timeout, $mdExpansionPanel, $window, $document, $mdDialog,
                                    $filter, $translate, hotkeys, types, ruleChainService, itembuffer, Modelfactory, flowchartConstants,
                                    ruleChain, ruleChainMetaData, ruleNodeComponents, helpLinks) {

    var vm = this;

    vm.$mdExpansionPanel = $mdExpansionPanel;
    vm.types = types;

    if ($state.current.data.import && !ruleChain) {
        $state.go('home.ruleChains');
        return;
    }

    vm.isImport = $state.current.data.import;
    vm.isConfirmOnExit = false;

    $scope.$watch(function() {
        return vm.isDirty || vm.isImport;
    }, (val) => {
        vm.isConfirmOnExit = val;
    });

    vm.errorTooltips = {};

    vm.isFullscreen = false;

    vm.editingRuleNode = null;
    vm.isEditingRuleNode = false;

    vm.editingRuleNodeLink = null;
    vm.isEditingRuleNodeLink = false;

    vm.isLibraryOpen = true;
    vm.enableHotKeys = true;

    Object.defineProperty(vm, 'isLibraryOpenReadonly', {
        get: function() { return vm.isLibraryOpen },
        set: function() {}
    });

    vm.ruleNodeSearch = '';

    vm.ruleChain = ruleChain;
    vm.ruleChainMetaData = ruleChainMetaData;

    vm.canvasControl = {};

    vm.ruleChainModel = {
        nodes: [],
        edges: []
    };

    vm.ruleNodeTypesModel = {};
    vm.ruleNodeTypesCanvasControl = {};
    vm.ruleChainLibraryLoaded = false;
    for (var type in types.ruleNodeType) {
        if (!types.ruleNodeType[type].special) {
            vm.ruleNodeTypesModel[type] = {
                model: {
                    nodes: [],
                    edges: []
                },
                selectedObjects: []
            };
            vm.ruleNodeTypesCanvasControl[type] = {};
        }
    }



    vm.selectedObjects = [];

    vm.modelservice = Modelfactory(vm.ruleChainModel, vm.selectedObjects);

    vm.saveRuleChain = saveRuleChain;
    vm.revertRuleChain = revertRuleChain;

    vm.objectsSelected = objectsSelected;
    vm.deleteSelected = deleteSelected;

    vm.isDebugModeEnabled = isDebugModeEnabled;
    vm.resetDebugModeInAllNodes = resetDebugModeInAllNodes;

    vm.triggerResize = triggerResize;

    vm.openRuleChainContextMenu = openRuleChainContextMenu;

    vm.helpLinkIdForRuleNodeType = helpLinkIdForRuleNodeType;

    initHotKeys();

    function openRuleChainContextMenu($event, $mdOpenMousepointMenu) {
        if (vm.canvasControl.modelservice && !$event.ctrlKey && !$event.metaKey) {
            var x = $event.clientX;
            var y = $event.clientY;
            var item = vm.canvasControl.modelservice.getItemInfoAtPoint(x, y);
            vm.contextInfo = prepareContextMenu(item);
            if (vm.contextInfo.items && vm.contextInfo.items.length > 0) {
                vm.contextMenuEvent = $event;
                $mdOpenMousepointMenu($event);
                return false;
            }
        }
    }

    function prepareContextMenu(item) {
        if (objectsSelected() || (!item.node && !item.edge)) {
            return prepareRuleChainContextMenu();
        } else if (item.node) {
            return prepareRuleNodeContextMenu(item.node);
        } else if (item.edge) {
            return prepareEdgeContextMenu(item.edge);
        }
    }

    function prepareRuleChainContextMenu() {
        var contextInfo = {
            headerClass: 'tb-rulechain',
            icon: 'settings_ethernet',
            title: vm.ruleChain.name,
            subtitle: $translate.instant('rulechain.rulechain')
        };
        contextInfo.items = [];
        if (vm.modelservice.nodes.getSelectedNodes().length) {
            contextInfo.items.push(
                {
                    action: function () {
                        copyRuleNodes();
                    },
                    enabled: true,
                    value: "rulenode.copy-selected",
                    icon: "content_copy",
                    shortcut: "M-C"
                }
            );
        }
        contextInfo.items.push(
            {
                action: function ($event) {
                    pasteRuleNodes($event);
                },
                enabled: itembuffer.hasRuleNodes(),
                value: "action.paste",
                icon: "content_paste",
                shortcut: "M-V"
            }
        );
        contextInfo.items.push(
            {
                divider: true
            }
        );
        if (objectsSelected()) {
            contextInfo.items.push(
                {
                    action: function () {
                        vm.modelservice.deselectAll();
                    },
                    enabled: true,
                    value: "rulenode.deselect-all",
                    icon: "tab_unselected",
                    shortcut: "Esc"
                }
            );
            contextInfo.items.push(
                {
                    action: function () {
                        vm.modelservice.deleteSelected();
                    },
                    enabled: true,
                    value: "rulenode.delete-selected",
                    icon: "clear",
                    shortcut: "Del"
                }
            );
        } else {
            contextInfo.items.push(
                {
                    action: function () {
                        vm.modelservice.selectAll();
                    },
                    enabled: true,
                    value: "rulenode.select-all",
                    icon: "select_all",
                    shortcut: "M-A"
                }
            );
        }
        contextInfo.items.push(
            {
                divider: true
            }
        );
        contextInfo.items.push(
            {
                action: function () {
                    vm.saveRuleChain();
                },
                enabled: !(vm.isInvalid || (!vm.isDirty && !vm.isImport)),
                value: "action.apply-changes",
                icon: "done",
                shortcut: "M-S"
            }
        );
        contextInfo.items.push(
            {
                action: function () {
                    vm.revertRuleChain();
                },
                enabled: vm.isDirty,
                value: "action.decline-changes",
                icon: "close",
                shortcut: "M-Z"
            }
        );
        return contextInfo;
    }

    function prepareRuleNodeContextMenu(node) {
        var contextInfo = {
            headerClass: node.nodeClass,
            icon: node.icon,
            iconUrl: node.iconUrl,
            title: node.name,
            subtitle: node.component.name
        };
        contextInfo.items = [];
        if (!node.readonly) {
            contextInfo.items.push(
                {
                    action: function () {
                        openNodeDetails(node);
                    },
                    enabled: true,
                    value: "rulenode.details",
                    icon: "menu"
                }
            );
            contextInfo.items.push(
                {
                    action: function () {
                        copyNode(node);
                    },
                    enabled: true,
                    value: "action.copy",
                    icon: "content_copy"
                }
            );
            contextInfo.items.push(
                {
                    action: function () {
                        vm.canvasControl.modelservice.nodes.delete(node);
                    },
                    enabled: true,
                    value: "action.delete",
                    icon: "clear",
                    shortcut: "M-X"
                }
            );
        }
        return contextInfo;
    }

    function prepareEdgeContextMenu(edge) {
        var contextInfo = {
            headerClass: 'tb-link',
            icon: 'trending_flat',
            title: edge.label,
            subtitle: $translate.instant('rulenode.link')
        };
        contextInfo.items = [];
        var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
        if (sourceNode.component.type != types.ruleNodeType.INPUT.value) {
            contextInfo.items.push(
                {
                    action: function () {
                        openLinkDetails(edge);
                    },
                    enabled: true,
                    value: "rulenode.details",
                    icon: "menu"
                }
            );
        }
        contextInfo.items.push(
            {
                action: function () {
                    vm.canvasControl.modelservice.edges.delete(edge);
                },
                enabled: true,
                value: "action.delete",
                icon: "clear",
                shortcut: "M-X"
            }
        );
        return contextInfo;
    }

    function initHotKeys() {
        hotkeys.bindTo($scope)
            .add({
                combo: 'ctrl+a',
                description: $translate.instant('rulenode.select-all-objects'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        vm.modelservice.selectAll();
                    }
                }
            })
            .add({
                combo: 'ctrl+c',
                description: $translate.instant('rulenode.copy-selected'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        copyRuleNodes();
                    }
                }
            })
            .add({
                combo: 'ctrl+v',
                description: $translate.instant('action.paste'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        if (itembuffer.hasRuleNodes()) {
                            pasteRuleNodes();
                        }
                    }
                }
            })
            .add({
                combo: 'esc',
                description: $translate.instant('rulenode.deselect-all-objects'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        event.stopPropagation();
                        vm.modelservice.deselectAll();
                    }
                }
            })
            .add({
                combo: 'ctrl+s',
                description: $translate.instant('action.apply'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        vm.saveRuleChain();
                    }
                }
            })
            .add({
                combo: 'ctrl+z',
                description: $translate.instant('action.decline-changes'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        vm.revertRuleChain();
                    }
                }
            })
            .add({
                combo: 'del',
                description: $translate.instant('rulenode.delete-selected-objects'),
                allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                callback: function (event) {
                    if (vm.enableHotKeys) {
                        event.preventDefault();
                        vm.modelservice.deleteSelected();
                    }
                }
            })
    }

    vm.onEditRuleNodeClosed = function() {
        vm.editingRuleNode = null;
    };

    vm.onEditRuleNodeLinkClosed = function() {
        vm.editingRuleNodeLink = null;
    };

    vm.saveRuleNode = function(theForm) {
        $scope.$broadcast('form-submit');
        if (theForm.$valid) {
            theForm.$setPristine();
            if (vm.editingRuleNode.error) {
                delete vm.editingRuleNode.error;
            }
            vm.ruleChainModel.nodes[vm.editingRuleNodeIndex] = vm.editingRuleNode;
            vm.editingRuleNode = angular.copy(vm.editingRuleNode);
            updateRuleNodesHighlight();
        }
    };

    vm.saveRuleNodeLink = function(theForm) {
        theForm.$setPristine();
        vm.ruleChainModel.edges[vm.editingRuleNodeLinkIndex] = vm.editingRuleNodeLink;
        vm.editingRuleNodeLink = angular.copy(vm.editingRuleNodeLink);
    };

    vm.onRevertRuleNodeEdit = function(theForm) {
        theForm.$setPristine();
        var node = vm.ruleChainModel.nodes[vm.editingRuleNodeIndex];
        vm.editingRuleNode = angular.copy(node);
    };

    vm.onRevertRuleNodeLinkEdit = function(theForm) {
        theForm.$setPristine();
        var edge = vm.ruleChainModel.edges[vm.editingRuleNodeLinkIndex];
        vm.editingRuleNodeLink = angular.copy(edge);
    };

    vm.nodeLibCallbacks = {
        nodeCallbacks: {
            'mouseEnter': function (event, node) {
                displayLibNodeDescriptionTooltip(event, node);
            },
            'mouseLeave': function () {
                destroyTooltips();
            },
            'mouseDown': function () {
                destroyTooltips();
            }
        }
    };

    vm.typeHeaderMouseEnter = function(event, typeId) {
        var ruleNodeType = types.ruleNodeType[typeId];
        displayTooltip(event,
            '<div class="tb-rule-node-tooltip tb-lib-tooltip">' +
            '<div id="tb-node-content" layout="column">' +
            '<div class="tb-node-title">' + $translate.instant(ruleNodeType.name) + '</div>' +
            '<div class="tb-node-details">' + $translate.instant(ruleNodeType.details) + '</div>' +
            '</div>' +
            '</div>'
        );
    };

    vm.destroyTooltips = destroyTooltips;

    function helpLinkIdForRuleNodeType() {
        return helpLinks.getRuleNodeLink(vm.editingRuleNode);
    }

    function destroyTooltips() {
        if (vm.tooltipTimeout) {
            $timeout.cancel(vm.tooltipTimeout);
            vm.tooltipTimeout = null;
        }
        var instances = angular.element.tooltipster.instances();
        instances.forEach((instance) => {
            if (!instance.isErrorTooltip) {
                instance.destroy();
            }
        });
    }

    function displayLibNodeDescriptionTooltip(event, node) {
        displayTooltip(event,
            '<div class="tb-rule-node-tooltip tb-lib-tooltip">' +
            '<div id="tb-node-content" layout="column">' +
            '<div class="tb-node-title">' + node.component.name + '</div>' +
            '<div class="tb-node-description">' + node.component.configurationDescriptor.nodeDefinition.description + '</div>' +
            '<div class="tb-node-details">' + node.component.configurationDescriptor.nodeDefinition.details + '</div>' +
            '</div>' +
            '</div>'
        );
    }

    function displayNodeDescriptionTooltip(event, node) {
        if (!vm.errorTooltips[node.id]) {
            var name, desc, details;
            if (node.component.type == vm.types.ruleNodeType.INPUT.value) {
                name = $translate.instant(vm.types.ruleNodeType.INPUT.name) + '';
                desc = $translate.instant(vm.types.ruleNodeType.INPUT.details) + '';
            } else {
                name = node.name;
                desc = $translate.instant(vm.types.ruleNodeType[node.component.type].name) + ' - ' + node.component.name;
                if (node.additionalInfo) {
                    details = node.additionalInfo.description;
                }
            }
            var tooltipContent = '<div class="tb-rule-node-tooltip">' +
                '<div id="tb-node-content" layout="column">' +
                '<div class="tb-node-title">' + name + '</div>' +
                '<div class="tb-node-description">' + desc + '</div>';
            if (details) {
                tooltipContent += '<div class="tb-node-details">' + details + '</div>';
            }
            tooltipContent += '</div>' +
                '</div>';
            displayTooltip(event, tooltipContent);
        }
    }

    function displayTooltip(event, content) {
        destroyTooltips();
        vm.tooltipTimeout = $timeout(() => {
            var element = angular.element(event.target);
            element.tooltipster(
                {
                    theme: 'tooltipster-shadow',
                    delay: 100,
                    trigger: 'custom',
                    triggerOpen: {
                        click: false,
                        tap: false
                    },
                    triggerClose: {
                        click: true,
                        tap: true,
                        scroll: true
                    },
                    side: 'right',
                    trackOrigin: true
                }
            );
            var contentElement = angular.element(content);
            $compile(contentElement)($scope);
            var tooltip = element.tooltipster('instance');
            tooltip.content(contentElement);
            tooltip.open();
        }, 500);
    }

    function updateNodeErrorTooltip(node) {
        if (node.error) {
            var element = angular.element('#' + node.id);
            var tooltip = vm.errorTooltips[node.id];
            if (!tooltip || !element.hasClass("tooltipstered")) {
                element.tooltipster(
                    {
                        theme: 'tooltipster-shadow',
                        delay: 0,
                        animationDuration: 0,
                        trigger: 'custom',
                        triggerOpen: {
                            click: false,
                            tap: false
                        },
                        triggerClose: {
                            click: false,
                            tap: false,
                            scroll: false
                        },
                        side: 'top',
                        trackOrigin: true
                    }
                );
                var content = '<div class="tb-rule-node-error-tooltip">' +
                    '<div id="tooltip-content" layout="column">' +
                    '<div class="tb-node-details">' + node.error + '</div>' +
                    '</div>' +
                    '</div>';
                var contentElement = angular.element(content);
                $compile(contentElement)($scope);
                tooltip = element.tooltipster('instance');
                tooltip.isErrorTooltip = true;
                tooltip.content(contentElement);
                vm.errorTooltips[node.id] = tooltip;
            }
            $mdUtil.nextTick(() => {
                tooltip.open();
            });
        } else {
            if (vm.errorTooltips[node.id]) {
                tooltip = vm.errorTooltips[node.id];
                tooltip.destroy();
                delete vm.errorTooltips[node.id];
            }
        }
    }

    function updateErrorTooltips(hide) {
        for (var nodeId in vm.errorTooltips) {
            var tooltip = vm.errorTooltips[nodeId];
            if (hide) {
                tooltip.close();
            } else {
                tooltip.open();
            }
        }
    }

    $scope.$watch(function() {
        return vm.isEditingRuleNode || vm.isEditingRuleNodeLink;
    }, (val) => {
        vm.enableHotKeys = !val;
        updateErrorTooltips(val);
    });

    vm.editCallbacks = {
        edgeDoubleClick: function (event, edge) {
            openLinkDetails(edge);
        },
        edgeEdit: function(event, edge) {
            openLinkDetails(edge);
        },
        nodeCallbacks: {
            'doubleClick': function (event, node) {
                openNodeDetails(node);
            },
            'nodeEdit': function (event, node) {
                openNodeDetails(node);
            },
            'mouseEnter': function (event, node) {
                displayNodeDescriptionTooltip(event, node);
            },
            'mouseLeave': function () {
                destroyTooltips();
            },
            'mouseDown': function () {
                destroyTooltips();
            }
        },
        isValidEdge: function (source, destination) {
            return source.type === flowchartConstants.rightConnectorType && destination.type === flowchartConstants.leftConnectorType;
        },
        createEdge: function (event, edge) {
            var deferred = $q.defer();
            var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
            if (sourceNode.component.type == types.ruleNodeType.INPUT.value) {
                var destNode = vm.modelservice.nodes.getNodeByConnectorId(edge.destination);
                if (destNode.component.type == types.ruleNodeType.RULE_CHAIN.value) {
                    deferred.reject();
                } else {
                    var res = $filter('filter')(vm.ruleChainModel.edges, {source: vm.inputConnectorId}, true);
                    if (res && res.length) {
                        vm.modelservice.edges.delete(res[0]);
                    }
                    deferred.resolve(edge);
                }
            } else {
                if (edge.label) {
                    if (!edge.labels) {
                        edge.labels = edge.label.split(' / ');
                    }
                    deferred.resolve(edge);
                } else {
                    var labels = ruleChainService.getRuleNodeSupportedLinks(sourceNode.component);
                    var allowCustomLabels = ruleChainService.ruleNodeAllowCustomLinks(sourceNode.component);
                    vm.enableHotKeys = false;
                    addRuleNodeLink(event, edge, labels, allowCustomLabels).then(
                        (link) => {
                            deferred.resolve(link);
                            vm.enableHotKeys = true;
                        },
                        () => {
                            deferred.reject();
                            vm.enableHotKeys = true;
                        }
                    );
                }
            }
            return deferred.promise;
        },
        dropNode: function (event, node) {
            addRuleNode(event, node);
        }
    };

    function openNodeDetails(node) {
        if (node.component.type != types.ruleNodeType.INPUT.value) {
            vm.isEditingRuleNodeLink = false;
            vm.editingRuleNodeLink = null;
            vm.isEditingRuleNode = true;
            vm.editingRuleNodeIndex = vm.ruleChainModel.nodes.indexOf(node);
            vm.editingRuleNode = angular.copy(node);
            $mdUtil.nextTick(() => {
                if (vm.ruleNodeForm) {
                    vm.ruleNodeForm.$setPristine();
                }
            });
        }
    }

    function openLinkDetails(edge) {
        var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
        if (sourceNode.component.type != types.ruleNodeType.INPUT.value) {
            vm.isEditingRuleNode = false;
            vm.editingRuleNode = null;
            vm.editingRuleNodeLinkLabels = ruleChainService.getRuleNodeSupportedLinks(sourceNode.component);
            vm.editingRuleNodeAllowCustomLabels = ruleChainService.ruleNodeAllowCustomLinks(sourceNode.component);
            vm.isEditingRuleNodeLink = true;
            vm.editingRuleNodeLinkIndex = vm.ruleChainModel.edges.indexOf(edge);
            vm.editingRuleNodeLink = angular.copy(edge);
            $mdUtil.nextTick(() => {
                if (vm.ruleNodeLinkForm) {
                    vm.ruleNodeLinkForm.$setPristine();
                }
            });
        }
    }

    function copyNode(node) {
        itembuffer.copyRuleNodes([node], []);
    }

    function copyRuleNodes() {
        var nodes = vm.modelservice.nodes.getSelectedNodes();
        var edges = vm.modelservice.edges.getSelectedEdges();
        var connections = [];
        for (var i=0;i<edges.length;i++) {
            var edge = edges[i];
            var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
            var destNode = vm.modelservice.nodes.getNodeByConnectorId(edge.destination);
            var isInputSource = sourceNode.component.type == types.ruleNodeType.INPUT.value;
            var fromIndex = nodes.indexOf(sourceNode);
            var toIndex = nodes.indexOf(destNode);
            if ( (isInputSource || fromIndex > -1) && toIndex > -1 ) {
                var connection = {
                    isInputSource: isInputSource,
                    fromIndex: fromIndex,
                    toIndex: toIndex,
                    label: edge.label,
                    labels: edge.labels
                };
                connections.push(connection);
            }
        }
        itembuffer.copyRuleNodes(nodes, connections);
    }

    function pasteRuleNodes(event) {
        var canvas = angular.element(vm.canvasControl.modelservice.getCanvasHtmlElement());
        var x,y;
        if (event) {
            var offset = canvas.offset();
            x = Math.round(event.clientX - offset.left);
            y = Math.round(event.clientY - offset.top);
        } else {
            var scrollParent = canvas.parent();
            var scrollTop = scrollParent.scrollTop();
            var scrollLeft = scrollParent.scrollLeft();
            x = scrollLeft + scrollParent.width()/2;
            y = scrollTop + scrollParent.height()/2;
        }
        var ruleNodes = itembuffer.pasteRuleNodes(vm.ruleChain.type, x, y);
        if (ruleNodes) {
            vm.modelservice.deselectAll();
            var nodes = [];
            for (var i=0;i<ruleNodes.nodes.length;i++) {
                var node = ruleNodes.nodes[i];
                node.id = 'rule-chain-node-' + vm.nextNodeID++;
                var component = node.component;
                if (component.configurationDescriptor.nodeDefinition.inEnabled) {
                    node.connectors.push(
                        {
                            type: flowchartConstants.leftConnectorType,
                            id: vm.nextConnectorID++
                        }
                    );
                }
                if (component.configurationDescriptor.nodeDefinition.outEnabled) {
                    node.connectors.push(
                        {
                            type: flowchartConstants.rightConnectorType,
                            id: vm.nextConnectorID++
                        }
                    );
                }
                nodes.push(node);
                vm.ruleChainModel.nodes.push(node);
                vm.modelservice.nodes.select(node);
            }
            for (i=0;i<ruleNodes.connections.length;i++) {
                var connection = ruleNodes.connections[i];
                var sourceNode = nodes[connection.fromIndex];
                var destNode = nodes[connection.toIndex];
                if ( (connection.isInputSource || sourceNode) &&  destNode ) {
                    var source, destination;
                    if (connection.isInputSource) {
                        source = vm.inputConnectorId;
                    } else {
                        var sourceConnectors = vm.modelservice.nodes.getConnectorsByType(sourceNode, flowchartConstants.rightConnectorType);
                        if (sourceConnectors && sourceConnectors.length) {
                            source = sourceConnectors[0].id;
                        }
                    }
                    var destConnectors = vm.modelservice.nodes.getConnectorsByType(destNode, flowchartConstants.leftConnectorType);
                    if (destConnectors && destConnectors.length) {
                        destination = destConnectors[0].id;
                    }
                    if (source && destination) {
                        var edge = {
                            source: source,
                            destination: destination,
                            label: connection.label,
                            labels: connection.labels
                        };
                        vm.ruleChainModel.edges.push(edge);
                        vm.modelservice.edges.select(edge);
                    }
                }
            }

            if (vm.canvasControl.adjustCanvasSize) {
                vm.canvasControl.adjustCanvasSize();
            }

            updateRuleNodesHighlight();

            validate();
        }
    }

    loadRuleChainLibrary(ruleNodeComponents, true);

    $scope.$watch('vm.ruleNodeSearch',
        function (newVal, oldVal) {
            if (!angular.equals(newVal, oldVal)) {
                var res = $filter('filter')(ruleNodeComponents, {name: vm.ruleNodeSearch});
                loadRuleChainLibrary(res);
            }
        }
    );

    $scope.$on('searchTextUpdated', function () {
        updateRuleNodesHighlight();
    });

    function loadRuleChainLibrary(ruleNodeComponents, loadRuleChain) {
        for (var componentType in vm.ruleNodeTypesModel) {
            vm.ruleNodeTypesModel[componentType].model.nodes.length = 0;
        }
        for (var i=0;i<ruleNodeComponents.length;i++) {
            var ruleNodeComponent = ruleNodeComponents[i];
            componentType = ruleNodeComponent.type;
            var model = vm.ruleNodeTypesModel[componentType].model;
            var icon = vm.types.ruleNodeType[componentType].icon;
            var iconUrl = null;
            if (ruleNodeComponent.configurationDescriptor.nodeDefinition.icon) {
                icon = ruleNodeComponent.configurationDescriptor.nodeDefinition.icon;
            }
            if (ruleNodeComponent.configurationDescriptor.nodeDefinition.iconUrl) {
                iconUrl = ruleNodeComponent.configurationDescriptor.nodeDefinition.iconUrl;
            }
            var node = {
                id: 'node-lib-' + componentType + '-' + model.nodes.length,
                component: ruleNodeComponent,
                name: '',
                nodeClass: vm.types.ruleNodeType[componentType].nodeClass,
                icon: icon,
                iconUrl: iconUrl,
                x: 30,
                y: 10+50*model.nodes.length,
                connectors: []
            };
            if (ruleNodeComponent.configurationDescriptor.nodeDefinition.inEnabled) {
                node.connectors.push(
                    {
                        type: flowchartConstants.leftConnectorType,
                        id: model.nodes.length * 2
                    }
                );
            }
            if (ruleNodeComponent.configurationDescriptor.nodeDefinition.outEnabled) {
                node.connectors.push(
                    {
                        type: flowchartConstants.rightConnectorType,
                        id: model.nodes.length * 2 + 1
                    }
                );
            }
            model.nodes.push(node);
        }
        vm.ruleChainLibraryLoaded = true;
        if (loadRuleChain) {
            prepareRuleChain();
        }
        $mdUtil.nextTick(() => {
            for (componentType in vm.ruleNodeTypesCanvasControl) {
                if (vm.ruleNodeTypesCanvasControl[componentType].adjustCanvasSize) {
                    vm.ruleNodeTypesCanvasControl[componentType].adjustCanvasSize(true);
                }
            }
            for (componentType in vm.ruleNodeTypesModel) {
                var panel = vm.$mdExpansionPanel(componentType);
                if (panel) {
                    if (!vm.ruleNodeTypesModel[componentType].model.nodes.length) {
                        panel.collapse();
                    } else {
                        panel.expand();
                    }
                }
            }
        });
    }

    function prepareRuleChain() {

        if (vm.ruleChainWatch) {
            vm.ruleChainWatch();
            vm.ruleChainWatch = null;
        }

        vm.nextNodeID = 1;
        vm.nextConnectorID = 1;

        vm.selectedObjects.length = 0;
        vm.ruleChainModel.nodes.length = 0;
        vm.ruleChainModel.edges.length = 0;

        vm.inputConnectorId = vm.nextConnectorID++;

        vm.ruleChainModel.nodes.push(
            {
                id: 'rule-chain-node-' + vm.nextNodeID++,
                component: types.inputNodeComponent,
                name: "",
                nodeClass: types.ruleNodeType.INPUT.nodeClass,
                icon: types.ruleNodeType.INPUT.icon,
                readonly: true,
                x: 50,
                y: 150,
                connectors: [
                    {
                        type: flowchartConstants.rightConnectorType,
                        id: vm.inputConnectorId
                    },
                ]

            }
        );
        ruleChainService.resolveTargetRuleChains(vm.ruleChainMetaData.ruleChainConnections)
            .then((ruleChainsMap) => {
                createRuleChainModel(ruleChainsMap);
            }
        );
    }

    function createRuleChainModel(ruleChainsMap) {
        var nodes = [];
        for (var i=0;i<vm.ruleChainMetaData.nodes.length;i++) {
            var ruleNode = vm.ruleChainMetaData.nodes[i];
            var component = ruleChainService.getRuleNodeComponentByClazz(ruleNode.type, vm.ruleChain.type);
            if (component) {
                var icon = vm.types.ruleNodeType[component.type].icon;
                var iconUrl = null;
                if (component.configurationDescriptor.nodeDefinition.icon) {
                    icon = component.configurationDescriptor.nodeDefinition.icon;
                }
                if (component.configurationDescriptor.nodeDefinition.iconUrl) {
                    iconUrl = component.configurationDescriptor.nodeDefinition.iconUrl;
                }
                var node = {
                    id: 'rule-chain-node-' + vm.nextNodeID++,
                    ruleNodeId: ruleNode.id,
                    additionalInfo: ruleNode.additionalInfo,
                    configuration: ruleNode.configuration,
                    debugMode: ruleNode.debugMode,
                    x: Math.round(ruleNode.additionalInfo.layoutX),
                    y: Math.round(ruleNode.additionalInfo.layoutY),
                    component: component,
                    name: ruleNode.name,
                    nodeClass: vm.types.ruleNodeType[component.type].nodeClass,
                    icon: icon,
                    iconUrl: iconUrl,
                    connectors: []
                };
                if (component.configurationDescriptor.nodeDefinition.inEnabled) {
                    node.connectors.push(
                        {
                            type: flowchartConstants.leftConnectorType,
                            id: vm.nextConnectorID++
                        }
                    );
                }
                if (component.configurationDescriptor.nodeDefinition.outEnabled) {
                    node.connectors.push(
                        {
                            type: flowchartConstants.rightConnectorType,
                            id: vm.nextConnectorID++
                        }
                    );
                }
                nodes.push(node);
                vm.ruleChainModel.nodes.push(node);
            }
        }

        if (vm.ruleChainMetaData.firstNodeIndex > -1) {
            var destNode = nodes[vm.ruleChainMetaData.firstNodeIndex];
            if (destNode) {
                var connectors = vm.modelservice.nodes.getConnectorsByType(destNode, flowchartConstants.leftConnectorType);
                if (connectors && connectors.length) {
                    var edge = {
                        source: vm.inputConnectorId,
                        destination: connectors[0].id
                    };
                    vm.ruleChainModel.edges.push(edge);
                }
            }
        }

        if (vm.ruleChainMetaData.connections) {
            var edgeMap = {};
            for (i = 0; i < vm.ruleChainMetaData.connections.length; i++) {
                var connection = vm.ruleChainMetaData.connections[i];
                var sourceNode = nodes[connection.fromIndex];
                destNode = nodes[connection.toIndex];
                if (sourceNode && destNode) {
                    var sourceConnectors = vm.modelservice.nodes.getConnectorsByType(sourceNode, flowchartConstants.rightConnectorType);
                    var destConnectors = vm.modelservice.nodes.getConnectorsByType(destNode, flowchartConstants.leftConnectorType);
                    if (sourceConnectors && sourceConnectors.length && destConnectors && destConnectors.length) {
                        var sourceId = sourceConnectors[0].id;
                        var destId = destConnectors[0].id;
                        var edgeKey = sourceId + '_' + destId;
                        edge = edgeMap[edgeKey];
                        if (!edge) {
                            edge = {
                                source: sourceId,
                                destination: destId,
                                label: connection.type,
                                labels: [connection.type]
                            };
                            edgeMap[edgeKey] = edge;
                            vm.ruleChainModel.edges.push(edge);
                        } else {
                            edge.label += ' / ' +connection.type;
                            edge.labels.push(connection.type);
                        }
                    }
                }
            }
        }

        if (vm.ruleChainMetaData.ruleChainConnections) {
            var ruleChainNodesMap = {};
            var ruleChainEdgeMap = {};
            for (i = 0; i < vm.ruleChainMetaData.ruleChainConnections.length; i++) {
                var ruleChainConnection = vm.ruleChainMetaData.ruleChainConnections[i];
                var ruleChain = ruleChainsMap[ruleChainConnection.targetRuleChainId.id];
                if (ruleChainConnection.additionalInfo && ruleChainConnection.additionalInfo.ruleChainNodeId) {
                    var ruleChainNode = ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId];
                    if (!ruleChainNode) {
                        ruleChainNode = {
                            id: 'rule-chain-node-' + vm.nextNodeID++,
                            additionalInfo: ruleChainConnection.additionalInfo,
                            x: Math.round(ruleChainConnection.additionalInfo.layoutX),
                            y: Math.round(ruleChainConnection.additionalInfo.layoutY),
                            component: types.ruleChainNodeComponent,
                            nodeClass: vm.types.ruleNodeType.RULE_CHAIN.nodeClass,
                            icon: vm.types.ruleNodeType.RULE_CHAIN.icon,
                            connectors: [
                                {
                                    type: flowchartConstants.leftConnectorType,
                                    id: vm.nextConnectorID++
                                }
                            ]
                        };
                        if (ruleChain.name) {
                            ruleChainNode.name = ruleChain.name;
                            ruleChainNode.targetRuleChainId = ruleChainConnection.targetRuleChainId.id;
                        } else {
                            ruleChainNode.name = "Unresolved";
                            ruleChainNode.targetRuleChainId = null;
                            ruleChainNode.error = $translate.instant('rulenode.invalid-target-rulechain');
                        }
                        ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId] = ruleChainNode;
                        vm.ruleChainModel.nodes.push(ruleChainNode);
                    }
                    sourceNode = nodes[ruleChainConnection.fromIndex];
                    if (sourceNode) {
                        connectors = vm.modelservice.nodes.getConnectorsByType(sourceNode, flowchartConstants.rightConnectorType);
                        if (connectors && connectors.length) {
                            sourceId = connectors[0].id;
                            destId = ruleChainNode.connectors[0].id;
                            edgeKey = sourceId + '_' + destId;
                            var ruleChainEdge = ruleChainEdgeMap[edgeKey];
                            if (!ruleChainEdge) {
                                ruleChainEdge = {
                                    source: sourceId,
                                    destination: destId,
                                    label: ruleChainConnection.type,
                                    labels: [ruleChainConnection.type]
                                };
                                ruleChainEdgeMap[edgeKey] = ruleChainEdge;
                                vm.ruleChainModel.edges.push(ruleChainEdge);
                            } else {
                                ruleChainEdge.label += ' / ' +ruleChainConnection.type;
                                ruleChainEdge.labels.push(ruleChainConnection.type);
                            }
                        }
                    }
                }
            }
        }

        if (vm.canvasControl.adjustCanvasSize) {
            vm.canvasControl.adjustCanvasSize(true);
        }

        vm.isDirty = false;

        updateRuleNodesHighlight();

        validate();

        $mdUtil.nextTick(() => {
            vm.ruleChainWatch = $scope.$watch('vm.ruleChainModel',
                function (newVal, oldVal) {
                    if (!angular.equals(newVal, oldVal)) {
                        validate();
                        if (!vm.isDirty) {
                            vm.isDirty = true;
                        }
                    }
                }, true
            );
        });
    }

    function updateRuleNodesHighlight() {
        for (var i = 0; i < vm.ruleChainModel.nodes.length; i++) {
            vm.ruleChainModel.nodes[i].highlighted = false;
        }
        if ($scope.searchConfig.searchText) {
            var res = $filter('filter')(vm.ruleChainModel.nodes, {name: $scope.searchConfig.searchText});
            if (res) {
                for (i = 0; i < res.length; i++) {
                    res[i].highlighted = true;
                }
            }
        }
    }

    function validate() {
        $mdUtil.nextTick(() => {
            vm.isInvalid = false;
            for (var i = 0; i < vm.ruleChainModel.nodes.length; i++) {
                if (vm.ruleChainModel.nodes[i].error) {
                    vm.isInvalid = true;
                }
                updateNodeErrorTooltip(vm.ruleChainModel.nodes[i]);
            }
        });
    }

    function saveRuleChain() {
        var saveRuleChainPromise;
        if (vm.isImport) {
            if (angular.isUndefined(vm.ruleChain.type)) {
                vm.ruleChain.type = types.ruleChainType.core;
            }
            saveRuleChainPromise = ruleChainService.saveRuleChain(vm.ruleChain);
        } else {
            saveRuleChainPromise = $q.when(vm.ruleChain);
        }
        saveRuleChainPromise.then(
            (ruleChain) => {
                vm.ruleChain = ruleChain;
                var ruleChainMetaData = {
                    ruleChainId: vm.ruleChain.id,
                    nodes: [],
                    connections: [],
                    ruleChainConnections: []
                };

                var nodes = [];

                for (var i=0;i<vm.ruleChainModel.nodes.length;i++) {
                    var node = vm.ruleChainModel.nodes[i];
                    if (node.component.type != types.ruleNodeType.INPUT.value && node.component.type != types.ruleNodeType.RULE_CHAIN.value) {
                        var ruleNode = {};
                        if (node.ruleNodeId) {
                            ruleNode.id = node.ruleNodeId;
                        }
                        ruleNode.type = node.component.clazz;
                        ruleNode.name = node.name;
                        ruleNode.configuration = node.configuration;
                        ruleNode.additionalInfo = node.additionalInfo;
                        ruleNode.debugMode = node.debugMode;
                        if (!ruleNode.additionalInfo) {
                            ruleNode.additionalInfo = {};
                        }
                        ruleNode.additionalInfo.layoutX = Math.round(node.x);
                        ruleNode.additionalInfo.layoutY = Math.round(node.y);
                        ruleChainMetaData.nodes.push(ruleNode);
                        nodes.push(node);
                    }
                }
                var res = $filter('filter')(vm.ruleChainModel.edges, {source: vm.inputConnectorId}, true);
                if (res && res.length) {
                    var firstNodeEdge = res[0];
                    var firstNode = vm.modelservice.nodes.getNodeByConnectorId(firstNodeEdge.destination);
                    ruleChainMetaData.firstNodeIndex = nodes.indexOf(firstNode);
                }
                for (i=0;i<vm.ruleChainModel.edges.length;i++) {
                    var edge = vm.ruleChainModel.edges[i];
                    var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
                    var destNode = vm.modelservice.nodes.getNodeByConnectorId(edge.destination);
                    if (sourceNode.component.type != types.ruleNodeType.INPUT.value) {
                        var fromIndex = nodes.indexOf(sourceNode);
                        if (destNode.component.type == types.ruleNodeType.RULE_CHAIN.value) {
                            var ruleChainConnection = {
                                fromIndex: fromIndex,
                                targetRuleChainId: {entityType: vm.types.entityType.rulechain, id: destNode.targetRuleChainId},
                                additionalInfo: destNode.additionalInfo
                            };
                            if (!ruleChainConnection.additionalInfo) {
                                ruleChainConnection.additionalInfo = {};
                            }
                            ruleChainConnection.additionalInfo.layoutX = Math.round(destNode.x);
                            ruleChainConnection.additionalInfo.layoutY = Math.round(destNode.y);
                            ruleChainConnection.additionalInfo.ruleChainNodeId = destNode.id;
                            for (var rcIndex=0;rcIndex<edge.labels.length;rcIndex++) {
                                var newRuleChainConnection = angular.copy(ruleChainConnection);
                                newRuleChainConnection.type = edge.labels[rcIndex];
                                ruleChainMetaData.ruleChainConnections.push(newRuleChainConnection);
                            }
                        } else {
                            var toIndex = nodes.indexOf(destNode);
                            var nodeConnection = {
                                fromIndex: fromIndex,
                                toIndex: toIndex
                            };
                            for (var cIndex=0;cIndex<edge.labels.length;cIndex++) {
                                var newNodeConnection = angular.copy(nodeConnection);
                                newNodeConnection.type = edge.labels[cIndex];
                                ruleChainMetaData.connections.push(newNodeConnection);
                            }
                        }
                    }
                }
                ruleChainService.saveRuleChainMetaData(ruleChainMetaData).then(
                    (ruleChainMetaData) => {
                        vm.ruleChainMetaData = ruleChainMetaData;
                        if (vm.isImport) {
                            vm.isDirty = false;
                            vm.isImport = false;
                            $mdUtil.nextTick(() => {
                                if (vm.ruleChain.type === vm.types.ruleChainType.core) {
                                    $state.go('home.ruleChains.ruleChain', {ruleChainId: vm.ruleChain.id.id});
                                } else {
                                    $state.go('home.edges.ruleChains.ruleChain', {ruleChainId: vm.ruleChain.id.id});
                                }
                            });
                        } else {
                            prepareRuleChain();
                        }
                    }
                );
            }
        );
    }

    function revertRuleChain() {
        prepareRuleChain();
    }

    function addRuleNode($event, ruleNode) {

        ruleNode.configuration = angular.copy(ruleNode.component.configurationDescriptor.nodeDefinition.defaultConfiguration);

        var ruleChainId = vm.ruleChain.id ? vm.ruleChain.id.id : null;
        var ruleChainType = vm.ruleChain.type ? vm.ruleChain.type : types.ruleChainType.core;

        vm.enableHotKeys = false;

        $mdDialog.show({
            controller: 'AddRuleNodeController',
            controllerAs: 'vm',
            templateUrl: addRuleNodeTemplate,
            parent: angular.element($document[0].body),
            locals: {ruleNode: ruleNode, ruleChainId: ruleChainId, ruleChainType: ruleChainType},
            fullscreen: true,
            targetEvent: $event
        }).then(function (ruleNode) {
            ruleNode.id = 'rule-chain-node-' + vm.nextNodeID++;
            ruleNode.connectors = [];
            if (ruleNode.component.configurationDescriptor.nodeDefinition.inEnabled) {
                ruleNode.connectors.push(
                    {
                        id: vm.nextConnectorID++,
                        type: flowchartConstants.leftConnectorType
                    }
                );
            }
            if (ruleNode.component.configurationDescriptor.nodeDefinition.outEnabled) {
                ruleNode.connectors.push(
                    {
                        id: vm.nextConnectorID++,
                        type: flowchartConstants.rightConnectorType
                    }
                );
            }
            vm.ruleChainModel.nodes.push(ruleNode);
            updateRuleNodesHighlight();
            vm.enableHotKeys = true;
        }, function () {
            vm.enableHotKeys = true;
        });
    }

    function addRuleNodeLink($event, link, labels, allowCustomLabels) {
        return $mdDialog.show({
            controller: 'AddRuleNodeLinkController',
            controllerAs: 'vm',
            templateUrl: addRuleNodeLinkTemplate,
            parent: angular.element($document[0].body),
            locals: {link: link, labels: labels, allowCustomLabels: allowCustomLabels},
            fullscreen: true,
            targetEvent: $event
        });
    }

    function objectsSelected() {
        return vm.modelservice.nodes.getSelectedNodes().length > 0 ||
            vm.modelservice.edges.getSelectedEdges().length > 0
    }

    function deleteSelected() {
        vm.modelservice.deleteSelected();
    }

    function isDebugModeEnabled() {
        var res = $filter('filter')(vm.ruleChainModel.nodes, {debugMode: true});
        return (res && res.length);
    }

    function resetDebugModeInAllNodes() {
        vm.ruleChainModel.nodes.forEach((node) => {
            if (node.component.type != types.ruleNodeType.INPUT.value && node.component.type != types.ruleNodeType.RULE_CHAIN.value) {
                node.debugMode = false;
            }
        });
    }

    function triggerResize() {
        var w = angular.element($window);
        w.triggerHandler('resize');
    }
}

/*@ngInject*/
export function AddRuleNodeController($scope, $mdDialog, ruleNode, ruleChainId, ruleChainType, helpLinks) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.ruleNode = ruleNode;
    vm.ruleChainId = ruleChainId;
    vm.ruleChainType = ruleChainType;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(vm.ruleNode);
    }
}

/*@ngInject*/
export function AddRuleNodeLinkController($scope, $mdDialog, link, labels, allowCustomLabels, helpLinks) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.link = link;
    vm.labels = labels;
    vm.allowCustomLabels = allowCustomLabels;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(vm.link);
    }
}
