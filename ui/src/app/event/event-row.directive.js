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
/* eslint-disable import/no-unresolved, import/default */

import eventErrorDialogTemplate from './event-content-dialog.tpl.html';

import eventRowLcEventTemplate from './event-row-lc-event.tpl.html';
import eventRowStatsTemplate from './event-row-stats.tpl.html';
import eventRowErrorTemplate from './event-row-error.tpl.html';
import eventRowDebugRuleNodeTemplate from './event-row-debug-rulenode.tpl.html';
import eventRowEdgeEventTemplate from './event-row-edge-event.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EventRowDirective($compile, $templateCache, $mdDialog, $document, $translate,
                                          types, utils, toast, entityService, ruleChainService) {

    var linker = function (scope, element, attrs) {

        var getTemplate = function(eventType) {
            var template = '';
            switch(eventType) {
                case types.eventType.lcEvent.value:
                    template = eventRowLcEventTemplate;
                    break;
                case types.eventType.stats.value:
                    template = eventRowStatsTemplate;
                    break;
                case types.eventType.error.value:
                    template = eventRowErrorTemplate;
                    break;
                case types.debugEventType.debugRuleNode.value:
                    template = eventRowDebugRuleNodeTemplate;
                    break;
                case types.debugEventType.debugRuleChain.value:
                    template = eventRowDebugRuleNodeTemplate;
                    break;
                case types.eventType.edgeEvent.value:
                    template = eventRowEdgeEventTemplate;
                    break;
            }
            return $templateCache.get(template);
        }

        scope.loadTemplate = function() {
            element.html(getTemplate(attrs.eventType));
            $compile(element.contents())(scope);
        }

        attrs.$observe('eventType', function() {
            scope.loadTemplate();
        });

        scope.types = types;

        scope.event = attrs.event;

        scope.showContent = function($event, content, title, contentType) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            if (!contentType) {
                contentType = null;
            }
            var sortedContent;
            try {
                sortedContent = angular.toJson(utils.sortObjectKeys(angular.fromJson(content)));
            }
            catch(err) {
                sortedContent = content;
            }
            $mdDialog.show({
                controller: 'EventContentDialogController',
                controllerAs: 'vm',
                templateUrl: eventErrorDialogTemplate,
                locals: {content: sortedContent, title: title, contentType: contentType, showingCallback: onShowingCallback},
                parent: angular.element($document[0].body),
                fullscreen: true,
                targetEvent: $event,
                multiple: true,
                onShowing: function(scope, element) {
                    onShowingCallback.onShowing(scope, element);
                }
            });
        }

        scope.showEdgeEntityContent = function($event, title, contentType) {
            var onShowingCallback = {
                onShowing: function(){}
            }
            if (!contentType) {
                contentType = null;
            }
            var content = '';
            switch(scope.event.type) {
                case types.edgeEventType.relation:
                    content = angular.toJson(scope.event.body);
                    showDialog();
                    break;
                case types.edgeEventType.ruleChainMetaData:
                    content = ruleChainService.getRuleChainMetaData(scope.event.entityId, {ignoreErrors: true}).then(
                        function success(info) {
                            showDialog();
                            return angular.toJson(info);
                        }, function fail() {
                            showError();
                        });
                    break;
                default:
                    content = entityService.getEntity(scope.event.type, scope.event.entityId, {ignoreErrors: true}).then(
                        function success(info) {
                            showDialog();
                            return angular.toJson(info);
                        }, function fail() {
                            showError();
                        });
                    break;
            }
            function showDialog() {
                $mdDialog.show({
                    controller: 'EventContentDialogController',
                    controllerAs: 'vm',
                    templateUrl: eventErrorDialogTemplate,
                    locals: {content: content, title: title, contentType: contentType, showingCallback: onShowingCallback},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    targetEvent: $event,
                    multiple: true,
                    onShowing: function(scope, element) {
                        onShowingCallback.onShowing(scope, element);
                    }
                });
            }
            function showError() {
                toast.showError($translate.instant('edge.load-entity-error'));
            }
        }

        scope.checkEdgeEventType = function (type) {
            return !(type === types.edgeEventType.widgetType ||
                type === types.edgeEventType.adminSettings ||
                type === types.edgeEventType.widgetsBundle );
        }

        scope.checkTooltip = function($event) {
            var el = $event.target;
            var $el = angular.element(el);
            if(el.offsetWidth < el.scrollWidth && !$el.attr('title')){
                $el.attr('title', $el.text());
            }
        }

        $compile(element.contents())(scope);

        scope.updateStatus = function(eventCreatedTime) {
            if (scope.queueStartTs) {
                var status;
                if (eventCreatedTime < scope.queueStartTs) {
                    status = $translate.instant('edge.success');
                    scope.isPending = false;
                } else {
                    status = $translate.instant('edge.failed');
                    scope.isPending = true;
                }
                return status;
            }
        }
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
