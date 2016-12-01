/*
 * Copyright © 2016 The Thingsboard Authors
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
import './timewindow.scss';

import thingsboardTimeinterval from './timeinterval.directive';
import thingsboardDatetimePeriod from './datetime-period.directive';

/* eslint-disable import/no-unresolved, import/default */

import timewindowButtonTemplate from './timewindow-button.tpl.html';
import timewindowTemplate from './timewindow.tpl.html';
import timewindowPanelTemplate from './timewindow-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import TimewindowPanelController from './timewindow-panel.controller';

export default angular.module('thingsboard.directives.timewindow', [thingsboardTimeinterval, thingsboardDatetimePeriod])
    .controller('TimewindowPanelController', TimewindowPanelController)
    .directive('tbTimewindow', Timewindow)
    .filter('milliSecondsToTimeString', MillisecondsToTimeString)
    .name;

/*@ngInject*/
function Timewindow($compile, $templateCache, $filter, $mdPanel, $document, $translate) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        /* tbTimewindow (ng-model)
         * {
         * 	  realtime: {
         * 			timewindowMs: 0
         * 	  },
         * 	  history: {
         * 			timewindowMs: 0,
         * 			fixedTimewindow: {
         * 				startTimeMs: 0,
         * 				endTimeMs: 0
         * 			}
         * 	  }
         * }
         */

        scope.historyOnly = angular.isDefined(attrs.historyOnly);

        var translationPending = false;

        $translate.onReady(function() {
            if (translationPending) {
                scope.updateDisplayValue();
                translationPending = false;
            }
        });

        var template;
        if (scope.asButton) {
            template = $templateCache.get(timewindowButtonTemplate);
        } else {
            template = $templateCache.get(timewindowTemplate);
        }
        element.html(template);

        scope.isHovered = false;

        scope.onHoverIn = function () {
            scope.isHovered = true;
        }

        scope.onHoverOut = function () {
            scope.isHovered = false;
        }

        scope.openEditMode = function (event) {
            var position = $mdPanel.newPanelPosition()
                .relativeTo(element)
                .addPanelPosition($mdPanel.xPosition.ALIGN_START, $mdPanel.yPosition.BELOW);
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'TimewindowPanelController',
                controllerAs: 'vm',
                templateUrl: timewindowPanelTemplate,
                panelClass: 'tb-timewindow-panel',
                position: position,
                locals: {
                    'timewindow': angular.copy(scope.model),
                    'historyOnly': scope.historyOnly,
                    'onTimewindowUpdate': function (timewindow) {
                        scope.model = timewindow;
                        scope.updateView();
                    }
                },
                openFrom: event,
                clickOutsideToClose: true,
                escapeToClose: true,
                focusOnOpen: false
            };
            $mdPanel.open(config);
        }

        scope.updateView = function () {
            var value = {};
            var model = scope.model;
            if (model.selectedTab === 0) {
                value.realtime = {
                    timewindowMs: model.realtime.timewindowMs
                };
            } else {
                if (model.history.historyType === 0) {
                    value.history = {
                        timewindowMs: model.history.timewindowMs
                    };
                } else {
                    value.history = {
                        fixedTimewindow: {
                            startTimeMs: model.history.fixedTimewindow.startTimeMs,
                            endTimeMs: model.history.fixedTimewindow.endTimeMs
                        }
                    };
                }
            }

            ngModelCtrl.$setViewValue(value);
            scope.updateDisplayValue();
        }

        scope.updateDisplayValue = function () {
            if ($translate.isReady()) {
                if (scope.model.selectedTab === 0 && !scope.historyOnly) {
                    scope.model.displayValue = $translate.instant('timewindow.realtime') + ' - ' +
                        $translate.instant('timewindow.last-prefix') + ' ' +
                        $filter('milliSecondsToTimeString')(scope.model.realtime.timewindowMs);
                } else {
                    scope.model.displayValue = !scope.historyOnly ? ($translate.instant('timewindow.history') + ' - ') : '';
                    if (scope.model.history.historyType === 0) {
                        scope.model.displayValue += $translate.instant('timewindow.last-prefix') + ' ' +
                            $filter('milliSecondsToTimeString')(scope.model.history.timewindowMs);
                    } else {
                        var startString = $filter('date')(scope.model.history.fixedTimewindow.startTimeMs, 'yyyy-MM-dd HH:mm:ss');
                        var endString = $filter('date')(scope.model.history.fixedTimewindow.endTimeMs, 'yyyy-MM-dd HH:mm:ss');
                        scope.model.displayValue += $translate.instant('timewindow.period', {startTime: startString, endTime: endString});
                    }
                }
            } else {
                translationPending = true;
            }
        }

        ngModelCtrl.$render = function () {
            var currentTime = (new Date).getTime();
            scope.model = {
                displayValue: "",
                selectedTab: 0,
                realtime: {
                    timewindowMs: 60000 // 1 min by default
                },
                history: {
                    historyType: 0,
                    timewindowMs: 60000, // 1 min by default
                    fixedTimewindow: {
                        startTimeMs: currentTime - 24 * 60 * 60 * 1000, // 1 day by default
                        endTimeMs: currentTime
                    }
                }
            };
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var model = scope.model;
                if (angular.isDefined(value.realtime)) {
                    model.selectedTab = 0;
                    model.realtime.timewindowMs = value.realtime.timewindowMs;
                } else {
                    model.selectedTab = 1;
                    if (angular.isDefined(value.history.timewindowMs)) {
                        model.history.historyType = 0;
                        model.history.timewindowMs = value.history.timewindowMs;
                    } else {
                        model.history.historyType = 1;
                        model.history.fixedTimewindow.startTimeMs = value.history.fixedTimewindow.startTimeMs;
                        model.history.fixedTimewindow.endTimeMs = value.history.fixedTimewindow.endTimeMs;
                    }
                }
            }
            scope.updateDisplayValue();
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            asButton: '=asButton'
        },
        link: linker
    };
}

/*@ngInject*/
function MillisecondsToTimeString($translate) {
    return function (millseconds) {
        var seconds = Math.floor(millseconds / 1000);
        var days = Math.floor(seconds / 86400);
        var hours = Math.floor((seconds % 86400) / 3600);
        var minutes = Math.floor(((seconds % 86400) % 3600) / 60);
        seconds = seconds % 60;
        var timeString = '';
        if (days > 0) timeString += $translate.instant('timewindow.days', {days: days}, 'messageformat');
        if (hours > 0) {
            if (timeString.length === 0 && hours === 1) {
                hours = 0;
            }
            timeString += $translate.instant('timewindow.hours', {hours: hours}, 'messageformat');
        }
        if (minutes > 0) {
            if (timeString.length === 0 && minutes === 1) {
                minutes = 0;
            }
            timeString += $translate.instant('timewindow.minutes', {minutes: minutes}, 'messageformat');
        }
        if (seconds > 0) {
            if (timeString.length === 0 && seconds === 1) {
                seconds = 0;
            }
            timeString += $translate.instant('timewindow.seconds', {seconds: seconds}, 'messageformat');
        }
        return timeString;
    }
}