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
export default angular.module('thingsboard.api.attribute', [])
    .factory('attributeService', AttributeService)
    .name;

/*@ngInject*/
function AttributeService($http, $q, $filter, types, telemetryWebsocketService) {

    var entityAttributesSubscriptionMap = {};

    var service = {
        getEntityKeys: getEntityKeys,
        getEntityTimeseriesValues: getEntityTimeseriesValues,
        getEntityAttributesValues: getEntityAttributesValues,
        getEntityAttributes: getEntityAttributes,
        subscribeForEntityAttributes: subscribeForEntityAttributes,
        unsubscribeForEntityAttributes: unsubscribeForEntityAttributes,
        saveEntityAttributes: saveEntityAttributes,
        deleteEntityAttributes: deleteEntityAttributes
    }

    return service;

    function getEntityKeys(entityType, entityId, query, type) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/keys/';
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

    function getEntityTimeseriesValues(entityType, entityId, keys, startTs, endTs, limit) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/values/timeseries';
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

    function getEntityAttributesValues(entityType, entityId, attributeScope, keys, config) {
        var deferred = $q.defer();
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/values/attributes/' + attributeScope;
        if (keys && keys.length) {
            url += '?keys=' + keys;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function processAttributes(attributes, query, deferred, successCallback, update, apply) {
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

    function getEntityAttributes(entityType, entityId, attributeScope, query, successCallback, config) {
        var deferred = $q.defer();
        var subscriptionId = entityType + entityId + attributeScope;
        var eas = entityAttributesSubscriptionMap[subscriptionId];
        if (eas) {
            if (eas.attributes) {
                processAttributes(eas.attributes, query, deferred, successCallback);
                eas.subscriptionCallback = function(attributes) {
                    processAttributes(attributes, query, null, successCallback, true, true);
                }
            } else {
                eas.subscriptionCallback = function(attributes) {
                    processAttributes(attributes, query, deferred, successCallback, false, true);
                    eas.subscriptionCallback = function(attributes) {
                        processAttributes(attributes, query, null, successCallback, true, true);
                    }
                }
            }
        } else {
            var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/values/attributes/' + attributeScope;
            $http.get(url, config).then(function success(response) {
                processAttributes(response.data, query, deferred, successCallback);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred;
    }

    function onSubscriptionData(data, subscriptionId) {
        var entityAttributesSubscription = entityAttributesSubscriptionMap[subscriptionId];
        if (entityAttributesSubscription) {
            if (!entityAttributesSubscription.attributes) {
                entityAttributesSubscription.attributes = [];
                entityAttributesSubscription.keys = {};
            }
            var attributes = entityAttributesSubscription.attributes;
            var keys = entityAttributesSubscription.keys;
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
            if (entityAttributesSubscription.subscriptionCallback) {
                entityAttributesSubscription.subscriptionCallback(attributes);
            }
        }
    }

    function subscribeForEntityAttributes(entityType, entityId, attributeScope) {
        var subscriptionId = entityType + entityId + attributeScope;
        var entityAttributesSubscription = entityAttributesSubscriptionMap[subscriptionId];
        if (!entityAttributesSubscription) {
            var subscriptionCommand = {
                entityType: entityType,
                entityId: entityId,
                scope: attributeScope
            };

            var type = attributeScope === types.latestTelemetry.value ?
                types.dataKeyType.timeseries : types.dataKeyType.attribute;

            var subscriber = {
                subscriptionCommands: [subscriptionCommand],
                type: type,
                onData: function (data) {
                    if (data.data) {
                        onSubscriptionData(data.data, subscriptionId);
                    }
                }
            };
            entityAttributesSubscription = {
                subscriber: subscriber,
                attributes: null
            };
            entityAttributesSubscriptionMap[subscriptionId] = entityAttributesSubscription;
            telemetryWebsocketService.subscribe(subscriber);
        }
        return subscriptionId;
    }

    function unsubscribeForEntityAttributes(subscriptionId) {
        var entityAttributesSubscription = entityAttributesSubscriptionMap[subscriptionId];
        if (entityAttributesSubscription) {
            telemetryWebsocketService.unsubscribe(entityAttributesSubscription.subscriber);
            delete entityAttributesSubscriptionMap[subscriptionId];
        }
    }

    function saveEntityAttributes(entityType, entityId, attributeScope, attributes) {
        var deferred = $q.defer();
        var attributesData = {};
        var deleteAttributes = [];
        for (var a=0; a<attributes.length;a++) {
            if (angular.isDefined(attributes[a].value) && attributes[a].value !== null) {
                attributesData[attributes[a].key] = attributes[a].value;
            } else {
                deleteAttributes.push(attributes[a]);
            }
        }
        var deleteEntityAttributesPromise;
        if (deleteAttributes.length) {
            deleteEntityAttributesPromise = deleteEntityAttributes(entityType, entityId, attributeScope, deleteAttributes);
        }
        if (Object.keys(attributesData).length) {
            var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/' + attributeScope;
            $http.post(url, attributesData).then(function success(response) {
                if (deleteEntityAttributesPromise) {
                    deleteEntityAttributesPromise.then(
                        function success() {
                            deferred.resolve(response.data);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    )
                } else {
                    deferred.resolve(response.data);
                }
            }, function fail() {
                deferred.reject();
            });
        } else if (deleteEntityAttributesPromise) {
            deleteEntityAttributesPromise.then(
                function success() {
                    deferred.resolve();
                },
                function fail() {
                    deferred.reject();
                }
            )
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function deleteEntityAttributes(entityType, entityId, attributeScope, attributes) {
        var deferred = $q.defer();
        var keys = '';
        for (var i = 0; i < attributes.length; i++) {
            if (i > 0) {
                keys += ',';
            }
            keys += attributes[i].key;
        }
        var url = '/api/plugins/telemetry/' + entityType + '/' + entityId + '/' + attributeScope + '?keys=' + keys;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }


}