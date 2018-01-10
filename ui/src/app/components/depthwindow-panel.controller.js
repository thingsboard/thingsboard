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
export default function DepthwindowPanelController(mdPanelRef, $scope, depthService, types, depthwindow, historyOnly, aggregation, onDepthwindowUpdate) {

    var vm = this;

    vm._mdPanelRef = mdPanelRef;
    vm.depthwindow = depthwindow;
    vm.historyOnly = historyOnly;
    vm.aggregation = aggregation;
    vm.onDepthwindowUpdate = onDepthwindowUpdate;
    vm.aggregationTypes = types.aggregation;
    vm.showLimit = showLimit;
    vm.showRealtimeDepthAggInterval = showRealtimeAggInterval;
    vm.showHistoryDepthAggInterval = showHistoryAggInterval;
    vm.minRealtimeDepthAggInterval = minRealtimeAggInterval;
    vm.maxRealtimeDepthAggInterval = maxRealtimeAggInterval;
    vm.minHistoryDepthAggInterval = minHistoryAggInterval;
    vm.maxHistoryDepthAggInterval = maxHistoryAggInterval;

    if (vm.historyOnly) {
        vm.depthwindow.selectedTab = 1;
    }

    vm._mdPanelRef.config.onOpenComplete = function () {
        $scope.theForm.$setPristine();
    }

    $scope.$watch('vm.depthwindow.selectedTab', function (newSelection, prevSelection) {
        if (newSelection !== prevSelection) {
            $scope.theForm.$setDirty();
        }
    });

    vm.cancel = function () {
        vm._mdPanelRef && vm._mdPanelRef.close();
    };

    vm.update = function () {
        vm._mdPanelRef && vm._mdPanelRef.close().then(function () {
            vm.onDepthwindowUpdate && vm.onDepthwindowUpdate(vm.depthwindow);
        });
    };

    function showLimit() {
        return vm.depthwindow.aggregation.type === vm.aggregationTypes.none.value;
    }

    function showRealtimeAggInterval() {
        return vm.depthwindow.aggregation.type !== vm.aggregationTypes.none.value &&
            vm.depthwindow.selectedTab === 0;
    }

    function showHistoryAggInterval() {
        return vm.depthwindow.aggregation.type !== vm.aggregationTypes.none.value &&
            vm.depthwindow.selectedTab === 1;
    }

    function minRealtimeAggInterval () {
        return depthService.minIntervalLimit(vm.depthwindow.realtime.depthwindowFt);
    }

    function maxRealtimeAggInterval () {
        return depthService.maxIntervalLimit(vm.depthwindow.realtime.depthwindowFt);
    }

    function minHistoryAggInterval () {
        return depthService.minIntervalLimit(currentHistoryDepthwindow());
    }

    function maxHistoryAggInterval () {
        return depthService.maxIntervalLimit(currentHistoryDepthwindow());
    }

    function currentHistoryDepthwindow() {
        if (vm.depthwindow.history.historyType === 0) {
            return vm.depthwindow.history.depthwindowFt;
        } else {
            return vm.depthwindow.history.fixedDepthwindow.endDepthFt -
                vm.depthwindow.history.fixedDepthwindow.startDepthFt;
        }
    }

}

