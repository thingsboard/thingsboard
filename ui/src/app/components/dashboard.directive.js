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
import './dashboard.scss';

import $ from 'jquery';
import angularGridster from 'angular-gridster';
import thingsboardTypes from '../common/types.constant';
import thingsboardApiWidget from '../api/widget.service';
import thingsboardWidget from './widget.directive';
import thingsboardToast from '../services/toast';
import thingsboardTimewindow from './timewindow.directive';
import thingsboardEvents from './tb-event-directives';
import thingsboardMousepointMenu from './mousepoint-menu.directive';

/* eslint-disable import/no-unresolved, import/default */

import dashboardTemplate from './dashboard.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

export default angular.module('thingsboard.directives.dashboard', [thingsboardTypes,
    thingsboardToast,
    thingsboardApiWidget,
    thingsboardWidget,
    thingsboardTimewindow,
    thingsboardEvents,
    thingsboardMousepointMenu,
    angularGridster.name])
    .directive('tbDashboard', Dashboard)
    .name;

/*@ngInject*/
function Dashboard() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            widgets: '=',
            aliasesInfo: '=',
            dashboardTimewindow: '=?',
            columns: '=',
            margins: '=',
            isEdit: '=',
            isMobile: '=',
            isMobileDisabled: '=?',
            isEditActionEnabled: '=',
            isExportActionEnabled: '=',
            isRemoveActionEnabled: '=',
            onEditWidget: '&?',
            onExportWidget: '&?',
            onRemoveWidget: '&?',
            onWidgetMouseDown: '&?',
            onWidgetClicked: '&?',
            prepareDashboardContextMenu: '&?',
            prepareWidgetContextMenu: '&?',
            loadWidgets: '&?',
            getStDiff: '&?',
            onInit: '&?',
            onInitFailed: '&?',
            dashboardStyle: '=?',
            dashboardClass: '=?'
        },
        controller: DashboardController,
        controllerAs: 'vm',
        templateUrl: dashboardTemplate
    };
}

