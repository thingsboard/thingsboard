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

import addAssetTemplate from './add-asset.tpl.html';
import assetCard from './asset-card.tpl.html';
import assignToCustomerTemplate from './assign-to-customer.tpl.html';
import addAssetsToCustomerTemplate from './add-assets-to-customer.tpl.html';
import addAssetsToEdgeTemplate from './add-assets-to-edge.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function AssetCardController(types) {

    var vm = this;

    vm.types = types;

    vm.isAssignedToCustomer = function() {
        if (vm.item && vm.item.customerId && vm.parentCtl.assetsScope === 'tenant' &&
            vm.item.customerId.id != vm.types.id.nullUid && !vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }

    vm.isPublic = function() {
        if (vm.item && vm.item.assignedCustomer && vm.parentCtl.assetsScope === 'tenant' && vm.item.assignedCustomer.isPublic) {
            return true;
        }
        return false;
    }
}


/*@ngInject*/
export function AssetController($rootScope, userService, assetService, customerService, edgeService, $state, $stateParams,
                                $document, $mdDialog, $q, $translate, types, importExport) {

    var customerId = $stateParams.customerId;
    var edgeId = $stateParams.edgeId;

    var assetActionsList = [];

    var assetGroupActionsList = [];

    var assetAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.add') },
            details: function() { return $translate.instant('asset.add-asset-text') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importEntities($event, types.entityType.asset).then(
                    function() {
                        vm.grid.refreshList();
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('asset.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.assetGridConfig = {
        deleteItemTitleFunc: deleteAssetTitle,
        deleteItemContentFunc: deleteAssetText,
        deleteItemsTitleFunc: deleteAssetsTitle,
        deleteItemsActionTitleFunc: deleteAssetsActionTitle,
        deleteItemsContentFunc: deleteAssetsText,

        saveItemFunc: saveAsset,

        getItemTitleFunc: getAssetTitle,

        itemCardController: 'AssetCardController',
        itemCardTemplateUrl: assetCard,
        parentCtl: vm,

        actionsList: assetActionsList,
        groupActionsList: assetGroupActionsList,
        addItemActions: assetAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addAssetTemplate,

        addItemText: function() { return $translate.instant('asset.add-asset-text') },
        noItemsText: function() { return $translate.instant('asset.no-assets-text') },
        itemDetailsText: function() { return $translate.instant('asset.asset-details') },
        isDetailsReadOnly: isCustomerUser,
        isSelectionEnabled: function () {
            return !isCustomerUser();
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.assetGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.assetGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.assetsScope = $state.$current.data.assetsType;

    vm.assignToCustomer = assignToCustomer;
    vm.makePublic = makePublic;
    vm.unassignFromCustomer = unassignFromCustomer;

    initController();

    function initController() {
        var fetchAssetsFunction = null;
        var deleteAssetFunction = null;
        var refreshAssetsParamsFunction = null;

        var user = userService.getCurrentUser();

        if (user.authority === 'CUSTOMER_USER') {
            vm.assetsScope = 'customer_user';
            customerId = user.customerId;
        }
        if (customerId) {
            vm.customerAssetsTitle = $translate.instant('customer.assets');
            customerService.getShortCustomerInfo(customerId).then(
                function success(info) {
                    if (info.isPublic) {
                        vm.customerAssetsTitle = $translate.instant('customer.public-assets');
                    }
                }
            );
        }

        if (vm.assetsScope === 'tenant') {
            fetchAssetsFunction = function (pageLink, assetType) {
                return assetService.getTenantAssets(pageLink, true, null, assetType);
            };
            deleteAssetFunction = function (assetId) {
                return assetService.deleteAsset(assetId);
            };
            refreshAssetsParamsFunction = function() {
                return {"topIndex": vm.topIndex};
            };

            assetActionsList.push({
                onAction: function ($event, item) {
                    makePublic($event, item);
                },
                name: function() { return $translate.instant('action.share') },
                details: function() { return $translate.instant('asset.make-public') },
                icon: "share",
                isEnabled: function(asset) {
                    return asset && (!asset.customerId || asset.customerId.id === types.id.nullUid);
                }
            });

            assetActionsList.push(
                {
                    onAction: function ($event, item) {
                        assignToCustomer($event, [ item.id.id ]);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('asset.assign-to-customer') },
                    icon: "assignment_ind",
                    isEnabled: function(asset) {
                        return asset && (!asset.customerId || asset.customerId.id === types.id.nullUid);
                    }
                }
            );

            assetActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromCustomer($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('asset.unassign-from-customer') },
                    icon: "assignment_return",
                    isEnabled: function(asset) {
                        return asset && asset.customerId && asset.customerId.id !== types.id.nullUid && !asset.assignedCustomer.isPublic;
                    }
                }
            );

            assetActionsList.push({
                onAction: function ($event, item) {
                    unassignFromCustomer($event, item, true);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('asset.make-private') },
                icon: "reply",
                isEnabled: function(asset) {
                    return asset && asset.customerId && asset.customerId.id !== types.id.nullUid && asset.assignedCustomer.isPublic;
                }
            });

            assetActionsList.push(
                {
                    onAction: function ($event, item) {
                        vm.grid.deleteItem($event, item);
                    },
                    name: function() { return $translate.instant('action.delete') },
                    details: function() { return $translate.instant('asset.delete') },
                    icon: "delete"
                }
            );

            assetGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        assignAssetsToCustomer($event, items);
                    },
                    name: function() { return $translate.instant('asset.assign-assets') },
                    details: function(selectedCount) {
                        return $translate.instant('asset.assign-assets-text', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_ind"
                }
            );

            assetGroupActionsList.push(
                {
                    onAction: function ($event) {
                        vm.grid.deleteItems($event);
                    },
                    name: function() { return $translate.instant('asset.delete-assets') },
                    details: deleteAssetsActionTitle,
                    icon: "delete"
                }
            );



        } else if (vm.assetsScope === 'customer' || vm.assetsScope === 'customer_user') {
            fetchAssetsFunction = function (pageLink, assetType) {
                return assetService.getCustomerAssets(customerId, pageLink, true, null, assetType);
            };
            deleteAssetFunction = function (assetId) {
                return assetService.unassignAssetFromCustomer(assetId);
            };
            refreshAssetsParamsFunction = function () {
                return {"customerId": customerId, "topIndex": vm.topIndex};
            };

            if (vm.assetsScope === 'customer') {
                assetActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, false);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('asset.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(asset) {
                            return asset && !asset.assignedCustomer.isPublic;
                        }
                    }
                );
                assetActionsList.push(
                    {
                        onAction: function ($event, item) {
                            unassignFromCustomer($event, item, true);
                        },
                        name: function() { return $translate.instant('action.make-private') },
                        details: function() { return $translate.instant('asset.make-private') },
                        icon: "reply",
                        isEnabled: function(asset) {
                            return asset && asset.assignedCustomer.isPublic;
                        }
                    }
                );

                assetGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignAssetsFromCustomer($event, items);
                        },
                        name: function() { return $translate.instant('asset.unassign-assets') },
                        details: function(selectedCount) {
                            return $translate.instant('asset.unassign-assets-action-title', {count: selectedCount}, "messageformat");
                        },
                        icon: "assignment_return"
                    }
                );

                vm.assetGridConfig.addItemAction = {
                    onAction: function ($event) {
                        addAssetsToCustomer($event);
                    },
                    name: function() { return $translate.instant('asset.assign-assets') },
                    details: function() { return $translate.instant('asset.assign-new-asset') },
                    icon: "add"
                };


            } else if (vm.assetsScope === 'customer_user') {
                vm.assetGridConfig.addItemAction = {};
            }
            vm.assetGridConfig.addItemActions = [];

        } else if (vm.assetsScope === 'edge') {
            fetchAssetsFunction = function (pageLink, assetType) {
                return assetService.getEdgeAssets(edgeId, pageLink, null, assetType);
            };
            deleteAssetFunction = function (assetId) {
                return assetService.unassignAssetFromEdge(edgeId, assetId);
            };
            refreshAssetsParamsFunction = function () {
                return {"edgeId": edgeId, "topIndex": vm.topIndex};
            };

            assetActionsList.push(
                {
                    onAction: function ($event, item) {
                        unassignFromEdge($event, item, false);
                    },
                    name: function() { return $translate.instant('action.unassign') },
                    details: function() { return $translate.instant('edge.unassign-from-edge') },
                    icon: "assignment_return"
                }
            );

            assetGroupActionsList.push(
                {
                    onAction: function ($event, items) {
                        unassignAssetsFromEdge($event, items);
                    },
                    name: function() { return $translate.instant('asset.unassign-assets') },
                    details: function(selectedCount) {
                        return $translate.instant('asset.unassign-assets-from-edge-action-title', {count: selectedCount}, "messageformat");
                    },
                    icon: "assignment_return"
                }
            );

            vm.assetGridConfig.addItemAction = {
                onAction: function ($event) {
                    addAssetsToEdge($event);
                },
                name: function() { return $translate.instant('asset.assign-assets') },
                details: function() { return $translate.instant('asset.assign-new-asset') },
                icon: "add"
            };
            vm.assetGridConfig.addItemActions = [];

        }

        vm.assetGridConfig.refreshParamsFunc = refreshAssetsParamsFunction;
        vm.assetGridConfig.fetchItemsFunc = fetchAssetsFunction;
        vm.assetGridConfig.deleteItemFunc = deleteAssetFunction;

    }

    function deleteAssetTitle(asset) {
        return $translate.instant('asset.delete-asset-title', {assetName: asset.name});
    }

    function deleteAssetText() {
        return $translate.instant('asset.delete-asset-text');
    }

    function deleteAssetsTitle(selectedCount) {
        return $translate.instant('asset.delete-assets-title', {count: selectedCount}, 'messageformat');
    }

    function deleteAssetsActionTitle(selectedCount) {
        return $translate.instant('asset.delete-assets-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteAssetsText () {
        return $translate.instant('asset.delete-assets-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function getAssetTitle(asset) {
        return asset ? asset.name : '';
    }

    function saveAsset(asset) {
        var deferred = $q.defer();
        assetService.saveAsset(asset).then(
            function success(savedAsset) {
                $rootScope.$broadcast('assetSaved');
                var assets = [ savedAsset ];
                customerService.applyAssignedCustomersInfo(assets).then(
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
        return vm.assetsScope === 'customer_user';
    }

    function assignToCustomer($event, assetIds) {
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
                    controller: 'AssignAssetToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: assignToCustomerTemplate,
                    locals: {assetIds: assetIds, customers: customers},
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

    function addAssetsToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        assetService.getTenantAssets({limit: pageSize, textSearch: ''}, false).then(
            function success(_assets) {
                var assets = {
                    pageSize: pageSize,
                    data: _assets.data,
                    nextPageLink: _assets.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _assets.hasNext,
                    pending: false
                };
                if (assets.hasNext) {
                    assets.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddAssetsToCustomerController',
                    controllerAs: 'vm',
                    templateUrl: addAssetsToCustomerTemplate,
                    locals: {customerId: customerId, assets: assets},
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

    function assignAssetsToCustomer($event, items) {
        var assetIds = [];
        for (var id in items.selections) {
            assetIds.push(id);
        }
        assignToCustomer($event, assetIds);
    }

    function unassignFromCustomer($event, asset, isPublic) {
        if ($event) {
            $event.stopPropagation();
        }
        var title;
        var content;
        var label;
        if (isPublic) {
            title = $translate.instant('asset.make-private-asset-title', {assetName: asset.name});
            content = $translate.instant('asset.make-private-asset-text');
            label = $translate.instant('asset.make-private');
        } else {
            title = $translate.instant('asset.unassign-asset-title', {assetName: asset.name});
            content = $translate.instant('asset.unassign-asset-text');
            label = $translate.instant('asset.unassign-asset');
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            assetService.unassignAssetFromCustomer(asset.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function unassignAssetsFromCustomer($event, items) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('asset.unassign-assets-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('asset.unassign-assets-text'))
            .ariaLabel($translate.instant('asset.unassign-asset'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(assetService.unassignAssetFromCustomer(id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

    function makePublic($event, asset) {
        if ($event) {
            $event.stopPropagation();
        }
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('asset.make-public-asset-title', {assetName: asset.name}))
            .htmlContent($translate.instant('asset.make-public-asset-text'))
            .ariaLabel($translate.instant('asset.make-public'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            assetService.makeAssetPublic(asset.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function addAssetsToEdge($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        assetService.getTenantAssets({limit: pageSize, textSearch: ''}, false).then(
            function success(_assets) {
                var assets = {
                    pageSize: pageSize,
                    data: _assets.data,
                    nextPageLink: _assets.nextPageLink,
                    selections: {},
                    selectedCount: 0,
                    hasNext: _assets.hasNext,
                    pending: false
                };
                if (assets.hasNext) {
                    assets.nextPageLink.limit = pageSize;
                }
                $mdDialog.show({
                    controller: 'AddAssetsToEdgeController',
                    controllerAs: 'vm',
                    templateUrl: addAssetsToEdgeTemplate,
                    locals: {edgeId: edgeId, assets: assets},
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

    function unassignFromEdge($event, asset) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('asset.unassign-asset-from-edge-title', {assetName: asset.name});
        var content = $translate.instant('asset.unassign-asset-from-edge-text');
        var label = $translate.instant('asset.unassign-asset');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            assetService.unassignAssetFromEdge(edgeId, asset.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }

    function unassignAssetsFromEdge($event, items) {
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('asset.unassign-assets-from-edge-title', {count: items.selectedCount}, 'messageformat'))
            .htmlContent($translate.instant('asset.unassign-assets-from-edge-text'))
            .ariaLabel($translate.instant('asset.unassign-asset-from-edge'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            var tasks = [];
            for (var id in items.selections) {
                tasks.push(assetService.unassignAssetFromEdge(edgeId, id));
            }
            $q.all(tasks).then(function () {
                vm.grid.refreshList();
            });
        });
    }

}
