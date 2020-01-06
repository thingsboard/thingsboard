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

import widgetLibraryTemplate from './widget-library.tpl.html';
import widgetEditorTemplate from './widget-editor.tpl.html';
import dashboardTemplate from '../dashboard/dashboard.tpl.html';
import widgetsBundlesTemplate from './widgets-bundles.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function WidgetLibraryRoutes($stateProvider) {
    $stateProvider
        .state('home.widgets-bundles', {
            url: '/widgets-bundles',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetsBundlesTemplate,
                    controller: 'WidgetsBundleController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: true,
                pageTitle: 'widgets-bundle.widgets-bundles'
            },
            ncyBreadcrumb: {
                label: '{"icon": "now_widgets", "label": "widgets-bundle.widgets-bundles"}'
            }
        })
        .state('home.widgets-bundles.widget-types', {
            url: '/:widgetsBundleId/widgetTypes',
            params: {'topIndex': 0},
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetLibraryTemplate,
                    controller: 'WidgetLibraryController',
                    controllerAs: 'vm'
                }
            },
            data: {
                searchEnabled: false,
                pageTitle: 'widget.widget-library'
            },
            ncyBreadcrumb: {
                label: '{"icon": "now_widgets", "label": "{{ vm.widgetsBundle.title }}", "translate": "false"}'
            }
        })
        .state('home.widgets-bundles.widget-types.widget-type', {
            url: '/:widgetTypeId',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "content@home": {
                    templateUrl: widgetEditorTemplate,
                    controller: 'WidgetEditorController',
                    controllerAs: 'vm'
                }
            },
            params: {
                widgetType: null
            },
            data: {
                searchEnabled: false,
                pageTitle: 'widget.editor'
            },
            ncyBreadcrumb: {
                label: '{"icon": "insert_chart", "label": "{{ vm.widget.widgetName }}", "translate": "false"}'
            }
        })
        .state('widgetEditor', {
            url: '/widget-editor',
            module: 'private',
            auth: ['SYS_ADMIN', 'TENANT_ADMIN'],
            views: {
                "@": {
                    templateUrl: dashboardTemplate,
                    controller: 'DashboardController',
                    controllerAs: 'vm'
                }
            },
            data: {
                widgetEditMode: true,
                searchEnabled: false,
                pageTitle: 'widget.editor'
            }
        })
}
