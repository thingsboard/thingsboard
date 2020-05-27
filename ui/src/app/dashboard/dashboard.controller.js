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
/* eslint-disable import/no-unresolved, import/default */

import entityAliasesTemplate from '../entity/alias/entity-aliases.tpl.html';
import dashboardSettingsTemplate from './dashboard-settings.tpl.html';
import manageDashboardLayoutsTemplate from './layouts/manage-dashboard-layouts.tpl.html';
import manageDashboardStatesTemplate from './states/manage-dashboard-states.tpl.html';
import addWidgetTemplate from './add-widget.tpl.html';
import selectTargetLayoutTemplate from './layouts/select-target-layout.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import AliasController from '../api/alias-controller';

/*@ngInject*/
export default function DashboardController(types, utils, dashboardUtils, widgetService, userService,
                                            dashboardService, timeService, entityService, itembuffer, importExport, hotkeys, $window, $rootScope,
                                            $scope, $element, $state, $stateParams, $mdDialog, $mdMedia, $timeout, $document, $q, $translate, $filter) {

    var vm = this;

    vm.user = userService.getCurrentUser();
    vm.dashboard = null;
    vm.editingWidget = null;
    vm.editingWidgetLayout = null;
    vm.editingWidgetOriginal = null;
    vm.editingWidgetLayoutOriginal = null;
    vm.editingWidgetSubtitle = null;
    vm.forceDashboardMobileMode = false;
    vm.isAddingWidget = false;
    vm.isEdit = false;
    vm.isEditingWidget = false;
    vm.latestWidgetTypes = [];
    vm.timeseriesWidgetTypes = [];
    vm.rpcWidgetTypes = [];
    vm.alarmWidgetTypes = [];
    vm.staticWidgetTypes = [];
    vm.widgetEditMode = $state.$current.data.widgetEditMode;
    vm.iframeMode = $rootScope.iframeMode;

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
        get: function() {
            return !vm.widgetEditMode &&
                (toolbarAlwaysOpen() || vm.isToolbarOpened || vm.isEdit || vm.showRightLayoutSwitch()); },
        set: function() { }
    });

    Object.defineProperty(vm, 'rightLayoutOpened', {
        get: function() {
            return !vm.isMobile || vm.isRightLayoutOpened; },
        set: function() { }
    });

    vm.layouts = {
        main: {
            show: false,
            layoutCtx: {
                id: 'main',
                widgets: [],
                widgetLayouts: {},
                gridSettings: {},
                ignoreLoading: false
            }
        },
        right: {
            show: false,
            layoutCtx: {
                id: 'right',
                widgets: [],
                widgetLayouts: {},
                gridSettings: {},
                ignoreLoading: false
            }
        }
    };

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

    vm.showCloseToolbar = function() {
        return !vm.toolbarAlwaysOpen() && !vm.isEdit && !vm.showRightLayoutSwitch();
    }

    vm.toolbarAlwaysOpen = toolbarAlwaysOpen;

    vm.showRightLayoutSwitch = function() {
        return vm.isMobile && vm.layouts.right.show;
    }

    vm.toggleLayouts = function() {
        vm.isRightLayoutOpened = !vm.isRightLayoutOpened;
    }

    vm.openRightLayout = function() {
        vm.isRightLayoutOpened = true;
    }

    vm.isRightLayoutOpened = false;
    vm.isMobile = !$mdMedia('gt-sm');

    $scope.$watch(function() { return $mdMedia('gt-sm'); }, function(isGtSm) {
        vm.isMobile = !isGtSm;
    });

    vm.mainLayoutWidth = function() {
        if (vm.isEditingWidget && vm.editingLayoutCtx.id === 'main') {
            return '100%';
        } else {
            return vm.layouts.right.show && !vm.isMobile ? '50%' : '100%';
        }
    }

    vm.mainLayoutHeight = function() {
        if (!vm.isEditingWidget || vm.editingLayoutCtx.id === 'main') {
            return '100%';
        } else {
            return '0px';
        }
    }

    vm.rightLayoutWidth = function() {
        if (vm.isEditingWidget && vm.editingLayoutCtx.id === 'right') {
            return '100%';
        } else {
            return vm.isMobile ? '100%' : '50%';
        }
    }

    vm.rightLayoutHeight = function() {
        if (!vm.isEditingWidget || vm.editingLayoutCtx.id === 'right') {
            return '100%';
        } else {
            return '0px';
        }
    }

    vm.addWidget = addWidget;
    vm.addWidgetFromType = addWidgetFromType;
    vm.exportDashboard = exportDashboard;
    vm.importWidget = importWidget;
    vm.isPublicUser = isPublicUser;
    vm.isTenantAdmin = isTenantAdmin;
    vm.isSystemAdmin = isSystemAdmin;
    vm.dashboardConfigurationError = dashboardConfigurationError;
    vm.showDashboardToolbar = showDashboardToolbar;
    vm.onAddWidgetClosed = onAddWidgetClosed;
    vm.onEditWidgetClosed = onEditWidgetClosed;
    vm.openDashboardState = openDashboardState;
    vm.openEntityAliases = openEntityAliases;
    vm.openDashboardSettings = openDashboardSettings;
    vm.manageDashboardLayouts = manageDashboardLayouts;
    vm.manageDashboardStates = manageDashboardStates;
    vm.saveDashboard = saveDashboard;
    vm.saveWidget = saveWidget;
    vm.toggleDashboardEditMode = toggleDashboardEditMode;
    vm.onRevertWidgetEdit = onRevertWidgetEdit;
    vm.helpLinkIdForWidgetType = helpLinkIdForWidgetType;
    vm.displayTitle = displayTitle;
    vm.displayExport = displayExport;
    vm.displayDashboardTimewindow = displayDashboardTimewindow;
    vm.displayDashboardsSelect = displayDashboardsSelect;
    vm.displayEntitiesSelect = displayEntitiesSelect;
    vm.getEntityAliasesIcon = getEntityAliasesIcon;
    vm.getEntityAliasesLabel = getEntityAliasesLabel;
    vm.getEntityAliasesList = getEntityAliasesList;
    vm.getMinEntitiesToShowSelect = getMinEntitiesToShowSelect;
    vm.hideFullscreenButton = hideFullscreenButton;

    vm.widgetsBundle;

    vm.dashboardCtx = {
        state: null,
        stateController: {
            openRightLayout: function() {
                vm.openRightLayout();
            }
        },
        onAddWidget: function(event, layoutCtx) {
            addWidget(event, layoutCtx);
        },
        onEditWidget: function(event, layoutCtx, widget) {
            editWidget(event, layoutCtx, widget);
        },
        onExportWidget: function(event, layoutCtx, widget) {
            exportWidget(event, layoutCtx, widget);
        },
        onWidgetMouseDown: function(event, layoutCtx, widget) {
            widgetMouseDown(event, layoutCtx, widget);
        },
        onWidgetClicked: function(event, layoutCtx, widget) {
            widgetClicked(event, layoutCtx, widget);
        },
        prepareDashboardContextMenu: function(layoutCtx) {
            return prepareDashboardContextMenu(layoutCtx);
        },
        prepareWidgetContextMenu: function(layoutCtx, widget) {
            return prepareWidgetContextMenu(layoutCtx, widget);
        },
        onRemoveWidget: function(event, layoutCtx, widget) {
            removeWidget(event, layoutCtx, widget);
        },
        copyWidget: function($event, layoutCtx, widget) {
            copyWidget($event, layoutCtx, widget);
        },
        copyWidgetReference: function($event, layoutCtx, widget) {
            copyWidgetReference($event, layoutCtx, widget);
        },
        pasteWidget: function($event, layoutCtx, pos) {
            pasteWidget($event, layoutCtx, pos);
        },
        pasteWidgetReference: function($event, layoutCtx, pos) {
            pasteWidgetReference($event, layoutCtx, pos);
        }
    };

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
                if ($state.current.name === 'dashboard') {
                    $state.go('dashboard', {dashboardId: vm.currentDashboardId});
                } else {
                    $state.go('home.dashboards.dashboard', {dashboardId: vm.currentDashboardId});
                }
            }
        }
    });

    $scope.$on("$destroy", function () {
        vm.dashboardCtx.stateController.cleanupPreservedStates();
    });

    loadDashboard();

    function loadWidgetLibrary() {
        vm.latestWidgetTypes = [];
        vm.timeseriesWidgetTypes = [];
        vm.rpcWidgetTypes = [];
        vm.alarmWidgetTypes = [];
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
                        } else if (widgetTypeInfo.type === types.widgetType.alarm.value) {
                            vm.alarmWidgetTypes.push(widget);
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

    function loadDashboard() {
        if (vm.widgetEditMode) {
            var widget = {
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
            };
            widget.config.title = widget.config.title || $rootScope.editWidgetInfo.widgetName;

            vm.dashboard = dashboardUtils.createSingleWidgetDashboard(widget);
            vm.dashboardConfiguration = vm.dashboard.configuration;
            vm.dashboardCtx.dashboard = vm.dashboard;
            vm.dashboardCtx.dashboardTimewindow = vm.dashboardConfiguration.timewindow;
            vm.dashboardCtx.aliasController = new AliasController($scope, $q, $filter, utils,
                types, entityService, vm.dashboardCtx.stateController, vm.dashboardConfiguration.entityAliases);
            var parentScope = $window.parent.angular.element($window.frameElement).scope();
            parentScope.$root.$broadcast('widgetEditModeInited');
            parentScope.$root.$apply();
        } else {
            dashboardService.getDashboard($stateParams.dashboardId)
                .then(function success(dashboard) {
                    vm.dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                    vm.dashboardConfiguration = vm.dashboard.configuration;
                    vm.dashboardCtx.dashboard = vm.dashboard;
                    vm.dashboardCtx.dashboardTimewindow = vm.dashboardConfiguration.timewindow;
                    vm.dashboardCtx.aliasController = new AliasController($scope, $q, $filter, utils,
                        types, entityService, vm.dashboardCtx.stateController, vm.dashboardConfiguration.entityAliases);
                }, function fail() {
                    vm.configurationError = true;
                });
        }
    }

    function openDashboardState(state, openRightLayout) {
        var layoutsData = dashboardUtils.getStateLayoutsData(vm.dashboard, state);
        if (layoutsData) {
            vm.dashboardCtx.state = state;
            vm.dashboardCtx.aliasController.dashboardStateChanged();
            var layoutVisibilityChanged = false;
            for (var l in vm.layouts) {
                var layout = vm.layouts[l];
                var showLayout;
                if (layoutsData[l]) {
                    showLayout = true;
                } else {
                    showLayout = false;
                }
                if (layout.show != showLayout) {
                    layout.show = showLayout;
                    layoutVisibilityChanged = !vm.isMobile;
                }
            }
            vm.isRightLayoutOpened = openRightLayout ? true : false;
            updateLayouts(layoutVisibilityChanged);
        }

        function updateLayouts(layoutVisibilityChanged) {
            for (l in vm.layouts) {
                layout = vm.layouts[l];
                if (layoutsData[l]) {
                    var layoutInfo = layoutsData[l];
                    if (layout.layoutCtx.id === 'main') {
                        layout.layoutCtx.ctrl.setResizing(layoutVisibilityChanged);
                    }
                    updateLayout(layout, layoutInfo.widgets, layoutInfo.widgetLayouts, layoutInfo.gridSettings);
                } else {
                    updateLayout(layout, [], {}, null);
                }
            }
        }
    }

    function updateLayout(layout, widgets, widgetLayouts, gridSettings) {
        if (gridSettings) {
            layout.layoutCtx.gridSettings = gridSettings;
        }
        layout.layoutCtx.widgets = widgets;
        layout.layoutCtx.widgetLayouts = widgetLayouts;
        if (layout.show && layout.layoutCtx.ctrl) {
            layout.layoutCtx.ctrl.reload();
        }
        layout.layoutCtx.ignoreLoading = true;
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

    function dashboardConfigurationError() {
        return vm.configurationError;
    }

    function showDashboardToolbar() {
        return true;
    }

    function openEntityAliases($event) {
        $mdDialog.show({
            controller: 'EntityAliasesController',
            controllerAs: 'vm',
            templateUrl: entityAliasesTemplate,
            locals: {
                config: {
                    entityAliases: angular.copy(vm.dashboard.configuration.entityAliases),
                    widgets: dashboardUtils.getWidgetsArray(vm.dashboard),
                    isSingleEntityAlias: false,
                    singleEntityAlias: null
                }
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (entityAliases) {
            vm.dashboard.configuration.entityAliases = entityAliases;
            entityAliasesUpdated();
        }, function () {
        });
    }

    function openDashboardSettings($event) {
        var gridSettings = null;
        var stateSettings = angular.copy(vm.dashboard.configuration.states[vm.dashboardCtx.state].settings);
        var layoutKeys = dashboardUtils.isSingleLayoutDashboard(vm.dashboard);
        if (layoutKeys) {
            gridSettings = angular.copy(vm.dashboard.configuration.states[layoutKeys.state].layouts[layoutKeys.layout].gridSettings)
        }
        var entityAliasesList = [];
        var allEntityAliases = vm.dashboardCtx.aliasController.getEntityAliases();
        for (var aliasId in allEntityAliases) {
            var currentAlias = allEntityAliases[aliasId];
            if (currentAlias.filter && !currentAlias.filter.resolveMultiple) {
                entityAliasesList.push(currentAlias);
            }
        }
        $mdDialog.show({
            controller: 'DashboardSettingsController',
            controllerAs: 'vm',
            templateUrl: dashboardSettingsTemplate,
            locals: {
                settings: angular.copy(vm.dashboard.configuration.settings),
                gridSettings: gridSettings,
                stateSettings: stateSettings,
                entityAliasesList: entityAliasesList
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (data) {
            vm.dashboard.configuration.settings = data.settings;
            vm.dashboard.configuration.states[vm.dashboardCtx.state].settings = data.stateSettings;
            var gridSettings = data.gridSettings;
            if (gridSettings) {
                updateLayoutGrid(layoutKeys, gridSettings);
            }
        }, function () {
        });
    }

    function manageDashboardLayouts($event) {
        $mdDialog.show({
            controller: 'ManageDashboardLayoutsController',
            controllerAs: 'vm',
            templateUrl: manageDashboardLayoutsTemplate,
            locals: {
                layouts: angular.copy(vm.dashboard.configuration.states[vm.dashboardCtx.state].layouts)
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (layouts) {
            updateLayouts(layouts);
        }, function () {
        });
    }

    function manageDashboardStates($event) {
        var dashboardConfiguration = vm.dashboard.configuration;
        var states = angular.copy(dashboardConfiguration.states);

        $mdDialog.show({
            controller: 'ManageDashboardStatesController',
            controllerAs: 'vm',
            templateUrl: manageDashboardStatesTemplate,
            locals: {
                states: states
            },
            parent: angular.element($document[0].body),
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (states) {
            updateStates(states);
        }, function () {
        });
    }

    function updateLayoutGrid(layoutKeys, gridSettings) {
        var layout = vm.dashboard.configuration.states[layoutKeys.state].layouts[layoutKeys.layout];
        var layoutCtx = vm.layouts[layoutKeys.layout];
        layoutCtx.widgets = [];
        dashboardUtils.updateLayoutSettings(layout, gridSettings);
        var layoutsData = dashboardUtils.getStateLayoutsData(vm.dashboard, layoutKeys.state);
        layoutCtx.widgets = layoutsData[layoutKeys.layout].widgets;
    }

    function updateLayouts(layouts) {
        dashboardUtils.setLayouts(vm.dashboard, vm.dashboardCtx.state, layouts);
        openDashboardState(vm.dashboardCtx.state);
    }

    function updateStates(states) {
        vm.dashboard.configuration.states = states;
        dashboardUtils.removeUnusedWidgets(vm.dashboard);
        var targetState = vm.dashboardCtx.state;
        if (!vm.dashboard.configuration.states[targetState]) {
            targetState = dashboardUtils.getRootStateId(vm.dashboardConfiguration.states);
        }
        openDashboardState(targetState);
    }

    function editWidget($event, layoutCtx, widget) {
        $event.stopPropagation();
        if (vm.editingWidgetOriginal === widget) {
            $timeout(onEditWidgetClosed());
        } else {
            var transition = !vm.forceDashboardMobileMode;
            vm.editingWidgetOriginal = widget;
            vm.editingWidgetLayoutOriginal = layoutCtx.widgetLayouts[widget.id];
            vm.editingWidget = angular.copy(vm.editingWidgetOriginal);
            vm.editingWidgetLayout = angular.copy(vm.editingWidgetLayoutOriginal);
            vm.editingLayoutCtx = layoutCtx;
            vm.editingWidgetSubtitle = widgetService.getInstantWidgetInfo(vm.editingWidget).widgetName;
            vm.forceDashboardMobileMode = true;
            vm.isEditingWidget = true;
            if (layoutCtx) {
                var delayOffset = transition ? 350 : 0;
                var delay = transition ? 400 : 300;
                $timeout(function () {
                    layoutCtx.ctrl.highlightWidget(vm.editingWidgetOriginal, delay);
                }, delayOffset, false);
            }
        }
    }
    function exportDashboard($event) {
        $event.stopPropagation();
        importExport.exportDashboard(vm.currentDashboardId);
    }

    function exportWidget($event, layoutCtx, widget) {
        $event.stopPropagation();
        importExport.exportWidget(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, widget);
    }

    function importWidget($event) {
        $event.stopPropagation();
        importExport.importWidget($event, vm.dashboard, vm.dashboardCtx.state,
            selectTargetLayout, entityAliasesUpdated).then(
            function success(importData) {
                var widget = importData.widget;
                var layoutId = importData.layoutId;
                vm.layouts[layoutId].layoutCtx.widgets.push(widget);
            }
        );
    }

    function widgetMouseDown($event, layoutCtx, widget) {
        if (vm.isEdit && !vm.isEditingWidget) {
            layoutCtx.ctrl.selectWidget(widget, 0);
        }
    }

    function widgetClicked($event, layoutCtx, widget) {
        if (vm.isEditingWidget) {
            editWidget($event, layoutCtx, widget);
        }
    }

    function prepareDashboardContextMenu(layoutCtx) {
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
                    action: openEntityAliases,
                    enabled: true,
                    value: "entity.aliases",
                    icon: "devices_other"
                }
            );
            dashboardContextActions.push(
                {
                    action: function ($event) {
                        layoutCtx.ctrl.pasteWidget($event);
                    },
                    enabled: itembuffer.hasWidget(),
                    value: "action.paste",
                    icon: "content_paste",
                    shortcut: "M-V"
                }
            );
            dashboardContextActions.push(
                {
                    action: function ($event) {
                        layoutCtx.ctrl.pasteWidgetReference($event);
                    },
                    enabled: itembuffer.canPasteWidgetReference(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id),
                    value: "action.paste-reference",
                    icon: "content_paste",
                    shortcut: "M-I"
                }
            );

        }
        return dashboardContextActions;
    }

    function prepareWidgetContextMenu(layoutCtx) {
        var widgetContextActions = [];
        if (vm.isEdit && !vm.isEditingWidget) {
            widgetContextActions.push(
                {
                    action: function (event, widget) {
                        editWidget(event, layoutCtx, widget);
                    },
                    enabled: true,
                    value: "action.edit",
                    icon: "edit"
                }
            );
            if (!vm.widgetEditMode) {
                widgetContextActions.push(
                    {
                        action: function (event, widget) {
                            copyWidget(event, layoutCtx, widget);
                        },
                        enabled: true,
                        value: "action.copy",
                        icon: "content_copy",
                        shortcut: "M-C"
                    }
                );
                widgetContextActions.push(
                    {
                        action: function (event, widget) {
                            copyWidgetReference(event, layoutCtx, widget);
                        },
                        enabled: true,
                        value: "action.copy-reference",
                        icon: "content_copy",
                        shortcut: "M-R"
                    }
                );
                widgetContextActions.push(
                    {
                        action: function (event, widget) {
                            removeWidget(event, layoutCtx, widget);
                        },
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

    function copyWidget($event, layoutCtx, widget) {
        itembuffer.copyWidget(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, widget);
    }

    function copyWidgetReference($event, layoutCtx, widget) {
        itembuffer.copyWidgetReference(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, widget);
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

    function toolbarAlwaysOpen() {
        if (vm.dashboard && vm.dashboard.configuration.settings &&
            angular.isDefined(vm.dashboard.configuration.settings.toolbarAlwaysOpen)) {
            return vm.dashboard.configuration.settings.toolbarAlwaysOpen;
        } else {
            return true;
        }
    }

    function displayTitle() {
        if (vm.dashboard && vm.dashboard.configuration.settings &&
            angular.isDefined(vm.dashboard.configuration.settings.showTitle)) {
            return vm.dashboard.configuration.settings.showTitle;
        } else {
            return false;
        }
    }

    function displayExport() {
        if (vm.dashboard && vm.dashboard.configuration.settings &&
            angular.isDefined(vm.dashboard.configuration.settings.showDashboardExport)) {
            return vm.dashboard.configuration.settings.showDashboardExport;
        } else {
            return true;
        }
    }

    function displayDashboardTimewindow() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.showDashboardTimewindow)) {
                return stateSettings.showDashboardTimewindow;
            }
        }
        return true;
    }

    function displayDashboardsSelect() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.showDashboardsSelect)) {
                return stateSettings.showDashboardsSelect;
            }
        }
        return true;
    }

    function displayEntitiesSelect() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.showEntitiesSelect)) {
                return stateSettings.showEntitiesSelect;
            }
        }
        return true;
    }

    function getEntityAliasesIcon() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.entityAliasesIcon)) {
                return stateSettings.entityAliasesIcon;
            }
        }
        return 'devices_other';
    }

    function getEntityAliasesLabel() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.entityAliasesLabel)) {
                return stateSettings.entityAliasesLabel;
            }
        }
        return '';
    }

    function getEntityAliasesList() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.entityAliasesList)) {
                return stateSettings.entityAliasesList;
            }
        }
        return null;
    }

    function getMinEntitiesToShowSelect() {
        if (vm.dashboard && vm.dashboardCtx.state) {
            var stateSettings = vm.dashboard.configuration.states[vm.dashboardCtx.state].settings;
            if (stateSettings && angular.isDefined(stateSettings.minEntitiesToShowSelect)) {
                return stateSettings.minEntitiesToShowSelect;
            }
        }
        return 1;
    }

    function hideFullscreenButton() {
        return vm.widgetEditMode || vm.iframeMode || $rootScope.forceFullscreen || $state.current.name === 'dashboard';
    }

    function onRevertWidgetEdit(widgetForm) {
        if (widgetForm.$dirty) {
            widgetForm.$setPristine();
            vm.editingWidget = angular.copy(vm.editingWidgetOriginal);
            vm.editingWidgetLayout = angular.copy(vm.editingWidgetLayoutOriginal);
        }
    }

    function saveWidget(widgetForm) {
        widgetForm.$setPristine();
        var widget = angular.copy(vm.editingWidget);
        var widgetLayout = angular.copy(vm.editingWidgetLayout);
        var id = vm.editingWidgetOriginal.id;
        var index = vm.editingLayoutCtx.widgets.indexOf(vm.editingWidgetOriginal);
        vm.dashboardConfiguration.widgets[id] = widget;
        vm.editingWidgetOriginal = widget;
        vm.editingWidgetLayoutOriginal = widgetLayout;
        vm.editingLayoutCtx.widgets[index] = widget;
        vm.editingLayoutCtx.widgetLayouts[widget.id] = widgetLayout;
        vm.editingLayoutCtx.ctrl.highlightWidget(vm.editingWidgetOriginal, 0);
    }

    function onEditWidgetClosed() {
        vm.editingWidgetOriginal = null;
        vm.editingWidget = null;
        vm.editingWidgetLayoutOriginal = null;
        vm.editingWidgetLayout = null;
        vm.editingLayoutCtx = null;
        vm.editingWidgetSubtitle = null;
        vm.isEditingWidget = false;
        resetHighlight();
        vm.forceDashboardMobileMode = false;
    }

    function addWidget(event, layoutCtx) {
        loadWidgetLibrary();
        vm.isAddingWidget = true;
        vm.addingLayoutCtx = layoutCtx;
    }

    function onAddWidgetClosed() {
        vm.timeseriesWidgetTypes = [];
        vm.latestWidgetTypes = [];
        vm.rpcWidgetTypes = [];
        vm.alarmWidgetTypes = [];
        vm.staticWidgetTypes = [];
    }

    function selectTargetLayout($event) {
        var deferred = $q.defer();
        var layouts = vm.dashboardConfiguration.states[vm.dashboardCtx.state].layouts;
        var layoutIds = Object.keys(layouts);
        if (layoutIds.length > 1) {
            $mdDialog.show({
                controller: 'SelectTargetLayoutController',
                controllerAs: 'vm',
                templateUrl: selectTargetLayoutTemplate,
                parent: angular.element($document[0].body),
                fullscreen: true,
                multiple: true,
                targetEvent: $event
            }).then(
                function success(layoutId) {
                    deferred.resolve(layoutId);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.resolve(layoutIds[0]);
        }
        return deferred.promise;
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

                function addWidgetToLayout(widget, layoutId) {
                    dashboardUtils.addWidgetToLayout(vm.dashboard, vm.dashboardCtx.state, layoutId, widget);
                    vm.layouts[layoutId].layoutCtx.widgets.push(widget);
                }

                function addWidget(widget) {
                    if (vm.addingLayoutCtx) {
                        addWidgetToLayout(widget, vm.addingLayoutCtx.id);
                        vm.addingLayoutCtx = null;
                    } else {
                        selectTargetLayout(event).then(
                            function success(layoutId) {
                                addWidgetToLayout(widget, layoutId);
                            }
                        );
                    }
                }

                if (widgetTypeInfo.typeParameters.useCustomDatasources) {
                    addWidget(newWidget);
                } else {
                    $mdDialog.show({
                        controller: 'AddWidgetController',
                        controllerAs: 'vm',
                        templateUrl: addWidgetTemplate,
                        locals: {
                            dashboard: vm.dashboard,
                            aliasController: vm.dashboardCtx.aliasController,
                            widget: newWidget,
                            widgetInfo: widgetTypeInfo
                        },
                        parent: angular.element($document[0].body),
                        fullscreen: true,
                        multiple: true,
                        targetEvent: event,
                        onComplete: function () {
                            var w = angular.element($window);
                            w.triggerHandler('resize');
                        }
                    }).then(function (result) {
                        var widget = result.widget;
                        addWidget(widget);
                    }, function () {
                    });
                }
            }
        );
    }

    function removeWidget(event, layoutCtx, widget) {
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
            var index = layoutCtx.widgets.indexOf(widget);
            if (index > -1) {
                layoutCtx.widgets.splice(index, 1);
                dashboardUtils.removeWidgetFromLayout(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, widget.id);
            }
        });
    }

    function pasteWidget(event, layoutCtx, pos) {
        itembuffer.pasteWidget(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, pos, entityAliasesUpdated).then(
            function (widget) {
                if (widget) {
                    layoutCtx.widgets.push(widget);
                }
            }
        );
    }

    function pasteWidgetReference(event, layoutCtx, pos) {
        itembuffer.pasteWidgetReference(vm.dashboard, vm.dashboardCtx.state, layoutCtx.id, pos).then(
            function (widget) {
                if (widget) {
                    layoutCtx.widgets.push(widget);
                }
            }
        );
    }

    function setEditMode(isEdit, revert) {
        vm.isEdit = isEdit;
        if (vm.isEdit) {
            vm.dashboardCtx.stateController.preserveState();
            vm.prevDashboard = angular.copy(vm.dashboard);
        } else {
            if (vm.widgetEditMode) {
                if (revert) {
                    vm.dashboard = vm.prevDashboard;
                }
            } else {
                resetHighlight();
                if (revert) {
                    vm.dashboard = vm.prevDashboard;
                    vm.dashboardConfiguration = vm.dashboard.configuration;
                    vm.dashboardCtx.dashboardTimewindow = vm.dashboardConfiguration.timewindow;
                    entityAliasesUpdated();
                } else {
                    vm.dashboard.configuration.timewindow = vm.dashboardCtx.dashboardTimewindow;
                }
            }
        }
    }

    function resetHighlight() {
        for (var l in vm.layouts) {
            if (vm.layouts[l].layoutCtx) {
                if (vm.layouts[l].layoutCtx.ctrl) {
                    vm.layouts[l].layoutCtx.ctrl.resetHighlight();
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

/*    function showAliasesResolutionError(error) {
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
    }*/

    function entityAliasesUpdated() {
        vm.dashboardCtx.aliasController.updateEntityAliases(vm.dashboard.configuration.entityAliases);
    }

    function notifyDashboardUpdated() {
        if (vm.widgetEditMode) {
            var parentScope = $window.parent.angular.element($window.frameElement).scope();
            var widget = vm.layouts.main.layoutCtx.widgets[0];
            var layout = vm.layouts.main.layoutCtx.widgetLayouts[widget.id];
            widget.sizeX = layout.sizeX;
            widget.sizeY = layout.sizeY;
            parentScope.$root.$broadcast('widgetEditUpdated', widget);
            parentScope.$root.$apply();
        } else {
            dashboardService.saveDashboard(vm.dashboard);
        }
    }

}
