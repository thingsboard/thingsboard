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
/* eslint-disable import/no-unresolved, import/default*/

import importDialogTemplate from './import-dialog.tpl.html';
import importDialogCSVTemplate from './import-dialog-csv.tpl.html';
import entityAliasesTemplate from '../entity/alias/entity-aliases.tpl.html';
import * as JSZip from 'jszip';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable no-undef, angular/window-service, angular/document-service */

/*@ngInject*/
export default function ImportExport($log, $translate, $q, $mdDialog, $document, $http, itembuffer, utils, types, $rootScope,
                                     dashboardUtils, entityService, dashboardService, ruleChainService, widgetService, toast, attributeService) {

    const ZIP_TYPE = {
        mimeType: 'application/zip',
        extension: 'zip'
    };

    var service = {
        exportDashboard: exportDashboard,
        importDashboard: importDashboard,
        exportWidget: exportWidget,
        importWidget: importWidget,
        exportRuleChain: exportRuleChain,
        importRuleChain: importRuleChain,
        exportWidgetType: exportWidgetType,
        importWidgetType: importWidgetType,
        exportWidgetsBundle: exportWidgetsBundle,
        importWidgetsBundle: importWidgetsBundle,
        exportJSZip: exportJSZip,
        exportExtension: exportExtension,
        importExtension: importExtension,
        importEntities: importEntities,
        convertCSVToJson: convertCSVToJson,
        exportToPc: exportToPc,
        createMultiEntity: createMultiEntity
    };

    return service;

    // Widgets bundle functions

    function exportWidgetsBundle(widgetsBundleId) {
        widgetService.getWidgetsBundle(widgetsBundleId).then(
            function success(widgetsBundle) {
                var bundleAlias = widgetsBundle.alias;
                var isSystem = widgetsBundle.tenantId.id === types.id.nullUid;
                widgetService.getBundleWidgetTypes(bundleAlias, isSystem).then(
                    function success(widgetTypes) {
                        prepareExport(widgetsBundle);
                        var widgetsBundleItem = {
                            widgetsBundle: prepareExport(widgetsBundle),
                            widgetTypes: []
                        };
                        for (var t in widgetTypes) {
                            var widgetType = widgetTypes[t];
                            if (angular.isDefined(widgetType.bundleAlias)) {
                                delete widgetType.bundleAlias;
                            }
                            widgetsBundleItem.widgetTypes.push(prepareExport(widgetType));
                        }
                        var name = widgetsBundle.title;
                        name = name.toLowerCase().replace(/\W/g, "_");
                        exportToPc(widgetsBundleItem, name + '.json');
                    },
                    function fail(rejection) {
                        var message = rejection;
                        if (!message) {
                            message = $translate.instant('error.unknown-error');
                        }
                        toast.showError($translate.instant('widgets-bundle.export-failed-error', {error: message}));
                    }
                );
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('widgets-bundle.export-failed-error', {error: message}));
            }
        );
    }

    function importNextWidgetType(widgetTypes, bundleAlias, index, deferred) {
        if (!widgetTypes || widgetTypes.length <= index) {
            deferred.resolve();
        } else {
            var widgetType = widgetTypes[index];
            widgetType.bundleAlias = bundleAlias;
            widgetService.saveImportedWidgetType(widgetType).then(
                function success() {
                    index++;
                    importNextWidgetType(widgetTypes, bundleAlias, index, deferred);
                },
                function fail() {
                    deferred.reject();
                }
            );

        }
    }

    function importWidgetsBundle($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'widgets-bundle.import', 'widgets-bundle.widgets-bundle-file').then(
            function success(widgetsBundleItem) {
                if (!validateImportedWidgetsBundle(widgetsBundleItem)) {
                    toast.showError($translate.instant('widgets-bundle.invalid-widgets-bundle-file-error'));
                    deferred.reject();
                } else {
                    var widgetsBundle = widgetsBundleItem.widgetsBundle;
                    widgetService.saveWidgetsBundle(widgetsBundle).then(
                        function success(savedWidgetsBundle) {
                            var bundleAlias = savedWidgetsBundle.alias;
                            var widgetTypes = widgetsBundleItem.widgetTypes;
                            importNextWidgetType(widgetTypes, bundleAlias, 0, deferred);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidgetsBundle(widgetsBundleItem) {
        if (angular.isUndefined(widgetsBundleItem.widgetsBundle)) {
            return false;
        }
        if (angular.isUndefined(widgetsBundleItem.widgetTypes)) {
            return false;
        }
        var widgetsBundle = widgetsBundleItem.widgetsBundle;
        if (angular.isUndefined(widgetsBundle.title)) {
            return false;
        }
        var widgetTypes = widgetsBundleItem.widgetTypes;
        for (var t in widgetTypes) {
            var widgetType = widgetTypes[t];
            if (!validateImportedWidgetType(widgetType)) {
                return false;
            }
        }

        return true;
    }

    // Widget type functions

    function exportWidgetType(widgetTypeId) {
        widgetService.getWidgetTypeById(widgetTypeId).then(
            function success(widgetType) {
                if (angular.isDefined(widgetType.bundleAlias)) {
                    delete widgetType.bundleAlias;
                }
                var name = widgetType.name;
                name = name.toLowerCase().replace(/\W/g, "_");
                exportToPc(prepareExport(widgetType), name + '.json');
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('widget-type.export-failed-error', {error: message}));
            }
        );
    }

    function importWidgetType($event, bundleAlias) {
        var deferred = $q.defer();
        openImportDialog($event, 'widget-type.import', 'widget-type.widget-type-file').then(
            function success(widgetType) {
                if (!validateImportedWidgetType(widgetType)) {
                    toast.showError($translate.instant('widget-type.invalid-widget-type-file-error'));
                    deferred.reject();
                } else {
                    widgetType.bundleAlias = bundleAlias;
                    widgetService.saveImportedWidgetType(widgetType).then(
                        function success() {
                            deferred.resolve();
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidgetType(widgetType) {
        if (angular.isUndefined(widgetType.name)
            || angular.isUndefined(widgetType.descriptor)) {
            return false;
        }
        return true;
    }

    // Rule chain functions

    function exportRuleChain(ruleChainId) {
        ruleChainService.getRuleChain(ruleChainId).then(
            (ruleChain) => {
                ruleChainService.getRuleChainMetaData(ruleChainId).then(
                    (ruleChainMetaData) => {
                        var ruleChainExport = {
                            ruleChain: prepareRuleChain(ruleChain),
                            metadata: prepareRuleChainMetaData(ruleChainMetaData)
                        };
                        var name = ruleChain.name;
                        name = name.toLowerCase().replace(/\W/g, "_");
                        exportToPc(ruleChainExport, name + '.json');
                    },
                    (rejection) => {
                        processExportRuleChainRejection(rejection);
                    }
                );
            },
            (rejection) => {
                processExportRuleChainRejection(rejection);
            }
        );
    }

    function prepareRuleChain(ruleChain) {
        ruleChain = prepareExport(ruleChain);
        if (ruleChain.firstRuleNodeId) {
            ruleChain.firstRuleNodeId = null;
        }
        ruleChain.root = false;
        return ruleChain;
    }

    function prepareRuleChainMetaData(ruleChainMetaData) {
        delete ruleChainMetaData.ruleChainId;
        for (var i = 0; i < ruleChainMetaData.nodes.length; i++) {
            var node = ruleChainMetaData.nodes[i];
            delete node.ruleChainId;
            ruleChainMetaData.nodes[i] = prepareExport(node);
        }
        return ruleChainMetaData;
    }

    function processExportRuleChainRejection(rejection) {
        var message = rejection;
        if (!message) {
            message = $translate.instant('error.unknown-error');
        }
        toast.showError($translate.instant('rulechain.export-failed-error', {error: message}));
    }

    function importRuleChain($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'rulechain.import', 'rulechain.rulechain-file').then(
            function success(ruleChainImport) {
                if (!validateImportedRuleChain(ruleChainImport)) {
                    toast.showError($translate.instant('rulechain.invalid-rulechain-file-error'));
                    deferred.reject();
                } else {
                    deferred.resolve(ruleChainImport);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedRuleChain(ruleChainImport) {
        if (angular.isUndefined(ruleChainImport.ruleChain)) {
            return false;
        }
        if (angular.isUndefined(ruleChainImport.metadata)) {
            return false;
        }
        if (angular.isUndefined(ruleChainImport.ruleChain.name)) {
            return false;
        }
        return true;
    }

    // Widget functions

    function exportWidget(dashboard, sourceState, sourceLayout, widget) {
        var widgetItem = itembuffer.prepareWidgetItem(dashboard, sourceState, sourceLayout, widget);
        var name = widgetItem.widget.config.title;
        name = name.toLowerCase().replace(/\W/g, "_");
        exportToPc(prepareExport(widgetItem), name + '.json');
    }

    function prepareAliasesInfo(aliasesInfo) {
        var datasourceAliases = aliasesInfo.datasourceAliases;
        var targetDeviceAliases = aliasesInfo.targetDeviceAliases;
        var datasourceIndex;
        if (datasourceAliases || targetDeviceAliases) {
            if (datasourceAliases) {
                for (datasourceIndex in datasourceAliases) {
                    datasourceAliases[datasourceIndex] = prepareEntityAlias(datasourceAliases[datasourceIndex]);
                }
            }
            if (targetDeviceAliases) {
                for (datasourceIndex in targetDeviceAliases) {
                    targetDeviceAliases[datasourceIndex] = prepareEntityAlias(targetDeviceAliases[datasourceIndex]);
                }
            }
        }
        return aliasesInfo;
    }

    function prepareEntityAlias(aliasInfo) {
        var alias;
        var filter;
        if (aliasInfo.deviceId) {
            alias = aliasInfo.aliasName;
            filter = {
                type: types.aliasFilterType.entityList.value,
                entityType: types.entityType.device,
                entityList: [aliasInfo.deviceId],
                resolveMultiple: false
            };
        } else if (aliasInfo.deviceFilter) {
            alias = aliasInfo.aliasName;
            filter = {
                type: aliasInfo.deviceFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value,
                entityType: types.entityType.device,
                resolveMultiple: false
            }
            if (filter.type == types.aliasFilterType.entityList.value) {
                filter.entityList = aliasInfo.deviceFilter.deviceList
            } else {
                filter.entityNameFilter = aliasInfo.deviceFilter.deviceNameFilter;
            }
        } else if (aliasInfo.entityFilter) {
            alias = aliasInfo.aliasName;
            filter = {
                type: aliasInfo.entityFilter.useFilter ? types.aliasFilterType.entityName.value : types.aliasFilterType.entityList.value,
                entityType: aliasInfo.entityType,
                resolveMultiple: false
            }
            if (filter.type == types.aliasFilterType.entityList.value) {
                filter.entityList = aliasInfo.entityFilter.entityList;
            } else {
                filter.entityNameFilter = aliasInfo.entityFilter.entityNameFilter;
            }
        } else {
            alias = aliasInfo.alias;
            filter = aliasInfo.filter;
        }
        return {
            alias: alias,
            filter: filter
        };
    }

    function importWidget($event, dashboard, targetState, targetLayoutFunction, onAliasesUpdateFunction) {
        var deferred = $q.defer();
        openImportDialog($event, 'dashboard.import-widget', 'dashboard.widget-file').then(
            function success(widgetItem) {
                if (!validateImportedWidget(widgetItem)) {
                    toast.showError($translate.instant('dashboard.invalid-widget-file-error'));
                    deferred.reject();
                } else {
                    var widget = widgetItem.widget;
                    widget = dashboardUtils.validateAndUpdateWidget(widget);
                    var aliasesInfo = prepareAliasesInfo(widgetItem.aliasesInfo);
                    var originalColumns = widgetItem.originalColumns;
                    var originalSize = widgetItem.originalSize;

                    var datasourceAliases = aliasesInfo.datasourceAliases;
                    var targetDeviceAliases = aliasesInfo.targetDeviceAliases;
                    if (datasourceAliases || targetDeviceAliases) {
                        var entityAliases = {};
                        var datasourceAliasesMap = {};
                        var targetDeviceAliasesMap = {};
                        var aliasId;
                        var datasourceIndex;
                        if (datasourceAliases) {
                            for (datasourceIndex in datasourceAliases) {
                                aliasId = utils.guid();
                                datasourceAliasesMap[aliasId] = datasourceIndex;
                                entityAliases[aliasId] = datasourceAliases[datasourceIndex];
                                entityAliases[aliasId].id = aliasId;
                            }
                        }
                        if (targetDeviceAliases) {
                            for (datasourceIndex in targetDeviceAliases) {
                                aliasId = utils.guid();
                                targetDeviceAliasesMap[aliasId] = datasourceIndex;
                                entityAliases[aliasId] = targetDeviceAliases[datasourceIndex];
                                entityAliases[aliasId].id = aliasId;
                            }
                        }

                        var aliasIds = Object.keys(entityAliases);
                        if (aliasIds.length > 0) {
                            processEntityAliases(entityAliases, aliasIds).then(
                                function (missingEntityAliases) {
                                    if (Object.keys(missingEntityAliases).length > 0) {
                                        editMissingAliases($event, [widget],
                                            true, 'dashboard.widget-import-missing-aliases-title', missingEntityAliases).then(
                                            function success(updatedEntityAliases) {
                                                for (var aliasId in updatedEntityAliases) {
                                                    var entityAlias = updatedEntityAliases[aliasId];
                                                    var datasourceIndex;
                                                    if (datasourceAliasesMap[aliasId]) {
                                                        datasourceIndex = datasourceAliasesMap[aliasId];
                                                        datasourceAliases[datasourceIndex] = entityAlias;
                                                    } else if (targetDeviceAliasesMap[aliasId]) {
                                                        datasourceIndex = targetDeviceAliasesMap[aliasId];
                                                        targetDeviceAliases[datasourceIndex] = entityAlias;
                                                    }
                                                }
                                                addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                                    aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                                            },
                                            function fail() {
                                                deferred.reject();
                                            }
                                        );
                                    } else {
                                        addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                            aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                                    }
                                }
                            );
                        } else {
                            addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                                aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                        }
                    } else {
                        addImportedWidget(dashboard, targetState, targetLayoutFunction, $event, widget,
                            aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred);
                    }
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function validateImportedWidget(widgetItem) {
        if (angular.isUndefined(widgetItem.widget)
            || angular.isUndefined(widgetItem.aliasesInfo)
            || angular.isUndefined(widgetItem.originalColumns)) {
            return false;
        }
        var widget = widgetItem.widget;
        if (angular.isUndefined(widget.isSystemType) ||
            angular.isUndefined(widget.bundleAlias) ||
            angular.isUndefined(widget.typeAlias) ||
            angular.isUndefined(widget.type)) {
            return false;
        }
        return true;
    }

    function addImportedWidget(dashboard, targetState, targetLayoutFunction, event, widget,
                               aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, deferred) {
        targetLayoutFunction(event).then(
            function success(targetLayout) {
                itembuffer.addWidgetToDashboard(dashboard, targetState, targetLayout, widget,
                    aliasesInfo, onAliasesUpdateFunction, originalColumns, originalSize, -1, -1).then(
                    function () {
                        deferred.resolve(
                            {
                                widget: widget,
                                layoutId: targetLayout
                            }
                        );
                    }
                );
            },
            function fail() {
                deferred.reject();
            }
        );
    }

    // Dashboard functions

    function exportDashboard(dashboardId) {
        dashboardService.getDashboard(dashboardId).then(
            function success(dashboard) {
                var name = dashboard.title;
                name = name.toLowerCase().replace(/\W/g, "_");
                exportToPc(prepareDashboardExport(dashboard), name + '.json');
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('dashboard.export-failed-error', {error: message}));
            }
        );
    }

    function prepareDashboardExport(dashboard) {
        dashboard = prepareExport(dashboard);
        delete dashboard.assignedCustomers;
        delete dashboard.publicCustomerId;
        delete dashboard.assignedCustomersText;
        delete dashboard.assignedCustomersIds;
        return dashboard;
    }

    function importDashboard($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'dashboard.import', 'dashboard.dashboard-file').then(
            function success(dashboard) {
                if (!validateImportedDashboard(dashboard)) {
                    toast.showError($translate.instant('dashboard.invalid-dashboard-file-error'));
                    deferred.reject();
                } else {
                    dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    var entityAliases = dashboard.configuration.entityAliases;
                    if (entityAliases) {
                        var aliasIds = Object.keys(entityAliases);
                        if (aliasIds.length > 0) {
                            processEntityAliases(entityAliases, aliasIds).then(
                                function (missingEntityAliases) {
                                    if (Object.keys(missingEntityAliases).length > 0) {
                                        editMissingAliases($event, dashboard.configuration.widgets,
                                            false, 'dashboard.dashboard-import-missing-aliases-title', missingEntityAliases).then(
                                            function success(updatedEntityAliases) {
                                                for (var aliasId in updatedEntityAliases) {
                                                    entityAliases[aliasId] = updatedEntityAliases[aliasId];
                                                }
                                                saveImportedDashboard(dashboard, deferred);
                                            },
                                            function fail() {
                                                deferred.reject();
                                            }
                                        );
                                    } else {
                                        saveImportedDashboard(dashboard, deferred);
                                    }
                                }
                            )
                        } else {
                            saveImportedDashboard(dashboard, deferred);
                        }
                    } else {
                        saveImportedDashboard(dashboard, deferred);
                    }
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function importEntities($event, entityType) {
        var deferred = $q.defer();

        switch (entityType) {
            case types.entityType.device:
                openImportDialogCSV($event, entityType, 'device.import', 'device.device-file').then(
                    function success() {
                        deferred.resolve();
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                return deferred.promise;
            case types.entityType.asset:
                openImportDialogCSV($event, entityType, 'asset.import', 'asset.asset-file').then(
                    function success() {
                        deferred.resolve();
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
                return deferred.promise;
        }

    }

    function saveImportedDashboard(dashboard, deferred) {
        dashboardService.saveDashboard(dashboard).then(
            function success() {
                deferred.resolve();
            },
            function fail() {
                deferred.reject();
            }
        )
    }

    function validateImportedDashboard(dashboard) {
        if (angular.isUndefined(dashboard.title) || angular.isUndefined(dashboard.configuration)) {
            return false;
        }
        return true;
    }


    function exportExtension(extensionId) {

        getExtension(extensionId)
            .then(
                function success(extension) {
                    var name = extension.title;
                    name = name.toLowerCase().replace(/\W/g, "_");
                    exportToPc(prepareExport(extension), name + '.json');
                },
                function fail(rejection) {
                    var message = rejection;
                    if (!message) {
                        message = $translate.instant('error.unknown-error');
                    }
                    toast.showError($translate.instant('extension.export-failed-error', {error: message}));
                }
            );

        function getExtension(extensionId) {
            var deferred = $q.defer();
            var url = '/api/plugins/telemetry/DEVICE/' + extensionId;
            $http.get(url, null)
                .then(function success(response) {
                    deferred.resolve(response.data);
                }, function fail() {
                    deferred.reject();
                });
            return deferred.promise;
        }

    }

    function importExtension($event, options) {
        var deferred = $q.defer();
        openImportDialog($event, 'extension.import-extensions', 'extension.file')
            .then(
                function success(extension) {
                    if (!validateImportedExtension(extension)) {
                        toast.showError($translate.instant('extension.invalid-file-error'));
                        deferred.reject();
                    } else {
                        attributeService
                            .saveEntityAttributes(
                                options.entityType,
                                options.entityId,
                                types.attributesScope.shared.value,
                                [{
                                    key: "configuration",
                                    value: angular.toJson(extension)
                                }]
                            )
                            .then(function success() {
                                options.successFunc();
                            });
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );
        return deferred.promise;
    }

    function validateImportedExtension(configuration) {
        if (configuration.length) {
            for (let i = 0; i < configuration.length; i++) {
                if (angular.isUndefined(configuration[i].configuration) || angular.isUndefined(configuration[i].id) || angular.isUndefined(configuration[i].type)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    function processEntityAliases(entityAliases, aliasIds) {
        var deferred = $q.defer();
        var missingEntityAliases = {};
        var index = -1;
        checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
        return deferred.promise;
    }

    function checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred) {
        index++;
        if (index == aliasIds.length) {
            deferred.resolve(missingEntityAliases);
        } else {
            checkEntityAlias(index, aliasIds, entityAliases, missingEntityAliases, deferred);
        }
    }

    function checkEntityAlias(index, aliasIds, entityAliases, missingEntityAliases, deferred) {
        var aliasId = aliasIds[index];
        var entityAlias = entityAliases[aliasId];
        entityService.checkEntityAlias(entityAlias).then(
            function (result) {
                if (result) {
                    checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
                } else {
                    var missingEntityAlias = angular.copy(entityAlias);
                    missingEntityAlias.filter = null;
                    missingEntityAliases[aliasId] = missingEntityAlias;
                    checkNextEntityAliasOrComplete(index, aliasIds, entityAliases, missingEntityAliases, deferred);
                }
            }
        );
    }

    function editMissingAliases($event, widgets, isSingleWidget, customTitle, missingEntityAliases) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'EntityAliasesController',
            controllerAs: 'vm',
            templateUrl: entityAliasesTemplate,
            locals: {
                config: {
                    entityAliases: missingEntityAliases,
                    widgets: widgets,
                    isSingleWidget: isSingleWidget,
                    customTitle: customTitle,
                    disableAdd: true
                }
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (updatedEntityAliases) {
            deferred.resolve(updatedEntityAliases);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

    function splitCSV(str, sep) {
        for (var foo = str.split(sep = sep || ","), x = foo.length - 1, tl; x >= 0; x--) {
            if (foo[x].replace(/"\s+$/, '"').charAt(foo[x].length - 1) == '"') {
                if ((tl = foo[x].replace(/^\s+"/, '"')).length > 1 && tl.charAt(0) == '"') {
                    foo[x] = foo[x].replace(/^\s*"|"\s*$/g, '').replace(/""/g, '"');
                } else if (x) {
                    foo.splice(x - 1, 2, [foo[x - 1], foo[x]].join(sep));
                } else foo = foo.shift().split(sep).concat(foo);
            } else foo[x].replace(/""/g, '"');
        }
        return foo;
    }

    function isNumeric(str) {
        str = str.replace(',', '.');
        return !isNaN(parseFloat(str)) && isFinite(str);
    }

    function convertStringToJSType(str) {
        if (isNumeric(str.replace(',', '.'))) {
            return parseFloat(str.replace(',', '.'));
        }
        if (str.search(/^(true|false)$/im) === 0) {
            return str === 'true';
        }
        if (str === "") {
            return null;
        }
        return str;
    }

    function convertCSVToJson(csvdata, config) {
        config = config || {};
        const delim = config.delim || ",";
        const header = config.header || false;
        let result = {};

        let csvlines = csvdata.split(/[\r\n]+/);
        let csvheaders = splitCSV(csvlines[0], delim);
        if (csvheaders.length < 2) {
            toast.showError($translate.instant('import.import-csv-number-columns-error'));
            return -1;
        }
        let csvrows = header ? csvlines.slice(1, csvlines.length) : csvlines;

        result.headers = csvheaders;
        result.rows = [];

        for (let r in csvrows) {
            if (Object.prototype.hasOwnProperty.call(csvrows, r)) {
                let row = csvrows[r];

                if (row.length === 0)
                    break;

                let rowitems = splitCSV(row, delim);
                if (rowitems.length !== result.headers.length) {
                    toast.showError($translate.instant('import.import-csv-invalid-format-error', {line: (header ? result.rows.length + 2: result.rows.length + 1)}));
                    return -1;
                }
                for (let i = 0; i < rowitems.length; i++) {
                    rowitems[i] = convertStringToJSType(rowitems[i]);
                }
                result.rows.push(rowitems);
            }
        }
        return result;
    }

    function sumObject(obj1, obj2){
        Object.keys(obj2).map(function(key) {
            if (angular.isObject(obj2[key])) {
                obj1[key] = obj1[key] || {};
                angular.merge(obj1[key], sumObject(obj1[key], obj2[key]));
            } else {
                obj1[key] = (obj1[key] || 0) + obj2[key];
            }
        });
        return obj1;
    }

    function qAllWithProgress(promises) {
        promises.forEach(function(p) {
            p.then(function() {
                $rootScope.$broadcast('createImportEntityCompleted', {});
            });
        });
        return $q.all(promises);
    }

    function createMultiEntity(arrayData, entityType, updateData, config) {
        let partSize = 100;
        partSize = arrayData.length > partSize ? partSize : arrayData.length;
        let allPromise = [];
        let statisticalInfo = {};
        let deferred = $q.defer();

        for(let i = 0; i < partSize; i++){
            const promise = entityService.saveEntityParameters(entityType, arrayData[i], updateData, config);
            allPromise.push(promise);
        }

        qAllWithProgress(allPromise).then(function success(response) {
            for (let i = 0; i < response.length; i++){
                statisticalInfo = sumObject(statisticalInfo, response[i]);
            }
            arrayData.splice(0, partSize);
            if(arrayData.length > 0){
                deferred.resolve(createMultiEntity(arrayData, entityType, updateData, config).then(function (response) {
                    return sumObject(statisticalInfo, response);
                }));
            } else {
                deferred.resolve(statisticalInfo);
            }
        });
        return deferred.promise;
    }

    // Common functions

    function prepareExport(data) {
        var exportedData = angular.copy(data);
        if (angular.isDefined(exportedData.id)) {
            delete exportedData.id;
        }
        if (angular.isDefined(exportedData.createdTime)) {
            delete exportedData.createdTime;
        }
        if (angular.isDefined(exportedData.tenantId)) {
            delete exportedData.tenantId;
        }
        if (angular.isDefined(exportedData.customerId)) {
            delete exportedData.customerId;
        }
        return exportedData;
    }

    function exportToPc(data, filename) {
        if (!data) {
            $log.error('No data');
            return;
        }

        if (!filename) {
            filename = 'download.json';
        }

        if (angular.isObject(data)) {
            data = angular.toJson(data, 2);
        }

        var blob = new Blob([data], {type: 'text/json'});

        // FOR IE:

        if (window.navigator && window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            var e = document.createEvent('MouseEvents'),
                a = document.createElement('a');

            a.download = filename;
            a.href = window.URL.createObjectURL(blob);
            a.dataset.downloadurl = ['text/json', a.download, a.href].join(':');
            e.initEvent('click', true, false, window,
                0, 0, 0, 0, 0, false, false, false, false, 0, null);
            a.dispatchEvent(e);
        }
    }

    function openImportDialog($event, importTitle, importFileLabel) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'ImportDialogController',
            controllerAs: 'vm',
            templateUrl: importDialogTemplate,
            locals: {
                importTitle: importTitle,
                importFileLabel: importFileLabel
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (importData) {
            deferred.resolve(importData);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

    function openImportDialogCSV($event, entityType, importTitle, importFileLabel) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'ImportDialogCSVController',
            controllerAs: 'vm',
            templateUrl: importDialogCSVTemplate,
            locals: {
                importTitle: importTitle,
                importFileLabel: importFileLabel,
                entityType: entityType
            },
            parent: angular.element($document[0].body),
            onComplete: fixedDialogSize,
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function () {
            deferred.resolve();
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

    function fixedDialogSize(scope, element) {
        let dialogElement = element[0].getElementsByTagName('md-dialog');
        dialogElement[0].style.width = dialogElement[0].offsetWidth + 2 + "px";
    }

    function exportJSZip(data, filename) {
        let jsZip = new JSZip();
        for (let keyName in data) {
            let valueData = data[keyName];
            jsZip.file(keyName, valueData);
        }
        jsZip.generateAsync({type: "blob"}).then(function (content) {
            downloadFile(content, filename, ZIP_TYPE);
        });
    }


    function downloadFile(data, filename, fileType) {
        if (!filename) {
            filename = 'download';
        }
        filename += '.' + fileType.extension;
        var blob = new Blob([data], {type: fileType.mimeType});
        // FOR IE:
        if (window.navigator && window.navigator.msSaveOrOpenBlob) {
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            var e = document.createEvent('MouseEvents'),
                a = document.createElement('a');
            a.download = filename;
            a.href = window.URL.createObjectURL(blob);
            a.dataset.downloadurl = [fileType.mimeType, a.download, a.href].join(':');
            e.initEvent('click', true, false, window,
                0, 0, 0, 0, 0, false, false, false, false, 0, null);
            a.dispatchEvent(e);
        }
    }
}

/* eslint-enable no-undef, angular/window-service, angular/document-service */
