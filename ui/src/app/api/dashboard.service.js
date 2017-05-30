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
export default angular.module('thingsboard.api.dashboard', [])
    .factory('dashboardService', DashboardService).name;

/*@ngInject*/
function DashboardService($http, $q, $location, customerService) {

    var service = {
        assignDashboardToCustomer: assignDashboardToCustomer,
        getCustomerDashboards: getCustomerDashboards,
        getServerTimeDiff: getServerTimeDiff,
        getDashboard: getDashboard,
        getDashboardInfo: getDashboardInfo,
        getTenantDashboardsByTenantId: getTenantDashboardsByTenantId,
        getTenantDashboards: getTenantDashboards,
        deleteDashboard: deleteDashboard,
        saveDashboard: saveDashboard,
        unassignDashboardFromCustomer: unassignDashboardFromCustomer,
        makeDashboardPublic: makeDashboardPublic,
        getPublicDashboardLink: getPublicDashboardLink
    }

    return service;

    function getTenantDashboardsByTenantId(tenantId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/tenant/' + tenantId + '/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantDashboards(pageLink) {
        var deferred = $q.defer();
        var url = '/api/tenant/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, null).then(function success(response) {
            customerService.applyAssignedCustomersInfo(response.data.data).then(
                function success(data) {
                    response.data.data = data;
                    deferred.resolve(response.data);
                },
                function fail() {
                    deferred.reject();
                }
            );
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerDashboards(customerId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.textSearch)) {
            url += '&textSearch=' + pageLink.textSearch;
        }
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&idOffset=' + pageLink.idOffset;
        }
        if (angular.isDefined(pageLink.textOffset)) {
            url += '&textOffset=' + pageLink.textOffset;
        }
        $http.get(url, null).then(function success(response) {
            customerService.applyAssignedCustomerInfo(response.data.data, customerId).then(
                function success(data) {
                    response.data.data = data;
                    deferred.resolve(response.data);
                },
                function fail() {
                    deferred.reject();
                }
            );
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getServerTimeDiff() {
        var deferred = $q.defer();
        var url = '/api/dashboard/serverTime';
        var ct1 = Date.now();
        $http.get(url, { ignoreLoading: true }).then(function success(response) {
            var ct2 = Date.now();
            var st = response.data;
            var stDiff = Math.ceil(st - (ct1+ct2)/2);
            deferred.resolve(stDiff);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDashboard(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDashboardInfo(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/info/' + dashboardId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveDashboard(dashboard) {
        var deferred = $q.defer();
        var url = '/api/dashboard';
        $http.post(url, dashboard).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteDashboard(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDashboardToCustomer(customerId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDashboardFromCustomer(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDashboardPublic(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getPublicDashboardLink(dashboard) {
        var url = $location.protocol() + '://' + $location.host();
        var port = $location.port();
        if (port != 80 && port != 443) {
            url += ":" + port;
        }
        url += "/dashboards/" + dashboard.id.id + "?publicId=" + dashboard.customerId.id;
        return url;
    }

}
