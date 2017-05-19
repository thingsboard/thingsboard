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
export default function AddWidgetToDashboardDialogController($scope, $mdDialog, $state, itembuffer, dashboardService, entityId, entityType, entityName, widget) {

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

    function add() {
        $scope.theForm.$setPristine();
        if (vm.addToDashboardType === 0) {
            dashboardService.getDashboard(vm.dashboardId).then(
                function success(dashboard) {
                    addWidgetToDashboard(dashboard);
                },
                function fail() {}
            );
        } else {
            addWidgetToDashboard(vm.newDashboard);
        }

    }

    function addWidgetToDashboard(theDashboard) {
        var aliasesInfo = {
            datasourceAliases: {},
            targetDeviceAliases: {}
        };
        aliasesInfo.datasourceAliases[0] = {
            aliasName: entityName,
            entityType: entityType,
            entityFilter: {
                useFilter: false,
                entityNameFilter: '',
                entityList: [entityId]
            }
        };
        theDashboard = itembuffer.addWidgetToDashboard(theDashboard, vm.widget, aliasesInfo, null, 48, -1, -1);
        dashboardService.saveDashboard(theDashboard).then(
            function success(dashboard) {
                $mdDialog.hide();
                if (vm.openDashboard) {
                    $state.go('home.dashboards.dashboard', {dashboardId: dashboard.id.id});
                }
            }
        );
    }

}
