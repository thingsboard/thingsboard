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

import './rulechain.scss';

/* eslint-disable import/no-unresolved, import/default */

import addRuleNodeTemplate from './add-rulenode.tpl.html';
import addRuleNodeLinkTemplate from './add-link.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


const deleteKeyCode = 46;
const ctrlKeyCode = 17;
const aKeyCode = 65;
const escKeyCode = 27;

/*@ngInject*/
export function RuleChainController($stateParams, $scope, $q, $mdUtil, $mdExpansionPanel, $document, $mdDialog, $filter, types, ruleChainService, Modelfactory, flowchartConstants, ruleChain, ruleChainMetaData) {

    var vm = this;

    vm.$mdExpansionPanel = $mdExpansionPanel;
    vm.types = types;

    vm.editingRuleNode = null;
    vm.isEditingRuleNode = false;

    vm.editingRuleNodeLink = null;
    vm.isEditingRuleNodeLink = false;

    vm.ruleChain = ruleChain;
    vm.ruleChainMetaData = ruleChainMetaData;

    vm.canvasControl = {};

    vm.ruleChainModel = {
        nodes: [],
        edges: []
    };

    vm.ruleNodeTypesModel = {};
    for (var type in types.ruleNodeType) {
        if (!types.ruleNodeType[type].special) {
            vm.ruleNodeTypesModel[type] = {
                model: {
                    nodes: [],
                    edges: []
                },
                selectedObjects: []
            };
        }
    }

    vm.selectedObjects = [];

    vm.modelservice = Modelfactory(vm.ruleChainModel, vm.selectedObjects);

    vm.ctrlDown = false;

    vm.saveRuleChain = saveRuleChain;
    vm.revertRuleChain = revertRuleChain;

    vm.keyDown = function (evt) {
        if (evt.keyCode === ctrlKeyCode) {
            vm.ctrlDown = true;
            evt.stopPropagation();
            evt.preventDefault();
        }
    };

    vm.keyUp = function (evt) {

        if (evt.keyCode === deleteKeyCode) {
            vm.modelservice.deleteSelected();
        }

        if (evt.keyCode == aKeyCode && vm.ctrlDown) {
            vm.modelservice.selectAll();
        }

        if (evt.keyCode == escKeyCode) {
            vm.modelservice.deselectAll();
        }

        if (evt.keyCode === ctrlKeyCode) {
            vm.ctrlDown = false;
            evt.stopPropagation();
            evt.preventDefault();
        }
    };

    vm.onEditRuleNodeClosed = function() {
        vm.editingRuleNode = null;
    };

    vm.onEditRuleNodeLinkClosed = function() {
        vm.editingRuleNodeLink = null;
    };

    vm.saveRuleNode = function(theForm) {
        theForm.$setPristine();
        vm.isEditingRuleNode = false;
        vm.ruleChainModel.nodes[vm.editingRuleNodeIndex] = vm.editingRuleNode;
        vm.editingRuleNode = angular.copy(vm.editingRuleNode);
    };

    vm.saveRuleNodeLink = function(theForm) {
        theForm.$setPristine();
        vm.isEditingRuleNodeLink = false;
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

    vm.editCallbacks = {
        edgeDoubleClick: function (event, edge) {
            var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
            if (sourceNode.nodeType != types.ruleNodeType.INPUT.value) {
                ruleChainService.getRuleNodeSupportedLinks(sourceNode.type).then(
                    (labels) => {
                        vm.isEditingRuleNode = false;
                        vm.editingRuleNode = null;
                        vm.editingRuleNodeLinkLabels = labels;
                        vm.isEditingRuleNodeLink = true;
                        vm.editingRuleNodeLinkIndex = vm.ruleChainModel.edges.indexOf(edge);
                        vm.editingRuleNodeLink = angular.copy(edge);
                    }
                );
            }
        },
        nodeCallbacks: {
            'doubleClick': function (event, node) {
                if (node.nodeType != types.ruleNodeType.INPUT.value) {
                    vm.isEditingRuleNodeLink = false;
                    vm.editingRuleNodeLink = null;
                    vm.isEditingRuleNode = true;
                    vm.editingRuleNodeIndex = vm.ruleChainModel.nodes.indexOf(node);
                    vm.editingRuleNode = angular.copy(node);
                }
            }
        },
        isValidEdge: function (source, destination) {
            return source.type === flowchartConstants.rightConnectorType && destination.type === flowchartConstants.leftConnectorType;
        },
        createEdge: function (event, edge) {
            var deferred = $q.defer();
            var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
            if (sourceNode.nodeType == types.ruleNodeType.INPUT.value) {
                var destNode = vm.modelservice.nodes.getNodeByConnectorId(edge.destination);
                if (destNode.nodeType == types.ruleNodeType.RULE_CHAIN.value) {
                    deferred.reject();
                } else {
                    var res = $filter('filter')(vm.ruleChainModel.edges, {source: vm.inputConnectorId});
                    if (res && res.length) {
                        vm.modelservice.edges.delete(res[0]);
                    }
                    deferred.resolve(edge);
                }
            } else {
                ruleChainService.getRuleNodeSupportedLinks(sourceNode.type).then(
                    (labels) => {
                        addRuleNodeLink(event, edge, labels).then(
                            (link) => {
                                deferred.resolve(link);
                            },
                            () => {
                                deferred.reject();
                            }
                        );
                    },
                    () => {
                        deferred.reject();
                    }
                );
            }
            return deferred.promise;
        },
        dropNode: function (event, node) {
            addRuleNode(event, node);
        }
    };

    loadRuleChainLibrary();

    function loadRuleChainLibrary() {
        ruleChainService.getRuleNodeTypes().then(
            (ruleNodeTypes) => {
                for (var i=0;i<ruleNodeTypes.length;i++) {
                    var ruleNodeType = ruleNodeTypes[i];
                    var nodeType = ruleNodeType.nodeType;
                    var model = vm.ruleNodeTypesModel[nodeType].model;
                    var node = {
                        id: model.nodes.length,
                        nodeType: nodeType,
                        type: ruleNodeType.type,
                        name: '',
                        nodeClass: vm.types.ruleNodeType[nodeType].nodeClass,
                        icon: vm.types.ruleNodeType[nodeType].icon,
                        x: 30,
                        y: 10+50*model.nodes.length,
                        connectors: []
                    };
                    if (nodeType == types.ruleNodeType.RULE_CHAIN.value) {
                        node.connectors.push(
                            {
                                type: flowchartConstants.leftConnectorType,
                                id: model.nodes.length
                            }
                        );
                    } else {
                        node.connectors.push(
                            {
                                type: flowchartConstants.leftConnectorType,
                                id: model.nodes.length*2
                            }
                        );
                        node.connectors.push(
                            {
                                type: flowchartConstants.rightConnectorType,
                                id: model.nodes.length*2+1
                            }
                        );
                    }
                    model.nodes.push(node);
                }
                prepareRuleChain();
            }
        );
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
                id: vm.nextNodeID++,
                type: "Input",
                name: "",
                nodeType: types.ruleNodeType.INPUT.value,
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
            var nodeType = ruleChainService.getRuleNodeComponentType(ruleNode.type);
            if (nodeType) {
                var node = {
                    id: vm.nextNodeID++,
                    ruleNodeId: ruleNode.id,
                    additionalInfo: ruleNode.additionalInfo,
                    configuration: ruleNode.configuration,
                    x: ruleNode.additionalInfo.layoutX,
                    y: ruleNode.additionalInfo.layoutY,
                    type: ruleNode.type,
                    name: ruleNode.name,
                    nodeType: nodeType,
                    nodeClass: vm.types.ruleNodeType[nodeType].nodeClass,
                    icon: vm.types.ruleNodeType[nodeType].icon,
                    connectors: [
                        {
                            type: flowchartConstants.leftConnectorType,
                            id: vm.nextConnectorID++
                        },
                        {
                            type: flowchartConstants.rightConnectorType,
                            id: vm.nextConnectorID++
                        }
                    ]
                };
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
            for (i = 0; i < vm.ruleChainMetaData.connections.length; i++) {
                var connection = vm.ruleChainMetaData.connections[0];
                var sourceNode = nodes[connection.fromIndex];
                destNode = nodes[connection.toIndex];
                if (sourceNode && destNode) {
                    var sourceConnectors = vm.modelservice.nodes.getConnectorsByType(sourceNode, flowchartConstants.rightConnectorType);
                    var destConnectors = vm.modelservice.nodes.getConnectorsByType(destNode, flowchartConstants.leftConnectorType);
                    if (sourceConnectors && sourceConnectors.length && destConnectors && destConnectors.length) {
                        edge = {
                            source: sourceConnectors[0].id,
                            destination: destConnectors[0].id,
                            label: connection.type
                        };
                        vm.ruleChainModel.edges.push(edge);
                    }
                }
            }
        }

        if (vm.ruleChainMetaData.ruleChainConnections) {
            var ruleChainNodesMap = {};
            for (i = 0; i < vm.ruleChainMetaData.ruleChainConnections.length; i++) {
                var ruleChainConnection = vm.ruleChainMetaData.ruleChainConnections[i];
                var ruleChain = ruleChainsMap[ruleChainConnection.targetRuleChainId.id];
                if (ruleChainConnection.additionalInfo && ruleChainConnection.additionalInfo.ruleChainNodeId) {
                    var ruleChainNode = ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId];
                    if (!ruleChainNode) {
                        ruleChainNode = {
                            id: vm.nextNodeID++,
                            additionalInfo: ruleChainConnection.additionalInfo,
                            targetRuleChainId: ruleChainConnection.targetRuleChainId.id,
                            x: ruleChainConnection.additionalInfo.layoutX,
                            y: ruleChainConnection.additionalInfo.layoutY,
                            type: 'Rule chain',
                            name: ruleChain.name,
                            nodeType: vm.types.ruleNodeType.RULE_CHAIN.value,
                            nodeClass: vm.types.ruleNodeType.RULE_CHAIN.nodeClass,
                            icon: vm.types.ruleNodeType.RULE_CHAIN.icon,
                            connectors: [
                                {
                                    type: flowchartConstants.leftConnectorType,
                                    id: vm.nextConnectorID++
                                }
                            ]
                        };
                        ruleChainNodesMap[ruleChainConnection.additionalInfo.ruleChainNodeId] = ruleChainNode;
                        vm.ruleChainModel.nodes.push(ruleChainNode);
                    }
                    sourceNode = nodes[ruleChainConnection.fromIndex];
                    if (sourceNode) {
                        connectors = vm.modelservice.nodes.getConnectorsByType(sourceNode, flowchartConstants.rightConnectorType);
                        if (connectors && connectors.length) {
                            var ruleChainEdge = {
                                source: connectors[0].id,
                                destination: ruleChainNode.connectors[0].id,
                                label: ruleChainConnection.type
                            };
                            vm.ruleChainModel.edges.push(ruleChainEdge);
                        }
                    }
                }
            }
        }

        vm.canvasControl.adjustCanvasSize();

        vm.isDirty = false;

        $mdUtil.nextTick(() => {
            vm.ruleChainWatch = $scope.$watch('vm.ruleChainModel',
                function (newVal, oldVal) {
                    if (!vm.isDirty && !angular.equals(newVal, oldVal)) {
                        vm.isDirty = true;
                    }
                }, true
            );
        });
    }

    function saveRuleChain() {
        var ruleChainMetaData = {
            ruleChainId: vm.ruleChain.id,
            nodes: [],
            connections: [],
            ruleChainConnections: []
        };

        var nodes = [];

        for (var i=0;i<vm.ruleChainModel.nodes.length;i++) {
            var node = vm.ruleChainModel.nodes[i];
            if (node.nodeType != types.ruleNodeType.INPUT.value && node.nodeType != types.ruleNodeType.RULE_CHAIN.value) {
                var ruleNode = {};
                if (node.ruleNodeId) {
                    ruleNode.id = node.ruleNodeId;
                }
                ruleNode.type = node.type;
                ruleNode.name = node.name;
                ruleNode.configuration = node.configuration;
                ruleNode.additionalInfo = node.additionalInfo;
                if (!ruleNode.additionalInfo) {
                    ruleNode.additionalInfo = {};
                }
                ruleNode.additionalInfo.layoutX = node.x;
                ruleNode.additionalInfo.layoutY = node.y;
                ruleChainMetaData.nodes.push(ruleNode);
                nodes.push(node);
            }
        }
        var res = $filter('filter')(vm.ruleChainModel.edges, {source: vm.inputConnectorId});
        if (res && res.length) {
            var firstNodeEdge = res[0];
            var firstNode = vm.modelservice.nodes.getNodeByConnectorId(firstNodeEdge.destination);
            ruleChainMetaData.firstNodeIndex = nodes.indexOf(firstNode);
        }
        for (i=0;i<vm.ruleChainModel.edges.length;i++) {
            var edge = vm.ruleChainModel.edges[i];
            var sourceNode = vm.modelservice.nodes.getNodeByConnectorId(edge.source);
            var destNode = vm.modelservice.nodes.getNodeByConnectorId(edge.destination);
            if (sourceNode.nodeType != types.ruleNodeType.INPUT.value) {
                var fromIndex = nodes.indexOf(sourceNode);
                if (destNode.nodeType == types.ruleNodeType.RULE_CHAIN.value) {
                    var ruleChainConnection = {
                        fromIndex: fromIndex,
                        targetRuleChainId: {entityType: vm.types.entityType.rulechain, id: destNode.targetRuleChainId},
                        additionalInfo: destNode.additionalInfo,
                        type: edge.label
                    };
                    if (!ruleChainConnection.additionalInfo) {
                        ruleChainConnection.additionalInfo = {};
                    }
                    ruleChainConnection.additionalInfo.layoutX = destNode.x;
                    ruleChainConnection.additionalInfo.layoutY = destNode.y;
                    ruleChainConnection.additionalInfo.ruleChainNodeId = destNode.id;
                    ruleChainMetaData.ruleChainConnections.push(ruleChainConnection);
                } else {
                    var toIndex = nodes.indexOf(destNode);
                    var nodeConnection = {
                        fromIndex: fromIndex,
                        toIndex: toIndex,
                        type: edge.label
                    };
                    ruleChainMetaData.connections.push(nodeConnection);
                }
            }
        }
        ruleChainService.saveRuleChainMetaData(ruleChainMetaData).then(
            (ruleChainMetaData) => {
                vm.ruleChainMetaData = ruleChainMetaData;
                prepareRuleChain();
            }
        );
    }

    function revertRuleChain() {
        prepareRuleChain();
    }

    function addRuleNode($event, ruleNode) {
        $mdDialog.show({
            controller: 'AddRuleNodeController',
            controllerAs: 'vm',
            templateUrl: addRuleNodeTemplate,
            parent: angular.element($document[0].body),
            locals: {ruleNode: ruleNode, ruleChainId: vm.ruleChain.id.id},
            fullscreen: true,
            targetEvent: $event
        }).then(function (ruleNode) {
            ruleNode.id = vm.nextNodeID++;
            ruleNode.connectors = [];
            ruleNode.connectors.push(
                {
                    id: vm.nextConnectorID++,
                    type: flowchartConstants.leftConnectorType
                }
            );
            if (ruleNode.nodeType != types.ruleNodeType.RULE_CHAIN.value) {
                ruleNode.connectors.push(
                    {
                        id: vm.nextConnectorID++,
                        type: flowchartConstants.rightConnectorType
                    }
                );
            }
            vm.ruleChainModel.nodes.push(ruleNode);
        }, function () {
        });
    }

    function addRuleNodeLink($event, link, labels) {
        return $mdDialog.show({
            controller: 'AddRuleNodeLinkController',
            controllerAs: 'vm',
            templateUrl: addRuleNodeLinkTemplate,
            parent: angular.element($document[0].body),
            locals: {link: link, labels: labels},
            fullscreen: true,
            targetEvent: $event
        });
    }

}

/*@ngInject*/
export function AddRuleNodeController($scope, $mdDialog, ruleNode, ruleChainId, helpLinks) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.ruleNode = ruleNode;
    vm.ruleChainId = ruleChainId;

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
export function AddRuleNodeLinkController($scope, $mdDialog, link, labels, helpLinks) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.link = link;
    vm.labels = labels;

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
