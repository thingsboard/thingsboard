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
import thingsboardApiDevice from './device.service';
import thingsboardApiTelemetryWebsocket from './telemetry-websocket.service';
import thingsboardTypes from '../common/types.constant';
import thingsboardUtils from '../common/utils.service';
import DataAggregator from './data-aggregator';

export default angular.module('thingsboard.api.datasource', [thingsboardApiDevice, thingsboardApiTelemetryWebsocket, thingsboardTypes, thingsboardUtils])
    .factory('datasourceService', DatasourceService)
    .name;

/*@ngInject*/
function DatasourceService($timeout, $filter, $log, telemetryWebsocketService, types, utils) {

    var subscriptions = {};

    var service = {
        subscribeToDatasource: subscribeToDatasource,
        unsubscribeFromDatasource: unsubscribeFromDatasource
    }

    return service;


    function subscribeToDatasource(listener) {
        var datasource = listener.datasource;

        if (datasource.type === types.datasourceType.device && !listener.deviceId) {
            return;
        }

        var subscriptionDataKeys = [];
        for (var d in datasource.dataKeys) {
            var dataKey = datasource.dataKeys[d];
            var subscriptionDataKey = {
                name: dataKey.name,
                type: dataKey.type,
                funcBody: dataKey.funcBody,
                postFuncBody: dataKey.postFuncBody
            }
            subscriptionDataKeys.push(subscriptionDataKey);
        }

        var datasourceSubscription = {
            datasourceType: datasource.type,
            dataKeys: subscriptionDataKeys,
            type: listener.widget.type
        };

        if (listener.widget.type === types.widgetType.timeseries.value) {
            datasourceSubscription.subscriptionTimewindow = angular.copy(listener.subscriptionTimewindow);
        }
        if (datasourceSubscription.datasourceType === types.datasourceType.device) {
            datasourceSubscription.deviceId = listener.deviceId;
        }

        listener.datasourceSubscriptionKey = utils.objectHashCode(datasourceSubscription);
        var subscription;
        if (subscriptions[listener.datasourceSubscriptionKey]) {
            subscription = subscriptions[listener.datasourceSubscriptionKey];
            subscription.syncListener(listener);
        } else {
            subscription = new DatasourceSubscription(datasourceSubscription, telemetryWebsocketService, $timeout, $filter, $log, types, utils);
            subscriptions[listener.datasourceSubscriptionKey] = subscription;
            subscription.start();
        }
        subscription.addListener(listener);
    }

    function unsubscribeFromDatasource(listener) {
        if (listener.datasourceSubscriptionKey) {
            if (subscriptions[listener.datasourceSubscriptionKey]) {
                var subscription = subscriptions[listener.datasourceSubscriptionKey];
                subscription.removeListener(listener);
                if (!subscription.hasListeners()) {
                    subscription.unsubscribe();
                    delete subscriptions[listener.datasourceSubscriptionKey];
                }
            }
            listener.datasourceSubscriptionKey = null;
        }
    }

}

