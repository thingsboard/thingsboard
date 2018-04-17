/*
 * Copyright © 2016-2018 The Thingsboard Authors
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

import linkFieldsetTemplate from './link-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function LinkDirective($compile, $templateCache, $filter) {
    var linker = function (scope, element) {
        var template = $templateCache.get(linkFieldsetTemplate);
        element.html(template);

        scope.selectedLabel = null;

        scope.$watch('link', function() {
            scope.selectedLabel = null;
             if (scope.link && scope.labels) {
                 if (scope.link.label) {
                     var result = $filter('filter')(scope.labels, {name: scope.link.label});
                     if (result && result.length) {
                         scope.selectedLabel = result[0];
                     } else {
                         result = $filter('filter')(scope.labels, {custom: true});
                         if (result && result.length && result[0].custom) {
                             scope.selectedLabel = result[0];
                         }
                     }
                 }
             }
        });

        scope.selectedLabelChanged = function() {
            if (scope.link && scope.selectedLabel) {
                if (!scope.selectedLabel.custom) {
                    scope.link.label = scope.selectedLabel.name;
                } else {
                    scope.link.label = "";
                }
            }
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            link: '=',
            labels: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '='
        }
    };
}
