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

export default class Subscription {
    constructor(subscriptionContext, options) {

        this.ctx = subscriptionContext;
        this.type = options.type;
        this.callbacks = options.callbacks;
        this.id = this.ctx.utils.guid();
        this.cafs = {};
        this.registrations = [];

        if (this.type === this.ctx.types.widgetType.rpc.value) {
            this.callbacks.rpcStateChanged = this.callbacks.rpcStateChanged || function(){};
            this.callbacks.onRpcSuccess = this.callbacks.onRpcSuccess || function(){};
            this.callbacks.onRpcFailed = this.callbacks.onRpcFailed || function(){};
            this.callbacks.onRpcErrorCleared = this.callbacks.onRpcErrorCleared || function(){};

            this.targetDeviceAliasIds = options.targetDeviceAliasIds;
            this.targetDeviceIds = options.targetDeviceIds;

            this.targetDeviceAliasId = null;
            this.targetDeviceId = null;

            this.rpcRejection = null;
            this.rpcErrorText = null;
            this.rpcEnabled = false;
            this.executingRpcRequest = false;
            this.executingPromises = [];
            this.initRpc();
        } else {
            this.callbacks.onDataUpdated = this.callbacks.onDataUpdated || function(){};
            this.callbacks.onDataUpdateError = this.callbacks.onDataUpdateError || function(){};
            this.callbacks.dataLoading = this.callbacks.dataLoading || function(){};
            this.callbacks.legendDataUpdated = this.callbacks.legendDataUpdated || function(){};
            this.callbacks.timeWindowUpdated = this.callbacks.timeWindowUpdated || function(){};

            this.datasources = options.datasources;
            this.datasourceListeners = [];
            this.data = [];
            this.hiddenData = [];
            this.originalTimewindow = null;
            this.timeWindow = {
                stDiff: this.ctx.stDiff
            }
            this.useDashboardTimewindow = options.useDashboardTimewindow;

            if (this.useDashboardTimewindow) {
                this.timeWindowConfig = angular.copy(options.dashboardTimewindow);
            } else {
                this.timeWindowConfig = angular.copy(options.timeWindowConfig);
            }

            this.subscriptionTimewindow = null;

            this.units = options.units || '';
            this.decimals = angular.isDefined(options.decimals) ? options.decimals : 2;

            this.loadingData = false;

            if (options.legendConfig) {
                this.legendConfig = options.legendConfig;
                this.legendData = {
                    keys: [],
                    data: []
                };
                this.displayLegend = true;
            } else {
                this.displayLegend = false;
            }
            this.caulculateLegendData = this.displayLegend &&
                this.type === this.ctx.types.widgetType.timeseries.value &&
                (this.legendConfig.showMin === true ||
                this.legendConfig.showMax === true ||
                this.legendConfig.showAvg === true ||
                this.legendConfig.showTotal === true);
            this.initDataSubscription();
        }
    }

    initDataSubscription() {
        var dataIndex = 0;
        for (var i = 0; i < this.datasources.length; i++) {
            var datasource = this.datasources[i];
            for (var a = 0; a < datasource.dataKeys.length; a++) {
                var dataKey = datasource.dataKeys[a];
                dataKey.pattern = angular.copy(dataKey.label);
                var datasourceData = {
                    datasource: datasource,
                    dataKey: dataKey,
                    data: []
                };
                this.data.push(datasourceData);
                this.hiddenData.push({data: []});
                if (this.displayLegend) {
                    var legendKey = {
                        dataKey: dataKey,
                        dataIndex: dataIndex++
                    };
                    this.legendData.keys.push(legendKey);
                    var legendKeyData = {
                        min: null,
                        max: null,
                        avg: null,
                        total: null,
                        hidden: false
                    };
                    this.legendData.data.push(legendKeyData);
                }
            }
        }

        var subscription = this;
        var registration;

        if (this.displayLegend) {
            this.legendData.keys = this.ctx.$filter('orderBy')(this.legendData.keys, '+label');
            registration = this.ctx.$scope.$watch(
                function() {
                    return subscription.legendData.data;
                },
                function (newValue, oldValue) {
                    for(var i = 0; i < newValue.length; i++) {
                        if(newValue[i].hidden != oldValue[i].hidden) {
                            subscription.updateDataVisibility(i);
                        }
                    }
                }, true);
            this.registrations.push(registration);
        }

        if (this.type === this.ctx.types.widgetType.timeseries.value) {
            if (this.useDashboardTimewindow) {
                registration = this.ctx.$scope.$on('dashboardTimewindowChanged', function (event, newDashboardTimewindow) {
                    if (!angular.equals(subscription.timeWindowConfig, newDashboardTimewindow) && newDashboardTimewindow) {
                        subscription.timeWindowConfig = angular.copy(newDashboardTimewindow);
                        subscription.unsubscribe();
                        subscription.subscribe();
                    }
                });
                this.registrations.push(registration);
            } else {
                this.startWatchingTimewindow();
            }
        }

        registration = this.ctx.$scope.$on('deviceAliasListChanged', function () {
            subscription.checkSubscriptions();
        });

        this.registrations.push(registration);
    }

