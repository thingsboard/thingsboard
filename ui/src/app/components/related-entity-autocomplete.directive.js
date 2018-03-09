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
import './related-entity-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import relatedEntityAutocompleteTemplate from './related-entity-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.relatedEntityAutocomplete', [])
    .directive('tbRelatedEntityAutocomplete', RelatedEntityAutocomplete)
    .name;

/*@ngInject*/
function RelatedEntityAutocomplete($compile, $templateCache, $q, $filter, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(relatedEntityAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entity = null;
        scope.entitySearchText = '';

        scope.allEntities = null;

        scope.fetchEntities = function(searchText) {
            var deferred = $q.defer();
            if (!scope.allEntities) {
                entityService.getRelatedEntities(scope.rootEntityId, scope.entityType, scope.entitySubtypes, -1, []).then(
                    function success(entities) {
                        if (scope.excludeEntityIds && scope.excludeEntityIds.length) {
                            var filteredEntities = [];
                            entities.forEach(function(entity) {
                                if (scope.excludeEntityIds.indexOf(entity.id.id) == -1) {
                                    filteredEntities.push(entity);
                                }
                            });
                            entities = filteredEntities;
                        }
                        scope.allEntities = entities;
                        filterEntities(searchText, deferred);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                filterEntities(searchText, deferred);
            }
            return deferred.promise;
        }

        function filterEntities(searchText, deferred) {
            var result = $filter('filter')(scope.allEntities, {name: searchText});
            deferred.resolve(result);
        }

        scope.entitySearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.entity ? scope.entity.id : null);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                entityService.getRelatedEntity(ngModelCtrl.$viewValue).then(
                    function success(entity) {
                        scope.entity = entity;
                    },
                    function fail() {
                        scope.entity = null;
                    }
                );
            } else {
                scope.entity = null;
            }
        }

        scope.$watch('entity', function () {
            scope.updateView();
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            rootEntityId: '=',
            entityType: '=',
            entitySubtypes: '=',
            excludeEntityIds: '=?',
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            placeholderText: '@',
            notFoundText: '@',
            requiredText: '@'
        }
    };
}
