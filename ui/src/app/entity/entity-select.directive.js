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
import './entity-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import entitySelectTemplate from './entity-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntitySelect($compile, $templateCache) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(entitySelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.model = {};

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = ngModelCtrl.$viewValue;
                if (scope.model && scope.model.entityType && scope.model.entityId) {
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
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.model.entityType = value.entityType;
                scope.model.entityId = value.id;
            } else {
                scope.model.entityType = null;
                scope.model.entityId = null;
            }
        }

        scope.$watch('model.entityType', function () {
            scope.updateView();
        });

        scope.$watch('model.entityId', function () {
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
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled'
        }
    };
}
