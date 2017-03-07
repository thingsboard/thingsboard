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

import './aliases-device-select.scss';

import $ from 'jquery';

/* eslint-disable import/no-unresolved, import/default */

import aliasesDeviceSelectButtonTemplate from './aliases-device-select-button.tpl.html';
import aliasesDeviceSelectPanelTemplate from './aliases-device-select-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */
/*@ngInject*/
export default function AliasesDeviceSelectDirective($compile, $templateCache, types, $mdPanel, $document, $translate) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        /* tbAliasesDeviceSelect (ng-model)
         * {
         *    "aliasId": {
         *        alias: alias,
         *        deviceId: deviceId
         *    }
         * }
         */

        var template = $templateCache.get(aliasesDeviceSelectButtonTemplate);

        scope.tooltipDirection = angular.isDefined(attrs.tooltipDirection) ? attrs.tooltipDirection : 'top';

        element.html(template);

        scope.openEditMode = function (event) {
            if (scope.disabled) {
                return;
            }
            var position;
            var panelHeight = 250;
            var panelWidth = 300;
            var offset = element[0].getBoundingClientRect();
            var bottomY = offset.bottom - $(window).scrollTop(); //eslint-disable-line
            var leftX = offset.left - $(window).scrollLeft(); //eslint-disable-line
            var yPosition;
            var xPosition;
            if (bottomY + panelHeight > $( window ).height()) { //eslint-disable-line
                yPosition = $mdPanel.yPosition.ABOVE;
            } else {
                yPosition = $mdPanel.yPosition.BELOW;
            }
            if (leftX + panelWidth > $( window ).width()) { //eslint-disable-line
                xPosition = $mdPanel.xPosition.ALIGN_END;
            } else {
                xPosition = $mdPanel.xPosition.ALIGN_START;
            }
            position = $mdPanel.newPanelPosition()
                .relativeTo(element)
                .addPanelPosition(xPosition, yPosition);
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'AliasesDeviceSelectPanelController',
                controllerAs: 'vm',
                templateUrl: aliasesDeviceSelectPanelTemplate,
                panelClass: 'tb-aliases-device-select-panel',
                position: position,
                fullscreen: false,
                locals: {
                    'deviceAliases': angular.copy(scope.model),
                    'deviceAliasesInfo': scope.deviceAliasesInfo,
                    'onDeviceAliasesUpdate': function (deviceAliases) {
                        scope.model = deviceAliases;
                        scope.updateView();
                    }
                },
                openFrom: event,
                clickOutsideToClose: true,
                escapeToClose: true,
                focusOnOpen: false
            };
            $mdPanel.open(config);
        }

        scope.updateView = function () {
            var value = angular.copy(scope.model);
            ngModelCtrl.$setViewValue(value);
            updateDisplayValue();
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                scope.model = angular.copy(value);
                updateDisplayValue();
            }
        }

        function updateDisplayValue() {
            var displayValue;
            var singleValue = true;
            var currentAliasId;
            for (var aliasId in scope.model) {
                if (!currentAliasId) {
                    currentAliasId = aliasId;
                } else {
                    singleValue = false;
                    break;
                }
            }
            if (singleValue) {
                var deviceId = scope.model[currentAliasId].deviceId;
                var devicesInfo = scope.deviceAliasesInfo[currentAliasId];
                for (var i=0;i<devicesInfo.length;i++) {
                    if (devicesInfo[i].id === deviceId) {
                        displayValue = devicesInfo[i].name;
                        break;
                    }
                }
            } else {
                displayValue = $translate.instant('device.devices');
            }
            scope.displayValue = displayValue;
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            deviceAliasesInfo:'='
        },
        link: linker
    };

}

/* eslint-enable angular/angularelement */