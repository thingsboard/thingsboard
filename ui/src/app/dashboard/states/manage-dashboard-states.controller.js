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

import './manage-dashboard-states.scss';

/* eslint-disable import/no-unresolved, import/default */

import dashboardStateDialogTemplate from './dashboard-state-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ManageDashboardStatesController($scope, $mdDialog, $filter, $document, $translate, states) {

    var vm = this;

    vm.allStates = [];
    for (var id in states) {
        var state = states[id];
        state.id = id;
        vm.allStates.push(state);
    }

    vm.states = [];
    vm.statesCount = 0;

    vm.query = {
        order: 'name',
        limit: 5,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addState = addState;
    vm.editState = editState;
    vm.deleteState = deleteState;

    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateStates();
        }
    });

    updateStates ();

    function updateStates () {
        var result = $filter('orderBy')(vm.allStates, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.statesCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.states = result.slice(startIndex, startIndex + vm.query.limit);
    }

    function enterFilterMode () {
        vm.query.search = '';
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateStates();
    }

    function onReorder () {
        updateStates();
    }

    function onPaginate () {
        updateStates();
    }

    function addState ($event) {
        openStateDialog($event, null, true);
    }

    function editState ($event, alertRule) {
        if ($event) {
            $event.stopPropagation();
        }
        openStateDialog($event, alertRule, false);
    }

    function openStateDialog($event, state, isAdd) {
        var prevStateId = null;
        if (!isAdd) {
            prevStateId = state.id;
        }
        $mdDialog.show({
            controller: 'DashboardStateDialogController',
            controllerAs: 'vm',
            templateUrl: dashboardStateDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {isAdd: isAdd, allStates: vm.allStates, state: angular.copy(state)},
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (state) {
            saveState(state, prevStateId);
            updateStates();
        });
    }

    function getStateIndex(id) {
        var result = $filter('filter')(vm.allStates, {id: id}, true);
        if (result && result.length) {
            return vm.allStates.indexOf(result[0]);
        }
        return -1;
    }

    function saveState(state, prevStateId) {
        if (prevStateId) {
            var index = getStateIndex(prevStateId);
            if (index > -1) {
                vm.allStates[index] = state;
            }
        } else {
            vm.allStates.push(state);
        }
        if (state.root) {
            for (var i=0; i < vm.allStates.length; i++) {
                var otherState = vm.allStates[i];
                if (otherState.id !== state.id) {
                    otherState.root = false;
                }
            }
        }
        $scope.theForm.$setDirty();
    }

    function deleteState ($event, state) {
        if ($event) {
            $event.stopPropagation();
        }
        if (state) {
            var title = $translate.instant('dashboard.delete-state-title');
            var content = $translate.instant('dashboard.delete-state-text', {stateName: state.name});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));

            confirm._options.skipHide = true;
            confirm._options.fullscreen = true;

            $mdDialog.show(confirm).then(function () {
                var index = getStateIndex(state.id);
                if (index > -1) {
                    vm.allStates.splice(index, 1);
                }
                $scope.theForm.$setDirty();
                updateStates();
            });
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        var savedStates = {};
        for (var i=0;i<vm.allStates.length;i++) {
            var state = vm.allStates[i];
            var id = state.id;
            delete state.id;
            savedStates[id] = state;
        }
        $mdDialog.hide(savedStates);
    }
}
