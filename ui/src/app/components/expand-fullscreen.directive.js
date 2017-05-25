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
import './expand-fullscreen.scss';

import $ from 'jquery';

export default angular.module('thingsboard.directives.expandFullscreen', [])
    .directive('tbExpandFullscreen', ExpandFullscreen)
    .name;

/* eslint-disable angular/angularelement */

/*@ngInject*/
function ExpandFullscreen($compile, $document, $timeout) {

    var uniqueId = 1;
    var linker = function (scope, element, attrs) {

        scope.body = angular.element($document.find('body').eq(0));
        scope.fullscreenParentId = 'fullscreen-parent' + uniqueId;
        scope.fullscreenParent = $('#' + scope.fullscreenParentId, scope.body)[0];
        if (!scope.fullscreenParent) {
            uniqueId++;
            var fullscreenParent = angular.element('<div id=\'' + scope.fullscreenParentId + '\' class=\'tb-fullscreen-parent\'></div>');
            scope.body.append(fullscreenParent);
            scope.fullscreenParent = $('#' + scope.fullscreenParentId, scope.body)[0];
            scope.fullscreenParent = angular.element(scope.fullscreenParent);
            scope.fullscreenParent.css('display', 'none');
        } else {
            scope.fullscreenParent = angular.element(scope.fullscreenParent);
        }

        scope.$on('$destroy', function () {
            scope.fullscreenParent.remove();
        });

        scope.elementParent = null;
        scope.expanded = false;
        scope.fullscreenZindex = scope.fullscreenZindex();

        if (!scope.fullscreenZindex) {
            scope.fullscreenZindex = '70';
        }

        scope.$watch('expanded', function (newExpanded, prevExpanded) {
            if (newExpanded != prevExpanded) {
                if (scope.expanded) {
                    scope.elementParent = element.parent();
                    element.detach();
                    if (scope.backgroundStyle) {
                        scope.fullscreenParent.attr("ng-style","backgroundStyle");
                        $compile(scope.fullscreenParent)(scope);
                    }
                    scope.fullscreenParent.append(element);
                    scope.fullscreenParent.css('display', '');
                    scope.fullscreenParent.css('z-index', scope.fullscreenZindex);
                    element.addClass('tb-fullscreen');
                } else {
                    if (scope.elementParent) {
                        element.detach();
                        scope.elementParent.append(element);
                        scope.elementParent = null;
                    }
                    element.removeClass('tb-fullscreen');
                    scope.fullscreenParent.css('display', 'none');
                    scope.fullscreenParent.css('z-index', '');
                }
                if (scope.onFullscreenChanged) {
                    scope.onFullscreenChanged({expanded: scope.expanded});
                }
            }
        });

        scope.$watch(function () {
            return scope.expand();
        }, function (newExpanded) {
            scope.expanded = newExpanded;
        });

        scope.toggleExpand = function ($event) {
            if ($event) {
                $event.stopPropagation();
            }
            scope.expanded = !scope.expanded;
        }

        var buttonSize;
        if (attrs.expandButtonSize) {
            buttonSize = attrs.expandButtonSize;
        }

        var tooltipDirection = angular.isDefined(attrs.expandTooltipDirection) ? attrs.expandTooltipDirection : 'top';

        var html = '<md-tooltip md-direction="{{expanded ? \'bottom\' : \'' + tooltipDirection + '\'}}">' +
            '{{(expanded ? \'fullscreen.exit\' : \'fullscreen.expand\') | translate}}' +
            '</md-tooltip>' +
            '<ng-md-icon ' + (buttonSize ? 'size="'+ buttonSize +'" ' : '') + 'icon="{{expanded ? \'fullscreen_exit\' : \'fullscreen\'}}" ' +
            'options=\'{"easing": "circ-in-out", "duration": 375, "rotation": "none"}\'>' +
            '</ng-md-icon>';

        if (attrs.expandButtonId) {
            $timeout(function() {
               var expandButton = $('#' + attrs.expandButtonId, element)[0];
                renderExpandButton(expandButton);
            });
        } else {
            renderExpandButton();
        }

        function renderExpandButton(expandButton) {
            if (expandButton) {
                expandButton = angular.element(expandButton);
                if (scope.hideExpandButton()) {
                    expandButton.remove();
                } else {
                    expandButton.attr('md-ink-ripple', 'false');
                    expandButton.append(html);

                    $compile(expandButton.contents())(scope);

                    expandButton.on("click", scope.toggleExpand);
                }
            } else if (!scope.hideExpandButton()) {
                var button = angular.element('<md-button class="tb-fullscreen-button-style tb-fullscreen-button-pos md-icon-button" ' +
                    'md-ink-ripple="false" ng-click="toggleExpand($event)">' +
                    html +
                    '</md-button>');

                $compile(button)(scope);

                element.prepend(button);
            }
        }
    }

    return {
        restrict: "A",
        link: linker,
        scope: {
            expand: "&tbExpandFullscreen",
            hideExpandButton: "&hideExpandButton",
            onFullscreenChanged: "&onFullscreenChanged",
            fullscreenZindex: "&fullscreenZindex",
            backgroundStyle: "=?fullscreenBackgroundStyle"
        }
    };
}

/* eslint-enable angular/angularelement */
