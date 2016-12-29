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
import './dashboard.scss';

import $ from 'jquery';
import gridster from 'angular-gridster';
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
    gridster.name])
    .directive('tbDashboard', Dashboard)
    .name;

/*@ngInject*/
function Dashboard() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            widgets: '=',
            deviceAliasList: '=',
            columns: '=',
            margins: '=',
            isEdit: '=',
            isMobile: '=',
            isMobileDisabled: '=?',
            isEditActionEnabled: '=',
            isRemoveActionEnabled: '=',
            onEditWidget: '&?',
            onRemoveWidget: '&?',
            onWidgetMouseDown: '&?',
            onWidgetClicked: '&?',
            prepareDashboardContextMenu: '&?',
            prepareWidgetContextMenu: '&?',
            loadWidgets: '&?',
            onInit: '&?',
            onInitFailed: '&?',
            dashboardStyle: '=?'
        },
        controller: DashboardController,
        controllerAs: 'vm',
        templateUrl: dashboardTemplate
    };
}

/*@ngInject*/
function DashboardController($scope, $rootScope, $element, $timeout, $log, toast, types) {

    var highlightedMode = false;
    var highlightedWidget = null;
    var selectedWidget = null;
    var mouseDownWidget = -1;
    var widgetMouseMoved = false;

    var gridsterParent = null;
    var gridsterElement = null;
    var gridster = null;

    var vm = this;

    vm.isMobileDisabled = angular.isDefined(vm.isMobileDisabled) ? vm.isMobileDisabled : false;

    vm.dashboardLoading = true;
    vm.visibleRect = {
        top: 0,
        bottom: 0,
        left: 0,
        right: 0
    };
    vm.gridsterOpts = {
        floating: false,
        maxRows: 100,
        columns: vm.columns ? vm.columns : 24,
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
        isMobile: vm.isMobileDisabled ? false : vm.isMobile,
        mobileBreakPoint: vm.isMobileDisabled ? 0 : (vm.isMobile ? 20000 : 960),
        margins: vm.margins ? vm.margins : [10, 10],
        saveGridItemCalculatedHeightInMobile: true
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
    vm.widgetMouseMove = widgetMouseMove;
    vm.widgetMouseUp = widgetMouseUp;

    vm.widgetColor = widgetColor;
    vm.widgetBackgroundColor = widgetBackgroundColor;
    vm.widgetPadding = widgetPadding;
    vm.showWidgetTitle = showWidgetTitle;
    vm.hasTimewindow = hasTimewindow;
    vm.editWidget = editWidget;
    vm.removeWidget = removeWidget;
    vm.loading = loading;

    vm.openDashboardContextMenu = openDashboardContextMenu;
    vm.openWidgetContextMenu = openWidgetContextMenu;

    vm.getEventGridPosition = getEventGridPosition;

    vm.contextMenuItems = [];
    vm.contextMenuEvent = null;

    vm.widgetContextMenuItems = [];
    vm.widgetContextMenuEvent = null;

    //$element[0].onmousemove=function(){
    //    widgetMouseMove();
   // }

    gridsterParent = $('#gridster-parent', $element);
    gridsterElement = angular.element($('#gridster-child', gridsterParent));

    gridsterParent.scroll(function () {
        updateVisibleRect();
    });

    gridsterParent.resize(function () {
        updateVisibleRect();
    });

    $scope.$watch('vm.isMobile', function () {
        vm.gridsterOpts.isMobile = vm.isMobileDisabled ? false : vm.isMobile;
        vm.gridsterOpts.mobileBreakPoint = vm.isMobileDisabled ? 0 : (vm.isMobile ? 20000 : 960);
    });

    $scope.$watch('vm.isMobileDisabled', function () {
        vm.gridsterOpts.isMobile = vm.isMobileDisabled ? false : vm.isMobile;
        vm.gridsterOpts.mobileBreakPoint = vm.isMobileDisabled ? 0 : (vm.isMobile ? 20000 : 960);
    });

    $scope.$watch('vm.columns', function () {
        vm.gridsterOpts.columns = vm.columns ? vm.columns : 24;
        if (gridster) {
            gridster.columns = vm.columns;
            updateGridsterParams();
        }
        updateVisibleRect();
    });

    $scope.$watch('vm.margins', function () {
        vm.gridsterOpts.margins = vm.margins ? vm.margins : [10, 10];
        if (gridster) {
            gridster.margins = vm.margins;
            updateGridsterParams();
        }
        updateVisibleRect();
    });

    $scope.$watch('vm.isEdit', function () {
        vm.gridsterOpts.resizable.enabled = vm.isEdit;
        vm.gridsterOpts.draggable.enabled = vm.isEdit;
        $scope.$broadcast('toggleDashboardEditMode', vm.isEdit);
    });

    $scope.$watch('vm.deviceAliasList', function () {
        $scope.$broadcast('deviceAliasListChanged', vm.deviceAliasList);
    }, true);

    $scope.$on('gridster-resized', function (event, sizes, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            gridster = theGridster;
            updateVisibleRect(false, true);
        }
    });

    $scope.$on('gridster-mobile-changed', function (event, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            gridster = theGridster;
            if (gridster.isMobile) {
                vm.gridsterOpts.rowHeight = 70;
            } else {
                vm.gridsterOpts.rowHeight = 'match';
            }
            $timeout(function () {
                updateVisibleRect(true);
            }, 500, false);
        }
    });

    $scope.$on('widgetPositionChanged', function () {
        vm.widgets.sort(function (widget1, widget2) {
            var res = widget1.row - widget2.row;
            if (res === 0) {
                res = widget1.col - widget2.col;
            }
            return res;
        });
    });

    loadDashboard();

    function loadDashboard() {
        resetWidgetClick();
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
        if (gridster) {
            if (gridster.colWidth === 'auto') {
                gridster.curColWidth = (gridster.curWidth + (gridster.outerMargin ? -gridster.margins[1] : gridster.margins[1])) / gridster.columns;
            } else {
                gridster.curColWidth = gridster.colWidth;
            }
            gridster.curRowHeight = gridster.rowHeight;
            if (angular.isString(gridster.rowHeight)) {
                if (gridster.rowHeight === 'match') {
                    gridster.curRowHeight = Math.round(gridster.curColWidth);
                } else if (gridster.rowHeight.indexOf('*') !== -1) {
                    gridster.curRowHeight = Math.round(gridster.curColWidth * gridster.rowHeight.replace('*', '').replace(' ', ''));
                } else if (gridster.rowHeight.indexOf('/') !== -1) {
                    gridster.curRowHeight = Math.round(gridster.curColWidth / gridster.rowHeight.replace('/', '').replace(' ', ''));
                }
            }
        }
    }

    function updateVisibleRect (force, containerResized) {
        if (gridster) {
            var position = $(gridster.$element).position()
            if (position) {
                var viewportWidth = gridsterParent.width();
                var viewportHeight = gridsterParent.height();
                var top = -position.top;
                var bottom = top + viewportHeight;
                var left = -position.left;
                var right = left + viewportWidth;

                var newVisibleRect = {
                    top: gridster.pixelsToRows(top),
                    topPx: top,
                    bottom: gridster.pixelsToRows(bottom),
                    bottomPx: bottom,
                    left: gridster.pixelsToColumns(left),
                    right: gridster.pixelsToColumns(right),
                    isMobile: gridster.isMobile,
                    curRowHeight: gridster.curRowHeight,
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
    }

    function checkIsLocalGridsterElement (gridster) {
        return gridsterElement[0] == gridster.$element[0];
    }

    function resetWidgetClick () {
        mouseDownWidget = -1;
        widgetMouseMoved = false;
    }

    function onWidgetFullscreenChanged(expanded, widget) {
        vm.isWidgetExpanded = expanded;
        $scope.$broadcast('onWidgetFullscreenChanged', vm.isWidgetExpanded, widget);
    }

    function widgetMouseDown ($event, widget) {
        mouseDownWidget = widget;
        widgetMouseMoved = false;
        if (vm.onWidgetMouseDown) {
            vm.onWidgetMouseDown({event: $event, widget: widget});
        }
    }

    function widgetMouseMove () {
        if (mouseDownWidget) {
            widgetMouseMoved = true;
        }
    }

    function widgetMouseUp ($event, widget) {
        $timeout(function () {
            if (!widgetMouseMoved && mouseDownWidget) {
                if (widget === mouseDownWidget) {
                    widgetClicked($event, widget);
                }
            }
            mouseDownWidget = null;
            widgetMouseMoved = false;
        }, 0);
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
        var offset = gridsterParent.offset();
        var x = event.pageX - offset.left + gridsterParent.scrollLeft();
        var y = event.pageY - offset.top + gridsterParent.scrollTop();
        if (gridster) {
            pos.row = gridster.pixelsToRows(y);
            pos.column = gridster.pixelsToColumns(x);
        }
        return pos;
    }

    function editWidget ($event, widget) {
        resetWidgetClick();
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.isEditActionEnabled && vm.onEditWidget) {
            vm.onEditWidget({event: $event, widget: widget});
        }
    }

    function removeWidget($event, widget) {
        resetWidgetClick();
        if ($event) {
            $event.stopPropagation();
        }
        if (vm.isRemoveActionEnabled && vm.onRemoveWidget) {
            vm.onRemoveWidget({event: $event, widget: widget});
        }
    }

    function highlightWidget(widget, delay) {
        highlightedMode = true;
        highlightedWidget = widget;
        var item = $('.gridster-item', gridster.$element)[vm.widgets.indexOf(widget)];
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

    function selectWidget(widget, delay) {
        selectedWidget = widget;
        var item = $('.gridster-item', gridster.$element)[vm.widgets.indexOf(widget)];
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

    function hasTimewindow(widget) {
        return widget.type === types.widgetType.timeseries.value;
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
        adoptMaxRows();
        vm.dashboardLoading = false;
        if (vm.onInit) {
            vm.onInit({dashboard: vm});
        }
    }

    function loading() {
        return $rootScope.loading;
    }

}

/* eslint-enable angular/angularelement */
