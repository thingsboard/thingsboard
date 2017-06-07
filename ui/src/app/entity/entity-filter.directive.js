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

import entityFilterTemplate from './entity-filter.tpl.html';
import entityFilterDialogTemplate from './entity-filter-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import EntityFilterDialogController from './entity-filter-dialog.controller';

import './entity-filter.scss';

/*@ngInject*/
export default function EntityFilterDirective($compile, $templateCache, $q, $document, $mdDialog, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityFilterTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;

     /*   scope.fetchEntities = function(searchText, limit) {
            var deferred = $q.defer();
            entityService.getEntitiesByNameFilter(scope.entityType, searchText, limit).then(function success(result) {
                if (result) {
                    deferred.resolve(result);
                } else {
                    deferred.resolve([]);
                }
            }, function fail() {
                deferred.reject();
            });
            return deferred.promise;
        }*/

        scope.updateValidity = function() {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                ngModelCtrl.$setValidity('filter', value.type ? true : false);
                /*if (value.useFilter) {
                    ngModelCtrl.$setValidity('entityList', true);
                    if (angular.isDefined(value.entityNameFilter) && value.entityNameFilter.length > 0) {
                        ngModelCtrl.$setValidity('entityNameFilter', true);
                        valid = angular.isDefined(scope.model.matchingFilterEntity) && scope.model.matchingFilterEntity != null;
                        ngModelCtrl.$setValidity('entityNameFilterEntityMatch', valid);
                    } else {
                        ngModelCtrl.$setValidity('entityNameFilter', false);
                    }
                } else {
                    ngModelCtrl.$setValidity('entityNameFilter', true);
                    ngModelCtrl.$setValidity('entityNameFilterDeviceMatch', true);
                    valid = angular.isDefined(value.entityList) && value.entityList.length > 0;
                    ngModelCtrl.$setValidity('entityList', valid);
                }*/

            }
        }

        ngModelCtrl.$render = function () {
            //destroyWatchers();
            if (ngModelCtrl.$viewValue) {
                scope.model = angular.copy(ngModelCtrl.$viewValue);
            } else {
                scope.model = {
                    type: null,
                    resolveMultiple: false
                }
            }
           /* if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var model = scope.model;
                model.useFilter = value.useFilter === true ? true: false;
                model.entityList = [];
                model.entityNameFilter = value.entityNameFilter || '';
                processEntityNameFilter(model.entityNameFilter).then(
                    function(entity) {
                        scope.model.matchingFilterEntity = entity;
                        if (value.entityList && value.entityList.length > 0) {
                            entityService.getEntities(scope.entityType, value.entityList).then(function (entities) {
                                model.entityList = entities;
                                updateMatchingEntity();
                                initWatchers();
                            });
                        } else {
                            updateMatchingEntity();
                            initWatchers();
                        }
                    }
                )
            }*/
        }

        scope.$watch('model.resolveMultiple', function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                value.resolveMultiple = scope.model.resolveMultiple;
                ngModelCtrl.$setViewValue(value);
                scope.updateValidity();
            }
        });

        scope.editFilter = function($event) {
            openEntityFilterDialog($event, false);
        }

        scope.createFilter = function($event) {
            openEntityFilterDialog($event, true);
        }

        function openEntityFilterDialog($event, isAdd) {
            $mdDialog.show({
                controller: EntityFilterDialogController,
                controllerAs: 'vm',
                templateUrl: entityFilterDialogTemplate,
                locals: {
                    isAdd: isAdd,
                    allowedEntityTypes: scope.allowedEntityTypes,
                    filter: angular.copy(scope.model)
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                skipHide: true,
                targetEvent: $event
            }).then(function (result) {
                scope.model = result.filter;
                ngModelCtrl.$setViewValue(result.filter);
                scope.updateValidity();
                if (scope.onMatchingEntityChange) {
                    scope.onMatchingEntityChange({entity: result.entity, stateEntity: result.stateEntity});
                }
            }, function () {
            });
        }

  /*      function updateMatchingEntity() {
            if (scope.model.useFilter) {
                scope.model.matchingEntity = scope.model.matchingFilterEntity;
            } else {
                if (scope.model.entityList && scope.model.entityList.length > 0) {
                    scope.model.matchingEntity = scope.model.entityList[0];
                } else {
                    scope.model.matchingEntity = null;
                }
            }
        }

        function processEntityNameFilter(entityNameFilter) {
            var deferred = $q.defer();
            if (angular.isDefined(entityNameFilter) && entityNameFilter.length > 0) {
                scope.fetchEntities(entityNameFilter, 1).then(function (entities) {
                    if (entities && entities.length > 0) {
                        deferred.resolve(entities[0]);
                    } else {
                        deferred.resolve(null);
                    }
                });
            } else {
                deferred.resolve(null);
            }
            return deferred.promise;
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
            if (scope.useFilterDeregistration) {
                scope.useFilterDeregistration();
                scope.useFilterDeregistration = null;
            }
            if (scope.entityNameFilterDeregistration) {
                scope.entityNameFilterDeregistration();
                scope.entityNameFilterDeregistration = null;
            }
            if (scope.matchingEntityDeregistration) {
                scope.matchingEntityDeregistration();
                scope.matchingEntityDeregistration = null;
            }
        }

        function initWatchers() {

            scope.entityTypeDeregistration = scope.$watch('entityType', function (newEntityType, prevEntityType) {
                if (!angular.equals(newEntityType, prevEntityType)) {
                    scope.model.entityList = [];
                    scope.model.entityNameFilter = '';
                }
            });

            scope.entityListDeregistration = scope.$watch('model.entityList', function () {
                if (ngModelCtrl.$viewValue) {
                    var value = ngModelCtrl.$viewValue;
                    value.entityList = [];
                    if (scope.model.entityList && scope.model.entityList.length > 0) {
                        for (var i=0;i<scope.model.entityList.length;i++) {
                            value.entityList.push(scope.model.entityList[i].id.id);
                        }
                    }
                    updateMatchingEntity();
                    ngModelCtrl.$setViewValue(value);
                    scope.updateValidity();
                }
            }, true);
            scope.useFilterDeregistration = scope.$watch('model.useFilter', function () {
                if (ngModelCtrl.$viewValue) {
                    var value = ngModelCtrl.$viewValue;
                    value.useFilter = scope.model.useFilter;
                    updateMatchingEntity();
                    ngModelCtrl.$setViewValue(value);
                    scope.updateValidity();
                }
            });
            scope.entityNameFilterDeregistration = scope.$watch('model.entityNameFilter', function (newNameFilter, prevNameFilter) {
                if (ngModelCtrl.$viewValue) {
                    if (!angular.equals(newNameFilter, prevNameFilter)) {
                        var value = ngModelCtrl.$viewValue;
                        value.entityNameFilter = scope.model.entityNameFilter;
                        processEntityNameFilter(value.entityNameFilter).then(
                            function(entity) {
                                scope.model.matchingFilterEntity = entity;
                                updateMatchingEntity();
                                ngModelCtrl.$setViewValue(value);
                                scope.updateValidity();
                            }
                        );
                    }
                }
            });

            scope.matchingEntityDeregistration = scope.$watch('model.matchingEntity', function (newMatchingEntity, prevMatchingEntity) {
                if (!angular.equals(newMatchingEntity, prevMatchingEntity)) {
                    if (scope.onMatchingEntityChange) {
                        scope.onMatchingEntityChange({entity: newMatchingEntity});
                    }
                }
            });
        }*/

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            allowedEntityTypes: '=?',
            onMatchingEntityChange: '&'
        }
    };

}
