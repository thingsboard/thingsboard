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

export default angular.module('thingsboard.api.queue', [])
    .factory('queueService', queueService)
    .name;

/*@ngInject*/
function queueService($http, $q) {
    var service = {
        getTenantQueuesByServiceType: getTenantQueuesByServiceType
    };

    return service;

    function getTenantQueuesByServiceType(serviceType, config) {
        let deferred = $q.defer();
        let url = '/api/tenant/queues?serviceType=' + serviceType;

        $http.get(url, config).then(function success(data) {
            deferred.resolve(data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}