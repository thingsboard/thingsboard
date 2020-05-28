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

import gatewaySelectTemplate from './gateway-config-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.gatewayConfigSelect', [])
    .directive('tbGatewayConfigSelect', GatewayConfigSelect)
    .name;

/*@ngInject*/
function GatewayConfigSelect($compile, $templateCache, $mdConstant, $translate, $mdDialog) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        const template = $templateCache.get(gatewaySelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.gateway = null;
        scope.gatewaySearchText = '';

        scope.updateValidity = function () {
            var value = ngModelCtrl.$viewValue;
            var valid = angular.isDefined(value) && value != null || !scope.tbRequired;
            ngModelCtrl.$setValidity('gateway', valid);
        };

        function startWatchers() {
            scope.$watch('gateway', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal) && newVal !== null) {
                    scope.updateView();
                }
            });
        }

        scope.gatewayNameSearch = function (gatewaySearchText) {
            return gatewaySearchText ? scope.gatewayList.filter(
                scope.createFilterForGatewayName(gatewaySearchText)) : scope.gatewayList;
        };

        scope.createFilterForGatewayName = function (query) {
            var lowercaseQuery = query.toLowerCase();
            return function filterFn(device) {
                return (device.name.toLowerCase().indexOf(lowercaseQuery) === 0);
            };
        };

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.gateway);
            scope.updateValidity();
            scope.getAccessToken(scope.gateway.id);
        };

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.gateway = ngModelCtrl.$viewValue;
                startWatchers();
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
                    scope.createNewGatewayDialog($event, scope.gatewaySearchText);
                }
            }
        };

        scope.createNewGatewayDialog = function ($event, deviceName) {
            if ($event) {
                $event.stopPropagation();
            }
            var title = $translate.instant('gateway.create-new-gateway');
            var content = $translate.instant('gateway.create-new-gateway-text', {gatewayName: deviceName});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));
            $mdDialog.show(confirm).then(
                () => {
                    let deviceObj = {
                        name: deviceName,
                        type: "Gateway",
                        additionalInfo: {
                            gateway: true
                        }
                    };
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
            gatewayList: '=?',
            getAccessToken: '=',
            createDevice: '=',
            theForm: '='
        }
    };
}

/* eslint-enable angular/angularelement */
