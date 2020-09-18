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
export default function AddRuleChainsToEdgeController(ruleChainService, $mdDialog, $q, $filter, edgeId, ruleChains) {

    var vm = this;

    vm.ruleChains = ruleChains;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.hasData = hasData;
    vm.noData = noData;
    vm.searchRuleChainTextUpdated = searchRuleChainTextUpdated;
    vm.toggleRuleChainSelection = toggleRuleChainSelection;

    vm.theRuleChains = {
        getItemAtIndex: function (index) {
            if (index > vm.ruleChains.data.length) {
                vm.theRuleChains.fetchMoreItems_(index);
                return null;
            }
            var item = vm.ruleChains.data[index];
            if (item) {
                item.indexNumber = index + 1;
            }
            return item;
        },

        getLength: function () {
            if (vm.ruleChains.hasNext) {
                return vm.ruleChains.data.length + vm.ruleChains.nextPageLink.limit;
            } else {
                return vm.ruleChains.data.length;
            }
        },

        fetchMoreItems_: function () {
            if (vm.ruleChains.hasNext && !vm.ruleChains.pending) {
                vm.ruleChains.pending = true;
                ruleChainService.getEdgesRuleChains(vm.ruleChains.nextPageLink).then(
                    function success(ruleChains) {
                        vm.ruleChains.data = vm.ruleChains.data.concat(ruleChains.data);
                        vm.ruleChains.nextPageLink = ruleChains.nextPageLink;
                        vm.ruleChains.hasNext = ruleChains.hasNext;
                        if (vm.ruleChains.hasNext) {
                            vm.ruleChains.nextPageLink.limit = vm.ruleChains.pageSize;
                        }
                        vm.ruleChains.pending = false;
                    },
                    function fail() {
                        vm.ruleChains.hasNext = false;
                        vm.ruleChains.pending = false;
                    });
            }
        }
    }

    function cancel () {
        $mdDialog.cancel();
    }

    function assign () {
        var tasks = [];
        for (var ruleChainId in vm.ruleChains.selections) {
            tasks.push(ruleChainService.assignRuleChainToEdge(edgeId, ruleChainId));
        }
        $q.all(tasks).then(function () {
            $mdDialog.hide();
        });
    }

    function noData () {
        return vm.ruleChains.data.length == 0 && !vm.ruleChains.hasNext;
    }

    function hasData () {
        return vm.ruleChains.data.length > 0;
    }

    function toggleRuleChainSelection ($event, ruleChain) {
        $event.stopPropagation();
        var selected = angular.isDefined(ruleChain.selected) && ruleChain.selected;
        ruleChain.selected = !selected;
        if (ruleChain.selected) {
            vm.ruleChains.selections[ruleChain.id.id] = true;
            vm.ruleChains.selectedCount++;
        } else {
            delete vm.ruleChains.selections[ruleChain.id.id];
            vm.ruleChains.selectedCount--;
        }
    }

    function searchRuleChainTextUpdated () {
        vm.ruleChains = {
            pageSize: vm.ruleChains.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.ruleChains.pageSize,
                textSearch: vm.searchText
            },
            selections: {},
            selectedCount: 0,
            hasNext: true,
            pending: false
        };
    }
}