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
function DashboardSelect($compile, $templateCache, $q, types, dashboardService, userService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(dashboardSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.dashboardId = null;

        var pageLink = {limit: 100};

        var promise;
        if (scope.dashboardsScope === 'customer' || userService.getAuthority() === 'CUSTOMER_USER') {
            if (scope.customerId && scope.customerId != types.id.nullUid) {
                promise = dashboardService.getCustomerDashboards(scope.customerId, pageLink);
            } else {
                promise = $q.when({data: []});
            }
        } else {
            promise = dashboardService.getTenantDashboards(pageLink);
        }

        promise.then(function success(result) {
            scope.dashboards = result.data;
        }, function fail() {
            scope.dashboards = [];
        });

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.dashboardId);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.dashboardId = ngModelCtrl.$viewValue;
            } else {
                scope.dashboardId = null;
            }
        }

        scope.$watch('dashboardId', function () {
            scope.updateView();
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            dashboardsScope: '@',
            customerId: '=',
            tbRequired: '=?',
            disabled:'=ngDisabled'
        }
    };
}
