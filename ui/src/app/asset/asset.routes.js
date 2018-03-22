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

import assetsTemplate from './assets.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AssetRoutes($stateProvider, types) {
    $stateProvider
        .state('home.assets', {
            url: '/assets',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: assetsTemplate,
                    controller: 'AssetController',
                    controllerAs: 'vm'
                }
            },
            data: {
                assetsType: 'tenant',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.asset,
                pageTitle: 'asset.assets'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "asset.assets"}'
            }
        })
        .state('home.customers.assets', {
            url: '/:customerId/assets',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: assetsTemplate,
                    controllerAs: 'vm',
                    controller: 'AssetController'
                }
            },
            data: {
                assetsType: 'customer',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.asset,
                pageTitle: 'customer.assets'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "{{ vm.customerAssetsTitle }}", "translate": "false"}'
            }
        });

}
