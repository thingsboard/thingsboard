/*
 * Copyright © 2016-2019 The Thingsboard Authors
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

import 'javascript-detect-element-resize/detect-element-resize';
import angularGridster from 'angular-gridster';
import thingsboardTypes from '../common/types.constant';
import thingsboardApiWidget from '../api/widget.service';
import thingsboardWidget from './widget/widget.directive';
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
            widgetLayouts: '=?',
            aliasController: '=',
            stateController: '=',
            dashboardTimewindow: '=?',
            columns: '=',
            margins: '=',
            isEdit: '=',
            autofillHeight: '=',
            mobileAutofillHeight: '=?',
            mobileRowHeight: '=?',
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
            onInit: '&?',
            onInitFailed: '&?',
            dashboardStyle: '=?',
            dashboardClass: '=?',
            ignoreLoading: '=?'
        },
        controller: DashboardController,
        controllerAs: 'vm',
        templateUrl: dashboardTemplate
    };
}

/*@ngInject*/
function DashboardController($scope, $rootScope, $element, $timeout, $mdMedia, $mdUtil, $q, timeService, types, utils) {

    var highlightedMode = false;
    var highlightedWidget = null;
    var selectedWidget = null;

    var gridsterParent = angular.element('#gridster-parent', $element);
    var gridsterElement = angular.element('#gridster-child', gridsterParent);

    var vm = this;

    vm.gridster = null;

    vm.isMobileDisabled = angular.isDefined(vm.isMobileDisabled) ? vm.isMobileDisabled : false;

    vm.isMobileSize = false;

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
        minSizeX: 1,
        minSizeY: 1,
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

    vm.widgetLayoutInfo = {
    };

    vm.widgetIds = [];

    vm.widgetItemMap = {
        sizeX: 'vm.widgetLayoutInfo[widget.id].sizeX',
        sizeY: 'vm.widgetLayoutInfo[widget.id].sizeY',
        row: 'vm.widgetLayoutInfo[widget.id].row',
        col: 'vm.widgetLayoutInfo[widget.id].col',
        minSizeY: 'widget.minSizeY',
        maxSizeY: 'widget.maxSizeY'
    };

    /*vm.widgetItemMap = {
        sizeX: 'vm.widgetSizeX(widget)',
        sizeY: 'vm.widgetSizeY(widget)',
        row: 'vm.widgetRow(widget)',
        col: 'vm.widgetCol(widget)',
        minSizeY: 'widget.minSizeY',
        maxSizeY: 'widget.maxSizeY'
    };*/

    vm.isWidgetExpanded = false;
    vm.isHighlighted = isHighlighted;
    vm.isNotHighlighted = isNotHighlighted;
    vm.selectWidget = selectWidget;
    vm.getSelectedWidget = getSelectedWidget;
    vm.highlightWidget = highlightWidget;
    vm.resetHighlight = resetHighlight;

    vm.onWidgetFullscreenChanged = onWidgetFullscreenChanged;

    vm.isAutofillHeight = autofillHeight;

    vm.widgetMouseDown = widgetMouseDown;
    vm.widgetClicked = widgetClicked;

    vm.widgetSizeX = widgetSizeX;
    vm.widgetSizeY = widgetSizeY;
    vm.widgetRow = widgetRow;
    vm.widgetCol = widgetCol;
    vm.widgetStyle = widgetStyle;
    vm.showWidgetTitle = showWidgetTitle;
    vm.showWidgetTitleIcon = showWidgetTitleIcon;
    vm.hasWidgetTitleTemplate = hasWidgetTitleTemplate;
    vm.widgetTitleTemplate = widgetTitleTemplate;
    vm.showWidgetTitlePanel = showWidgetTitlePanel;
    vm.showWidgetActions = showWidgetActions;
    vm.widgetTitleStyle = widgetTitleStyle;
    vm.widgetTitle = widgetTitle;
    vm.widgetTitleIcon = widgetTitleIcon;
    vm.widgetTitleIconStyle = widgetTitleIconStyle;
    vm.customWidgetHeaderActions = customWidgetHeaderActions;
    vm.widgetActions = widgetActions;
    vm.dropWidgetShadow = dropWidgetShadow;
    vm.enableWidgetFullscreen = enableWidgetFullscreen;
    vm.hasTimewindow = hasTimewindow;
    vm.hasAggregation = hasAggregation;
    vm.editWidget = editWidget;
    vm.exportWidget = exportWidget;
    vm.removeWidget = removeWidget;
    vm.loading = loading;

    vm.openDashboardContextMenu = openDashboardContextMenu;
    vm.openWidgetContextMenu = openWidgetContextMenu;

    vm.getEventGridPosition = getEventGridPosition;
    vm.reload = reload;

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
        onUpdateTimewindow: function(startTimeMs, endTimeMs, interval) {
            if (!vm.originalDashboardTimewindow) {
                vm.originalDashboardTimewindow = angular.copy(vm.dashboardTimewindow);
            }
            $timeout(function() {
                vm.dashboardTimewindow = timeService.toHistoryTimewindow(vm.dashboardTimewindow, startTimeMs, endTimeMs, interval);
            }, 0);
        },
    };

    addResizeListener(gridsterParent[0], onGridsterParentResize); // eslint-disable-line no-undef

    $scope.$on("$destroy", function () {
        removeResizeListener(gridsterParent[0], onGridsterParentResize); // eslint-disable-line no-undef
    });

    function onGridsterParentResize() {
        if (gridsterParent.height() && autofillHeight()) {
            updateMobileOpts();
        }
    }

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

        if (vm.gridsterOpts.isMobile != isMobile) {
            vm.gridsterOpts.isMobile = isMobile;
            vm.gridsterOpts.mobileModeEnabled = isMobile;
        }
        if (vm.gridsterOpts.mobileBreakPoint != mobileBreakPoint) {
            vm.gridsterOpts.mobileBreakPoint = mobileBreakPoint;
        }
        detectRowSize(isMobile).then(
            function(rowHeight) {
                if (vm.gridsterOpts.rowHeight != rowHeight) {
                    vm.gridsterOpts.rowHeight = rowHeight;
                }
            }
        );
        vm.isMobileSize = checkIsMobileSize();
    }

    function checkIsMobileSize() {
        var isMobileDisabled = vm.isMobileDisabled === true;
        var isMobileSize = vm.isMobile === true && !isMobileDisabled;
        if (!isMobileSize && !isMobileDisabled) {
            isMobileSize = !$mdMedia('gt-sm');
        }
        return isMobileSize;
    }

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

    $scope.$watch(function() {
        return $mdMedia('gt-sm') + ',' + vm.isMobile + ',' + vm.isMobileDisabled;
    }, function() {
        updateMobileOpts();
        sortWidgets();
    });

    $scope.$watch(function() {
        return vm.autofillHeight + ',' + vm.mobileAutofillHeight + ',' + vm.mobileRowHeight;
    }, function () {
        updateMobileOpts();
    });

    $scope.$watch('vm.margins', function () {
        var margins = vm.margins ? vm.margins : [10, 10];
        if (!angular.equals(vm.gridsterOpts.margins, margins)) {
            vm.gridsterOpts.margins = margins;
            updateMobileOpts();
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

    $scope.$watch('vm.isMobileSize', function (newVal, prevVal) {
        if (!angular.equals(newVal, prevVal)) {
            $scope.$broadcast('mobileModeChanged', vm.isMobileSize);
        }
    });

    $scope.$watchCollection('vm.widgets', function () {
        var ids = [];
        for (var i=0;i<vm.widgets.length;i++) {
            var widget = vm.widgets[i];
            if (!widget.id) {
                widget.id = utils.guid();
            }
            ids.push(widget.id);
        }
        ids.sort(function (id1, id2) {
            return id1.localeCompare(id2);
        });
        if (angular.equals(ids, vm.widgetIds)) {
            return;
        }
        vm.widgetIds = ids;
        for (i=0;i<vm.widgets.length;i++) {
            widget = vm.widgets[i];
            var layoutInfoObject = vm.widgetLayoutInfo[widget.id];
            if (!layoutInfoObject) {
                layoutInfoObject = {
                    widget: widget
                };
                Object.defineProperty(layoutInfoObject, 'sizeX', {
                    get: function() { return widgetSizeX(this.widget) },
                    set: function(newSizeX) { setWidgetSizeX(this.widget, newSizeX)}
                });
                Object.defineProperty(layoutInfoObject, 'sizeY', {
                    get: function() { return widgetSizeY(this.widget) },
                    set: function(newSizeY) { setWidgetSizeY(this.widget, newSizeY)}
                });
                Object.defineProperty(layoutInfoObject, 'row', {
                    get: function() { return widgetRow(this.widget) },
                    set: function(newRow) { setWidgetRow(this.widget, newRow)}
                });
                Object.defineProperty(layoutInfoObject, 'col', {
                    get: function() { return widgetCol(this.widget) },
                    set: function(newCol) { setWidgetCol(this.widget, newCol)}
                });
                vm.widgetLayoutInfo[widget.id] = layoutInfoObject;
            }
        }
        for (var widgetId in vm.widgetLayoutInfo) {
            if (ids.indexOf(widgetId) === -1) {
                delete vm.widgetLayoutInfo[widgetId];
            }
        }
        sortWidgets();
        $mdUtil.nextTick(function () {
            if (autofillHeight()) {
                updateMobileOpts();
            }
        });
    });

    $scope.$watch('vm.widgetLayouts', function () {
        updateMobileOpts();
        sortWidgets();
    });

    $scope.$on('gridster-resized', function (event, sizes, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            vm.gridster = theGridster;
            setupGridster(vm.gridster);
            vm.isResizing = false;
            //TODO: widgets visibility
            //updateVisibleRect(false, true);
        }
    });

    $scope.$on('gridster-mobile-changed', function (event, theGridster) {
        if (checkIsLocalGridsterElement(theGridster)) {
            vm.gridster = theGridster;
            setupGridster(vm.gridster);
            detectRowSize(vm.gridster.isMobile).then(
                function(rowHeight) {
                    if (vm.gridsterOpts.rowHeight != rowHeight) {
                        vm.gridsterOpts.rowHeight = rowHeight;
                        updateGridsterParams();
                    }
                }
            );
            vm.isMobileSize = checkIsMobileSize();

            //TODO: widgets visibility
            /*$timeout(function () {
                updateVisibleRect(true);
            }, 500, false);*/
        }
    });

    function autofillHeight() {
        if (vm.gridsterOpts.isMobile) {
            return angular.isDefined(vm.mobileAutofillHeight) ? vm.mobileAutofillHeight : false;
        } else {
            return angular.isDefined(vm.autofillHeight) ? vm.autofillHeight : false;
        }
    }

    function detectViewportHeight() {
        var deferred = $q.defer();
        var viewportHeight = gridsterParent.height();
        if (viewportHeight) {
            deferred.resolve(viewportHeight);
        } else {
            $scope.viewportHeightWatch = $scope.$watch(function() { return gridsterParent.height(); },
                function(viewportHeight) {
                    if (viewportHeight) {
                        $scope.viewportHeightWatch();
                        deferred.resolve(viewportHeight);
                    }
                }
            );
        }
        return deferred.promise;
    }

    function detectRowSize(isMobile) {
        var deferred = $q.defer();
        var rowHeight;
        if (autofillHeight()) {
            detectViewportHeight().then(
                function(viewportHeight) {
                    var totalRows = 0;
                    for (var i = 0; i < vm.widgets.length; i++) {
                        var w = vm.widgets[i];
                        var sizeY = widgetSizeY(w);
                        if (isMobile) {
                            totalRows += sizeY;
                        } else {
                            var row = widgetRow(w);
                            var bottom = row + sizeY;
                            totalRows = Math.max(totalRows, bottom);
                        }
                    }
                    rowHeight = (viewportHeight - vm.gridsterOpts.margins[1]*(vm.widgets.length+1) + vm.gridsterOpts.margins[0]*vm.widgets.length) / totalRows;
                    deferred.resolve(rowHeight);
                }
            );
        } else if (isMobile) {
            rowHeight = angular.isDefined(vm.mobileRowHeight) ? vm.mobileRowHeight : 70;
            deferred.resolve(rowHeight);
        } else {
            rowHeight = 'match';
            deferred.resolve(rowHeight);
        }
        return deferred.promise;
    }

    function widgetOrder(widget) {
        var order;
        var hasLayout = vm.widgetLayouts && vm.widgetLayouts[widget.id];
        if (hasLayout && angular.isDefined(vm.widgetLayouts[widget.id].mobileOrder)
                && vm.widgetLayouts[widget.id].mobileOrder >= 0) {
            order = vm.widgetLayouts[widget.id].mobileOrder;
        } else if (angular.isDefined(widget.config.mobileOrder) && widget.config.mobileOrder >= 0) {
            order = widget.config.mobileOrder;
        } else if (hasLayout) {
            order = vm.widgetLayouts[widget.id].row;
        } else {
            order = widget.row;
        }
        return order;
    }

    $scope.$on('widgetPositionChanged', function () {
        sortWidgets();
    });

    loadDashboard();

    function sortWidgets() {
        vm.widgets.sort(function (widget1, widget2) {
            var row1 = widgetOrder(widget1);
            var row2 = widgetOrder(widget2);
            var res = row1 - row2;
            if (res === 0) {
                res = widgetCol(widget1) - widgetCol(widget2);
            }
            return res;
        });
    }

    function reload() {
        loadDashboard();
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
            var item = angular.element('.gridster-item', vm.gridster.$element)[vm.widgets.indexOf(widget)];
            if (item) {
                var height = angular.element(item).outerHeight(true);
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
        if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
            return vm.widgetLayouts[widget.id].sizeX;
        } else {
            return widget.sizeX;
        }
    }

    function setWidgetSizeX(widget, sizeX) {
        if (!vm.gridsterOpts.isMobile) {
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                vm.widgetLayouts[widget.id].sizeX = sizeX;
            } else {
                widget.sizeX = sizeX;
            }
        }
    }

    function widgetSizeY(widget) {
        if (vm.gridsterOpts.isMobile && !vm.mobileAutofillHeight) {
            var mobileHeight;
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                mobileHeight = vm.widgetLayouts[widget.id].mobileHeight;
            }
            if (!mobileHeight && widget.config.mobileHeight) {
                mobileHeight = widget.config.mobileHeight;
            }
            if (mobileHeight) {
                return mobileHeight;
            } else {
                return widget.sizeY * 24 / vm.gridsterOpts.columns;
            }
        } else {
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                return vm.widgetLayouts[widget.id].sizeY;
            } else {
                return widget.sizeY;
            }
        }
    }

    function setWidgetSizeY(widget, sizeY) {
        if (!vm.gridsterOpts.isMobile && !vm.autofillHeight) {
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                vm.widgetLayouts[widget.id].sizeY = sizeY;
            } else {
                widget.sizeY = sizeY;
            }
        }
    }

    function widgetRow(widget) {
        if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
            return vm.widgetLayouts[widget.id].row;
        } else {
            return widget.row;
        }
    }

    function setWidgetRow(widget, row) {
        if (!vm.gridsterOpts.isMobile) {
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                vm.widgetLayouts[widget.id].row = row;
            } else {
                widget.row = row;
            }
        }
    }

    function widgetCol(widget) {
        if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
            return vm.widgetLayouts[widget.id].col;
        } else {
            return widget.col;
        }
    }

    function setWidgetCol(widget, col) {
        if (!vm.gridsterOpts.isMobile) {
            if (vm.widgetLayouts && vm.widgetLayouts[widget.id]) {
                vm.widgetLayouts[widget.id].col = col;
            } else {
                widget.col = col;
            }
        }
    }

    function widgetStyle(widget) {
        var style = {cursor: 'pointer',
                     color: widgetColor(widget),
                     backgroundColor: widgetBackgroundColor(widget),
                     padding: widgetPadding(widget),
                     margin: widgetMargin(widget)};
        if (angular.isDefined(widget.config.widgetStyle)) {
            Object.assign(style, widget.config.widgetStyle);
        }
        return style;
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

    function widgetMargin(widget) {
        if (widget.config.margin) {
            return widget.config.margin;
        } else {
            return '0px';
        }
    }

    function showWidgetTitle(widget) {
        if (angular.isDefined(widget.config.showTitle)) {
            return widget.config.showTitle;
        } else {
            return true;
        }
    }

    function showWidgetTitleIcon(widget) {
        if (angular.isDefined(widget.config.showTitleIcon)) {
            return widget.config.showTitleIcon;
        } else {
            return false;
        }
    }

    function hasWidgetTitleTemplate(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.widgetTitleTemplate) {
            return true;
        } else {
            return false;
        }
    }

    function widgetTitleTemplate(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.widgetTitleTemplate) {
            return ctx.widgetTitleTemplate;
        } else {
            return '';
        }
    }

    function showWidgetTitlePanel(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.hideTitlePanel) {
            return false;
        } else {
            return hasWidgetTitleTemplate(widget) || showWidgetTitle(widget) || hasTimewindow(widget);
        }
    }

    function showWidgetActions(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.hideTitlePanel) {
            return false;
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

    function widgetTitle(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.widgetTitle
            && ctx.widgetTitle.length) {
            return ctx.widgetTitle;
        } else {
            return widget.config.title;
        }
    }

    function widgetTitleIcon(widget) {
        if (angular.isDefined(widget.config.titleIcon)) {
            return widget.config.titleIcon;
        } else {
            return '';
        }
    }

    function widgetTitleIconStyle(widget) {
        var style = {};
        if (angular.isDefined(widget.config.iconColor)) {
            style.color = widget.config.iconColor;
        }
        if (angular.isDefined(widget.config.iconSize)) {
            style.fontSize = widget.config.iconSize;
        }
        return style;
    }

    function customWidgetHeaderActions(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.customHeaderActions && ctx.customHeaderActions.length) {
            return ctx.customHeaderActions;
        } else {
            return [];
        }
    }

    function widgetActions(widget) {
        var ctx = widgetContext(widget);
        if (ctx && ctx.widgetActions && ctx.widgetActions.length) {
            return ctx.widgetActions;
        } else {
            return [];
        }
    }

    function widgetContext(widget) {
        var context;
        if (widget.$ctx) {
            context = widget.$ctx();
        }
        return context;
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
        if (widget.type === types.widgetType.timeseries.value || widget.type === types.widgetType.alarm.value) {
            return angular.isDefined(widget.config.useDashboardTimewindow) ?
                (!widget.config.useDashboardTimewindow && (angular.isUndefined(widget.config.displayTimewindow) || widget.config.displayTimewindow)) : false;
        } else {
            return false;
        }
    }

    function hasAggregation(widget) {
        return widget.type === types.widgetType.timeseries.value;
    }

    function adoptMaxRows() {
        if (vm.widgets) {
            var maxRows = vm.gridsterOpts.maxRows;
            for (var i = 0; i < vm.widgets.length; i++) {
                var w = vm.widgets[i];
                var bottom = widgetRow(w) + widgetSizeY(w);
                maxRows = Math.max(maxRows, bottom);
            }
            vm.gridsterOpts.maxRows = Math.max(maxRows, vm.gridsterOpts.maxRows);
        }
    }

    function dashboardLoaded() {
        $mdUtil.nextTick(function () {
            if (vm.dashboardTimewindowWatch) {
                vm.dashboardTimewindowWatch();
                vm.dashboardTimewindowWatch = null;
            }
            vm.dashboardTimewindowWatch = $scope.$watch('vm.dashboardTimewindow', function () {
                $scope.$broadcast('dashboardTimewindowChanged', vm.dashboardTimewindow);
            }, true);
            adoptMaxRows();
            vm.dashboardLoading = false;
            if ($scope.gridsterScopeWatcher) {
                $scope.gridsterScopeWatcher();
            }
            $scope.gridsterScopeWatcher = $scope.$watch(
                function() {
                    var hasScope = gridsterElement.scope() ? true : false;
                    return hasScope;
                },
                function(hasScope) {
                    if (hasScope) {
                        $scope.gridsterScopeWatcher();
                        $scope.gridsterScopeWatcher = null;
                        var gridsterScope = gridsterElement.scope();
                        vm.gridster = gridsterScope.gridster;
                        setupGridster(vm.gridster);
                        if (vm.onInit) {
                            vm.onInit({dashboard: vm});
                        }
                    }
                }
            );
        });
    }

    function setupGridster(gridster) {
        if (gridster) {
            if (!gridster.origMoveOverlappingItems) {
                gridster.origMoveOverlappingItems = gridster.moveOverlappingItems;
                gridster.moveOverlappingItems = () => {};
            }
        }
    }

    function loading() {
        return !vm.ignoreLoading && $rootScope.loading;
    }

}

/* eslint-enable angular/angularelement */
