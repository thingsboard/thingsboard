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
import './relation-type-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import relationTypeAutocompleteTemplate from './relation-type-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RelationTypeAutocomplete($compile, $templateCache, $q, $filter, assetService, deviceService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(relationTypeAutocompleteTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.hideLabel = angular.isDefined(attrs.hideLabel) ? true : false;

        scope.relationType = null;
        scope.relationTypeSearchText = '';
        scope.relationTypes = [];
        for (var type in types.entityRelationType) {
            scope.relationTypes.push(types.entityRelationType[type]);
        }

        scope.fetchRelationTypes = function(searchText) {
            var deferred = $q.defer();
            var result = $filter('filter')(scope.relationTypes, {'$': searchText});
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([searchText]);
            }
            return deferred.promise;
        }

        scope.relationTypeSearchTextChanged = function() {
        }

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.relationType);
            }
        }

        ngModelCtrl.$render = function () {
            scope.relationType = ngModelCtrl.$viewValue;
        }

        scope.$watch('relationType', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
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
