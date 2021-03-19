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
import './downlink.scss';

/* eslint-disable import/no-unresolved, import/default */

import edgeDownlinksTableTemplate from './edge-downlinks-table.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeDownlinksDirective($compile, $templateCache, $rootScope, $translate, types, edgeService, attributeService) {

    var linker = function (scope, element) {

        var template = $templateCache.get(edgeDownlinksTableTemplate);
        element.html(template);

        var pageSize = 20;
        var startTime = 0;
        var endTime = 0;

        scope.timewindow = {
            history: {
                timewindowMs: 24 * 60 * 60 * 1000 // 1 day
            }
        }

        scope.topIndex = 0;

        scope.theDownlinks = {
            getItemAtIndex: function (index) {
                if (index > scope.downlinks.data.length) {
                    scope.theDownlinks.fetchMoreItems_(index);
                    return null;
                }
                var item = scope.downlinks.data[index];
                if (item) {
                    item.indexNumber = index + 1;
                }
                return item;
            },

            getLength: function () {
                if (scope.downlinks.hasNext) {
                    return scope.downlinks.data.length + scope.downlinks.nextPageLink.limit;
                } else {
                    return scope.downlinks.data.length;
                }
            },

            fetchMoreItems_: function () {
                if (scope.downlinks.hasNext && !scope.downlinks.pending) {
                    if (scope.entityId) {
                        scope.loadEdgeInfo();
                        scope.downlinks.pending = true;
                        edgeService.getEdgeEvents(scope.entityId, scope.downlinks.nextPageLink).then(
                            function success(downlinks) {
                                scope.downlinks.data = scope.downlinks.data.concat(prepareEdgeDownlinksData(downlinks.data));
                                scope.downlinks.nextPageLink = downlinks.nextPageLink;
                                scope.downlinks.hasNext = downlinks.hasNext;
                                if (scope.downlinks.hasNext) {
                                    scope.downlinks.nextPageLink.limit = pageSize;
                                }
                                scope.downlinks.pending = false;
                            },
                            function fail() {
                                scope.downlinks.hasNext = false;
                                scope.downlinks.pending = false;
                            });
                    } else {
                        scope.downlinks.hasNext = false;
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
            scope.downlinks = {
                data: [],
                nextPageLink: {
                    limit: pageSize,
                    startTime: startTime,
                    endTime: endTime
                },
                hasNext: true,
                pending: false
            };
            scope.theDownlinks.getItemAtIndex(pageSize);
        }

        scope.noData = function() {
            return scope.downlinks.data.length == 0 && !scope.downlinks.hasNext;
        }

        scope.hasData = function() {
            return scope.downlinks.data.length > 0;
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

        scope.subscriptionId = null;

        scope.loadEdgeInfo = function() {
            attributeService.getEntityAttributesValues(
                types.entityType.edge,
                scope.entityId,
                types.attributesScope.server.value,
                types.edgeAttributeKeys.queueStartTs,
                null).then(
                    function success(attributes) {
                        attributes.length > 0 ? scope.onEdgeAttributesUpdate(attributes) : scope.queueStartTs = 0;
                    });
            scope.checkSubscription();
        }

        scope.onEdgeAttributesUpdate = function(attributes) {
            let edgeAttributes = attributes.reduce(function (map, attribute) {
                map[attribute.key] = attribute;
                return map;
            }, {});
            if (edgeAttributes.queueStartTs) {
                scope.queueStartTs = edgeAttributes.queueStartTs.lastUpdateTs;
            }
        }

        scope.checkSubscription = function() {
            var newSubscriptionId = null;
            if (scope.entityId && types.attributesScope.server.value) {
                newSubscriptionId =
                    attributeService.subscribeForEntityAttributes(types.entityType.edge, scope.entityId, types.attributesScope.server.value);
            }
            if (scope.subscriptionId && scope.subscriptionId != newSubscriptionId) {
                attributeService.unsubscribeForEntityAttributes(scope.subscriptionId);
            }
            scope.subscriptionId = newSubscriptionId;
        }

        scope.$on('$destroy', function () {
            if (scope.subscriptionId) {
                attributeService.unsubscribeForEntityAttributes(scope.subscriptionId);
            }
        });

        scope.reload();

        $compile(element.contents())(scope);
    }
    function prepareEdgeDownlinksData(data) {

        data.forEach(
            edgeDownlink => {
                edgeDownlink.edgeEventActionText = $translate.instant(types.edgeEventActionTypeTranslations[edgeDownlink.action].name);
                edgeDownlink.edgeEventTypeText = $translate.instant(types.edgeEventTypeTranslations[edgeDownlink.type].name);
            }
        );
        return data;
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            entityId: '='
        }
    };
}
