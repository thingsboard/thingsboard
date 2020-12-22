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
/* eslint-disable import/no-unresolved, import/default */

import addEdgeTemplate from './add-edge.tpl.html';
import edgeCard from './edge-card.tpl.html';
import assignToCustomerTemplate from './assign-to-customer.tpl.html';
import addEdgesToCustomerTemplate from './add-edges-to-customer.tpl.html';
import setRootRuleChainToEdgesTemplate from './set-root-rule-chain-to-edges.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function EdgeCardController(types) {

    var vm = this;

    vm.types = types;

    vm.isAssignedToCustomer = function() {
        if (vm.item && vm.item.customerId && vm.parentCtl.edgesScope === 'tenant' &&
            vm.item.customerId.id != vm.types.id.nullUid && !vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }

    vm.isPublic = function() {
        if (vm.item && vm.item.assignedCustomer && vm.parentCtl.edgesScope === 'tenant' && vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }
}


/*@ngInject*/
export function EdgeController($rootScope, userService, edgeService, customerService, ruleChainService,
                               $state, $stateParams, $document, $mdDialog, $q, $translate, types, importExport) {

    var customerId = $stateParams.customerId;

    var edgeActionsList = [];

    var edgeGroupActionsList = [];

    var edgeAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.add') },
            details: function() { return $translate.instant('edge.add-edge-text') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importEntities($event, types.entityType.edge).then(
                    function() {
                        vm.grid.refreshList();
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('edge.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.edgeGridConfig = {
        deleteItemTitleFunc: deleteEdgeTitle,
        deleteItemContentFunc: deleteEdgeText,
        deleteItemsTitleFunc: deleteEdgesTitle,
        deleteItemsActionTitleFunc: deleteEdgesActionTitle,
        deleteItemsContentFunc: deleteEdgesText,

        saveItemFunc: saveEdge,

        getItemTitleFunc: getEdgeTitle,

        itemCardController: 'EdgeCardController',
        itemCardTemplateUrl: edgeCard,
        parentCtl: vm,

        actionsList: edgeActionsList,
        groupActionsList: edgeGroupActionsList,
        addItemActions: edgeAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addEdgeTemplate,

        addItemText: function() { return $translate.instant('edge.add-edge-text') },
        noItemsText: function() { return $translate.instant('edge.no-edges-text') },
        itemDetailsText: function() { return $translate.instant('edge.edge-details') },
        isDetailsReadOnly: isCustomerUser,
        isSelectionEnabled: function () {
            return !isCustomerUser();
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.edgeGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.edgeGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.edgesScope = $state.$current.data.edgesType;

    vm.assignToCustomer = assignToCustomer;
    vm.makePublic = makePublic;
    vm.unassignFromCustomer = unassignFromCustomer;
    vm.openEdgeAssets = openEdgeAssets;
    vm.openEdgeDevices = openEdgeDevices;
    vm.openEdgeEntityViews = openEdgeEntityViews;
    vm.openEdgeDashboards = openEdgeDashboards;
    vm.openEdgeRuleChains = openEdgeRuleChains;

    initController();

    function initController() {
        var fetchEdgesFunction = null;
        var deleteEdgeFunction = null;
        var refreshEdgesParamsFunction = null;

        var user = userService.getCurrentUser();

        if (user.authority === 'CUSTOMER_USER') {
            vm.edgesScope = 'customer_user';
            customerId = user.customerId;
        }
        if (customerId) {
            vm.customerEdgesTitle = $translate.instant('customer.edges');
            customerService.getShortCustomerInfo(customerId).then(
                function success(info) {
                    if (info.isPublic) {
                        vm.customerEdgesTitle = $translate.instant('customer.public-edges');
                    }
                }
            );
        }

        if (vm.edgesScope === 'tenant') {
            fetchEdgesFunction = function (pageLink, edgeType) {
                return edgeService.getTenantEdges(pageLink, true, edgeType, null);
            };
            deleteEdgeFunction = function (edgeId) {
                return edgeService.deleteEdge(edgeId);
            };
            refreshEdgesParamsFunction = function() {
                return {"topIndex": vm.topIndex};
            };

            edgeActionsList.push({
                onAction: function ($event, item) {
                    makePublic($event, item);
                },
                name: function() { return $translate.instant('action.share') },
                details: function() { return $translate.instant('edge.make-public') },
                icon: "share",
                isEnabled: function(edge) {
                    return edge && (!edge.customerId || edge.customerId.id === types.id.nullUid);
                }
            });

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        assignToCustomer($event, [ item.id.id ]);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('edge.assign-to-customer') },
                    icon: "assignment_ind",
                    isEnabled: function(edge) {
                        return edge && (!edge.customerId || edge.customerId.id === types.id.nullUid);
                    }
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromCustomer($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('edge.unassign-from-customer') },
                    icon: "assignment_return",
                    isEnabled: function(edge) {
                        return edge && edge.customerId && edge.customerId.id !== types.id.nullUid && !edge.assignedCustomer.isPublic;
                    }
                }
            );

            edgeActionsList.push({
                onAction: function ($event, item) {
                    unassignFromCustomer($event, item, true);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('edge.make-private') },
                icon: "reply",
                isEnabled: function(edge) {
                    return edge && edge.customerId && edge.customerId.id !== types.id.nullUid && edge.assignedCustomer.isPublic;
                }
            });

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        openEdgeAssets($event, item);
                    },
                    name: function() { return $translate.instant('asset.assets') },
                    details: function() {
                        return $translate.instant('edge.manage-edge-assets');
                    },
                    icon: "domain"
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        openEdgeDevices($event, item);
                    },
                    name: function() { return $translate.instant('device.devices') },
                    details: function() {
                        return $translate.instant('edge.manage-edge-devices');
                    },
                    icon: "devices_other"
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        openEdgeEntityViews($event, item);
                    },
                    name: function() { return $translate.instant('entity-view.entity-views') },
                    details: function() {
                        return $translate.instant('edge.manage-edge-entity-views');
                    },
                    icon: "view_quilt"
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        openEdgeDashboards($event, item);
                    },
                    name: function() { return $translate.instant('dashboard.dashboards') },
                    details: function() {
                        return $translate.instant('edge.manage-edge-dashboards');
                    },
                    icon: "dashboard"
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        openEdgeRuleChains($event, item);
                    },
                    name: function() { return $translate.instant('rulechain.rulechains') },
                    details: function() {
                        return $translate.instant('edge.manage-edge-rulechains');
                    },
                    icon: "settings_ethernet"
                }
            );

            edgeActionsList.push(
                {
                    onAction: function ($event, item) {
                        vm.grid.deleteItem($event, item);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: function() { return $translate.instant('edge.delete') },
                    icon: "delete"
                }
            );

            edgeGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        setRootRuleChainToEdges($event, items);
                    },
                    name: function() { return $translate.instant('edge.set-root-rule-chain-to-edges') },
                    details: function(selectedCount) {
                        return $translate.instant('edge.set-root-rule-chain-to-edges-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "flag"
                }
            );

            edgeGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        assignEdgesToCustomer($event, items);
                    },
                    name: function() { return $translate.instant('edge.assign-edges') },
                    details: function(selectedCount) {
                        return $translate.instant('edge.assign-edges-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_ind"
                }
            );

            edgeGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('edge.delete-edges') },
                    details: deleteEdgesActionTitle,
                    icon: "delete"
                }
            );

       } else if (vm.edgesScope === 'customer' || vm.edgesScope === 'customer_user') {
            fetchEdgesFunction = function (pageLink, edgeType) {
                return edgeService.getCustomerEdges(customerId, pageLink, true, edgeType, null);
            };
            deleteEdgeFunction = function (edgeId) {
                return edgeService.unassignEdgeFromCustomer(edgeId);
            };
            refreshEdgesParamsFunction = function () {
                return {"customerId": customerId, "topIndex": vm.topIndex};
            };

            if (vm.edgesScope === 'customer') {
                edgeActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, false);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('edge.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(edge) {
                            return edge && !edge.assignedCustomer.isPublic;
                        }
                    }
                );
                edgeActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, true);
                        },
                        name: function() { return $translate.instant('action.make-private') },
                        details: function() { return $translate.instant('edge.make-private') },
                        icon: "reply",
                        isEnabled: function(edge) {
                            return edge && edge.assignedCustomer.isPublic;
                        }
                    }
                );

                edgeGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignEdgesFromCustomer($event, items);
                        },
                        name: function() { return $translate.instant('edge.unassign-edges') },
                        details: function(selectedCount) {
                            return $translate.instant('edge.unassign-edges-action-title', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_return"
                    }
                );

                vm.edgeGridConfig.addItemAction = {
                    onAction: function ($event) {
                        addEdgesToCustomer($event);
                    },
                    name: function() { return $translate.instant('edge.assign-edges') },
                    details: function() { return $translate.instant('edge.assign-new-edge') },
                    icon: "add"
                };


            } else if (vm.edgesScope === 'customer_user') {
                vm.edgeGridConfig.addItemAction = {};
            }
            vm.edgeGridConfig.addItemActions = [];

        }

        vm.edgeGridConfig.refreshParamsFunc = refreshEdgesParamsFunction;
        vm.edgeGridConfig.fetchItemsFunc = fetchEdgesFunction;
        vm.edgeGridConfig.deleteItemFunc = deleteEdgeFunction;

    }

    function deleteEdgeTitle(edge) {
        return $translate.instant('edge.delete-edge-title', {edgeName: edge.name});
    }

    function deleteEdgeText() {
        return $translate.instant('edge.delete-edge-text');
    }

    function deleteEdgesTitle(selectedCount) {
        return $translate.instant('edge.delete-edges-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEdgesActionTitle(selectedCount) {
        return $translate.instant('edge.delete-edges-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEdgesText () {
        return $translate.instant('edge.delete-edges-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getEdgeTitle(edge) {
        return edge ? edge.name : '';
    }

    function saveEdge(edge) {
        var deferred = $q.defer();
        edgeService.saveEdge(edge).then(
            function success(savedEdge) {
                $rootScope.$broadcast('edgeSaved');
                var edges = [ savedEdge ];
                customerService.applyAssignedCustomersInfo(edges).then(
                    function success(items) {
                        if (items && items.length == 1) {
                            deferred.resolve(items[0]);
                        } else {
                            deferred.reject();
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function isCustomerUser() {
        return vm.edgesScope === 'customer_user';
    }

    function assignToCustomer($event, edgeIds) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        customerService.getCustomers({limit: pageSize, textSearch: ''}).then(
            function success(_customers) {
                var customers = {
                    pageSize: pageSize,
                    data: _customers.data,
                    nextPageLink: _customers.nextPageLink,
                    selection: null,
                    hasNext: _customers.hasNext,
                    pending: false
                };
                if (customers.hasNext) {
                    customers.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AssignEdgeToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: assignToCustomerTemplate,
                    locals: {edgeIds: edgeIds, customers: customers},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    vm.grid.refreshList();
                }, function () {
                });
            },
            function fail() {
            });
    }

    function addEdgesToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        edgeService.getTenantEdges({limit: pageSize, textSearch: ''}, false).then(
            function success(_edges) {
                var edges = {
                    pageSize: pageSize,
                    data: _edges.data,
                    nextPageLink: _edges.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _edges.hasNext,
                    pending: false
                };
                if (edges.hasNext) {
                    edges.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddEdgesToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: addEdgesToCustomerTemplate,
                    locals: {customerId: customerId, edges: edges},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    vm.grid.refreshList();
                }, function () {
                });
            },
            function fail() {
            });
    }

    function setRootRuleChainToEdges($event, items) {
        var edgeIds = [];
        for (var id in items.selections) {
            edgeIds.push(id);
        }
        setRootRuleChain($event, edgeIds);
    }

    function setRootRuleChain($event, edgeIds) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        ruleChainService.getEdgesRuleChains({limit: pageSize, textSearch: ''}).then(
            function success(_ruleChains) {
                var ruleChains = {
                    pageSize: pageSize,
                    data: _ruleChains.data,
                    nextPageLink: _ruleChains.nextPageLink,
                    selection: null,
                    hasNext: _ruleChains.hasNext,
                    pending: false
                };
                if (ruleChains.hasNext) {
                    ruleChains.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'SetRootRuleChainToEdgesController',
                    controllerAs: 'vm',
                    templateUrl: setRootRuleChainToEdgesTemplate,
                    locals: {edgeIds: edgeIds, ruleChains: ruleChains},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    vm.grid.refreshList();
                }, function () {
                });
            },
            function fail() {
            });
    }

    function assignEdgesToCustomer($event, items) {
        var edgeIds = [];
        for (var id in items.selections) {
            edgeIds.push(id);
        }
        assignToCustomer($event, edgeIds);
    }

    function unassignFromCustomer($event, edge, isPublic) {
        if ($event) {
            $event.stopPropagation();
        }
        var title;
        var content;
        var label;
        if (isPublic) {
            title = $translate.instant('edge.make-private-edge-title', {edgeName: edge.name});
            content = $translate.instant('edge.make-private-edge-text');
            label = $translate.instant('edge.make-private');
        } else {
            title = $translate.instant('edge.unassign-edge-title', {edgeName: edge.name});
            content = $translate.instant('edge.unassign-edge-text');
            label = $translate.instant('edge.unassign-edge');
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            edgeService.unassignEdgeFromCustomer(edge.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function unassignEdgesFromCustomer($event, items) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('edge.unassign-edges-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('edge.unassign-edges-text'))
            .ariaLabel($translate.instant('edge.unassign-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(edgeService.unassignEdgeFromCustomer(id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function makePublic($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('edge.make-public-edge-title', {edgeName: edge.name}))
            .htmlContent($translate.instant('edge.make-public-edge-text'))
            .ariaLabel($translate.instant('edge.make-public'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            edgeService.makeEdgePublic(edge.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function openEdgeDashboards($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.edges.dashboards', {edgeId: edge.id.id});
    }

    function openEdgeRuleChains($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.edges.ruleChains', {edgeId: edge.id.id});
    }

    function openEdgeAssets($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.edges.assets', {edgeId: edge.id.id});
    }

    function openEdgeDevices($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.edges.devices', {edgeId: edge.id.id});
    }

    function openEdgeEntityViews($event, edge) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.edges.entityViews', {edgeId: edge.id.id});
    }

}
