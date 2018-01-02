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

import depthPeriodTemplate from './depth-period.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.depthPeriod', [])
    .directive('tbDepthPeriod', DepthPeriod)
    .name;

/*@ngInject*/
function DepthPeriod($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(depthPeriodTemplate);
        element.html(template);

        ngModelCtrl.$render = function () {
            var depth = 0.0;
            scope.startDepth = depth
            scope.endDepth = depth + 200.0;
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.startDepth = value.startDepthFt;
                scope.endDepth = value.endDepthFt;
            }
        }

        scope.updateView = function () {
            var value = null;
            if (scope.startDepth && scope.endDepth) {
                value = {
                    startDepthFt: scope.startDepth,
                    endDepthFt: scope.endDepth
                };
                ngModelCtrl.$setValidity('depthPeriod', true);
            } else {
                ngModelCtrl.$setValidity('depthPeriod', !scope.required);
            }
            ngModelCtrl.$setViewValue(value);
        }

        scope.$watch('required', function () {
            scope.updateView();
        });

        scope.$watch('startDepth', function () {
            scope.updateView();
        });

        scope.$watch('endDepth', function () {
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
