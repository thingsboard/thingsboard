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

import jsonFormTemplate from './jsonform.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function JsonFormRoutes($stateProvider) {
    $stateProvider
        .state('home.jsonform', {
            url: '/jsonform',
            module: 'private',
            auth: ['SYS_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: jsonFormTemplate,
                    controllerAs: 'vm',
                    controller: 'JsonFormController'
                }
            },
            data: {
                key: 'general',
                pageTitle: 'admin.general-settings'
            },
            ncyBreadcrumb: {
                label: '{"icon": "settings", "label": "admin.system-settings"}'
            }
        });
}
