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
export default angular.module('thingsboard.api.ruleChain', [])
    .factory('ruleChainService', RuleChainService).name;

/*@ngInject*/
function RuleChainService($http, $q, $filter, $ocLazyLoad, $translate, types, componentDescriptorService) {

    var ruleNodeComponents = {};

    var service = {
        getRuleChains: getRuleChains,
        getRuleChain: getRuleChain,
        saveRuleChain: saveRuleChain,
        setRootRuleChain: setRootRuleChain,
        deleteRuleChain: deleteRuleChain,
        getRuleChainMetaData: getRuleChainMetaData,
        saveRuleChainMetaData: saveRuleChainMetaData,
        getRuleNodeComponents: getRuleNodeComponents,
        getRuleNodeComponentByClazz: getRuleNodeComponentByClazz,
        getRuleNodeSupportedLinks: getRuleNodeSupportedLinks,
        ruleNodeAllowCustomLinks: ruleNodeAllowCustomLinks,
        resolveTargetRuleChains: resolveTargetRuleChains,
        testScript: testScript,
        getLatestRuleNodeDebugInput: getLatestRuleNodeDebugInput,
        getEdgeRuleChains: getEdgeRuleChains,
        getEdgesRuleChains: getEdgesRuleChains,
        assignRuleChainToEdge: assignRuleChainToEdge,
        unassignRuleChainFromEdge: unassignRuleChainFromEdge,
        setDefaultRootEdgeRuleChain: setDefaultRootEdgeRuleChain,
        addDefaultEdgeRuleChain: addDefaultEdgeRuleChain,
        removeDefaultEdgeRuleChain: removeDefaultEdgeRuleChain,
        getDefaultEdgeRuleChains: getDefaultEdgeRuleChains
    };

    return service;

    function getRuleChains(pageLink, config, type) {
        var deferred = $q.defer();
        var url = '/api/ruleChains?limit=' + pageLink.limit;
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
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRuleChain(ruleChainId, config) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveRuleChain(ruleChain) {
        var deferred = $q.defer();
        var url = '/api/ruleChain';
        $http.post(url, ruleChain).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function setRootRuleChain(ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId + '/root';
        $http.post(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteRuleChain(ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRuleChainMetaData(ruleChainId, config) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId + '/metadata';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveRuleChainMetaData(ruleChainMetaData) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/metadata';
        $http.post(url, ruleChainMetaData).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRuleNodeSupportedLinks(component) {
        var relationTypes = component.configurationDescriptor.nodeDefinition.relationTypes;
        var linkLabels = {};
        for (var i=0;i<relationTypes.length;i++) {
            var label = relationTypes[i];
            linkLabels[label] = {
                name: label,
                value: label
            };
        }
        return linkLabels;
    }

    function ruleNodeAllowCustomLinks(component) {
        return component.configurationDescriptor.nodeDefinition.customRelations;
    }

    function getRuleNodeComponents(ruleChainType) {
        var deferred = $q.defer();
        if (ruleNodeComponents[ruleChainType]) {
            deferred.resolve(ruleNodeComponents[ruleChainType]);
        } else {
            loadRuleNodeComponents(ruleChainType).then(
                (components) => {
                    resolveRuleNodeComponentsUiResources(components).then(
                        (components) => {
                            ruleNodeComponents[ruleChainType] = components;
                            ruleNodeComponents[ruleChainType].push(
                                types.ruleChainNodeComponent
                            );
                            ruleNodeComponents[ruleChainType].sort(
                                (comp1, comp2) => {
                                    var result = comp1.type.localeCompare(comp2.type);
                                    if (result == 0) {
                                        result = comp1.name.localeCompare(comp2.name);
                                    }
                                    return result;
                                }
                            );
                            deferred.resolve(ruleNodeComponents[ruleChainType]);
                        },
                        () => {
                            deferred.reject();
                        }
                    );
                },
                () => {
                    deferred.reject();
                }
            );
        }
        return deferred.promise;
    }

    function resolveRuleNodeComponentsUiResources(components) {
        var deferred = $q.defer();
        var tasks = [];
        for (var i=0;i<components.length;i++) {
            var component = components[i];
            tasks.push(resolveRuleNodeComponentUiResources(component));
        }
        $q.all(tasks).then(
            (components) => {
                deferred.resolve(components);
            },
            () => {
                deferred.resolve(components);
            }
        );
        return deferred.promise;
    }

    function resolveRuleNodeComponentUiResources(component) {
        var deferred = $q.defer();
        var uiResources = component.configurationDescriptor.nodeDefinition.uiResources;
        if (uiResources && uiResources.length) {
            var tasks = [];
            for (var i=0;i<uiResources.length;i++) {
                var uiResource = uiResources[i];
                tasks.push($ocLazyLoad.load(uiResource));
            }
            $q.all(tasks).then(
                () => {
                    deferred.resolve(component);
                },
                () => {
                    component.configurationDescriptor.nodeDefinition.uiResourceLoadError = $translate.instant('rulenode.ui-resources-load-error');
                    deferred.resolve(component);
                }
            )
        } else {
            deferred.resolve(component);
        }
        return deferred.promise;
    }

    function getRuleNodeComponentByClazz(clazz, ruleNodeType) {
        var res = $filter('filter')(ruleNodeComponents[ruleNodeType], {clazz: clazz}, true);
        if (res && res.length) {
            return res[0];
        }
        var unknownComponent = angular.copy(types.unknownNodeComponent);
        unknownComponent.clazz = clazz;
        unknownComponent.configurationDescriptor.nodeDefinition.details = "Unknown Rule Node class: " + clazz;
        return unknownComponent;
    }

    function resolveTargetRuleChains(ruleChainConnections) {
        var deferred = $q.defer();
        if (ruleChainConnections && ruleChainConnections.length) {
            var tasks = [];
            for (var i = 0; i < ruleChainConnections.length; i++) {
                tasks.push(resolveRuleChain(ruleChainConnections[i].targetRuleChainId.id));
            }
            $q.all(tasks).then(
                (ruleChains) => {
                    var ruleChainsMap = {};
                    for (var i = 0; i < ruleChains.length; i++) {
                        ruleChainsMap[ruleChains[i].id.id] = ruleChains[i];
                    }
                    deferred.resolve(ruleChainsMap);
                },
                () => {
                    deferred.reject();
                }
            );
        } else {
            deferred.resolve({});
        }
        return deferred.promise;
    }

    function resolveRuleChain(ruleChainId) {
        var deferred = $q.defer();
        getRuleChain(ruleChainId, {ignoreErrors: true}).then(
            (ruleChain) => {
                deferred.resolve(ruleChain);
            },
            () => {
                deferred.resolve({
                    id: {id: ruleChainId, entityType: types.entityType.rulechain}
                });
            }
        );
        return deferred.promise;
    }

    function loadRuleNodeComponents(ruleChainType) {
        return componentDescriptorService.getComponentDescriptorsByTypes(types.ruleNodeTypeComponentTypes, ruleChainType);
    }

    function testScript(inputParams) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/testScript';
        $http.post(url, inputParams).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getLatestRuleNodeDebugInput(ruleNodeId) {
        var deferred = $q.defer();
        var url = '/api/ruleNode/' + ruleNodeId + '/debugIn';
        $http.get(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEdgesRuleChains(pageLink, config) {
        return getRuleChains(pageLink, config, types.ruleChainType.edge);
    }

    function getEdgeRuleChains(edgeId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/ruleChains?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            if (pageLink.textSearch) {
                response.data.data = $filter('filter')(response.data.data, {title: pageLink.textSearch});
            }
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignRuleChainToEdge(edgeId, ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/ruleChain/' + ruleChainId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignRuleChainFromEdge(edgeId, ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/ruleChain/' + ruleChainId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function setDefaultRootEdgeRuleChain(ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId + '/defaultRootEdge';
        $http.post(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addDefaultEdgeRuleChain(ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId + '/defaultEdge';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeDefaultEdgeRuleChain(ruleChainId) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/' + ruleChainId + '/defaultEdge';
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDefaultEdgeRuleChains(config) {
        var deferred = $q.defer();
        var url = '/api/ruleChain/defaultEdgeRuleChains';
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