    startWatchingTimewindow() {
        var subscription = this;
        this.timeWindowWatchRegistration = this.ctx.$scope.$watch(function () {
            return subscription.timeWindowConfig;
        }, function (newTimewindow, prevTimewindow) {
            if (!angular.equals(newTimewindow, prevTimewindow)) {
                subscription.unsubscribe();
                subscription.subscribe();
            }
        }, true);
        this.registrations.push(this.timeWindowWatchRegistration);
    }

    stopWatchingTimewindow() {
        if (this.timeWindowWatchRegistration) {
            this.timeWindowWatchRegistration();
            var index = this.registrations.indexOf(this.timeWindowWatchRegistration);
            if (index > -1) {
                this.registrations.splice(index, 1);
            }
        }
    }

    initRpc() {

        if (this.targetDeviceAliasIds && this.targetDeviceAliasIds.length > 0) {
            this.targetDeviceAliasId = this.targetDeviceAliasIds[0];
            if (this.ctx.aliasesInfo.deviceAliases[this.targetDeviceAliasId]) {
                this.targetDeviceId = this.ctx.aliasesInfo.deviceAliases[this.targetDeviceAliasId].deviceId;
            }
            var subscription = this;
            var registration = this.ctx.$scope.$on('deviceAliasListChanged', function () {
                var deviceId = null;
                if (subscription.ctx.aliasesInfo.deviceAliases[subscription.targetDeviceAliasId]) {
                    deviceId = subscription.ctx.aliasesInfo.deviceAliases[subscription.targetDeviceAliasId].deviceId;
                }
                if (!angular.equals(deviceId, subscription.targetDeviceId)) {
                    subscription.targetDeviceId = deviceId;
                    if (subscription.targetDeviceId) {
                        subscription.rpcEnabled = true;
                    } else {
                        subscription.rpcEnabled = subscription.ctx.$scope.widgetEditMode ? true : false;
                    }
                    subscription.callbacks.rpcStateChanged(subscription);
                }
            });
            this.registrations.push(registration);
        } else if (this.targetDeviceIds && this.targetDeviceIds.length > 0) {
            this.targetDeviceId = this.targetDeviceIds[0];
        }

        if (this.targetDeviceId) {
            this.rpcEnabled = true;
        } else {
            this.rpcEnabled = this.ctx.$scope.widgetEditMode ? true : false;
        }
        this.callbacks.rpcStateChanged(this);
    }

    clearRpcError() {
        this.rpcRejection = null;
        this.rpcErrorText = null;
        this.callbacks.onRpcErrorCleared(this);
    }

    sendOneWayCommand(method, params, timeout) {
        return this.sendCommand(true, method, params, timeout);
    }

    sendTwoWayCommand(method, params, timeout) {
        return this.sendCommand(false, method, params, timeout);
    }

