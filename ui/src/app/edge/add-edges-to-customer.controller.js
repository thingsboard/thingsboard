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
export default function AddEdgesToCustomerController(edgeService, $mdDialog, $q, customerId, edges) {

    var vm = this;

    vm.edges = edges;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchEdgeTextUpdated = searchEdgeTextUpdated;
    vm.toggleEdgeSelection = toggleEdgeSelection;

    vm.theEdges = {
        getItemAtIndex: function (index) {
            if (index > vm.edges.data.length) {
                vm.theEdges.fetchMoreItems_(index);
                return null;
            }
            var item = vm.edges.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.edges.hasNext) {
                return vm.edges.data.length + vm.edges.nextPageLink.limit;
            } else {
                return vm.edges.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.edges.hasNext && !vm.edges.pending) {
                vm.edges.pending = true;
                edgeService.getTenantEdges(vm.edges.nextPageLink, false).then(
                    function success(edges) {
                        vm.edges.data = vm.edges.data.concat(edges.data);
                        vm.edges.nextPageLink = edges.nextPageLink;
                        vm.edges.hasNext = edges.hasNext;
                        if (vm.edges.hasNext) {
                            vm.edges.nextPageLink.limit = vm.edges.pageSize;
                        }
                        vm.edges.pending = false;
                    },
                    function fail() {
                        vm.edges.hasNext = false;
                        vm.edges.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var edgeId in vm.edges.selections) {
            tasks.push(edgeService.assignEdgeToCustomer(customerId, edgeId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.edges.data.length == 0 && !vm.edges.hasNext;
    }

    function hasData() {
        return vm.edges.data.length > 0;
    }

    function toggleEdgeSelection($event, edge) {
        $event.stopPropagation();
        var selected = angular.isDefined(edge.selected) && edge.selected;
        edge.selected = !selected;
        if (edge.selected) {
            vm.edges.selections[edge.id.id] = true;
            vm.edges.selectedCount++;
        } else {
            delete vm.edges.selections[edge.id.id];
            vm.edges.selectedCount--;
        }
    }

    function searchEdgeTextUpdated() {
        vm.edges = {
            pageSize: vm.edges.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.edges.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }

}
