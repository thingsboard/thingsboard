/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
/* eslint-disable import/no-unresolved, import/default */

import selectTargetStateTemplate from '../../dashboard/states/select-target-state.tpl.html';
import selectTargetLayoutTemplate from '../../dashboard/layouts/select-target-layout.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AddWidgetToDashboardDialogController($scope, $mdDialog, $state, $q, $document, dashboardUtils,
                                                             utils, types, itembuffer, dashboardService, entityId, entityType, entityName, widget) {

    var vm = this;

    vm.widget = widget;
    vm.dashboardId = null;
    vm.addToDashboardType = 0;
    vm.newDashboard = {};
    vm.openDashboard = false;

    vm.add = add;
    vm.cancel = cancel;

    function cancel() {
        $mdDialog.cancel();
    }

    function selectTargetState($event, dashboard) {
        var deferred = $q.defer();
        var states = dashboard.configuration.states;
        var stateIds = Object.keys(states);
        if (stateIds.length > 1) {
            $mdDialog.show({
                controller: 'SelectTargetStateController',
                controllerAs: 'vm',
                templateUrl: selectTargetStateTemplate,
                parent: angular.element($document[0].body),
                locals: {
                    states: states
                },
                fullscreen: true,
                skipHide: true,
                targetEvent: $event
            }).then(
                function success(stateId) {
                    deferred.resolve(stateId);
                },
                function fail() {
                    deferred.reject();
                }
            );

        } else {
            deferred.resolve(stateIds[0]);
        }
        return deferred.promise;
    }

    function selectTargetLayout($event, dashboard, targetState) {
        var deferred = $q.defer();
        var layouts = dashboard.configuration.states[targetState].layouts;
        var layoutIds = Object.keys(layouts);
        if (layoutIds.length > 1) {
            $mdDialog.show({
                controller: 'SelectTargetLayoutController',
                controllerAs: 'vm',
                templateUrl: selectTargetLayoutTemplate,
                parent: angular.element($document[0].body),
                fullscreen: true,
                skipHide: true,
                targetEvent: $event
            }).then(
                function success(layoutId) {
                    deferred.resolve(layoutId);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.resolve(layoutIds[0]);
        }
        return deferred.promise;
    }

    function add($event) {
        if (vm.addToDashboardType === 0) {
            dashboardService.getDashboard(vm.dashboardId).then(
                function success(dashboard) {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    selectTargetState($event, dashboard).then(
                        function(targetState) {
                            selectTargetLayout($event, dashboard, targetState).then(
                                function(targetLayout) {
                                    addWidgetToDashboard(dashboard, targetState, targetLayout);
                                }
                            );
                       }
                    );
                },
                function fail() {}
            );
        } else {
            addWidgetToDashboard(vm.newDashboard, 'default', 'main');
        }

    }

    function addWidgetToDashboard(theDashboard, targetState, targetLayout) {
        var aliasesInfo = {
            datasourceAliases: {},
            targetDeviceAliases: {}
        };
        aliasesInfo.datasourceAliases[0] = {
            alias: entityName,
            filter: dashboardUtils.createSingleEntityFilter(entityType, entityId)
        };
        itembuffer.addWidgetToDashboard(theDashboard, targetState, targetLayout, vm.widget, aliasesInfo, null, 48, null, -1, -1).then(
            function(theDashboard) {
                dashboardService.saveDashboard(theDashboard).then(
                    function success(dashboard) {
                        $scope.theForm.$setPristine();
                        $mdDialog.hide();
                        if (vm.openDashboard) {
                            var stateParams = {
                                dashboardId: dashboard.id.id
                            }
                            var stateIds = Object.keys(dashboard.configuration.states);
                            var stateIndex = stateIds.indexOf(targetState);
                            if (stateIndex > 0) {
                                stateParams.state = utils.objToBase64([ {id: targetState, params: {}} ]);
                            }
                            $state.go('home.dashboards.dashboard', stateParams);
                        }
                    }
                );
            }
        );
    }

}
