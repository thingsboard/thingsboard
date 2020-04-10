import thingsboardTypes from "../common/types.constant";

export default angular.module('thingsboard.api.queue', [thingsboardTypes])
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
        let url = 'api/tenant/queues?serviceType=' + serviceType;

        $http.get(url, config).then(function success(data) {
            deferred.resolve(data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}