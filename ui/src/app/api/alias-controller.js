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
export default class AliasController {

    constructor($scope, $q, $filter, utils, types, entityService, stateController, entityAliases) {
        this.$scope = $scope;
        this.$q = $q;
        this.$filter = $filter;
        this.utils = utils;
        this.types = types;
        this.entityService = entityService;
        this.stateController = stateController;
        this.entityAliases = angular.copy(entityAliases);
        this.resolvedAliases = {};
        this.resolvedAliasesPromise = {};
        this.resolvedAliasesToStateEntities = {};
    }

    updateEntityAliases(newEntityAliases) {
        var changedAliasIds = [];
        for (var aliasId in newEntityAliases) {
            var newEntityAlias = newEntityAliases[aliasId];
            var prevEntityAlias = this.entityAliases[aliasId];
            if (!angular.equals(newEntityAlias, prevEntityAlias)) {
                changedAliasIds.push(aliasId);
                this.setAliasUnresolved(aliasId);
            }
        }
        for (aliasId in this.entityAliases) {
            if (!newEntityAliases[aliasId]) {
                changedAliasIds.push(aliasId);
                this.setAliasUnresolved(aliasId);
            }
        }
        this.entityAliases = angular.copy(newEntityAliases);
        if (changedAliasIds.length) {
            this.$scope.$broadcast('entityAliasesChanged', changedAliasIds);
        }
    }

    dashboardStateChanged() {
        var changedAliasIds = [];
        for (var aliasId in this.resolvedAliasesToStateEntities) {
            var stateEntityInfo = this.resolvedAliasesToStateEntities[aliasId];
            var newEntityId = this.stateController.getEntityId(stateEntityInfo.entityParamName);
            var prevEntityId = stateEntityInfo.entityId;
            if (!angular.equals(newEntityId, prevEntityId)) {
                changedAliasIds.push(aliasId);
                this.setAliasUnresolved(aliasId);
            }
        }
        if (changedAliasIds.length) {
            this.$scope.$broadcast('entityAliasesChanged', changedAliasIds);
        }
    }

    setAliasUnresolved(aliasId) {
        delete this.resolvedAliases[aliasId];
        delete this.resolvedAliasesPromise[aliasId];
        delete this.resolvedAliasesToStateEntities[aliasId];
    }

    getEntityAliases() {
        return this.entityAliases;
    }

    getEntityAliasId(aliasName) {
        for (var aliasId in this.entityAliases) {
            var alias = this.entityAliases[aliasId];
            if (alias.alias == aliasName) {
                return aliasId;
            }
        }
        return null;
    }

    getAliasInfo(aliasId) {
        var deferred = this.$q.defer();
        var aliasInfo = this.resolvedAliases[aliasId];
        if (aliasInfo) {
            deferred.resolve(aliasInfo);
            return deferred.promise;
        } else if (this.resolvedAliasesPromise[aliasId]) {
           return this.resolvedAliasesPromise[aliasId];
        } else {
            this.resolvedAliasesPromise[aliasId] = deferred.promise;
            var aliasCtrl = this;
            var entityAlias = this.entityAliases[aliasId];
            if (entityAlias) {
                this.entityService.resolveAlias(entityAlias, this.stateController.getStateParams()).then(
                    function success(aliasInfo) {
                        aliasCtrl.resolvedAliases[aliasId] = aliasInfo;
                        delete aliasCtrl.resolvedAliasesPromise[aliasId];
                        if (aliasInfo.stateEntity) {
                            var stateEntityInfo = {
                                entityParamName: aliasInfo.entityParamName,
                                entityId: aliasCtrl.stateController.getEntityId(aliasInfo.entityParamName)
                            };
                            aliasCtrl.resolvedAliasesToStateEntities[aliasId] =
                                stateEntityInfo;
                        }
                        aliasCtrl.$scope.$broadcast('entityAliasResolved', aliasId);
                        deferred.resolve(aliasInfo);
                    },
                    function fail() {
                        deferred.reject();
                        delete aliasCtrl.resolvedAliasesPromise[aliasId];
                    }
                );
            } else {
                deferred.reject();
                delete aliasCtrl.resolvedAliasesPromise[aliasId];
            }
            return this.resolvedAliasesPromise[aliasId];
        }
    }

