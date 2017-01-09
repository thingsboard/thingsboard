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

import rulesTemplate from './rules.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleRoutes($stateProvider) {

    $stateProvider
        .state('home.rules', {
            url: '/rules',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: rulesTemplate,
                    controllerAs: 'vm',
                    controller: 'RuleController'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'rule.rules'
            },
            ncyBreadcrumb: {
                label: '{"icon": "settings_ethernet", "label": "rule.rules"}'
            }
        });
}
