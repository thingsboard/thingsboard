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

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function WidgetController($scope, $timeout, $window, $element, $q, $log, $injector, types, visibleRect,
                                         datasourceService, deviceService, isPreview, widget, deviceAliasList, fns) {

    var vm = this;

    var timeWindow = {};
    var subscriptionTimewindow = {
        fixedWindow: null,
        realtimeWindowMs: null
    };

    /*
     *   data = array of datasourceData
     *   datasourceData = {
     *   			tbDatasource,
     *   			dataKey,     { name, config }
     *   			data = array of [time, value]
     *   }
     *
     *
     */
    var data = [];
    var datasourceListeners = [];
    var targetDeviceAliasId = null;
    var targetDeviceId = null;


    //var bounds = {top: 0, left: 0, bottom: 0, right: 0};
    //TODO: widgets visibility
    /*var visible = false;*/

    var lastWidth, lastHeight;
    var containerParent = $($element);
    var container = $('#container', $element);
    var containerElement = container[0];
    var inited = false;

   // var gridsterItemElement;

    var gridsterItem;
    var timer;

    var init = fns.init || function () {
        };

    var redraw = fns.redraw || function () {
        };

    var destroy = fns.destroy || function () {
        };

    $scope.$timeout = $timeout;
    $scope.$q = $q;
    $scope.$injector = $injector;

    $scope.rpcRejection = null;
    $scope.rpcErrorText = null;
    $scope.rpcEnabled = false;
    $scope.executingRpcRequest = false;
    $scope.executingPromises = [];

    function sendCommand(oneWayElseTwoWay, method, params, timeout) {
        if (!$scope.rpcEnabled) {
            return $q.reject();
        }

        if ($scope.rpcRejection && $scope.rpcRejection.status !== 408) {
            $scope.rpcRejection = null;
            $scope.rpcErrorText = null;
        }

        var requestBody = {
            method: method,
            params: params
        };

        if (timeout && timeout > 0) {
            requestBody.timeout = timeout;
        }

        var deferred = $q.defer();
        $scope.executingRpcRequest = true;
        if ($scope.widgetEditMode) {
            $timeout(function() {
                $scope.executingRpcRequest = false;
                if (oneWayElseTwoWay) {
                    deferred.resolve();
                } else {
                    deferred.resolve(requestBody);
                }
            }, 500);
        } else {
            $scope.executingPromises.push(deferred.promise);
            var targetSendFunction = oneWayElseTwoWay ? deviceService.sendOneWayRpcCommand : deviceService.sendTwoWayRpcCommand;
            targetSendFunction(targetDeviceId, requestBody).then(
                function success(responseBody) {
                    $scope.rpcRejection = null;
                    $scope.rpcErrorText = null;
                    var index = $scope.executingPromises.indexOf(deferred.promise);
                    if (index >= 0) {
                        $scope.executingPromises.splice( index, 1 );
                    }
                    $scope.executingRpcRequest = $scope.executingPromises.length > 0;
                    deferred.resolve(responseBody);
                },
                function fail(rejection) {
                    var index = $scope.executingPromises.indexOf(deferred.promise);
                    if (index >= 0) {
                        $scope.executingPromises.splice( index, 1 );
                    }
                    $scope.executingRpcRequest = $scope.executingPromises.length > 0;
                    if (!$scope.executingRpcRequest || rejection.status === 408) {
                        $scope.rpcRejection = rejection;
                        if (rejection.status === 408) {
                            $scope.rpcErrorText = 'Device is offline.';
                        } else {
                            $scope.rpcErrorText =  'Error : ' + rejection.status + ' - ' + rejection.statusText;
                            if (rejection.data && rejection.data.length > 0) {
                                $scope.rpcErrorText += '</br>';
                                $scope.rpcErrorText += rejection.data;
                            }
                        }
                    }
                    deferred.reject(rejection);
                }
            );
        }
        return deferred.promise;
    }

    $scope.clearRpcError = function() {
        $scope.rpcRejection = null;
        $scope.rpcErrorText = null;
    }

    var controlApi = {};

    controlApi.sendOneWayCommand = function(method, params, timeout) {
        return sendCommand(true, method, params, timeout);
    };

    controlApi.sendTwoWayCommand = function(method, params, timeout) {
        return sendCommand(false, method, params, timeout);
    };

    vm.gridsterItemInitialized = gridsterItemInitialized;
    //TODO: widgets visibility
    /*vm.visibleRectChanged = visibleRectChanged;

    function visibleRectChanged(newVisibleRect) {
        visibleRect = newVisibleRect;
        updateVisibility();
    }*/

    function gridsterItemInitialized(item) {
        if (item) {
            gridsterItem = item;
           // gridsterItemElement = $(item.$element);
            //updateVisibility();
            onRedraw();
        }
    }

    initWidget();

    function initWidget() {
        if (widget.type !== types.widgetType.rpc.value) {
            for (var i in widget.config.datasources) {
                var datasource = angular.copy(widget.config.datasources[i]);
                for (var a in datasource.dataKeys) {
                    var dataKey = datasource.dataKeys[a];
                    var datasourceData = {
                        datasource: datasource,
                        dataKey: dataKey,
                        data: []
                    };
                    data.push(datasourceData);
                }
            }
        } else {
            if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length > 0) {
                targetDeviceAliasId = widget.config.targetDeviceAliasIds[0];
                if (deviceAliasList[targetDeviceAliasId]) {
                    targetDeviceId = deviceAliasList[targetDeviceAliasId].deviceId;
                }
            }
            if (targetDeviceId) {
                $scope.rpcEnabled = true;
            } else {
                $scope.rpcEnabled = $scope.widgetEditMode ? true : false;
            }
        }

        $scope.$on('toggleDashboardEditMode', function (event, isEdit) {
            isPreview = isEdit;
            onRedraw();
        });

        $scope.$on('gridster-item-resized', function (event, item) {
            if (item) {
                updateBounds();
            }
        });

        $scope.$on('gridster-item-transition-end', function (event, item) {
            if (item) {
                updateBounds();
            }
        });

        $scope.$watch(function () {
            return widget.row + ',' + widget.col + ',' + widget.config.mobileOrder;
        }, function () {
            updateBounds();
            $scope.$emit("widgetPositionChanged", widget);
        });

        $scope.$on('onWidgetFullscreenChanged', function(event, isWidgetExpanded, fullscreenWidget) {
            if (widget === fullscreenWidget) {
                onRedraw(0);
            }
        });

        $scope.$on('deviceAliasListChanged', function (event, newDeviceAliasList) {
            deviceAliasList = newDeviceAliasList;
            if (widget.type === types.widgetType.rpc.value) {
                if (targetDeviceAliasId) {
                    var deviceId = null;
                    if (deviceAliasList[targetDeviceAliasId]) {
                        deviceId = deviceAliasList[targetDeviceAliasId].deviceId;
                    }
                    if (!angular.equals(deviceId, targetDeviceId)) {
                        targetDeviceId = deviceId;
                        if (targetDeviceId) {
                            $scope.rpcEnabled = true;
                        } else {
                            $scope.rpcEnabled = $scope.widgetEditMode ? true : false;
                        }
                        inited = false;
                        onRedraw();
                    }
                }
            } else {
                checkSubscriptions();
            }
        });

        $scope.$on("$destroy", function () {
            unsubscribe();
            destroy();
        });

        subscribe();

        if (widget.type === types.widgetType.timeseries.value) {
            $scope.$watch(function () {
                return widget.config.timewindow;
            }, function (newTimewindow, prevTimewindow) {
                if (!angular.equals(newTimewindow, prevTimewindow)) {
                    unsubscribe();
                    subscribe();
                }
            });
        } else if (widget.type === types.widgetType.rpc.value) {
            if (!inited) {
                init(containerElement, widget.config.settings, widget.config.datasources, data, $scope, controlApi);
                inited = true;
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

    function updateBounds() {
        /*bounds = {
            top: widget.row,
            left: widget.col,
            bottom: widget.row + widget.sizeY,
            right: widget.col + widget.sizeX
        };
        updateVisibility(true);*/
        onRedraw();
    }

    var originalTimewindow;

    var timewindowFunctions = {
        onUpdateTimewindow: onUpdateTimewindow,
        onResetTimewindow: onResetTimewindow
    };

    function onResetTimewindow() {
        if (originalTimewindow) {
            widget.config.timewindow = angular.copy(originalTimewindow);
            originalTimewindow = null;
        }
    }

    function onUpdateTimewindow(startTimeMs, endTimeMs) {
        if (!originalTimewindow) {
            originalTimewindow = angular.copy(widget.config.timewindow);
        }
        widget.config.timewindow = {
            history: {
                fixedTimewindow: {
                    startTimeMs: startTimeMs,
                    endTimeMs: endTimeMs
                }
            }
        };
    }

    function onRedraw(delay, dataUpdate, tickUpdate) {
        //TODO: widgets visibility
        /*if (!visible) {
            return;
        }*/
        if (angular.isUndefined(delay)) {
            delay = 0;
        }
        $timeout(function () {
            var width = containerParent.width();
            var height = containerParent.height();
            var sizeChanged = false;

            if (!lastWidth || lastWidth != width || !lastHeight || lastHeight != height) {
                if (width > 0 && height > 0) {
                    container.css('height', height + 'px');
                    container.css('width', width + 'px');
                    lastWidth = width;
                    lastHeight = height;
                    sizeChanged = true;
                }
            }

            if (width > 20 && height > 20) {
                if (!inited) {
                    init(containerElement, widget.config.settings, widget.config.datasources, data, $scope, controlApi, timewindowFunctions, gridsterItem);
                    inited = true;
                }
                if (widget.type === types.widgetType.timeseries.value) {
                    if (dataUpdate && timer) {
                        $timeout.cancel(timer);
                        timer = $timeout(onTick, 1500, false);
                    }
                    if (subscriptionTimewindow.realtimeWindowMs) {
                        timeWindow.maxTime = (new Date).getTime();
                        timeWindow.minTime = timeWindow.maxTime - subscriptionTimewindow.realtimeWindowMs;
                    } else if (subscriptionTimewindow.fixedWindow) {
                        timeWindow.maxTime = subscriptionTimewindow.fixedWindow.endTimeMs;
                        timeWindow.minTime = subscriptionTimewindow.fixedWindow.startTimeMs;
                    }
                }
                redraw(containerElement, width, height, data, timeWindow, sizeChanged, $scope, dataUpdate, tickUpdate, gridsterItem);
            }
        }, delay, false);
    }

    function onDataUpdated(sourceData, datasourceIndex, dataKeyIndex) {
        data[datasourceIndex + dataKeyIndex].data = sourceData;
        onRedraw(0, true);
    }

    function checkSubscriptions() {
        if (widget.type !== types.widgetType.rpc.value) {
            var subscriptionsChanged = false;
            for (var i in datasourceListeners) {
                var listener = datasourceListeners[i];
                var deviceId = null;
                var aliasName = null;
                if (listener.datasource.type === types.datasourceType.device) {
                    if (deviceAliasList[listener.datasource.deviceAliasId]) {
                        deviceId = deviceAliasList[listener.datasource.deviceAliasId].deviceId;
                        aliasName = deviceAliasList[listener.datasource.deviceAliasId].alias;
                    }
                    if (!angular.equals(deviceId, listener.deviceId) ||
                        !angular.equals(aliasName, listener.datasource.name)) {
                        subscriptionsChanged = true;
                        break;
                    }
                }
            }
            if (subscriptionsChanged) {
                unsubscribe();
                subscribe();
            }
        }
    }

    function unsubscribe() {
        if (widget.type !== types.widgetType.rpc.value) {
            if (timer) {
                $timeout.cancel(timer);
                timer = null;
            }
            for (var i in datasourceListeners) {
                var listener = datasourceListeners[i];
                datasourceService.unsubscribeFromDatasource(listener);
            }
            datasourceListeners = [];
        }
    }

    function onTick() {
        onRedraw(0, false, true);
        timer = $timeout(onTick, 1000, false);
    }

    function subscribe() {
        if (widget.type !== types.widgetType.rpc.value) {
            var index = 0;
            subscriptionTimewindow.fixedWindow = null;
            subscriptionTimewindow.realtimeWindowMs = null;
            if (widget.type === types.widgetType.timeseries.value &&
                angular.isDefined(widget.config.timewindow)) {
                if (angular.isDefined(widget.config.timewindow.realtime)) {
                    subscriptionTimewindow.realtimeWindowMs = widget.config.timewindow.realtime.timewindowMs;
                } else if (angular.isDefined(widget.config.timewindow.history)) {
                    if (angular.isDefined(widget.config.timewindow.history.timewindowMs)) {
                        var currentTime = (new Date).getTime();
                        subscriptionTimewindow.fixedWindow = {
                            startTimeMs: currentTime - widget.config.timewindow.history.timewindowMs,
                            endTimeMs: currentTime
                        }
                    } else {
                        subscriptionTimewindow.fixedWindow = {
                            startTimeMs: widget.config.timewindow.history.fixedTimewindow.startTimeMs,
                            endTimeMs: widget.config.timewindow.history.fixedTimewindow.endTimeMs
                        }
                    }
                }
            }
            for (var i in widget.config.datasources) {
                var datasource = widget.config.datasources[i];
                var deviceId = null;
                if (datasource.type === types.datasourceType.device && datasource.deviceAliasId) {
                    if (deviceAliasList[datasource.deviceAliasId]) {
                        deviceId = deviceAliasList[datasource.deviceAliasId].deviceId;
                        datasource.name = deviceAliasList[datasource.deviceAliasId].alias;
                    }
                } else {
                    datasource.name = types.datasourceType.function;
                }
                var listener = {
                    widget: widget,
                    subscriptionTimewindow: subscriptionTimewindow,
                    datasource: datasource,
                    deviceId: deviceId,
                    dataUpdated: function (data, datasourceIndex, dataKeyIndex) {
                        onDataUpdated(data, datasourceIndex, dataKeyIndex);
                    },
                    datasourceIndex: index
                };

                for (var a = 0; a < datasource.dataKeys.length; a++) {
                    data[index + a].data = [];
                }

                index += datasource.dataKeys.length;

                datasourceListeners.push(listener);
                datasourceService.subscribeToDatasource(listener);
            }

            if (subscriptionTimewindow.realtimeWindowMs) {
                timer = $timeout(onTick, 0, false);
            }
        }
    }

}

/* eslint-enable angular/angularelement */