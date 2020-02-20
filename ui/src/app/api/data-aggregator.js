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
export default class DataAggregator {

    constructor(onDataCb, tsKeyNames, startTs, limit, aggregationType, timeWindow, interval,
                stateData, types, $timeout, $filter) {
        this.onDataCb = onDataCb;
        this.tsKeyNames = tsKeyNames;
        this.dataBuffer = {};
        for (var k = 0; k < tsKeyNames.length; k++) {
            this.dataBuffer[tsKeyNames[k]] = [];
        }
        this.startTs = startTs;
        this.aggregationType = aggregationType;
        this.types = types;
        this.$timeout = $timeout;
        this.$filter = $filter;
        this.dataReceived = false;
        this.resetPending = false;
        this.noAggregation = aggregationType === types.aggregation.none.value;
        this.limit = limit;
        this.timeWindow = timeWindow;
        this.interval = interval;
        this.stateData = stateData;
        if (this.stateData) {
            this.lastPrevKvPairData = {};
        }
        this.aggregationTimeout = Math.max(this.interval, 1000);
        switch (aggregationType) {
            case types.aggregation.min.value:
                this.aggFunction = min;
                break;
            case types.aggregation.max.value:
                this.aggFunction = max;
                break;
            case types.aggregation.avg.value:
                this.aggFunction = avg;
                break;
            case types.aggregation.sum.value:
                this.aggFunction = sum;
                break;
            case types.aggregation.count.value:
                this.aggFunction = count;
                break;
            case types.aggregation.none.value:
                this.aggFunction = none;
                break;
            default:
                this.aggFunction = avg;
        }
    }

    reset(startTs, timeWindow, interval) {
        if (this.intervalTimeoutHandle) {
            this.$timeout.cancel(this.intervalTimeoutHandle);
            this.intervalTimeoutHandle = null;
        }
        this.intervalScheduledTime = currentTime();
        this.startTs = startTs;
        this.timeWindow = timeWindow;
        this.interval = interval;
        this.endTs = this.startTs + this.timeWindow;
        this.elapsed = 0;
        this.aggregationTimeout = Math.max(this.interval, 1000);
        this.resetPending = true;
        var self = this;
        this.intervalTimeoutHandle = this.$timeout(function() {
            self.onInterval();
        }, this.aggregationTimeout, false);
    }

    onData(data, update, history, apply) {
        if (!this.dataReceived || this.resetPending) {
            var updateIntervalScheduledTime = true;
            if (!this.dataReceived) {
                this.elapsed = 0;
                this.dataReceived = true;
                this.endTs = this.startTs + this.timeWindow;
            }
            if (this.resetPending) {
                this.resetPending = false;
                updateIntervalScheduledTime = false;
            }
            if (update) {
                this.aggregationMap = {};
                updateAggregatedData(this.aggregationMap, this.aggregationType === this.types.aggregation.count.value,
                    this.noAggregation, this.aggFunction, data.data, this.interval, this.startTs);
            } else {
                this.aggregationMap = processAggregatedData(data.data, this.aggregationType === this.types.aggregation.count.value, this.noAggregation);
            }
            if (updateIntervalScheduledTime) {
                this.intervalScheduledTime = currentTime();
            }
            this.onInterval(history, apply);
        } else {
            updateAggregatedData(this.aggregationMap, this.aggregationType === this.types.aggregation.count.value,
                this.noAggregation, this.aggFunction, data.data, this.interval, this.startTs);
            if (history) {
                this.intervalScheduledTime = currentTime();
                this.onInterval(history, apply);
            }
        }
    }

    onInterval(history, apply) {
        var now = currentTime();
        this.elapsed += now - this.intervalScheduledTime;
        this.intervalScheduledTime = now;
        if (this.intervalTimeoutHandle) {
            this.$timeout.cancel(this.intervalTimeoutHandle);
            this.intervalTimeoutHandle = null;
        }
        if (!history) {
            var delta = Math.floor(this.elapsed / this.interval);
            if (delta || !this.data) {
                this.startTs += delta * this.interval;
                this.endTs += delta * this.interval;
                this.data = this.updateData();
                this.elapsed = this.elapsed - delta * this.interval;
            }
        } else {
            this.data = this.updateData();
        }
        if (this.onDataCb) {
            this.onDataCb(this.data, apply);
        }

        var self = this;
        if (!history) {
            this.intervalTimeoutHandle = this.$timeout(function() {
                self.onInterval();
            }, this.aggregationTimeout, false);
        }
    }

