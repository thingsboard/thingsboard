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

/* eslint-disable import/no-unresolved, import/default */

import depthintervalTemplate from './depthinterval.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.depthinterval', [])
    .directive('tbDepthinterval', Depthinterval)
    .name;

/*@ngInject*/
function Depthinterval($compile, $templateCache, depthService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(depthintervalTemplate);
        element.html(template);

        scope.rendered = false;

        scope.boundInterval = function() {
            scope.intervals = depthService.getIntervals();
            if (scope.rendered) {
                var newIntervalFt = ngModelCtrl.$viewValue;
                if (newIntervalFt !== ngModelCtrl.$viewValue) {
                    scope.setIntervalFt(newIntervalFt);
                    scope.updateView();
                }
            }
        };

        scope.setIntervalFt = function (intervalFt) {
                scope.intervalFt = intervalFt;
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var intervalFt = ngModelCtrl.$viewValue;
                scope.setIntervalFt(intervalFt);
            }
            scope.rendered = true;
        };

        scope.updateView = function () {
            if (!scope.rendered) {
                return;
            }
            var value = null;
            var intervalFt = scope.intervalFt;
            if (!isNaN(intervalFt) && intervalFt > 0) {
                value = intervalFt;
                ngModelCtrl.$setValidity('tb-depthinterval', true);
            } else {
                ngModelCtrl.$setValidity('tb-depthinterval', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
            scope.boundInterval();
        };

        scope.$watch('required', function (newRequired, prevRequired) {
            if (angular.isDefined(newRequired) && newRequired !== prevRequired) {
                scope.updateView();
            }
        });

        scope.$watch('intervalFt', function (newIntervalFt, prevIntervalFt) {
            if (angular.isDefined(newIntervalFt) && newIntervalFt !== prevIntervalFt) {
                scope.updateView();
            }
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
            predefinedName: '=?'
        },
        link: linker
    };
}
