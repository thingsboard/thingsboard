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
import $ from 'jquery';
import 'javascript-detect-element-resize/detect-element-resize';
import Subscription from '../api/subscription';

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function WidgetController($scope, $timeout, $window, $element, $q, $log, $injector, $filter, tbRaf, types, utils, timeService,
                                         datasourceService, deviceService, visibleRect, isEdit, stDiff, dashboardTimewindow,
                                         dashboardTimewindowApi, widget, aliasesInfo, widgetType) {

    var vm = this;

    $scope.$timeout = $timeout;
    $scope.$q = $q;
    $scope.$injector = $injector;
    $scope.tbRaf = tbRaf;

    $scope.rpcRejection = null;
    $scope.rpcErrorText = null;
    $scope.rpcEnabled = false;
    $scope.executingRpcRequest = false;

    var gridsterItemInited = false;

    var cafs = {};

    /*
     *   data = array of datasourceData
     *   datasourceData = {
     *   			tbDatasource,
     *   			dataKey,     { name, config }
     *   			data = array of [time, value]
     *   }
     */

    var widgetContext = {
        inited: false,
        $scope: $scope,
        $container: $('#container', $element),
        $containerParent: $($element),
        width: 0,
        height: 0,
        isEdit: isEdit,
        isMobile: false,
        widgetConfig: widget.config,
        settings: widget.config.settings,
        units: widget.config.units || '',
        decimals: angular.isDefined(widget.config.decimals) ? widget.config.decimals : 2,
        subscriptions: {},
        defaultSubscription: null,
        timewindowFunctions: {
            onUpdateTimewindow: function(startTimeMs, endTimeMs) {
                if (widgetContext.defaultSubscription) {
                    widgetContext.defaultSubscription.onUpdateTimewindow(startTimeMs, endTimeMs);
                }
            },
            onResetTimewindow: function() {
                if (widgetContext.defaultSubscription) {
                    widgetContext.defaultSubscription.onResetTimewindow();
                }
            }
        },
        subscriptionApi: {
            createSubscription: function(options, subscribe) {
                return createSubscription(options, subscribe);
            },


    //      type: "timeseries" or "latest" or "rpc"
    /*      devicesSubscriptionInfo = [
            {
                deviceId:   ""
                deviceName: ""
                timeseries: [{ name: "", label: "" }, ..]
                attributes: [{ name: "", label: "" }, ..]
            }
    ..
    ]*/

    //      options = {
    //        timeWindowConfig,
    //        useDashboardTimewindow,
    //        legendConfig,
    //        decimals,
    //        units,
    //        callbacks [ onDataUpdated(subscription, apply) ]
    //      }
    //

            createSubscriptionFromInfo: function (type, subscriptionsInfo, options, useDefaultComponents, subscribe) {
                return createSubscriptionFromInfo(type, subscriptionsInfo, options, useDefaultComponents, subscribe);
            },
            removeSubscription: function(id) {
                var subscription = widgetContext.subscriptions[id];
                if (subscription) {
                    subscription.destroy();
                    delete widgetContext.subscriptions[id];
                }
            }
        },
        controlApi: {
            sendOneWayCommand: function(method, params, timeout) {
                if (widgetContext.defaultSubscription) {
                    return widgetContext.defaultSubscription.sendOneWayCommand(method, params, timeout);
                }
                return null;
            },
            sendTwoWayCommand: function(method, params, timeout) {
                if (widgetContext.defaultSubscription) {
                    return widgetContext.defaultSubscription.sendTwoWayCommand(method, params, timeout);
                }
                return null;
            }
        },
        utils: {
            formatValue: formatValue
        }
    };

    var subscriptionContext = {
        $scope: $scope,
        $q: $q,
        $filter: $filter,
        $timeout: $timeout,
        tbRaf: tbRaf,
        timeService: timeService,
        deviceService: deviceService,
        datasourceService: datasourceService,
        utils: utils,
        widgetUtils: widgetContext.utils,
        dashboardTimewindowApi: dashboardTimewindowApi,
        types: types,
        stDiff: stDiff,
        aliasesInfo: aliasesInfo
    };

    var widgetTypeInstance;

    vm.useCustomDatasources = false;

    try {
        widgetTypeInstance = new widgetType(widgetContext);
    } catch (e) {
        handleWidgetException(e);
        widgetTypeInstance = {};
    }
    if (!widgetTypeInstance.onInit) {
        widgetTypeInstance.onInit = function() {};
    }
    if (!widgetTypeInstance.onDataUpdated) {
        widgetTypeInstance.onDataUpdated = function() {};
    }
    if (!widgetTypeInstance.onResize) {
        widgetTypeInstance.onResize = function() {};
    }
    if (!widgetTypeInstance.onEditModeChanged) {
        widgetTypeInstance.onEditModeChanged = function() {};
    }
    if (!widgetTypeInstance.onMobileModeChanged) {
        widgetTypeInstance.onMobileModeChanged = function() {};
    }
    if (!widgetTypeInstance.onDestroy) {
        widgetTypeInstance.onDestroy = function() {};
    }
    if (widgetTypeInstance.useCustomDatasources) {
        vm.useCustomDatasources = widgetTypeInstance.useCustomDatasources();
    }

    //TODO: widgets visibility

    //var bounds = {top: 0, left: 0, bottom: 0, right: 0};
    /*var visible = false;*/
    /*vm.visibleRectChanged = visibleRectChanged;

    function visibleRectChanged(newVisibleRect) {
        visibleRect = newVisibleRect;
        updateVisibility();
    }*/

    $scope.clearRpcError = function() {
        if (widgetContext.defaultSubscription) {
            widgetContext.defaultSubscription.clearRpcError();
        }
    }

    vm.gridsterItemInitialized = gridsterItemInitialized;

    initialize();


    /*
            options = {
                 type,
                 targetDeviceAliasIds,  // RPC
                 targetDeviceIds,       // RPC
                 datasources,
                 timeWindowConfig,
                 useDashboardTimewindow,
                 legendConfig,
                 decimals,
                 units,
                 callbacks
            }
     */

    function createSubscriptionFromInfo(type, subscriptionsInfo, options, useDefaultComponents, subscribe) {
        var deferred = $q.defer();
        options.type = type;

        if (useDefaultComponents) {
            defaultComponentsOptions(options);
        } else {
            if (!options.timeWindowConfig) {
                options.useDashboardTimewindow = true;
            }
        }

        utils.createDatasoucesFromSubscriptionsInfo(subscriptionsInfo).then(
            function (datasources) {
                options.datasources = datasources;
                var subscription = createSubscription(options, subscribe);
                if (useDefaultComponents) {
                    defaultSubscriptionOptions(subscription, options);
                }
                deferred.resolve(subscription);
            }
        );
        return deferred.promise;
    }

    function createSubscription(options, subscribe) {
        options.dashboardTimewindow = dashboardTimewindow;
        var subscription =
            new Subscription(subscriptionContext, options);
        widgetContext.subscriptions[subscription.id] = subscription;
        if (subscribe) {
            subscription.subscribe();
        }
        return subscription;
    }

    function defaultComponentsOptions(options) {
        options.useDashboardTimewindow = angular.isDefined(widget.config.useDashboardTimewindow)
            ? widget.config.useDashboardTimewindow : true;

        options.timeWindowConfig = options.useDashboardTimewindow ? dashboardTimewindow : widget.config.timewindow;
        options.legendConfig = null;

        if ($scope.displayLegend) {
            options.legendConfig = $scope.legendConfig;
        }
        options.decimals = widgetContext.decimals;
        options.units = widgetContext.units;

        options.callbacks = {
            onDataUpdated: function() {
                widgetTypeInstance.onDataUpdated();
            },
            onDataUpdateError: function(subscription, e) {
                handleWidgetException(e);
            },
            dataLoading: function(subscription) {
                if ($scope.loadingData !== subscription.loadingData) {
                    $scope.loadingData = subscription.loadingData;
                }
            },
            legendDataUpdated: function(subscription, apply) {
                if (apply) {
                    $scope.$digest();
                }
            },
            timeWindowUpdated: function(subscription, timeWindowConfig) {
                widget.config.timewindow = timeWindowConfig;
                $scope.$apply();
            }
        }
    }

    function defaultSubscriptionOptions(subscription, options) {
        if (!options.useDashboardTimewindow) {
            $scope.$watch(function () {
                return widget.config.timewindow;
            }, function (newTimewindow, prevTimewindow) {
                if (!angular.equals(newTimewindow, prevTimewindow)) {
                    subscription.updateTimewindowConfig(widget.config.timewindow);
                }
            });
        }
        if ($scope.displayLegend) {
            $scope.legendData = subscription.legendData;
        }
    }

    function createDefaultSubscription() {
        var subscription;
        var options;
        if (widget.type !== types.widgetType.rpc.value && widget.type !== types.widgetType.static.value) {
            options = {
                type: widget.type,
                datasources: angular.copy(widget.config.datasources)
            };
            defaultComponentsOptions(options);

            subscription = createSubscription(options);

            defaultSubscriptionOptions(subscription, options);

            // backward compatibility

            widgetContext.datasources = subscription.datasources;
            widgetContext.data = subscription.data;
            widgetContext.hiddenData = subscription.hiddenData;
            widgetContext.timeWindow = subscription.timeWindow;

        } else if (widget.type === types.widgetType.rpc.value) {
            $scope.loadingData = false;
            options = {
                type: widget.type,
                targetDeviceAliasIds: widget.config.targetDeviceAliasIds
            }
            options.callbacks = {
                rpcStateChanged: function(subscription) {
                    $scope.rpcEnabled = subscription.rpcEnabled;
                    $scope.executingRpcRequest = subscription.executingRpcRequest;
                },
                onRpcSuccess: function(subscription) {
                    $scope.executingRpcRequest = subscription.executingRpcRequest;
                },
                onRpcFailed: function(subscription) {
                    $scope.executingRpcRequest = subscription.executingRpcRequest;
                    $scope.rpcErrorText = subscription.rpcErrorText;
                    $scope.rpcRejection = subscription.rpcRejection;
                },
                onRpcErrorCleared: function() {
                    $scope.rpcErrorText = null;
                    $scope.rpcRejection = null;
                }
            }
            subscription = createSubscription(options);
        } else if (widget.type === types.widgetType.static.value) {
            $scope.loadingData = false;
        }
        if (subscription) {
            widgetContext.defaultSubscription = subscription;
        }
    }


    function initialize() {

        if (!vm.useCustomDatasources) {
            createDefaultSubscription();
        } else {
            $scope.loadingData = false;
        }

        $scope.$on('toggleDashboardEditMode', function (event, isEdit) {
            onEditModeChanged(isEdit);
        });

        addResizeListener(widgetContext.$containerParent[0], onResize); // eslint-disable-line no-undef

        $scope.$watch(function () {
            return widget.row + ',' + widget.col + ',' + widget.config.mobileOrder;
        }, function () {
            //updateBounds();
            $scope.$emit("widgetPositionChanged", widget);
        });

        $scope.$on('gridster-item-resized', function (event, item) {
            if (!widgetContext.isMobile) {
                widget.sizeX = item.sizeX;
                widget.sizeY = item.sizeY;
            }
        });

        $scope.$on('mobileModeChanged', function (event, newIsMobile) {
            onMobileModeChanged(newIsMobile);
        });

        $scope.$on("$destroy", function () {
            removeResizeListener(widgetContext.$containerParent[0], onResize); // eslint-disable-line no-undef
            onDestroy();
        });
    }

    function handleWidgetException(e) {
        $log.error(e);
        $scope.widgetErrorData = utils.processWidgetException(e);
    }

    function onInit() {
        if (!widgetContext.inited) {
            widgetContext.inited = true;
            try {
                widgetTypeInstance.onInit();
            } catch (e) {
                handleWidgetException(e);
            }
            if (!vm.useCustomDatasources && widgetContext.defaultSubscription) {
                widgetContext.defaultSubscription.subscribe();
            }
        }
    }

    function checkSize() {
        var width = widgetContext.$containerParent.width();
        var height = widgetContext.$containerParent.height();
        var sizeChanged = false;

        if (!widgetContext.width || widgetContext.width != width || !widgetContext.height || widgetContext.height != height) {
            if (width > 0 && height > 0) {
                widgetContext.$container.css('height', height + 'px');
                widgetContext.$container.css('width', width + 'px');
                widgetContext.width = width;
                widgetContext.height = height;
                sizeChanged = true;
            }
        }
        return sizeChanged;
    }

    function onResize() {
        if (checkSize()) {
            if (widgetContext.inited) {
                if (cafs['resize']) {
                    cafs['resize']();
                    cafs['resize'] = null;
                }
                cafs['resize'] = tbRaf(function() {
                    try {
                        widgetTypeInstance.onResize();
                    } catch (e) {
                        handleWidgetException(e);
                    }
                });
            } else if (gridsterItemInited) {
                onInit();
            }
        }
    }

    function gridsterItemInitialized(item) {
        if (item && item.gridster) {
            widgetContext.isMobile = item.gridster.isMobile;
            gridsterItemInited = true;
            if (checkSize()) {
                onInit();
            }
            // gridsterItemElement = $(item.$element);
            //updateVisibility();
        }
    }

    function onEditModeChanged(isEdit) {
        if (widgetContext.isEdit != isEdit) {
            widgetContext.isEdit = isEdit;
            if (widgetContext.inited) {
                if (cafs['editMode']) {
                    cafs['editMode']();
                    cafs['editMode'] = null;
                }
                cafs['editMode'] = tbRaf(function() {
                    try {
                        widgetTypeInstance.onEditModeChanged();
                    } catch (e) {
                        handleWidgetException(e);
                    }
                });
            }
        }
    }

    function onMobileModeChanged(isMobile) {
        if (widgetContext.isMobile != isMobile) {
            widgetContext.isMobile = isMobile;
            if (widgetContext.inited) {
                if (cafs['mobileMode']) {
                    cafs['mobileMode']();
                    cafs['mobileMode'] = null;
                }
                cafs['mobileMode'] = tbRaf(function() {
                    try {
                        widgetTypeInstance.onMobileModeChanged();
                    } catch (e) {
                        handleWidgetException(e);
                    }
                });
            }
        }
    }

    function isNumeric(val) {
        return (val - parseFloat( val ) + 1) >= 0;
    }

    function formatValue(value, dec, units) {
        if (angular.isDefined(value) &&
            value !== null && isNumeric(value)) {
            var formatted = value;
            if (angular.isDefined(dec)) {
                formatted = formatted.toFixed(dec);
            }
            formatted = (formatted * 1).toString();
            if (angular.isDefined(units) && units.length > 0) {
                formatted += ' ' + units;
            }
            return formatted;
        } else {
            return '';
        }
    }

    function onDestroy() {
        for (var id in widgetContext.subscriptions) {
            var subscription = widgetContext.subscriptions[id];
            subscription.destroy();
        }
        widgetContext.subscriptions = [];
        if (widgetContext.inited) {
            widgetContext.inited = false;
            for (var cafId in cafs) {
                if (cafs[cafId]) {
                    cafs[cafId]();
                    cafs[cafId] = null;
                }
            }
            try {
                widgetTypeInstance.onDestroy();
            } catch (e) {
                handleWidgetException(e);
            }
        }
    }

    //TODO: widgets visibility
    /*function updateVisibility(forceRedraw) {
        if (visibleRect) {
            forceRedraw = forceRedraw || visibleRect.containerResized;
            var newVisible = false;
            if (visibleRect.isMobile && gridsterItemElement) {
                var topPx = gridsterItemElement.position().top;
                var bottomPx = topPx + widget.sizeY * visibleRect.curRowHeight;
                newVisible = !(topPx > visibleRect.bottomPx ||
                bottomPx < visibleRect.topPx);
            } else {
                newVisible = !(bounds.left > visibleRect.right ||
                bounds.right < visibleRect.left ||
                bounds.top > visibleRect.bottom ||
                bounds.bottom < visibleRect.top);
            }
            if (visible != newVisible) {
                visible = newVisible;
                if (visible) {
                    onRedraw(50);
                }
            } else if (forceRedraw && visible) {
                onRedraw(50);
            }
        }
    }*/

/*    function updateBounds() {
        bounds = {
            top: widget.row,
            left: widget.col,
            bottom: widget.row + widget.sizeY,
            right: widget.col + widget.sizeX
        };
        updateVisibility(true);
        onRedraw();
    }*/

}

/* eslint-enable angular/angularelement */