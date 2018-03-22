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

import customersTemplate from './customers.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CustomerRoutes($stateProvider) {

    $stateProvider
        .state('home.customers', {
            url: '/customers',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: customersTemplate,
                    controllerAs: 'vm',
                    controller: 'CustomerController'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'customer.customers'
            },
            ncyBreadcrumb: {
                label: '{"icon": "supervisor_account", "label": "customer.customers"}'
            }
        });

}
