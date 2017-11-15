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

 export default angular.module('thingsboard.api.computation', [])
     .factory('computationService', ComputationService)
     .name;


/*@ngInject*/
function ComputationService($http, $q) {

    var service = {
        upload: upload
    }

    return service;

    function upload(file) {
        var deferred = $q.defer();
        var url = '/api/computations/upload';
        var fd = new FormData();
        fd.append("file", file);
        $http.post(url, fd, {
                    transformRequest: angular.identity,
                    headers: {'Content-Type': undefined}
                }).then(function success(response) {
                      deferred.resolve(response.data);
                  }, function fail(response) {
                      deferred.reject(response.data);
                  });
        return deferred.promise;
    }
}