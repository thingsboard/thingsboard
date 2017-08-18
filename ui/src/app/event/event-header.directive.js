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

import eventHeaderLcEventTemplate from './event-header-lc-event.tpl.html';
import eventHeaderStatsTemplate from './event-header-stats.tpl.html';
import eventHeaderErrorTemplate from './event-header-error.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EventHeaderDirective($compile, $templateCache, types) {

    var linker = function (scope, element, attrs) {

        var getTemplate = function(eventType) {
            var template = '';
            switch(eventType) {
                case types.eventType.lcEvent.value:
                    template = eventHeaderLcEventTemplate;
                    break;
                case types.eventType.stats.value:
                    template = eventHeaderStatsTemplate;
                    break;
                case types.eventType.error.value:
                    template = eventHeaderErrorTemplate;
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

    }

    return {
        restrict: "A",
        replace: false,
        link: linker,
        scope: false
    };
}
