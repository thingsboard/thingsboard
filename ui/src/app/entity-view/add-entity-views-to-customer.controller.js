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
export default function AddEntityViewsToCustomerController(entityViewService, $mdDialog, $q, customerId, entityViews) {

    var vm = this;

    vm.entityViews = entityViews;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchEntityViewTextUpdated = searchEntityViewTextUpdated;
    vm.toggleEntityViewSelection = toggleEntityViewSelection;

    vm.theEntityViews = {
        getItemAtIndex: function (index) {
            if (index > vm.entityViews.data.length) {
                vm.theEntityViews.fetchMoreItems_(index);
                return null;
            }
            var item = vm.entityViews.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.entityViews.hasNext) {
                return vm.entityViews.data.length + vm.entityViews.nextPageLink.limit;
            } else {
                return vm.entityViews.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.entityViews.hasNext && !vm.entityViews.pending) {
                vm.entityViews.pending = true;
                entityViewService.getTenantEntityViews(vm.entityViews.nextPageLink, false).then(
                    function success(entityViews) {
                        vm.entityViews.data = vm.entityViews.data.concat(entityViews.data);
                        vm.entityViews.nextPageLink = entityViews.nextPageLink;
                        vm.entityViews.hasNext = entityViews.hasNext;
                        if (vm.entityViews.hasNext) {
                            vm.entityViews.nextPageLink.limit = vm.entityViews.pageSize;
                        }
                        vm.entityViews.pending = false;
                    },
                    function fail() {
                        vm.entityViews.hasNext = false;
                        vm.entityViews.pending = false;
                    });
            }
        }
    };

    function cancel () {
        $mdDialog.cancel();
    }

    function assign() {
        var tasks = [];
        for (var entityViewId in vm.entityViews.selections) {
            tasks.push(entityViewService.assignEntityViewToCustomer(customerId, entityViewId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData() {
        return vm.entityViews.data.length == 0 && !vm.entityViews.hasNext;
    }

    function hasData() {
        return vm.entityViews.data.length > 0;
    }

    function toggleEntityViewSelection($event, entityView) {
        $event.stopPropagation();
        var selected = angular.isDefined(entityView.selected) && entityView.selected;
        entityView.selected = !selected;
        if (entityView.selected) {
            vm.entityViews.selections[entityView.id.id] = true;
            vm.entityViews.selectedCount++;
        } else {
            delete vm.entityViews.selections[entityView.id.id];
            vm.entityViews.selectedCount--;
        }
    }

    function searchEntityViewTextUpdated() {
        vm.entityViews = {
            pageSize: vm.entityViews.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.entityViews.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }

}
