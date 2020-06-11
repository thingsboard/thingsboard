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
import './aliases-entity-select.scss';

import $ from 'jquery';

/* eslint-disable import/no-unresolved, import/default */

import aliasesEntitySelectButtonTemplate from './aliases-entity-select-button.tpl.html';
import aliasesEntitySelectPanelTemplate from './aliases-entity-select-panel.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */
/*@ngInject*/
export default function AliasesEntitySelectDirective($compile, $templateCache, $mdMedia, types, $mdPanel, $document, $translate, $filter) {

    var linker = function (scope, element, attrs) {

        /* tbAliasesEntitySelect (ng-model)
         * {
         *    "aliasId": {
         *        alias: alias,
         *        entityType: entityType,
         *        entityId: entityId
         *    }
         * }
         */

        var template = $templateCache.get(aliasesEntitySelectButtonTemplate);

        scope.tooltipDirection = angular.isDefined(attrs.tooltipDirection) ? attrs.tooltipDirection : 'top';

        element.html(template);

        scope.openEditMode = function (event) {
            if (scope.disabled) {
                return;
            }
            var position;
            var panelHeight = $mdMedia('min-height: 350px') ? 250 : 150;
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
                xPosition = $mdPanel.xPosition.CENTER;
            } else {
                xPosition = $mdPanel.xPosition.ALIGN_START;
            }
            position = $mdPanel.newPanelPosition()
                .relativeTo(element)
                .addPanelPosition(xPosition, yPosition);
            var config = {
                attachTo: angular.element($document[0].body),
                controller: 'AliasesEntitySelectPanelController',
                controllerAs: 'vm',
                templateUrl: aliasesEntitySelectPanelTemplate,
                panelClass: 'tb-aliases-entity-select-panel',
                position: position,
                fullscreen: false,
                locals: {
                    'aliasController': scope.aliasController,
                    'entityAliasesIcon': scope.entityAliasesIcon,
                    'entityAliasesLabel': scope.entityAliasesLabel,
                    'entityAliasesList': scope.entityAliasesList,
                    'showEntityLabel': scope.showEntityLabel,
                    'onEntityAliasesUpdate': function () {
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

        scope.$on('entityAliasesChanged', function() {
            scope.updateView();
        });

        scope.$on('entityAliasResolved', function() {
            scope.updateView();
        });

        scope.$watch('entityAliasesList', function() {
            scope.updateView();
        });

        scope.updateView = function () {
            updateDisplayValue();
        }

        function updateDisplayValue() {
            var displayValue;
            var singleValue = true;
            var resolvedEntitiesLength = 0;
            var currentAliasId;
            var entityAliases = scope.aliasController.getEntityAliases();
            for (var aliasId in entityAliases) {
                var entityAlias = entityAliases[aliasId];
                if (!entityAlias.filter.resolveMultiple) {
                    var resolvedAlias = scope.aliasController.getInstantAliasInfo(aliasId);
                    if (resolvedAlias && resolvedAlias.currentEntity) {
                        var entityAliasesMatch = $filter('filter')(scope.entityAliasesList, {alias: resolvedAlias.alias});
                        if (entityAliasesMatch && entityAliasesMatch.length) {
                            if (!currentAliasId) {
                                currentAliasId = aliasId;
                            } else {
                                singleValue = false;
                                break;
                            }
                        }
                    }
                }
            }
            if (singleValue && currentAliasId) {
                var aliasInfo = scope.aliasController.getInstantAliasInfo(currentAliasId);
                resolvedEntitiesLength = aliasInfo.resolvedEntities.length;
                displayValue = aliasInfo.currentEntity.name;
            } else {
                displayValue = $translate.instant('entity.entities');
            }
            scope.displayValue = (scope.entityAliasesLabel ? scope.entityAliasesLabel + ': ' : '') + displayValue;
            scope.displayEntitySelect = !currentAliasId || (singleValue &&
                                        resolvedEntitiesLength < scope.minEntitiesToShowSelect) ? false : true;
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        scope: {
            aliasController:'=',
            entityAliasesIcon:'=',
            entityAliasesLabel:'=',
            entityAliasesList:'=',
            showEntityLabel:'=',
            minEntitiesToShowSelect: '='
        },
        link: linker
    };

}

/* eslint-enable angular/angularelement */