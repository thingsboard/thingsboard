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
import './dashboard-autocomplete.scss';

import thingsboardApiDashboard from '../api/dashboard.service';
import thingsboardApiUser from '../api/user.service';

/* eslint-disable import/no-unresolved, import/default */

import dashboardAutocompleteTemplate from './dashboard-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.dashboardAutocomplete', [thingsboardApiDashboard, thingsboardApiUser])
    .directive('tbDashboardAutocomplete', DashboardAutocomplete)
    .name;

/*@ngInject*/
function DashboardAutocomplete($compile, $templateCache, $q, dashboardService, userService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(dashboardAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.dashboard = null;
        scope.dashboardSearchText = '';

        scope.fetchDashboards = function(searchText) {
            var pageLink = {limit: 50, textSearch: searchText};

            var deferred = $q.defer();

            var promise;
            if (scope.dashboardsScope === 'customer' || userService.getAuthority() === 'CUSTOMER_USER') {
                if (scope.customerId) {
                    promise = dashboardService.getCustomerDashboards(scope.customerId, pageLink, {ignoreLoading: true});
                } else {
                    promise = $q.when({data: []});
                }
            } else {
                if (userService.getAuthority() === 'SYS_ADMIN') {
                    if (scope.tenantId) {
                        promise = dashboardService.getTenantDashboardsByTenantId(scope.tenantId, pageLink, {ignoreLoading: true});
                    } else {
                        promise = $q.when({data: []});
                    }
                } else {
                    promise = dashboardService.getTenantDashboards(pageLink, {ignoreLoading: true});
                }
            }

            promise.then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.dashboardSearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.dashboard ? scope.dashboard.id.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                dashboardService.getDashboardInfo(ngModelCtrl.$viewValue).then(
                    function success(dashboard) {
                        scope.dashboard = dashboard;
                        startWatchers();
                    },
                    function fail() {
                        scope.dashboard = null;
                        scope.updateView();
                        startWatchers();
                    }
                );
            } else {
                scope.dashboard = null;
                startWatchers();
            }
        }

        function startWatchers() {
            scope.$watch('dashboard', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
            scope.$watch('disabled', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
        }

        if (scope.selectFirstDashboard) {
            var pageLink = {limit: 1, textSearch: ''};
            scope.dashboardFetchFunction(pageLink).then(function success(result) {
                var dashboards = result.data;
                if (dashboards.length > 0) {
                    scope.dashboard = dashboards[0];
                    scope.updateView();
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
            tenantId: '=',
            customerId: '=',
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            selectFirstDashboard: '='
        }
    };
}
