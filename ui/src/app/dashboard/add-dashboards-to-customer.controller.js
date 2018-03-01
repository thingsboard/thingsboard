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
export default function AddDashboardsToCustomerController(dashboardService, $mdDialog, $q, customerId, dashboards) {

    var vm = this;

    vm.dashboards = dashboards;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchDashboardTextUpdated = searchDashboardTextUpdated;
    vm.toggleDashboardSelection = toggleDashboardSelection;

    vm.theDashboards = {
        getItemAtIndex: function (index) {
            if (index > vm.dashboards.data.length) {
                vm.theDashboards.fetchMoreItems_(index);
                return null;
            }
            var item = vm.dashboards.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.dashboards.hasNext) {
                return vm.dashboards.data.length + vm.dashboards.nextPageLink.limit;
            } else {
                return vm.dashboards.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.dashboards.hasNext && !vm.dashboards.pending) {
                vm.dashboards.pending = true;
                dashboardService.getTenantDashboards(vm.dashboards.nextPageLink).then(
                    function success(dashboards) {
                        vm.dashboards.data = vm.dashboards.data.concat(dashboards.data);
                        vm.dashboards.nextPageLink = dashboards.nextPageLink;
                        vm.dashboards.hasNext = dashboards.hasNext;
                        if (vm.dashboards.hasNext) {
                            vm.dashboards.nextPageLink.limit = vm.dashboards.pageSize;
                        }
                        vm.dashboards.pending = false;
                    },
                    function fail() {
                        vm.dashboards.hasNext = false;
                        vm.dashboards.pending = false;
                    });
            }
        }
    }

    function cancel () {
        $mdDialog.cancel();
    }

    function assign () {
        var tasks = [];
        for (var dashboardId in vm.dashboards.selections) {
            tasks.push(dashboardService.assignDashboardToCustomer(customerId, dashboardId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData () {
        return vm.dashboards.data.length == 0 && !vm.dashboards.hasNext;
    }

    function hasData () {
        return vm.dashboards.data.length > 0;
    }

    function toggleDashboardSelection ($event, dashboard) {
        $event.stopPropagation();
        var selected = angular.isDefined(dashboard.selected) && dashboard.selected;
        dashboard.selected = !selected;
        if (dashboard.selected) {
            vm.dashboards.selections[dashboard.id.id] = true;
            vm.dashboards.selectedCount++;
        } else {
            delete vm.dashboards.selections[dashboard.id.id];
            vm.dashboards.selectedCount--;
        }
    }

    function searchDashboardTextUpdated () {
        vm.dashboards = {
            pageSize: vm.dashboards.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.dashboards.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }
}