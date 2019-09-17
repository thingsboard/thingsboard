/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
export default angular.module('thingsboard.api.edge', [])
    .factory('edgeService', EdgeService)
    .name;

/*@ngInject*/
function EdgeService($http, $q) {

    var service = {
        getEdges: getEdges,
        getEdgesByIds: getEdgesByIds,
        getEdge: getEdge,
        deleteEdge: deleteEdge,
        saveEdge: saveEdge
    };

    return service;

    function getEdges(pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/edges?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEdgesByIds(edgeIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<edgeIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += edgeIds[i];
        }
        var url = '/api/edges?edgeIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var entities = response.data;
            entities.sort(function (entity1, entity2) {
                var id1 =  entity1.id.id;
                var id2 =  entity2.id.id;
                var index1 = edgeIds.indexOf(id1);
                var index2 = edgeIds.indexOf(id2);
                return index1 - index2;
            });
            deferred.resolve(entities);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getEdge(edgeId, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveEdge(edge) {
        var deferred = $q.defer();
        var url = '/api/edge';
        $http.post(url, edge).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteEdge(edgeId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }
}
