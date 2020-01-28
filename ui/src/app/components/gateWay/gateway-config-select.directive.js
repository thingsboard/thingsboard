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
import './gateway-config-select.scss';

/* eslint-disable import/no-unresolved, import/default */

import gatewayAliasSelectTemplate from './gateway-config-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.gatewayConfigSelect', [])
    .directive('tbGatewayConfigSelect', GatewayConfigSelect)
    .name;

/*@ngInject*/
function GatewayConfigSelect($compile, $templateCache, $mdConstant, $translate, $mdDialog) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(gatewayAliasSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;

        scope.ngModelCtrl = ngModelCtrl;
        scope.singleSelect = null;

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('singleSelect', valid);
        };

        scope.$watch('singleSelect', function () {
            scope.updateView();
        });

        scope.gatewayNameSearch = function (gatewaySearchText) {
            return gatewaySearchText ? scope.gatewayList.filter(
                scope.createFilterForGatewayName(gatewaySearchText)) : scope.gatewayList;
        };

        scope.createFilterForGatewayName = function (query) {
            var lowercaseQuery = query.toLowerCase();
            return function filterFn(device) {
                return (device.toLowerCase().indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.singleSelect);
            scope.updateValidity();
            let deviceObj = {"name": scope.singleSelect, "type": "Gateway", "additionalInfo": {
                    "gateway": true
                }};
            scope.getAccessToken(deviceObj);
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.singleSelect = ngModelCtrl.$viewValue;
            }
        };

        scope.textIsEmpty = function (str) {
            return (!str || 0 === str.length);
        };

        scope.gatewayNameEnter = function ($event) {
            if ($event.keyCode === $mdConstant.KEY_CODE.ENTER) {
                $event.preventDefault();
                let indexRes = scope.gatewayList.findIndex((element) => element.key === scope.gatewaySearchText);
                    if (indexRes === -1) {
                        scope.createNewGatewayDialog($event, {name: scope.gatewaySearchText});
                    }
            }
        };

        scope.createNewGatewayDialog = function ($event, deviceName) {
            if ($event) {
                $event.stopPropagation();
            }
            var title = $translate.instant('gateway.create-new-gateway');
            var content = $translate.instant('gateway.create-new-gateway-text', {gatewayName: deviceName.name});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(
                () => {
                    let deviceObj = {"name": deviceName.name, "type": "Gateway", "additionalInfo": {
                            "gateway": true
                        }};
                    scope.createDevice(deviceObj);
                },
                () => {
                    scope.gatewaySearchText = "";
                }
            );
        };
        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            tbRequired: '=?',
            allowedEntityTypes: '=?',
            gatewayList: '=?',
            getAccessToken: '=',
            createDevice: '=',
            theForm: '='
        }
    };
}

/* eslint-enable angular/angularelement */