    sendCommand(oneWayElseTwoWay, method, params, timeout) {
        if (!this.rpcEnabled) {
            return this.ctx.$q.reject();
        }

        if (this.rpcRejection && this.rpcRejection.status !== 408) {
            this.rpcRejection = null;
            this.rpcErrorText = null;
            this.callbacks.onRpcErrorCleared(this);
        }

        var subscription = this;

        var requestBody = {
            method: method,
            params: params
        };

        if (timeout && timeout > 0) {
            requestBody.timeout = timeout;
        }

        var deferred = this.ctx.$q.defer();
        this.executingRpcRequest = true;
        this.callbacks.rpcStateChanged(this);
        if (this.ctx.$scope.widgetEditMode) {
            this.ctx.$timeout(function() {
                subscription.executingRpcRequest = false;
                subscription.callbacks.rpcStateChanged(subscription);
                if (oneWayElseTwoWay) {
                    deferred.resolve();
                } else {
                    deferred.resolve(requestBody);
                }
            }, 500);
        } else {
            this.executingPromises.push(deferred.promise);
            var targetSendFunction = oneWayElseTwoWay ? this.ctx.deviceService.sendOneWayRpcCommand : this.ctx.deviceService.sendTwoWayRpcCommand;
            targetSendFunction(this.targetDeviceId, requestBody).then(
                function success(responseBody) {
                    subscription.rpcRejection = null;
                    subscription.rpcErrorText = null;
                    var index = subscription.executingPromises.indexOf(deferred.promise);
                    if (index >= 0) {
                        subscription.executingPromises.splice( index, 1 );
                    }
                    subscription.executingRpcRequest = subscription.executingPromises.length > 0;
                    subscription.callbacks.onRpcSuccess(subscription);
                    deferred.resolve(responseBody);
                },
                function fail(rejection) {
                    var index = subscription.executingPromises.indexOf(deferred.promise);
                    if (index >= 0) {
                        subscription.executingPromises.splice( index, 1 );
                    }
                    subscription.executingRpcRequest = subscription.executingPromises.length > 0;
                    subscription.callbacks.rpcStateChanged(subscription);
                    if (!subscription.executingRpcRequest || rejection.status === 408) {
                        subscription.rpcRejection = rejection;
                        if (rejection.status === 408) {
                            subscription.rpcErrorText = 'Device is offline.';
                        } else {
                            subscription.rpcErrorText =  'Error : ' + rejection.status + ' - ' + rejection.statusText;
                            if (rejection.data && rejection.data.length > 0) {
                                subscription.rpcErrorText += '</br>';
                                subscription.rpcErrorText += rejection.data;
                            }
                        }
                        subscription.callbacks.onRpcFailed(subscription);
                    }
                    deferred.reject(rejection);
                }
            );
        }
        return deferred.promise;
    }

    updateDataVisibility(index) {
        var hidden = this.legendData.data[index].hidden;
        if (hidden) {
            this.hiddenData[index].data = this.data[index].data;
            this.data[index].data = [];
        } else {
            this.data[index].data = this.hiddenData[index].data;
            this.hiddenData[index].data = [];
        }
        this.onDataUpdated();
    }

    onDataUpdated(apply) {
        if (this.cafs['dataUpdated']) {
            this.cafs['dataUpdated']();
            this.cafs['dataUpdated'] = null;
        }
        var subscription = this;
        this.cafs['dataUpdated'] = this.ctx.tbRaf(function() {
            try {
                subscription.callbacks.onDataUpdated(subscription, apply);
            } catch (e) {
                subscription.callbacks.onDataUpdateError(subscription, e);
            }
        });
        if (apply) {
            this.ctx.$scope.$digest();
        }
    }

    updateTimewindowConfig(newTimewindow) {
        this.timeWindowConfig = newTimewindow;
    }

    onResetTimewindow() {
        if (this.useDashboardTimewindow) {
            this.ctx.dashboardTimewindowApi.onResetTimewindow();
        } else {
            if (this.originalTimewindow) {
                this.stopWatchingTimewindow();
                this.timeWindowConfig = angular.copy(this.originalTimewindow);
                this.originalTimewindow = null;
                this.callbacks.timeWindowUpdated(this, this.timeWindowConfig);
                this.unsubscribe();
                this.subscribe();
                this.startWatchingTimewindow();
            }
        }
    }

    onUpdateTimewindow(startTimeMs, endTimeMs) {
        if (this.useDashboardTimewindow) {
            this.ctx.dashboardTimewindowApi.onUpdateTimewindow(startTimeMs, endTimeMs);
        } else {
            this.stopWatchingTimewindow();
            if (!this.originalTimewindow) {
                this.originalTimewindow = angular.copy(this.timeWindowConfig);
            }
            this.timeWindowConfig = this.ctx.timeService.toHistoryTimewindow(this.timeWindowConfig, startTimeMs, endTimeMs);
            this.callbacks.timeWindowUpdated(this, this.timeWindowConfig);
            this.unsubscribe();
            this.subscribe();
            this.startWatchingTimewindow();
        }
    }

    notifyDataLoading() {
        this.loadingData = true;
        this.callbacks.dataLoading(this);
    }

    notifyDataLoaded() {
        this.loadingData = false;
        this.callbacks.dataLoading(this);
    }

    updateTimewindow() {
        this.timeWindow.interval = this.subscriptionTimewindow.aggregation.interval || 1000;
        if (this.subscriptionTimewindow.realtimeWindowMs) {
            this.timeWindow.maxTime = (new Date).getTime() + this.timeWindow.stDiff;
            this.timeWindow.minTime = this.timeWindow.maxTime - this.subscriptionTimewindow.realtimeWindowMs;
        } else if (this.subscriptionTimewindow.fixedWindow) {
            this.timeWindow.maxTime = this.subscriptionTimewindow.fixedWindow.endTimeMs;
            this.timeWindow.minTime = this.subscriptionTimewindow.fixedWindow.startTimeMs;
        }
    }

