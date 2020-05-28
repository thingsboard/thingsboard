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
export default angular.module('tbJsonToString', [])
    .directive('tbJsonToString', InputJson)
    .name;

function InputJson() {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, element, attr, ngModelCtrl) {
            function into(input) {
                try {
                    ngModelCtrl.$setValidity('invalidJSON', true);
                    return angular.fromJson(input);
                } catch (e) {
                    ngModelCtrl.$setValidity('invalidJSON', false);
                }
            }
            function out(data) {
                try {
                    ngModelCtrl.$setValidity('invalidJSON', true);
                    return angular.toJson(data);
                } catch (e) {
                    ngModelCtrl.$setValidity('invalidJSON', false);
                }
            }
            ngModelCtrl.$parsers.push(into);
            ngModelCtrl.$formatters.push(out);
        }
    };
}
