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
import Raphael from 'raphael';
import tinycolor from 'tinycolor2';
import $ from 'jquery';

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.ledLight', [])
    .directive('tbLedLight', LedLight).name;

/*@ngInject*/
function LedLight($compile) {

    var linker = function (scope, element) {
        scope.offOpacity = scope.offOpacity || "0.4";
        scope.glowColor = tinycolor(scope.colorOn).lighten().toHexString();

        scope.$watch('tbEnabled',function() {
            scope.draw();
        });

        scope.$watch('size',function() {
            scope.update();
        });

        scope.draw = function () {
            if (scope.tbEnabled) {
                scope.circleElement.attr("fill", scope.colorOn);
                scope.circleElement.attr("stroke", scope.colorOn);
                scope.circleElement.attr("opacity", "1");

                if (scope.circleElement.theGlow) {
                    scope.circleElement.theGlow.remove();
                }

                scope.circleElement.theGlow = scope.circleElement.glow(
                    {
                        color: scope.glowColor,
                        width: scope.radius + scope.glowSize,
                        opacity: 0.8,
                        fill: true
                    });
            } else {
                if (scope.circleElement.theGlow) {
                    scope.circleElement.theGlow.remove();
                }

                /*scope.circleElement.theGlow = scope.circleElement.glow(
                 {
                 color: scope.glowColor,
                 width: scope.radius + scope.glowSize,
                 opacity: 0.4,
                 fill: true
                 });*/

                scope.circleElement.attr("fill", scope.colorOff);
                scope.circleElement.attr("stroke", scope.colorOff);
                scope.circleElement.attr("opacity", scope.offOpacity);
            }
        }

        scope.update = function() {
            scope.size = scope.size || 50;
            scope.canvasSize = scope.size;
            scope.radius = scope.canvasSize / 4;
            scope.glowSize = scope.radius / 5;

            var template = '<div id="canvas_container" style="width: ' + scope.size + 'px; height: ' + scope.size + 'px;"></div>';
            element.html(template);
            $compile(element.contents())(scope);
            scope.paper = new Raphael($('#canvas_container', element)[0], scope.canvasSize, scope.canvasSize);
            var center = scope.canvasSize / 2;
            scope.circleElement = scope.paper.circle(center, center, scope.radius);
            scope.draw();
        }

        scope.update();
    }


    return {
        restrict: "E",
        link: linker,
        scope: {
            size: '=?',
            colorOn: '=',
            colorOff: '=',
            offOpacity: '=?',
            //glowColor: '=',
            tbEnabled: '='
        }
    };

}

/* eslint-enable angular/angularelement */
