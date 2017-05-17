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
/* eslint-disable import/no-unresolved, import/default */

import entityAliasesTemplate from '../entity/entity-aliases.tpl.html';
import editWidgetTemplate from './edit-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EditWidgetDirective($compile, $templateCache, types, widgetService, entityService, $q, $document, $mdDialog) {

    var linker = function (scope, element) {
        var template = $templateCache.get(editWidgetTemplate);
        element.html(template);

        scope.$watch('widget', function () {
            if (scope.widget) {
                widgetService.getWidgetInfo(scope.widget.bundleAlias,
                    scope.widget.typeAlias,
                    scope.widget.isSystemType).then(
                    function(widgetInfo) {
                        scope.$applyAsync(function(scope) {
                            scope.widgetConfig = scope.widget.config;
                            var settingsSchema = widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
                            var dataKeySettingsSchema = widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;
                            scope.isDataEnabled = !widgetInfo.useCustomDatasources;
                            if (!settingsSchema || settingsSchema === '') {
                                scope.settingsSchema = {};
                            } else {
                                scope.settingsSchema = angular.fromJson(settingsSchema);
                            }
                            if (!dataKeySettingsSchema || dataKeySettingsSchema === '') {
                                scope.dataKeySettingsSchema = {};
                            } else {
                                scope.dataKeySettingsSchema = angular.fromJson(dataKeySettingsSchema);
                            }

                            scope.functionsOnly = scope.dashboard ? false : true;

                            scope.theForm.$setPristine();
                        });
                    }
                );
            }
        });

        scope.fetchEntityKeys = function (entityAliasId, query, type) {
            var entityAlias = scope.aliasesInfo.entityAliases[entityAliasId];
            if (entityAlias && entityAlias.entityId) {
                return entityService.getEntityKeys(entityAlias.entityType, entityAlias.entityId, query, type);
            } else {
                return $q.when([]);
            }
        };

        scope.createEntityAlias = function (event, alias, allowedEntityTypes) {

            var deferred = $q.defer();
            var singleEntityAlias = {id: null, alias: alias, entityType: types.entityType.device, entityFilter: null};

            $mdDialog.show({
                controller: 'EntityAliasesController',
                controllerAs: 'vm',
                templateUrl: entityAliasesTemplate,
                locals: {
                    config: {
                        entityAliases: angular.copy(scope.dashboard.configuration.entityAliases),
                        widgets: null,
                        isSingleEntityAlias: true,
                        singleEntityAlias: singleEntityAlias,
                        allowedEntityTypes: allowedEntityTypes
                    }
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                skipHide: true,
                targetEvent: event
            }).then(function (singleEntityAlias) {
                scope.dashboard.configuration.entityAliases[singleEntityAlias.id] =
                            { alias: singleEntityAlias.alias, entityType: singleEntityAlias.entityType, entityFilter: singleEntityAlias.entityFilter };
                entityService.processEntityAliases(scope.dashboard.configuration.entityAliases).then(
                    function(resolution) {
                        if (!resolution.error) {
                            scope.aliasesInfo = resolution.aliasesInfo;
                        }
                        deferred.resolve(singleEntityAlias);
                    }
                );
            }, function () {
                deferred.reject();
            });

            return deferred.promise;
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            dashboard: '=',
            aliasesInfo: '=',
            widget: '=',
            theForm: '='
        }
    };
}
