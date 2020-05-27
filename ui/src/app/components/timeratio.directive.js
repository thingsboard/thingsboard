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
import './timeratio.scss';

/* eslint-disable import/no-unresolved, import/default */

import timeratioTemplate from './timeratio.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.timeratio', [])
    .directive('tbTimeratio', Timeratio)
    .name;

/*@ngInject*/
function Timeratio($compile, $templateCache, timeService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(timeratioTemplate);
        element.html(template);

        scope.boundRatio = function() {
            var min = timeService.getMinDatapointsLimit();
            var max = timeService.getMaxDatapointsLimit();
            if (scope.rendered) {
                var newTimeRatio = ngModelCtrl.$viewValue;
                if (newTimeRatio < min) {
                    newTimeRatio = min;
                } else if (newTimeRatio > max) {
                    newTimeRatio = max;
                }
                if (newTimeRatio !== ngModelCtrl.$viewValue) {
                    scope.updateView();
                }
            }
        }

        scope.setTimeRatio = function (timeRatio) {
            scope.timeRatio = timeRatio;
        }

        ngModelCtrl.$render = function () {
            var timeRatio = ngModelCtrl.$viewValue;
            scope.setTimeRatio(timeRatio);
            scope.rendered = true;
        }

        scope.updateView = function () {
            if (!scope.rendered) {
                return;
            }
            var value = null;
            var timeRatio = scope.timeRatio;
            if (!isNaN(timeRatio) && timeRatio > 0) {
                value = timeRatio;
                ngModelCtrl.$setValidity('tb-timeratio', true);
            } else {
                ngModelCtrl.$setValidity('tb-timeratio', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
            scope.boundRatio();
        }

        scope.$watch('required', function (newRequired, prevRequired) {
            if (angular.isDefined(newRequired) && newRequired !== prevRequired) {
                scope.updateView();
            }
        });

        scope.$watch('min', function (newMin, prevMin) {
            if (angular.isDefined(newMin) && newMin !== prevMin) {
                scope.updateView();
            }
        });

        scope.$watch('max', function (newMax, prevMax) {
            if (angular.isDefined(newMax) && newMax !== prevMax) {
                scope.updateView();
            }
        });

        scope.$watch('timeRatio', function (newTimeRatio, prevTimeRatio) {
            if (angular.isDefined(newTimeRatio) && newTimeRatio !== prevTimeRatio) {
                scope.updateView();
            }
        });

        scope.boundRatio();

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            required: '=ngRequired',
            min: '=?',
            max: '=?',
            predefinedName: '=?',
            hideFlag: '=?',
            fromDashboardFlag: '=?',
            isToolbar: '=?',
            isEdit: '=?',
            form: '=?'
        },
        link: linker
    };
}
