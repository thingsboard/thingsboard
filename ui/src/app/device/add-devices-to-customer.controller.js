/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
export default function AddDevicesToCustomerController(deviceService, $mdDialog, $q, customerId, devices) {

    var vm = this;

    vm.devices = devices;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchDeviceTextUpdated = searchDeviceTextUpdated;
    vm.toggleDeviceSelection = toggleDeviceSelection;

    vm.theDevices = {
        getItemAtIndex: function (index) {
            if (index > vm.devices.data.length) {
                vm.theDevices.fetchMoreItems_(index);
                return null;
            }
            var item = vm.devices.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.devices.hasNext) {
                return vm.devices.data.length + vm.devices.nextPageLink.limit;
            } else {
                return vm.devices.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.devices.hasNext && !vm.devices.pending) {
                vm.devices.pending = true;
                deviceService.getTenantDevices(vm.devices.nextPageLink, false).then(
                    function success(devices) {
                        vm.devices.data = vm.devices.data.concat(devices.data);
                        vm.devices.nextPageLink = devices.nextPageLink;
                        vm.devices.hasNext = devices.hasNext;
                        if (vm.devices.hasNext) {
                            vm.devices.nextPageLink.limit = vm.devices.pageSize;
                        }
                        vm.devices.pending = false;
                    },
                    function fail() {
                        vm.devices.hasNext = false;
                        vm.devices.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var deviceId in vm.devices.selections) {
            tasks.push(deviceService.assignDeviceToCustomer(customerId, deviceId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.devices.data.length == 0 && !vm.devices.hasNext;
    }

    function hasData() {
        return vm.devices.data.length > 0;
    }

    function toggleDeviceSelection($event, device) {
        $event.stopPropagation();
        var selected = angular.isDefined(device.selected) && device.selected;
        device.selected = !selected;
        if (device.selected) {
            vm.devices.selections[device.id.id] = true;
            vm.devices.selectedCount++;
        } else {
            delete vm.devices.selections[device.id.id];
            vm.devices.selectedCount--;
        }
    }

    function searchDeviceTextUpdated() {
        vm.devices = {
            pageSize: vm.devices.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.devices.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }

}
