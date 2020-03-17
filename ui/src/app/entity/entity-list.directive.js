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

import entityListTemplate from './entity-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-list.scss';

/*@ngInject*/
export default function EntityListDirective($compile, $templateCache, $q, $mdUtil, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityListTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.$watch('tbRequired', function () {
            scope.updateValidity();
        });

        scope.fetchEntities = function(searchText, limit) {
             var deferred = $q.defer();
             entityService.getEntitiesByNameFilter(scope.entityType, searchText, limit, {ignoreLoading: true}).then(
                 function success(result) {
                    if (result) {
                        deferred.resolve(result);
                    } else {
                        deferred.resolve([]);
                    }
                 },
                 function fail() {
                    deferred.reject();
                 }
             );
             return deferred.promise;
         }

        scope.updateValidity = function() {
            var value = ngModelCtrl.$viewValue;
            var valid = !scope.tbRequired || value && value.length > 0;
            ngModelCtrl.$setValidity('entityList', valid);
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            var value = ngModelCtrl.$viewValue;
            scope.entityList = [];
            if (value && value.length > 0) {
                entityService.getEntities(scope.entityType, value).then(function (entities) {
                    scope.entityList = entities;
                    initWatchers();
                });
            } else {
                initWatchers();
            }
        }

        function initWatchers() {
            scope.entityTypeDeregistration = scope.$watch('entityType', function (newEntityType, prevEntityType) {
                if (!angular.equals(newEntityType, prevEntityType)) {
                    scope.entityList = [];
                }
            });
            scope.entityListDeregistration = scope.$watch('entityList', function () {
                var ids = [];
                if (scope.entityList && scope.entityList.length > 0) {
                    for (var i=0;i<scope.entityList.length;i++) {
                        ids.push(scope.entityList[i].id.id);
                    }
                }
                var value = ngModelCtrl.$viewValue;
                if (!angular.equals(ids, value)) {
                    ngModelCtrl.$setViewValue(ids);
                }
                scope.updateValidity();
            }, true);
        }

        function destroyWatchers() {
            if (scope.entityTypeDeregistration) {
                scope.entityTypeDeregistration();
                scope.entityTypeDeregistration = null;
            }
            if (scope.entityListDeregistration) {
                scope.entityListDeregistration();
                scope.entityListDeregistration = null;
            }
        }

        $compile(element.contents())(scope);

        $mdUtil.nextTick(function(){
            var inputElement = angular.element('input', element);
            inputElement.on('blur', function() {
                scope.inputTouched = true;
            } );
        });

    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            disabled:'=ngDisabled',
            tbRequired: '=?',
            entityType: '='
        }
    };

}
