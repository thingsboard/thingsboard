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
import './grid.scss';

import thingsboardScopeElement from './scope-element.directive';
import thingsboardDetailsSidenav from './details-sidenav.directive';

/* eslint-disable import/no-unresolved, import/default */

import gridTemplate from './grid.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.grid', [thingsboardScopeElement, thingsboardDetailsSidenav])
    .directive('tbGrid', Grid)
    .controller('AddItemController', AddItemController)
    .controller('ItemCardController', ItemCardController)
    .directive('tbGridCardContent', GridCardContent)
    .filter('range', RangeFilter)
    .name;

/*@ngInject*/
function RangeFilter() {
    return function(input, total) {
        total = parseInt(total);

        for (var i=0; i<total; i++) {
            input.push(i);
        }

        return input;
    };
}

/*@ngInject*/
function ItemCardController() {

    var vm = this; //eslint-disable-line

}

/*@ngInject*/
function GridCardContent($compile, $controller) {
    var linker = function(scope, element) {

        var controllerInstance = null;

        scope.$watch('itemTemplate',
            function() {
                initContent();
            }
        );
        scope.$watch('itemController',
            function() {
                initContent();
            }
        );
        scope.$watch('parentCtl',
            function() {
                controllerInstance.parentCtl = scope.parentCtl;
            }
        );
        scope.$watch('item',
            function() {
                controllerInstance.item = scope.item;
            }
        );

        function initContent() {
            if (scope.itemTemplate && scope.itemController && !controllerInstance) {
                element.html(scope.itemTemplate);
                var locals = {};
                angular.extend(locals, {$scope: scope, $element: element});
                var controller = $controller(scope.itemController, locals, true, 'vm');
                controller.instance = controller();
                controllerInstance = controller.instance;
                controllerInstance.item = scope.item;
                controllerInstance.parentCtl = scope.parentCtl;
                $compile(element.contents())(scope);
            }
        }
    };

    return {
        restrict: "E",
        link: linker,
        scope: {
            parentCtl: "=parentCtl",
            gridCtl: "=gridCtl",
            itemTemplate: "=itemTemplate",
            itemController: "=itemController",
            item: "=item"
        }
    };
}

/*@ngInject*/
function Grid() {
    return {
        restrict: "E",
        scope: true,
        transclude: {
            detailsButtons: '?detailsButtons'
        },
        bindToController: {
            gridConfiguration: '&?'
        },
        controller: GridController,
        controllerAs: 'vm',
        templateUrl: gridTemplate
    }
}

