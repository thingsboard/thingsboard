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

import addDashboardTemplate from './add-dashboard.tpl.html';
import dashboardCard from './dashboard-card.tpl.html';
import addDashboardsToCustomerTemplate from './add-dashboards-to-customer.tpl.html';
import makeDashboardPublicDialogTemplate from './make-dashboard-public-dialog.tpl.html';
import manageAssignedCustomersTemplate from './manage-assigned-customers.tpl.html';
import addDashboardsToEdgeTemplate from './add-dashboards-to-edge.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './dashboard-card.scss';

/*@ngInject*/
export function MakeDashboardPublicDialogController($mdDialog, $translate, toast, dashboardService, dashboard) {

    var vm = this;

    vm.dashboard = dashboard;
    vm.publicLink = dashboardService.getPublicDashboardLink(dashboard);

    vm.onPublicLinkCopied = onPublicLinkCopied;
    vm.close = close;

    function onPublicLinkCopied(){
        toast.showSuccess($translate.instant('dashboard.public-link-copied-message'), 750, angular.element('#make-dialog-public-content'), 'bottom left');
    }

    function close() {
        $mdDialog.hide();
    }

}

/*@ngInject*/
export function DashboardCardController(types) {

    var vm = this;
    vm.types = types;

}

/*@ngInject*/
export function DashboardsController(userService, dashboardService, customerService, importExport, edgeService,
                                             $state, $stateParams, $mdDialog, $document, $q, $translate, types) {

    var customerId = $stateParams.customerId;
    var edgeId = $stateParams.edgeId;

    var dashboardActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('dashboard.details') },
            details: function() { return $translate.instant('dashboard.dashboard-details') },
            icon: "edit"
        }
    ];

    var dashboardGroupActionsList = [];

    var vm = this;
    vm.types = types;

    vm.dashboardGridConfig = {
        deleteItemTitleFunc: deleteDashboardTitle,
        deleteItemContentFunc: deleteDashboardText,
        deleteItemsTitleFunc: deleteDashboardsTitle,
        deleteItemsActionTitleFunc: deleteDashboardsActionTitle,
        deleteItemsContentFunc: deleteDashboardsText,

        loadItemDetailsFunc: loadDashboard,

        saveItemFunc: saveDashboard,

        clickItemFunc: openDashboard,

        getItemTitleFunc: getDashboardTitle,
        itemCardController: 'DashboardCardController',
        itemCardTemplateUrl: dashboardCard,
        parentCtl: vm,

        actionsList: dashboardActionsList,
        groupActionsList: dashboardGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addDashboardTemplate,

        addItemText: function() { return $translate.instant('dashboard.add-dashboard-text') },
        noItemsText: function() { return $translate.instant('dashboard.no-dashboards-text') },
        itemDetailsText: function() { return $translate.instant('dashboard.dashboard-details') },
        isDetailsReadOnly: function () {
            return vm.dashboardsScope === 'customer_user';
        },
        isSelectionEnabled: function () {
            return !(vm.dashboardsScope === 'customer_user');
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.dashboardGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.dashboardGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.dashboardsScope = $state.$current.data.dashboardsType;

    vm.makePublic = makePublic;
    vm.makePrivate = makePrivate;
    vm.manageAssignedCustomers = manageAssignedCustomers;
    vm.unassignFromCustomer = unassignFromCustomer;
    vm.exportDashboard = exportDashboard;

    initController();

    function initController() {
        var fetchDashboardsFunction = null;
        var deleteDashboardFunction = null;
        var refreshDashboardsParamsFunction = null;

        var user = userService.getCurrentUser();

        if (user.authority === 'CUSTOMER_USER') {
            vm.dashboardsScope = 'customer_user';
            customerId = user.customerId;
        }

        if (customerId) {
            vm.customerId = customerId;
            vm.customerDashboardsTitle = $translate.instant('customer.dashboards');
            customerService.getShortCustomerInfo(customerId).then(
                function success(info) {
                    if (info.isPublic) {
                        vm.customerDashboardsTitle = $translate.instant('customer.public-dashboards');
                    }
                }
            );
        }

        if (edgeId) {
            vm.edgeDashboardsTitle = $translate.instant('edge.dashboards');
            edgeService.getEdge(edgeId).then(
                function success(edge) {
                    if (edge.customerId) {
                        vm.edgeCustomerId = edge.customerId;
                    }
                }
            )
        }

        if (vm.dashboardsScope === 'tenant') {
            fetchDashboardsFunction = function (pageLink) {
                return dashboardService.getTenantDashboards(pageLink);
            };
            deleteDashboardFunction = function (dashboardId) {
                return dashboardService.deleteDashboard(dashboardId);
            };
            refreshDashboardsParamsFunction = function () {
                return {"topIndex": vm.topIndex};
            };

            dashboardActionsList.push(
                {
                    onAction: function ($event, item) {
                        exportDashboard($event, item);
                    },
                    name: function() { $translate.instant('action.export') },
                    details: function() { return $translate.instant('dashboard.export') },
                    icon: "file_download"
                });

            dashboardActionsList.push({
                    onAction: function ($event, item) {
                        makePublic($event, item);
                    },
                    name: function() { return $translate.instant('action.share') },
                    details: function() { return $translate.instant('dashboard.make-public') },
                    icon: "share",
                    isEnabled: function(dashboard) {
                        return dashboard && !dashboard.publicCustomerId;
                    }
                });
            dashboardActionsList.push({
                onAction: function ($event, item) {
                    makePrivate($event, item);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('dashboard.make-private') },
                icon: "reply",
                isEnabled: function(dashboard) {
                    return dashboard && dashboard.publicCustomerId;
                }
            });
            dashboardActionsList.push({
                onAction: function ($event, item) {
                    manageAssignedCustomers($event, item);
                },
                name: function() { return $translate.instant('action.assign') },
                details: function() { return $translate.instant('dashboard.manage-assigned-customers') },
                icon: "assignment_ind",
                isEnabled: function(dashboard) {
                    return dashboard;
                }
            });

            /*dashboardActionsList.push({
                    onAction: function ($event, item) {
                        assignToCustomer($event, [ item.id.id ]);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('dashboard.assign-to-customer') },
                    icon: "assignment_ind",
                    isEnabled: function(dashboard) {
                        return dashboard && (!dashboard.customerId || dashboard.customerId.id === types.id.nullUid);
                    }
                });*/
            /*dashboardActionsList.push({
                    onAction: function ($event, item) {
                        unassignFromCustomer($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('dashboard.unassign-from-customer') },
                    icon: "assignment_return",
                    isEnabled: function(dashboard) {
                        return dashboard && dashboard.customerId && dashboard.customerId.id !== types.id.nullUid && !dashboard.assignedCustomer.isPublic;
                    }
                });*/

            dashboardActionsList.push(
                {
                    onAction: function ($event, item) {
                        vm.grid.deleteItem($event, item);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: function() { return $translate.instant('dashboard.delete') },
                    icon: "delete"
                }
            );

            dashboardGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            assignDashboardsToCustomers($event, items);
                        },
                        name: function() { return $translate.instant('dashboard.assign-dashboards') },
                        details: function(selectedCount) {
                            return $translate.instant('dashboard.assign-dashboards-text', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_ind"
                    }
            );
            dashboardGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        unassignDashboardsFromCustomers($event, items);
                    },
                    name: function() { return $translate.instant('dashboard.unassign-dashboards') },
                    details: function(selectedCount) {
                        return $translate.instant('dashboard.unassign-dashboards-action-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_return"                }
            );

            dashboardGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('dashboard.delete-dashboards') },
                    details: deleteDashboardsActionTitle,
                    icon: "delete"
                }
            );

            vm.dashboardGridConfig.addItemActions = [];
            vm.dashboardGridConfig.addItemActions.push({
                onAction: function ($event) {
                    vm.grid.addItem($event);
                },
                name: function() { return $translate.instant('action.create') },
                details: function() { return $translate.instant('dashboard.create-new-dashboard') },
                icon: "insert_drive_file"
            });
            vm.dashboardGridConfig.addItemActions.push({
                onAction: function ($event) {
                    importExport.importDashboard($event).then(
                        function() {
                            vm.grid.refreshList();
                        }
                    );
                },
                name: function() { return $translate.instant('action.import') },
                details: function() { return $translate.instant('dashboard.import') },
                icon: "file_upload"
            });
        } else if (vm.dashboardsScope === 'customer' || vm.dashboardsScope === 'customer_user') {
            fetchDashboardsFunction = function (pageLink) {
                return dashboardService.getCustomerDashboards(customerId, pageLink);
            };
            deleteDashboardFunction = function (dashboardId) {
                return dashboardService.unassignDashboardFromCustomer(customerId, dashboardId);
            };
            refreshDashboardsParamsFunction = function () {
                return {"customerId": customerId, "topIndex": vm.topIndex};
            };

            if (vm.dashboardsScope === 'customer') {
                dashboardActionsList.push(
                    {
                        onAction: function ($event, item) {
                            exportDashboard($event, item);
                        },
                        name: function() { $translate.instant('action.export') },
                        details: function() { return $translate.instant('dashboard.export') },
                        icon: "file_download"
                    }
                );

                dashboardActionsList.push(
                    {
                        onAction: function ($event, item) {
                            makePrivate($event, item);
                        },
                        name: function() { return $translate.instant('action.make-private') },
                        details: function() { return $translate.instant('dashboard.make-private') },
                        icon: "reply",
                        isEnabled: function(dashboard) {
                            return dashboard && customerId == dashboard.publicCustomerId;
                        }
                    }
                );

                dashboardActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, customerId);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('dashboard.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(dashboard) {
                            return dashboard && customerId != dashboard.publicCustomerId;
                        }
                    }
                );

                dashboardGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignDashboardsFromCustomer($event, items, customerId);
                        },
                        name: function() { return $translate.instant('dashboard.unassign-dashboards') },
                        details: function(selectedCount) {
                            return $translate.instant('dashboard.unassign-dashboards-action-title', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_return"
                    }
                );

                vm.dashboardGridConfig.addItemAction = {
                    onAction: function ($event) {
                        addDashboardsToCustomer($event);
                    },
                    name: function() { return $translate.instant('dashboard.assign-dashboards') },
                    details: function() { return $translate.instant('dashboard.assign-new-dashboard') },
                    icon: "add"
                };
            } else if (vm.dashboardsScope === 'customer_user') {
                vm.dashboardGridConfig.addItemAction = {};
            }
        } else if (vm.dashboardsScope === 'edge') {
            fetchDashboardsFunction = function (pageLink) {
                return dashboardService.getEdgeDashboards(edgeId, pageLink);
            };
            deleteDashboardFunction = function (dashboardId) {
                return dashboardService.unassignDashboardFromEdge(edgeId, dashboardId);
            };
            refreshDashboardsParamsFunction = function () {
                return {"edgeId": edgeId, "topIndex": vm.topIndex};
            };

            dashboardActionsList.push(
                {
                    onAction: function ($event, item) {
                        exportDashboard($event, item);
                    },
                    name: function() { $translate.instant('action.export') },
                    details: function() { return $translate.instant('dashboard.export') },
                    icon: "file_download"
                }
            );

            dashboardActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromEdge($event, item, edgeId);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('dashboard.unassign-from-edge') },
                    icon: "assignment_return"
                }
            );

            dashboardGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        unassignDashboardsFromEdge($event, items, edgeId);
                    },
                    name: function() { return $translate.instant('dashboard.unassign-dashboards') },
                    details: function(selectedCount) {
                        return $translate.instant('dashboard.unassign-dashboards-from-edge-action-title', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_return"
                }
            );

            vm.dashboardGridConfig.addItemAction = {
                onAction: function ($event) {
                    addDashboardsToEdge($event);
                },
                name: function() { return $translate.instant('dashboard.assign-dashboards') },
                details: function() { return $translate.instant('dashboard.assign-new-dashboard') },
                icon: "add"
            };
        }

        vm.dashboardGridConfig.refreshParamsFunc = refreshDashboardsParamsFunction;
        vm.dashboardGridConfig.fetchItemsFunc = fetchDashboardsFunction;
        vm.dashboardGridConfig.deleteItemFunc = deleteDashboardFunction;

    }

    function deleteDashboardTitle (dashboard) {
        return $translate.instant('dashboard.delete-dashboard-title', {dashboardTitle: dashboard.title});
    }

    function deleteDashboardText () {
        return $translate.instant('dashboard.delete-dashboard-text');
    }

    function deleteDashboardsTitle (selectedCount) {
        return $translate.instant('dashboard.delete-dashboards-title', {count: selectedCount}, 'messageformat');
    }

    function deleteDashboardsActionTitle(selectedCount) {
        return $translate.instant('dashboard.delete-dashboards-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteDashboardsText () {
        return $translate.instant('dashboard.delete-dashboards-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getDashboardTitle(dashboard) {
        return dashboard ? dashboard.title : '';
    }

    function loadDashboard(dashboard) {
        return dashboardService.getDashboard(dashboard.id.id);
    }

    function saveDashboard(dashboard) {
        var deferred = $q.defer();
        dashboardService.saveDashboard(dashboard).then(
            function success(savedDashboard) {
                var dashboards = [ savedDashboard ];
                customerService.applyAssignedCustomersInfo(dashboards).then(
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

    function manageAssignedCustomers($event, dashboard) {
        showManageAssignedCustomersDialog($event, [dashboard.id.id], 'manage', dashboard.assignedCustomersIds);
    }

    function assignDashboardsToCustomers($event, items) {
        var dashboardIds = [];
        for (var id in items.selections) {
            dashboardIds.push(id);
        }
        showManageAssignedCustomersDialog($event, dashboardIds, 'assign');
    }

    function unassignDashboardsFromCustomers($event, items) {
        var dashboardIds = [];
        for (var id in items.selections) {
            dashboardIds.push(id);
        }
        showManageAssignedCustomersDialog($event, dashboardIds, 'unassign');
    }

    function showManageAssignedCustomersDialog($event, dashboardIds, actionType, assignedCustomers) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'ManageAssignedCustomersController',
            controllerAs: 'vm',
            templateUrl: manageAssignedCustomersTemplate,
            locals: {actionType: actionType, dashboardIds: dashboardIds, assignedCustomers: assignedCustomers},
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

    function addDashboardsToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        dashboardService.getTenantDashboards({limit: pageSize, textSearch: ''}).then(
            function success(_dashboards) {
                var dashboards = {
                    pageSize: pageSize,
                    data: _dashboards.data,
                    nextPageLink: _dashboards.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _dashboards.hasNext,
                    pending: false
                };
                if (dashboards.hasNext) {
                    dashboards.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddDashboardsToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: addDashboardsToCustomerTemplate,
                    locals: {customerId: customerId, dashboards: dashboards},
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

    function unassignFromCustomer($event, dashboard, customerId) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('dashboard.unassign-dashboard-title', {dashboardTitle: dashboard.title});
        var content = $translate.instant('dashboard.unassign-dashboard-text');
        var label = $translate.instant('dashboard.unassign-dashboard');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            dashboardService.unassignDashboardFromCustomer(customerId, dashboard.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function makePublic($event, dashboard) {
        if ($event) {
            $event.stopPropagation();
        }
        dashboardService.makeDashboardPublic(dashboard.id.id).then(function success(dashboard) {
            $mdDialog.show({
                controller: 'MakeDashboardPublicDialogController',
                controllerAs: 'vm',
                templateUrl: makeDashboardPublicDialogTemplate,
                locals: {dashboard: dashboard},
                parent: angular.element($document[0].body),
                fullscreen: true,
                targetEvent: $event
            }).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function makePrivate($event, dashboard) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('dashboard.make-private-dashboard-title', {dashboardTitle: dashboard.title});
        var content = $translate.instant('dashboard.make-private-dashboard-text');
        var label = $translate.instant('dashboard.make-private-dashboard');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            dashboardService.makeDashboardPrivate(dashboard.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function exportDashboard($event, dashboard) {
        $event.stopPropagation();
        importExport.exportDashboard(dashboard.id.id);
    }

    function unassignDashboardsFromCustomer($event, items, customerId) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('dashboard.unassign-dashboards-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('dashboard.unassign-dashboards-text'))
            .ariaLabel($translate.instant('dashboard.unassign-dashboards'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(dashboardService.unassignDashboardFromCustomer(customerId, id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function openDashboard($event, dashboard) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.dashboardsScope === 'customer') {
            $state.go('home.customers.dashboards.dashboard', {
                customerId: customerId,
                dashboardId: dashboard.id.id
            });
        } else if (vm.dashboardsScope === 'edge') {
            $state.go('home.edges.dashboards.dashboard', {
                edgeId: edgeId,
                dashboardId: dashboard.id.id
            });
        } else {
            $state.go('home.dashboards.dashboard', {dashboardId: dashboard.id.id});
        }
    }

    function unassignDashboardsFromEdge($event, items, edgeId) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('dashboard.unassign-dashboards-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('dashboard.unassign-dashboards-from-edge-text'))
            .ariaLabel($translate.instant('dashboard.unassign-dashboards'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(dashboardService.unassignDashboardFromEdge(edgeId, id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function addDashboardsToEdge($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        dashboardService.getTenantDashboards({limit: pageSize, textSearch: ''}).then(
            function success(_dashboards) {
                var dashboards = {
                    pageSize: pageSize,
                    data: _dashboards.data,
                    nextPageLink: _dashboards.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _dashboards.hasNext,
                    pending: false
                };
                if (dashboards.hasNext) {
                    dashboards.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddDashboardsToEdgeController',
                    controllerAs: 'vm',
                    templateUrl: addDashboardsToEdgeTemplate,
                    locals: {edgeId: edgeId, edgeCustomerId: vm.edgeCustomerId.id, dashboards: dashboards},
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

    function unassignFromEdge($event, dashboard, edgeId) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('dashboard.unassign-dashboard-title', {dashboardTitle: dashboard.title});
        var content = $translate.instant('dashboard.unassign-dashboard-from-edge-text');
        var label = $translate.instant('dashboard.unassign-dashboard');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            dashboardService.unassignDashboardFromEdge(edgeId, dashboard.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }
}
