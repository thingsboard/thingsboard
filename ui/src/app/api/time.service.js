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
export default angular.module('thingsboard.api.time', [])
    .factory('timeService', TimeService)
    .name;

const SECOND = 1000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const MIN_INTERVAL = SECOND;
const MAX_INTERVAL = 365 * 20 * DAY;

const MIN_LIMIT = 7;
//const AVG_LIMIT = 200;
//const MAX_LIMIT = 500;

/*@ngInject*/
function TimeService($translate, $http, $q, types) {

    var predefIntervals;
    var maxDatapointsLimit;

    var service = {
        loadMaxDatapointsLimit: loadMaxDatapointsLimit,
        minIntervalLimit: minIntervalLimit,
        maxIntervalLimit: maxIntervalLimit,
        boundMinInterval: boundMinInterval,
        boundMaxInterval: boundMaxInterval,
        getIntervals: getIntervals,
        matchesExistingInterval: matchesExistingInterval,
        boundToPredefinedInterval: boundToPredefinedInterval,
        defaultTimewindow: defaultTimewindow,
        toHistoryTimewindow: toHistoryTimewindow,
        createSubscriptionTimewindow: createSubscriptionTimewindow,
        createTimewindowForComparison: createTimewindowForComparison,
        getMaxDatapointsLimit: function () {
            return maxDatapointsLimit;
        },
        getMinDatapointsLimit: function () {
            return MIN_LIMIT;
        }
    }

    return service;

    function loadMaxDatapointsLimit() {
        var deferred = $q.defer();
        var url = '/api/dashboard/maxDatapointsLimit';
        $http.get(url, {ignoreLoading: true}).then(function success(response) {
            maxDatapointsLimit = response.data;
            if (!maxDatapointsLimit || maxDatapointsLimit <= MIN_LIMIT) {
                maxDatapointsLimit = MIN_LIMIT + 1;
            }
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function minIntervalLimit(timewindow) {
        var min = timewindow / 500;
        return boundMinInterval(min);
    }

    function avgInterval(timewindow) {
        var avg = timewindow / 200;
        return boundMinInterval(avg);
    }

    function maxIntervalLimit(timewindow) {
        var max = timewindow / MIN_LIMIT;
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
        initPredefIntervals();
        for (var i in predefIntervals) {
            var interval = predefIntervals[i];
            if (interval.value >= min && interval.value <= max) {
                intervals.push(interval);
            }
        }
        return intervals;
    }

    function initPredefIntervals() {
        if (!predefIntervals) {
            predefIntervals = [
                {
                    name: $translate.instant('timeinterval.seconds-interval', {seconds: 1}, 'messageformat'),
                    value: 1 * SECOND
                },
                {
                    name: $translate.instant('timeinterval.seconds-interval', {seconds: 5}, 'messageformat'),
                    value: 5 * SECOND
                },
                {
                    name: $translate.instant('timeinterval.seconds-interval', {seconds: 10}, 'messageformat'),
                    value: 10 * SECOND
                },
                {
                    name: $translate.instant('timeinterval.seconds-interval', {seconds: 15}, 'messageformat'),
                    value: 15 * SECOND
                },
                {
                    name: $translate.instant('timeinterval.seconds-interval', {seconds: 30}, 'messageformat'),
                    value: 30 * SECOND
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 1}, 'messageformat'),
                    value: 1 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 2}, 'messageformat'),
                    value: 2 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 5}, 'messageformat'),
                    value: 5 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 10}, 'messageformat'),
                    value: 10 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 15}, 'messageformat'),
                    value: 15 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.minutes-interval', {minutes: 30}, 'messageformat'),
                    value: 30 * MINUTE
                },
                {
                    name: $translate.instant('timeinterval.hours-interval', {hours: 1}, 'messageformat'),
                    value: 1 * HOUR
                },
                {
                    name: $translate.instant('timeinterval.hours-interval', {hours: 2}, 'messageformat'),
                    value: 2 * HOUR
                },
                {
                    name: $translate.instant('timeinterval.hours-interval', {hours: 5}, 'messageformat'),
                    value: 5 * HOUR
                },
                {
                    name: $translate.instant('timeinterval.hours-interval', {hours: 10}, 'messageformat'),
                    value: 10 * HOUR
                },
                {
                    name: $translate.instant('timeinterval.hours-interval', {hours: 12}, 'messageformat'),
                    value: 12 * HOUR
                },
                {
                    name: $translate.instant('timeinterval.days-interval', {days: 1}, 'messageformat'),
                    value: 1 * DAY
                },
                {
                    name: $translate.instant('timeinterval.days-interval', {days: 7}, 'messageformat'),
                    value: 7 * DAY
                },
                {
                    name: $translate.instant('timeinterval.days-interval', {days: 30}, 'messageformat'),
                    value: 30 * DAY
                }
            ];
        }
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
    }

    function defaultTimewindow() {
        var currentTime = (new Date).getTime();
        var timewindow = {
            displayValue: "",
            selectedTab: 0,            
            hideInterval: false,
            hideAggregation: false,
            hideAggInterval: false,
            realtime: {
                interval: SECOND,
                timewindowMs: MINUTE // 1 min by default
            },
            history: {
                historyType: 0,
                    interval: SECOND,
                    timewindowMs: MINUTE, // 1 min by default
                    fixedTimewindow: {
                        startTimeMs: currentTime - DAY, // 1 day by default
                        endTimeMs: currentTime
                    }
            },
            aggregation: {
                type: types.aggregation.avg.value,
                limit: Math.floor(maxDatapointsLimit / 2)
            }
        }
        return timewindow;
    }

    function toHistoryTimewindow(timewindow, startTimeMs, endTimeMs, interval) {
        if (timewindow.history) {
            interval = angular.isDefined(interval) ? interval : timewindow.history.interval;
        } else if (timewindow.realtime) {
            interval = timewindow.realtime.interval;
        } else {
            interval = 0;
        }

        var aggType;
        var limit;
        if (timewindow.aggregation) {
            aggType = timewindow.aggregation.type || types.aggregation.avg.value;
            limit = timewindow.aggregation.limit || maxDatapointsLimit;
        } else {
            aggType = types.aggregation.avg.value;
            limit = maxDatapointsLimit;
        }


        var historyTimewindow = {
            hideInterval: timewindow.hideInterval || false,
            hideAggregation: timewindow.hideAggregation || false,
            hideAggInterval: timewindow.hideAggInterval || false,
            history: {
                fixedTimewindow: {
                    startTimeMs: startTimeMs,
                    endTimeMs: endTimeMs
                },
                interval: boundIntervalToTimewindow(endTimeMs - startTimeMs, interval, types.aggregation.avg.value)
            },
            aggregation: {
                type: aggType,
                limit: limit
            }
        }

        return historyTimewindow;
    }

    function createSubscriptionTimewindow(timewindow, stDiff, stateData) {

        var subscriptionTimewindow = {
            fixedWindow: null,
            realtimeWindowMs: null,
            aggregation: {
                interval: SECOND,
                limit: maxDatapointsLimit,
                type: types.aggregation.avg.value
            }
        };
        var aggTimewindow = 0;
        if (stateData) {
            subscriptionTimewindow.aggregation = {
                interval: SECOND,
                limit: maxDatapointsLimit,
                type: types.aggregation.none.value,
                stateData: true
            };
        } else {
            subscriptionTimewindow.aggregation = {
                interval: SECOND,
                limit: maxDatapointsLimit,
                type: types.aggregation.avg.value
            };
        }

        if (angular.isDefined(timewindow.aggregation) && !stateData) {
            subscriptionTimewindow.aggregation = {
                type: timewindow.aggregation.type || types.aggregation.avg.value,
                limit: timewindow.aggregation.limit || maxDatapointsLimit
            };
        }
        if (angular.isDefined(timewindow.realtime)) {
            subscriptionTimewindow.realtimeWindowMs = timewindow.realtime.timewindowMs;
            subscriptionTimewindow.aggregation.interval =
                boundIntervalToTimewindow(subscriptionTimewindow.realtimeWindowMs, timewindow.realtime.interval,
                    subscriptionTimewindow.aggregation.type);
            subscriptionTimewindow.startTs = (new Date).getTime() + stDiff - subscriptionTimewindow.realtimeWindowMs;
            var startDiff = subscriptionTimewindow.startTs % subscriptionTimewindow.aggregation.interval;
            aggTimewindow = subscriptionTimewindow.realtimeWindowMs;
            if (startDiff) {
                subscriptionTimewindow.startTs -= startDiff;
                aggTimewindow += subscriptionTimewindow.aggregation.interval;
            }
        } else if (angular.isDefined(timewindow.history)) {
            if (angular.isDefined(timewindow.history.timewindowMs)) {
                var currentTime = (new Date).getTime();
                subscriptionTimewindow.fixedWindow = {
                    startTimeMs: currentTime - timewindow.history.timewindowMs,
                    endTimeMs: currentTime
                }
                aggTimewindow = timewindow.history.timewindowMs;

            } else {
                subscriptionTimewindow.fixedWindow = {
                    startTimeMs: timewindow.history.fixedTimewindow.startTimeMs,
                    endTimeMs: timewindow.history.fixedTimewindow.endTimeMs
                }
                aggTimewindow = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
            }
            subscriptionTimewindow.startTs = subscriptionTimewindow.fixedWindow.startTimeMs;
            subscriptionTimewindow.aggregation.interval =
                boundIntervalToTimewindow(aggTimewindow, timewindow.history.interval, subscriptionTimewindow.aggregation.type);
        }
        var aggregation = subscriptionTimewindow.aggregation;
        aggregation.timeWindow = aggTimewindow;
        if (aggregation.type !== types.aggregation.none.value) {
            aggregation.limit = Math.ceil(aggTimewindow / subscriptionTimewindow.aggregation.interval);
        }
        return subscriptionTimewindow;
    }

    function boundIntervalToTimewindow(timewindow, intervalMs, aggType) {
        if (aggType === types.aggregation.none.value) {
            return SECOND;
        } else {
            var min = minIntervalLimit(timewindow);
            var max = maxIntervalLimit(timewindow);
            if (intervalMs) {
                return toBound(intervalMs, min, max, intervalMs);
            } else {
                return boundToPredefinedInterval(min, max, avgInterval(timewindow));
            }
        }
    }

    function createTimewindowForComparison(subscriptionTimewindow, timeUnit) {
        var timewindowForComparison = {
            fixedWindow: null,
            realtimeWindowMs: null,
            aggregation: subscriptionTimewindow.aggregation
        };

        if (subscriptionTimewindow.realtimeWindowMs) {
            timewindowForComparison.startTs = moment(subscriptionTimewindow.startTs).subtract(1, timeUnit).valueOf(); //eslint-disable-line
            timewindowForComparison.realtimeWindowMs = subscriptionTimewindow.realtimeWindowMs;
        } else if (subscriptionTimewindow.fixedWindow) {
            var timeInterval = subscriptionTimewindow.fixedWindow.endTimeMs - subscriptionTimewindow.fixedWindow.startTimeMs;
            var endTimeMs = moment(subscriptionTimewindow.fixedWindow.endTimeMs).subtract(1, timeUnit).valueOf(); //eslint-disable-line

            timewindowForComparison.startTs = endTimeMs - timeInterval;
            timewindowForComparison.fixedWindow = {
                startTimeMs: timewindowForComparison.startTs,
                endTimeMs: endTimeMs
            };
        }

        return timewindowForComparison;
    }
}
