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
import thingsboardApiLogin  from './login.service';
import angularStorage from 'angular-storage';

export default angular.module('thingsboard.api.user', [thingsboardApiLogin,
    angularStorage])
    .factory('userService', UserService)
    .name;

/*@ngInject*/
function UserService($http, $q, $rootScope, adminService, dashboardService, timeService, loginService, toast, store, jwtHelper, $translate, $state, $location, $mdDialog) {
    var currentUser = null,
        currentUserDetails = null,
        lastPublicDashboardId = null,
        allowedDashboardIds = [],
        redirectParams = null,
        userTokenAccessEnabled = false,
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
        setAuthorizationRequestHeader: setAuthorizationRequestHeader,
        setRedirectParams: setRedirectParams,
        gotoDefaultPlace: gotoDefaultPlace,
        forceDefaultPlace: forceDefaultPlace,
        updateLastPublicDashboardId: updateLastPublicDashboardId,
        logout: logout,
        reloadUser: reloadUser,
        isUserTokenAccessEnabled: isUserTokenAccessEnabled,
        loginAsUser: loginAsUser,
        setUserCredentialsEnabled: setUserCredentialsEnabled
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
        userTokenAccessEnabled = false;
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
        $http.post('/api/auth/logout', null, {ignoreErrors: true}).then(function success() {
            clearJwtToken(true);
        }, function fail() {
            clearJwtToken(true);
        });
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

        function updateUserLang() {
            if (currentUserDetails.additionalInfo && currentUserDetails.additionalInfo.lang) {
                $translate.use(currentUserDetails.additionalInfo.lang);
            }
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
                var sysParamsPromise = loadSystemParams();
                if (currentUser.isPublic) {
                    $rootScope.forceFullscreen = true;
                    sysParamsPromise.then(
                        () => { fetchAllowedDashboardIds(); },
                        () => { deferred.reject(); }
                    );
                } else if (currentUser.userId) {
                    getUser(currentUser.userId, true).then(
                        function success(user) {
                            sysParamsPromise.then(
                                () => {
                                    currentUserDetails = user;
                                    updateUserLang();
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
                                () => {
                                    deferred.reject();
                                    logout();
                                }
                            );
                        },
                        function fail() {
                            deferred.reject();
                            logout();
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
                    $location.search('publicId', null);
                    deferred.reject();
                });
            } else if (locationSearch.accessToken) {
                var token = locationSearch.accessToken;
                var refreshToken = locationSearch.refreshToken;
                $location.search('accessToken', null);
                if (refreshToken) {
                    $location.search('refreshToken', null);
                }
                try {
                    updateAndValidateToken(token, 'jwt_token', false);
                    if (refreshToken) {
                        updateAndValidateToken(refreshToken, 'refresh_token', false);
                    } else {
                        store.remove('refresh_token');
                        store.remove('refresh_token_expiration');
                    }
                } catch (e) {
                    deferred.reject();
                }
                procceedJwtTokenValidate();
            } else if (locationSearch.username && locationSearch.password) {
                var user = {};
                user.name = locationSearch.username;
                user.password = locationSearch.password;
                $location.search('username', null);
                $location.search('password', null);

                loginService.login(user).then(function success(response) {
                    var token = response.data.token;
                    var refreshToken = response.data.refreshToken;
                    try {
                        updateAndValidateToken(token, 'jwt_token', false);
                        updateAndValidateToken(refreshToken, 'refresh_token', false);
                    } catch (e) {
                        deferred.reject();
                    }
                    procceedJwtTokenValidate();
                }, function fail() {
                    deferred.reject();
                });
            } else if (locationSearch.loginError) {
                showLoginErrorDialog(locationSearch.loginError);
                $location.search('loginError', null);
                deferred.reject();
            } else {
                procceedJwtTokenValidate();
            }
        } else {
            deferred.resolve();
        }
        return deferred.promise;
    }

    function showLoginErrorDialog(loginError) {
        $translate(['login.error',
          'action.close']).then(function (translations) {
          var alert = $mdDialog.alert()
            .title(translations['login.error'])
            .htmlContent(loginError)
            .ok(translations['action.close']);
          $mdDialog.show(alert);
        });
    }

    function loadIsUserTokenAccessEnabled() {
        var deferred = $q.defer();
        if (currentUser.authority === 'SYS_ADMIN' || currentUser.authority === 'TENANT_ADMIN') {
            var url = '/api/user/tokenAccessEnabled';
            $http.get(url).then(function success(response) {
                userTokenAccessEnabled = response.data;
                deferred.resolve(response.data);
            }, function fail() {
                userTokenAccessEnabled = false;
                deferred.reject();
            });
        } else {
            userTokenAccessEnabled = false;
            deferred.resolve(false);
        }
        return deferred.promise;
    }

    function loadSystemParams() {
        var promises = [];
        promises.push(loadIsUserTokenAccessEnabled());
        promises.push(timeService.loadMaxDatapointsLimit());
        return $q.all(promises);
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

    function setAuthorizationRequestHeader(request) {
        var jwtToken = store.get('jwt_token');
        if (jwtToken) {
            request.setRequestHeader('X-Authorization', 'Bearer ' + jwtToken);
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
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function setUserCredentialsEnabled(userId, userCredentialsEnabled) {
        var deferred = $q.defer();
        var url = '/api/user/' + userId + '/userCredentialsEnabled';
        if (angular.isDefined(userCredentialsEnabled)) {
            url += '?userCredentialsEnabled=' + userCredentialsEnabled;
        }
        $http.post(url, null).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function getUser(userId, ignoreErrors, config) {
        var deferred = $q.defer();
        var url = '/api/user/' + userId;
        if (!config) {
            config = {};
        }
        config = Object.assign(config, { ignoreErrors: ignoreErrors });
        $http.get(url, config).then(function success(response) {
            deferred.resolve(response.data);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function deleteUser(userId) {
        var deferred = $q.defer();
        var url = '/api/user/' + userId;
        $http.delete(url).then(function success() {
            deferred.resolve();
        }, function fail() {
            deferred.reject();
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
                    } else if ((to.name === 'home.dashboards.dashboard' || to.name === 'dashboard')
                        && allowedDashboardIds.indexOf(params.dashboardId) > -1) {
                        return false;
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    function setRedirectParams(params) {
        redirectParams = params;
    }

    function gotoDefaultPlace(params) {
        if (currentUser && isAuthenticated()) {
            var place = redirectParams ? redirectParams.toName : 'home.links';
            params = redirectParams ? redirectParams.params : params;
            redirectParams = null;
            if (currentUser.authority === 'TENANT_ADMIN' || currentUser.authority === 'CUSTOMER_USER') {
                if (userHasDefaultDashboard()) {
                    place = $rootScope.forceFullscreen ? 'dashboard' : 'home.dashboards.dashboard';
                    params = {dashboardId: currentUserDetails.additionalInfo.defaultDashboardId};
                } else if (isPublic()) {
                    place = 'dashboard';
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
            $state.go(place, params, {reload: true});
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

    function isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    function loginAsUser(userId) {
        var url = '/api/user/' + userId + '/token';
        $http.get(url).then(function success(response) {
            var token = response.data.token;
            var refreshToken = response.data.refreshToken;
            setUserFromJwtToken(token, refreshToken, true);
        }, function fail() {
        });
    }

}
