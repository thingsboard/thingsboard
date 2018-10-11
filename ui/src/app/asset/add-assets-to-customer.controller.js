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
/*@ngInject*/
export default function AddAssetsToCustomerController(assetService, $mdDialog, $q, customerId, assets) {

    var vm = this;

    vm.assets = assets;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchAssetTextUpdated = searchAssetTextUpdated;
    vm.toggleAssetSelection = toggleAssetSelection;

    vm.theAssets = {
        getItemAtIndex: function (index) {
            if (index > vm.assets.data.length) {
                vm.theAssets.fetchMoreItems_(index);
                return null;
            }
            var item = vm.assets.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.assets.hasNext) {
                return vm.assets.data.length + vm.assets.nextPageLink.limit;
            } else {
                return vm.assets.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.assets.hasNext && !vm.assets.pending) {
                vm.assets.pending = true;
                assetService.getTenantAssets(vm.assets.nextPageLink).then(
                    function success(assets) {
                        vm.assets.data = vm.assets.data.concat(assets.data);
                        vm.assets.nextPageLink = assets.nextPageLink;
                        vm.assets.hasNext = assets.hasNext;
                        if (vm.assets.hasNext) {
                            vm.assets.nextPageLink.limit = vm.assets.pageSize;
                        }
                        vm.assets.pending = false;
                    },
                    function fail() {
                        vm.assets.hasNext = false;
                        vm.assets.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var assetId in vm.assets.selections) {
            tasks.push(assetService.assignAssetToCustomer(customerId, assetId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.assets.data.length == 0 && !vm.assets.hasNext;
    }

    function hasData() {
        return vm.assets.data.length > 0;
    }

    function toggleAssetSelection($event, asset) {
        $event.stopPropagation();
        var selected = angular.isDefined(asset.selected) && asset.selected;
        asset.selected = !selected;
        if (asset.selected) {
            vm.assets.selections[asset.id.id] = true;
            vm.assets.selectedCount++;
        } else {
            delete vm.assets.selections[asset.id.id];
            vm.assets.selectedCount--;
        }
    }

    function searchAssetTextUpdated() {
        vm.assets = {
            pageSize: vm.assets.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.assets.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }

}
