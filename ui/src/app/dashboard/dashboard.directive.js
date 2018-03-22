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

import dashboardFieldsetTemplate from './dashboard-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardDirective($compile, $templateCache, $translate, types, toast, dashboardService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(dashboardFieldsetTemplate);
        element.html(template);
        scope.publicLink = null;
        scope.$watch('dashboard', function(newVal) {
            if (newVal) {
                if (scope.dashboard.publicCustomerId) {
                    scope.publicLink = dashboardService.getPublicDashboardLink(scope.dashboard);
                } else {
                    scope.publicLink = null;
                }
            }
        });

        scope.onPublicLinkCopied = function() {
            toast.showSuccess($translate.instant('dashboard.public-link-copied-message'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            dashboard: '=',
            isEdit: '=',
            customerId: '=',
            dashboardScope: '=',
            theForm: '=',
            onMakePublic: '&',
            onMakePrivate: '&',
            onManageAssignedCustomers: '&',
            onUnassignFromCustomer: '&',
            onExportDashboard: '&',
            onDeleteDashboard: '&'
        }
    };
}
