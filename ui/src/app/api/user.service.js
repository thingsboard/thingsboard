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
import thingsboardApiLogin  from './login.service';
import angularStorage from 'angular-storage';

export default angular.module('thingsboard.api.user', [thingsboardApiLogin,
    angularStorage])
    .factory('userService', UserService)
    .name;

/*@ngInject*/
function UserService($http, $q, $rootScope, adminService, dashboardService, loginService, toast, store, jwtHelper, $translate, $state, $location) {
    var currentUser = null,
        currentUserDetails = null,
        lastPublicDashboardId = null,
        allowedDashboardIds = [],
        userLoaded = false;

    var refreshTokenQueue = [];

    var service = {
        deleteUser: deleteUser,
        getAuthority: getAuthority,
        isPublic: isPublic,
        getPublicId: getPublicId,
        parsePublicId: parsePublicId,
        isAuthenticated: isAuthenticated,
        getCurrentUser: getCurrentUser,
        getCustomerUsers: getCustomerUsers,
        getUser: getUser,
        getTenantAdmins: getTenantAdmins,
        isUserLoaded: isUserLoaded,
        saveUser: saveUser,
        sendActivationEmail: sendActivationEmail,
        getActivationLink: getActivationLink,
        setUserFromJwtToken: setUserFromJwtToken,
        getJwtToken: getJwtToken,
        clearJwtToken: clearJwtToken,
        isJwtTokenValid : isJwtTokenValid,
        validateJwtToken: validateJwtToken,
        refreshJwtToken: refreshJwtToken,
        refreshTokenPending: refreshTokenPending,
        updateAuthorizationHeader: updateAuthorizationHeader,
        gotoDefaultPlace: gotoDefaultPlace,
        forceDefaultPlace: forceDefaultPlace,
        updateLastPublicDashboardId: updateLastPublicDashboardId,
        logout: logout,
        reloadUser: reloadUser
    }

    reloadUser();

    return service;

    function reloadUser() {
        userLoaded = false;
        loadUser(true).then(function success() {
            notifyUserLoaded();
        }, function fail() {
            notifyUserLoaded();
        });
    }

    function updateAndValidateToken(token, prefix, notify) {
        var valid = false;
        var tokenData = jwtHelper.decodeToken(token);
        var issuedAt = tokenData.iat;
        var expTime = tokenData.exp;
        if (issuedAt && expTime) {
            var ttl = expTime - issuedAt;
            if (ttl > 0) {
                var clientExpiration = new Date().valueOf() + ttl*1000;
                store.set(prefix, token);
                store.set(prefix + '_expiration', clientExpiration);
                valid = true;
            }
        }
        if (!valid && notify) {
            $rootScope.$broadcast('unauthenticated');
        }
    }

    function clearTokenData() {
        store.remove('jwt_token');
        store.remove('jwt_token_expiration');
        store.remove('refresh_token');
        store.remove('refresh_token_expiration');
    }

    function setUserFromJwtToken(jwtToken, refreshToken, notify, doLogout) {
        currentUser = null;
        currentUserDetails = null;
        lastPublicDashboardId = null;
        allowedDashboardIds = [];
        if (!jwtToken) {
            clearTokenData();
            if (notify) {
                $rootScope.$broadcast('unauthenticated', doLogout);
            }
        } else {
            updateAndValidateToken(jwtToken, 'jwt_token', true);
            updateAndValidateToken(refreshToken, 'refresh_token', true);
            if (notify) {
                loadUser(false).then(function success() {
                    $rootScope.$broadcast('authenticated');
                }, function fail() {
                    $rootScope.$broadcast('unauthenticated');
                });
            } else {
                loadUser(false);
            }
        }
    }

    function isAuthenticated() {
        return store.get('jwt_token');
    }

    function getJwtToken() {
        return store.get('jwt_token');
    }

    function logout() {
        clearJwtToken(true);
    }

    function clearJwtToken(doLogout) {
        setUserFromJwtToken(null, null, true, doLogout);
    }

    function isJwtTokenValid() {
        return isTokenValid('jwt_token');
    }

    function isTokenValid(prefix) {
        var clientExpiration = store.get(prefix + '_expiration');
        return clientExpiration && clientExpiration > new Date().valueOf();
    }

    function validateJwtToken(doRefresh) {
        var deferred = $q.defer();
        if (!isTokenValid('jwt_token')) {
            if (doRefresh) {
                refreshJwtToken().then(function success() {
                    deferred.resolve();
                }, function fail() {
                    deferred.reject();
                });
            } else {
                clearJwtToken(false);
                deferred.reject();
            }
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function resolveRefreshTokenQueue(data) {
        for (var q=0; q < refreshTokenQueue.length;q++) {
            refreshTokenQueue[q].resolve(data);
        }
        refreshTokenQueue = [];
    }

    function rejectRefreshTokenQueue(message) {
        for (var q=0;q<refreshTokenQueue.length;q++) {
            refreshTokenQueue[q].reject(message);
        }
        refreshTokenQueue = [];
    }

    function refreshTokenPending() {
        return refreshTokenQueue.length > 0;
    }

    function refreshJwtToken() {
        var deferred = $q.defer();
        refreshTokenQueue.push(deferred);
        if (refreshTokenQueue.length === 1) {
            var refreshToken = store.get('refresh_token');
            var refreshTokenValid = isTokenValid('refresh_token');
            setUserFromJwtToken(null, null, false, false);
            if (!refreshTokenValid) {
                rejectRefreshTokenQueue($translate.instant('access.refresh-token-expired'));
            } else {
                var refreshTokenRequest = {
                    refreshToken: refreshToken
                };
                $http.post('/api/auth/token', refreshTokenRequest).then(function success(response) {
                    var token = response.data.token;
                    var refreshToken = response.data.refreshToken;
                    setUserFromJwtToken(token, refreshToken, false);
                    resolveRefreshTokenQueue(response.data);
                }, function fail() {
                    clearJwtToken(false);
                    rejectRefreshTokenQueue($translate.instant('access.refresh-token-failed'));
                });
            }
        }
        return deferred.promise;
    }

    function getCurrentUser() {
        return currentUser;
    }

    function getAuthority() {
        if (currentUser) {
            return currentUser.authority;
        } else {
            return '';
        }
    }

    function isPublic() {
        if (currentUser) {
            return currentUser.isPublic;
        } else {
            return false;
        }
    }

    function getPublicId() {
        if (isPublic()) {
            return currentUser.sub;
        } else {
            return null;
        }
    }

    function parsePublicId() {
        var token = getJwtToken();
        if (token) {
            var tokenData = jwtHelper.decodeToken(token);
            if (tokenData && tokenData.isPublic) {
                return tokenData.sub;
            }
        }
        return null;
    }

    function isUserLoaded() {
        return userLoaded;
    }

    function loadUser(doTokenRefresh) {

        var deferred = $q.defer();

        function fetchAllowedDashboardIds() {
            var pageLink = {limit: 100};
            var fetchDashboardsPromise;
            if (currentUser.authority === 'TENANT_ADMIN') {
                fetchDashboardsPromise = dashboardService.getTenantDashboards(pageLink);
            } else {
                fetchDashboardsPromise = dashboardService.getCustomerDashboards(currentUser.customerId, pageLink);
            }
            fetchDashboardsPromise.then(
                function success(result) {
                    var dashboards = result.data;
                    for (var d=0;d<dashboards.length;d++) {
                        allowedDashboardIds.push(dashboards[d].id.id);
                    }
                    deferred.resolve();
                },
                function fail() {
                    deferred.reject();
                }
            );
        }

        function procceedJwtTokenValidate() {
            validateJwtToken(doTokenRefresh).then(function success() {
                var jwtToken = store.get('jwt_token');
                currentUser = jwtHelper.decodeToken(jwtToken);
                if (currentUser && currentUser.scopes && currentUser.scopes.length > 0) {
                    currentUser.authority = currentUser.scopes[0];
                } else if (currentUser) {
                    currentUser.authority = "ANONYMOUS";
                }
                if (currentUser.isPublic) {
                    $rootScope.forceFullscreen = true;
                    fetchAllowedDashboardIds();
                } else if (currentUser.userId) {
                    getUser(currentUser.userId).then(
                        function success(user) {
                            currentUserDetails = user;
                            $rootScope.forceFullscreen = false;
                            if (userForceFullscreen()) {
                                $rootScope.forceFullscreen = true;
                            }
                            if ($rootScope.forceFullscreen && (currentUser.authority === 'TENANT_ADMIN' ||
                                currentUser.authority === 'CUSTOMER_USER')) {
                                fetchAllowedDashboardIds();
                            } else {
                                deferred.resolve();
                            }
                        },
                        function fail() {
                            deferred.reject();
                        }
                    )
                } else {
                    deferred.reject();
                }
            }, function fail() {
                deferred.reject();
            });
        }

        if (!currentUser) {
            var locationSearch = $location.search();
            if (locationSearch.publicId) {
                loginService.publicLogin(locationSearch.publicId).then(function success(response) {
                    var token = response.data.token;
                    var refreshToken = response.data.refreshToken;
                    updateAndValidateToken(token, 'jwt_token', false);
                    updateAndValidateToken(refreshToken, 'refresh_token', false);
                    procceedJwtTokenValidate();
                }, function fail() {
                    deferred.reject();
                });
            } else {
                procceedJwtTokenValidate();
            }
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function notifyUserLoaded() {
        if (!userLoaded) {
            userLoaded = true;
            $rootScope.$broadcast('userLoaded');
        }
    }

    function updateAuthorizationHeader(headers) {
        var jwtToken = store.get('jwt_token');
        if (jwtToken) {
            headers['X-Authorization'] = 'Bearer ' + jwtToken;
        }
        return jwtToken;
    }

    function getTenantAdmins(tenantId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/tenant/' + tenantId + '/users?limit=' + pageLink.limit;
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

    function getCustomerUsers(customerId, pageLink) {
        var deferred = $q.defer();
        var url = '/api/customer/' + customerId + '/users?limit=' + pageLink.limit;
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

    function saveUser(user, sendActivationMail) {
        var deferred = $q.defer();
        var url = '/api/user';
        if (angular.isDefined(sendActivationMail)) {
            url += '?sendActivationMail=' + sendActivationMail;
        }
        $http.post(url, user).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function getUser(userId) {
        var deferred = $q.defer();
        var url = '/api/user/' + userId;
        $http.get(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function deleteUser(userId) {
        var deferred = $q.defer();
        var url = '/api/user/' + userId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail(response) {
            deferred.reject(response.data);
        });
        return deferred.promise;
    }

    function sendActivationEmail(email) {
        var deferred = $q.defer();
        var url = '/api/user/sendActivationMail?email=' + email;
        $http.post(url, null).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getActivationLink(userId) {
        var deferred = $q.defer();
        var url = `/api/user/${userId}/activationLink`
        $http.get(url).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function forceDefaultPlace(to, params) {
        if (currentUser && isAuthenticated()) {
            if (currentUser.authority === 'TENANT_ADMIN' || currentUser.authority === 'CUSTOMER_USER') {
                if ((userHasDefaultDashboard() && $rootScope.forceFullscreen) || isPublic()) {
                    if (to.name === 'home.profile') {
                        if (userHasProfile()) {
                            return false;
                        } else {
                            return true;
                        }
                    } else if (to.name === 'home.dashboards.dashboard' && allowedDashboardIds.indexOf(params.dashboardId) > -1) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    function gotoDefaultPlace(params) {
        if (currentUser && isAuthenticated()) {
            var place = 'home.links';
            if (currentUser.authority === 'TENANT_ADMIN' || currentUser.authority === 'CUSTOMER_USER') {
                if (userHasDefaultDashboard()) {
                    place = 'home.dashboards.dashboard';
                    params = {dashboardId: currentUserDetails.additionalInfo.defaultDashboardId};
                } else if (isPublic()) {
                    place = 'home.dashboards.dashboard';
                    params = {dashboardId: lastPublicDashboardId};
                }
            } else if (currentUser.authority === 'SYS_ADMIN') {
                adminService.checkUpdates().then(
                    function (updateMessage) {
                        if (updateMessage && updateMessage.updateAvailable) {
                            toast.showInfo(updateMessage.message, 0, null, 'bottom right');
                        }
                    }
                );
            }
            $state.go(place, params);
        } else {
            $state.go('login', params);
        }
    }

    function userHasDefaultDashboard() {
        return currentUserDetails &&
               currentUserDetails.additionalInfo &&
               currentUserDetails.additionalInfo.defaultDashboardId;
    }

    function userForceFullscreen() {
        return (currentUser && currentUser.isPublic) ||
               (currentUserDetails.additionalInfo &&
                currentUserDetails.additionalInfo.defaultDashboardFullscreen &&
                currentUserDetails.additionalInfo.defaultDashboardFullscreen === true);
    }

    function userHasProfile() {
        return currentUser && !currentUser.isPublic;
    }

    function updateLastPublicDashboardId(dashboardId) {
        if (isPublic()) {
            lastPublicDashboardId = dashboardId;
        }
    }

}
