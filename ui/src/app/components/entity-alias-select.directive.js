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
import $ from 'jquery';

import './entity-alias-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityAliasSelectTemplate from './entity-alias-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.entityAliasSelect', [])
    .directive('tbEntityAliasSelect', EntityAliasSelect)
    .name;

/*@ngInject*/
function EntityAliasSelect($compile, $templateCache, $mdConstant, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entityAliasSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;

        scope.ngModelCtrl = ngModelCtrl;
        scope.entityAliasList = [];
        scope.entityAlias = null;

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('entityAlias', valid);
        };

        scope.$watch('aliasController', function () {
            scope.entityAliasList = [];
            var entityAliases = scope.aliasController.getEntityAliases();
            for (var aliasId in entityAliases) {
                if (scope.allowedEntityTypes) {
                    if (!entityService.filterAliasByEntityTypes(entityAliases[aliasId], scope.allowedEntityTypes)) {
                        continue;
                    }
                }
                scope.entityAliasList.push(entityAliases[aliasId]);
            }
        });

        scope.$watch('entityAlias', function () {
            scope.updateView();
        });

        scope.entityAliasSearch = function (entityAliasSearchText) {
            return entityAliasSearchText ? scope.entityAliasList.filter(
                scope.createFilterForEntityAlias(entityAliasSearchText)) : scope.entityAliasList;
        };

        scope.createFilterForEntityAlias = function (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(entityAlias) {
                return (angular.lowercase(entityAlias.alias).indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.entityAlias);
            scope.updateValidity();
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.entityAlias = ngModelCtrl.$viewValue;
            }
        }

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.entityAliasEnter = function($event) {
            if ($event.keyCode === $mdConstant.KEY_CODE.ENTER) {
                $event.preventDefault();
                if (!scope.entityAlias) {
                    var found = scope.entityAliasSearch(scope.entityAliasSearchText);
                    found = found.length > 0;
                    if (!found) {
                        scope.createEntityAlias($event, scope.entityAliasSearchText);
                    }
                }
            }
        }

        scope.createEntityAlias = function (event, alias) {
            var autoChild = $('#entity-autocomplete', element)[0].firstElementChild;
            var el = angular.element(autoChild);
            el.scope().$mdAutocompleteCtrl.hidden = true;
            el.scope().$mdAutocompleteCtrl.hasNotFound = false;
            event.preventDefault();
            var promise = scope.onCreateEntityAlias({event: event, alias: alias, allowedEntityTypes: scope.allowedEntityTypes});
            if (promise) {
                promise.then(
                    function success(newAlias) {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                        if (newAlias) {
                            scope.entityAliasList.push(newAlias);
                            scope.entityAlias = newAlias;
                        }
                    },
                    function fail() {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                    }
                );
            } else {
                el.scope().$mdAutocompleteCtrl.hasNotFound = true;
            }
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            aliasController: '=',
            allowedEntityTypes: '=?',
            onCreateEntityAlias: '&'
        }
    };
}

/* eslint-enable angular/angularelement */
