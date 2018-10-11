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
/* eslint-disable import/no-unresolved, import/default */

import addAssetTemplate from './add-asset.tpl.html';
import assetCard from './asset-card.tpl.html';
import addAssetsToCustomerTemplate from './add-assets-to-customer.tpl.html';
import manageAssignedCustomersTemplate from './manage-assigned-customers.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export function AssetCardController(types) {

    var vm = this;

    vm.types = types;

    vm.isAssignedToCustomer = function() {
        return (vm.item && vm.item.assignedCustomers.length !== 0 && vm.parentCtl.assetsScope === 'tenant');
    }

    vm.isPublic = function() {
        return (vm.parentCtl.assetsScope === 'tenant' && vm.item && vm.item.publicCustomerId);
    }
}


/*@ngInject*/
export function AssetController($rootScope, userService, assetService, customerService, $state, $stateParams,
                                $document, $mdDialog, $q, $translate, types) {

    var customerId = $stateParams.customerId;

    var assetActionsList = [];

    var assetGroupActionsList = [];

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

    vm.makePublic = makePublic;
    vm.makePrivate = makePrivate;
    vm.unassignFromCustomer = unassignFromCustomer;
    vm.manageAssignedCustomers = manageAssignedCustomers;

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
                return assetService.getTenantAssets(pageLink, null, assetType);
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
                    return asset && (!asset.publicCustomerId);
                }
            });


            assetActionsList.push({
                onAction: function ($event, item) {
                    makePrivate($event, item);
                },
                name: function() { return $translate.instant('action.make-private') },
                details: function() { return $translate.instant('asset.make-private') },
                icon: "reply",
                isEnabled: function(asset) {
                    return asset && asset.publicCustomerId;
                }
            });

            assetActionsList.push(
                {
                    onAction: function ($event, item) {
                        manageAssignedCustomers($event, item);
                    },
                    name: function() { return $translate.instant('action.assign') },
                    details: function() { return $translate.instant('asset.manage-assigned-customers') },
                    icon: "assignment_ind",
                    isEnabled: function(asset) {
                        return asset;
                    }
                }
            );

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
                        assignAssetsToCustomers($event, items);
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
                return assetService.getCustomerAssets(customerId, pageLink, null, assetType);
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
                            unassignFromCustomer($event, item, customerId);
                        },
                        name: function() { return $translate.instant('action.unassign') },
                        details: function() { return $translate.instant('asset.unassign-from-customer') },
                        icon: "assignment_return",
                        isEnabled: function(asset) {
                            return asset && customerId != asset.publicCustomerId;
                        }
                    }
                );
                assetActionsList.push(
                    {
                        onAction: function ($event, item) {
                            makePrivate($event, item);
                        },
                        name: function() { return $translate.instant('action.make-private') },
                        details: function() { return $translate.instant('asset.make-private') },
                        icon: "reply",
                        isEnabled: function(asset) {
                            return asset && asset.publicCustomerId;
                        }
                    }
                );

                assetGroupActionsList.push(
                    {
                        onAction: function ($event, items) {
                            unassignAssetsFromCustomers($event, items);
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
            function success() {
                $rootScope.$broadcast('assetSaved');
                deferred.resolve();
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

    function addAssetsToCustomer($event) {
        if ($event) {
            $event.stopPropagation();
        }
        var pageSize = 10;
        assetService.getTenantAssets({limit: pageSize, textSearch: ''}).then(
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

    function manageAssignedCustomers($event, asset) {
        showManageAssignedCustomersDialog($event, [asset.id.id], 'manage', asset.assignedCustomersIds);
    }

    function assignAssetsToCustomers($event, items) {
        var assetIds = [];
        for (var id in items.selections) {
            assetIds.push(id);
        }
        showManageAssignedCustomersDialog($event, assetIds, 'assign');
    }

    function unassignAssetsFromCustomers($event, items) {
        var assetIds = [];
        for (var id in items.selections) {
            assetIds.push(id);
        }
        showManageAssignedCustomersDialog($event, assetIds, 'unassign');
    }

    function showManageAssignedCustomersDialog($event, assetIds, actionType, assignedCustomers) {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: 'ManageAssetAssignedCustomersController',
            controllerAs: 'vm',
            templateUrl: manageAssignedCustomersTemplate,
            locals: {actionType: actionType, assetIds: assetIds, assignedCustomers: assignedCustomers},
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

    function unassignFromCustomer($event, asset, customerId) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('asset.unassign-asset-title', {assetName: asset.name});
        var content = $translate.instant('asset.unassign-asset-text');
        var label = $translate.instant('asset.unassign-asset');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            assetService.unassignAssetFromCustomer(asset.id.id, customerId).then(function success() {
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

    function makePrivate($event, asset) {
        if ($event) {
            $event.stopPropagation();
        }
        var title = $translate.instant('asset.make-private-asset-title', {assetTitle: asset.title});
        var content = $translate.instant('asset.make-private-asset-text');
        var label = $translate.instant('asset.make-private-asset');
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title(title)
            .htmlContent(content)
            .ariaLabel(label)
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            assetService.makeAssetPrivate(asset.id.id).then(function success() {
                vm.grid.refreshList();
            });
        });
    }
}
