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
/* eslint-disable import/no-unresolved, import/default */

import entityAliasDialogTemplate from '../entity/alias/entity-alias-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function AddWidgetController($scope, widgetService, entityService, $mdDialog, $q, $document, types, dashboard,
                                            aliasController, widget, widgetInfo) {

    var vm = this;

    vm.dashboard = dashboard;
    vm.aliasController = aliasController;
    vm.widget = widget;
    vm.widgetInfo = widgetInfo;

    vm.functionsOnly = false;

    vm.helpLinkIdForWidgetType = helpLinkIdForWidgetType;
    vm.add = add;
    vm.cancel = cancel;
    vm.fetchEntityKeys = fetchEntityKeys;
    vm.fetchDashboardStates = fetchDashboardStates;
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
                case types.widgetType.alarm.value: {
                    link = 'widgetsConfigAlarm';
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
        $mdDialog.cancel();
    }

    function add () {
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            vm.widget.config = vm.widgetConfig.config;
            vm.widget.config.mobileOrder = vm.widgetConfig.layout.mobileOrder;
            vm.widget.config.mobileHeight = vm.widgetConfig.layout.mobileHeight;
            $mdDialog.hide({widget: vm.widget});
        }
    }

    function fetchEntityKeys (entityAliasId, query, type) {
        var deferred = $q.defer();
        vm.aliasController.getAliasInfo(entityAliasId).then(
            function success(aliasInfo) {
                var entity = aliasInfo.currentEntity;
                if (entity) {
                    entityService.getEntityKeys(entity.entityType, entity.id, query, type, {ignoreLoading: true}).then(
                        function success(keys) {
                            deferred.resolve(keys);
                        },
                        function fail() {
                            deferred.resolve([]);
                        }
                    );
                } else {
                    deferred.resolve([]);
                }
            },
            function fail() {
                deferred.resolve([]);
            }
        );
        return deferred.promise;
    }

    function fetchDashboardStates (query) {
        var deferred = $q.defer();
        var stateIds = Object.keys(vm.dashboard.configuration.states);
        var result = query ? stateIds.filter(
            createFilterForDashboardState(query)) : stateIds;
        if (result && result.length) {
            deferred.resolve(result);
        } else {
            deferred.resolve([query]);
        }
        return deferred.promise;
    }

    function createFilterForDashboardState (query) {
        var lowercaseQuery = angular.lowercase(query);
        return function filterFn(stateId) {
            return (angular.lowercase(stateId).indexOf(lowercaseQuery) === 0);
        };
    }

    function createEntityAlias (event, alias, allowedEntityTypes) {

        var deferred = $q.defer();
        var singleEntityAlias = {id: null, alias: alias, filter: {}};

        $mdDialog.show({
            controller: 'EntityAliasDialogController',
            controllerAs: 'vm',
            templateUrl: entityAliasDialogTemplate,
            locals: {
                isAdd: true,
                allowedEntityTypes: allowedEntityTypes,
                entityAliases: vm.dashboard.configuration.entityAliases,
                alias: singleEntityAlias
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            skipHide: true,
            targetEvent: event
        }).then(function (singleEntityAlias) {
            vm.dashboard.configuration.entityAliases[singleEntityAlias.id] = singleEntityAlias;
            vm.aliasController.updateEntityAliases(vm.dashboard.configuration.entityAliases);
            deferred.resolve(singleEntityAlias);
        }, function () {
            deferred.reject();
        });

        return deferred.promise;
    }
}
