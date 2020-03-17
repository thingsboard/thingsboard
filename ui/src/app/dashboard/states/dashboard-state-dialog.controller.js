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
export default function DashboardStateDialogController($scope, $mdDialog, $filter, dashboardUtils, isAdd, allStates, state) {

    var vm = this;

    vm.isAdd = isAdd;
    vm.allStates = allStates;
    vm.state = state;

    vm.stateIdTouched = false;

    if (vm.isAdd) {
        vm.state = dashboardUtils.createDefaultState('', false);
        vm.state.id = '';
        vm.prevStateId = '';
    } else {
        vm.state = state;
        vm.prevStateId = vm.state.id;
    }

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.state.name", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.state.name != null) {
            checkStateName();
        }
    });

    $scope.$watch("vm.state.id", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.state.id != null) {
            checkStateId();
        }
    });

    function checkStateName() {
        if (!vm.stateIdTouched && vm.isAdd) {
            vm.state.id = vm.state.name.toLowerCase().replace(/\W/g,"_");
        }
    }

    function checkStateId() {
        var result = $filter('filter')(vm.allStates, {id: vm.state.id}, true);
        if (result && result.length && result[0].id !== vm.prevStateId) {
            $scope.theForm.stateId.$setValidity('stateExists', false);
        } else {
            $scope.theForm.stateId.$setValidity('stateExists', true);
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        vm.state.id = vm.state.id.trim();
        $mdDialog.hide(vm.state);
    }
}
