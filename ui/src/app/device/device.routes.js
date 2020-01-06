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

import devicesTemplate from './devices.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DeviceRoutes($stateProvider, types) {
    $stateProvider
        .state('home.devices', {
            url: '/devices',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: devicesTemplate,
                    controller: 'DeviceController',
                    controllerAs: 'vm'
                }
            },
            data: {
                devicesType: 'tenant',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.device,
                pageTitle: 'device.devices'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "device.devices"}'
            }
        })
        .state('home.customers.devices', {
            url: '/:customerId/devices',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: devicesTemplate,
                    controllerAs: 'vm',
                    controller: 'DeviceController'
                }
            },
            data: {
                devicesType: 'customer',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.device,
                pageTitle: 'customer.devices'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "{{ vm.customerDevicesTitle }}", "translate": "false"}'
            }
        });

}