/*@ngInject*/
function GridController($scope, $state, $mdDialog, $document, $q, $mdUtil, $timeout, $translate, $mdMedia, $templateCache, $window, userService) {

    var vm = this;

    var columns = 1;
    if ($mdMedia('md')) {
        columns = 2;
    } else if ($mdMedia('lg')) {
        columns = 3;
    } else if ($mdMedia('gt-lg')) {
        columns = 4;
    }

    var pageSize = 10 * columns;

    vm.columns = columns;

    vm.addItem = addItem;
    vm.deleteItem = deleteItem;
    vm.deleteItems = deleteItems;
    vm.hasData = hasData;
    vm.isCurrentItem = isCurrentItem;
    vm.moveToTop = moveToTop;
    vm.noData = noData;
    vm.onCloseDetails = onCloseDetails;
    vm.onToggleDetailsEditMode = onToggleDetailsEditMode;
    vm.openItem = openItem;
    vm.operatingItem = operatingItem;
    vm.refreshList = refreshList;
    vm.saveItem = saveItem;
    vm.toggleItemSelection = toggleItemSelection;
    vm.triggerResize = triggerResize;
    vm.isTenantAdmin = isTenantAdmin;

    $scope.$watch(function () {
        return $mdMedia('xs') || $mdMedia('sm');
    }, function (sm) {
        if (sm) {
            columnsUpdated(1);
        }
    });
    $scope.$watch(function () {
        return $mdMedia('md');
    }, function (md) {
        if (md) {
            columnsUpdated(2);
        }
    });
    $scope.$watch(function () {
        return $mdMedia('lg');
    }, function (lg) {
        if (lg) {
            columnsUpdated(3);
        }
    });
    $scope.$watch(function () {
        return $mdMedia('gt-lg');
    }, function (gtLg) {
        if (gtLg) {
            columnsUpdated(4);
        }
    });

    initGridConfiguration();

    vm.itemRows = {
        getItemAtIndex: function (index) {
            if (index >= vm.items.rowData.length) {
                vm.itemRows.fetchMoreItems_(index);
                return null;
            }
            return vm.items.rowData[index];
        },

        getLength: function () {
            if (vm.items.hasNext && !vm.items.pending) {
                return vm.items.rowData.length + pageSize;
            } else {
                return vm.items.rowData.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.items.hasNext && !vm.items.pending) {
                var promise = vm.fetchItemsFunc(vm.items.nextPageLink, $scope.searchConfig.searchEntitySubtype);
                if (promise) {
                    vm.items.pending = true;
                    promise.then(
                        function success(items) {
                            if (vm.items.reloadPending) {
                                vm.items.pending = false;
                                reload();
                            } else {
                                vm.items.data = vm.items.data.concat(items.data);
                                var startIndex = vm.items.data.length - items.data.length;
                                var endIndex = vm.items.data.length;
                                for (var i = startIndex; i < endIndex; i++) {
                                    var item = vm.items.data[i];
                                    item.index = i;
                                    var row = Math.floor(i / vm.columns);
                                    var itemRow = vm.items.rowData[row];
                                    if (!itemRow) {
                                        itemRow = [];
                                        vm.items.rowData.push(itemRow);
                                    }
                                    itemRow.push(item);
                                }
                                vm.items.nextPageLink = items.nextPageLink;
                                vm.items.hasNext = items.hasNext;
                                if (vm.items.hasNext) {
                                    vm.items.nextPageLink.limit = pageSize;
                                }
                                vm.items.pending = false;
                                if (vm.items.loadCallback) {
                                    vm.items.loadCallback();
                                    vm.items.loadCallback = null;
                                }
                            }
                        },
                        function fail() {
                            vm.items.hasNext = false;
                            vm.items.pending = false;
                        });
                } else {
                    vm.items.hasNext = false;
                }
            }
        }
    };

    function columnsUpdated(newColumns) {
        if (vm.columns !== newColumns) {
            var newTopIndex = Math.ceil(vm.columns * vm.topIndex / newColumns);
            pageSize = 10 * newColumns;
            vm.items.rowData = [];
            if (vm.items.nextPageLink) {
                vm.items.nextPageLink.limit = pageSize;
            }

            for (var i = 0; i < vm.items.data.length; i++) {
                var item = vm.items.data[i];
                var row = Math.floor(i / newColumns);
                var itemRow = vm.items.rowData[row];
                if (!itemRow) {
                    itemRow = [];
                    vm.items.rowData.push(itemRow);
                }
                itemRow.push(item);
            }

            vm.columns = newColumns;
            vm.topIndex = newTopIndex;
            vm.itemRows.getItemAtIndex(newTopIndex+pageSize);
            $timeout(function() {
                moveToIndex(newTopIndex);
            }, 500);
        }
    }

    function initGridConfiguration() {
        vm.gridConfiguration = vm.gridConfiguration || function () {
                return {};
            };

        vm.config = vm.gridConfiguration();

        vm.itemHeight = vm.config.itemHeight || 199;

        vm.refreshParamsFunc = vm.config.refreshParamsFunc || function () {
                return {"topIndex": vm.topIndex};
            };

        vm.deleteItemTitleFunc = vm.config.deleteItemTitleFunc || function () {
                return $translate.instant('grid.delete-item-title');
            };

        vm.deleteItemContentFunc = vm.config.deleteItemContentFunc || function () {
                return $translate.instant('grid.delete-item-text');
            };

        vm.deleteItemsTitleFunc = vm.config.deleteItemsTitleFunc || function () {
                return $translate.instant('grid.delete-items-title', {count: vm.items.selectedCount}, 'messageformat');
            };

        vm.deleteItemsActionTitleFunc = vm.config.deleteItemsActionTitleFunc || function (selectedCount) {
                return $translate.instant('grid.delete-items-action-title', {count: selectedCount}, 'messageformat');
            };

        vm.deleteItemsContentFunc = vm.config.deleteItemsContentFunc || function () {
                return $translate.instant('grid.delete-items-text');
            };

        vm.fetchItemsFunc = vm.config.fetchItemsFunc || function () {
                return $q.when([]);
            };

        vm.loadItemDetailsFunc = vm.config.loadItemDetailsFunc || function (item) {
                return $q.when(item);
            };

        vm.saveItemFunc = vm.config.saveItemFunc || function (item) {
                return $q.when(item);
            };

        vm.deleteItemFunc = vm.config.deleteItemFunc || function () {
                return $q.when();
            };

        vm.clickItemFunc = vm.config.clickItemFunc || function ($event, item) {
                vm.openItem($event, item);
            };

        vm.itemCardTemplate = '<span></span>';
        if (vm.config.itemCardTemplate) {
            vm.itemCardTemplate = vm.config.itemCardTemplate;
        } else if (vm.config.itemCardTemplateUrl) {
            vm.itemCardTemplate = $templateCache.get(vm.config.itemCardTemplateUrl);
        }
        if (vm.config.itemCardController) {
            vm.itemCardController =  vm.config.itemCardController;
        } else {
            vm.itemCardController = 'ItemCardController';
        }
        if (vm.config.addItemController) {
            vm.addItemController =  vm.config.addItemController;
        } else {
            vm.addItemController = 'AddItemController';
        }

        vm.parentCtl = vm.config.parentCtl || vm;

        vm.getItemTitleFunc = vm.config.getItemTitleFunc || function () {
                return '';
            };

        vm.actionsList = vm.config.actionsList || [];

        for (var i = 0; i < vm.actionsList.length; i++) {
            var action = vm.actionsList[i];
            action.isEnabled = action.isEnabled || function() {
                    return true;
                };
        }

        vm.groupActionsList = vm.config.groupActionsList || [
                {
                    onAction: function ($event) {
                        deleteItems($event);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: vm.deleteItemsActionTitleFunc,
                    icon: "delete"
                }
            ];

        vm.addItemText = vm.config.addItemText || function () {
                return $translate.instant('grid.add-item-text');
            };

        vm.addItemAction = vm.config.addItemAction || {
                onAction: function ($event) {
                    addItem($event);
                },
                name: function() { return $translate.instant('action.add') },
                details: function() { return vm.addItemText() },
                icon: "add"
            };

        vm.addItemActionsOpen = false;

        vm.addItemActions = vm.config.addItemActions || [];

        vm.onGridInited = vm.config.onGridInited || function () {
            };

        vm.addItemTemplateUrl = vm.config.addItemTemplateUrl;

        vm.noItemsText = vm.config.noItemsText || function () {
                return $translate.instant('grid.no-items-text');
            };

        vm.itemDetailsText = vm.config.itemDetailsText || function () {
                return $translate.instant('grid.item-details');
            };

        vm.isDetailsReadOnly = vm.config.isDetailsReadOnly || function () {
                return false;
            };

        vm.isSelectionEnabled = vm.config.isSelectionEnabled || function () {
                return true;
            };

        vm.topIndex = vm.config.topIndex || 0;

        vm.items = vm.config.items || {
                data: [],
                rowData: [],
                nextPageLink: {
                    limit: vm.topIndex + pageSize,
                    textSearch: $scope.searchConfig.searchText
                },
                selections: {},
                selectedCount: 0,
                hasNext: true,
                pending: false
            };

        vm.detailsConfig = {
            isDetailsOpen: false,
            isDetailsEditMode: false,
            currentItem: null,
            editingItem: null
        };
    }

    $scope.$on('searchTextUpdated', function () {
        reload();
    });

    $scope.$on('searchEntitySubtypeUpdated', function () {
        reload();
    });

    vm.onGridInited(vm);

    vm.itemRows.getItemAtIndex(pageSize);

    function reload() {
        if (vm.items && vm.items.pending) {
            vm.items.reloadPending = true;
        } else {
            vm.items.data.length = 0;
            vm.items.rowData.length = 0;
            vm.items.nextPageLink = {
                limit: pageSize,
                textSearch: $scope.searchConfig.searchText
            };
            vm.items.selections = {};
            vm.items.selectedCount = 0;
            vm.items.hasNext = true;
            vm.items.pending = false;
            vm.detailsConfig.isDetailsOpen = false;
            vm.items.reloadPending = false;
            vm.itemRows.getItemAtIndex(pageSize);
        }
    }

    function refreshList() {
        let preservedTopIndex = vm.topIndex;
        vm.items.data.length = 0;
        vm.items.rowData.length = 0;
        vm.items.nextPageLink = {
            limit: preservedTopIndex + pageSize,
            textSearch: $scope.searchConfig.searchText
        };
        vm.items.selections = {};
        vm.items.selectedCount = 0;
        vm.items.hasNext = true;
        vm.items.pending = false;
        vm.detailsConfig.isDetailsOpen = false;
        vm.items.reloadPending = false;
        vm.items.loadCallback = () => {
            $mdUtil.nextTick(() => {
                moveToIndex(preservedTopIndex);
            });
        };
        vm.itemRows.getItemAtIndex(preservedTopIndex+pageSize);
    }

    function addItem($event) {
        $mdDialog.show({
            controller: vm.addItemController,
            controllerAs: 'vm',
            templateUrl: vm.addItemTemplateUrl,
            parent: angular.element($document[0].body),
            locals: {saveItemFunction: vm.saveItemFunc},
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            refreshList();
        }, function () {
        });
    }

    function openItem($event, item) {
        $event.stopPropagation();
        if (vm.detailsConfig.currentItem != null && vm.detailsConfig.currentItem.id.id === item.id.id) {
            if (vm.detailsConfig.isDetailsOpen) {
                vm.detailsConfig.isDetailsOpen = false;
                return;
            }
        }
        vm.loadItemDetailsFunc(item).then(function success(detailsItem) {
            detailsItem.index = item.index;
            vm.detailsConfig.currentItem = detailsItem;
            vm.detailsConfig.isDetailsEditMode = false;
            vm.detailsConfig.isDetailsOpen = true;
        });
    }

    function isCurrentItem(item) {
        if (item != null && vm.detailsConfig.currentItem != null &&
            vm.detailsConfig.currentItem.id.id === item.id.id) {
            return vm.detailsConfig.isDetailsOpen;
        } else {
            return false;
        }
    }

    function onToggleDetailsEditMode(theForm) {
        if (!vm.detailsConfig.isDetailsEditMode) {
            theForm.$setPristine();
        }
    }

    function onCloseDetails() {
        vm.detailsConfig.currentItem = null;
    }

    function operatingItem() {
        if (!vm.detailsConfig.isDetailsEditMode) {
            if (vm.detailsConfig.editingItem) {
                vm.detailsConfig.editingItem = null;
            }
            return vm.detailsConfig.currentItem;
        } else {
            if (!vm.detailsConfig.editingItem) {
                vm.detailsConfig.editingItem = angular.copy(vm.detailsConfig.currentItem);
            }
            return vm.detailsConfig.editingItem;
        }
    }

    function saveItem(theForm) {
        vm.saveItemFunc(vm.detailsConfig.editingItem).then(function success(item) {
            theForm.$setPristine();
            vm.detailsConfig.isDetailsEditMode = false;
            var index = vm.detailsConfig.currentItem.index;
            item.index = index;
            vm.detailsConfig.currentItem = item;
            vm.items.data[index] = item;
            var row = Math.floor(index / vm.columns);
            var itemRow = vm.items.rowData[row];
            var column = index % vm.columns;
            itemRow[column] = item;
        });
    }

    function deleteItem($event, item) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(vm.deleteItemTitleFunc(item))
            .htmlContent(vm.deleteItemContentFunc(item))
            .ariaLabel($translate.instant('grid.delete-item'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
                vm.deleteItemFunc(item.id.id).then(function success() {
                    refreshList();
                });
            },
            function () {
            });

    }

    function deleteItems($event) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(vm.deleteItemsTitleFunc(vm.items.selectedCount))
            .htmlContent(vm.deleteItemsContentFunc())
            .ariaLabel($translate.instant('grid.delete-items'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
                var tasks = [];
                for (var id in vm.items.selections) {
                    tasks.push(vm.deleteItemFunc(id));
                }
                $q.all(tasks).then(function () {
                    refreshList();
                });
            },
            function () {
            });
    }


    function toggleItemSelection($event, item) {
        $event.stopPropagation();
        var selected = angular.isDefined(item.selected) && item.selected;
        item.selected = !selected;
        if (item.selected) {
            vm.items.selections[item.id.id] = true;
            vm.items.selectedCount++;
        } else {
            delete vm.items.selections[item.id.id];
            vm.items.selectedCount--;
        }
    }

    function triggerResize() {
        var w = angular.element($window);
        w.triggerHandler('resize');
    }

    function isTenantAdmin() {
        return userService.getAuthority() == 'TENANT_ADMIN';
    }

    function moveToTop() {
        moveToIndex(0, true);
    }

    function moveToIndex(index, animate) {
        var repeatContainer = $scope.repeatContainer[0];
        var scrollElement = repeatContainer.children[0];
        var startY = scrollElement.scrollTop;
        var stopY = index * vm.itemHeight;
        if (stopY > 0) {
            stopY+= 16;
        }
        var distance = Math.abs(startY - stopY);
        if (distance < 100 || !animate) {
            scrollElement.scrollTop = stopY;
            return;
        }
        var upElseDown = stopY < startY;
        var speed = Math.round(distance / 100);
        if (speed >= 20) speed = 20;
        var step = Math.round(distance / 25);

        var leapY = upElseDown ? startY - step : startY + step;
        var timer = 0;
        for (var i = startY; upElseDown ? (i > stopY) : (i < stopY); upElseDown ? (i -= step) : (i += step)) {
            $timeout(function (topY) {
                scrollElement.scrollTop = topY;
            }, timer * speed, true, leapY);
            if (upElseDown) {
                leapY -= step;
                if (leapY < stopY) {
                    leapY = stopY;
                }
            } else {
                leapY += step;
                if (leapY > stopY) {
                    leapY = stopY;
                }
            }
            timer++;
        }

    }

    function noData() {
        return vm.items.data.length == 0 && !vm.items.hasNext;
    }

    function hasData() {
        return vm.items.data.length > 0;
    }

}

/*@ngInject*/
function AddItemController($scope, $mdDialog, saveItemFunction, helpLinks) {

    var vm = this;

    vm.helpLinks = helpLinks;
    vm.item = {};

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function add() {
        saveItemFunction(vm.item).then(function success(item) {
            vm.item = item;
            $scope.theForm.$setPristine();
            $mdDialog.hide();
        });
    }
}