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
export default angular.module('thingsboard.api.dashboard', [])
    .factory('dashboardService', DashboardService).name;

/*@ngInject*/
function DashboardService($rootScope, $http, $q, $location, $filter) {

    var stDiffPromise;

    $rootScope.dadshboardServiceStateChangeStartHandle = $rootScope.$on('$stateChangeStart', function () {
        stDiffPromise = undefined;
    });

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
        updateDashboardCustomers: updateDashboardCustomers,
        addDashboardCustomers: addDashboardCustomers,
        removeDashboardCustomers: removeDashboardCustomers,
        makeDashboardPublic: makeDashboardPublic,
        makeDashboardPrivate: makeDashboardPrivate,
        getPublicDashboardLink: getPublicDashboardLink,
        getEdgeDashboards: getEdgeDashboards,
        assignDashboardToEdge: assignDashboardToEdge,
        unassignDashboardFromEdge: unassignDashboardFromEdge
    }

    return service;

    function getTenantDashboardsByTenantId(tenantId, pageLink, config) {
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
        $http.get(url, config).then(function success(response) {
            deferred.resolve(prepareDashboards(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getTenantDashboards(pageLink, config) {
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
        $http.get(url, config).then(function success(response) {
            deferred.resolve(prepareDashboards(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getCustomerDashboards(customerId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            response.data = prepareDashboards(response.data);
            if (pageLink.textSearch) {
                response.data.data = $filter('filter')(response.data.data, {title: pageLink.textSearch});
            }
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getServerTimeDiff() {
        if (stDiffPromise) {
            return stDiffPromise;
        } else {
            var deferred = $q.defer();
            stDiffPromise = deferred.promise;
            var url = '/api/dashboard/serverTime';
            var ct1 = Date.now();
            $http.get(url, {ignoreLoading: true}).then(function success(response) {
                var ct2 = Date.now();
                var st = response.data;
                var stDiff = Math.ceil(st - (ct1 + ct2) / 2);
                deferred.resolve(stDiff);
            }, function fail() {
                deferred.reject();
            });
        }
        return stDiffPromise;
    }

    function getDashboard(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId;
        $http.get(url, null).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getDashboardInfo(dashboardId, config) {
        var deferred = $q.defer();
        var url = '/api/dashboard/info/' + dashboardId;
        $http.get(url, config).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function saveDashboard(dashboard) {
        var deferred = $q.defer();
        var url = '/api/dashboard';
        $http.post(url, cleanDashboard(dashboard)).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
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
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDashboardFromCustomer(customerId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function updateDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function addDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers/add';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function removeDashboardCustomers(dashboardId, customerIds) {
        var deferred = $q.defer();
        var url = '/api/dashboard/' + dashboardId + '/customers/remove';
        $http.post(url, customerIds).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDashboardPublic(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function makeDashboardPrivate(dashboardId) {
        var deferred = $q.defer();
        var url = '/api/customer/public/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
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
        url += "/dashboard/" + dashboard.id.id + "?publicId=" + dashboard.publicCustomerId;
        return url;
    }

    function prepareDashboards(dashboardsData) {
        if (dashboardsData.data) {
            for (var i = 0; i < dashboardsData.data.length; i++) {
                dashboardsData.data[i] = prepareDashboard(dashboardsData.data[i]);
            }
        }
        return dashboardsData;
    }

    function prepareDashboard(dashboard) {
        dashboard.publicCustomerId = null;
        dashboard.assignedCustomersText = "";
        dashboard.assignedCustomersIds = [];
        if (dashboard.assignedCustomers && dashboard.assignedCustomers.length) {
            var assignedCustomersTitles = [];
            for (var i = 0; i < dashboard.assignedCustomers.length; i++) {
                var assignedCustomer = dashboard.assignedCustomers[i];
                dashboard.assignedCustomersIds.push(assignedCustomer.customerId.id);
                if (assignedCustomer.public) {
                    dashboard.publicCustomerId = assignedCustomer.customerId.id;
                } else {
                    assignedCustomersTitles.push(assignedCustomer.title);
                }
            }
            dashboard.assignedCustomersText = assignedCustomersTitles.join(', ');
        }
        return dashboard;
    }

    function cleanDashboard(dashboard) {
        delete dashboard.publicCustomerId;
        delete dashboard.assignedCustomersText;
        delete dashboard.assignedCustomersIds;
        return dashboard;
    }

    function getEdgeDashboards(edgeId, pageLink, config) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/dashboards?limit=' + pageLink.limit;
        if (angular.isDefined(pageLink.idOffset)) {
            url += '&offset=' + pageLink.idOffset;
        }
        $http.get(url, config).then(function success(response) {
            response.data = prepareDashboards(response.data);
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function assignDashboardToEdge(edgeId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/dashboard/' + dashboardId;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function unassignDashboardFromEdge(edgeId, dashboardId) {
        var deferred = $q.defer();
        var url = '/api/edge/' + edgeId + '/dashboard/' + dashboardId;
        $http.delete(url).then(function success(response) {
            deferred.resolve(prepareDashboard(response.data));
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
