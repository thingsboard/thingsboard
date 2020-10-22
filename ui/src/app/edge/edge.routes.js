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
import ruleChainsTemplate from "../rulechain/rulechains.tpl.html";
import ruleChainTemplate from "../rulechain/rulechain.tpl.html";

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
        }).state('home.edges.entityViews', {
            url: '/:edgeId/entityViews',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
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
        }).state('home.edges.devices', {
            url: '/:edgeId/devices',
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
                devicesType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.device,
                pageTitle: 'edge.devices'
            },
            ncyBreadcrumb: {
                label: '{"icon": "devices_other", "label": "edge.devices"}'
            }
        }).state('home.edges.assets', {
            url: '/:edgeId/assets',
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
                assetsType: 'edge',
                searchEnabled: true,
                searchByEntitySubtype: true,
                searchEntityType: types.entityType.asset,
                pageTitle: 'edge.assets'
            },
            ncyBreadcrumb: {
                label: '{"icon": "domain", "label": "edge.assets"}'
            }
        }).state('home.edges.dashboards', {
            url: '/:edgeId/dashboards',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
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
        }).state('home.edges.dashboards.dashboard', {
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
        }).state('home.edges.ruleChains', {
            url: '/:edgeId/ruleChains',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: ruleChainsTemplate,
                    controllerAs: 'vm',
                    controller: 'RuleChainsController'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'edge.rulechains',
                ruleChainsType: 'edge'
            },
            ncyBreadcrumb: {
                label: '{"icon": "code", "label": "rulechain.edge-rulechains"}'
            }
        }).state('home.edges.ruleChains.ruleChain', {
            url: '/:ruleChainId',
            reloadOnSearch: false,
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: ruleChainTemplate,
                    controller: 'RuleChainController',
                    controllerAs: 'vm'
                }
            },
            resolve: {
                ruleChain:
                /*@ngInject*/
                    function($stateParams, ruleChainService) {
                        return ruleChainService.getRuleChain($stateParams.ruleChainId);
                    },
                ruleChainMetaData:
                /*@ngInject*/
                    function($stateParams, ruleChainService) {
                        return ruleChainService.getRuleChainMetaData($stateParams.ruleChainId);
                    },
                ruleNodeComponents:
                /*@ngInject*/
                    function($stateParams, ruleChainService) {
                        return ruleChainService.getRuleNodeComponents(types.ruleChainType.edge);
                    }
            },
            data: {
                import: false,
                searchEnabled: false,
                pageTitle: 'edge.rulechain'
            },
            ncyBreadcrumb: {
                label: '{"icon": "code", "label": "{{ vm.ruleChain.name }}", "translate": "false"}'
            }
        });
}
