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
import thingsboardDatakeyConfig from './datakey-config.directive';

export default angular.module('thingsboard.dialogs.datakeyConfigDialog', [thingsboardDatakeyConfig])
    .controller('DatakeyConfigDialogController', DatakeyConfigDialogController)
    .name;

/*@ngInject*/
function DatakeyConfigDialogController($scope, $mdDialog, deviceService, dataKey, dataKeySettingsSchema, deviceAlias, deviceAliases) {

    var vm = this;

    vm.dataKey = dataKey;
    vm.dataKeySettingsSchema = dataKeySettingsSchema;
    vm.deviceAlias = deviceAlias;
    vm.deviceAliases = deviceAliases;

    vm.hide = function () {
        $mdDialog.hide();
    };

    vm.cancel = function () {
        $mdDialog.cancel();
    };

    vm.fetchDeviceKeys = function (deviceAliasId, query, type) {
        var deviceId = vm.deviceAliases[deviceAliasId];
        if (deviceId) {
            return deviceService.getDeviceKeys(deviceId, query, type);
        } else {
            return [];
        }
    };

    vm.save = function () {
        $scope.$broadcast('form-submit');
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            $mdDialog.hide(vm.dataKey);
        }
    };
}