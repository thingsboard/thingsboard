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
function UserService($http, $q, $rootScope, store, jwtHelper, $translate) {
    var currentUser = null,
        userLoaded = false;

    var refreshTokenQueue = [];

    var service = {
        deleteUser: deleteUser,
        getAuthority: getAuthority,
        isAuthenticated: isAuthenticated,
        getCurrentUser: getCurrentUser,
        getCustomerUsers: getCustomerUsers,
        getUser: getUser,
        getTenantAdmins: getTenantAdmins,
        isUserLoaded: isUserLoaded,
        saveUser: saveUser,
        sendActivationEmail: sendActivationEmail,
        setUserFromJwtToken: setUserFromJwtToken,
        getJwtToken: getJwtToken,
        clearJwtToken: clearJwtToken,
        isJwtTokenValid : isJwtTokenValid,
        validateJwtToken: validateJwtToken,
        refreshJwtToken: refreshJwtToken,
        refreshTokenPending: refreshTokenPending,
        updateAuthorizationHeader: updateAuthorizationHeader,
        logout: logout
    }

    loadUser(true).then(function success() {
        notifyUserLoaded();
    }, function fail() {
        notifyUserLoaded();
    });

    return service;

    function updateAndValidateToken(token, prefix) {
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
        if (!valid) {
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
        if (!jwtToken) {
            clearTokenData();
            if (notify) {
                $rootScope.$broadcast('unauthenticated', doLogout);
            }
        } else {
            updateAndValidateToken(jwtToken, 'jwt_token');
            updateAndValidateToken(refreshToken, 'refresh_token');
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
        for (var q in refreshTokenQueue) {
            refreshTokenQueue[q].resolve(data);
        }
        refreshTokenQueue = [];
    }

    function rejectRefreshTokenQueue(message) {
        for (var q in refreshTokenQueue) {
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

    function isUserLoaded() {
        return userLoaded;
    }

    function loadUser(doTokenRefresh) {
        var deferred = $q.defer();
        if (!currentUser) {
            validateJwtToken(doTokenRefresh).then(function success() {
                var jwtToken = store.get('jwt_token');
                currentUser = jwtHelper.decodeToken(jwtToken);
                if (currentUser && currentUser.scopes && currentUser.scopes.length > 0) {
                    currentUser.authority = currentUser.scopes[0];
                } else if (currentUser) {
                    currentUser.authority = "ANONYMOUS";
                }
                deferred.resolve();
            }, function fail() {
                deferred.reject();
            });
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

    function saveUser(user) {
        var deferred = $q.defer();
        var url = '/api/user';
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

}
