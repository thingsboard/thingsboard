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
import './settings-card.scss';

/*@ngInject*/
export default function AdminController(adminService, toast, $scope, $rootScope, $state, $translate) {

    var vm = this;
    vm.save = save;
    vm.sendTestMail = sendTestMail;
    vm.smtpProtocols = ('smtp smtps').split(' ').map(function (protocol) {
        return protocol;
    });

    vm.tlsVersions = ['TLSv1', 'TLSv1.1', 'TLSv1.2', 'TLSv1.3'];

    $translate('admin.test-mail-sent').then(function (translation) {
        vm.testMailSent = translation;
    }, function (translationId) {
        vm.testMailSent = translationId;
    });


    loadSettings();

    function loadSettings() {
        adminService.getAdminSettings($state.$current.data.key).then(function success(settings) {
            vm.settings = settings;
        });
    }

    function save() {
        adminService.saveAdminSettings(vm.settings).then(function success(settings) {
            vm.settings = settings;
            vm.settingsForm.$setPristine();
        });
    }

    function sendTestMail() {
        adminService.sendTestMail(vm.settings).then(function success() {
            toast.showSuccess($translate.instant('admin.test-mail-sent'));
        });
    }

}
