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

export default angular.module('thingsboard.api.entity', [thingsboardTypes])
    .factory('entityService', EntityService)
    .name;

/*@ngInject*/
function EntityService($http, $q, userService, deviceService,
                       assetService, tenantService, customerService,
                       ruleService, pluginService, types, utils) {
    var service = {
        getEntity: getEntity,
        getEntities: getEntities,
        getEntitiesByNameFilter: getEntitiesByNameFilter,
        entityName: entityName,
        processEntityAliases: processEntityAliases,
        getEntityKeys: getEntityKeys,
        checkEntityAlias: checkEntityAlias,
        createDatasoucesFromSubscriptionsInfo: createDatasoucesFromSubscriptionsInfo
    };

    return service;

    function getEntityPromise(entityType, entityId, config) {
        var promise;
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.getDevice(entityId, true, config);
                break;
            case types.entityType.asset:
                promise = assetService.getAsset(entityId, true, config);
                break;
            case types.entityType.tenant:
                promise = tenantService.getTenant(entityId);
                break;
            case types.entityType.customer:
                promise = customerService.getCustomer(entityId);
                break;
            case types.entityType.rule:
                promise = ruleService.getRule(entityId);
                break;
            case types.entityType.plugin:
                promise = pluginService.getPlugin(entityId);
                break;
        }
        return promise;
    }

    function getEntity(entityType, entityId, config) {
        var deferred = $q.defer();
        var promise = getEntityPromise(entityType, entityId, config);
        promise.then(
            function success(result) {
                deferred.resolve(result);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getEntitiesByIdsPromise(fetchEntityFunction, entityIds) {
        var tasks = [];
        var deferred = $q.defer();
        for (var i=0;i<entityIds.length;i++) {
            tasks.push(fetchEntityFunction(entityIds[i]));
        }
        $q.all(tasks).then(
            function success(entities) {
                if (entities) {
                    entities.sort(function (entity1, entity2) {
                        var id1 = entity1.id.id;
                        var id2 = entity2.id.id;
                        var index1 = entityIds.indexOf(id1);
                        var index2 = entityIds.indexOf(id2);
                        return index1 - index2;
                    });
                    deferred.resolve(entities);
                } else {
                    deferred.resolve([]);
                }
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getEntitiesPromise(entityType, entityIds, config) {
        var promise;
        switch (entityType) {
            case types.entityType.device:
                promise = deviceService.getDevices(entityIds, config);
                break;
            case types.entityType.asset:
                promise = assetService.getAssets(entityIds, config);
                break;
            case types.entityType.tenant:
                promise = getEntitiesByIdsPromise(tenantService.getTenant, entityIds);
                break;
            case types.entityType.customer:
                promise = getEntitiesByIdsPromise(customerService.getCustomer, entityIds);
                break;
            case types.entityType.rule:
                promise = getEntitiesByIdsPromise(ruleService.getRule, entityIds);
                break;
            case types.entityType.plugin:
                promise = getEntitiesByIdsPromise(pluginService.getPlugin, entityIds);
                break;
        }
        return promise;
    }

    function getEntities(entityType, entityIds, config) {
        var deferred = $q.defer();
        var promise = getEntitiesPromise(entityType, entityIds, config);
        promise.then(
            function success(result) {
                deferred.resolve(result);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getEntitiesByPageLinkPromise(entityType, pageLink, config) {
        var promise;
        var user = userService.getCurrentUser();
        var customerId = user.customerId;
        switch (entityType) {
            case types.entityType.device:
                if (user.authority === 'CUSTOMER_USER') {
                    promise = deviceService.getCustomerDevices(customerId, pageLink, false, config);
                } else {
                    promise = deviceService.getTenantDevices(pageLink, false, config);
                }
                break;
            case types.entityType.asset:
                if (user.authority === 'CUSTOMER_USER') {
                    promise = assetService.getCustomerAssets(customerId, pageLink, false, config);
                } else {
                    promise = assetService.getTenantAssets(pageLink, false, config);
                }
                break;
            case types.entityType.tenant:
                promise = tenantService.getTenants(pageLink);
                break;
            case types.entityType.customer:
                promise = customerService.getCustomers(pageLink);
                break;
            case types.entityType.rule:
                promise = ruleService.getAllRules(pageLink);
                break;
            case types.entityType.plugin:
                promise = pluginService.getAllPlugins(pageLink);
                break;
        }
        return promise;
    }

    function getEntitiesByNameFilter(entityType, entityNameFilter, limit, config) {
        var deferred = $q.defer();
        var pageLink = {limit: limit, textSearch: entityNameFilter};
        var promise = getEntitiesByPageLinkPromise(entityType, pageLink, config);
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

    function entityName(entityType, entity) {
        var name = '';
        switch (entityType) {
            case types.entityType.device:
            case types.entityType.asset:
            case types.entityType.rule:
            case types.entityType.plugin:
                name = entity.name;
                break;
            case types.entityType.tenant:
            case types.entityType.customer:
                name = entity.title;
                break;
        }
        return name;
    }

    function entityToEntityInfo(entityType, entity) {
        return { name: entityName(entityType, entity), entityType: entityType, id: entity.id.id };
    }

    function entitiesToEntitiesInfo(entityType, entities) {
        var entitiesInfo = [];
        for (var d = 0; d < entities.length; d++) {
            entitiesInfo.push(entityToEntityInfo(entityType, entities[d]));
        }
        return entitiesInfo;
    }

    function processEntityAlias(index, aliasIds, entityAliases, resolution, deferred) {
        if (index < aliasIds.length) {
            var aliasId = aliasIds[index];
            var entityAlias = entityAliases[aliasId];
            var alias = entityAlias.alias;
            var entityFilter = entityAlias.entityFilter;
            if (entityFilter.useFilter) {
                var entityNameFilter = entityFilter.entityNameFilter;
                getEntitiesByNameFilter(entityAlias.entityType, entityNameFilter, 100).then(
                    function(entities) {
                        if (entities && entities != null) {
                            var resolvedAlias = {alias: alias, entityType: entityAlias.entityType, entityId: entities[0].id.id};
                            resolution.aliasesInfo.entityAliases[aliasId] = resolvedAlias;
                            resolution.aliasesInfo.entityAliasesInfo[aliasId] = entitiesToEntitiesInfo(entityAlias.entityType, entities);
                            index++;
                            processEntityAlias(index, aliasIds, entityAliases, resolution, deferred);
                        } else {
                            if (!resolution.error) {
                                resolution.error = 'dashboard.invalid-aliases-config';
                            }
                            index++;
                            processEntityAlias(index, aliasIds, entityAliases, resolution, deferred);
                        }
                    });
            } else {
                var entityList = entityFilter.entityList;
                getEntities(entityAlias.entityType, entityList).then(
                    function success(entities) {
                        if (entities && entities.length > 0) {
                            var resolvedAlias = {alias: alias, entityType: entityAlias.entityType, entityId: entities[0].id.id};
                            resolution.aliasesInfo.entityAliases[aliasId] = resolvedAlias;
                            resolution.aliasesInfo.entityAliasesInfo[aliasId] = entitiesToEntitiesInfo(entityAlias.entityType, entities);
                            index++;
                            processEntityAlias(index, aliasIds, entityAliases, resolution, deferred);
                        } else {
                            if (!resolution.error) {
                                resolution.error = 'dashboard.invalid-aliases-config';
                            }
                            index++;
                            processEntityAlias(index, aliasIds, entityAliases, resolution, deferred);
                        }
                    },
                    function fail() {
                        if (!resolution.error) {
                            resolution.error = 'dashboard.invalid-aliases-config';
                        }
                        index++;
                        processEntityAlias(index, aliasIds, entityAliases, resolution, deferred);
                    }
                );
            }
        } else {
            deferred.resolve(resolution);
        }
    }

    function processEntityAliases(entityAliases) {
        var deferred = $q.defer();
        var resolution = {
            aliasesInfo: {
                entityAliases: {},
                entityAliasesInfo: {}
            }
        };
        var aliasIds = [];
        if (entityAliases) {
            for (var aliasId in entityAliases) {
                aliasIds.push(aliasId);
            }
        }
        processEntityAlias(0, aliasIds, entityAliases, resolution, deferred);
        return deferred.promise;
    }

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

    function checkEntityAlias(entityAlias) {
        var deferred = $q.defer();
        var entityType = entityAlias.entityType;
        var entityFilter = entityAlias.entityFilter;
        var promise;
        if (entityFilter.useFilter) {
            var entityNameFilter = entityFilter.entityNameFilter;
            promise = getEntitiesByNameFilter(entityType, entityNameFilter, 1);
        } else {
            var entityList = entityFilter.entityList;
            promise = getEntities(entityType, entityList);
        }
        promise.then(
            function success(entities) {
                if (entities && entities.length > 0) {
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

    function createDatasoucesFromSubscriptionsInfo(subscriptionsInfo) {
        var deferred = $q.defer();
        var datasources = [];
        processSubscriptionsInfo(0, subscriptionsInfo, datasources, deferred);
        return deferred.promise;
    }

    function processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred) {
        if (index < subscriptionsInfo.length) {
            var subscriptionInfo = validateSubscriptionInfo(subscriptionsInfo[index]);
            if (subscriptionInfo.type === types.datasourceType.entity) {
                if (subscriptionInfo.entityId) {
                    getEntity(subscriptionInfo.entityType, subscriptionInfo.entityId, {ignoreLoading: true}).then(
                        function success(entity) {
                            createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        },
                        function fail() {
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        }
                    );
                } else if (subscriptionInfo.entityName || subscriptionInfo.entityNamePrefix
                    || subscriptionInfo.entityIds) {
                    var promise;
                    if (subscriptionInfo.entityName) {
                        promise = getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityName, 1, {ignoreLoading: true});
                    } else if (subscriptionInfo.entityNamePrefix) {
                        promise = getEntitiesByNameFilter(subscriptionInfo.entityType, subscriptionInfo.entityNamePrefix, 100, {ignoreLoading: true});
                    } else if (subscriptionInfo.entityIds) {
                        promise = getEntities(subscriptionInfo.entityType, subscriptionInfo.entityIds, {ignoreLoading: true});
                    }
                    promise.then(
                        function success(entities) {
                            if (entities && entities.length > 0) {
                                for (var i = 0; i < entities.length; i++) {
                                    var entity = entities[i];
                                    createDatasourceFromSubscription(subscriptionInfo, datasources, entity);
                                }
                            }
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        },
                        function fail() {
                            index++;
                            processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                        }
                    )
                } else {
                    index++;
                    processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
                }
            } else if (subscriptionInfo.type === types.datasourceType.function) {
                createDatasourceFromSubscription(subscriptionInfo, datasources);
                index++;
                processSubscriptionsInfo(index, subscriptionsInfo, datasources, deferred);
            }
        } else {
            deferred.resolve(datasources);
        }
    }

    function validateSubscriptionInfo(subscriptionInfo) {
        if (subscriptionInfo.type === 'device') {
            subscriptionInfo.type = types.datasourceType.entity;
            subscriptionInfo.entityType = types.entityType.device;
            if (subscriptionInfo.deviceId) {
                subscriptionInfo.entityId = subscriptionInfo.deviceId;
            } else if (subscriptionInfo.deviceName) {
                subscriptionInfo.entityName = subscriptionInfo.deviceName;
            } else if (subscriptionInfo.deviceNamePrefix) {
                subscriptionInfo.entityNamePrefix = subscriptionInfo.deviceNamePrefix;
            } else if (subscriptionInfo.deviceIds) {
                subscriptionInfo.entityIds = subscriptionInfo.deviceIds;
            }
        }
        return subscriptionInfo;
    }

    function createDatasourceFromSubscription(subscriptionInfo, datasources, entity) {
        var datasource;
        if (subscriptionInfo.type === types.datasourceType.entity) {
            datasource = {
                type: subscriptionInfo.type,
                entityName: entity.name ? entity.name : entity.title,
                name: entity.name ? entity.name : entity.title,
                entityType: subscriptionInfo.entityType,
                entityId: entity.id.id,
                dataKeys: []
            }
        } else if (subscriptionInfo.type === types.datasourceType.function) {
            datasource = {
                type: subscriptionInfo.type,
                name: subscriptionInfo.name || types.datasourceType.function,
                dataKeys: []
            }
        }
        datasources.push(datasource);
        if (subscriptionInfo.timeseries) {
            createDatasourceKeys(subscriptionInfo.timeseries, types.dataKeyType.timeseries, datasource, datasources);
        }
        if (subscriptionInfo.attributes) {
            createDatasourceKeys(subscriptionInfo.attributes, types.dataKeyType.attribute, datasource, datasources);
        }
        if (subscriptionInfo.functions) {
            createDatasourceKeys(subscriptionInfo.functions, types.dataKeyType.function, datasource, datasources);
        }
    }

    function createDatasourceKeys(keyInfos, type, datasource, datasources) {
        for (var i=0;i<keyInfos.length;i++) {
            var keyInfo = keyInfos[i];
            var dataKey = utils.createKey(keyInfo, type, datasources);
            datasource.dataKeys.push(dataKey);
        }
    }

}