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
import './relation-filters.scss';

/* eslint-disable import/no-unresolved, import/default */

import relationFiltersTemplate from './relation-filters.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RelationFilters($compile, $templateCache) {

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            allowedEntityTypes: '=?'
        },
        link: linker
    };

    function linker( scope, element, attrs, ngModelCtrl ) {

        var template = $templateCache.get(relationFiltersTemplate);
        element.html(template);

        scope.relationFilters = [];

        scope.addFilter = addFilter;
        scope.removeFilter = removeFilter;

        ngModelCtrl.$render = function () {
            if (scope.relationFiltersWatch) {
                scope.relationFiltersWatch();
                scope.relationFiltersWatch = null;
            }
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.relationFilters.length = 0;
                value.forEach(function (filter) {
                    scope.relationFilters.push(filter);
                });
            }
            scope.relationFiltersWatch = scope.$watch('relationFilters', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    updateValue();
                }
            }, true);
        }

        function addFilter() {
            var filter = {
                relationType: null,
                entityTypes: []
            };
            scope.relationFilters.push(filter);
        }

        function removeFilter($event, filter) {
            var index = scope.relationFilters.indexOf(filter);
            if (index > -1) {
                scope.relationFilters.splice(index, 1);
            }
        }

        function updateValue() {
            var value = ngModelCtrl.$viewValue;
            if (!value) {
                value = [];
                ngModelCtrl.$setViewValue(value);
            } else {
                value.length = 0;
            }
            scope.relationFilters.forEach(function (filter) {
                value.push(filter);
            });
        }
        $compile(element.contents())(scope);
    }
}