    resolveDatasource(datasource, isSingle) {
        var deferred = this.$q.defer();
        if (datasource.type === this.types.datasourceType.entity) {
            if (datasource.entityAliasId) {
                this.getAliasInfo(datasource.entityAliasId).then(
                    function success(aliasInfo) {
                        datasource.aliasName = aliasInfo.alias;
                        if (aliasInfo.resolveMultiple && !isSingle) {
                            var newDatasource;
                            var resolvedEntities = aliasInfo.resolvedEntities;
                            if (resolvedEntities && resolvedEntities.length) {
                                var datasources = [];
                                for (var i=0;i<resolvedEntities.length;i++) {
                                    var resolvedEntity = resolvedEntities[i];
                                    newDatasource = angular.copy(datasource);
                                    newDatasource.entityId = resolvedEntity.id;
                                    newDatasource.entityType = resolvedEntity.entityType;
                                    newDatasource.entityName = resolvedEntity.name;
                                    newDatasource.entityDescription = resolvedEntity.entityDescription
                                    newDatasource.name = resolvedEntity.name;
                                    newDatasource.generated = i > 0 ? true : false;
                                    datasources.push(newDatasource);
                                }
                                deferred.resolve(datasources);
                            } else {
                                if (aliasInfo.stateEntity) {
                                    newDatasource = angular.copy(datasource);
                                    newDatasource.unresolvedStateEntity = true;
                                    deferred.resolve([newDatasource]);
                                } else {
                                    deferred.reject();
                                }
                            }
                        } else {
                            var entity = aliasInfo.currentEntity;
                            if (entity) {
                                datasource.entityId = entity.id;
                                datasource.entityType = entity.entityType;
                                datasource.entityName = entity.name;
                                datasource.name = entity.name;
                                datasource.entityDescription = entity.entityDescription;
                                deferred.resolve([datasource]);
                            } else {
                                if (aliasInfo.stateEntity) {
                                    datasource.unresolvedStateEntity = true;
                                    deferred.resolve([datasource]);
                                } else {
                                    deferred.reject();
                                }
                            }
                        }
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else { // entityId
                datasource.aliasName = datasource.entityName;
                datasource.name = datasource.entityName;
                deferred.resolve([datasource]);
            }
        } else { // function
            deferred.resolve([datasource]);
        }
        return deferred.promise;
    }

    resolveAlarmSource(alarmSource) {
        var deferred = this.$q.defer();
        var aliasCtrl = this;
        this.resolveDatasource(alarmSource, true).then(
            function success(datasources) {
                var datasource = datasources[0];
                if (datasource.type === aliasCtrl.types.datasourceType.function) {
                    var name;
                    if (datasource.name && datasource.name.length) {
                        name = datasource.name;
                    } else {
                        name = aliasCtrl.types.datasourceType.function;
                    }
                    datasource.name = name;
                    datasource.aliasName = name;
                    datasource.entityName = name;
                } else if (datasource.unresolvedStateEntity) {
                    datasource.name = "Unresolved";
                    datasource.entityName = "Unresolved";
                }
                deferred.resolve(datasource);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    resolveDatasources(datasources) {

        var aliasCtrl = this;

        function updateDataKeyLabel(dataKey, datasource) {
            if (!dataKey.pattern) {
                dataKey.pattern = angular.copy(dataKey.label);
            }
            dataKey.label = aliasCtrl.utils.createLabelFromDatasource(datasource, dataKey.pattern);
        }

        function updateDatasourceKeyLabels(datasource) {
            for (var dk = 0; dk < datasource.dataKeys.length; dk++) {
                updateDataKeyLabel(datasource.dataKeys[dk], datasource);
            }
        }

        var deferred = this.$q.defer();
        var newDatasources = angular.copy(datasources);
        var datasorceResolveTasks = [];

        newDatasources.forEach(function (datasource) {
            var resolveDatasourceTask = aliasCtrl.resolveDatasource(datasource);
            datasorceResolveTasks.push(resolveDatasourceTask);
        });
        this.$q.all(datasorceResolveTasks).then(
            function success(datasourcesArrays) {
                var datasources = [].concat.apply([], datasourcesArrays);
                datasources = aliasCtrl.$filter('orderBy')(datasources, '+generated');
                var index = 0;
                var functionIndex = 0;
                datasources.forEach(function(datasource) {
                    if (datasource.type === aliasCtrl.types.datasourceType.function) {
                        var name;
                        if (datasource.name && datasource.name.length) {
                            name = datasource.name;
                        } else {
                            functionIndex++;
                            name = aliasCtrl.types.datasourceType.function;
                            if (functionIndex > 1) {
                                name += ' ' + functionIndex;
                            }
                        }
                        datasource.name = name;
                        datasource.aliasName = name;
                        datasource.entityName = name;
                     } else if (datasource.unresolvedStateEntity) {
                        datasource.name = "Unresolved";
                        datasource.entityName = "Unresolved";
                     }
                     datasource.dataKeys.forEach(function(dataKey) {
                         if (datasource.generated) {
                             dataKey._hash = Math.random();
                             dataKey.color = aliasCtrl.utils.getMaterialColor(index);
                         }
                         index++;
                     });
                     updateDatasourceKeyLabels(datasource);
                });
                deferred.resolve(datasources);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    getInstantAliasInfo(aliasId) {
        return this.resolvedAliases[aliasId];
    }

    updateCurrentAliasEntity(aliasId, currentEntity) {
        var aliasInfo = this.resolvedAliases[aliasId];
        if (aliasInfo) {
            var prevCurrentEntity = aliasInfo.currentEntity;
            if (!angular.equals(currentEntity, prevCurrentEntity)) {
                aliasInfo.currentEntity = currentEntity;
                this.$scope.$broadcast('entityAliasesChanged', [aliasId]);
            }
        }
    }

}