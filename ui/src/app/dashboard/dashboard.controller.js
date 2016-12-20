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

import deviceAliasesTemplate from './device-aliases.tpl.html';
import dashboardBackgroundTemplate from './dashboard-settings.tpl.html';
import addWidgetTemplate from './add-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardController(types, widgetService, userService,
                                            dashboardService, $window, $rootScope,
                                            $scope, $state, $stateParams, $mdDialog, $timeout, $document, $q, $translate, $filter) {

    var user = userService.getCurrentUser();

    var vm = this;

    vm.dashboard = null;
    vm.editingWidget = null;
    vm.editingWidgetIndex = null;
    vm.editingWidgetSubtitle = null;
    vm.forceDashboardMobileMode = false;
    vm.isAddingWidget = false;
    vm.isEdit = false;
    vm.isEditingWidget = false;
    vm.latestWidgetTypes = [];
    vm.timeseriesWidgetTypes = [];
    vm.rpcWidgetTypes = [];
    vm.widgetEditMode = $state.$current.data.widgetEditMode;
    vm.widgets = [];

    vm.addWidget = addWidget;
    vm.addWidgetFromType = addWidgetFromType;
    vm.dashboardInited = dashboardInited;
    vm.dashboardInitFailed = dashboardInitFailed;
    vm.widgetClicked = widgetClicked;
    vm.editWidget = editWidget;
    vm.isTenantAdmin = isTenantAdmin;
    vm.loadDashboard = loadDashboard;
    vm.noData = noData;
    vm.onAddWidgetClosed = onAddWidgetClosed;
    vm.onEditWidgetClosed = onEditWidgetClosed;
    vm.openDeviceAliases = openDeviceAliases;
    vm.openDashboardSettings = openDashboardSettings;
    vm.removeWidget = removeWidget;
    vm.saveDashboard = saveDashboard;
    vm.saveWidget = saveWidget;
    vm.toggleDashboardEditMode = toggleDashboardEditMode;
    vm.onRevertWidgetEdit = onRevertWidgetEdit;
    vm.helpLinkIdForWidgetType = helpLinkIdForWidgetType;

    vm.widgetsBundle;

    $scope.$watch('vm.widgetsBundle', function (newVal, prevVal) {
        if (newVal !== prevVal && !vm.widgetEditMode) {
            loadWidgetLibrary();
        }
    });

    function loadWidgetLibrary() {
        vm.latestWidgetTypes = [];
        vm.timeseriesWidgetTypes = [];
        vm.rpcWidgetTypes = [];
        if (vm.widgetsBundle) {
            var bundleAlias = vm.widgetsBundle.alias;
            var isSystem = vm.widgetsBundle.tenantId.id === types.id.nullUid;

            widgetService.getBundleWidgetTypes(bundleAlias, isSystem).then(
                function (widgetTypes) {

                    widgetTypes = $filter('orderBy')(widgetTypes, ['-name']);

                    var top = 0;
                    var sizeY = 0;

                    if (widgetTypes.length > 0) {
                        loadNext(0);
                    }

                    function loadNextOrComplete(i) {
                        i++;
                        if (i < widgetTypes.length) {
                            loadNext(i);
                        }
                    }

                    function loadNext(i) {
                        var widgetType = widgetTypes[i];
                        var widgetTypeInfo = widgetService.toWidgetInfo(widgetType);
                        var widget = {
                            isSystemType: isSystem,
                            bundleAlias: bundleAlias,
                            typeAlias: widgetTypeInfo.alias,
                            type: widgetTypeInfo.type,
                            title: widgetTypeInfo.widgetName,
                            sizeX: widgetTypeInfo.sizeX,
                            sizeY: widgetTypeInfo.sizeY,
                            row: top,
                            col: 0,
                            config: angular.fromJson(widgetTypeInfo.defaultConfig)
                        };
                        widget.config.title = widgetTypeInfo.widgetName;
                        if (widgetTypeInfo.type === types.widgetType.timeseries.value) {
                            vm.timeseriesWidgetTypes.push(widget);
                        } else if (widgetTypeInfo.type === types.widgetType.latest.value) {
                            vm.latestWidgetTypes.push(widget);
                        } else if (widgetTypeInfo.type === types.widgetType.rpc.value) {
                            vm.rpcWidgetTypes.push(widget);
                        }
                        top += sizeY;
                        loadNextOrComplete(i);

                    }
                }
            );
        }
    }

    function loadDashboard() {

        var deferred = $q.defer();

        if (vm.widgetEditMode) {
            $timeout(function () {
                vm.widgets = [{
                    isSystemType: true,
                    bundleAlias: 'customWidgetBundle',
                    typeAlias: 'customWidget',
                    type: $rootScope.editWidgetInfo.type,
                    title: 'My widget',
                    sizeX: $rootScope.editWidgetInfo.sizeX * 2,
                    sizeY: $rootScope.editWidgetInfo.sizeY * 2,
                    row: 2,
                    col: 4,
                    config: angular.fromJson($rootScope.editWidgetInfo.defaultConfig)
                }];
                vm.widgets[0].config.title = vm.widgets[0].config.title || $rootScope.editWidgetInfo.widgetName;
                deferred.resolve();
                var parentScope = $window.parent.angular.element($window.frameElement).scope();
                parentScope.$root.$broadcast('widgetEditModeInited');
                parentScope.$root.$apply();

                $scope.$watch('vm.widgets', function () {
                    var widget = vm.widgets[0];
                    parentScope.$root.$broadcast('widgetEditUpdated', widget);
                    parentScope.$root.$apply();
                }, true);

            });
        } else {

            dashboardService.getDashboard($stateParams.dashboardId)
                .then(function success(dashboard) {
                    vm.dashboard = dashboard;
                    if (vm.dashboard.configuration == null) {
                        vm.dashboard.configuration = {widgets: [], deviceAliases: {}};
                    }
                    if (angular.isUndefined(vm.dashboard.configuration.widgets)) {
                        vm.dashboard.configuration.widgets = [];
                    }
                    if (angular.isUndefined(vm.dashboard.configuration.deviceAliases)) {
                        vm.dashboard.configuration.deviceAliases = {};
                    }
                    vm.widgets = vm.dashboard.configuration.widgets;
                    deferred.resolve();
                }, function fail(e) {
                    deferred.reject(e);
                });

        }
        return deferred.promise;
    }

    function dashboardInitFailed() {
        var parentScope = $window.parent.angular.element($window.frameElement).scope();
        parentScope.$emit('widgetEditModeInited');
        parentScope.$apply();
    }

    function dashboardInited(dashboard) {
        vm.dashboardContainer = dashboard;
    }

    function isTenantAdmin() {
        return user.authority === 'TENANT_ADMIN';
    }

    function noData() {
        return vm.widgets.length == 0;
    }

    function openDeviceAliases($event) {
        var aliasToWidgetsMap = {};
        var widgetsTitleList;
        for (var w in vm.widgets) {
            var widget = vm.widgets[w];
            if (widget.type === types.widgetType.rpc.value) {
                if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length > 0) {
                    var targetDeviceAliasId = widget.config.targetDeviceAliasIds[0];
                    widgetsTitleList = aliasToWidgetsMap[targetDeviceAliasId];
                    if (!widgetsTitleList) {
                        widgetsTitleList = [];
                        aliasToWidgetsMap[targetDeviceAliasId] = widgetsTitleList;
                    }
                    widgetsTitleList.push(widget.config.title);
                }
            } else {
                for (var i in widget.config.datasources) {
                    var datasource = widget.config.datasources[i];
                    if (datasource.type === types.datasourceType.device && datasource.deviceAliasId) {
                        widgetsTitleList = aliasToWidgetsMap[datasource.deviceAliasId];
                        if (!widgetsTitleList) {
                            widgetsTitleList = [];
                            aliasToWidgetsMap[datasource.deviceAliasId] = widgetsTitleList;
                        }
                        widgetsTitleList.push(widget.config.title);
                    }
                }
            }
        }

        $mdDialog.show({
            controller: 'DeviceAliasesController',
            controllerAs: 'vm',
            templateUrl: deviceAliasesTemplate,
            locals: {
                deviceAliases: angular.copy(vm.dashboard.configuration.deviceAliases),
                aliasToWidgetsMap: aliasToWidgetsMap,
                isSingleDevice: false,
                singleDeviceAlias: null
            },
            parent: angular.element($document[0].body),
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (deviceAliases) {
            vm.dashboard.configuration.deviceAliases = deviceAliases;
        }, function () {
        });
    }

    function openDashboardSettings($event) {
        $mdDialog.show({
            controller: 'DashboardSettingsController',
            controllerAs: 'vm',
            templateUrl: dashboardBackgroundTemplate,
            locals: {
                gridSettings: angular.copy(vm.dashboard.configuration.gridSettings)
            },
            parent: angular.element($document[0].body),
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (gridSettings) {
            vm.dashboard.configuration.gridSettings = gridSettings;
        }, function () {
        });
    }

    function editWidget($event, widget) {
        $event.stopPropagation();
        var newEditingIndex = vm.widgets.indexOf(widget);
        if (vm.editingWidgetIndex === newEditingIndex) {
            $timeout(onEditWidgetClosed());
        } else {
            var transition = !vm.forceDashboardMobileMode;
            vm.editingWidgetIndex = vm.widgets.indexOf(widget);
            vm.editingWidget = angular.copy(widget);
            vm.editingWidgetSubtitle = widgetService.getInstantWidgetInfo(vm.editingWidget).widgetName;
            vm.forceDashboardMobileMode = true;
            vm.isEditingWidget = true;

            if (vm.dashboardContainer) {
                var delayOffset = transition ? 350 : 0;
                var delay = transition ? 400 : 300;
                $timeout(function () {
                    vm.dashboardContainer.highlightWidget(vm.editingWidgetIndex, delay);
                }, delayOffset, false);
            }
        }
    }

    function widgetClicked($event, widget) {
        if (vm.isEditingWidget) {
            editWidget($event, widget);
        }
    }

    function helpLinkIdForWidgetType() {
        var link = 'widgetsConfig';
        if (vm.editingWidget && vm.editingWidget.type) {
            switch (vm.editingWidget.type) {
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
            }
        }
        return link;
    }

    function onRevertWidgetEdit(widgetForm) {
        if (widgetForm.$dirty) {
            widgetForm.$setPristine();
            vm.editingWidget = angular.copy(vm.widgets[vm.editingWidgetIndex]);
        }
    }

    function saveWidget(widgetForm) {
        widgetForm.$setPristine();
        vm.widgets[vm.editingWidgetIndex] = angular.copy(vm.editingWidget);
    }

    function onEditWidgetClosed() {
        vm.editingWidgetIndex = null;
        vm.editingWidget = null;
        vm.editingWidgetSubtitle = null;
        vm.isEditingWidget = false;
        if (vm.dashboardContainer) {
            vm.dashboardContainer.resetHighlight();
        }
        vm.forceDashboardMobileMode = false;
    }

    function addWidget() {
        loadWidgetLibrary();
        vm.isAddingWidget = true;
    }

    function onAddWidgetClosed() {
        vm.timeseriesWidgetTypes = [];
        vm.latestWidgetTypes = [];
        vm.rpcWidgetTypes = [];
    }

    function addWidgetFromType(event, widget) {
        vm.onAddWidgetClosed();
        vm.isAddingWidget = false;
        widgetService.getWidgetInfo(widget.bundleAlias, widget.typeAlias, widget.isSystemType).then(
            function (widgetTypeInfo) {
                var config = angular.fromJson(widgetTypeInfo.defaultConfig);
                config.title = 'New ' + widgetTypeInfo.widgetName;
                config.datasources = [];
                var newWidget = {
                    isSystemType: widget.isSystemType,
                    bundleAlias: widget.bundleAlias,
                    typeAlias: widgetTypeInfo.alias,
                    type: widgetTypeInfo.type,
                    title: 'New widget',
                    sizeX: widgetTypeInfo.sizeX,
                    sizeY: widgetTypeInfo.sizeY,
                    config: config
                };
                $mdDialog.show({
                    controller: 'AddWidgetController',
                    controllerAs: 'vm',
                    templateUrl: addWidgetTemplate,
                    locals: {dashboard: vm.dashboard, widget: newWidget, widgetInfo: widgetTypeInfo},
                    parent: angular.element($document[0].body),
                    fullscreen: true,
                    skipHide: true,
                    targetEvent: event,
                    onComplete: function () {
                        var w = angular.element($window);
                        w.triggerHandler('resize');
                    }
                }).then(function (widget) {
                    var columns = 24;
                    if (vm.dashboard.configuration.gridSettings && vm.dashboard.configuration.gridSettings.columns) {
                        columns = vm.dashboard.configuration.gridSettings.columns;
                    }
                    if (columns != 24) {
                        var ratio = columns / 24;
                        widget.sizeX *= ratio;
                        widget.sizeY *= ratio;
                    }
                    vm.widgets.push(widget);
                }, function () {
                });
            }
        );
    }

    function removeWidget(event, widget) {
        var title = widget.config.title;
        if (!title || title.length === 0) {
            title = widgetService.getInstantWidgetInfo(widget).widgetName;
        }
        var confirm = $mdDialog.confirm()
            .targetEvent(event)
            .title($translate.instant('widget.remove-widget-title', {widgetTitle: title}))
            .htmlContent($translate.instant('widget.remove-widget-text'))
            .ariaLabel($translate.instant('widget.remove'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            vm.widgets.splice(vm.widgets.indexOf(widget), 1);
        });
    }

    function toggleDashboardEditMode() {
        vm.isEdit = !vm.isEdit;
        if (vm.isEdit) {
            if (vm.widgetEditMode) {
                vm.prevWidgets = angular.copy(vm.widgets);
            } else {
                vm.prevDashboard = angular.copy(vm.dashboard);
            }
        } else {
            if (vm.widgetEditMode) {
                vm.widgets = vm.prevWidgets;
            } else {
                vm.dashboard = vm.prevDashboard;
                vm.widgets = vm.dashboard.configuration.widgets;
            }
        }
    }

    function saveDashboard() {
        vm.isEdit = false;
        notifyDashboardUpdated();
    }

    function notifyDashboardUpdated() {
        if (!vm.widgetEditMode) {
            dashboardService.saveDashboard(vm.dashboard);
        }
    }

}
