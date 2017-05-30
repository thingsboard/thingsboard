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

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AddWidgetController($scope, widgetService, entityService, $mdDialog, $q, $document, types, dashboard, aliasesInfo, widget, widgetInfo) {

    var vm = this;

    vm.dashboard = dashboard;
    vm.aliasesInfo = aliasesInfo;
    vm.widget = widget;
    vm.widgetInfo = widgetInfo;

    vm.functionsOnly = false;

    vm.helpLinkIdForWidgetType = helpLinkIdForWidgetType;
    vm.add = add;
    vm.cancel = cancel;
    vm.fetchEntityKeys = fetchEntityKeys;
    vm.createEntityAlias = createEntityAlias;

    vm.widgetConfig = {
        config: vm.widget.config,
        layout: {}
    };

    vm.widgetConfig.layout.mobileOrder = vm.widget.config.mobileOrder;
    vm.widgetConfig.layout.mobileHeight = vm.widget.config.mobileHeight;

    var settingsSchema = vm.widgetInfo.typeSettingsSchema || widgetInfo.settingsSchema;
    var dataKeySettingsSchema = vm.widgetInfo.typeDataKeySettingsSchema || widgetInfo.dataKeySettingsSchema;

    if (!settingsSchema || settingsSchema === '') {
        vm.settingsSchema = {};
    } else {
        vm.settingsSchema = angular.fromJson(settingsSchema);
    }
    if (!dataKeySettingsSchema || dataKeySettingsSchema === '') {
        vm.dataKeySettingsSchema = {};
    } else {
        vm.dataKeySettingsSchema = angular.fromJson(dataKeySettingsSchema);
    }

    function helpLinkIdForWidgetType() {
        var link = 'widgetsConfig';
        if (vm.widget && vm.widget.type) {
            switch (vm.widget.type) {
                case types.widgetType.timeseries.value: {
                    link = 'widgetsConfigTimeseries';
                    break;
                }
                case types.widgetType.latest.value: {
                    link = 'widgetsConfigLatest';
                    break;
                }
                case types.widgetType.rpc.value: {
                    link = 'widgetsConfigRpc';
                    break;
                }
                case types.widgetType.static.value: {
                    link = 'widgetsConfigStatic';
                    break;
                }
            }
        }
        return link;
    }

    function cancel () {
        $mdDialog.cancel({aliasesInfo: vm.aliasesInfo});
    }

    function add () {
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            vm.widget.config = vm.widgetConfig.config;
            vm.widget.config.mobileOrder = vm.widgetConfig.layout.mobileOrder;
            vm.widget.config.mobileHeight = vm.widgetConfig.layout.mobileHeight;
            $mdDialog.hide({widget: vm.widget, aliasesInfo: vm.aliasesInfo});
        }
    }

    function fetchEntityKeys (entityAliasId, query, type) {
        var entityAlias = vm.aliasesInfo.entityAliases[entityAliasId];
        if (entityAlias && entityAlias.entityId) {
            return entityService.getEntityKeys(entityAlias.entityType, entityAlias.entityId, query, type);
        } else {
            return $q.when([]);
        }
    }

    function createEntityAlias (event, alias, allowedEntityTypes) {

        var deferred = $q.defer();
        var singleEntityAlias = {id: null, alias: alias, entityType: types.entityType.device, entityFilter: null};

        $mdDialog.show({
            controller: 'EntityAliasesController',
            controllerAs: 'vm',
            templateUrl: entityAliasesTemplate,
            locals: {
                config: {
                    entityAliases: angular.copy(vm.dashboard.configuration.entityAliases),
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
            vm.dashboard.configuration.entityAliases[singleEntityAlias.id] =
                { alias: singleEntityAlias.alias, entityType: singleEntityAlias.entityType, entityFilter: singleEntityAlias.entityFilter };
            entityService.processEntityAliases(vm.dashboard.configuration.entityAliases).then(
                function(resolution) {
                    if (!resolution.error) {
                        vm.aliasesInfo = resolution.aliasesInfo;
                    }
                    deferred.resolve(singleEntityAlias);
                }
            );
        }, function () {
            deferred.reject();
        });

        return deferred.promise;
    }
}
