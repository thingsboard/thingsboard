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
import $ from 'jquery';

export default angular.module('thingsboard.directives.circularProgress', [])
    .directive('tbCircularProgress', CircularProgress)
    .name;

/* eslint-disable angular/angularelement */

/*@ngInject*/
function CircularProgress($compile) {

    var linker = function (scope, element) {

        var circularProgressElement = angular.element('<md-progress-circular style="margin: auto;" md-mode="indeterminate" md-diameter="20"></md-progress-circular>');

        $compile(circularProgressElement)(scope);

        var children = null;
        var cssWidth = element.prop('style')['width'];
        var width = null;
        if (!cssWidth) {
            $(element).css('width', width + 'px');
        }

        scope.$watch('circularProgress', function (newCircularProgress, prevCircularProgress) {
            if (newCircularProgress != prevCircularProgress) {
                if (newCircularProgress) {
                    if (!cssWidth) {
                        $(element).css('width', '');
                        width = element.prop('offsetWidth');
                        $(element).css('width', width + 'px');
                    }
                    children = $(element).children();
                    $(element).empty();
                    $(element).append($(circularProgressElement));
                } else {
                    $(element).empty();
                    $(element).append(children);
                    if (cssWidth) {
                        $(element).css('width', cssWidth);
                    } else {
                        $(element).css('width', '');
                    }
                }
            }
        });

    }

    return {
        restrict: "A",
        link: linker,
        scope: {
            circularProgress: "=tbCircularProgress"
        }
    };
}

/* eslint-enable angular/angularelement */
