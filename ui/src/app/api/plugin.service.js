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
export default angular.module('thingsboard.api.plugin', [])
    .factory('pluginService', PluginService).name;

/*@ngInject*/
function PluginService($http, $q, $rootScope, $filter, componentDescriptorService, types, utils) {

    var allPlugins = undefined;
    var allActionPlugins = undefined;
    var systemPlugins = undefined;
    var tenantPlugins = undefined;

    $rootScope.pluginServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        invalidatePluginsCache();
    });

    var service = {
        getSystemPlugins: getSystemPlugins,
        getTenantPlugins: getTenantPlugins,
        getAllPlugins: getAllPlugins,
        getAllActionPlugins: getAllActionPlugins,
        getPluginByToken: getPluginByToken,
        getPlugin: getPlugin,
        deletePlugin: deletePlugin,
        savePlugin: savePlugin,
        activatePlugin: activatePlugin,
        suspendPlugin: suspendPlugin
    }

    return service;

    function invalidatePluginsCache() {
        allPlugins = undefined;
        allActionPlugins = undefined;
        systemPlugins = undefined;
        tenantPlugins = undefined;
    }

    function loadPluginsCache() {
        var deferred = $q.defer();
        if (!allPlugins) {
            var url = '/api/plugins';
            $http.get(url, null).then(function success(response) {
                componentDescriptorService.getComponentDescriptorsByType(types.componentType.plugin).then(
                    function success(pluginComponents) {
                        allPlugins = response.data;
                        allActionPlugins = [];
                        systemPlugins = [];
                        tenantPlugins = [];
                        allPlugins = $filter('orderBy')(allPlugins, ['+name', '-createdTime']);
                        var pluginHasActionsByClazz = {};
                        for (var index in pluginComponents) {
                            pluginHasActionsByClazz[pluginComponents[index].clazz] =
                                (pluginComponents[index].actions != null && pluginComponents[index].actions.length > 0);
                        }
                        for (var i = 0; i < allPlugins.length; i++) {
                            var plugin = allPlugins[i];
                            if (pluginHasActionsByClazz[plugin.clazz] === true) {
                                allActionPlugins.push(plugin);
                            }
                            if (plugin.tenantId.id === types.id.nullUid) {
                                systemPlugins.push(plugin);
                            } else {
                                tenantPlugins.push(plugin);
                            }
                        }
                        deferred.resolve();
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            }, function fail() {
                deferred.reject();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function getSystemPlugins(pageLink) {
        var deferred = $q.defer();
        loadPluginsCache().then(
            function success() {
                utils.filterSearchTextEntities(systemPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantPlugins(pageLink) {
        var deferred = $q.defer();
        loadPluginsCache().then(
            function success() {
                utils.filterSearchTextEntities(tenantPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllActionPlugins(pageLink) {
        var deferred = $q.defer();
        loadPluginsCache().then(
            function success() {
                utils.filterSearchTextEntities(allActionPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllPlugins(pageLink) {
        var deferred = $q.defer();
        loadPluginsCache().then(
            function success() {
                utils.filterSearchTextEntities(allPlugins, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getPluginByToken(pluginToken) {
        var deferred = $q.defer();
        var url = '/api/plugin/token/' + pluginToken;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getPlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function savePlugin(plugin) {
        var deferred = $q.defer();
        var url = '/api/plugin';
        $http.post(url, plugin).then(function success(response) {
            invalidatePluginsCache();
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deletePlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId;
        $http.delete(url).then(function success() {
            invalidatePluginsCache();
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function activatePlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId + '/activate';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function suspendPlugin(pluginId) {
        var deferred = $q.defer();
        var url = '/api/plugin/' + pluginId + '/suspend';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

}
