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
        scope.hideLabels = angular.isDefined(attrs.hideLabels);

        scope.updateValidity = function() {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                ngModelCtrl.$setValidity('filter', value.type ? true : false);
            }
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.model = angular.copy(ngModelCtrl.$viewValue);
            } else {
                scope.model = {
                    type: null,
                    resolveMultiple: false
                }
            }
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
