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
import './timeinterval.scss';

/* eslint-disable import/no-unresolved, import/default */

import timeintervalTemplate from './timeinterval.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.timeinterval', [])
    .directive('tbTimeinterval', Timeinterval)
    .name;

/*@ngInject*/
function Timeinterval($compile, $templateCache, timeService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(timeintervalTemplate);
        element.html(template);

        scope.rendered = false;
        scope.days = 0;
        scope.hours = 0;
        scope.mins = 1;
        scope.secs = 0;

        scope.advanced = false;

        scope.boundInterval = function() {
            var min = timeService.boundMinInterval(scope.min);
            var max = timeService.boundMaxInterval(scope.max);
            scope.intervals = timeService.getIntervals(scope.min, scope.max);
            if (scope.rendered) {
                var newIntervalMs = ngModelCtrl.$viewValue;
                if (newIntervalMs < min) {
                    newIntervalMs = min;
                } else if (newIntervalMs > max) {
                    newIntervalMs = max;
                }
                if (!scope.advanced) {
                    newIntervalMs = timeService.boundToPredefinedInterval(min, max, newIntervalMs);
                }
                if (newIntervalMs !== ngModelCtrl.$viewValue) {
                    scope.setIntervalMs(newIntervalMs);
                    scope.updateView();
                }
            }
        }

        scope.setIntervalMs = function (intervalMs) {
            if (!scope.advanced) {
                scope.intervalMs = intervalMs;
            }
            var intervalSeconds = Math.floor(intervalMs / 1000);
            scope.days = Math.floor(intervalSeconds / 86400);
            scope.hours = Math.floor((intervalSeconds % 86400) / 3600);
            scope.mins = Math.floor(((intervalSeconds % 86400) % 3600) / 60);
            scope.secs = intervalSeconds % 60;
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var intervalMs = ngModelCtrl.$viewValue;
                if (!scope.rendered) {
                    scope.advanced = !timeService.matchesExistingInterval(scope.min, scope.max, intervalMs);
                }
                scope.setIntervalMs(intervalMs);
            }
            scope.rendered = true;
        }

        function calculateIntervalMs() {
            return (scope.days * 86400 +
                scope.hours * 3600 +
                scope.mins * 60 +
                scope.secs) * 1000;
        }

        scope.updateView = function () {
            if (!scope.rendered) {
                return;
            }
            var value = null;
            var intervalMs;
            if (!scope.advanced) {
                intervalMs = scope.intervalMs;
                if (!intervalMs || isNaN(intervalMs)) {
                    intervalMs = calculateIntervalMs();
                }
            } else {
                intervalMs = calculateIntervalMs();
            }
            if (!isNaN(intervalMs) && intervalMs > 0) {
                value = intervalMs;
                ngModelCtrl.$setValidity('tb-timeinterval', true);
            } else {
                ngModelCtrl.$setValidity('tb-timeinterval', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
            scope.boundInterval();
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

        scope.$watch('intervalMs', function (newIntervalMs, prevIntervalMs) {
            if (angular.isDefined(newIntervalMs) && newIntervalMs !== prevIntervalMs) {
                scope.updateView();
            }
        });

        scope.$watch('advanced', function (newAdvanced, prevAdvanced) {
            if (angular.isDefined(newAdvanced) && newAdvanced !== prevAdvanced) {
                if (!scope.advanced) {
                    scope.intervalMs = calculateIntervalMs();
                } else {
                    var intervalMs = scope.intervalMs;
                    if (!intervalMs || isNaN(intervalMs)) {
                        intervalMs = calculateIntervalMs();
                    }
                    scope.setIntervalMs(intervalMs);
                }
                scope.updateView();
            }
        });

        scope.$watch('secs', function (newSecs) {
            if (angular.isUndefined(newSecs)) {
                return;
            }
            if (newSecs < 0) {
                if ((scope.days + scope.hours + scope.mins) > 0) {
                    scope.secs = newSecs + 60;
                    scope.mins--;
                } else {
                    scope.secs = 0;
                }
            } else if (newSecs >= 60) {
                scope.secs = newSecs - 60;
                scope.mins++;
            }
            scope.updateView();
        });

        scope.$watch('mins', function (newMins) {
            if (angular.isUndefined(newMins)) {
                return;
            }
            if (newMins < 0) {
                if ((scope.days + scope.hours) > 0) {
                    scope.mins = newMins + 60;
                    scope.hours--;
                } else {
                    scope.mins = 0;
                }
            } else if (newMins >= 60) {
                scope.mins = newMins - 60;
                scope.hours++;
            }
            scope.updateView();
        });

        scope.$watch('hours', function (newHours) {
            if (angular.isUndefined(newHours)) {
                return;
            }
            if (newHours < 0) {
                if (scope.days > 0) {
                    scope.hours = newHours + 24;
                    scope.days--;
                } else {
                    scope.hours = 0;
                }
            } else if (newHours >= 24) {
                scope.hours = newHours - 24;
                scope.days++;
            }
            scope.updateView();
        });

        scope.$watch('days', function (newDays) {
            if (angular.isUndefined(newDays)) {
                return;
            }
            if (newDays < 0) {
                scope.days = 0;
            }
            scope.updateView();
        });

        scope.boundInterval();

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
            isEdit: '=?'
        },
        link: linker
    };
}
