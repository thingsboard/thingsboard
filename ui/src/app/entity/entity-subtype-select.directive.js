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
import './entity-subtype-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySubtypeSelectTemplate from './entity-subtype-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySubtypeSelect($compile, $templateCache, $translate, assetService, deviceService, entityViewService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySubtypeSelectTemplate);
        element.html(template);

        if (angular.isDefined(attrs.hideLabel)) {
            scope.showLabel = false;
        } else {
            scope.showLabel = true;
        }

        scope.ngModelCtrl = ngModelCtrl;

        scope.entitySubtypes = [];

        scope.subTypeName = function(subType) {
            if (subType && subType.length) {
                if (scope.typeTranslatePrefix) {
                    return $translate.instant(scope.typeTranslatePrefix + '.' + subType);
                } else {
                    return subType;
                }
            } else {
                return $translate.instant('entity.all-subtypes');
            }
        }

        scope.$watch('entityType', function () {
            load();
        });

        scope.$watch('entitySubtype', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.entitySubtype);
        };

        ngModelCtrl.$render = function () {
            scope.entitySubtype = ngModelCtrl.$viewValue;
        };

        function loadSubTypes() {
            scope.entitySubtypes = [];
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
                        scope.entitySubtypes.push('');
                        types.forEach(function(type) {
                            scope.entitySubtypes.push(type.type);
                        });
                        if (scope.entitySubtypes.indexOf(scope.entitySubtype) == -1) {
                            scope.entitySubtype = '';
                        }
                    },
                    function fail() {}
                );
            }

        }

        function load() {
            if (scope.entityType == types.entityType.asset) {
                scope.entitySubtypeTitle = 'asset.asset-type';
                scope.entitySubtypeRequiredText = 'asset.asset-type-required';
            } else if (scope.entityType == types.entityType.device) {
                scope.entitySubtypeTitle = 'device.device-type';
                scope.entitySubtypeRequiredText = 'device.device-type-required';
            } else if (scope.entityType == types.entityType.entityView) {
                scope.entitySubtypeTitle = 'entity-view.entity-view-type';
                scope.entitySubtypeRequiredText = 'entity-view.entity-view-type-required';
            }
            scope.entitySubtypes.length = 0;
            if (scope.entitySubtypesList && scope.entitySubtypesList.length) {
                scope.entitySubtypesList.forEach(function(subType) {
                    scope.entitySubtypes.push(subType);
                });
            } else {
                loadSubTypes();
                if (scope.entityType == types.entityType.asset) {
                    scope.$on('assetSaved', function() {
                        loadSubTypes();
                    });
                } else if (scope.entityType == types.entityType.device) {
                    scope.$on('deviceSaved', function() {
                        loadSubTypes();
                    });
                } else if (scope.entityType == types.entityType.entityView) {
                    scope.$on('entityViewSaved', function() {
                        loadSubTypes();
                    });
                }
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
            entityType: "=",
            entitySubtypesList: "=?",
            typeTranslatePrefix: "@?"
        }
    };
}