/*@ngInject*/
function DashboardController($scope, $rootScope, $element, $timeout, $mdMedia, timeService, types) {

    var highlightedMode = false;
    var highlightedWidget = null;
    var selectedWidget = null;

    var gridsterParent = $('#gridster-parent', $element);
    var gridsterElement = angular.element($('#gridster-child', gridsterParent));

    var vm = this;

    vm.gridster = null;

    vm.stDiff = 0;

    vm.isMobileDisabled = angular.isDefined(vm.isMobileDisabled) ? vm.isMobileDisabled : false;

    if (!('dashboardTimewindow' in vm)) {
        vm.dashboardTimewindow = timeService.defaultTimewindow();
    }

    vm.dashboardLoading = true;
    vm.visibleRect = {
        top: 0,
        bottom: 0,
        left: 0,
        right: 0
    };
    vm.gridsterOpts = {
        pushing: false,
        floating: false,
        swapping: false,
        maxRows: 100,
        columns: vm.columns ? vm.columns : 24,
        margins: vm.margins ? vm.margins : [10, 10],
        minSizeX: 2,
        minSizeY: 2,
        defaultSizeX: 8,
        defaultSizeY: 6,
        resizable: {
            enabled: vm.isEdit
        },
        draggable: {
            enabled: vm.isEdit
        },
        saveGridItemCalculatedHeightInMobile: true
    };

    updateMobileOpts();

    vm.widgetItemMap = {
        sizeX: 'vm.widgetSizeX(widget)',
        sizeY: 'vm.widgetSizeY(widget)',
        row: 'widget.row',
        col: 'widget.col',
        minSizeY: 'widget.minSizeY',
        maxSizeY: 'widget.maxSizeY'
    };

    vm.isWidgetExpanded = false;
    vm.isHighlighted = isHighlighted;
    vm.isNotHighlighted = isNotHighlighted;
    vm.selectWidget = selectWidget;
    vm.getSelectedWidget = getSelectedWidget;
    vm.highlightWidget = highlightWidget;
    vm.resetHighlight = resetHighlight;

    vm.onWidgetFullscreenChanged = onWidgetFullscreenChanged;

    vm.widgetMouseDown = widgetMouseDown;
    vm.widgetClicked = widgetClicked;

    vm.widgetSizeX = widgetSizeX;
    vm.widgetSizeY = widgetSizeY;
    vm.widgetColor = widgetColor;
    vm.widgetBackgroundColor = widgetBackgroundColor;
    vm.widgetPadding = widgetPadding;
    vm.showWidgetTitle = showWidgetTitle;
    vm.widgetTitleStyle = widgetTitleStyle;
    vm.dropWidgetShadow = dropWidgetShadow;
    vm.enableWidgetFullscreen = enableWidgetFullscreen;
    vm.hasTimewindow = hasTimewindow;
    vm.editWidget = editWidget;
    vm.exportWidget = exportWidget;
    vm.removeWidget = removeWidget;
    vm.loading = loading;

    vm.openDashboardContextMenu = openDashboardContextMenu;
    vm.openWidgetContextMenu = openWidgetContextMenu;

    vm.getEventGridPosition = getEventGridPosition;

    vm.contextMenuItems = [];
    vm.contextMenuEvent = null;

    vm.widgetContextMenuItems = [];
    vm.widgetContextMenuEvent = null;

    vm.dashboardTimewindowApi = {
        onResetTimewindow: function() {
            $timeout(function() {
                if (vm.originalDashboardTimewindow) {
                    vm.dashboardTimewindow = angular.copy(vm.originalDashboardTimewindow);
                    vm.originalDashboardTimewindow = null;
                }
            }, 0);
        },
        onUpdateTimewindow: function(startTimeMs, endTimeMs) {
            if (!vm.originalDashboardTimewindow) {
                vm.originalDashboardTimewindow = angular.copy(vm.dashboardTimewindow);
            }
            $timeout(function() {
                vm.dashboardTimewindow = timeService.toHistoryTimewindow(vm.dashboardTimewindow, startTimeMs, endTimeMs);
            }, 0);
        }
    };

    //TODO: widgets visibility
    /*gridsterParent.scroll(function () {
        updateVisibleRect();
    });

    gridsterParent.resize(function () {
        updateVisibleRect();
    });*/

    function updateMobileOpts() {
        var isMobileDisabled = vm.isMobileDisabled === true;
        var isMobile = vm.isMobile === true && !isMobileDisabled;
        var mobileBreakPoint = isMobileDisabled ? 0 : (isMobile ? 20000 : 960);
        if (!isMobile && !isMobileDisabled) {
            isMobile = !$mdMedia('gt-sm');
        }
        var rowHeight = isMobile ? 70 : 'match';
        if (vm.gridsterOpts.isMobile != isMobile) {
            vm.gridsterOpts.isMobile = isMobile;
            vm.gridsterOpts.mobileModeEnabled = isMobile;
        }
        if (vm.gridsterOpts.mobileBreakPoint != mobileBreakPoint) {
            vm.gridsterOpts.mobileBreakPoint = mobileBreakPoint;
        }
        if (vm.gridsterOpts.rowHeight != rowHeight) {
            vm.gridsterOpts.rowHeight = rowHeight;
        }
    }

    $scope.$watch(function() { return $mdMedia('gt-sm'); }, function() {
        updateMobileOpts();
    });

    $scope.$watch('vm.isMobile', function () {
        updateMobileOpts();
    });

    $scope.$watch('vm.isMobileDisabled', function () {
        updateMobileOpts();
    });

    $scope.$watch('vm.columns', function () {
        var columns = vm.columns ? vm.columns : 24;
        if (vm.gridsterOpts.columns != columns) {
            vm.gridsterOpts.columns = columns;
            if (vm.gridster) {
                vm.gridster.columns = vm.columns;
                updateGridsterParams();
            }
            //TODO: widgets visibility
            //updateVisibleRect();
        }
    });

    $scope.$watch('vm.margins', function () {
        var margins = vm.margins ? vm.margins : [10, 10];
        if (!angular.equals(vm.gridsterOpts.margins, margins)) {
            vm.gridsterOpts.margins = margins;
            if (vm.gridster) {
                vm.gridster.margins = vm.margins;
                updateGridsterParams();
            }
            //TODO: widgets visibility
            //updateVisibleRect();
        }
    });

    $scope.$watch('vm.isEdit', function () {
        vm.gridsterOpts.resizable.enabled = vm.isEdit;
        vm.gridsterOpts.draggable.enabled = vm.isEdit;
        $scope.$broadcast('toggleDashboardEditMode', vm.isEdit);
    });

    $scope.$watch('vm.aliasesInfo.deviceAliases', function () {
        $scope.$broadcast('deviceAliasListChanged', vm.aliasesInfo);
    }, true);

    $scope.$on('gridster-resized', function (event, sizes, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            vm.gridster = theGridster;
            //TODO: widgets visibility
            //updateVisibleRect(false, true);
        }
    });

    $scope.$on('gridster-mobile-changed', function (event, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            vm.gridster = theGridster;
            var rowHeight = vm.gridster.isMobile ? 70 : 'match';
            if (vm.gridsterOpts.rowHeight != rowHeight) {
                vm.gridsterOpts.rowHeight = rowHeight;
                updateGridsterParams();
            }

            $scope.$broadcast('mobileModeChanged', vm.gridster.isMobile);

            //TODO: widgets visibility
            /*$timeout(function () {
                updateVisibleRect(true);
            }, 500, false);*/
        }
    });

    $scope.$on('widgetPositionChanged', function () {
        vm.widgets.sort(function (widget1, widget2) {
            var row1;
            var row2;
            if (angular.isDefined(widget1.config.mobileOrder)) {
                row1 = widget1.config.mobileOrder;
            } else {
                row1 = widget1.row;
            }
            if (angular.isDefined(widget2.config.mobileOrder)) {
                row2 = widget2.config.mobileOrder;
            } else {
                row2 = widget2.row;
            }
            var res = row1 - row2;
            if (res === 0) {
                res = widget1.col - widget2.col;
            }
            return res;
        });
    });

    loadStDiff();

    function loadStDiff() {
        if (vm.getStDiff) {
            var promise = vm.getStDiff();
            if (promise) {
                promise.then(function (stDiff) {
                    vm.stDiff = stDiff;
                    loadDashboard();
                }, function () {
                    vm.stDiff = 0;
                    loadDashboard();
                });
            } else {
                vm.stDiff = 0;
                loadDashboard();
            }
        } else {
            vm.stDiff = 0;
            loadDashboard();
        }
    }

    function loadDashboard() {
        $timeout(function () {
            if (vm.loadWidgets) {
                var promise = vm.loadWidgets();
                if (promise) {
                    promise.then(function () {
                        dashboardLoaded();
                    }, function () {
                        dashboardLoaded();
                    });
                } else {
                    dashboardLoaded();
                }
            } else {
                dashboardLoaded();
            }
        }, 0, false);
    }

    function updateGridsterParams() {
        if (vm.gridster) {
            if (vm.gridster.colWidth === 'auto') {
                vm.gridster.curColWidth = (vm.gridster.curWidth + (vm.gridster.outerMargin ? -vm.gridster.margins[1] : vm.gridster.margins[1])) / vm.gridster.columns;
            } else {
                vm.gridster.curColWidth = vm.gridster.colWidth;
            }
            vm.gridster.curRowHeight = vm.gridster.rowHeight;
            if (angular.isString(vm.gridster.rowHeight)) {
                if (vm.gridster.rowHeight === 'match') {
                    vm.gridster.curRowHeight = Math.round(vm.gridster.curColWidth);
                } else if (vm.gridster.rowHeight.indexOf('*') !== -1) {
                    vm.gridster.curRowHeight = Math.round(vm.gridster.curColWidth * vm.gridster.rowHeight.replace('*', '').replace(' ', ''));
                } else if (vm.gridster.rowHeight.indexOf('/') !== -1) {
                    vm.gridster.curRowHeight = Math.round(vm.gridster.curColWidth / vm.gridster.rowHeight.replace('/', '').replace(' ', ''));
                }
            }
        }
    }

    //TODO: widgets visibility
    /*function updateVisibleRect (force, containerResized) {
        if (vm.gridster) {
            var position = $(vm.gridster.$element).position()
            if (position) {
                var viewportWidth = gridsterParent.width();
                var viewportHeight = gridsterParent.height();
                var top = -position.top;
                var bottom = top + viewportHeight;
                var left = -position.left;
                var right = left + viewportWidth;

                var newVisibleRect = {
                    top: vm.gridster.pixelsToRows(top),
                    topPx: top,
                    bottom: vm.gridster.pixelsToRows(bottom),
                    bottomPx: bottom,
                    left: vm.gridster.pixelsToColumns(left),
                    right: vm.gridster.pixelsToColumns(right),
                    isMobile: vm.gridster.isMobile,
                    curRowHeight: vm.gridster.curRowHeight,
                    containerResized: containerResized
                };

                if (force ||
                    newVisibleRect.top != vm.visibleRect.top ||
                    newVisibleRect.topPx != vm.visibleRect.topPx ||
                    newVisibleRect.bottom != vm.visibleRect.bottom ||
                    newVisibleRect.bottomPx != vm.visibleRect.bottomPx ||
                    newVisibleRect.left != vm.visibleRect.left ||
                    newVisibleRect.right != vm.visibleRect.right ||
                    newVisibleRect.isMobile != vm.visibleRect.isMobile ||
                    newVisibleRect.curRowHeight != vm.visibleRect.curRowHeight ||
                    newVisibleRect.containerResized != vm.visibleRect.containerResized) {
                    vm.visibleRect = newVisibleRect;
                    $scope.$broadcast('visibleRectChanged', vm.visibleRect);
                }
            }
        }
    }*/

    function checkIsLocalGridsterElement (gridster) {
        return gridsterElement && gridsterElement[0] === gridster.$element[0];
    }

    function onWidgetFullscreenChanged(expanded, widget) {
        vm.isWidgetExpanded = expanded;
        $scope.$broadcast('onWidgetFullscreenChanged', vm.isWidgetExpanded, widget);
    }

    function widgetMouseDown ($event, widget) {
        if (vm.onWidgetMouseDown) {
            vm.onWidgetMouseDown({event: $event, widget: widget});
        }
    }

    function widgetClicked ($event, widget) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.onWidgetClicked) {
            vm.onWidgetClicked({event: $event, widget: widget});
        }
    }

    function openDashboardContextMenu($event, $mdOpenMousepointMenu) {
        if (vm.prepareDashboardContextMenu) {
            vm.contextMenuItems = vm.prepareDashboardContextMenu();
            if (vm.contextMenuItems && vm.contextMenuItems.length > 0) {
                vm.contextMenuEvent = $event;
                $mdOpenMousepointMenu($event);
            }
        }
    }

    function openWidgetContextMenu($event, widget, $mdOpenMousepointMenu) {
        if (vm.prepareWidgetContextMenu) {
            vm.widgetContextMenuItems = vm.prepareWidgetContextMenu({widget: widget});
            if (vm.widgetContextMenuItems && vm.widgetContextMenuItems.length > 0) {
                vm.widgetContextMenuEvent = $event;
                $mdOpenMousepointMenu($event);
            }
        }
    }

    function getEventGridPosition(event) {
        var pos = {
            row: 0,
            column: 0
        }
        if (!gridsterParent) {
            return pos;
        }
        var offset = gridsterParent.offset();
        var x = event.pageX - offset.left + gridsterParent.scrollLeft();
        var y = event.pageY - offset.top + gridsterParent.scrollTop();
        if (vm.gridster) {
            pos.row = vm.gridster.pixelsToRows(y);
            pos.column = vm.gridster.pixelsToColumns(x);
        }
        return pos;
    }

    function editWidget ($event, widget) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.isEditActionEnabled && vm.onEditWidget) {
            vm.onEditWidget({event: $event, widget: widget});
        }
    }

    function exportWidget ($event, widget) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.isExportActionEnabled && vm.onExportWidget) {
            vm.onExportWidget({event: $event, widget: widget});
        }
    }

    function removeWidget($event, widget) {
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.isRemoveActionEnabled && vm.onRemoveWidget) {
            vm.onRemoveWidget({event: $event, widget: widget});
        }
    }

    function highlightWidget(widget, delay) {
        if (!highlightedMode || highlightedWidget != widget) {
            highlightedMode = true;
            highlightedWidget = widget;
            scrollToWidget(widget, delay);
        }
    }

    function selectWidget(widget, delay) {
        if (selectedWidget != widget) {
            selectedWidget = widget;
            scrollToWidget(widget, delay);
        }
    }

    function scrollToWidget(widget, delay) {
        if (vm.gridster) {
            var item = $('.gridster-item', vm.gridster.$element)[vm.widgets.indexOf(widget)];
            if (item) {
                var height = $(item).outerHeight(true);
                var rectHeight = gridsterParent.height();
                var offset = (rectHeight - height) / 2;
                var scrollTop = item.offsetTop;
                if (offset > 0) {
                    scrollTop -= offset;
                }
                gridsterParent.animate({
                    scrollTop: scrollTop
                }, delay);
            }
        }
    }

    function getSelectedWidget() {
        return selectedWidget;
    }

    function resetHighlight() {
        highlightedMode = false;
        highlightedWidget = null;
        selectedWidget = null;
    }

    function isHighlighted(widget) {
        return (highlightedMode && highlightedWidget === widget) || (selectedWidget === widget);
    }

    function isNotHighlighted(widget) {
        return highlightedMode && highlightedWidget != widget;
    }

    function widgetSizeX(widget) {
        return widget.sizeX;
    }

    function widgetSizeY(widget) {
        if (vm.gridsterOpts.isMobile) {
            if (widget.config.mobileHeight) {
                return widget.config.mobileHeight;
            } else {
                return widget.sizeY * 24 / vm.gridsterOpts.columns;
            }
        } else {
            return widget.sizeY;
        }
    }

    function widgetColor(widget) {
        if (widget.config.color) {
            return widget.config.color;
        } else {
            return 'rgba(0, 0, 0, 0.87)';
        }
    }

    function widgetBackgroundColor(widget) {
        if (widget.config.backgroundColor) {
            return widget.config.backgroundColor;
        } else {
            return '#fff';
        }
    }

    function widgetPadding(widget) {
        if (widget.config.padding) {
            return widget.config.padding;
        } else {
            return '8px';
        }
    }

    function showWidgetTitle(widget) {
        if (angular.isDefined(widget.config.showTitle)) {
            return widget.config.showTitle;
        } else {
            return true;
        }
    }

    function widgetTitleStyle(widget) {
        if (angular.isDefined(widget.config.titleStyle)) {
            return widget.config.titleStyle;
        } else {
            return {};
        }
    }

    function dropWidgetShadow(widget) {
        if (angular.isDefined(widget.config.dropShadow)) {
            return widget.config.dropShadow;
        } else {
            return true;
        }
    }

    function enableWidgetFullscreen(widget) {
        if (angular.isDefined(widget.config.enableFullscreen)) {
            return widget.config.enableFullscreen;
        } else {
            return true;
        }
    }

    function hasTimewindow(widget) {
        if (widget.type === types.widgetType.timeseries.value) {
            return angular.isDefined(widget.config.useDashboardTimewindow) ?
                !widget.config.useDashboardTimewindow : false;
        } else {
            return false;
        }
    }

    function adoptMaxRows() {
        if (vm.widgets) {
            var maxRows = vm.gridsterOpts.maxRows;
            for (var i = 0; i < vm.widgets.length; i++) {
                var w = vm.widgets[i];
                var bottom = w.row + w.sizeY;
                maxRows = Math.max(maxRows, bottom);
            }
            vm.gridsterOpts.maxRows = Math.max(maxRows, vm.gridsterOpts.maxRows);
        }
    }

    function dashboardLoaded() {
        $timeout(function () {
            $scope.$watch('vm.dashboardTimewindow', function () {
                $scope.$broadcast('dashboardTimewindowChanged', vm.dashboardTimewindow);
            }, true);
            adoptMaxRows();
            vm.dashboardLoading = false;
            $timeout(function () {
                var gridsterScope = gridsterElement.scope();
                vm.gridster = gridsterScope.gridster;
                if (vm.onInit) {
                    vm.onInit({dashboard: vm});
                }
            }, 0, false);
        }, 0, false);
    }

    function loading() {
        return $rootScope.loading;
    }

}

/* eslint-enable angular/angularelement */
