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

import ruleNodeTemplate from './rulenode.tpl.html';
import ruleChainsTemplate from './rulechains.tpl.html';
import ruleChainTemplate from './rulechain.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleChainRoutes($stateProvider, NodeTemplatePathProvider, types) {

    NodeTemplatePathProvider.setTemplatePath(ruleNodeTemplate);

    $stateProvider
        .state('home.ruleChains', {
            url: '/ruleChains',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: ruleChainsTemplate,
                    controllerAs: 'vm',
                    controller: 'RuleChainsController'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'rulechain.rulechains',
                ruleChainsType: 'tenant'
            },
            ncyBreadcrumb: {
                label: '{"icon": "settings_ethernet", "label": "rulechain.rulechains"}'
            }
        }).state('home.ruleChains.ruleChain', {
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
                        return ruleChainService.getRuleNodeComponents(types.ruleChainType.core);
                    }
            },
            data: {
                import: false,
                searchEnabled: true,
                pageTitle: 'rulechain.rulechain'
            },
            ncyBreadcrumb: {
                label: '{"icon": "settings_ethernet", "label": "{{ vm.ruleChain.name + (vm.ruleChain.root ? (\' (\' + (\'rulechain.root\' | translate) + \')\') : \'\') }}", "translate": "false"}'
            }
    }).state('home.ruleChains.importRuleChain', {
        url: '/ruleChain/import',
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
        params: {
            ruleChainImport: {},
            ruleChainType: {}
        },
        resolve: {
            ruleChain:
            /*@ngInject*/
                function($stateParams) {
                    return $stateParams.ruleChainImport.ruleChain;
                },
            ruleChainMetaData:
            /*@ngInject*/
                function($stateParams) {
                    return $stateParams.ruleChainImport.metadata;
                },
            ruleNodeComponents:
            /*@ngInject*/
                function($stateParams, ruleChainService) {
                    return ruleChainService.getRuleNodeComponents($stateParams.ruleChainType);
                }
        },
        data: {
            import: true,
            searchEnabled: true,
            pageTitle: 'rulechain.rulechain'
        },
        ncyBreadcrumb: {
            label: '{"icon": "settings_ethernet", "label": "{{ (\'rulechain.import\' | translate) + \': \'+ vm.ruleChain.name }}", "translate": "false"}'
        }
    }).state('home.edges.ruleChains', {
        url: '/ruleChains',
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
            pageTitle: 'edge.rulechain-templates',
            ruleChainsType: 'edges'
        },
        ncyBreadcrumb: {
            label: '{"icon": "settings_ethernet", "label": "edge.rulechain-templates"}'
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
            pageTitle: 'edge.rulechain-template'
        },
        ncyBreadcrumb: {
            label: '{"icon": "settings_ethernet", "label": "{{ vm.ruleChain.name }}", "translate": "false"}'
        }
    }).state('home.edges.ruleChains.importRuleChain', {
        url: '/edges/ruleChains/import',
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
        params: {
            ruleChainImport: {},
            ruleChainType: {}
        },
        resolve: {
            ruleChain:
            /*@ngInject*/
                function($stateParams) {
                    return $stateParams.ruleChainImport.ruleChain;
                },
            ruleChainMetaData:
            /*@ngInject*/
                function($stateParams) {
                    return $stateParams.ruleChainImport.metadata;
                },
            ruleNodeComponents:
            /*@ngInject*/
                function($stateParams, ruleChainService) {
                    return ruleChainService.getRuleNodeComponents($stateParams.ruleChainType);
                }
        },
        data: {
            import: true,
            searchEnabled: true,
            pageTitle: 'edge.rulechain-template'
        },
        ncyBreadcrumb: {
            label: '{"icon": "settings_ethernet", "label": "{{ (\'rulechain.import\' | translate) + \': \'+ vm.ruleChain.name }}", "translate": "false"}'
        }
    });
}
