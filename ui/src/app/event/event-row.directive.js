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

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EventRowDirective($compile, $templateCache, $mdDialog, $document, types) {

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

        scope.checkTooltip = function($event) {
            var el = $event.target;
            var $el = angular.element(el);
            if(el.offsetWidth < el.scrollWidth && !$el.attr('title')){
                $el.attr('title', $el.text());
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
