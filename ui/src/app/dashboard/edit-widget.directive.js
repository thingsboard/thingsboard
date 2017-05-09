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

import deviceAliasesTemplate from './device-aliases.tpl.html';
import editWidgetTemplate from './edit-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EditWidgetDirective($compile, $templateCache, widgetService, deviceService, $q, $document, $mdDialog) {

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

        scope.fetchDeviceKeys = function (deviceAliasId, query, type) {
            var deviceAlias = scope.aliasesInfo.deviceAliases[deviceAliasId];
            if (deviceAlias && deviceAlias.deviceId) {
                return deviceService.getDeviceKeys(deviceAlias.deviceId, query, type);
            } else {
                return $q.when([]);
            }
        };

        scope.createDeviceAlias = function (event, alias) {

            var deferred = $q.defer();
            var singleDeviceAlias = {id: null, alias: alias, deviceFilter: null};

            $mdDialog.show({
                controller: 'DeviceAliasesController',
                controllerAs: 'vm',
                templateUrl: deviceAliasesTemplate,
                locals: {
                    config: {
                        deviceAliases: angular.copy(scope.dashboard.configuration.deviceAliases),
                        widgets: null,
                        isSingleDeviceAlias: true,
                        singleDeviceAlias: singleDeviceAlias
                    }
                },
                parent: angular.element($document[0].body),
                fullscreen: true,
                skipHide: true,
                targetEvent: event
            }).then(function (singleDeviceAlias) {
                scope.dashboard.configuration.deviceAliases[singleDeviceAlias.id] =
                            { alias: singleDeviceAlias.alias, deviceFilter: singleDeviceAlias.deviceFilter };
                deviceService.processDeviceAliases(scope.dashboard.configuration.deviceAliases).then(
                    function(resolution) {
                        if (!resolution.error) {
                            scope.aliasesInfo = resolution.aliasesInfo;
                        }
                        deferred.resolve(singleDeviceAlias);
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
