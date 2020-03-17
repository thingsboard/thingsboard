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

import edgesTemplate from './edges.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeRoutes($stateProvider, types) {
    $stateProvider
        .state('home.edges', {
            url: '/edges',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: edgesTemplate,
                    controller: 'EdgeController',
                    controllerAs: 'vm'
                }
            },
            data: {
                edgesType: 'tenant',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.edge,
                pageTitle: 'edge.edges'
            },
            ncyBreadcrumb: {
                label: '{"icon": "transform", "label": "edge.edges"}'
            }
        })
        .state('home.customers.edges', {
            url: '/:customerId/edges',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: edgesTemplate,
                    controllerAs: 'vm',
                    controller: 'EdgeController'
                }
            },
            data: {
                edgesType: 'customer',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.edge,
                pageTitle: 'customer.edges'
            },
            ncyBreadcrumb: {
                label: '{"icon": "transform", "label": "{{ vm.customerEdgesTitle }}", "translate": "false"}'
            }
        });
}
