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

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function WidgetController($scope, $timeout, $window, $element, $q, $log, $injector, tbRaf, types, utils,
                                         datasourceService, deviceService, visibleRect, isEdit, widget, deviceAliasList, widgetType) {

    var vm = this;

    $scope.$timeout = $timeout;
    $scope.$q = $q;
    $scope.$injector = $injector;
    $scope.tbRaf = tbRaf;

    $scope.rpcRejection = null;
    $scope.rpcErrorText = null;
    $scope.rpcEnabled = false;
    $scope.executingRpcRequest = false;
    $scope.executingPromises = [];

    var gridsterItemInited = false;

    var datasourceListeners = [];
    var targetDeviceAliasId = null;
    var targetDeviceId = null;
    var originalTimewindow = null;
    var subscriptionTimewindow = {
        fixedWindow: null,
        realtimeWindowMs: null,
        aggregation: null
    };
    var dataUpdateTimer = null;
    var dataUpdateCaf = null;

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

    var widgetContext = {
        inited: false,
        $scope: $scope,
        $container: $('#container', $element),
        $containerParent: $($element),
        width: 0,
        height: 0,
        isEdit: isEdit,
        isMobile: false,
        settings: widget.config.settings,
        datasources: widget.config.datasources,
        data: [],
        timeWindow: {},
        timewindowFunctions: {
            onUpdateTimewindow: onUpdateTimewindow,
            onResetTimewindow: onResetTimewindow
        },
        controlApi: {
            sendOneWayCommand: function(method, params, timeout) {
                return sendCommand(true, method, params, timeout);
            },
            sendTwoWayCommand: function(method, params, timeout) {
                return sendCommand(false, method, params, timeout);
            }
        }
    };

    var widgetTypeInstance;
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

    //var bounds = {top: 0, left: 0, bottom: 0, right: 0};
    //TODO: widgets visibility
    /*var visible = false;*/

    $scope.clearRpcError = function() {
        $scope.rpcRejection = null;
        $scope.rpcErrorText = null;
    }

    vm.gridsterItemInitialized = gridsterItemInitialized;

    //TODO: widgets visibility
    /*vm.visibleRectChanged = visibleRectChanged;

    function visibleRectChanged(newVisibleRect) {
        visibleRect = newVisibleRect;
        updateVisibility();
    }*/

    initialize();

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
            if (widgetContext.dataUpdatePending) {
                widgetContext.dataUpdatePending = false;
                onDataUpdated();
            }
        }
    }

    function updateTimewindow(startTs, endTs) {
        if (subscriptionTimewindow.realtimeWindowMs) {
            widgetContext.timeWindow.maxTime = endTs || (new Date).getTime();
            widgetContext.timeWindow.minTime = startTs || (widgetContext.timeWindow.maxTime - subscriptionTimewindow.realtimeWindowMs);
        } else if (subscriptionTimewindow.fixedWindow) {
            widgetContext.timeWindow.maxTime = subscriptionTimewindow.fixedWindow.endTimeMs;
            widgetContext.timeWindow.minTime = subscriptionTimewindow.fixedWindow.startTimeMs;
        }
    }

    function onDataUpdated() {
        if (dataUpdateTimer) {
            $timeout.cancel(dataUpdateTimer);
            dataUpdateTimer = null;
        }
        if (widgetContext.inited) {
            if (dataUpdateCaf) {
                dataUpdateCaf();
                dataUpdateCaf = null;
            }
            dataUpdateCaf = tbRaf(function() {
                    try {
                        widgetTypeInstance.onDataUpdated();
                    } catch (e) {
                        handleWidgetException(e);
                    }
                });
        } else {
            widgetContext.dataUpdatePending = true;
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
                tbRaf(function() {
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
                tbRaf(function() {
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
                tbRaf(function() {
                    try {
                        widgetTypeInstance.onMobileModeChanged();
                    } catch (e) {
                        handleWidgetException(e);
                    }
                });
            }
        }
    }

    function onDestroy() {
        unsubscribe();
        if (widgetContext.inited) {
            widgetContext.inited = false;
            widgetContext.dataUpdatePending = false;
            try {
                widgetTypeInstance.onDestroy();
            } catch (e) {
                handleWidgetException(e);
            }
        }
    }

    function onRestart() {
        onDestroy();
        onInit();
    }

    function initialize() {
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
                    widgetContext.data.push(datasourceData);
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
                        onRestart();
                    }
                }
            } else {
                checkSubscriptions();
            }
        });

        $scope.$on("$destroy", function () {
            removeResizeListener(widgetContext.$containerParent[0], onResize); // eslint-disable-line no-undef
            onDestroy();
        });

        if (widget.type === types.widgetType.timeseries.value) {
            $scope.$watch(function () {
                return widget.config.timewindow;
            }, function (newTimewindow, prevTimewindow) {
                if (!angular.equals(newTimewindow, prevTimewindow)) {
                    unsubscribe();
                    subscribe();
                }
            });
        }
        subscribe();
    }

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

    function dataUpdated(sourceData, datasourceIndex, dataKeyIndex) {
        var update = true;
        if (widget.type === types.widgetType.latest.value) {
            var prevData = widgetContext.data[datasourceIndex + dataKeyIndex].data;
            if (prevData && prevData[0] && prevData[0].length > 1 && sourceData.data.length > 0) {
                var prevValue = prevData[0][1];
                if (prevValue === sourceData.data[0][1]) {
                    update = false;
                }
            }
        }
        if (update) {
            if (subscriptionTimewindow.realtimeWindowMs) {
                updateTimewindow(sourceData.startTs, sourceData.endTs);
            }
            widgetContext.data[datasourceIndex + dataKeyIndex].data = sourceData.data;
            if (widgetContext.data.length > 1 && !dataUpdateTimer) {
                dataUpdateTimer = $timeout(onDataUpdated, 300, false);
            } else {
                onDataUpdated();
            }
        }
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
            if (dataUpdateTimer) {
                $timeout.cancel(dataUpdateTimer);
                dataUpdateTimer = null;
            }
            for (var i in datasourceListeners) {
                var listener = datasourceListeners[i];
                datasourceService.unsubscribeFromDatasource(listener);
            }
            datasourceListeners = [];
        }
    }

    function subscribe() {
        if (widget.type !== types.widgetType.rpc.value) {
            var index = 0;
            subscriptionTimewindow.fixedWindow = null;
            subscriptionTimewindow.realtimeWindowMs = null;
            subscriptionTimewindow.aggregation = {
                limit: 200,
                type: types.aggregation.avg.value
            };
            if (widget.type === types.widgetType.timeseries.value &&
                angular.isDefined(widget.config.timewindow)) {

                if (angular.isDefined(widget.config.timewindow.aggregation)) {
                    subscriptionTimewindow.aggregation = {
                        limit: widget.config.timewindow.aggregation.limit || 200,
                        type: widget.config.timewindow.aggregation.type || types.aggregation.avg.value
                    };
                }

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
                updateTimewindow();
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
                        dataUpdated(data, datasourceIndex, dataKeyIndex);
                    },
                    datasourceIndex: index
                };

                for (var a = 0; a < datasource.dataKeys.length; a++) {
                    widgetContext.data[index + a].data = [];
                }

                index += datasource.dataKeys.length;

                datasourceListeners.push(listener);
                datasourceService.subscribeToDatasource(listener);
            }
        }
    }

}

/* eslint-enable angular/angularelement */