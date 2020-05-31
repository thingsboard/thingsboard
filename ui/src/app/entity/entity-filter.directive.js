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

import entityFilterTemplate from './entity-filter.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './entity-filter.scss';

/*@ngInject*/
export default function EntityFilterDirective($compile, $templateCache, $q, $document, $mdDialog, types, entityService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(entityFilterTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;
        scope.types = types;
        scope.aliasFilterTypes = entityService.getAliasFilterTypesByEntityTypes(scope.allowedEntityTypes);

        scope.$watch('filter.type', function (newType, prevType) {
            if (newType && newType != prevType) {
                updateFilter();
            }
        });

        function updateFilter() {
            var filter = {};
            filter.type = scope.filter.type;
            filter.resolveMultiple = true;
            switch (filter.type) {
                case types.aliasFilterType.singleEntity.value:
                    filter.singleEntity = null;
                    filter.resolveMultiple = false;
                    break;
                case types.aliasFilterType.entityList.value:
                    filter.entityType = null;
                    filter.entityList = [];
                    break;
                case types.aliasFilterType.entityName.value:
                    filter.entityType = null;
                    filter.entityNameFilter = '';
                    break;
                case types.aliasFilterType.stateEntity.value:
                    filter.stateEntityParamName = null;
                    filter.defaultStateEntity = null;
                    filter.resolveMultiple = false;
                    break;
                case types.aliasFilterType.assetType.value:
                    filter.assetType = null;
                    filter.assetNameFilter = '';
                    break;
                case types.aliasFilterType.deviceType.value:
                    filter.deviceType = null;
                    filter.deviceNameFilter = '';
                    break;
                case types.aliasFilterType.entityViewType.value:
                    filter.entityViewType = null;
                    filter.entityViewNameFilter = '';
                    break;
                case types.aliasFilterType.relationsQuery.value:
                case types.aliasFilterType.assetSearchQuery.value:
                case types.aliasFilterType.deviceSearchQuery.value:
                case types.aliasFilterType.entityViewSearchQuery.value:
                    filter.rootStateEntity = false;
                    filter.stateEntityParamName = null;
                    filter.defaultStateEntity = null;
                    filter.rootEntity = null;
                    filter.direction = types.entitySearchDirection.from;
                    filter.maxLevel = 1;
                    filter.fetchLastLevelOnly = false;
                    if (filter.type === types.aliasFilterType.relationsQuery.value) {
                        filter.filters = [];
                    } else if (filter.type === types.aliasFilterType.assetSearchQuery.value) {
                        filter.relationType = null;
                        filter.assetTypes = [];
                    } else if (filter.type === types.aliasFilterType.deviceSearchQuery.value) {
                        filter.relationType = null;
                        filter.deviceTypes = [];
                    } else if (filter.type === types.aliasFilterType.entityViewSearchQuery.value) {
                        filter.relationType = null;
                        filter.entityViewTypes = [];
                    }
                    break;
            }
            scope.filter = filter;
        }

        scope.$watch('filter', function () {
            scope.updateView();
        });

        scope.updateView = function() {
            ngModelCtrl.$setViewValue(scope.filter);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.filter = ngModelCtrl.$viewValue;
            } else {
                scope.filter = {
                    type: null,
                    resolveMultiple: false
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
            theForm: '=',
            allowedEntityTypes: '=?'
        }
    };

}
