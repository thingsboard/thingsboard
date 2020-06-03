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

import addEntityViewTemplate from './add-entity-view.tpl.html';
import entityViewCard from './entity-view-card.tpl.html';
import assignToCustomerTemplate from './assign-to-customer.tpl.html';
import addEntityViewsToCustomerTemplate from './add-entity-views-to-customer.tpl.html';
import addEntityViewsToEdgeTemplate from './add-entity-views-to-edge.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function EntityViewCardController(types) {

    var vm = this;

    vm.types = types;

    vm.isAssignedToCustomer = function() {
        if (vm.item && vm.item.customerId && vm.parentCtl.entityViewsScope === 'tenant' &&
            vm.item.customerId.id != vm.types.id.nullUid && !vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }

    vm.isPublic = function() {
        if (vm.item && vm.item.assignedCustomer && vm.parentCtl.entityViewsScope === 'tenant' && vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }
}


/*@ngInject*/
export function EntityViewController($rootScope, userService, entityViewService, customerService, edgeService, $state, $stateParams,
                                     $document, $mdDialog, $q, $translate, types) {

    var customerId = $stateParams.customerId;
    var edgeId = $stateParams.edgeId;

    var entityViewActionsList = [];

    var entityViewGroupActionsList = [];

    var vm = this;

    vm.types = types;

    vm.entityViewGridConfig = {
        deleteItemTitleFunc: deleteEntityViewTitle,
        deleteItemContentFunc: deleteEntityViewText,
        deleteItemsTitleFunc: deleteEntityViewsTitle,
        deleteItemsActionTitleFunc: deleteEntityViewsActionTitle,
        deleteItemsContentFunc: deleteEntityViewsText,

        saveItemFunc: saveEntityView,

        getItemTitleFunc: getEntityViewTitle,

        itemCardController: 'EntityViewCardController',
        itemCardTemplateUrl: entityViewCard,
        parentCtl: vm,

        actionsList: entityViewActionsList,
        groupActionsList: entityViewGroupActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addEntityViewTemplate,

        addItemText: function() { return $translate.instant('entity-view.add-entity-view-text') },
        noItemsText: function() { return $translate.instant('entity-view.no-entity-views-text') },
        itemDetailsText: function() { return $translate.instant('entity-view.entity-view-details') },
        isDetailsReadOnly: isCustomerUser,
        isSelectionEnabled: function () {
            return !isCustomerUser();
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.entityViewGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.entityViewGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.entityViewsScope = $state.$current.data.entityViewsType;

    vm.assignToCustomer = assignToCustomer;
    vm.makePublic = makePublic;
    vm.unassignFromCustomer = unassignFromCustomer;

    initController();

    function initController() {
        var fetchEntityViewsFunction = null;
        var deleteEntityViewFunction = null;
        var refreshEntityViewsParamsFunction = null;

        var user = userService.getCurrentUser();

        if (user.authority === 'CUSTOMER_USER') {
            vm.entityViewsScope = 'customer_user';
            customerId = user.customerId;
        }
        if (customerId) {
            vm.customerEntityViewsTitle = $translate.instant('customer.entity-views');
            customerService.getShortCustomerInfo(customerId).then(
                function success(info) {
                    if (info.isPublic) {
                        vm.customerEntityViewsTitle = $translate.instant('customer.public-entity-views');
                    }
                }
            );
        }

        if (vm.entityViewsScope === 'tenant') {
            fetchEntityViewsFunction = function (pageLink, entityViewType) {
                return entityViewService.getTenantEntityViews(pageLink, true, null, entityViewType);
            };
            deleteEntityViewFunction = function (entityViewId) {
                return entityViewService.deleteEntityView(entityViewId);
            };
            refreshEntityViewsParamsFunction = function() {
                return {"topIndex": vm.topIndex};
            };

            entityViewActionsList.push({
                onAction: function ($event, item) {
                    makePublic($event, item);
                },
                name: function() { return $translate.instant('action.share') },
                details: function() { return $translate.instant('entity-view.make-public') },
                icon: "share",
                isEnabled: function(entityView) {
                    return entityView && (!entityView.customerId || entityView.customerId.id === types.id.nullUid);
                }
            });

            entityViewActionsList.push(
                {
                    onAction: function ($event, item) {
                        assignToCustomer($event, [ item.id.id ]);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('entity-view.assign-to-customer') },
                    icon: "assignment_ind",
                    isEnabled: function(entityView) {
                        return entityView && (!entityView.customerId || entityView.customerId.id === types.id.nullUid);
                    }
                }
            );

            entityViewActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromCustomer($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('entity-view.unassign-from-customer') },
                    icon: "assignment_return",
                    isEnabled: function(entityView) {
                        return entityView && entityView.customerId && entityView.customerId.id !== types.id.nullUid && !entityView.assignedCustomer.isPublic;
                    }
                }
            );

            entityViewActionsList.push({
                onAction: function ($event, item) {
                    unassignFromCustomer($event, item, true);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('entity-view.make-private') },
                icon: "reply",
                isEnabled: function(entityView) {
                    return entityView && entityView.customerId && entityView.customerId.id !== types.id.nullUid && entityView.assignedCustomer.isPublic;
                }
            });

            entityViewActionsList.push(
                {
                    onAction: function ($event, item) {
                        vm.grid.deleteItem($event, item);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: function() { return $translate.instant('entity-view.delete') },
                    icon: "delete"
                }
            );

            entityViewGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        assignEntityViewsToCustomer($event, items);
                    },
                    name: function() { return $translate.instant('entity-view.assign-entity-views') },
                    details: function(selectedCount) {
                        return $translate.instant('entity-view.assign-entity-views-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_ind"
                }
            );

            entityViewGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('entity-view.delete-entity-views') },
                    details: deleteEntityViewsActionTitle,
                    icon: "delete"
                }
            );



        } else if (vm.entityViewsScope === 'customer' || vm.entityViewsScope === 'customer_user') {
            fetchEntityViewsFunction = function (pageLink, entityViewType) {
                return entityViewService.getCustomerEntityViews(customerId, pageLink, true, null, entityViewType);
            };
            deleteEntityViewFunction = function (entityViewId) {
                return entityViewService.unassignEntityViewFromCustomer(entityViewId);
            };
            refreshEntityViewsParamsFunction = function () {
                return {"customerId": customerId, "topIndex": vm.topIndex};
            };

            if (vm.entityViewsScope === 'customer') {
                entityViewActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, false);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('entity-view.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(entityView) {
                            return entityView && !entityView.assignedCustomer.isPublic;
                        }
                    }
                );

                entityViewGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignEntityViewsFromCustomer($event, items);
                        },
                        name: function() { return $translate.instant('entity-view.unassign-entity-views') },
                        details: function(selectedCount) {
                            return $translate.instant('entity-view.unassign-entity-views-action-title', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_return"
                    }
                );

                vm.entityViewGridConfig.addItemAction = {
                    onAction: function ($event) {
                        addEntityViewsToCustomer($event);
                    },
                    name: function() { return $translate.instant('entity-view.assign-entity-views') },
                    details: function() { return $translate.instant('entity-view.assign-new-entity-view') },
                    icon: "add"
                };


            } else if (vm.entityViewsScope === 'customer_user') {
                vm.entityViewGridConfig.addItemAction = {};
            }
        } else if (vm.entityViewsScope === 'edge') {
            fetchEntityViewsFunction = function (pageLink, entityViewType) {
                return entityViewService.getEdgeEntityViews(edgeId, pageLink, null, entityViewType);
            };
            deleteEntityViewFunction = function (entityViewId) {
                return entityViewService.unassignEntityViewFromEdge(edgeId, entityViewId);
            };
            refreshEntityViewsParamsFunction = function () {
                return {"edgeId": edgeId, "topIndex": vm.topIndex};
            };

            entityViewActionsList.push({
                    onAction: function ($event, item) {
                        unassignFromEdge($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('entity-view.unassign-from-edge') },
                    icon: "assignment_return"
                }
            );

            entityViewGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        unassignEntityViewsFromEdge($event, items);
                    },
                    name: function() { return $translate.instant('entity-view.unassign-entity-views') },
                    details: function(selectedCount) {
                        return $translate.instant('entity-view.unassign-entity-views-from-edge-action-title', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_return"
                }
            );

            vm.entityViewGridConfig.addItemAction = {
                onAction: function ($event) {
                    addEntityViewsToEdge($event);
                },
                name: function() { return $translate.instant('entity-view.assign-entity-views') },
                details: function() { return $translate.instant('entity-view.assign-new-entity-view') },
                icon: "add"
            };
            vm.entityViewGridConfig.addItemActions = [];

        }

        vm.entityViewGridConfig.refreshParamsFunc = refreshEntityViewsParamsFunction;
        vm.entityViewGridConfig.fetchItemsFunc = fetchEntityViewsFunction;
        vm.entityViewGridConfig.deleteItemFunc = deleteEntityViewFunction;

    }

    function deleteEntityViewTitle(entityView) {
        return $translate.instant('entity-view.delete-entity-view-title', {entityViewName: entityView.name});
    }

    function deleteEntityViewText() {
        return $translate.instant('entity-view.delete-entity-view-text');
    }

    function deleteEntityViewsTitle(selectedCount) {
        return $translate.instant('entity-view.delete-entity-views-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityViewsActionTitle(selectedCount) {
        return $translate.instant('entity-view.delete-entity-views-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteEntityViewsText () {
        return $translate.instant('entity-view.delete-entity-views-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getEntityViewTitle(entityView) {
        return entityView ? entityView.name : '';
    }

    function saveEntityView(entityView) {
        var deferred = $q.defer();
        entityViewService.saveEntityView(entityView).then(
            function success(savedEntityView) {
                $rootScope.$broadcast('entityViewSaved');
                var entityViews = [ savedEntityView ];
                customerService.applyAssignedCustomersInfo(entityViews).then(
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
        return vm.entityViewsScope === 'customer_user';
    }

    function assignToCustomer($event, entityViewIds) {
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
                    controller: 'AssignEntityViewToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: assignToCustomerTemplate,
                    locals: {entityViewIds: entityViewIds, customers: customers},
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

    function addEntityViewsToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        entityViewService.getTenantEntityViews({limit: pageSize, textSearch: ''}, false).then(
            function success(_entityViews) {
                var entityViews = {
                    pageSize: pageSize,
                    data: _entityViews.data,
                    nextPageLink: _entityViews.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _entityViews.hasNext,
                    pending: false
                };
                if (entityViews.hasNext) {
                    entityViews.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddEntityViewsToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: addEntityViewsToCustomerTemplate,
                    locals: {customerId: customerId, entityViews: entityViews},
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

    function assignEntityViewsToCustomer($event, items) {
        var entityViewIds = [];
        for (var id in items.selections) {
            entityViewIds.push(id);
        }
        assignToCustomer($event, entityViewIds);
    }

    function unassignFromCustomer($event, entityView, isPublic) {
        if ($event) {
            $event.stopPropagation();
        }
        var title;
        var content;
        var label;
        if (isPublic) {
            title = $translate.instant('entity-view.make-private-entity-view-title', {entityViewName: entityView.name});
            content = $translate.instant('entity-view.make-private-entity-view-text');
            label = $translate.instant('entity-view.make-private');
        } else {
            title = $translate.instant('entity-view.unassign-entity-view-title', {entityViewName: entityView.name});
            content = $translate.instant('entity-view.unassign-entity-view-text');
            label = $translate.instant('entity-view.unassign-entity-view');
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            entityViewService.unassignEntityViewFromCustomer(entityView.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function unassignEntityViewsFromCustomer($event, items) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('entity-view.unassign-entity-views-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('entity-view.unassign-entity-views-text'))
            .ariaLabel($translate.instant('entity-view.unassign-entity-view'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(entityViewService.unassignEntityViewFromCustomer(id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function makePublic($event, entityView) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('entity-view.make-public-entity-view-title', {entityViewName: entityView.name}))
            .htmlContent($translate.instant('entity-view.make-public-entity-view-text'))
            .ariaLabel($translate.instant('entity-view.make-public'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            entityViewService.makeEntityViewPublic(entityView.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function addEntityViewsToEdge($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        entityViewService.getTenantEntityViews({limit: pageSize, textSearch: ''}, false).then(
            function success(_entityViews) {
                var entityViews = {
                    pageSize: pageSize,
                    data: _entityViews.data,
                    nextPageLink: _entityViews.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _entityViews.hasNext,
                    pending: false
                };
                if (entityViews.hasNext) {
                    entityViews.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddEntityViewsToEdgeController',
                    controllerAs: 'vm',
                    templateUrl: addEntityViewsToEdgeTemplate,
                    locals: {edgeId: edgeId, entityViews: entityViews},
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

    function unassignEntityViewsFromEdge($event, items) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('entity-view.unassign-entity-views-from-edge-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('entity-view.unassign-entity-views-from-edge-text'))
            .ariaLabel($translate.instant('entity-view.unassign-entity-view-from-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(entityViewService.unassignEntityViewFromEdge(edgeId, id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function unassignFromEdge($event, entityView) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('entity-view.unassign-entity-view-from-edge-title', {entityViewName: entityView.name});
        var content = $translate.instant('entity-view.unassign-entity-view-from-edge-text');
        var label = $translate.instant('entity-view.unassign-entity-view');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            entityViewService.unassignEntityViewFromEdge(edgeId, entityView.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }
}
