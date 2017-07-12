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
export default function WidgetActionDialogController($scope, $mdDialog, $filter, $q, dashboardService, dashboardUtils, types, utils,
                                                     isAdd, fetchDashboardStates, actionSources, widgetActions, action) {

    var vm = this;

    vm.types = types;

    vm.isAdd = isAdd;
    vm.fetchDashboardStates = fetchDashboardStates;
    vm.actionSources = actionSources;
    vm.widgetActions = widgetActions;

    vm.targetDashboardStateSearchText = '';

    vm.selectedDashboardStateIds = [];

    if (vm.isAdd) {
        vm.action = {
            id: utils.guid()
        };
    } else {
        vm.action = action;
    }

    vm.actionSourceName = actionSourceName;

    vm.targetDashboardStateSearchTextChanged = function() {
    }

    vm.dashboardStateSearch = dashboardStateSearch;
    vm.cancel = cancel;
    vm.save = save;

    $scope.$watch("vm.action.name", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.action.name != null) {
            checkActionName();
        }
    });

    $scope.$watch("vm.action.actionSourceId", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.action.actionSourceId != null) {
            checkActionName();
        }
    });

    $scope.$watch("vm.action.targetDashboardId", function() {
        vm.selectedDashboardStateIds = [];
        if (vm.action.targetDashboardId) {
            dashboardService.getDashboard(vm.action.targetDashboardId).then(
                function success(dashboard) {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    var states = dashboard.configuration.states;
                    vm.selectedDashboardStateIds = Object.keys(states);
                }
            );
        }
    });

    $scope.$watch('vm.action.type', function(newType) {
        if (newType) {
            switch (newType) {
                case vm.types.widgetActionTypes.openDashboardState.value:
                case vm.types.widgetActionTypes.updateDashboardState.value:
                case vm.types.widgetActionTypes.openDashboard.value:
                    if (angular.isUndefined(vm.action.setEntityId)) {
                        vm.action.setEntityId = true;
                    }
                    break;
            }
        }
    });

    function checkActionName() {
        var actionNameIsUnique = true;
        if (vm.action.actionSourceId && vm.action.name) {
            var sourceActions = vm.widgetActions[vm.action.actionSourceId];
            if (sourceActions) {
                var result = $filter('filter')(sourceActions, {name: vm.action.name}, true);
                if (result && result.length && result[0].id !== vm.action.id) {
                    actionNameIsUnique = false;
                }
            }
        }
        $scope.theForm.name.$setValidity('actionNameNotUnique', actionNameIsUnique);
    }

    function actionSourceName (actionSource) {
        if (actionSource) {
            return utils.customTranslation(actionSource.name, actionSource.name);
        } else {
            return '';
        }
    }

    function dashboardStateSearch (query) {
        if (vm.action.type == vm.types.widgetActionTypes.openDashboard.value) {
            var deferred = $q.defer();
            var result = query ? vm.selectedDashboardStateIds.filter(
                createFilterForDashboardState(query)) : vm.selectedDashboardStateIds;
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([query]);
            }
            return deferred.promise;
        } else {
            return vm.fetchDashboardStates({query: query});
        }
    }

    function createFilterForDashboardState (query) {
        var lowercaseQuery = angular.lowercase(query);
        return function filterFn(stateId) {
            return (angular.lowercase(stateId).indexOf(lowercaseQuery) === 0);
        };
    }

    function cleanupAction(action) {
        var result = {};
        result.id = action.id;
        result.actionSourceId = action.actionSourceId;
        result.name = action.name;
        result.icon = action.icon;
        result.type = action.type;
        switch (action.type) {
            case vm.types.widgetActionTypes.openDashboardState.value:
            case vm.types.widgetActionTypes.updateDashboardState.value:
                result.targetDashboardStateId = action.targetDashboardStateId;
                result.openRightLayout = action.openRightLayout;
                result.setEntityId = action.setEntityId;
                result.stateEntityParamName = action.stateEntityParamName;
                break;
            case vm.types.widgetActionTypes.openDashboard.value:
                result.targetDashboardId = action.targetDashboardId;
                result.targetDashboardStateId = action.targetDashboardStateId;
                result.setEntityId = action.setEntityId;
                result.stateEntityParamName = action.stateEntityParamName;
                break;
            case vm.types.widgetActionTypes.custom.value:
                result.customFunction = action.customFunction;
                break;
        }
        return result;
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {
        $scope.theForm.$setPristine();
        $mdDialog.hide(cleanupAction(vm.action));
    }
}
