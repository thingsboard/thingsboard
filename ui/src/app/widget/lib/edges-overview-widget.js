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
import './edges-overview-widget.scss';

/* eslint-disable import/no-unresolved, import/default */
import edgesOverviewWidgetTemplate from './edges-overview-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.edgesOverviewWidget', [])
    .directive('tbEdgesOverviewWidget', EdgesOverviewWidget)
    .name;
/* eslint-disable no-unused-vars, no-undef */
/*@ngInject*/
function EdgesOverviewWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: EdgesOverviewWidgetController,
        controllerAs: 'vm',
        templateUrl: edgesOverviewWidgetTemplate
    };
}

/*@ngInject*/
function EdgesOverviewWidgetController($scope, $translate, types, utils, entityService, edgeService, customerService, userService) {
    var vm = this;

    vm.showData = true;

    vm.nodeIdCounter = 0;

    vm.entityNodesMap = {};
    vm.entityGroupsNodesMap = {};

    vm.customerTitle = null;
    vm.edgeIsDatasource = true;

    var edgeGroupsTypes = [
        types.entityType.asset,
        types.entityType.device,
        types.entityType.entityView,
        types.entityType.dashboard,
        types.entityType.rulechain,
    ]

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.subscription = vm.ctx.defaultSubscription;
            vm.datasources = vm.subscription.datasources;
            updateDatasources();
        }
    });

    function updateDatasources() {
        vm.loadNodes = loadNodes;
    }

    function loadNodes(node, cb) {
        var datasource = vm.datasources[0];
        if (node.id === '#' && datasource) {
            if (datasource.type === types.datasourceType.entity && datasource.entity && datasource.entity.id.entityType === types.entityType.edge) {
                var selectedEdge = datasource.entity;
                vm.customerTitle = getCustomerTitle(selectedEdge.id.id);
                vm.ctx.widgetTitle = selectedEdge.name;
                cb(loadNodesForEdge(selectedEdge.id.id, selectedEdge));
            } else if (datasource.type === types.datasourceType.function) {
                cb(loadNodesForEdge(null, null));
            } else {
                vm.edgeIsDatasource = false;
                cb([]);
            }
        } else if (node.data && node.data.entity && node.data.entity.id.entityType === types.entityType.edge) {
            var edgeId = node.data.entity.id.id;
            var entityType = node.data.entityType;
            var pageLink = {limit: 100};
            entityService.getAssignedToEdgeEntitiesByType(edgeId, entityType, pageLink).then(
                (entities) => {
                    if (entities.data.length > 0) {
                        cb(entitiesToNodes(node.id, entities.data));
                    } else {
                        cb([]);
                    }
                }
            )
        } else {
            cb([]);
        }
    }

    function entitiesToNodes(parentNodeId, entities) {
        var nodes = [];
        vm.entityNodesMap[parentNodeId] = {};
        if (entities) {
            entities.forEach(
                (entity) => {
                    var node = createEntityNode(parentNodeId, entity, entity.id.entityType);
                    nodes.push(node);
                }
            );
        }
        return nodes;
    }

    function createEntityNode(parentNodeId, entity, entityType) {
        var nodesMap = vm.entityNodesMap[parentNodeId];
        if (!nodesMap) {
            nodesMap = {};
            vm.entityNodesMap[parentNodeId] = nodesMap;
        }
        var node = {
            id: ++vm.nodeIdCounter,
            icon: 'material-icons ' + iconForGroupType(entityType),
            text: entity.name,
            children: false,
            data: {
                entity,
                internalId: entity.id.id
            }
        };
        nodesMap[entity.id.id] = node.id;
        return node;
    }

    function loadNodesForEdge(parentNodeId, entity) {
        var nodes = [];
        vm.entityGroupsNodesMap[parentNodeId] = {};
        var allowedGroupTypes = edgeGroupsTypes;
        if (userService.getAuthority() === 'CUSTOMER_USER') {
            allowedGroupTypes = edgeGroupsTypes.filter(type => type !== types.entityType.rulechain);
        }
        allowedGroupTypes.forEach(
            (entityType) => {
                var node = {
                    id: ++vm.nodeIdCounter,
                    icon: 'material-icons ' + iconForGroupType(entityType),
                    text: textForGroupType(entityType),
                    children: true,
                    data: {
                        entityType,
                        entity,
                        internalId: entity ? entity.id.id + '_' + entityType : utils.guid()
                    }
                };
                nodes.push(node);
            }
        )
        return nodes;
    }

    function iconForGroupType(entityType) {
        switch (entityType) {
            case types.entityType.asset:
                return 'tb-asset-group';
            case types.entityType.device:
                return 'tb-device-group';
            case types.entityType.entityView:
                return 'tb-entity-view-group';
            case types.entityType.dashboard:
                return 'tb-dashboard-group';
            case types.entityType.rulechain:
                return 'tb-rule-chain-group';
        }
        return '';
    }

    function textForGroupType(entityType) {
        switch (entityType) {
            case types.entityType.asset:
                return $translate.instant('asset.assets');
            case types.entityType.device:
                return $translate.instant('device.devices');
            case types.entityType.entityView:
                return $translate.instant('entity-view.entity-views');
            case types.entityType.rulechain:
                return $translate.instant('rulechain.rulechains');
            case types.entityType.dashboard:
                return $translate.instant('dashboard.dashboards');
        }
        return '';
    }

    function getCustomerTitle(edgeId) {
        edgeService.getEdge(edgeId, true).then(
            function success(edge) {
                if (edge.customerId.id !== types.id.nullUid) {
                    customerService.getCustomer(edge.customerId.id, { ignoreErrors: true }).then(
                        function success(customer) {
                            vm.customerTitle = $translate.instant('edge.assigned-to-customer-widget', { customerTitle: customer.title });
                        },
                        function fail() {
                        }
                    );
                }
            },
            function fail() {
            }
        )
    }

}
