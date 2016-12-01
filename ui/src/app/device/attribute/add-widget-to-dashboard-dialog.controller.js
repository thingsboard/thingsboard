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
export default function AddWidgetToDashboardDialogController($scope, $mdDialog, $state, dashboardService, deviceId, deviceName, widget) {

    var vm = this;

    vm.widget = widget;
    vm.dashboard = null;
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
        var theDashboard;
        var deviceAliases;
        widget.col = 0;
        widget.sizeX /= 2;
        widget.sizeY /= 2;
        if (vm.addToDashboardType === 0) {
            theDashboard = vm.dashboard;
            if (!theDashboard.configuration) {
                theDashboard.configuration = {};
            }
            deviceAliases = theDashboard.configuration.deviceAliases;
            if (!deviceAliases) {
                deviceAliases = {};
                theDashboard.configuration.deviceAliases = deviceAliases;
            }
            var newAliasId;
            for (var aliasId in deviceAliases) {
                if (deviceAliases[aliasId].deviceId === deviceId) {
                    newAliasId = aliasId;
                    break;
                }
            }
            if (!newAliasId) {
                var newAliasName = createDeviceAliasName(deviceAliases, deviceName);
                newAliasId = 0;
                for (aliasId in deviceAliases) {
                    newAliasId = Math.max(newAliasId, aliasId);
                }
                newAliasId++;
                deviceAliases[newAliasId] = {alias: newAliasName, deviceId: deviceId};
            }
            widget.config.datasources[0].deviceAliasId = newAliasId;

            if (!theDashboard.configuration.widgets) {
                theDashboard.configuration.widgets = [];
            }

            var row = 0;
            for (var w in theDashboard.configuration.widgets) {
                var existingWidget = theDashboard.configuration.widgets[w];
                var wRow = existingWidget.row ? existingWidget.row : 0;
                var wSizeY = existingWidget.sizeY ? existingWidget.sizeY : 1;
                var bottom = wRow + wSizeY;
                row = Math.max(row, bottom);
            }
            widget.row = row;
            theDashboard.configuration.widgets.push(widget);
        } else {
            theDashboard = vm.newDashboard;
            deviceAliases = {};
            deviceAliases['1'] = {alias: deviceName, deviceId: deviceId};
            theDashboard.configuration = {};
            theDashboard.configuration.widgets = [];
            widget.row = 0;
            theDashboard.configuration.widgets.push(widget);
            theDashboard.configuration.deviceAliases = deviceAliases;
        }
        dashboardService.saveDashboard(theDashboard).then(
            function success(dashboard) {
                $mdDialog.hide();
                if (vm.openDashboard) {
                    $state.go('home.dashboards.dashboard', {dashboardId: dashboard.id.id});
                }
            }
        );

    }

    function createDeviceAliasName(deviceAliases, alias) {
        var c = 0;
        var newAlias = angular.copy(alias);
        var unique = false;
        while (!unique) {
            unique = true;
            for (var devAliasId in deviceAliases) {
                var devAlias = deviceAliases[devAliasId];
                if (newAlias === devAlias.alias) {
                    c++;
                    newAlias = alias + c;
                    unique = false;
                }
            }
        }
        return newAlias;
    }

}
