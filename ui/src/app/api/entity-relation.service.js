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
export default angular.module('thingsboard.api.entityRelation', [])
    .factory('entityRelationService', EntityRelationService)
    .name;

/*@ngInject*/
function EntityRelationService($http, $q) {

    var service = {
        saveRelation: saveRelation,
        deleteRelation: deleteRelation,
        deleteRelations: deleteRelations,
        getRelation: getRelation,
        findByFrom: findByFrom,
        findInfoByFrom: findInfoByFrom,
        findByFromAndType: findByFromAndType,
        findByTo: findByTo,
        findInfoByTo: findInfoByTo,
        findByToAndType: findByToAndType,
        findByQuery: findByQuery,
        findInfoByQuery: findInfoByQuery
    }

    return service;

    function saveRelation(relation) {
        var deferred = $q.defer();
        var url = '/api/relation';
        $http.post(url, relation).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteRelation(fromId, fromType, relationType, toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relation?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        url += '&toId=' + toId;
        url += '&toType=' + toType;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteRelations(entityId, entityType) {
        var deferred = $q.defer();
        var url = '/api/relations?entityId=' + entityId;
        url += '&entityType=' + entityType;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getRelation(fromId, fromType, relationType, toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relation?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        url += '&toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByFrom(fromId, fromType) {
        var deferred = $q.defer();
        var url = '/api/relations?fromId=' + fromId;
        url += '&fromType=' + fromType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByFrom(fromId, fromType) {
        var deferred = $q.defer();
        var url = '/api/relations/info?fromId=' + fromId;
        url += '&fromType=' + fromType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByFromAndType(fromId, fromType, relationType) {
        var deferred = $q.defer();
        var url = '/api/relations?fromId=' + fromId;
        url += '&fromType=' + fromType;
        url += '&relationType=' + relationType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByTo(toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relations?toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByTo(toId, toType) {
        var deferred = $q.defer();
        var url = '/api/relations/info?toId=' + toId;
        url += '&toType=' + toType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByToAndType(toId, toType, relationType) {
        var deferred = $q.defer();
        var url = '/api/relations?toId=' + toId;
        url += '&toType=' + toType;
        url += '&relationType=' + relationType;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findByQuery(query) {
        var deferred = $q.defer();
        var url = '/api/relations';
        $http.post(url, query).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function findInfoByQuery(query, config) {
        var deferred = $q.defer();
        var url = '/api/relations/info';
        $http.post(url, query, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

}
