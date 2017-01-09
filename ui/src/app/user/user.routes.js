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

import usersTemplate from '../user/users.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function UserRoutes($stateProvider) {

    $stateProvider
        .state('home.tenants.users', {
            url: '/:tenantId/users',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: usersTemplate,
                    controllerAs: 'vm',
                    controller: 'UserController'
                }
            },
            data: {
                usersType: 'tenant',
                searchEnabled: true,
                pageTitle: 'user.tenant-admins'
            },
            ncyBreadcrumb: {
                label: '{"icon": "account_circle", "label": "user.tenant-admins"}'
            }
        })
        .state('home.customers.users', {
            url: '/:customerId/users',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: usersTemplate,
                    controllerAs: 'vm',
                    controller: 'UserController'
                }
            },
            data: {
                usersType: 'customer',
                searchEnabled: true,
                pageTitle: 'user.customer-users'
            },
            ncyBreadcrumb: {
                label: '{"icon": "account_circle", "label": "user.customer-users"}'
            }
        });

}
