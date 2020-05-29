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
import './timewindow.scss';

import $ from 'jquery';
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

/* eslint-disable angular/angularelement */
/*@ngInject*/
function Timewindow($compile, $templateCache, $filter, $mdPanel, $document, $mdMedia, $translate, timeService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        /* tbTimewindow (ng-model)
         * {         
         *    hideInterval: false,
         *    hideAggregation: false,
         *    hideAggInterval: false,
         * 	  realtime: {
         * 	        interval: 0,
         * 			timewindowMs: 0
         * 	  },
         * 	  history: {
         * 	        interval: 0,
         * 			timewindowMs: 0,
         * 			fixedTimewindow: {
         * 				startTimeMs: 0,
         * 				endTimeMs: 0
         * 			}
         * 	  },
         * 	  aggregation: {
         * 	        type: types.aggregation.avg.value,
         * 	        limit: 200
         * 	  }
         * }
         */

        scope.historyOnly = angular.isDefined(attrs.historyOnly);

        scope.isEdit = attrs.isEdit === 'true';

        scope.aggregation = scope.$eval(attrs.aggregation);

        scope.isToolbar = angular.isDefined(attrs.isToolbar);

        scope.hideLabel = function() {
            return scope.isToolbar && !$mdMedia('gt-md');
        }

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
            scope.direction = angular.isDefined(attrs.direction) ? attrs.direction : 'left';
            scope.tooltipDirection = angular.isDefined(attrs.tooltipDirection) ? attrs.tooltipDirection : 'top';
            template = $templateCache.get(timewindowTemplate);
        }
        element.html(template);

        scope.openEditMode = function (event) {
            if (scope.timewindowDisabled) {
                return;
            }
            var position;
            var isGtSm = $mdMedia('gt-sm');
            if (isGtSm) {
                var panelHeight = 375;
                var panelWidth = 417;
                var offset = element[0].getBoundingClientRect();
                var bottomY = offset.bottom - $(window).scrollTop(); //eslint-disable-line
                var leftX = offset.left - $(window).scrollLeft(); //eslint-disable-line
                var yPosition;
                var xPosition;
                if (bottomY + panelHeight > $( window ).height()) { //eslint-disable-line
                    yPosition = $mdPanel.yPosition.ABOVE;
                } else {
                    yPosition = $mdPanel.yPosition.BELOW;
                }
                if (leftX + panelWidth > $( window ).width()) { //eslint-disable-line
                    xPosition = $mdPanel.xPosition.ALIGN_END;
                } else {
                    xPosition = $mdPanel.xPosition.ALIGN_START;
                }
                position = $mdPanel.newPanelPosition()
                    .relativeTo(element)
                    .addPanelPosition(xPosition, yPosition);
            } else {
                position = $mdPanel.newPanelPosition()
                    .absolute()
                    .top('0%')
                    .left('0%');
            }
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'TimewindowPanelController',
                controllerAs: 'vm',
                templateUrl: timewindowPanelTemplate,
                panelClass: 'tb-timewindow-panel',
                position: position,
                fullscreen: !isGtSm,
                locals: {
                    'timewindow': angular.copy(scope.model),
                    'historyOnly': scope.historyOnly,
                    'aggregation': scope.aggregation,                    
                    'isEdit': scope.isEdit,
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
                    interval: model.realtime.interval,
                    timewindowMs: model.realtime.timewindowMs
                };
            } else {
                if (model.history.historyType === 0) {
                    value.history = {
                        interval: model.history.interval,
                        timewindowMs: model.history.timewindowMs
                    };
                } else {
                    value.history = {   
                        interval: model.history.interval,
                        fixedTimewindow: {
                            startTimeMs: model.history.fixedTimewindow.startTimeMs,
                            endTimeMs: model.history.fixedTimewindow.endTimeMs
                        }
                    };
                }
            }
            value.aggregation = {
                type: model.aggregation.type,
                limit: model.aggregation.limit
            };            
            value.hideInterval = model.hideInterval;
            value.hideAggregation = model.hideAggregation;
            value.hideAggInterval = model.hideAggInterval;
            ngModelCtrl.$setViewValue(value);
            scope.updateDisplayValue();
        }

        scope.updateDisplayValue = function () {
            if ($translate.isReady()) {
                if (scope.model.hideInterval && !scope.model.hideAggregation) {
                    var aggregationName = '';
                    for (var type in types.aggregation) {
                        if (types.aggregation[type].value ===  scope.model.aggregation.type) {
                            aggregationName = types.aggregation[type].name;
                            break;
                        }
                    }
                    scope.model.displayValue = $translate.instant('aggregation.function') + ' - ' + $translate.instant(aggregationName);
                } else {
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
                }
            } else {
                translationPending = true;
            }
        }

        function isTimewindowDisabled () {
            return scope.disabled || (!scope.isEdit && scope.model.hideInterval && scope.model.hideAggregation && scope.model.hideAggInterval);
        }

        ngModelCtrl.$render = function () {
            scope.model = timeService.defaultTimewindow();
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var model = scope.model;
                if (angular.isDefined(value.realtime)) {
                    model.selectedTab = 0;
                    model.realtime.interval = value.realtime.interval;
                    model.realtime.timewindowMs = value.realtime.timewindowMs;
                } else {
                    model.selectedTab = 1;
                    model.history.interval = value.history.interval;
                    if (angular.isDefined(value.history.timewindowMs)) {
                        model.history.historyType = 0;
                        model.history.timewindowMs = value.history.timewindowMs;
                    } else {
                        model.history.historyType = 1;
                        model.history.fixedTimewindow.startTimeMs = value.history.fixedTimewindow.startTimeMs;
                        model.history.fixedTimewindow.endTimeMs = value.history.fixedTimewindow.endTimeMs;
                    }
                }
                if (angular.isDefined(value.aggregation)) {
                    if (angular.isDefined(value.aggregation.type) && value.aggregation.type.length > 0) {
                        model.aggregation.type = value.aggregation.type;
                    }
                    model.aggregation.limit = value.aggregation.limit || Math.floor(timeService.getMaxDatapointsLimit() / 2);
                }
                model.hideInterval = value.hideInterval;
                model.hideAggregation = value.hideAggregation;
                model.hideAggInterval = value.hideAggInterval;
            }
            scope.timewindowDisabled = isTimewindowDisabled();
            scope.updateDisplayValue();
        };

        scope.$watchGroup(['disabled', 'isEdit'], function(newValue, oldValue) {
            if (!angular.equals(newValue, oldValue)) {
                scope.timewindowDisabled = isTimewindowDisabled();
            }
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            asButton: '=asButton',
            disabled:'=ngDisabled',
            isEdit: '&?'

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
/* eslint-enable angular/angularelement */
