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

import importDialogTemplate from './import-dialog.tpl.html';
import deviceAliasesTemplate from '../dashboard/device-aliases.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


/* eslint-disable no-undef, angular/window-service, angular/document-service */

/*@ngInject*/
export default function ImportExport($log, $translate, $q, $mdDialog, $document, itembuffer, types,
                                     deviceService, dashboardService, pluginService, ruleService, widgetService, toast) {


    var service = {
        exportDashboard: exportDashboard,
        importDashboard: importDashboard,
        exportWidget: exportWidget,
        importWidget: importWidget,
        exportPlugin: exportPlugin,
        importPlugin: importPlugin,
        exportRule: exportRule,
        importRule: importRule,
        exportWidgetType: exportWidgetType,
        importWidgetType: importWidgetType,
        exportWidgetsBundle: exportWidgetsBundle,
        importWidgetsBundle: importWidgetsBundle
    }

    return service;

    // Widgets bundle functions

    function exportWidgetsBundle(widgetsBundleId) {
        widgetService.getWidgetsBundle(widgetsBundleId).then(
            function success(widgetsBundle) {
                var bundleAlias = widgetsBundle.alias;
                var isSystem = widgetsBundle.tenantId.id === types.id.nullUid;
                widgetService.getBundleWidgetTypes(bundleAlias, isSystem).then(
                    function success (widgetTypes) {
                        prepareExport(widgetsBundle);
                        var widgetsBundleItem = {
                           widgetsBundle:  prepareExport(widgetsBundle),
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
                        name = name.toLowerCase().replace(/\W/g,"_");
                        exportToPc(widgetsBundleItem, name + '.json');
                    },
                    function fail (rejection) {
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
                name = name.toLowerCase().replace(/\W/g,"_");
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
            || angular.isUndefined(widgetType.descriptor))
        {
            return false;
        }
        return true;
    }

    // Rule functions

    function exportRule(ruleId) {
        ruleService.getRule(ruleId).then(
            function success(rule) {
                var name = rule.name;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareExport(rule), name + '.json');
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('rule.export-failed-error', {error: message}));
            }
        );
    }

    function importRule($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'rule.import', 'rule.rule-file').then(
            function success(rule) {
                if (!validateImportedRule(rule)) {
                    toast.showError($translate.instant('rule.invalid-rule-file-error'));
                    deferred.reject();
                } else {
                    rule.state = 'SUSPENDED';
                    ruleService.saveRule(rule).then(
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

    function validateImportedRule(rule) {
        if (angular.isUndefined(rule.name)
            || angular.isUndefined(rule.pluginToken)
            || angular.isUndefined(rule.filters)
            || angular.isUndefined(rule.action))
        {
            return false;
        }
        return true;
    }

    // Plugin functions

    function exportPlugin(pluginId) {
        pluginService.getPlugin(pluginId).then(
            function success(plugin) {
                if (!plugin.configuration || plugin.configuration === null) {
                    plugin.configuration = {};
                }
                var name = plugin.name;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareExport(plugin), name + '.json');
            },
            function fail(rejection) {
                var message = rejection;
                if (!message) {
                    message = $translate.instant('error.unknown-error');
                }
                toast.showError($translate.instant('plugin.export-failed-error', {error: message}));
            }
        );
    }

    function importPlugin($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'plugin.import', 'plugin.plugin-file').then(
            function success(plugin) {
                if (!validateImportedPlugin(plugin)) {
                    toast.showError($translate.instant('plugin.invalid-plugin-file-error'));
                    deferred.reject();
                } else {
                    plugin.state = 'SUSPENDED';
                    pluginService.savePlugin(plugin).then(
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

    function validateImportedPlugin(plugin) {
        if (angular.isUndefined(plugin.name)
            || angular.isUndefined(plugin.clazz)
            || angular.isUndefined(plugin.apiToken)
            || angular.isUndefined(plugin.configuration))
        {
            return false;
        }
        return true;
    }

    // Widget functions

    function exportWidget(dashboard, widget) {
        var widgetItem = itembuffer.prepareWidgetItem(dashboard, widget);
        var name = widgetItem.widget.config.title;
        name = name.toLowerCase().replace(/\W/g,"_");
        exportToPc(prepareExport(widgetItem), name + '.json');
    }

    function prepareDeviceAlias(aliasInfo) {
        var deviceFilter;
        if (aliasInfo.deviceId) {
            deviceFilter = {
                useFilter: false,
                deviceNameFilter: '',
                deviceList: [aliasInfo.deviceId]
            }
            delete aliasInfo.deviceId;
        } else {
            deviceFilter = aliasInfo.deviceFilter;
        }
        return {
            alias: aliasInfo.aliasName,
            deviceFilter: deviceFilter
        };
    }

    function importWidget($event, dashboard, onAliasesUpdate) {
        openImportDialog($event, 'dashboard.import-widget', 'dashboard.widget-file').then(
            function success(widgetItem) {
                if (!validateImportedWidget(widgetItem)) {
                    toast.showError($translate.instant('dashboard.invalid-widget-file-error'));
                } else {
                    var widget = widgetItem.widget;
                    var aliasesInfo = widgetItem.aliasesInfo;
                    var originalColumns = widgetItem.originalColumns;

                    var datasourceAliases = aliasesInfo.datasourceAliases;
                    var targetDeviceAliases = aliasesInfo.targetDeviceAliases;
                    if (datasourceAliases || targetDeviceAliases) {
                        var deviceAliases = {};
                        var datasourceAliasesMap = {};
                        var targetDeviceAliasesMap = {};
                        var aliasId = 1;
                        var datasourceIndex;
                        if (datasourceAliases) {
                            for (datasourceIndex in datasourceAliases) {
                                datasourceAliasesMap[aliasId] = datasourceIndex;
                                deviceAliases[aliasId] = prepareDeviceAlias(datasourceAliases[datasourceIndex]);
                                aliasId++;
                            }
                        }
                        if (targetDeviceAliases) {
                            for (datasourceIndex in targetDeviceAliases) {
                                targetDeviceAliasesMap[aliasId] = datasourceIndex;
                                deviceAliases[aliasId] = prepareDeviceAlias(targetDeviceAliases[datasourceIndex]);
                                aliasId++;
                            }
                        }

                        var aliasIds = Object.keys(deviceAliases);
                        if (aliasIds.length > 0) {
                            processDeviceAliases(deviceAliases, aliasIds).then(
                                function(missingDeviceAliases) {
                                    if (Object.keys(missingDeviceAliases).length > 0) {
                                        editMissingAliases($event, [ widget ],
                                              true, 'dashboard.widget-import-missing-aliases-title', missingDeviceAliases).then(
                                            function success(updatedDeviceAliases) {
                                                for (var aliasId in updatedDeviceAliases) {
                                                    var deviceAlias = updatedDeviceAliases[aliasId];
                                                    var datasourceIndex;
                                                    if (datasourceAliasesMap[aliasId]) {
                                                        datasourceIndex = datasourceAliasesMap[aliasId];
                                                        datasourceAliases[datasourceIndex].deviceFilter = deviceAlias.deviceFilter;
                                                    } else if (targetDeviceAliasesMap[aliasId]) {
                                                        datasourceIndex = targetDeviceAliasesMap[aliasId];
                                                        targetDeviceAliases[datasourceIndex].deviceFilter = deviceAlias.deviceFilter;
                                                    }
                                                }
                                                addImportedWidget(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns);
                                            },
                                            function fail() {}
                                        );
                                    } else {
                                        addImportedWidget(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns);
                                    }
                                }
                            );
                        } else {
                            addImportedWidget(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns);
                        }
                    } else {
                        addImportedWidget(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns);
                    }
                }
            },
            function fail() {}
        );
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

    function addImportedWidget(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns) {
        itembuffer.addWidgetToDashboard(dashboard, widget, aliasesInfo, onAliasesUpdate, originalColumns, -1, -1);
    }

    // Dashboard functions

    function exportDashboard(dashboardId) {
        dashboardService.getDashboard(dashboardId).then(
            function success(dashboard) {
                var name = dashboard.title;
                name = name.toLowerCase().replace(/\W/g,"_");
                exportToPc(prepareExport(dashboard), name + '.json');
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

    function importDashboard($event) {
        var deferred = $q.defer();
        openImportDialog($event, 'dashboard.import', 'dashboard.dashboard-file').then(
            function success(dashboard) {
                if (!validateImportedDashboard(dashboard)) {
                    toast.showError($translate.instant('dashboard.invalid-dashboard-file-error'));
                    deferred.reject();
                } else {
                    var deviceAliases = dashboard.configuration.deviceAliases;
                    if (deviceAliases) {
                        var aliasIds = Object.keys( deviceAliases );
                        if (aliasIds.length > 0) {
                            processDeviceAliases(deviceAliases, aliasIds).then(
                                function(missingDeviceAliases) {
                                    if (Object.keys( missingDeviceAliases ).length > 0) {
                                        editMissingAliases($event, dashboard.configuration.widgets,
                                                false, 'dashboard.dashboard-import-missing-aliases-title', missingDeviceAliases).then(
                                            function success(updatedDeviceAliases) {
                                                for (var aliasId in updatedDeviceAliases) {
                                                    deviceAliases[aliasId] = updatedDeviceAliases[aliasId];
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

    function processDeviceAliases(deviceAliases, aliasIds) {
        var deferred = $q.defer();
        var missingDeviceAliases = {};
        var index = -1;
        checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
        return deferred.promise;
    }

    function checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred) {
        index++;
        if (index == aliasIds.length) {
            deferred.resolve(missingDeviceAliases);
        } else {
            checkDeviceAlias(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
        }
    }

    function checkDeviceAlias(index, aliasIds, deviceAliases, missingDeviceAliases, deferred) {
        var aliasId = aliasIds[index];
        var deviceAlias = deviceAliases[aliasId];
        deviceService.checkDeviceAlias(deviceAlias).then(
            function(result) {
                if (result) {
                    checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
                } else {
                    var missingDeviceAlias = angular.copy(deviceAlias);
                    missingDeviceAlias.deviceFilter = null;
                    missingDeviceAliases[aliasId] = missingDeviceAlias;
                    checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
                }
            }
        );
    }

    function editMissingAliases($event, widgets, isSingleWidget, customTitle, missingDeviceAliases) {
        var deferred = $q.defer();
        $mdDialog.show({
            controller: 'DeviceAliasesController',
            controllerAs: 'vm',
            templateUrl: deviceAliasesTemplate,
            locals: {
                config: {
                    deviceAliases: missingDeviceAliases,
                    widgets: widgets,
                    isSingleWidget: isSingleWidget,
                    isSingleDeviceAlias: false,
                    singleDeviceAlias: null,
                    customTitle: customTitle,
                    disableAdd: true
                }
            },
            parent: angular.element($document[0].body),
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (updatedDeviceAliases) {
            deferred.resolve(updatedDeviceAliases);
        }, function () {
            deferred.reject();
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
        }
        else{
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
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (importData) {
            deferred.resolve(importData);
        }, function () {
            deferred.reject();
        });
        return deferred.promise;
    }

}

/* eslint-enable no-undef, angular/window-service, angular/document-service */