    updateRealtimeSubscription(subscriptionTimewindow) {
        if (subscriptionTimewindow) {
            this.subscriptionTimewindow = subscriptionTimewindow;
        } else {
            this.subscriptionTimewindow =
                this.ctx.timeService.createSubscriptionTimewindow(
                    this.timeWindowConfig,
                    this.timeWindow.stDiff);
        }
        this.updateTimewindow();
        return this.subscriptionTimewindow;
    }

    dataUpdated(sourceData, datasourceIndex, dataKeyIndex, apply) {
        this.notifyDataLoaded();
        var update = true;
        var currentData;
        if (this.displayLegend && this.legendData.data[datasourceIndex + dataKeyIndex].hidden) {
            currentData = this.hiddenData[datasourceIndex + dataKeyIndex];
        } else {
            currentData = this.data[datasourceIndex + dataKeyIndex];
        }
        if (this.type === this.ctx.types.widgetType.latest.value) {
            var prevData = currentData.data;
            if (prevData && prevData[0] && prevData[0].length > 1 && sourceData.data.length > 0) {
                var prevValue = prevData[0][1];
                if (prevValue === sourceData.data[0][1]) {
                    update = false;
                }
            }
        }
        if (update) {
            if (this.subscriptionTimewindow && this.subscriptionTimewindow.realtimeWindowMs) {
                this.updateTimewindow();
            }
            currentData.data = sourceData.data;
            if (this.caulculateLegendData) {
                this.updateLegend(datasourceIndex + dataKeyIndex, sourceData.data, apply);
            }
            this.onDataUpdated(apply);
        }
    }

    updateLegend(dataIndex, data, apply) {
        var legendKeyData = this.legendData.data[dataIndex];
        if (this.legendConfig.showMin) {
            legendKeyData.min = this.ctx.widgetUtils.formatValue(calculateMin(data), this.decimals, this.units);
        }
        if (this.legendConfig.showMax) {
            legendKeyData.max = this.ctx.widgetUtils.formatValue(calculateMax(data), this.decimals, this.units);
        }
        if (this.legendConfig.showAvg) {
            legendKeyData.avg = this.ctx.widgetUtils.formatValue(calculateAvg(data), this.decimals, this.units);
        }
        if (this.legendConfig.showTotal) {
            legendKeyData.total = this.ctx.widgetUtils.formatValue(calculateTotal(data), this.decimals, this.units);
        }
        this.callbacks.legendDataUpdated(this, apply !== false);
    }

    subscribe() {
        if (this.type === this.ctx.types.widgetType.rpc.value) {
            return;
        }
        this.notifyDataLoading();
        if (this.type === this.ctx.types.widgetType.timeseries.value && this.timeWindowConfig) {
            this.updateRealtimeSubscription();
            if (this.subscriptionTimewindow.fixedWindow) {
                this.onDataUpdated();
            }
        }
        var index = 0;
        for (var i = 0; i < this.datasources.length; i++) {
            var datasource = this.datasources[i];
            if (angular.isFunction(datasource))
                continue;
            var deviceId = null;
            if (datasource.type === this.ctx.types.datasourceType.device) {
                var aliasName = null;
                var deviceName = null;
                if (datasource.deviceId) {
                    deviceId = datasource.deviceId;
                    datasource.name = datasource.deviceName;
                    aliasName = datasource.deviceName;
                    deviceName = datasource.deviceName;
                } else if (datasource.deviceAliasId && this.ctx.aliasesInfo.deviceAliases[datasource.deviceAliasId]) {
                    deviceId = this.ctx.aliasesInfo.deviceAliases[datasource.deviceAliasId].deviceId;
                    datasource.name = this.ctx.aliasesInfo.deviceAliases[datasource.deviceAliasId].alias;
                    aliasName = this.ctx.aliasesInfo.deviceAliases[datasource.deviceAliasId].alias;
                    deviceName = '';
                    var devicesInfo = this.ctx.aliasesInfo.deviceAliasesInfo[datasource.deviceAliasId];
                    for (var d = 0; d < devicesInfo.length; d++) {
                        if (devicesInfo[d].id === deviceId) {
                            deviceName = devicesInfo[d].name;
                            break;
                        }
                    }
                }
            } else {
                datasource.name = datasource.name || this.ctx.types.datasourceType.function;
            }
            for (var dk = 0; dk < datasource.dataKeys.length; dk++) {
                updateDataKeyLabel(datasource.dataKeys[dk], datasource.name, deviceName, aliasName);
            }

            var subscription = this;

            var listener = {
                subscriptionType: this.type,
                subscriptionTimewindow: this.subscriptionTimewindow,
                datasource: datasource,
                deviceId: deviceId,
                dataUpdated: function (data, datasourceIndex, dataKeyIndex, apply) {
                    subscription.dataUpdated(data, datasourceIndex, dataKeyIndex, apply);
                },
                updateRealtimeSubscription: function () {
                    this.subscriptionTimewindow = subscription.updateRealtimeSubscription();
                    return this.subscriptionTimewindow;
                },
                setRealtimeSubscription: function (subscriptionTimewindow) {
                    subscription.updateRealtimeSubscription(angular.copy(subscriptionTimewindow));
                },
                datasourceIndex: index
            };

            for (var a = 0; a < datasource.dataKeys.length; a++) {
                this.data[index + a].data = [];
            }

            index += datasource.dataKeys.length;

            this.datasourceListeners.push(listener);
            this.ctx.datasourceService.subscribeToDatasource(listener);
        }
    }

