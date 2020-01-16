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
/*@ngInject*/
export default function AssignAssetToCustomerController(customerService, assetService, $mdDialog, $q, assetIds, customers) {

    var vm = this;

    vm.customers = customers;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isCustomerSelected = isCustomerSelected;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchCustomerTextUpdated = searchCustomerTextUpdated;
    vm.toggleCustomerSelection = toggleCustomerSelection;

    vm.theCustomers = {
        getItemAtIndex: function (index) {
            if (index > vm.customers.data.length) {
                vm.theCustomers.fetchMoreItems_(index);
                return null;
            }
            var item = vm.customers.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.customers.hasNext) {
                return vm.customers.data.length + vm.customers.nextPageLink.limit;
            } else {
                return vm.customers.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.customers.hasNext && !vm.customers.pending) {
                vm.customers.pending = true;
                customerService.getCustomers(vm.customers.nextPageLink).then(
                    function success(customers) {
                        vm.customers.data = vm.customers.data.concat(customers.data);
                        vm.customers.nextPageLink = customers.nextPageLink;
                        vm.customers.hasNext = customers.hasNext;
                        if (vm.customers.hasNext) {
                            vm.customers.nextPageLink.limit = vm.customers.pageSize;
                        }
                        vm.customers.pending = false;
                    },
                    function fail() {
                        vm.customers.hasNext = false;
                        vm.customers.pending = false;
                    });
            }
        }
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var i=0;i<assetIds.length;i++) {
            tasks.push(assetService.assignAssetToCustomer(vm.customers.selection.id.id, assetIds[i]));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.customers.data.length == 0 && !vm.customers.hasNext;
    }

    function hasData() {
        return vm.customers.data.length > 0;
    }

    function toggleCustomerSelection($event, customer) {
        $event.stopPropagation();
        if (vm.isCustomerSelected(customer)) {
            vm.customers.selection = null;
        } else {
            vm.customers.selection = customer;
        }
    }

    function isCustomerSelected(customer) {
        return vm.customers.selection != null && customer &&
            customer.id.id === vm.customers.selection.id.id;
    }

    function searchCustomerTextUpdated() {
        vm.customers = {
            pageSize: vm.customers.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.customers.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }
}
