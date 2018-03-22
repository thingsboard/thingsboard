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
import './user-menu.scss';

/* eslint-disable import/no-unresolved, import/default */

import userMenuTemplate from './user-menu.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.usermenu', [])
    .directive('tbUserMenu', UserMenu)
    .name;

/*@ngInject*/
function UserMenu() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            displayUserInfo: '=',
        },
        controller: UserMenuController,
        controllerAs: 'vm',
        templateUrl: userMenuTemplate
    };
}

/*@ngInject*/
function UserMenuController($scope, userService, $translate, $state) {

    var vm = this;

    var dashboardUser = userService.getCurrentUser();

    vm.authorityName = authorityName;
    vm.logout = logout;
    vm.openProfile = openProfile;
    vm.userDisplayName = userDisplayName;

    function authorityName() {
        var name = "user.anonymous";
        if (dashboardUser) {
            var authority = dashboardUser.authority;
            if (authority === 'SYS_ADMIN') {
                name = 'user.sys-admin';
            } else if (authority === 'TENANT_ADMIN') {
                name = 'user.tenant-admin';
            } else if (authority === 'CUSTOMER_USER') {
                name = 'user.customer';
            }
        }
        return $translate.instant(name);
    }

    function userDisplayName() {
        var name = "";
        if (dashboardUser) {
            if ((dashboardUser.firstName && dashboardUser.firstName.length > 0) ||
                (dashboardUser.lastName && dashboardUser.lastName.length > 0)) {
                if (dashboardUser.firstName) {
                    name += dashboardUser.firstName;
                }
                if (dashboardUser.lastName) {
                    if (name.length > 0) {
                        name += " ";
                    }
                    name += dashboardUser.lastName;
                }
            } else {
                name = dashboardUser.email;
            }
        }
        return name;
    }

    function openProfile() {
        $state.go('home.profile');
    }

    function logout() {
        userService.logout();
    }
}