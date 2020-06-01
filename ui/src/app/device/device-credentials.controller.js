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
export default function ManageDeviceCredentialsController(deviceService, $scope, $mdDialog, deviceId, isReadOnly) {

    var vm = this;

    vm.credentialsTypes = [
        {
            name: 'Access token',
            value: 'ACCESS_TOKEN'
        },
        {
            name: 'X.509 Certificate',
            value: 'X509_CERTIFICATE'
        },
        {
            name: 'LwM2M Credentials',
            value: 'LWM2M_CREDENTIALS'
        }
    ];

    vm.deviceCredentials = {};
    vm.isReadOnly = isReadOnly;

    vm.valid = valid;
    vm.cancel = cancel;
    vm.save = save;
    vm.clear = clear;

    loadDeviceCredentials();

    function loadDeviceCredentials() {
        deviceService.getDeviceCredentials(deviceId).then(function success(deviceCredentials) {
            vm.deviceCredentials = deviceCredentials;
        });
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function valid() {
        return vm.deviceCredentials &&
               (vm.deviceCredentials.credentialsType === 'ACCESS_TOKEN'
                   && vm.deviceCredentials.credentialsId
                   && vm.deviceCredentials.credentialsId.length > 0
                   || vm.deviceCredentials.credentialsType === 'X509_CERTIFICATE'
                   && vm.deviceCredentials.credentialsValue
                   && vm.deviceCredentials.credentialsValue.length > 0
                   || vm.deviceCredentials.credentialsType === 'LWM2M_CREDENTIALS'
                   && vm.deviceCredentials.credentialsId
                   && vm.deviceCredentials.credentialsId.length > 0
                   && vm.deviceCredentials.credentialsValue
                   && vm.deviceCredentials.credentialsValue.length > 0)
    }

    function clear() {
        vm.deviceCredentials.credentialsId = null;
        vm.deviceCredentials.credentialsValue = null;
    }

    function save() {
        deviceService.saveDeviceCredentials(vm.deviceCredentials).then(function success(deviceCredentials) {
            vm.deviceCredentials = deviceCredentials;
            $scope.theForm.$setPristine();
            $mdDialog.hide();
        });
    }
}