    unsubscribe() {
        if (this.type !== this.ctx.types.widgetType.rpc.value) {
            for (var i = 0; i < this.datasourceListeners.length; i++) {
                var listener = this.datasourceListeners[i];
                this.ctx.datasourceService.unsubscribeFromDatasource(listener);
            }
            this.datasourceListeners = [];
        }
    }

    checkSubscriptions() {
        var subscriptionsChanged = false;
        for (var i = 0; i < this.datasourceListeners.length; i++) {
            var listener = this.datasourceListeners[i];
            var deviceId = null;
            var aliasName = null;
            if (listener.datasource.type === this.ctx.types.datasourceType.device) {
                if (listener.datasource.deviceAliasId &&
                    this.ctx.aliasesInfo.deviceAliases[listener.datasource.deviceAliasId]) {
                    deviceId = this.ctx.aliasesInfo.deviceAliases[listener.datasource.deviceAliasId].deviceId;
                    aliasName = this.ctx.aliasesInfo.deviceAliases[listener.datasource.deviceAliasId].alias;
                }
                if (!angular.equals(deviceId, listener.deviceId) ||
                    !angular.equals(aliasName, listener.datasource.name)) {
                    subscriptionsChanged = true;
                    break;
                }
            }
        }
        if (subscriptionsChanged) {
            this.unsubscribe();
            this.subscribe();
        }
    }

    destroy() {
        this.unsubscribe();
        for (var cafId in this.cafs) {
            if (this.cafs[cafId]) {
                this.cafs[cafId]();
                this.cafs[cafId] = null;
            }
        }
        this.registrations.forEach(function (registration) {
            registration();
        });
        this.registrations = [];
    }

}

const varsRegex = /\$\{([^\}]*)\}/g;

function updateDataKeyLabel(dataKey, dsName, deviceName, aliasName) {
    var pattern = dataKey.pattern;
    var label = dataKey.pattern;
    var match = varsRegex.exec(pattern);
    while (match !== null) {
        var variable = match[0];
        var variableName = match[1];
        if (variableName === 'dsName') {
            label = label.split(variable).join(dsName);
        } else if (variableName === 'deviceName') {
            label = label.split(variable).join(deviceName);
        } else if (variableName === 'aliasName') {
            label = label.split(variable).join(aliasName);
        }
        match = varsRegex.exec(pattern);
    }
    dataKey.label = label;
}

function calculateMin(data) {
    if (data.length > 0) {
        var result = Number(data[0][1]);
        for (var i=1;i<data.length;i++) {
            result = Math.min(result, Number(data[i][1]));
        }
        return result;
    } else {
        return null;
    }
}

function calculateMax(data) {
    if (data.length > 0) {
        var result = Number(data[0][1]);
        for (var i=1;i<data.length;i++) {
            result = Math.max(result, Number(data[i][1]));
        }
        return result;
    } else {
        return null;
    }
}

function calculateAvg(data) {
    if (data.length > 0) {
        return calculateTotal(data)/data.length;
    } else {
        return null;
    }
}

function calculateTotal(data) {
    if (data.length > 0) {
        var result = 0;
        for (var i = 0; i < data.length; i++) {
            result += Number(data[i][1]);
        }
        return result;
    } else {
        return null;
    }
}
