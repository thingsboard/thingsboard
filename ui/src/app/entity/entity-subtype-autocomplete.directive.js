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
import './entity-subtype-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySubtypeAutocompleteTemplate from './entity-subtype-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySubtypeAutocomplete($compile, $templateCache, $q, $filter, assetService, deviceService, entityViewService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySubtypeAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.subType = null;
        scope.subTypeSearchText = '';
        scope.entitySubtypes = null;

        var comparator = function(actual, expected) {
            if (angular.isUndefined(actual)) {
                return false;
            }
            if ((actual === null) || (expected === null)) {
                return actual === expected;
            }
            return actual.startsWith(expected);
        };

        scope.fetchSubTypes = function(searchText) {
            var deferred = $q.defer();
            loadSubTypes().then(
                function success(subTypes) {
                    var result = $filter('filter')(subTypes, {'$': searchText}, comparator);
                    if (result && result.length) {
                        if (searchText && searchText.length && result.indexOf(searchText) === -1) {
                            result.push(searchText);
                        }
                        result.sort();
                        deferred.resolve(result);
                    } else {
                        deferred.resolve([searchText]);
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );
            return deferred.promise;
        }

        scope.subTypeSearchTextChanged = function() {
            //scope.subType = scope.subTypeSearchText;
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.subType);
            }
        }

        ngModelCtrl.$render = function () {
            scope.subType = ngModelCtrl.$viewValue;
        }

        scope.$watch('entityType', function () {
            load();
        });

        scope.$watch('subType', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        function loadSubTypes() {
            var deferred = $q.defer();
            if (!scope.entitySubtypes) {
                var entitySubtypesPromise;
                if (scope.entityType == types.entityType.asset) {
                    entitySubtypesPromise = assetService.getAssetTypes({ignoreLoading: true});
                } else if (scope.entityType == types.entityType.device) {
                    entitySubtypesPromise = deviceService.getDeviceTypes({ignoreLoading: true});
                } else if (scope.entityType == types.entityType.entityView) {
                    entitySubtypesPromise = entityViewService.getEntityViewTypes({ignoreLoading: true});
                }
                if (entitySubtypesPromise) {
                    entitySubtypesPromise.then(
                        function success(types) {
                            scope.entitySubtypes = [];
                            types.forEach(function (type) {
                                scope.entitySubtypes.push(type.type);
                            });
                            deferred.resolve(scope.entitySubtypes);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    deferred.reject();
                }
            } else {
                deferred.resolve(scope.entitySubtypes);
            }
            return deferred.promise;
        }

        function load() {
            if (scope.entityType == types.entityType.asset) {
                scope.selectEntitySubtypeText = 'asset.select-asset-type';
                scope.entitySubtypeText = 'asset.asset-type';
                scope.entitySubtypeRequiredText = 'asset.asset-type-required';
                scope.$on('assetSaved', function() {
                    scope.entitySubtypes = null;
                });
            } else if (scope.entityType == types.entityType.device) {
                scope.selectEntitySubtypeText = 'device.select-device-type';
                scope.entitySubtypeText = 'device.device-type';
                scope.entitySubtypeRequiredText = 'device.device-type-required';
                scope.$on('deviceSaved', function() {
                    scope.entitySubtypes = null;
                });
            } else if (scope.entityType == types.entityType.entityView) {
                scope.selectEntitySubtypeText = 'entity-view.select-entity-view-type';
                scope.entitySubtypeText = 'entity-view.entity-view-type';
                scope.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
                scope.$on('entityViewSaved', function() {
                    scope.entitySubtypes = null;
                });
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            entityType: "="
        }
    };
}
