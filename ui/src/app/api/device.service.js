/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
function DeviceService($http, $q, $window, userService, attributeService, customerService, types) {

    var service = {
        assignDeviceToCustomer: assignDeviceToCustomer,
        deleteDevice: deleteDevice,
        getCustomerDevices: getCustomerDevices,
        getDevice: getDevice,
        getDevices: getDevices,
        getDeviceCredentials: getDeviceCredentials,
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
        sendTwoWayRpcCommand: sendTwoWayRpcCommand,
        findByQuery: findByQuery,
        getDeviceTypes: getDeviceTypes,
        findByName: findByName
    };

    return service;

    function getTenantDevices(pageLink, applyCustomersInfo, config, type) {
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
        if (angular.isDefined(type) && type.length) {
            url += '&type=' + type;
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

    function getCustomerDevices(customerId, pageLink, applyCustomersInfo, config, type) {
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
        if (angular.isDefined(type) && type.length) {
            url += '&type=' + type;
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
        config = Object.assign(config, {ignoreErrors: ignoreErrors});
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
        for (var i = 0; i < deviceIds.length; i++) {
            if (i > 0) {
                ids += ',';
            }
            ids += deviceIds[i];
        }
        var url = '/api/devices?deviceIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var devices = response.data;
            devices.sort(function (device1, device2) {
                var id1 = device1.id.id;
                var id2 = device2.id.id;
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

    function saveDevice(device, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/device';
        $http.post(url, device, config).then(function success(response) {
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

    function getDeviceCredentials(deviceId, sync, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/device/' + deviceId + '/credentials';
        if (sync) {
            var request = new $window.XMLHttpRequest();
            request.open('GET', url, false);
            request.setRequestHeader("Accept", "application/json, text/plain, */*");
            userService.setAuthorizationRequestHeader(request);
            request.send(null);
            if (request.status === 200) {
                deferred.resolve(angular.fromJson(request.responseText));
            } else {
                deferred.reject();
            }
        } else {
            $http.get(url, config).then(function success(response) {
                deferred.resolve(response.data);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred.promise;
    }

    function saveDeviceCredentials(deviceCredentials, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/device/credentials';
        $http.post(url, deviceCredentials, config).then(function success(response) {
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

    function getDeviceAttributes(deviceId, attributeScope, query, successCallback, config) {
        return attributeService.getEntityAttributes(types.entityType.device, deviceId, attributeScope, query, successCallback, config);
    }

    function subscribeForDeviceAttributes(deviceId, attributeScope) {
        return attributeService.subscribeForEntityAttributes(types.entityType.device, deviceId, attributeScope);
    }

    function unsubscribeForDeviceAttributes(subscriptionId) {
        attributeService.unsubscribeForEntityAttributes(subscriptionId);
    }

    function saveDeviceAttributes(deviceId, attributeScope, attributes) {
        return attributeService.saveEntityAttributes(types.entityType.device, deviceId, attributeScope, attributes);
    }

    function deleteDeviceAttributes(deviceId, attributeScope, attributes) {
        return attributeService.deleteEntityAttributes(types.entityType.device, deviceId, attributeScope, attributes);
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

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/devices';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, {ignoreErrors: ignoreErrors});
        $http.post(url, query, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDeviceTypes(config) {
        var deferred = $q.defer();
        var url = '/api/device/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByName(deviceName, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/tenant/devices?deviceName=' + deviceName;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
