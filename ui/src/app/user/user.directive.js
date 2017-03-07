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

import './user-fieldset.scss';

/* eslint-disable import/no-unresolved, import/default */

import userFieldsetTemplate from './user-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function UserDirective($compile, $templateCache, dashboardService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(userFieldsetTemplate);
        element.html(template);

        scope.isCustomerUser = function() {
            return scope.user && scope.user.authority === 'CUSTOMER_USER';
        }

        scope.$watch('user', function(newUser, prevUser) {
            if (!angular.equals(newUser, prevUser) && newUser) {
                scope.defaultDashboard = null;
                if (scope.isCustomerUser() && scope.user.additionalInfo &&
                    scope.user.additionalInfo.defaultDashboardId) {
                    dashboardService.getDashboard(scope.user.additionalInfo.defaultDashboardId).then(
                        function(dashboard) {
                            scope.defaultDashboard = dashboard;
                        }
                    )
                }
            }
        });

        scope.$watch('defaultDashboard', function(newDashboard, prevDashboard) {
            if (!angular.equals(newDashboard, prevDashboard)) {
                if (scope.isCustomerUser()) {
                    if (!scope.user.additionalInfo) {
                        scope.user.additionalInfo = {};
                    }
                    scope.user.additionalInfo.defaultDashboardId = newDashboard ? newDashboard.id.id : null;
                }
            }
        });

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            user: '=',
            isEdit: '=',
            theForm: '=',
            onResendActivation: '&',
            onDeleteUser: '&'
        }
    };
}