    updateData() {
        for (var k = 0; k < this.tsKeyNames.length; k++) {
            this.dataBuffer[this.tsKeyNames[k]] = [];
        }
        for (var key in this.aggregationMap) {
            var aggKeyData = this.aggregationMap[key];
            var keyData = this.dataBuffer[key];
            for (var aggTimestamp in aggKeyData) {
                if (aggTimestamp <= this.startTs) {
                    if (this.stateData &&
                        (!this.lastPrevKvPairData[key] || this.lastPrevKvPairData[key][0] < aggTimestamp)) {
                        this.lastPrevKvPairData[key] = [Number(aggTimestamp), aggKeyData[aggTimestamp].aggValue];
                    }
                    delete aggKeyData[aggTimestamp];
                } else if (aggTimestamp <= this.endTs) {
                    var aggData = aggKeyData[aggTimestamp];
                    var kvPair = [Number(aggTimestamp), aggData.aggValue];
                    keyData.push(kvPair);
                }
            }
            keyData = this.$filter('orderBy')(keyData, '+this[0]');
            if (this.stateData) {
                this.updateStateBounds(keyData, angular.copy(this.lastPrevKvPairData[key]));
            }
            if (keyData.length > this.limit) {
                keyData = keyData.slice(keyData.length - this.limit);
            }
            this.dataBuffer[key] = keyData;
        }
        return this.dataBuffer;
    }

    updateStateBounds(keyData, lastPrevKvPair) {
        if (lastPrevKvPair) {
            lastPrevKvPair[0] = this.startTs;
        }
        var firstKvPair;
        if (!keyData.length) {
            if (lastPrevKvPair) {
                firstKvPair = lastPrevKvPair;
                keyData.push(firstKvPair);
            }
        } else {
            firstKvPair = keyData[0];
        }
        if (firstKvPair && firstKvPair[0] > this.startTs) {
            if (lastPrevKvPair) {
                keyData.unshift(lastPrevKvPair);
            }
        }
        if (keyData.length) {
            var lastKvPair = keyData[keyData.length-1];
            if (lastKvPair[0] < this.endTs) {
                lastKvPair = angular.copy(lastKvPair);
                lastKvPair[0] = this.endTs;
                keyData.push(lastKvPair);
            }
        }
    }

    destroy() {
        if (this.intervalTimeoutHandle) {
            this.$timeout.cancel(this.intervalTimeoutHandle);
            this.intervalTimeoutHandle = null;
        }
        this.aggregationMap = null;
    }

}

/* eslint-disable */
function currentTime() {
    return window.performance && window.performance.now ?
        window.performance.now() : Date.now();
}
/* eslint-enable */

function processAggregatedData(data, isCount, noAggregation) {
    var aggregationMap = {};
    for (var key in data) {
        var aggKeyData = aggregationMap[key];
        if (!aggKeyData) {
            aggKeyData = {};
            aggregationMap[key] = aggKeyData;
        }
        var keyData = data[key];
        for (var i = 0; i < keyData.length; i++) {
            var kvPair = keyData[i];
            var timestamp = kvPair[0];
            var value = convertValue(kvPair[1], noAggregation);
            var aggKey = timestamp;
            var aggData = {
                count: isCount ? value : 1,
                sum: value,
                aggValue: value
            }
            aggKeyData[aggKey] = aggData;
        }
    }
    return aggregationMap;
}

function updateAggregatedData(aggregationMap, isCount, noAggregation, aggFunction, data, interval, startTs) {
    for (var key in data) {
        var aggKeyData = aggregationMap[key];
        if (!aggKeyData) {
            aggKeyData = {};
            aggregationMap[key] = aggKeyData;
        }
        var keyData = data[key];
        for (var i = 0; i < keyData.length; i++) {
            var kvPair = keyData[i];
            var timestamp = kvPair[0];
            var value = convertValue(kvPair[1], noAggregation);
            var aggTimestamp = noAggregation ? timestamp : (startTs + Math.floor((timestamp - startTs) / interval) * interval + interval/2);
            var aggData = aggKeyData[aggTimestamp];
            if (!aggData) {
                aggData = {
                    count: 1,
                    sum: value,
                    aggValue: isCount ? 1 : value
                }
                aggKeyData[aggTimestamp] = aggData;
            } else {
                aggFunction(aggData, value);
            }
        }
    }
}

function convertValue(value, noAggregation) {
    if (!noAggregation || value && isNumeric(value)) {
        return Number(value);
    } else {
        return value;
    }
}

function isNumeric(value) {
    return (value - parseFloat( value ) + 1) >= 0;
}

function avg(aggData, value) {
    aggData.count++;
    aggData.sum += value;
    aggData.aggValue = aggData.sum / aggData.count;
}

function min(aggData, value) {
    aggData.aggValue = Math.min(aggData.aggValue, value);
}

function max(aggData, value) {
    aggData.aggValue = Math.max(aggData.aggValue, value);
}

function sum(aggData, value) {
    aggData.aggValue = aggData.aggValue + value;
}

function count(aggData) {
    aggData.count++;
    aggData.aggValue = aggData.count;
}

function none(aggData, value) {
    aggData.aggValue = value;
}
