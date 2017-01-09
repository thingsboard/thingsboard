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
export default angular.module('thingsboard.api.rule', [])
    .factory('ruleService', RuleService).name;

/*@ngInject*/
function RuleService($http, $q, $rootScope, $filter, types, utils) {

    var allRules = undefined;
    var systemRules = undefined;
    var tenantRules = undefined;

    $rootScope.ruleServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        invalidateRulesCache();
    });

    var service = {
        getSystemRules: getSystemRules,
        getTenantRules: getTenantRules,
        getAllRules: getAllRules,
        getRulesByPluginToken: getRulesByPluginToken,
        getRule: getRule,
        deleteRule: deleteRule,
        saveRule: saveRule,
        activateRule: activateRule,
        suspendRule: suspendRule
    }

    return service;

    function invalidateRulesCache() {
        allRules = undefined;
        systemRules = undefined;
        tenantRules = undefined;
    }

    function loadRulesCache() {
        var deferred = $q.defer();
        if (!allRules) {
            var url = '/api/rules';
            $http.get(url, null).then(function success(response) {
                allRules = response.data;
                systemRules = [];
                tenantRules = [];
                allRules = $filter('orderBy')(allRules, ['+name', '-createdTime']);
                for (var i = 0; i < allRules.length; i++) {
                    var rule = allRules[i];
                    if (rule.tenantId.id === types.id.nullUid) {
                        systemRules.push(rule);
                    } else {
                        tenantRules.push(rule);
                    }
                }
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function getSystemRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(systemRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getTenantRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(tenantRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getAllRules(pageLink) {
        var deferred = $q.defer();
        loadRulesCache().then(
            function success() {
                utils.filterSearchTextEntities(allRules, 'name', pageLink, deferred);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function getRulesByPluginToken(pluginToken) {
        var deferred = $q.defer();
        var url = '/api/rule/token/' + pluginToken;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveRule(rule) {
        var deferred = $q.defer();
        var url = '/api/rule';
        $http.post(url, rule).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function activateRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId + '/activate';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function suspendRule(ruleId) {
        var deferred = $q.defer();
        var url = '/api/rule/' + ruleId + '/suspend';
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

}
