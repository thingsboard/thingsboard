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
import './entity-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySelectTemplate from './entity-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySelect($compile, $templateCache, entityService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.entityTypeCurrentTenant = types.aliasEntityType.current_tenant;

        var entityTypes = entityService.prepareAllowedEntityTypesList(scope.allowedEntityTypes, scope.useAliasEntityTypes);

        var entityTypeKeys = Object.keys(entityTypes);

        if (entityTypeKeys.length === 1) {
            scope.displayEntityTypeSelect = false;
            scope.defaultEntityType = entityTypes[entityTypeKeys[0]];
        } else {
            scope.displayEntityTypeSelect = true;
        }

        scope.model = {
            entityType: scope.defaultEntityType
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = ngModelCtrl.$viewValue;
                if (scope.model && scope.model.entityType &&
                    (scope.model.entityId || scope.model.entityType === scope.entityTypeCurrentTenant)) {
                    if (!value) {
                        value = {};
                    }
                    value.entityType = scope.model.entityType;
                    value.id = scope.model.entityId;
                    ngModelCtrl.$setViewValue(value);
                } else {
                    ngModelCtrl.$setViewValue(null);
                }
            }
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.model.entityType = value.entityType;
                scope.model.entityId = value.id;
            } else {
                scope.model.entityType = scope.defaultEntityType;
                scope.model.entityId = null;
            }
            initWatchers();
        }

        function initWatchers() {
            scope.entityTypeDeregistration = scope.$watch('model.entityType', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });

            scope.entityIdDeregistration = scope.$watch('model.entityId', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });

            scope.disabledDeregistration = scope.$watch('disabled', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    scope.updateView();
                }
            });
        }

        function destroyWatchers() {
            if (scope.entityTypeDeregistration) {
                scope.entityTypeDeregistration();
                scope.entityTypeDeregistration = null;
            }
            if (scope.entityIdDeregistration) {
                scope.entityIdDeregistration();
                scope.entityIdDeregistration = null;
            }
            if (scope.disabledDeregistration) {
                scope.disabledDeregistration();
                scope.disabledDeregistration = null;
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
            allowedEntityTypes: "=?",
            useAliasEntityTypes: "=?"
        }
    };
}
