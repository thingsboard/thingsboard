/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
function AssetService($http, $q, customerService, userService) {

    var service = {
        getAsset: getAsset,
        getAssets: getAssets,
        saveAsset: saveAsset,
        deleteAsset: deleteAsset,
        assignAssetToCustomer: assignAssetToCustomer,
        unassignAssetFromCustomer: unassignAssetFromCustomer,
        updateAssetCustomers: updateAssetCustomers,
        addAssetCustomers: addAssetCustomers,
        removeAssetCustomers: removeAssetCustomers,
        makeAssetPublic: makeAssetPublic,
        makeAssetPrivate: makeAssetPrivate,
        getTenantAssets: getTenantAssets,
        getCustomerAssets: getCustomerAssets,
        findByQuery: findByQuery,
        fetchAssetsByNameFilter: fetchAssetsByNameFilter,
        getAssetTypes: getAssetTypes
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
            deferred.resolve(prepareAsset(response.data));
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
            var assets = prepareAssets(response.data);
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
            deferred.resolve(prepareAsset(response.data));
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
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignAssetFromCustomer(assetId, customerId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/asset/' + assetId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.delete(url, config).then(function success(response) {
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function updateAssetCustomers(assetId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId + '/customers';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addAssetCustomers(assetId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId + '/customers/add';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeAssetCustomers(assetId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/asset/' + assetId + '/customers/remove';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareAsset(response.data));
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
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeAssetPrivate(assetId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/asset/' + assetId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareAsset(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantAssets(pageLink, config, type) {
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
            deferred.resolve(prepareAssets(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerAssets(customerId, pageLink, config, type) {
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
            deferred.resolve(prepareAssets(response.data));
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
            deferred.resolve(prepareAssets(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function fetchAssetsByNameFilter(assetNameFilter, limit, config) {
        var deferred = $q.defer();
        var user = userService.getCurrentUser();
        var promise;
        var pageLink = {limit: limit, textSearch: assetNameFilter};
        if (user.authority === 'CUSTOMER_USER') {
            var customerId = user.customerId;
            promise = getCustomerAssets(customerId, pageLink, config);
        } else {
            promise = getTenantAssets(pageLink, config);
        }
        promise.then(
            function success(result) {
                if (result.data && result.data.length > 0) {
                    deferred.resolve(prepareAssets(result.data));
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

    function prepareAssets(assetsData) {
        if (assetsData.data) {
            for (var i = 0; i < assetsData.data.length; i++) {
                assetsData.data[i] = prepareAsset(assetsData.data[i]);
            }
        }
        return assetsData;
    }

    function prepareAsset(asset) {
        asset.publicCustomerId = null;
        asset.assignedCustomersText = "";
        asset.assignedCustomersIds = [];
        if (asset.assignedCustomers && asset.assignedCustomers.length) {
            var assignedCustomersTitles = [];
            for (var i = 0; i < asset.assignedCustomers.length; i++) {
                var assignedCustomer = asset.assignedCustomers[i];
                asset.assignedCustomersIds.push(assignedCustomer.customerId.id);
                if (assignedCustomer.public) {
                    asset.publicCustomerId = assignedCustomer.customerId.id;
                } else {
                    assignedCustomersTitles.push(assignedCustomer.title);
                }
            }
            asset.assignedCustomersText = assignedCustomersTitles.join(', ');
        }
        return asset;
    }

}
