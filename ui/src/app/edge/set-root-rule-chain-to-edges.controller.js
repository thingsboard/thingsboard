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
export default function SetRootRuleChainToEdgesController(ruleChainService, edgeService, $mdDialog, $q, edgeIds, ruleChains) {

    var vm = this;

    vm.ruleChains = ruleChains;
    vm.searchText = '';

    vm.assign = assign;
    vm.cancel = cancel;
    vm.isRuleChainSelected = isRuleChainSelected;
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
    };

    function cancel() {
        $mdDialog.cancel();
    }

    function assign() {
        var assignTasks = [];
        for (var i=0;i<edgeIds.length;i++) {
            assignTasks.push(ruleChainService.assignRuleChainToEdge(edgeIds[i], vm.ruleChains.selection.id.id));
        }
        $q.all(assignTasks).then(function () {
            var setRootTasks = [];
            for (var j=0;j<edgeIds.length;j++) {
                setRootTasks.push(edgeService.setRootRuleChain(edgeIds[j], vm.ruleChains.selection.id.id));
            }
            $q.all(setRootTasks).then(function () {
                $mdDialog.hide();
            });
        });
    }

    function noData() {
        return vm.ruleChains.data.length == 0 && !vm.ruleChains.hasNext;
    }

    function hasData() {
        return vm.ruleChains.data.length > 0;
    }

    function toggleRuleChainSelection($event, ruleChain) {
        $event.stopPropagation();
        if (vm.isRuleChainSelected(ruleChain)) {
            vm.ruleChains.selection = null;
        } else {
            vm.ruleChains.selection = ruleChain;
        }
    }

    function isRuleChainSelected(ruleChain) {
        return vm.ruleChains.selection != null && ruleChain &&
            ruleChain.id.id === vm.ruleChains.selection.id.id;
    }

    function searchRuleChainTextUpdated() {
        vm.ruleChains = {
            pageSize: vm.ruleChains.pageSize,
            data: [],
            nextPageLink: {
                limit: vm.ruleChains.pageSize,
                textSearch: vm.searchText
            },
            selection: null,
            hasNext: true,
            pending: false
        };
    }
}
