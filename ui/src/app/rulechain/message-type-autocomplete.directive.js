/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import './message-type-autocomplete.scss';

/* eslint-disable import/no-unresolved, import/default */

import messageTypeAutocompleteTemplate from './message-type-autocomplete.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function MessageTypeAutocomplete($compile, $templateCache, $q, $filter, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(messageTypeAutocompleteTemplate);
        element.html(template);

        var messageTypeList = [];
        for (var t in types.messageType) {
            var type = types.messageType[t];
            messageTypeList.push(type);
        }

        scope.messageType = null;
        scope.messageTypeSearchText = '';

        scope.fetchMessageTypes = function(searchText) {
            var deferred = $q.defer();
            var result = $filter('filter')(messageTypeList, {'name': searchText});
            if (result && result.length) {
                deferred.resolve(result);
            } else {
                deferred.resolve([{name: searchText, value: searchText}]);
            }
            return deferred.promise;
        };

        scope.messageTypeSearchTextChanged = function() {
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                var value = null;
                if (scope.messageType) {
                    value = scope.messageType.value;
                }
                ngModelCtrl.$setViewValue(value);
            }
        };

        ngModelCtrl.$render = function () {
            var value = ngModelCtrl.$viewValue;
            if (value) {
                var result = $filter('filter')(messageTypeList, {'value': value}, true);
                if (result && result.length) {
                    scope.messageType = result[0];
                } else {
                    scope.messageType = {
                        name: value,
                        value: value
                    };
                }
            } else {
                scope.messageType = null;
            }
        };

        scope.$watch('messageType', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            disabled:'=ngDisabled',
            required:'=ngRequired'
        }
    };
}
