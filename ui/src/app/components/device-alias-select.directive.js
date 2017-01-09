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
import $ from 'jquery';

import './device-alias-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import deviceAliasSelectTemplate from './device-alias-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.deviceAliasSelect', [])
    .directive('tbDeviceAliasSelect', DeviceAliasSelect)
    .name;

/*@ngInject*/
function DeviceAliasSelect($compile, $templateCache, $mdConstant) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(deviceAliasSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;

        scope.ngModelCtrl = ngModelCtrl;
        scope.deviceAliasList = [];
        scope.deviceAlias = null;

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('deviceAlias', valid);
        };

        scope.$watch('deviceAliases', function () {
            scope.deviceAliasList = [];
            for (var aliasId in scope.deviceAliases) {
                var deviceAlias = {id: aliasId, alias: scope.deviceAliases[aliasId].alias, deviceId: scope.deviceAliases[aliasId].deviceId};
                scope.deviceAliasList.push(deviceAlias);
            }
        }, true);

        scope.$watch('deviceAlias', function () {
            scope.updateView();
        });

        scope.deviceAliasSearch = function (deviceAliasSearchText) {
            return deviceAliasSearchText ? scope.deviceAliasList.filter(
                scope.createFilterForDeviceAlias(deviceAliasSearchText)) : scope.deviceAliasList;
        };

        scope.createFilterForDeviceAlias = function (query) {
            var lowercaseQuery = angular.lowercase(query);
            return function filterFn(deviceAlias) {
                return (angular.lowercase(deviceAlias.alias).indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.deviceAlias);
            scope.updateValidity();
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.deviceAlias = ngModelCtrl.$viewValue;
            }
        }

        scope.textIsNotEmpty = function(text) {
            return (text && text != null && text.length > 0) ? true : false;
        }

        scope.deviceAliasEnter = function($event) {
            if ($event.keyCode === $mdConstant.KEY_CODE.ENTER) {
                $event.preventDefault();
                if (!scope.deviceAlias) {
                    var found = scope.deviceAliasSearch(scope.deviceAliasSearchText);
                    found = found.length > 0;
                    if (!found) {
                        scope.createDeviceAlias($event, scope.deviceAliasSearchText);
                    }
                }
            }
        }

        scope.createDeviceAlias = function (event, alias) {
            var autoChild = $('#device-autocomplete', element)[0].firstElementChild;
            var el = angular.element(autoChild);
            el.scope().$mdAutocompleteCtrl.hidden = true;
            el.scope().$mdAutocompleteCtrl.hasNotFound = false;
            event.preventDefault();
            var promise = scope.onCreateDeviceAlias({event: event, alias: alias});
            if (promise) {
                promise.then(
                    function success(newAlias) {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                        if (newAlias) {
                            scope.deviceAliasList.push(newAlias);
                            scope.deviceAlias = newAlias;
                        }
                    },
                    function fail() {
                        el.scope().$mdAutocompleteCtrl.hasNotFound = true;
                    }
                );
            } else {
                el.scope().$mdAutocompleteCtrl.hasNotFound = true;
            }
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            deviceAliases: '=',
            onCreateDeviceAlias: '&'
        }
    };
}

/* eslint-enable angular/angularelement */
