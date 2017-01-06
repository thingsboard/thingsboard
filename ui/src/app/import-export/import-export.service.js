/*
 * Copyright © 2016 The Thingsboard Authors
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
export default function ImportExport($log, $translate, $q, $mdDialog, $document, itembuffer, deviceService, dashboardService, toast) {


    var service = {
        exportDashboard: exportDashboard,
        importDashboard: importDashboard,
        exportWidget: exportWidget,
        importWidget: importWidget
    }

    return service;

    // Widget functions

    function exportWidget(dashboard, widget) {
        var widgetItem = itembuffer.prepareWidgetItem(dashboard, widget);
        var name = widgetItem.widget.config.title;
        name = name.toLowerCase().replace(/\W/g,"_");
        exportToPc(prepareExport(widgetItem), name + '.json');
    }

    function importWidget($event, dashboard) {
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
                                deviceAliases[aliasId] = {
                                    alias: datasourceAliases[datasourceIndex].aliasName,
                                    deviceId: datasourceAliases[datasourceIndex].deviceId
                                };
                                aliasId++;
                            }
                        }
                        if (targetDeviceAliases) {
                            for (datasourceIndex in targetDeviceAliases) {
                                targetDeviceAliasesMap[aliasId] = datasourceIndex;
                                deviceAliases[aliasId] = {
                                    alias: targetDeviceAliases[datasourceIndex].aliasName,
                                    deviceId: targetDeviceAliases[datasourceIndex].deviceId
                                };
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
                                                        datasourceAliases[datasourceIndex].deviceId = deviceAlias.deviceId;
                                                    } else if (targetDeviceAliasesMap[aliasId]) {
                                                        datasourceIndex = targetDeviceAliasesMap[aliasId];
                                                        targetDeviceAliases[datasourceIndex].deviceId = deviceAlias.deviceId;
                                                    }
                                                }
                                                addImportedWidget(dashboard, widget, aliasesInfo, originalColumns);
                                            },
                                            function fail() {}
                                        );
                                    } else {
                                        addImportedWidget(dashboard, widget, aliasesInfo, originalColumns);
                                    }
                                }
                            );
                        } else {
                            addImportedWidget(dashboard, widget, aliasesInfo, originalColumns);
                        }
                    } else {
                        addImportedWidget(dashboard, widget, aliasesInfo, originalColumns);
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

    function addImportedWidget(dashboard, widget, aliasesInfo, originalColumns) {
        itembuffer.addWidgetToDashboard(dashboard, widget, aliasesInfo, originalColumns, -1, -1);
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
        if (deviceAlias.deviceId) {
            deviceService.getDevice(deviceAlias.deviceId, true).then(
                function success() {
                    checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
                },
                function fail() {
                    var missingDeviceAlias = angular.copy(deviceAlias);
                    missingDeviceAlias.deviceId = null;
                    missingDeviceAliases[aliasId] = missingDeviceAlias;
                    checkNextDeviceAliasOrComplete(index, aliasIds, deviceAliases, missingDeviceAliases, deferred);
                }
            );
        }
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
                    isSingleDevice: false,
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
