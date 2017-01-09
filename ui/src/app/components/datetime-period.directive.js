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
import './datetime-period.scss';

/* eslint-disable import/no-unresolved, import/default */

import datetimePeriodTemplate from './datetime-period.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.datetimePeriod', [])
    .directive('tbDatetimePeriod', DatetimePeriod)
    .name;

/*@ngInject*/
function DatetimePeriod($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(datetimePeriodTemplate);
        element.html(template);

        ngModelCtrl.$render = function () {
            var date = new Date();
            scope.startDate = new Date(
                date.getFullYear(),
                date.getMonth(),
                date.getDate() - 1);
            scope.endDate = date;
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.startDate = new Date(value.startTimeMs);
                scope.endDate = new Date(value.endTimeMs);
            }
        }

        scope.updateMinMaxDates = function () {
            scope.maxStartDate = angular.copy(new Date(scope.endDate.getTime() - 1000));
            scope.minEndDate = angular.copy(new Date(scope.startDate.getTime() + 1000));
            scope.maxEndDate = new Date();
        }

        scope.updateView = function () {
            var value = null;
            if (scope.startDate && scope.endDate) {
                value = {
                    startTimeMs: scope.startDate.getTime(),
                    endTimeMs: scope.endDate.getTime()
                };
                ngModelCtrl.$setValidity('datetimePeriod', true);
            } else {
                ngModelCtrl.$setValidity('datetimePeriod', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
        }

        scope.$watch('required', function () {
            scope.updateView();
        });

        scope.$watch('startDate', function (newDate) {
            if (newDate) {
                if (newDate.getTime() > scope.maxStartDate) {
                    scope.startDate = angular.copy(scope.maxStartDate);
                }
                scope.updateMinMaxDates();
            }
            scope.updateView();
        });

        scope.$watch('endDate', function (newDate) {
            if (newDate) {
                if (newDate.getTime() < scope.minEndDate) {
                    scope.endDate = angular.copy(scope.minEndDate);
                } else if (newDate.getTime() > scope.maxEndDate) {
                    scope.endDate = angular.copy(scope.maxEndDate);
                }
                scope.updateMinMaxDates();
            }
            scope.updateView();
        });

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            required: '=ngRequired'
        },
        link: linker
    };
}
