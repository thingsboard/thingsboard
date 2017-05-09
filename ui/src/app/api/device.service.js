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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.device', [thingsboardTypes])
    .factory('deviceService', DeviceService)
    .name;

/*@ngInject*/
function DeviceService($http, $q, $filter, userService, customerService, telemetryWebsocketService, types) {


    var deviceAttributesSubscriptionMap = {};

    var service = {
        assignDeviceToCustomer: assignDeviceToCustomer,
        deleteDevice: deleteDevice,
        getCustomerDevices: getCustomerDevices,
        getDevice: getDevice,
        getDevices: getDevices,
        processDeviceAliases: processDeviceAliases,
        checkDeviceAlias: checkDeviceAlias,
        fetchAliasDeviceByNameFilter: fetchAliasDeviceByNameFilter,
        getDeviceCredentials: getDeviceCredentials,
        getDeviceKeys: getDeviceKeys,
        getDeviceTimeseriesValues: getDeviceTimeseriesValues,
        getTenantDevices: getTenantDevices,
        saveDevice: saveDevice,
        saveDeviceCredentials: saveDeviceCredentials,
        unassignDeviceFromCustomer: unassignDeviceFromCustomer,
        makeDevicePublic: makeDevicePublic,
        getDeviceAttributes: getDeviceAttributes,
        subscribeForDeviceAttributes: subscribeForDeviceAttributes,
        unsubscribeForDeviceAttributes: unsubscribeForDeviceAttributes,
        saveDeviceAttributes: saveDeviceAttributes,
        deleteDeviceAttributes: deleteDeviceAttributes,
        sendOneWayRpcCommand: sendOneWayRpcCommand,
        sendTwoWayRpcCommand: sendTwoWayRpcCommand
    }

    return service;

    function getTenantDevices(pageLink, applyCustomersInfo, config) {
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
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomersInfo(response.data.data).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerDevices(customerId, pageLink, applyCustomersInfo, config) {
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
        $http.get(url, config).then(function success(response) {
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomerInfo(response.data.data, customerId).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });

        return deferred.promise;
    }

    function getDevice(deviceId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getDevices(deviceIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<deviceIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += deviceIds[i];
        }
        var url = '/api/devices?deviceIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var devices = response.data;
            devices.sort(function (device1, device2) {
               var id1 =  device1.id.id;
               var id2 =  device2.id.id;
               var index1 = deviceIds.indexOf(id1);
               var index2 = deviceIds.indexOf(id2);
               return index1 - index2;
            });
            deferred.resolve(devices);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function fetchAliasDeviceByNameFilter(deviceNameFilter, limit, applyCustomersInfo, config) {
        var deferred = $q.defer();
        var user = userService.getCurrentUser();
        var promise;
        var pageLink = {limit: limit, textSearch: deviceNameFilter};
        if (user.authority === 'CUSTOMER_USER') {
            var customerId = user.customerId;
            promise = getCustomerDevices(customerId, pageLink, applyCustomersInfo, config);
        } else {
            promise = getTenantDevices(pageLink, applyCustomersInfo, config);
        }
        promise.then(
            function success(result) {
                if (result.data && result.data.length > 0) {
                    deferred.resolve(result.data);
                } else {
                    deferred.resolve(null);
                }
            },
            function fail() {
                deferred.resolve(null);
            }
        );
        return deferred.promise;
    }

    function deviceToDeviceInfo(device) {
        return { name: device.name, id: device.id.id };
    }

    function devicesToDevicesInfo(devices) {
        var devicesInfo = [];
        for (var d = 0; d < devices.length; d++) {
            devicesInfo.push(deviceToDeviceInfo(devices[d]));
        }
        return devicesInfo;
    }

    function processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred) {
        if (index < aliasIds.length) {
            var aliasId = aliasIds[index];
            var deviceAlias = deviceAliases[aliasId];
            var alias = deviceAlias.alias;
            if (!deviceAlias.deviceFilter) {
                getDevice(deviceAlias.deviceId).then(
                    function success(device) {
                        var resolvedAlias = {alias: alias, deviceId: device.id.id};
                        resolution.aliasesInfo.deviceAliases[aliasId] = resolvedAlias;
                        resolution.aliasesInfo.deviceAliasesInfo[aliasId] = [
                            deviceToDeviceInfo(device)
                        ];
                        index++;
                        processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                    },
                    function fail() {
                        if (!resolution.error) {
                            resolution.error = 'dashboard.invalid-aliases-config';
                        }
                        index++;
                        processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                    }
                );
            } else {
                var deviceFilter = deviceAlias.deviceFilter;
                if (deviceFilter.useFilter) {
                    var deviceNameFilter = deviceFilter.deviceNameFilter;
                    fetchAliasDeviceByNameFilter(deviceNameFilter, 100, false).then(
                        function(devices) {
                            if (devices && devices != null) {
                                var resolvedAlias = {alias: alias, deviceId: devices[0].id.id};
                                resolution.aliasesInfo.deviceAliases[aliasId] = resolvedAlias;
                                resolution.aliasesInfo.deviceAliasesInfo[aliasId] = devicesToDevicesInfo(devices);
                                index++;
                                processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                            } else {
                                if (!resolution.error) {
                                    resolution.error = 'dashboard.invalid-aliases-config';
                                }
                                index++;
                                processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                            }
                        });
                } else {
                    var deviceList = deviceFilter.deviceList;
                    getDevices(deviceList).then(
                        function success(devices) {
                            if (devices && devices.length > 0) {
                                var resolvedAlias = {alias: alias, deviceId: devices[0].id.id};
                                resolution.aliasesInfo.deviceAliases[aliasId] = resolvedAlias;
                                resolution.aliasesInfo.deviceAliasesInfo[aliasId] = devicesToDevicesInfo(devices);
                                index++;
                                processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                            } else {
                                if (!resolution.error) {
                                    resolution.error = 'dashboard.invalid-aliases-config';
                                }
                                index++;
                                processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                            }
                        },
                        function fail() {
                            if (!resolution.error) {
                                resolution.error = 'dashboard.invalid-aliases-config';
                            }
                            index++;
                            processDeviceAlias(index, aliasIds, deviceAliases, resolution, deferred);
                        }
                    );
                }
            }
        } else {
            deferred.resolve(resolution);
        }
    }

    function processDeviceAliases(deviceAliases) {
        var deferred = $q.defer();
        var resolution = {
            aliasesInfo: {
                deviceAliases: {},
                deviceAliasesInfo: {}
            }
        };
        var aliasIds = [];
        if (deviceAliases) {
            for (var aliasId in deviceAliases) {
                aliasIds.push(aliasId);
            }
        }
        processDeviceAlias(0, aliasIds, deviceAliases, resolution, deferred);
        return deferred.promise;
    }

    function checkDeviceAlias(deviceAlias) {
        var deferred = $q.defer();
        var deviceFilter;
        if (deviceAlias.deviceId) {
            deviceFilter = {
                useFilter: false,
                deviceNameFilter: '',
                deviceList: [deviceAlias.deviceId]
            }
        } else {
            deviceFilter = deviceAlias.deviceFilter;
        }
        var promise;
        if (deviceFilter.useFilter) {
            var deviceNameFilter = deviceFilter.deviceNameFilter;
            promise = fetchAliasDeviceByNameFilter(deviceNameFilter, 1, false);
        } else {
            var deviceList = deviceFilter.deviceList;
            promise = getDevices(deviceList);
        }
        promise.then(
            function success(devices) {
                if (devices && devices.length > 0) {
                    deferred.resolve(true);
                } else {
                    deferred.resolve(false);
                }
            },
            function fail() {
                deferred.resolve(false);
            }
        );
        return deferred.promise;
    }

    function saveDevice(device) {
        var deferred = $q.defer();
        var url = '/api/device';
        $http.post(url, device).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteDevice(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDeviceCredentials(deviceId) {
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId + '/credentials';
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveDeviceCredentials(deviceCredentials) {
        var deferred = $q.defer();
        var url = '/api/device/credentials';
        $http.post(url, deviceCredentials).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDeviceToCustomer(customerId, deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/device/' + deviceId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDeviceFromCustomer(deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/device/' + deviceId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDevicePublic(deviceId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/device/' + deviceId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
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
                    for (var i=0; i<dataKeys.length;i++) {
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

    function processDeviceAttributes(attributes, query, deferred, successCallback, update, apply) {
        attributes = $filter('orderBy')(attributes, query.order);
        if (query.search != null) {
            attributes = $filter('filter')(attributes, {key: query.search});
        }
        var responseData = {
            count: attributes.length
        }
        var startIndex = query.limit * (query.page - 1);
        responseData.data = attributes.slice(startIndex, startIndex + query.limit);
        successCallback(responseData, update, apply);
        if (deferred) {
            deferred.resolve();
        }
    }

    function getDeviceAttributes(deviceId, attributeScope, query, successCallback, config) {
        var deferred = $q.defer();
        var subscriptionId = deviceId + attributeScope;
        var das = deviceAttributesSubscriptionMap[subscriptionId];
        if (das) {
            if (das.attributes) {
                processDeviceAttributes(das.attributes, query, deferred, successCallback);
                das.subscriptionCallback = function(attributes) {
                    processDeviceAttributes(attributes, query, null, successCallback, true, true);
                }
            } else {
                das.subscriptionCallback = function(attributes) {
                    processDeviceAttributes(attributes, query, deferred, successCallback, false, true);
                    das.subscriptionCallback = function(attributes) {
                        processDeviceAttributes(attributes, query, null, successCallback, true, true);
                    }
                }
            }
        } else {
            var url = '/api/plugins/telemetry/' + deviceId + '/values/attributes/' + attributeScope;
            $http.get(url, config).then(function success(response) {
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
                deviceId: deviceId,
                scope: attributeScope
            };

            var type = attributeScope === types.latestTelemetry.value ?
                types.dataKeyType.timeseries : types.dataKeyType.attribute;

            var subscriber = {
                subscriptionCommand: subscriptionCommand,
                type: type,
                onData: function (data) {
                    if (data.data) {
                        onSubscriptionData(data.data, subscriptionId);
                    }
                }
            };
            deviceAttributesSubscription = {
                subscriber: subscriber,
                attributes: null
            }
            deviceAttributesSubscriptionMap[subscriptionId] = deviceAttributesSubscription;
            telemetryWebsocketService.subscribe(subscriber);
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
        for (var a=0; a<attributes.length;a++) {
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
