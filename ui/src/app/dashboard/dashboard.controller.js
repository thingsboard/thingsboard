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
import dashboardBackgroundTemplate from './dashboard-settings.tpl.html';
import addWidgetTemplate from './add-widget.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardController(types, widgetService, userService,
                                            dashboardService, timeService, deviceService, itembuffer, importExport, hotkeys, $window, $rootScope,
                                            $scope, $state, $stateParams, $mdDialog, $timeout, $document, $q, $translate, $filter) {

    var vm = this;

    vm.user = userService.getCurrentUser();
    vm.dashboard = null;
    vm.editingWidget = null;
    vm.editingWidgetOriginal = null;
    vm.editingWidgetSubtitle = null;
    vm.forceDashboardMobileMode = false;
    vm.isAddingWidget = false;
    vm.isEdit = false;
    vm.isEditingWidget = false;
    vm.latestWidgetTypes = [];
    vm.timeseriesWidgetTypes = [];
    vm.rpcWidgetTypes = [];
    vm.staticWidgetTypes = [];
    vm.widgetEditMode = $state.$current.data.widgetEditMode;
    vm.iframeMode = $rootScope.iframeMode;
    vm.widgets = [];
    vm.dashboardInitComplete = false;

    vm.isToolbarOpened = false;

    vm.thingsboardVersion = THINGSBOARD_VERSION; //eslint-disable-line

    vm.currentDashboardId = $stateParams.dashboardId;
    if ($stateParams.customerId) {
        vm.currentCustomerId = $stateParams.customerId;
        vm.currentDashboardScope = 'customer';
    } else {
        vm.currentDashboardScope = vm.user.authority === 'TENANT_ADMIN' ? 'tenant' : 'customer';
        vm.currentCustomerId = vm.user.customerId;
    }

    Object.defineProperty(vm, 'toolbarOpened', {
        get: function() { return vm.isToolbarOpened || vm.isEdit; },
        set: function() { }
    });

    vm.openToolbar = function() {
        $timeout(function() {
            vm.isToolbarOpened = true;
        });
    }

    vm.closeToolbar = function() {
        $timeout(function() {
            vm.isToolbarOpened = false;
        });
    }

    vm.addWidget = addWidget;
    vm.addWidgetFromType = addWidgetFromType;
    vm.dashboardInited = dashboardInited;
    vm.dashboardInitFailed = dashboardInitFailed;
    vm.widgetMouseDown = widgetMouseDown;
    vm.widgetClicked = widgetClicked;
    vm.prepareDashboardContextMenu = prepareDashboardContextMenu;
    vm.prepareWidgetContextMenu = prepareWidgetContextMenu;
    vm.editWidget = editWidget;
    vm.exportDashboard = exportDashboard;
    vm.exportWidget = exportWidget;
    vm.importWidget = importWidget;
    vm.isPublicUser = isPublicUser;
    vm.isTenantAdmin = isTenantAdmin;
    vm.isSystemAdmin = isSystemAdmin;
    vm.loadDashboard = loadDashboard;
    vm.getServerTimeDiff = getServerTimeDiff;
    vm.noData = noData;
    vm.dashboardConfigurationError = dashboardConfigurationError;
    vm.showDashboardToolbar = showDashboardToolbar;
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
    vm.displayTitle = displayTitle;
    vm.displayExport = displayExport;
    vm.displayDashboardTimewindow = displayDashboardTimewindow;
    vm.displayDevicesSelect = displayDevicesSelect;

    vm.widgetsBundle;

    $scope.$watch('vm.widgetsBundle', function (newVal, prevVal) {
        if (newVal !== prevVal && !vm.widgetEditMode) {
            loadWidgetLibrary();
        }
    });

    $scope.$watch('vm.currentDashboardId', function (newVal, prevVal) {
        if (newVal !== prevVal && !vm.widgetEditMode) {
            if (vm.currentDashboardScope === 'customer' && vm.user.authority === 'TENANT_ADMIN') {
                $state.go('home.customers.dashboards.dashboard', {
                    customerId: vm.currentCustomerId,
                    dashboardId: vm.currentDashboardId
                });
            } else {
                $state.go('home.dashboards.dashboard', {dashboardId: vm.currentDashboardId});
            }
        }
    });


    function loadWidgetLibrary() {
        vm.latestWidgetTypes = [];
        vm.timeseriesWidgetTypes = [];
        vm.rpcWidgetTypes = [];
        vm.staticWidgetTypes = [];
        if (vm.widgetsBundle) {
            var bundleAlias = vm.widgetsBundle.alias;
            var isSystem = vm.widgetsBundle.tenantId.id === types.id.nullUid;

            widgetService.getBundleWidgetTypes(bundleAlias, isSystem).then(
                function (widgetTypes) {

                    widgetTypes = $filter('orderBy')(widgetTypes, ['-createdTime']);

                    var top = 0;

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
                        } else if (widgetTypeInfo.type === types.widgetType.static.value) {
                            vm.staticWidgetTypes.push(widget);
                        }
                        top += widget.sizeY;
                        loadNextOrComplete(i);

                    }
                }
            );
        }
    }

    function getServerTimeDiff() {
        return dashboardService.getServerTimeDiff();
    }

    function loadDashboard() {

        var deferred = $q.defer();

        if (vm.widgetEditMode) {
            $timeout(function () {
                vm.dashboardConfiguration = {
                    timewindow: timeService.defaultTimewindow()
                };
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

                    if (angular.isUndefined(vm.dashboard.configuration.timewindow)) {
                        vm.dashboard.configuration.timewindow = timeService.defaultTimewindow();
                    }
                    deviceService.processDeviceAliases(vm.dashboard.configuration.deviceAliases)
                        .then(
                            function(resolution) {
                                if (resolution.error && !isTenantAdmin()) {
                                    vm.configurationError = true;
                                    showAliasesResolutionError(resolution.error);
                                    deferred.reject();
                                } else {
                                    vm.aliasesInfo = resolution.aliasesInfo;
                                    vm.dashboardConfiguration = vm.dashboard.configuration;
                                    vm.widgets = vm.dashboard.configuration.widgets;
                                    deferred.resolve();
                                }
                            }
                        );
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
        vm.dashboardInitComplete = true;
    }

    function dashboardInited(dashboard) {
        vm.dashboardContainer = dashboard;
        initHotKeys();
        vm.dashboardInitComplete = true;
    }

    function isPublicUser() {
        return vm.user.isPublic === true;
    }

    function isTenantAdmin() {
        return vm.user.authority === 'TENANT_ADMIN';
    }

    function isSystemAdmin() {
        return vm.user.authority === 'SYS_ADMIN';
    }

    function noData() {
        return vm.dashboardInitComplete && !vm.configurationError && vm.widgets.length == 0;
    }

    function dashboardConfigurationError() {
        return vm.dashboardInitComplete && vm.configurationError;
    }

    function showDashboardToolbar() {
        return vm.dashboardInitComplete;
    }

    function openDeviceAliases($event) {
        $mdDialog.show({
            controller: 'DeviceAliasesController',
            controllerAs: 'vm',
            templateUrl: deviceAliasesTemplate,
            locals: {
                config: {
                    deviceAliases: angular.copy(vm.dashboard.configuration.deviceAliases),
                    widgets: vm.widgets,
                    isSingleDeviceAlias: false,
                    singleDeviceAlias: null
                }
            },
            parent: angular.element($document[0].body),
            skipHide: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (deviceAliases) {
            vm.dashboard.configuration.deviceAliases = deviceAliases;
            deviceAliasesUpdated();
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
            var prevGridSettings = vm.dashboard.configuration.gridSettings;
            var prevColumns = prevGridSettings ? prevGridSettings.columns : 24;
            var ratio = gridSettings.columns / prevColumns;
            var currentWidgets = angular.copy(vm.widgets);
            vm.widgets = [];
            vm.dashboard.configuration.gridSettings = gridSettings;
            for (var w in currentWidgets) {
                var widget = currentWidgets[w];
                widget.sizeX = Math.round(widget.sizeX * ratio);
                widget.sizeY = Math.round(widget.sizeY * ratio);
                widget.col = Math.round(widget.col * ratio);
                widget.row = Math.round(widget.row * ratio);
            }
            vm.dashboard.configuration.widgets = currentWidgets;
            vm.widgets = vm.dashboard.configuration.widgets;
        }, function () {
        });
    }

    function editWidget($event, widget) {
        $event.stopPropagation();
        if (vm.editingWidgetOriginal === widget) {
            $timeout(onEditWidgetClosed());
        } else {
            var transition = !vm.forceDashboardMobileMode;
            vm.editingWidgetOriginal = widget;
            vm.editingWidget = angular.copy(vm.editingWidgetOriginal);
            vm.editingWidgetSubtitle = widgetService.getInstantWidgetInfo(vm.editingWidget).widgetName;
            vm.forceDashboardMobileMode = true;
            vm.isEditingWidget = true;

            if (vm.dashboardContainer) {
                var delayOffset = transition ? 350 : 0;
                var delay = transition ? 400 : 300;
                $timeout(function () {
                    vm.dashboardContainer.highlightWidget(vm.editingWidgetOriginal, delay);
                }, delayOffset, false);
            }
        }
    }
    function exportDashboard($event) {
        $event.stopPropagation();
        importExport.exportDashboard(vm.currentDashboardId);
    }

    function exportWidget($event, widget) {
        $event.stopPropagation();
        importExport.exportWidget(vm.dashboard, widget);
    }

    function importWidget($event) {
        $event.stopPropagation();
        importExport.importWidget($event, vm.dashboard, deviceAliasesUpdated);
    }

    function widgetMouseDown($event, widget) {
        if (vm.isEdit && !vm.isEditingWidget) {
            vm.dashboardContainer.selectWidget(widget, 0);
        }
    }

    function widgetClicked($event, widget) {
        if (vm.isEditingWidget) {
            editWidget($event, widget);
        }
    }

    function isHotKeyAllowed(event) {
        var target = event.target || event.srcElement;
        var scope = angular.element(target).scope();
        return scope && scope.$parent !== $rootScope;
    }

    function initHotKeys() {
        $translate(['action.copy', 'action.paste', 'action.delete']).then(function (translations) {
            hotkeys.bindTo($scope)
                .add({
                    combo: 'ctrl+c',
                    description: translations['action.copy'],
                    callback: function (event) {
                        if (isHotKeyAllowed(event) &&
                            vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
                            var widget = vm.dashboardContainer.getSelectedWidget();
                            if (widget) {
                                event.preventDefault();
                                copyWidget(event, widget);
                            }
                        }
                    }
                })
                .add({
                    combo: 'ctrl+v',
                    description: translations['action.paste'],
                    callback: function (event) {
                        if (isHotKeyAllowed(event) &&
                            vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
                            if (itembuffer.hasWidget()) {
                                event.preventDefault();
                                pasteWidget(event);
                            }
                        }
                    }
                })
                .add({
                    combo: 'ctrl+x',
                    description: translations['action.delete'],
                    callback: function (event) {
                        if (isHotKeyAllowed(event) &&
                            vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
                            var widget = vm.dashboardContainer.getSelectedWidget();
                            if (widget) {
                                event.preventDefault();
                                removeWidget(event, widget);
                            }
                        }
                    }
                });
        });
    }

    function prepareDashboardContextMenu() {
        var dashboardContextActions = [];
        if (vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
            dashboardContextActions.push(
                {
                    action: openDashboardSettings,
                    enabled: true,
                    value: "dashboard.settings",
                    icon: "settings"
                }
            );
            dashboardContextActions.push(
                {
                    action: openDeviceAliases,
                    enabled: true,
                    value: "device.aliases",
                    icon: "devices_other"
                }
            );
            dashboardContextActions.push(
                {
                    action: pasteWidget,
                    enabled: itembuffer.hasWidget(),
                    value: "action.paste",
                    icon: "content_paste",
                    shortcut: "M-V"
                }
            );
        }
        return dashboardContextActions;
    }

    function pasteWidget($event) {
        var pos = vm.dashboardContainer.getEventGridPosition($event);
        itembuffer.pasteWidget(vm.dashboard, pos, deviceAliasesUpdated);
    }

    function prepareWidgetContextMenu() {
        var widgetContextActions = [];
        if (vm.isEdit && !vm.isEditingWidget) {
            widgetContextActions.push(
                {
                    action: editWidget,
                    enabled: true,
                    value: "action.edit",
                    icon: "edit"
                }
            );
            if (!vm.widgetEditMode) {
                widgetContextActions.push(
                    {
                        action: copyWidget,
                        enabled: true,
                        value: "action.copy",
                        icon: "content_copy",
                        shortcut: "M-C"
                    }
                );
                widgetContextActions.push(
                    {
                        action: removeWidget,
                        enabled: true,
                        value: "action.delete",
                        icon: "clear",
                        shortcut: "M-X"
                    }
                );
            }
        }
        return widgetContextActions;
    }

    function copyWidget($event, widget) {
        itembuffer.copyWidget(vm.dashboard, widget);
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
                case types.widgetType.static.value: {
                    link = 'widgetsConfigStatic';
                    break;
                }
            }
        }
        return link;
    }

    function displayTitle() {
        if (vm.dashboard && vm.dashboard.configuration.gridSettings &&
            angular.isDefined(vm.dashboard.configuration.gridSettings.showTitle)) {
            return vm.dashboard.configuration.gridSettings.showTitle;
        } else {
            return true;
        }
    }

    function displayExport() {
        if (vm.dashboard && vm.dashboard.configuration.gridSettings &&
            angular.isDefined(vm.dashboard.configuration.gridSettings.showDashboardExport)) {
            return vm.dashboard.configuration.gridSettings.showDashboardExport;
        } else {
            return true;
        }
    }

    function displayDashboardTimewindow() {
        if (vm.dashboard && vm.dashboard.configuration.gridSettings &&
            angular.isDefined(vm.dashboard.configuration.gridSettings.showDashboardTimewindow)) {
            return vm.dashboard.configuration.gridSettings.showDashboardTimewindow;
        } else {
            return true;
        }
    }

    function displayDevicesSelect() {
        if (vm.dashboard && vm.dashboard.configuration.gridSettings &&
            angular.isDefined(vm.dashboard.configuration.gridSettings.showDevicesSelect)) {
            return vm.dashboard.configuration.gridSettings.showDevicesSelect;
        } else {
            return true;
        }
    }

    function onRevertWidgetEdit(widgetForm) {
        if (widgetForm.$dirty) {
            widgetForm.$setPristine();
            vm.editingWidget = angular.copy(vm.editingWidgetOriginal);
        }
    }

    function saveWidget(widgetForm) {
        widgetForm.$setPristine();
        var widget = angular.copy(vm.editingWidget);
        var index = vm.widgets.indexOf(vm.editingWidgetOriginal);
        vm.widgets[index] = widget;
        vm.editingWidgetOriginal = widget;
        vm.dashboardContainer.highlightWidget(vm.editingWidgetOriginal, 0);
    }

    function onEditWidgetClosed() {
        vm.editingWidgetOriginal = null;
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
        vm.staticWidgetTypes = [];
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

                function addWidget(widget) {
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
                }

                if (widgetTypeInfo.useCustomDatasources) {
                    addWidget(newWidget);
                } else {
                    $mdDialog.show({
                        controller: 'AddWidgetController',
                        controllerAs: 'vm',
                        templateUrl: addWidgetTemplate,
                        locals: {
                            dashboard: vm.dashboard,
                            aliasesInfo: vm.aliasesInfo,
                            widget: newWidget,
                            widgetInfo: widgetTypeInfo
                        },
                        parent: angular.element($document[0].body),
                        fullscreen: true,
                        skipHide: true,
                        targetEvent: event,
                        onComplete: function () {
                            var w = angular.element($window);
                            w.triggerHandler('resize');
                        }
                    }).then(function (result) {
                        var widget = result.widget;
                        vm.aliasesInfo = result.aliasesInfo;
                        addWidget(widget);
                    }, function (rejection) {
                        vm.aliasesInfo = rejection.aliasesInfo;
                    });
                }
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

    function setEditMode(isEdit, revert) {
        vm.isEdit = isEdit;
        if (vm.isEdit) {
            if (vm.widgetEditMode) {
                vm.prevWidgets = angular.copy(vm.widgets);
            } else {
                vm.prevDashboard = angular.copy(vm.dashboard);
            }
        } else {
            if (vm.widgetEditMode) {
                if (revert) {
                    vm.widgets = vm.prevWidgets;
                }
            } else {
                if (vm.dashboardContainer) {
                    vm.dashboardContainer.resetHighlight();
                }
                if (revert) {
                    vm.dashboard = vm.prevDashboard;
                    vm.widgets = vm.dashboard.configuration.widgets;
                    vm.dashboardConfiguration = vm.dashboard.configuration;
                    deviceAliasesUpdated();
                }
            }
        }
    }

    function toggleDashboardEditMode() {
        setEditMode(!vm.isEdit, true);
    }

    function saveDashboard() {
        setEditMode(false, false);
        notifyDashboardUpdated();
    }

    function showAliasesResolutionError(error) {
        var alert = $mdDialog.alert()
            .parent(angular.element($document[0].body))
            .clickOutsideToClose(true)
            .title($translate.instant('dashboard.alias-resolution-error-title'))
            .htmlContent($translate.instant(error))
            .ariaLabel($translate.instant('dashboard.alias-resolution-error-title'))
            .ok($translate.instant('action.close'))
        alert._options.skipHide = true;
        alert._options.fullscreen = true;

        $mdDialog.show(alert);
    }

    function deviceAliasesUpdated() {
        deviceService.processDeviceAliases(vm.dashboard.configuration.deviceAliases)
            .then(
                function(resolution) {
                    if (resolution.aliasesInfo) {
                        vm.aliasesInfo = resolution.aliasesInfo;
                    }
                }
            );
    }

    function notifyDashboardUpdated() {
        if (vm.widgetEditMode) {
            var parentScope = $window.parent.angular.element($window.frameElement).scope();
            var widget = vm.widgets[0];
            parentScope.$root.$broadcast('widgetEditUpdated', widget);
            parentScope.$root.$apply();
        } else {
            dashboardService.saveDashboard(vm.dashboard);
        }
    }

}
