/*
 * Copyright © 2016-2017 Ganesh hegde - HashmapInc Authors
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

export default angular.module('thingsboard.api.application', [thingsboardTypes])
    .factory('applicationService', ApplicationService)
    .name;

/*@ngInject*/
function ApplicationService($http, $q, customerService) {

    var service = {
        assignApplicationToCustomer: assignApplicationToCustomer,
        deleteApplication: deleteApplication,
        getCustomerApplications: getCustomerApplications,
        getApplication: getApplication,
        getApplications: getApplications,
        getTenantApplications: getTenantApplications,
        saveApplication: saveApplication,
        unassignApplicationFromCustomer: unassignApplicationFromCustomer,
        makeApplicationPublic: makeApplicationPublic,
        assignMiniDashboardToApplication: assignMiniDashboardToApplication,
        assignRulesToApplication: assignRulesToApplication,
        assignDeviceTypesToApplication: assignDeviceTypesToApplication,
        getApplicationsByDeviceType: getApplicationsByDeviceType,
        assignDashboardToApplication: assignDashboardToApplication
    }

    return service;

    function getTenantApplications(pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/tenant/applications?limit=' + pageLink.limit;
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
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomersInfo(response.data.data).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerApplications(customerId, pageLink, applyCustomersInfo, config, type) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/applications?limit=' + pageLink.limit;
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
            if (applyCustomersInfo) {
                customerService.applyAssignedCustomerInfo(response.data.data, customerId).then(
                    function success(data) {
                        response.data.data = data;
                        deferred.resolve(response.data);
                    },
                    function fail() {
                        deferred.reject();
                    }
                );
            } else {
                deferred.resolve(response.data);
            }
        }, function fail() {
            deferred.reject();
        });

        return deferred.promise;
    }

    function getApplication(applicationId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/application/' + applicationId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getApplications(applicationIds, config) {
        var deferred = $q.defer();
        var ids = '';
        for (var i=0;i<applicationIds.length;i++) {
            if (i>0) {
                ids += ',';
            }
            ids += applicationIds[i];
        }
        var url = '/api/application?applicationIds=' + ids;
        $http.get(url, config).then(function success(response) {
            var applications = response.data;
            applications.sort(function (application1, application2) {
               var id1 =  application1.id.id;
               var id2 =  application2.id.id;
               var index1 = applicationIds.indexOf(id1);
               var index2 = applicationIds.indexOf(id2);
               return index1 - index2;
            });
            deferred.resolve(applications);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function saveApplication(application) {
        var deferred = $q.defer();
        var url = '/api/application';
        $http.post(url, application).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteApplication(applicationId) {
        var deferred = $q.defer();
        var url = '/api/application/' + applicationId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignApplicationToCustomer(customerId, applicationId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/application/' + applicationId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignApplicationFromCustomer(applicationId) {
        var deferred = $q.defer();
        var url = '/api/customer/application/' + applicationId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeApplicationPublic(applicationId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/application/' + applicationId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignMiniDashboardToApplication(applicationId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/mini/' + dashboardId + '/application/' + applicationId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignRulesToApplication(rules) {
        var deferred = $q.defer();
        var url = '/api/app/assignRules';
        $http.post(url, rules).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDeviceTypesToApplication(applicationId, deviceTypes){
        var deferred = $q.defer();
        var url = '/api/app/' + applicationId + '/deviceTypes';
        $http.post(url, deviceTypes).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

        function getApplicationsByDeviceType(deviceType) {
        var deferred = $q.defer();
        var url = '/api/applications/' + deviceType;
        $http.get(url, deviceType).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }
        function assignDashboardToApplication(applicationId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/main/' + dashboardId + '/application/' + applicationId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
