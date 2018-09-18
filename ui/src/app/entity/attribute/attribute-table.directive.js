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
import 'angular-material-data-table/dist/md-data-table.min.css';
import './attribute-table.scss';

/* eslint-disable import/no-unresolved, import/default */

import attributeTableTemplate from './attribute-table.tpl.html';
import addAttributeDialogTemplate from './add-attribute-dialog.tpl.html';
import addWidgetToDashboardDialogTemplate from './add-widget-to-dashboard-dialog.tpl.html';
import editAttributeValueTemplate from './edit-attribute-value.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import EditAttributeValueController from './edit-attribute-value.controller';
import AliasController from '../../api/alias-controller';

/*@ngInject*/
export default function AttributeTableDirective($compile, $templateCache, $rootScope, $q, $mdEditDialog, $mdDialog,
                                                $mdUtil, $document, $translate, $filter, $timeout, utils, types, dashboardUtils,
                                                entityService, attributeService, widgetService) {

    var linker = function (scope, element, attrs) {

        var template = $templateCache.get(attributeTableTemplate);

        element.html(template);

        var getAttributeScopeByValue = function(attributeScopeValue) {
            if (scope.types.latestTelemetry.value === attributeScopeValue) {
                return scope.types.latestTelemetry;
            }
            for (var attrScope in scope.attributeScopes) {
                if (scope.attributeScopes[attrScope].value === attributeScopeValue) {
                    return scope.attributeScopes[attrScope];
                }
            }
        }

        scope.types = types;

        scope.entityType = attrs.entityType;

        if (scope.entityType === types.entityType.device || scope.entityType === types.entityType.entityView) {
            scope.attributeScopes = types.attributesScope;
            scope.attributeScopeSelectionReadonly = false;
        } else {
            scope.attributeScopes = {};
            scope.attributeScopes.server = types.attributesScope.server;
            scope.attributeScopeSelectionReadonly = true;
        }

        scope.attributeScope = getAttributeScopeByValue(attrs.defaultAttributeScope);

        if (scope.entityType != types.entityType.device) {
            if (scope.attributeScope != types.latestTelemetry) {
                scope.attributeScope = scope.attributeScopes.server;
            }
        }

        scope.attributes = {
            count: 0,
            data: []
        };

        scope.selectedAttributes = [];
        scope.mode = 'default'; // 'widget'
        scope.subscriptionId = null;

        scope.query = {
            order: 'key',
            limit: 5,
            page: 1,
            search: null
        };

        scope.$watch("entityId", function(newVal) {
            if (newVal) {
                scope.resetFilter();
                scope.getEntityAttributes(false, true);
            }
        });

        scope.$watch("attributeScope", function(newVal, prevVal) {
            if (newVal && !angular.equals(newVal, prevVal)) {
                scope.mode = 'default';
                scope.query.search = null;
                scope.selectedAttributes = [];
                scope.getEntityAttributes(false, true);
            }
        });

        scope.resetFilter = function() {
            scope.mode = 'default';
            scope.query.search = null;
            scope.selectedAttributes = [];
            scope.attributeScope = getAttributeScopeByValue(attrs.defaultAttributeScope);
        }

        scope.enterFilterMode = function(event) {
            let $button = angular.element(event.currentTarget);
            let $toolbarsContainer = $button.closest('.toolbarsContainer');

            scope.query.search = '';

            $timeout(()=>{
                $toolbarsContainer.find('.searchInput').focus();
            })
        }

        scope.exitFilterMode = function() {
            scope.query.search = null;
            scope.getEntityAttributes();
        }

        scope.$watch("query.search", function(newVal, prevVal) {
            if (!angular.equals(newVal, prevVal) && scope.query.search != null) {
                scope.getEntityAttributes();
            }
        });

        function success(attributes, update, apply) {
            scope.attributes = attributes;
            if (!update) {
                scope.selectedAttributes = [];
            }
            if (apply) {
                scope.$digest();
            }
        }

        scope.onReorder = function() {
            scope.getEntityAttributes(false, false);
        }

        scope.onPaginate = function() {
            scope.getEntityAttributes(false, false);
        }

        scope.getEntityAttributes = function(forceUpdate, reset) {
            if (scope.attributesDeferred) {
                scope.attributesDeferred.resolve();
            }
            if (scope.entityId && scope.entityType && scope.attributeScope) {
                if (reset) {
                    scope.attributes = {
                        count: 0,
                        data: []
                    };
                }
                scope.checkSubscription();
                scope.attributesDeferred = attributeService.getEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value,
                    scope.query, function(attributes, update, apply) {
                        success(attributes, update || forceUpdate, apply);
                    }
                );
            } else {
                var deferred = $q.defer();
                scope.attributesDeferred = deferred;
                success({
                    count: 0,
                    data: []
                });
                deferred.resolve();
            }
        }

        scope.checkSubscription = function() {
            var newSubscriptionId = null;
            if (scope.entityId && scope.entityType && scope.attributeScope.clientSide && scope.mode != 'widget') {
                newSubscriptionId = attributeService.subscribeForEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value);
            }
            if (scope.subscriptionId && scope.subscriptionId != newSubscriptionId) {
                attributeService.unsubscribeForEntityAttributes(scope.subscriptionId);
            }
            scope.subscriptionId = newSubscriptionId;
        }

        scope.$on('$destroy', function() {
            if (scope.subscriptionId) {
                attributeService.unsubscribeForEntityAttributes(scope.subscriptionId);
            }
        });

        scope.editAttribute = function($event, attribute) {
            if (!scope.attributeScope.clientSide) {
                $event.stopPropagation();
                $mdEditDialog.show({
                    controller: EditAttributeValueController,
                    templateUrl: editAttributeValueTemplate,
                    locals: {attributeValue: attribute.value,
                             save: function (model) {
                                var updatedAttribute = angular.copy(attribute);
                                updatedAttribute.value = model.value;
                                attributeService.saveEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value, [updatedAttribute]).then(
                                    function success() {
                                        scope.getEntityAttributes();
                                    }
                                );
                            }},
                    targetEvent: $event
                });
            }
        }

        scope.addAttribute = function($event) {
            if (!scope.attributeScope.clientSide) {
                $event.stopPropagation();
                $mdDialog.show({
                    controller: 'AddAttributeDialogController',
                    controllerAs: 'vm',
                    templateUrl: addAttributeDialogTemplate,
                    parent: angular.element($document[0].body),
                    locals: {entityType: scope.entityType, entityId: scope.entityId, attributeScope: scope.attributeScope.value},
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {
                    scope.getEntityAttributes();
                });
            }
        }

        scope.deleteAttributes = function($event) {
            if (!scope.attributeScope.clientSide) {
                $event.stopPropagation();
                var confirm = $mdDialog.confirm()
                    .targetEvent($event)
                    .title($translate.instant('attribute.delete-attributes-title', {count: scope.selectedAttributes.length}, 'messageformat'))
                    .htmlContent($translate.instant('attribute.delete-attributes-text'))
                    .ariaLabel($translate.instant('attribute.delete-attributes'))
                    .cancel($translate.instant('action.no'))
                    .ok($translate.instant('action.yes'));
                $mdDialog.show(confirm).then(function () {
                        attributeService.deleteEntityAttributes(scope.entityType, scope.entityId, scope.attributeScope.value, scope.selectedAttributes).then(
                            function success() {
                                scope.selectedAttributes = [];
                                scope.getEntityAttributes();
                            }
                        )
                });
            }
        }

        scope.nextWidget = function() {
            $mdUtil.nextTick(function () {
                if (scope.widgetsCarousel.index < scope.widgetsList.length - 1) {
                    scope.widgetsCarousel.index++;
                }
            });
        }

        scope.prevWidget = function() {
            $mdUtil.nextTick(function () {
                if (scope.widgetsCarousel.index > 0) {
                    scope.widgetsCarousel.index--;
                }
            });
        }

        scope.enterWidgetMode = function() {

            if (scope.widgetsIndexWatch) {
                scope.widgetsIndexWatch();
                scope.widgetsIndexWatch = null;
            }

            if (scope.widgetsBundleWatch) {
                scope.widgetsBundleWatch();
                scope.widgetsBundleWatch = null;
            }

            scope.mode = 'widget';
            scope.checkSubscription();
            scope.widgetsList = [];
            scope.widgetsListCache = [];
            scope.widgetsLoaded = false;
            scope.widgetsCarousel = {
                index: 0
            }
            scope.widgetsBundle = null;
            scope.firstBundle = true;
            scope.selectedWidgetsBundleAlias = types.systemBundleAlias.cards;

            var entityAlias = {
                id: utils.guid(),
                alias: scope.entityName,
                filter: dashboardUtils.createSingleEntityFilter(scope.entityType, scope.entityId)
            };
            var entitiAliases = {};
            entitiAliases[entityAlias.id] = entityAlias;

            var stateController = {
                getStateParams: function() {
                    return {};
                }
            };
            scope.aliasController = new AliasController(scope, $q, $filter, utils,
                types, entityService, stateController, entitiAliases);

            var dataKeyType = scope.attributeScope === types.latestTelemetry ?
                types.dataKeyType.timeseries : types.dataKeyType.attribute;

            var datasource = {
                type: types.datasourceType.entity,
                entityAliasId: entityAlias.id,
                dataKeys: []
            }
            var i = 0;
            for (var attr =0; attr < scope.selectedAttributes.length;attr++) {
                var attribute = scope.selectedAttributes[attr];
                var dataKey = {
                    name: attribute.key,
                    label: attribute.key,
                    type: dataKeyType,
                    color: utils.getMaterialColor(i),
                    settings: {},
                    _hash: Math.random()
                }
                datasource.dataKeys.push(dataKey);
                i++;
            }

            scope.widgetsIndexWatch = scope.$watch('widgetsCarousel.index', function(newVal, prevVal) {
                if (scope.mode === 'widget' && (newVal != prevVal)) {
                    var index = scope.widgetsCarousel.index;
                    for (var i = 0; i < scope.widgetsList.length; i++) {
                        scope.widgetsList[i].splice(0, scope.widgetsList[i].length);
                        if (i === index) {
                            scope.widgetsList[i].push(scope.widgetsListCache[i][0]);
                        }
                    }
                }
            });

            scope.widgetsBundleWatch = scope.$watch('widgetsBundle', function(newVal, prevVal) {
                if (scope.mode === 'widget' && (scope.firstBundle === true || newVal != prevVal)) {
                    scope.widgetsList = [];
                    scope.widgetsListCache = [];
                    scope.widgetsCarousel.index = 0;
                    scope.firstBundle = false;
                    if (scope.widgetsBundle) {
                        scope.widgetsLoaded = false;
                        var bundleAlias = scope.widgetsBundle.alias;
                        var isSystem = scope.widgetsBundle.tenantId.id === types.id.nullUid;
                        widgetService.getBundleWidgetTypes(scope.widgetsBundle.alias, isSystem).then(
                            function success(widgetTypes) {

                                widgetTypes = $filter('orderBy')(widgetTypes, ['-descriptor.type','-createdTime']);

                                for (var i = 0; i < widgetTypes.length; i++) {
                                    var widgetType = widgetTypes[i];
                                    var widgetInfo = widgetService.toWidgetInfo(widgetType);
                                    if (widgetInfo.type !== types.widgetType.static.value) {
                                        var sizeX = widgetInfo.sizeX * 2;
                                        var sizeY = widgetInfo.sizeY * 2;
                                        var col = Math.floor(Math.max(0, (20 - sizeX) / 2));
                                        var widget = {
                                            isSystemType: isSystem,
                                            bundleAlias: bundleAlias,
                                            typeAlias: widgetInfo.alias,
                                            type: widgetInfo.type,
                                            title: widgetInfo.widgetName,
                                            sizeX: sizeX,
                                            sizeY: sizeY,
                                            row: 0,
                                            col: col,
                                            config: angular.fromJson(widgetInfo.defaultConfig)
                                        };

                                        widget.config.title = widgetInfo.widgetName;
                                        widget.config.datasources = [datasource];
                                        var length;
                                        if (scope.attributeScope === types.latestTelemetry && widgetInfo.type !== types.widgetType.rpc.value) {
                                            length = scope.widgetsListCache.push([widget]);
                                            scope.widgetsList.push(length === 1 ? [widget] : []);
                                        } else if (widgetInfo.type === types.widgetType.latest.value) {
                                            length = scope.widgetsListCache.push([widget]);
                                            scope.widgetsList.push(length === 1 ? [widget] : []);
                                        }
                                    }
                                }
                                scope.widgetsLoaded = true;
                            }
                        );
                    }
                }
            });
        }

        scope.exitWidgetMode = function() {
            if (scope.widgetsBundleWatch) {
                scope.widgetsBundleWatch();
                scope.widgetsBundleWatch = null;
            }
            if (scope.widgetsIndexWatch) {
                scope.widgetsIndexWatch();
                scope.widgetsIndexWatch = null;
            }
            scope.selectedWidgetsBundleAlias = null;
            scope.mode = 'default';
            scope.getEntityAttributes(true);
        }

        scope.addWidgetToDashboard = function($event) {
            if (scope.mode === 'widget' && scope.widgetsListCache.length > 0) {
                var widget = scope.widgetsListCache[scope.widgetsCarousel.index][0];
                $event.stopPropagation();
                $mdDialog.show({
                    controller: 'AddWidgetToDashboardDialogController',
                    controllerAs: 'vm',
                    templateUrl: addWidgetToDashboardDialogTemplate,
                    parent: angular.element($document[0].body),
                    locals: {entityId: scope.entityId, entityType: scope.entityType, entityName: scope.entityName, widget: angular.copy(widget)},
                    fullscreen: true,
                    targetEvent: $event
                }).then(function () {

                });
            }
        }

        scope.loading = function() {
            return $rootScope.loading;
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            entityId: '=',
            entityName: '=',
            disableAttributeScopeSelection: '@?'
        }
    };
}