function DatasourceSubscription(datasourceSubscription, telemetryWebsocketService, $timeout, $filter, $log, types, utils) {

    var listeners = [];
    var datasourceType = datasourceSubscription.datasourceType;
    var datasourceData = {};
    var dataKeys = {};
    var subscribers = {};
    var history = datasourceSubscription.subscriptionTimewindow &&
        datasourceSubscription.subscriptionTimewindow.fixedWindow;
    var realtime = datasourceSubscription.subscriptionTimewindow &&
        datasourceSubscription.subscriptionTimewindow.realtimeWindowMs;
    var dataGenFunction = null;
    var timer;
    var frequency;

    var subscription = {
        addListener: addListener,
        hasListeners: hasListeners,
        removeListener: removeListener,
        syncListener: syncListener,
        start: start,
        unsubscribe: unsubscribe
    }

    initializeSubscription();

    return subscription;

    function initializeSubscription() {
        for (var i = 0; i < datasourceSubscription.dataKeys.length; i++) {
            var dataKey = angular.copy(datasourceSubscription.dataKeys[i]);
            dataKey.index = i;
            var key;
            if (datasourceType === types.datasourceType.function) {
                key = utils.objectHashCode(dataKey);
                if (!dataKey.func) {
                    dataKey.func = new Function("time", "prevValue", dataKey.funcBody);
                }
                datasourceData[key] = {
                    data: []
                };
                dataKeys[key] = dataKey;
            } else if (datasourceType === types.datasourceType.device) {
                key = dataKey.name + '_' + dataKey.type;
                if (dataKey.postFuncBody && !dataKey.postFunc) {
                    dataKey.postFunc = new Function("time", "value", "prevValue", dataKey.postFuncBody);
                }
                var dataKeysList = dataKeys[key];
                if (!dataKeysList) {
                    dataKeysList = [];
                    dataKeys[key] = dataKeysList;
                }
                var index = dataKeysList.push(dataKey) - 1;
                datasourceData[key + '_' + index] = {
                    data: []
                };
            }
            dataKey.key = key;
        }
        if (datasourceType === types.datasourceType.function) {
            frequency = 1000;
            if (datasourceSubscription.type === types.widgetType.timeseries.value) {
                dataGenFunction = generateSeries;
                var window;
                if (realtime) {
                    window = datasourceSubscription.subscriptionTimewindow.realtimeWindowMs;
                } else {
                    window = datasourceSubscription.subscriptionTimewindow.fixedWindow.endTimeMs -
                        datasourceSubscription.subscriptionTimewindow.fixedWindow.startTimeMs;
                }
                frequency = window / 1000 * 20;
            } else if (datasourceSubscription.type === types.widgetType.latest.value) {
                dataGenFunction = generateLatest;
                frequency = 1000;
            }
        }
    }

    function addListener(listener) {
        listeners.push(listener);
        if (history) {
            start();
        }
    }

    function hasListeners() {
        return listeners.length > 0;
    }

    function removeListener(listener) {
        listeners.splice(listeners.indexOf(listener), 1);
    }

    function syncListener(listener) {
        var key;
        var dataKey;
        if (datasourceType === types.datasourceType.function) {
            for (key in dataKeys) {
                dataKey = dataKeys[key];
                listener.dataUpdated(datasourceData[key],
                    listener.datasourceIndex,
                    dataKey.index);
            }
        } else if (datasourceType === types.datasourceType.device) {
            for (key in dataKeys) {
                var dataKeysList = dataKeys[key];
                for (var i = 0; i < dataKeysList.length; i++) {
                    dataKey = dataKeysList[i];
                    var datasourceKey = key + '_' + i;
                    listener.dataUpdated(datasourceData[datasourceKey],
                        listener.datasourceIndex,
                        dataKey.index);
                }
            }
        }
    }

    function start() {
        if (history && !hasListeners()) {
            return;
        }
        //$log.debug("started!");
        if (datasourceType === types.datasourceType.device) {

            //send subscribe command

            var tsKeys = '';
            var attrKeys = '';

            for (var key in dataKeys) {
                var dataKeysList = dataKeys[key];
                var dataKey = dataKeysList[0];
                if (dataKey.type === types.dataKeyType.timeseries) {
                    if (tsKeys.length > 0) {
                        tsKeys += ',';
                    }
                    tsKeys += dataKey.name;
                } else if (dataKey.type === types.dataKeyType.attribute) {
                    if (attrKeys.length > 0) {
                        attrKeys += ',';
                    }
                    attrKeys += dataKey.name;
                }
            }

            if (tsKeys.length > 0) {

                var subscriber;
                var subscriptionCommand;

                if (history) {

                    var historyCommand = {
                        deviceId: datasourceSubscription.deviceId,
                        keys: tsKeys,
                        startTs: datasourceSubscription.subscriptionTimewindow.fixedWindow.startTimeMs,
                        endTs: datasourceSubscription.subscriptionTimewindow.fixedWindow.endTimeMs,
                        limit: datasourceSubscription.subscriptionTimewindow.aggregation.limit,
                        agg: datasourceSubscription.subscriptionTimewindow.aggregation.type
                    };

                    subscriber = {
                        historyCommand: historyCommand,
                        type: types.dataKeyType.timeseries,
                        onData: function (data) {
                            if (data.data) {
                                onData(data.data, types.dataKeyType.timeseries);
                            }
                        },
                        onReconnected: function() {
                            onReconnected();
                        }
                    };

                    telemetryWebsocketService.subscribe(subscriber);
                    subscribers[subscriber.historyCommand.cmdId] = subscriber;

                } else {

                    subscriptionCommand = {
                        deviceId: datasourceSubscription.deviceId,
                        keys: tsKeys
                    };

                    subscriber = {
                        subscriptionCommand: subscriptionCommand,
                        type: types.dataKeyType.timeseries
                    };

                    if (datasourceSubscription.type === types.widgetType.timeseries.value) {
                        subscriptionCommand.timeWindow = datasourceSubscription.subscriptionTimewindow.realtimeWindowMs;
                        subscriptionCommand.limit = datasourceSubscription.subscriptionTimewindow.aggregation.limit;
                        subscriptionCommand.agg = datasourceSubscription.subscriptionTimewindow.aggregation.type;
                        var dataAggregator = new DataAggregator(
                            function(data, startTs, endTs) {
                                onData(data, types.dataKeyType.timeseries, startTs, endTs);
                            },
                            subscriptionCommand.limit,
                            subscriptionCommand.agg,
                            subscriptionCommand.timeWindow,
                            types,
                            $timeout,
                            $filter
                        );
                        subscriber.onData = function(data) {
                            dataAggregator.onData(data);
                        }
                        subscriber.onReconnected = function() {
                            dataAggregator.reset();
                            onReconnected();
                        }
                        subscriber.onDestroy = function() {
                            dataAggregator.destroy();
                        }
                    } else {
                        subscriber.onReconnected = function() {
                            onReconnected();
                        }
                        subscriber.onData = function(data) {
                            if (data.data) {
                                onData(data.data, types.dataKeyType.timeseries);
                            }
                        }
                    }

                    telemetryWebsocketService.subscribe(subscriber);
                    subscribers[subscriber.subscriptionCommand.cmdId] = subscriber;

                }
            }

            if (attrKeys.length > 0) {

                subscriptionCommand = {
                    deviceId: datasourceSubscription.deviceId,
                    keys: attrKeys
                };

                subscriber = {
                    subscriptionCommand: subscriptionCommand,
                    type: types.dataKeyType.attribute,
                    onData: function (data) {
                        if (data.data) {
                            onData(data.data, types.dataKeyType.attribute);
                        }
                    },
                    onReconnected: function() {
                        onReconnected();
                    }
                };

                telemetryWebsocketService.subscribe(subscriber);
                subscribers[subscriber.cmdId] = subscriber;

            }

        } else if (dataGenFunction) {
            if (history) {
                onTick();
            } else {
                timer = $timeout(onTick, 0, false);
            }
        }

    }

    function unsubscribe() {
        if (timer) {
            $timeout.cancel(timer);
        }
        if (datasourceType === types.datasourceType.device) {
            for (var cmdId in subscribers) {
                var subscriber = subscribers[cmdId];
                telemetryWebsocketService.unsubscribe(subscriber);
                if (subscriber.onDestroy) {
                    subscriber.onDestroy();
                }
            }
            subscribers = {};
        }
    }

    function boundToInterval(data, timewindowMs) {
        if (data.length > 1) {
            var start = data[0][0];
            var end = data[data.length - 1][0];
            var i = 0;
            var currentInterval = end - start;
            while (currentInterval > timewindowMs && i < data.length - 2) {
                i++;
                start = data[i][0];
                currentInterval = end - start;
            }
            if (i > 1) {
                data.splice(0, i - 1);
            }
        }
        return data;
    }

    function generateSeries(dataKey, startTime, endTime) {
        var data = [];
        var prevSeries;
        var datasourceKeyData = datasourceData[dataKey.key].data;
        if (datasourceKeyData.length > 0) {
            prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
        } else {
            prevSeries = [0, 0];
        }
        for (var time = startTime; time <= endTime; time += frequency) {
            var series = [];
            series.push(time);
            var value = dataKey.func(time, prevSeries[1]);
            series.push(value);
            data.push(series);
            prevSeries = series;
        }
        if (data.length > 0) {
            dataKey.lastUpdateTime = data[data.length - 1][0];
        }
        if (realtime) {
            datasourceData[dataKey.key].data = boundToInterval(datasourceKeyData.concat(data),
                datasourceSubscription.subscriptionTimewindow.realtimeWindowMs);
        } else {
            datasourceData[dataKey.key].data = data;
        }
        for (var i in listeners) {
            var listener = listeners[i];
            listener.dataUpdated(datasourceData[dataKey.key],
                listener.datasourceIndex,
                dataKey.index);
        }
    }

    function generateLatest(dataKey) {
        var prevSeries;
        var datasourceKeyData = datasourceData[dataKey.key].data;
        if (datasourceKeyData.length > 0) {
            prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
        } else {
            prevSeries = [0, 0];
        }
        var series = [];
        var time = (new Date).getTime();
        series.push(time);
        var value = dataKey.func(time, prevSeries[1]);
        series.push(value);
        datasourceData[dataKey.key].data = [series];
        for (var i in listeners) {
            var listener = listeners[i];
            listener.dataUpdated(datasourceData[dataKey.key],
                listener.datasourceIndex,
                dataKey.index);
        }
    }

    function onTick() {
        var key;
        if (datasourceSubscription.type === types.widgetType.timeseries.value) {
            var startTime;
            var endTime;
            for (key in dataKeys) {
                var dataKey = dataKeys[key];
                if (!startTime) {
                    if (realtime) {
                        endTime = (new Date).getTime();
                        if (dataKey.lastUpdateTime) {
                            startTime = dataKey.lastUpdateTime + frequency;
                        } else {
                            startTime = endTime - datasourceSubscription.subscriptionTimewindow.realtimeWindowMs;
                        }
                    } else {
                        startTime = datasourceSubscription.subscriptionTimewindow.fixedWindow.startTimeMs;
                        endTime = datasourceSubscription.subscriptionTimewindow.fixedWindow.endTimeMs;
                    }
                }
                generateSeries(dataKey, startTime, endTime);
            }
        } else if (datasourceSubscription.type === types.widgetType.latest.value) {
            for (key in dataKeys) {
                generateLatest(dataKeys[key]);
            }
        }

        if (!history) {
            timer = $timeout(onTick, frequency / 2, false);
        }
    }

    function onReconnected() {
        if (datasourceType === types.datasourceType.device) {
            for (var key in dataKeys) {
                var dataKeysList = dataKeys[key];
                for (var i = 0; i < dataKeysList.length; i++) {
                    var dataKey = dataKeysList[i];
                    var datasourceKey = key + '_' + i;
                    datasourceData[datasourceKey] = {
                        data: []
                    };
                    for (var l in listeners) {
                        var listener = listeners[l];
                        listener.dataUpdated(datasourceData[datasourceKey],
                            listener.datasourceIndex,
                            dataKey.index);
                    }
                }
            }
        }
    }

    function isNumeric(val) {
        return (val - parseFloat( val ) + 1) >= 0;
    }

    function convertValue(val) {
        if (val && isNumeric(val)) {
            return Number(val);
        } else {
            return val;
        }
    }

    function onData(sourceData, type, startTs, endTs) {
        for (var keyName in sourceData) {
            var keyData = sourceData[keyName];
            var key = keyName + '_' + type;
            var dataKeyList = dataKeys[key];
            for (var keyIndex = 0; keyIndex < dataKeyList.length; keyIndex++) {
                var datasourceKey = key + "_" + keyIndex;
                if (datasourceData[datasourceKey].data) {
                    var dataKey = dataKeyList[keyIndex];
                    var data = [];
                    var prevSeries;
                    var datasourceKeyData;
                    if (realtime) {
                        datasourceKeyData = [];
                    } else {
                        datasourceKeyData = datasourceData[datasourceKey].data;
                    }
                    if (datasourceKeyData.length > 0) {
                        prevSeries = datasourceKeyData[datasourceKeyData.length - 1];
                    } else {
                        prevSeries = [0, 0];
                    }
                    if (datasourceSubscription.type === types.widgetType.timeseries.value) {
                        var series, time, value;
                        for (var i in keyData) {
                            series = keyData[i];
                            time = series[0];
                            value = convertValue(series[1]);
                            if (dataKey.postFunc) {
                                value = dataKey.postFunc(time, value, prevSeries[1]);
                            }
                            series = [time, value];
                            data.push(series);
                            prevSeries = series;
                        }
                    } else if (datasourceSubscription.type === types.widgetType.latest.value) {
                        if (keyData.length > 0) {
                            series = keyData[0];
                            time = series[0];
                            value = series[1];
                            if (dataKey.postFunc) {
                                value = dataKey.postFunc(time, value, prevSeries[1]);
                            }
                            series = [time, value];
                            data.push(series);
                        }
                    }
                    if (data.length > 0 || (startTs && endTs)) {
                        datasourceData[datasourceKey].data = data;
                        datasourceData[datasourceKey].startTs = startTs;
                        datasourceData[datasourceKey].endTs = endTs;
                        for (var i2 in listeners) {
                            var listener = listeners[i2];
                            listener.dataUpdated(datasourceData[datasourceKey],
                                listener.datasourceIndex,
                                dataKey.index);
                        }
                    }
                }
            }
        }
    }
}

