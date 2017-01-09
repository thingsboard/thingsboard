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
import './event.scss';

/* eslint-disable import/no-unresolved, import/default */

import eventTableTemplate from './event-table.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EventTableDirective($compile, $templateCache, $rootScope, types, eventService) {

    var linker = function (scope, element, attrs) {

        var template = $templateCache.get(eventTableTemplate);

        element.html(template);

        if (attrs.disabledEventTypes) {
            var disabledEventTypes = attrs.disabledEventTypes.split(',');
            scope.eventTypes = {};
            for (var type in types.eventType) {
                var eventType = types.eventType[type];
                var enabled = true;
                for (var disabledType in disabledEventTypes) {
                    if (eventType.value === disabledEventTypes[disabledType]) {
                        enabled = false;
                        break;
                    }
                }
                if (enabled) {
                    scope.eventTypes[type] = eventType;
                }
            }
        } else {
            scope.eventTypes = types.eventType;
        }

        scope.eventType = attrs.defaultEventType;

        var pageSize = 20;
        var startTime = 0;
        var endTime = 0;

        scope.timewindow = {
            history: {
                timewindowMs: 24 * 60 * 60 * 1000 // 1 day
            }
        }

        scope.topIndex = 0;

        scope.theEvents = {
            getItemAtIndex: function (index) {
                if (index > scope.events.data.length) {
                    scope.theEvents.fetchMoreItems_(index);
                    return null;
                }
                var item = scope.events.data[index];
                if (item) {
                    item.indexNumber = index + 1;
                }
                return item;
            },

            getLength: function () {
                if (scope.events.hasNext) {
                    return scope.events.data.length + scope.events.nextPageLink.limit;
                } else {
                    return scope.events.data.length;
                }
            },

            fetchMoreItems_: function () {
                if (scope.events.hasNext && !scope.events.pending) {
                    if (scope.entityType && scope.entityId && scope.eventType && scope.tenantId) {
                        var promise = eventService.getEvents(scope.entityType, scope.entityId,
                            scope.eventType, scope.tenantId, scope.events.nextPageLink);
                        if (promise) {
                            scope.events.pending = true;
                            promise.then(
                                function success(events) {
                                    scope.events.data = scope.events.data.concat(events.data);
                                    scope.events.nextPageLink = events.nextPageLink;
                                    scope.events.hasNext = events.hasNext;
                                    if (scope.events.hasNext) {
                                        scope.events.nextPageLink.limit = pageSize;
                                    }
                                    scope.events.pending = false;
                                },
                                function fail() {
                                    scope.events.hasNext = false;
                                    scope.events.pending = false;
                                });
                        } else {
                            scope.events.hasNext = false;
                        }
                    } else {
                        scope.events.hasNext = false;
                    }
                }
            }
        };

        scope.$watch("entityId", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                scope.resetFilter();
                scope.reload();
            }
        });

        scope.$watch("eventType", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                scope.reload();
            }
        });

        scope.$watch("timewindow", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                scope.reload();
            }
        }, true);

        scope.resetFilter = function() {
            scope.timewindow = {
                history: {
                    timewindowMs: 24 * 60 * 60 * 1000 // 1 day
                }
            };
            scope.eventType = attrs.defaultEventType;
        }

        scope.updateTimeWindowRange = function() {
            if (scope.timewindow.history.timewindowMs) {
                var currentTime = (new Date).getTime();
                startTime = currentTime - scope.timewindow.history.timewindowMs;
                endTime = currentTime;
            } else {
                startTime = scope.timewindow.history.fixedTimewindow.startTimeMs;
                endTime = scope.timewindow.history.fixedTimewindow.endTimeMs;
            }
        }

        scope.reload = function() {
            scope.topIndex = 0;
            scope.selected = [];
            scope.updateTimeWindowRange();
            scope.events = {
                data: [],
                nextPageLink: {
                    limit: pageSize,
                    startTime: startTime,
                    endTime: endTime
                },
                hasNext: true,
                pending: false
            };
            scope.theEvents.getItemAtIndex(pageSize);
        }

        scope.noData = function() {
            return scope.events.data.length == 0 && !scope.events.hasNext;
        }

        scope.hasData = function() {
            return scope.events.data.length > 0;
        }

        scope.loading = function() {
            return $rootScope.loading;
        }

        scope.hasScroll = function() {
            var repeatContainer = scope.repeatContainer[0];
            if (repeatContainer) {
                var scrollElement = repeatContainer.children[0];
                if (scrollElement) {
                    return scrollElement.scrollHeight > scrollElement.clientHeight;
                }
            }
            return false;
        }

        scope.reload();

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            entityType: '=',
            entityId: '=',
            tenantId: '='
        }
    };
}
