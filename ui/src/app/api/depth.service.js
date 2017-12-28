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
export default angular.module('thingsboard.api.depth', [])
    .factory('depthService', DepthService)
    .name;

const DECI_FT = 10;
/*const SECOND = 1000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;*/
const AVG_LIMIT = 200;
const MAX_LIMIT = 500;
const MIN_LIMIT = 10;
const MIN_INTEVAL = 10;
const MAX_INTEVAL = 30;
var startDpt = 3010;

/*@ngInject*/
function DepthService($translate, types, $log) {

    var service = {
        minIntervalLimit: minIntervalLimit,
        maxIntervalLimit: maxIntervalLimit,
        /*boundMinInterval: boundMinInterval,
        boundMaxInterval: boundMaxInterval,
        getIntervals: getIntervals,
        matchesExistingInterval: matchesExistingInterval,
        boundToPredefinedInterval: boundToPredefinedInterval,*/
        defaultDepthwindow: defaultDepthwindow,
        toHistoryDepthwindow: toHistoryDepthwindow,
        createSubscriptionDepthwindow: createSubscriptionDepthwindow,
        avgAggregationLimit: function () {
            return AVG_LIMIT;
        }
    }

    return service;

    /*function minIntervalLimit(depthwindow) {
        var min = depthwindow / MAX_LIMIT;
        return boundMinInterval(min);
    }

    function avgInterval(depthwindow) {
        var avg = depthwindow / AVG_LIMIT;
        return boundMinInterval(avg);
    }

    function maxIntervalLimit(depthwindow) {
        var max = depthwindow / MIN_LIMIT;
        return boundMaxInterval(max);
    }

    function boundMinInterval(min) {
        return toBound(min, MIN_INTERVAL, MAX_INTERVAL, MIN_INTERVAL);
    }

    function boundMaxInterval(max) {
        return toBound(max, MIN_INTERVAL, MAX_INTERVAL, MAX_INTERVAL);
    }

    function toBound(value, min, max, defValue) {
        if (angular.isDefined(value)) {
            value = Math.max(value, min);
            value = Math.min(value, max);
            return value;
        } else {
            return defValue;
        }
    }

    function getIntervals(min, max) {
        min = boundMinInterval(min);
        max = boundMaxInterval(max);
        var intervals = [];
        for (var i in predefIntervals) {
            var interval = predefIntervals[i];
            if (interval.value >= min && interval.value <= max) {
                intervals.push(interval);
            }
        }
        return intervals;
    }

    function matchesExistingInterval(min, max, intervalMs) {
        var intervals = getIntervals(min, max);
        for (var i in intervals) {
            var interval = intervals[i];
            if (intervalMs === interval.value) {
                return true;
            }
        }
        return false;
    }

    function boundToPredefinedInterval(min, max, intervalMs) {
        var intervals = getIntervals(min, max);
        var minDelta = MAX_INTERVAL;
        var boundedInterval = intervalMs || min;
        var matchedInterval;
        for (var i in intervals) {
            var interval = intervals[i];
            var delta = Math.abs(interval.value - boundedInterval);
            if (delta < minDelta) {
                matchedInterval = interval;
                minDelta = delta;
            }
        }
        boundedInterval = matchedInterval.value;
        return boundedInterval;
    }*/

    function defaultDepthwindow() {
        //var currentTime = (new Date).getTime();
        var depthwindow = {
            displayValue: "",
            selectedTab: 0,
            realtime: {
                interval: DECI_FT,
                depthwindowFt: 100 // 1 min by default
            },
            history: {
                historyType: 0,
                interval: DECI_FT,
                depthwindowFt: 100, // 1 min by default
                fixedDepthwindow: {
                    startDepthFt: 0, //currentTime - DAY, // 1 day by default
                    endDepthFt: 100 //currentTime
                }
            },
            aggregation: {
                type: types.aggregation.avg.value,
                limit: AVG_LIMIT
            }
        }
        return depthwindow;
    }

    function toHistoryDepthwindow(depthwindow, startDepthFt, endDepthFt) {

        var interval = 0;
        if (depthwindow.history) {
            interval = depthwindow.history.interval;
        } else if (depthwindow.realtime) {
            interval = depthwindow.realtime.interval;
        }

        var aggType;
        if (depthwindow.aggregation) {
            aggType = depthwindow.aggregation.type || types.aggregation.avg.value;
        } else {
            aggType = types.aggregation.avg.value;
        }

        var historyDepthwindow = {
            history: {
                fixedDepthwindow: {
                    startDepthFt: startDepthFt,
                    endDepthFt: endDepthFt
                },
                interval: boundIntervalToDepthwindow(endDepthFt - startDepthFt, interval, aggType)
            },
            aggregation: {
                type: aggType
            }
        }

        return historyDepthwindow;
    }

    function createSubscriptionDepthwindow(depthwindow, stDiff, stateData) {

        var subscriptionDepthwindow = {
            fixedWindow: null,
            realtimeWindowFt: null,
            aggregation: {
                interval: DECI_FT,
                limit: AVG_LIMIT,
                type: types.aggregation.avg.value
            }
        };
        var aggDepthwindow = 0;
        if (stateData) {
            subscriptionDepthwindow.aggregation = {
                interval: DECI_FT,
                limit: MAX_LIMIT,
                type: types.aggregation.none.value,
                stateData: true
            };
        } else {
            subscriptionDepthwindow.aggregation = {
                interval: DECI_FT,
                limit: AVG_LIMIT,
                type: types.aggregation.avg.value
            };
        }

        if (angular.isDefined(depthwindow.aggregation) && !stateData) {
            subscriptionDepthwindow.aggregation = {
                type: depthwindow.aggregation.type || types.aggregation.avg.value,
                limit: depthwindow.aggregation.limit || AVG_LIMIT
            };
        }
        if (angular.isDefined(depthwindow.realtime)) {
            subscriptionDepthwindow.realtimeWindowFt = depthwindow.realtime.depthwindowFt;
            subscriptionDepthwindow.aggregation.interval =
                boundIntervalToDepthwindow(subscriptionDepthwindow.realtimeWindowFt, depthwindow.realtime.interval,
                    subscriptionDepthwindow.aggregation.type);
            subscriptionDepthwindow.startDs = /*(new Date).getTime()*/ startDpt + stDiff - subscriptionDepthwindow.realtimeWindowFt;
            $log.log("realtime window ft " + subscriptionDepthwindow.realtimeWindowFt);
            startDpt = startDpt + subscriptionDepthwindow.realtimeWindowFt;
            $log.log("StartDpt " + startDpt);
            // We need to have a different value at new Date
            var startDiff = subscriptionDepthwindow.startDs % subscriptionDepthwindow.aggregation.interval;
            $log.log("StartDiff " + startDiff);
            aggDepthwindow = subscriptionDepthwindow.realtimeWindowFt;
            if (startDiff) {
                subscriptionDepthwindow.startDs -= startDiff;
                aggDepthwindow += subscriptionDepthwindow.aggregation.interval;
            }
        } else if (angular.isDefined(depthwindow.history)) {
            if (angular.isDefined(depthwindow.history.depthwindowFt)) {
                //var currentTime = (new Date).getTime();
                subscriptionDepthwindow.fixedWindow = {
                    startDepthFt: /*currentTime*/ startDpt - depthwindow.history.depthwindowFt,
                    endDepthFt: startDpt //currentTime
                }
                startDpt = startDpt + depthwindow.history.depthwindowFt;
                aggDepthwindow = depthwindow.history.depthwindowFt;

            } else {
                subscriptionDepthwindow.fixedWindow = {
                    startDepthFt: depthwindow.history.fixedDepthwindow.startDepthFt,
                    endDepthFt: depthwindow.history.fixedDepthwindow.endDepthFt
                }
                aggDepthwindow = subscriptionDepthwindow.fixedWindow.endDepthFt - subscriptionDepthwindow.fixedWindow.startDepthFt;
            }
            subscriptionDepthwindow.startDs = subscriptionDepthwindow.fixedWindow.startDepthFt;
            subscriptionDepthwindow.aggregation.interval =
                boundIntervalToDepthwindow(aggDepthwindow, depthwindow.history.interval, subscriptionDepthwindow.aggregation.type);
        }
        var aggregation = subscriptionDepthwindow.aggregation;
        aggregation.depthWindow = aggDepthwindow;
        if (aggregation.type !== types.aggregation.none.value) {
            aggregation.limit = Math.ceil(aggDepthwindow / subscriptionDepthwindow.aggregation.interval);
        }
        return subscriptionDepthwindow;
    }

    function minIntervalLimit(depthwindow) {
        depthwindow / MAX_LIMIT;
        return MIN_INTEVAL;
    }

    function maxIntervalLimit(depthwindow) {
        depthwindow / MIN_LIMIT;
        return MAX_INTEVAL;
    }


    function boundIntervalToDepthwindow(depthwindow, intervalFt, aggType) {
        if (aggType === types.aggregation.none.value) {
            return DECI_FT;
        }
    }


}