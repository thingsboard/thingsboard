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

import dashboardLayoutTemplate from './dashboard-layout.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function DashboardLayout() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            layoutCtx: '=',
            dashboardCtx: '=',
            isEdit: '=',
            isMobile: '=',
            widgetEditMode: '=',
            getStDiff: '&?'
        },
        controller: DashboardLayoutController,
        controllerAs: 'vm',
        templateUrl: dashboardLayoutTemplate
    };
}

/*@ngInject*/
function DashboardLayoutController($scope, $rootScope, $translate, $window, hotkeys, itembuffer) {

    var vm = this;

    vm.noData = noData;
    vm.addWidget = addWidget;
    vm.editWidget = editWidget;
    vm.exportWidget = exportWidget;
    vm.widgetMouseDown = widgetMouseDown;
    vm.widgetClicked = widgetClicked;
    vm.prepareDashboardContextMenu = prepareDashboardContextMenu;
    vm.prepareWidgetContextMenu = prepareWidgetContextMenu;
    vm.removeWidget = removeWidget;
    vm.pasteWidget = pasteWidget;
    vm.pasteWidgetReference = pasteWidgetReference;
    vm.dashboardInited = dashboardInited;
    vm.dashboardInitFailed = dashboardInitFailed;

    vm.reload = function() {
        if (vm.dashboardContainer) {
            vm.dashboardContainer.reload();
        }
    };

    vm.setResizing = function(resizing) {
        if (vm.dashboardContainer) {
            vm.dashboardContainer.isResizing = resizing;
        }
    }

    vm.resetHighlight = function() {
        if (vm.dashboardContainer) {
            vm.dashboardContainer.resetHighlight();
        }
    };

    vm.highlightWidget = function(widget, delay) {
        if (vm.dashboardContainer) {
            vm.dashboardContainer.highlightWidget(widget, delay);
        }
    };

    vm.selectWidget = function(widget, delay) {
        if (vm.dashboardContainer) {
            vm.dashboardContainer.selectWidget(widget, delay);
        }
    };

    vm.dashboardInitComplete = false;

    initHotKeys();

    $scope.$on('$destroy', function() {
        vm.dashboardContainer = null;
    });

    $scope.$watch('vm.layoutCtx', function () {
        if (vm.layoutCtx) {
            vm.layoutCtx.ctrl = vm;
        }
    });

    function noData() {
        return vm.dashboardInitComplete && vm.layoutCtx &&
            vm.layoutCtx.widgets && vm.layoutCtx.widgets.length == 0;
    }

    function addWidget($event) {
        if (vm.dashboardCtx.onAddWidget) {
            vm.dashboardCtx.onAddWidget($event, vm.layoutCtx);
        }
    }

    function editWidget($event, widget) {
        if (vm.dashboardCtx.onEditWidget) {
            vm.dashboardCtx.onEditWidget($event, vm.layoutCtx, widget);
        }
    }

    function exportWidget($event, widget) {
        if (vm.dashboardCtx.onExportWidget) {
            vm.dashboardCtx.onExportWidget($event, vm.layoutCtx, widget);
        }
    }

    function widgetMouseDown($event, widget) {
        if (vm.dashboardCtx.onWidgetMouseDown) {
            vm.dashboardCtx.onWidgetMouseDown($event, vm.layoutCtx, widget);
        }
    }

    function widgetClicked($event, widget) {
        if (vm.dashboardCtx.onWidgetClicked) {
            vm.dashboardCtx.onWidgetClicked($event, vm.layoutCtx, widget);
        }
    }

    function prepareDashboardContextMenu() {
        if (vm.dashboardCtx.prepareDashboardContextMenu) {
            return vm.dashboardCtx.prepareDashboardContextMenu(vm.layoutCtx);
        }
    }

    function prepareWidgetContextMenu(widget) {
        if (vm.dashboardCtx.prepareWidgetContextMenu) {
            return vm.dashboardCtx.prepareWidgetContextMenu(vm.layoutCtx, widget);
        }
    }

    function removeWidget($event, widget) {
        if (vm.dashboardCtx.onRemoveWidget) {
            vm.dashboardCtx.onRemoveWidget($event, vm.layoutCtx, widget);
        }
    }

    function dashboardInitFailed() {
        var parentScope = $window.parent.angular.element($window.frameElement).scope();
        parentScope.$emit('widgetEditModeInited');
        parentScope.$apply();
        vm.dashboardInitComplete = true;
    }

    function dashboardInited(dashboardContainer) {
        vm.dashboardContainer = dashboardContainer;
        vm.dashboardInitComplete = true;
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
                    combo: 'ctrl+r',
                    description: translations['action.copy-reference'],
                    callback: function (event) {
                        if (isHotKeyAllowed(event) &&
                            vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
                            var widget = vm.dashboardContainer.getSelectedWidget();
                            if (widget) {
                                event.preventDefault();
                                copyWidgetReference(event, widget);
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
                    combo: 'ctrl+i',
                    description: translations['action.paste-reference'],
                    callback: function (event) {
                        if (isHotKeyAllowed(event) &&
                            vm.isEdit && !vm.isEditingWidget && !vm.widgetEditMode) {
                            if (itembuffer.canPasteWidgetReference(vm.dashboardCtx.dashboard,
                                    vm.dashboardCtx.state, vm.layoutCtx.id)) {
                                event.preventDefault();
                                pasteWidgetReference(event);
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
                                vm.dashboardCtx.onRemoveWidget(event, vm.layoutCtx, widget);
                            }
                        }
                    }
                });
        });
    }

    function copyWidget($event, widget) {
        if (vm.dashboardCtx.copyWidget) {
            vm.dashboardCtx.copyWidget($event, vm.layoutCtx, widget);
        }
    }

    function copyWidgetReference($event, widget) {
        if (vm.dashboardCtx.copyWidgetReference) {
            vm.dashboardCtx.copyWidgetReference($event, vm.layoutCtx, widget);
        }
    }

    function pasteWidget($event) {
        var pos = vm.dashboardContainer.getEventGridPosition($event);
        if (vm.dashboardCtx.pasteWidget) {
            vm.dashboardCtx.pasteWidget($event, vm.layoutCtx, pos);
        }
    }

    function pasteWidgetReference($event) {
        var pos = vm.dashboardContainer.getEventGridPosition($event);
        if (vm.dashboardCtx.pasteWidgetReference) {
            vm.dashboardCtx.pasteWidgetReference($event, vm.layoutCtx, pos);
        }
    }

}
