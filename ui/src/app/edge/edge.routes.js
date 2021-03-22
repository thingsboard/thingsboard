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
import entityViewsTemplate from "../entity-view/entity-views.tpl.html";
import devicesTemplate from "../device/devices.tpl.html";
import assetsTemplate from "../asset/assets.tpl.html";
import dashboardsTemplate from "../dashboard/dashboards.tpl.html";
import dashboardTemplate from "../dashboard/dashboard.tpl.html";

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeRoutes($stateProvider, types) {
    $stateProvider
        .state('home.edges', {
            url: '/edges',
            params: {'topIndex': 0},
            redirectTo: 'home.edges.instances',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            data: {
                pageTitle: 'edge.management'
            },
            ncyBreadcrumb: {
                label: '{"icon": "settings_input_antenna", "label": "edge.management"}'
            }
        }).state('home.edges.instances', {
            url: '/instances',
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
                pageTitle: 'edge.edge-instances'
            },
            ncyBreadcrumb: {
                label: '{"icon": "router", "label": "edge.edge-instances"}'
            }
        }).state('home.edges.instances.entityViews', {
            url: '/:edgeId/entityViews',
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: entityViewsTemplate,
                    controllerAs: 'vm',
                    controller: 'EntityViewController'
                }
            },
            data: {
                entityViewsType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.entityView,
                pageTitle: 'edge.entity-views'
            },
            ncyBreadcrumb: {
                label: '{"icon": "view_quilt", "label": "edge.entity-views"}'
            }
        }).state('home.edges.instances.devices', {
            url: '/:edgeId/devices',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: devicesTemplate,
                    controllerAs: 'vm',
                    controller: 'DeviceController'
                }
            },
            data: {
                devicesType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.device,
                pageTitle: 'edge.devices'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "edge.devices"}'
            }
        }).state('home.edges.instances.assets', {
            url: '/:edgeId/assets',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: assetsTemplate,
                    controllerAs: 'vm',
                    controller: 'AssetController'
                }
            },
            data: {
                assetsType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.asset,
                pageTitle: 'edge.assets'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "edge.assets"}'
            }
        }).state('home.edges.instances.dashboards', {
            url: '/:edgeId/dashboards',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: dashboardsTemplate,
                    controllerAs: 'vm',
                    controller: 'DashboardsController'
                }
            },
            data: {
                dashboardsType: 'edge',
                searchEnabled: true,
                pageTitle: 'edge.dashboards'
            },
            ncyBreadcrumb: {
                label: '{"icon": "dashboard", "label": "edge.dashboards"}'
            }
        }).state('home.edges.instances.dashboards.dashboard', {
            url: '/:dashboardId?state',
            reloadOnSearch: false,
            module: 'private',
            auth: ['TENANT_ADMIN', 'CUSTOMER_USER'],
            views: {
                "content@home": {
                    templateUrl: dashboardTemplate,
                    controller: 'DashboardController',
                    controllerAs: 'vm'
                }
            },
            data: {
                widgetEditMode: false,
                searchEnabled: false,
                pageTitle: 'dashboard.dashboard',
                dashboardsType: 'edge',
            },
            ncyBreadcrumb: {
                label: '{"icon": "dashboard", "label": "{{ vm.dashboard.title }}", "translate": "false"}'
            }
        }).state('home.customers.edges', {
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
                label: '{"icon": "router", "label": "{{ vm.customerEdgesTitle }}", "translate": "false"}'
            }
        });
}
