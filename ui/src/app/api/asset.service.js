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
export default angular.module('thingsboard.api.asset', [])
    .factory('assetService', AssetService)
    .name;

/*@ngInject*/
function AssetService($http, $q, $filter, customerService, userService) {

    var service = {
        getAsset: getAsset,
        getAssets: getAssets,
        saveAsset: saveAsset,
        deleteAsset: deleteAsset,
        assignAssetToCustomer: assignAssetToCustomer,
        unassignAssetFromCustomer: unassignAssetFromCustomer,
        makeAssetPublic: makeAssetPublic,
        getTenantAssets: getTenantAssets,
        getCustomerAssets: getCustomerAssets,
        findByQuery: findByQuery,
        fetchAssetsByNameFilter: fetchAssetsByNameFilter,
        getAssetTypes: getAssetTypes,
        findByName: findByName,
        assignAssetToEdge: assignAssetToEdge,
        unassignAssetFromEdge: unassignAssetFromEdge,
        getEdgeAssets: getEdgeAssets
    }

    return service;

    function getAsset(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getAssets(assetIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<assetIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += assetIds[i];
        }
        var url = '/api/assets?assetIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var assets = response.data;
            assets.sort(function (asset1, asset2) {
                var id1 =  asset1.id.id;
                var id2 =  asset2.id.id;
                var index1 = assetIds.indexOf(id1);
                var index2 = assetIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(assets);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveAsset(asset, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset';
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, asset, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteAsset(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignAssetToCustomer(customerId, assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignAssetFromCustomer(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeAssetPublic(assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/public/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantAssets(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/assets?limit=' + pageLink.limit;
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

    function getCustomerAssets(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/assets?limit=' + pageLink.limit;
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

    function findByQuery(query, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/assets';
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

    function fetchAssetsByNameFilter(assetNameFilter, limit, applyCustomersInfo, config) {
        var deferred = $q.defer();
        var user = userService.getCurrentUser();
        var promise;
        var pageLink = {limit: limit, textSearch: assetNameFilter};
        if (user.authority === 'CUSTOMER_USER') {
            var customerId = user.customerId;
            promise = getCustomerAssets(customerId, pageLink, applyCustomersInfo, config);
        } else {
            promise = getTenantAssets(pageLink, applyCustomersInfo, config);
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

    function getAssetTypes(config) {
        var deferred = $q.defer();
        var url = '/api/asset/types';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByName(assetName, config) {
        config = config || {};
        var deferred = $q.defer();
        var url = '/api/tenant/assets?assetName=' + assetName;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignAssetToEdge(edgeId, assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.post(url, null, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignAssetFromEdge(edgeId, assetId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEdgeAssets(edgeId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/assets?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
