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
export default function AssignEntityViewToEdgeController(edgeService, entityViewService, $mdDialog, $q, entityViewIds, edges) {

    var vm = this;

    vm.edges = edges;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isEdgeSelected = isEdgeSelected;
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
                edgeService.getEdges(vm.edges.nextPageLink).then(
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

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var i=0; i < entityViewIds.length;i++) {
            tasks.push(entityViewService.assignEntityViewToEdge(vm.edges.selection.id.id, entityViewIds[i]));
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
        if (vm.isEdgeSelected(edge)) {
            vm.edges.selection = null;
        } else {
            vm.edges.selection = edge;
        }
    }

    function isEdgeSelected(edge) {
        return vm.edges.selection != null && edge &&
            edge.id.id === vm.edges.selection.id.id;
    }

    function searchEdgeTextUpdated() {
        vm.edges = {
            pageSize: vm.edges.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.edges.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }
}
