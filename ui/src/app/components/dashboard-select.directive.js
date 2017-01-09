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
import './dashboard-select.scss';

import thingsboardApiDashboard from '../api/dashboard.service';
import thingsboardApiUser from '../api/user.service';

/* eslint-disable import/no-unresolved, import/default */

import dashboardSelectTemplate from './dashboard-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.dashboardSelect', [thingsboardApiDashboard, thingsboardApiUser])
    .directive('tbDashboardSelect', DashboardSelect)
    .name;

/*@ngInject*/
function DashboardSelect($compile, $templateCache, $q, dashboardService, userService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(dashboardSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.dashboard = null;
        scope.dashboardSearchText = '';

        scope.dashboardFetchFunction = dashboardService.getTenantDashboards;
        if (angular.isDefined(scope.dashboardsScope)) {
            if (scope.dashboardsScope === 'customer') {
                scope.dashboardFetchFunction = dashboardService.getCustomerDashboards;
            } else {
                scope.dashboardFetchFunction = dashboardService.getTenantDashboards;
            }
        } else {
            if (userService.getAuthority() === 'TENANT_ADMIN') {
                scope.dashboardFetchFunction = dashboardService.getTenantDashboards;
            } else if (userService.getAuthority() === 'CUSTOMER_USER') {
                scope.dashboardFetchFunction = dashboardService.getCustomerDashboards;
            }
        }

        scope.fetchDashboards = function(searchText) {
            var pageLink = {limit: 10, textSearch: searchText};

            var deferred = $q.defer();

            scope.dashboardFetchFunction(pageLink).then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.dashboardSearchTextChanged = function() {
        }

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.dashboard);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.dashboard = ngModelCtrl.$viewValue;
            }
        }

        scope.$watch('dashboard', function () {
            scope.updateView();
        });

        if (scope.selectFirstDashboard) {
            var pageLink = {limit: 1, textSearch: ''};
            scope.dashboardFetchFunction(pageLink).then(function success(result) {
                var dashboards = result.data;
                if (dashboards.length > 0) {
                    scope.dashboard = dashboards[0];
                }
            }, function fail() {
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            dashboardsScope: '@',
            theForm: '=?',
            tbRequired: '=?',
            selectFirstDashboard: '='
        }
    };
}
