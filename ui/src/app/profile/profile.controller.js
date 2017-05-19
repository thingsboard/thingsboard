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
/* eslint-disable import/no-unresolved, import/default */

import changePasswordTemplate from './change-password.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function ProfileController(userService, $scope, $document, $mdDialog, $translate) {
    var vm = this;

    vm.profileUser = {};

    vm.save = save;
    vm.changePassword = changePassword;
    vm.languageList = {
        en_US: {value : "en_US", name: "language.en_US"}, 
        ko_KR: {value : "ko_KR", name: "language.ko_KR"},
        zh_CN: {value : "zh_CN", name: "language.zh_CN"},
        ru_RU: {value : "ru_RU", name: "language.ru_RU"},
        es_ES: {value : "es_ES", name: "language.es_ES"},
    };

    loadProfile();

    function loadProfile() {
        userService.getUser(userService.getCurrentUser().userId).then(function success(user) {
            vm.profileUser = user;
            vm.profileUser.lang = $translate.use();
        });
    }

    function save() {
        userService.saveUser(vm.profileUser).then(function success(user) {
            $translate.use(vm.profileUser.lang);
            vm.profileUser = user;
            vm.profileUser.lang = $translate.use();
            $scope.theForm.$setPristine();
        });
    }

    function changePassword($event) {
        $mdDialog.show({
            controller: 'ChangePasswordController',
            controllerAs: 'vm',
            templateUrl: changePasswordTemplate,
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
        }, function () {
        });
    }
}
