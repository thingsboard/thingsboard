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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.device', [thingsboardTypes])
    .factory('deviceService', DeviceService)
    .name;

/*@ngInject*/
function DeviceService($http, $q, $filter, telemetryWebsocketService, types) {


    var deviceAttributesSubscriptionMap = {};

    var service = {
        assignDeviceToCustomer: assignDeviceToCustomer,
        deleteDevice: deleteDevice,
        getCustomerDevices: getCustomerDevices,
        getDevice: getDevice,
        getDeviceCredentials: getDeviceCredentials,
        getDeviceKeys: getDeviceKeys,
        getDeviceTimeseriesValues: getDeviceTimeseriesValues,
        getTenantDevices: getTenantDevices,
        saveDevice: saveDevice,
        saveDeviceCredentials: saveDeviceCredentials,
        unassignDeviceFromCustomer: unassignDeviceFromCustomer,
        getDeviceAttributes: getDeviceAttributes,
        subscribeForDeviceAttributes: subscribeForDeviceAttributes,
        unsubscribeForDeviceAttributes: unsubscribeForDeviceAttributes,
        saveDeviceAttributes: saveDeviceAttributes,
        deleteDeviceAttributes: deleteDeviceAttributes,
        sendOneWayRpcCommand: sendOneWayRpcCommand,
        sendTwoWayRpcCommand: sendTwoWayRpcCommand
    }

    return service;

    function getTenantDevices(pageLink) {
        var deferred = $q.defer();
        var url = '/api/tenant/devices?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerDevices(customerId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/devices?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDevice(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveDevice(device) {
        var deferred = $q.defer();
        var url = '/api/device';
        $http.post(url, device).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteDevice(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getDeviceCredentials(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId + '/credentials';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveDeviceCredentials(deviceCredentials) {
        var deferred = $q.defer();
        var url = '/api/device/credentials';
        $http.post(url, deviceCredentials).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function assignDeviceToCustomer(customerId, deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/device/' + deviceId;
        $http.post(url, null).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function unassignDeviceFromCustomer(deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/device/' + deviceId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getDeviceKeys(deviceId, query, type) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + deviceId + '/keys/';
        if (type === types.dataKeyType.timeseries) {
            url += 'timeseries';
        } else if (type === types.dataKeyType.attribute) {
            url += 'attributes';
        }
        $http.get(url, null).then(function success(response) {
            var result = [];
            if (response.data) {
                if (query) {
                    var dataKeys = response.data;
                    var lowercaseQuery = angular.lowercase(query);
                    for (var i in dataKeys) {
                        if (angular.lowercase(dataKeys[i]).indexOf(lowercaseQuery) === 0) {
                            result.push(dataKeys[i]);
                        }
                    }
                } else {
                    result = response.data;
                }
            }
            deferred.resolve(result);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getDeviceTimeseriesValues(deviceId, keys, startTs, endTs, limit) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + deviceId + '/values/timeseries';
        url += '?keys=' + keys;
        url += '&startTs=' + startTs;
        url += '&endTs=' + endTs;
        if (angular.isDefined(limit)) {
            url += '&limit=' + limit;
        }
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function processDeviceAttributes(attributes, query, deferred, successCallback, update) {
        attributes = $filter('orderBy')(attributes, query.order);
        if (query.search != null) {
            attributes = $filter('filter')(attributes, {key: query.search});
        }
        var responseData = {
            count: attributes.length
        }
        var startIndex = query.limit * (query.page - 1);
        responseData.data = attributes.slice(startIndex, startIndex + query.limit);
        successCallback(responseData, update);
        if (deferred) {
            deferred.resolve();
        }
    }

    function getDeviceAttributes(deviceId, attributeScope, query, successCallback) {
        var deferred = $q.defer();
        var subscriptionId = deviceId + attributeScope;
        var das = deviceAttributesSubscriptionMap[subscriptionId];
        if (das) {
            if (das.attributes) {
                processDeviceAttributes(das.attributes, query, deferred, successCallback);
                das.subscriptionCallback = function(attributes) {
                    processDeviceAttributes(attributes, query, null, successCallback, true);
                }
            } else {
                das.subscriptionCallback = function(attributes) {
                    processDeviceAttributes(attributes, query, deferred, successCallback);
                    das.subscriptionCallback = function(attributes) {
                        processDeviceAttributes(attributes, query, null, successCallback, true);
                    }
                }
            }
        } else {
            var url = '/api/plugins/telemetry/' + deviceId + '/values/attributes/' + attributeScope;
            $http.get(url, null).then(function success(response) {
                processDeviceAttributes(response.data, query, deferred, successCallback);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred;
    }

    function onSubscriptionData(data, subscriptionId) {
        var deviceAttributesSubscription = deviceAttributesSubscriptionMap[subscriptionId];
        if (deviceAttributesSubscription) {
            if (!deviceAttributesSubscription.attributes) {
                deviceAttributesSubscription.attributes = [];
                deviceAttributesSubscription.keys = {};
            }
            var attributes = deviceAttributesSubscription.attributes;
            var keys = deviceAttributesSubscription.keys;
            for (var key in data) {
                var index = keys[key];
                var attribute;
                if (index > -1) {
                    attribute = attributes[index];
                } else {
                    attribute = {
                        key: key
                    };
                    index = attributes.push(attribute)-1;
                    keys[key] = index;
                }
                var attrData = data[key][0];
                attribute.lastUpdateTs = attrData[0];
                attribute.value = attrData[1];
            }
            if (deviceAttributesSubscription.subscriptionCallback) {
                deviceAttributesSubscription.subscriptionCallback(attributes);
            }
        }
    }

    function subscribeForDeviceAttributes(deviceId, attributeScope) {
        var subscriptionId = deviceId + attributeScope;
        var deviceAttributesSubscription = deviceAttributesSubscriptionMap[subscriptionId];
        if (!deviceAttributesSubscription) {
            var subscriptionCommand = {
                deviceId: deviceId
            };

            var type = attributeScope === types.latestTelemetry.value ?
                types.dataKeyType.timeseries : types.dataKeyType.attribute;

            var subscriber = {
                subscriptionCommand: subscriptionCommand,
                type: type,
                onData: function (data) {
                    onSubscriptionData(data, subscriptionId);
                }
            };
            telemetryWebsocketService.subscribe(subscriber);
            deviceAttributesSubscription = {
                subscriber: subscriber,
                attributes: null
            }
            deviceAttributesSubscriptionMap[subscriptionId] = deviceAttributesSubscription;
        }
        return subscriptionId;
    }
    function unsubscribeForDeviceAttributes(subscriptionId) {
        var deviceAttributesSubscription = deviceAttributesSubscriptionMap[subscriptionId];
        if (deviceAttributesSubscription) {
            telemetryWebsocketService.unsubscribe(deviceAttributesSubscription.subscriber);
            delete deviceAttributesSubscriptionMap[subscriptionId];
        }
    }

    function saveDeviceAttributes(deviceId, attributeScope, attributes) {
        var deferred = $q.defer();
        var attributesData = {};
        for (var a in attributes) {
            attributesData[attributes[a].key] = attributes[a].value;
        }
        var url = '/api/plugins/telemetry/' + deviceId + '/' + attributeScope;
        $http.post(url, attributesData).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteDeviceAttributes(deviceId, attributeScope, attributes) {
        var deferred = $q.defer();
        var keys = '';
        for (var i = 0; i < attributes.length; i++) {
            if (i > 0) {
                keys += ',';
            }
            keys += attributes[i].key;
        }
        var url = '/api/plugins/telemetry/' + deviceId + '/' + attributeScope + '?keys=' + keys;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function sendOneWayRpcCommand(deviceId, requestBody) {
        var deferred = $q.defer();
        var url = '/api/plugins/rpc/oneway/' + deviceId;
        $http.post(url, requestBody).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(rejection) {
            deferred.reject(rejection);
        });
        return deferred.promise;
    }

    function sendTwoWayRpcCommand(deviceId, requestBody) {
        var deferred = $q.defer();
        var url = '/api/plugins/rpc/twoway/' + deviceId;
        $http.post(url, requestBody).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(rejection) {
            deferred.reject(rejection);
        });
        return deferred.promise;
    }

}
