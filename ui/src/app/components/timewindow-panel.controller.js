/*
 * Copyright © 2016 The Thingsboard Authors
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
export default function TimewindowPanelController(mdPanelRef, $scope, timewindow, historyOnly, onTimewindowUpdate) {

    var vm = this;

    vm._mdPanelRef = mdPanelRef;
    vm.timewindow = timewindow;
    vm.historyOnly = historyOnly;
    vm.onTimewindowUpdate = onTimewindowUpdate;

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
}
