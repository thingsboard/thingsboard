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
import './legend-config.scss';

import $ from 'jquery';

/* eslint-disable import/no-unresolved, import/default */

import legendConfigButtonTemplate from './legend-config-button.tpl.html';
import legendConfigPanelTemplate from './legend-config-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import LegendConfigPanelController from './legend-config-panel.controller';


export default angular.module('thingsboard.directives.legendConfig', [])
    .controller('LegendConfigPanelController', LegendConfigPanelController)
    .directive('tbLegendConfig', LegendConfig)
    .name;

/* eslint-disable angular/angularelement */
/*@ngInject*/
function LegendConfig($compile, $templateCache, types, $mdPanel, $document) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        /* tbLegendConfig (ng-model)
         * {
         * 	  position: types.position.bottom.value,
         * 	  showMin: false,
         * 	  showMax: false,
         * 	  showAvg: true,
         * 	  showTotal: false
         * }
         */

        var template = $templateCache.get(legendConfigButtonTemplate);
        element.html(template);

        scope.openEditMode = function (event) {
            if (scope.disabled) {
                return;
            }
            var position;
            var panelHeight = 220;
            var panelWidth = 220;
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
                controller: 'LegendConfigPanelController',
                controllerAs: 'vm',
                templateUrl: legendConfigPanelTemplate,
                panelClass: 'tb-legend-config-panel',
                position: position,
                fullscreen: false,
                locals: {
                    'legendConfig': angular.copy(scope.model),
                    'onLegendConfigUpdate': function (legendConfig) {
                        scope.model = legendConfig;
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
            var value = {};
            var model = scope.model;
            value.position = model.position;
            value.showMin = model.showMin;
            value.showMax = model.showMax;
            value.showAvg = model.showAvg;
            value.showTotal = model.showTotal;
            ngModelCtrl.$setViewValue(value);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                if (!scope.model) {
                    scope.model = {};
                }
                var model = scope.model;
                model.position = value.position || types.position.bottom.value;
                model.showMin = angular.isDefined(value.showMin) ? value.showMin : false;
                model.showMax = angular.isDefined(value.showMax) ? value.showMax : false;
                model.showAvg = angular.isDefined(value.showAvg) ? value.showAvg : true;
                model.showTotal = angular.isDefined(value.showTotal) ? value.showTotal : false;
            } else {
                scope.model = {
                    position: types.position.bottom.value,
                    showMin: false,
                    showMax: false,
                    showAvg: true,
                    showTotal: false
                }
            }
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            disabled:'=ngDisabled'
        },
        link: linker
    };

}

/* eslint-enable angular/angularelement */