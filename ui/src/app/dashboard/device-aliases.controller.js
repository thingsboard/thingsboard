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
import './device-aliases.scss';

/*@ngInject*/
export default function DeviceAliasesController(deviceService, toast, $scope, $mdDialog, $document, $q, $translate,
                                                  types, config) {

    var vm = this;

    vm.isSingleDeviceAlias = config.isSingleDeviceAlias;
    vm.singleDeviceAlias = config.singleDeviceAlias;
    vm.deviceAliases = [];
    vm.title = config.customTitle ? config.customTitle : 'device.aliases';
    vm.disableAdd = config.disableAdd;
    vm.aliasToWidgetsMap = {};


    vm.onFilterDeviceChanged = onFilterDeviceChanged;
    vm.addAlias = addAlias;
    vm.removeAlias = removeAlias;

    vm.cancel = cancel;
    vm.save = save;

    initController();

    function initController() {
        var aliasId;
        if (config.widgets) {
            var widgetsTitleList, widget;
            if (config.isSingleWidget && config.widgets.length == 1) {
                widget = config.widgets[0];
                widgetsTitleList = [widget.config.title];
                for (aliasId in config.deviceAliases) {
                    vm.aliasToWidgetsMap[aliasId] = widgetsTitleList;
                }
            } else {
                for (var w in config.widgets) {
                    widget = config.widgets[w];
                    if (widget.type === types.widgetType.rpc.value) {
                        if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length > 0) {
                            var targetDeviceAliasId = widget.config.targetDeviceAliasIds[0];
                            widgetsTitleList = vm.aliasToWidgetsMap[targetDeviceAliasId];
                            if (!widgetsTitleList) {
                                widgetsTitleList = [];
                                vm.aliasToWidgetsMap[targetDeviceAliasId] = widgetsTitleList;
                            }
                            widgetsTitleList.push(widget.config.title);
                        }
                    } else {
                        for (var i in widget.config.datasources) {
                            var datasource = widget.config.datasources[i];
                            if (datasource.type === types.datasourceType.device && datasource.deviceAliasId) {
                                widgetsTitleList = vm.aliasToWidgetsMap[datasource.deviceAliasId];
                                if (!widgetsTitleList) {
                                    widgetsTitleList = [];
                                    vm.aliasToWidgetsMap[datasource.deviceAliasId] = widgetsTitleList;
                                }
                                widgetsTitleList.push(widget.config.title);
                            }
                        }
                    }
                }
            }
        }

        if (vm.isSingleDeviceAlias) {
            if (!vm.singleDeviceAlias.deviceFilter || vm.singleDeviceAlias.deviceFilter == null) {
                vm.singleDeviceAlias.deviceFilter = {
                    useFilter: false,
                    deviceNameFilter: '',
                    deviceList: [],
                };
            }
        }

        for (aliasId in config.deviceAliases) {
            var deviceAlias = config.deviceAliases[aliasId];
            var alias = deviceAlias.alias;
            var deviceFilter;
            if (!deviceAlias.deviceFilter) {
                deviceFilter = {
                    useFilter: false,
                    deviceNameFilter: '',
                    deviceList: [],
                };
                if (deviceAlias.deviceId) {
                    deviceFilter.deviceList = [deviceAlias.deviceId];
                } else {
                    deviceFilter.deviceList = [];
                }
            } else {
                deviceFilter = deviceAlias.deviceFilter;
            }
            var result = {id: aliasId, alias: alias, deviceFilter: deviceFilter, changed: true};
            vm.deviceAliases.push(result);
        }
    }

    function onFilterDeviceChanged(device, deviceAlias) {
        if (deviceAlias) {
            if (!deviceAlias.alias || deviceAlias.alias.length == 0) {
                deviceAlias.changed = false;
            }
            if (!deviceAlias.changed && device) {
                deviceAlias.alias = device.name;
            }
        }
    }

    function addAlias() {
        var aliasId = 0;
        for (var a in vm.deviceAliases) {
            aliasId = Math.max(vm.deviceAliases[a].id, aliasId);
        }
        aliasId++;
        var deviceAlias = {id: aliasId, alias: '', deviceFilter: {useFilter: false, deviceNameFilter: '', deviceList: []}, changed: false};
        vm.deviceAliases.push(deviceAlias);
    }

    function removeAlias($event, deviceAlias) {
        var index = vm.deviceAliases.indexOf(deviceAlias);
        if (index > -1) {
            var widgetsTitleList = vm.aliasToWidgetsMap[deviceAlias.id];
            if (widgetsTitleList) {
                var widgetsListHtml = '';
                for (var t in widgetsTitleList) {
                    widgetsListHtml += '<br/>\'' + widgetsTitleList[t] + '\'';
                }
                var alert = $mdDialog.alert()
                    .parent(angular.element($document[0].body))
                    .clickOutsideToClose(true)
                    .title($translate.instant('device.unable-delete-device-alias-title'))
                    .htmlContent($translate.instant('device.unable-delete-device-alias-text', {deviceAlias: deviceAlias.alias, widgetsList: widgetsListHtml}))
                    .ariaLabel($translate.instant('device.unable-delete-device-alias-title'))
                    .ok($translate.instant('action.close'))
                    .targetEvent($event);
                alert._options.skipHide = true;
                alert._options.fullscreen = true;

                $mdDialog.show(alert);
            } else {
                vm.deviceAliases.splice(index, 1);
                if ($scope.theForm) {
                    $scope.theForm.$setDirty();
                }
            }
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function cleanupDeviceFilter(deviceFilter) {
        if (deviceFilter.useFilter) {
            deviceFilter.deviceList = [];
        } else {
            deviceFilter.deviceNameFilter = '';
        }
        return deviceFilter;
    }

    function save() {

        var deviceAliases = {};
        var uniqueAliasList = {};

        var valid = true;
        var aliasId, maxAliasId;
        var alias;
        var i;

        if (vm.isSingleDeviceAlias) {
            maxAliasId = 0;
            vm.singleDeviceAlias.deviceFilter = cleanupDeviceFilter(vm.singleDeviceAlias.deviceFilter);
            for (i in vm.deviceAliases) {
                aliasId = vm.deviceAliases[i].id;
                alias = vm.deviceAliases[i].alias;
                if (alias === vm.singleDeviceAlias.alias) {
                    valid = false;
                    break;
                }
                maxAliasId = Math.max(aliasId, maxAliasId);
            }
            maxAliasId++;
            vm.singleDeviceAlias.id = maxAliasId;
        } else {
            for (i in vm.deviceAliases) {
                aliasId = vm.deviceAliases[i].id;
                alias = vm.deviceAliases[i].alias;
                if (!uniqueAliasList[alias]) {
                    uniqueAliasList[alias] = alias;
                    deviceAliases[aliasId] = {alias: alias, deviceFilter: cleanupDeviceFilter(vm.deviceAliases[i].deviceFilter)};
                } else {
                    valid = false;
                    break;
                }
            }
        }
        if (valid) {
            $scope.theForm.$setPristine();
            if (vm.isSingleDeviceAlias) {
                $mdDialog.hide(vm.singleDeviceAlias);
            } else {
                $mdDialog.hide(deviceAliases);
            }
        } else {
            toast.showError($translate.instant('device.duplicate-alias-error', {alias: alias}));
        }
    }

}
