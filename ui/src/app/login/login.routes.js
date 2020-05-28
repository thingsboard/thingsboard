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
/* eslint-disable import/no-unresolved, import/default */

import loginTemplate from './login.tpl.html';
import resetPasswordTemplate from './reset-password.tpl.html';
import resetPasswordRequestTemplate from './reset-password-request.tpl.html';
import createPasswordTemplate from './create-password.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function LoginRoutes($stateProvider) {
    $stateProvider.state('login', {
        url: '/login',
        module: 'public',
        views: {
            "@": {
                controller: 'LoginController',
                controllerAs: 'vm',
                templateUrl: loginTemplate
            }
        },
        data: {
            pageTitle: 'login.login'
        }
    }).state('login.resetPasswordRequest', {
        url: '/resetPasswordRequest',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordRequestController',
                controllerAs: 'vm',
                templateUrl: resetPasswordRequestTemplate
            }
        },
        data: {
            pageTitle: 'login.request-password-reset'
        }
    }).state('login.resetPassword', {
        url: '/resetPassword?resetToken',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordController',
                controllerAs: 'vm',
                templateUrl: resetPasswordTemplate
            }
        },
        data: {
            pageTitle: 'login.reset-password'
        }
    }).state('login.resetExpiredPassword', {
        url: '/resetExpiredPassword?resetToken',
        module: 'public',
        views: {
            "@": {
                controller: 'ResetPasswordController',
                controllerAs: 'vm',
                templateUrl: resetPasswordTemplate
            }
        },
        data: {
            expiredPassword: true,
            pageTitle: 'login.reset-password'
        }
    }).state('login.createPassword', {
        url: '/createPassword?activateToken',
        module: 'public',
        views: {
            "@": {
                controller: 'CreatePasswordController',
                controllerAs: 'vm',
                templateUrl: createPasswordTemplate
            }
        },
        data: {
            pageTitle: 'login.create-password'
        }
    });
}
