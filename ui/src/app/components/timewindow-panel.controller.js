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
export default function TimewindowPanelController(mdPanelRef, $scope, timeService, types, timewindow, historyOnly, aggregation, isEdit, isToolbar, onTimewindowUpdate) {

    var vm = this;

    vm._mdPanelRef = mdPanelRef;
    vm.timewindow = timewindow;
    vm.historyOnly = historyOnly;
    vm.aggregation = aggregation;
    vm.onTimewindowUpdate = onTimewindowUpdate;
    vm.aggregationTypes = types.aggregation;
    vm.showLimit = showLimit;
    vm.showRealtimeAggInterval = showRealtimeAggInterval;
    vm.showHistoryAggInterval = showHistoryAggInterval;
    vm.minRealtimeAggInterval = minRealtimeAggInterval;
    vm.maxRealtimeAggInterval = maxRealtimeAggInterval;
    vm.minHistoryAggInterval = minHistoryAggInterval;
    vm.maxHistoryAggInterval = maxHistoryAggInterval;
    vm.minDatapointsLimit = minDatapointsLimit;
    vm.maxDatapointsLimit = maxDatapointsLimit;    
    vm.minTimeRatioLimit = minTimeRatioLimit;
    vm.maxTimeRatioLimit = maxTimeRatioLimit;
    vm.isEdit = isEdit;
    vm.isToolbar = isToolbar;

    if (vm.historyOnly) {
        vm.timewindow.selectedTab = 1;
    }

    vm._mdPanelRef.config.onOpenComplete = function () {
        $scope.theForm.$setPristine();
    }

    $scope.$watch('vm.timewindow.selectedTab', function (newSelection, prevSelection) {
        if (newSelection !== prevSelection) {
            $scope.theForm.$setDirty();
        }
    });

    vm.cancel = function () {
        vm._mdPanelRef && vm._mdPanelRef.close();
    };

    vm.update = function () {
        vm._mdPanelRef && vm._mdPanelRef.close().then(function () {
            vm.onTimewindowUpdate && vm.onTimewindowUpdate(vm.timewindow);
        });
    };

    function showLimit() {
        return vm.timewindow.aggregation.type === vm.aggregationTypes.none.value;
    }

    function showRealtimeAggInterval() {
        return vm.timewindow.aggregation.type !== vm.aggregationTypes.none.value &&
               vm.timewindow.selectedTab === 0;
    }

    function showHistoryAggInterval() {
        return vm.timewindow.aggregation.type !== vm.aggregationTypes.none.value &&
            vm.timewindow.selectedTab === 1;
    }

    function minRealtimeAggInterval () {
        return timeService.minIntervalLimit(vm.timewindow.realtime.timewindowMs);
    }

    function maxRealtimeAggInterval () {
        return timeService.maxIntervalLimit(vm.timewindow.realtime.timewindowMs);
    }

    function minHistoryAggInterval () {
        return timeService.minIntervalLimit(currentHistoryTimewindow());
    }

    function maxHistoryAggInterval () {
        return timeService.maxIntervalLimit(currentHistoryTimewindow());
    }

    function minDatapointsLimit () {
        return timeService.getMinDatapointsLimit();
    }

    function maxDatapointsLimit () {
        return timeService.getMaxDatapointsLimit();
    }

    function minTimeRatioLimit () {
        return timeService.getMinTimeRatioLimit();
    }

    function maxTimeRatioLimit () {
        return timeService.getMaxTimeRatioLimit();
    }

    function currentHistoryTimewindow() {
        if (vm.timewindow.history.historyType === 0) {
            return vm.timewindow.history.timewindowMs;
        } else {
            return vm.timewindow.history.fixedTimewindow.endTimeMs -
                vm.timewindow.history.fixedTimewindow.startTimeMs;
        }
    }

}

