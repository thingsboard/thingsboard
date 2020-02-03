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
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.entityView', [thingsboardTypes])
    .factory('entityViewService', EntityViewService)
    .name;

/*@ngInject*/
function EntityViewService($http, $q, $window, userService, attributeService, customerService, types) {

    var service = {
        assignEntityViewToCustomer: assignEntityViewToCustomer,
        deleteEntityView: deleteEntityView,
        getCustomerEntityViews: getCustomerEntityViews,
        getEntityView: getEntityView,
        getTenantEntityViews: getTenantEntityViews,
        saveEntityView: saveEntityView,
        unassignEntityViewFromCustomer: unassignEntityViewFromCustomer,
        getEntityViewAttributes: getEntityViewAttributes,
        subscribeForEntityViewAttributes: subscribeForEntityViewAttributes,
        unsubscribeForEntityViewAttributes: unsubscribeForEntityViewAttributes,
        findByQuery: findByQuery,
        getEntityViewTypes: getEntityViewTypes,
        makeEntityViewPublic: makeEntityViewPublic
    }

    return service;

    function getTenantEntityViews(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/entityViews?limit=' + pageLink.limit;
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

    function getCustomerEntityViews(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/entityViews?limit=' + pageLink.limit;
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

    function getEntityView(entityViewId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityView/' + entityViewId;
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

    function saveEntityView(entityView) {
        var deferred = $q.defer();
        var url = '/api/entityView';

        $http.post(url, entityView).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteEntityView(entityViewId) {
        var deferred = $q.defer();
        var url = '/api/entityView/' + entityViewId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignEntityViewToCustomer(customerId, entityViewId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/entityView/' + entityViewId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignEntityViewFromCustomer(entityViewId) {
        var deferred = $q.defer();
        var url = '/api/customer/entityView/' + entityViewId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityViewAttributes(entityViewId, attributeScope, query, successCallback, config) {
        return attributeService.getEntityAttributes(types.entityType.entityView, entityViewId, attributeScope, query, successCallback, config);
    }

    function subscribeForEntityViewAttributes(entityViewId, attributeScope) {
        return attributeService.subscribeForEntityAttributes(types.entityType.entityView, entityViewId, attributeScope);
    }

    function unsubscribeForEntityViewAttributes(subscriptionId) {
        attributeService.unsubscribeForEntityAttributes(subscriptionId);
    }

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/entityViews';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, query, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEntityViewTypes(config) {
        var deferred = $q.defer();
        var url = '/api/entityView/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeEntityViewPublic(entityViewId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/entityView/' + entityViewId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }


}
