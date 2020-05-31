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
export default angular.module('thingsboard.api.login', [])
    .factory('loginService', LoginService)
    .name;

/*@ngInject*/
function LoginService($http, $q, $rootScope) {

    var service = {
        activate: activate,
        changePassword: changePassword,
        hasUser: hasUser,
        login: login,
        publicLogin: publicLogin,
        resetPassword: resetPassword,
        sendResetPasswordLink: sendResetPasswordLink,
        loadOAuth2Clients: loadOAuth2Clients
    }

    return service;

    function hasUser() {
        return true;
    }

    function login(user) {
        var deferred = $q.defer();
        var loginRequest = {
            username: user.name,
            password: user.password
        };
        $http.post('/api/auth/login', loginRequest).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response);
        });
        return deferred.promise;
    }

    function publicLogin(publicId) {
        var deferred = $q.defer();
        var pubilcLoginRequest = {
            publicId: publicId
        };
        $http.post('/api/auth/login/public', pubilcLoginRequest).then(function success(response) {
            deferred.resolve(response);
        }, function fail(response) {
            deferred.reject(response);
        });
        return deferred.promise;
    }

    function sendResetPasswordLink(email) {
        var deferred = $q.defer();
        var url = '/api/noauth/resetPasswordByEmail';
        $http.post(url, {email: email}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function resetPassword(resetToken, password) {
        var deferred = $q.defer();
        var url = '/api/noauth/resetPassword';
        $http.post(url, {resetToken: resetToken, password: password}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function activate(activateToken, password, sendActivationMail) {
        var deferred = $q.defer();
        var url = '/api/noauth/activate';
        if(sendActivationMail === true || sendActivationMail === false) {
            url += '?sendActivationMail=' + sendActivationMail;
        }
        $http.post(url, {activateToken: activateToken, password: password}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function changePassword(currentPassword, newPassword) {
        var deferred = $q.defer();
        var url = '/api/auth/changePassword';
        $http.post(url, {currentPassword: currentPassword, newPassword: newPassword}).then(function success(response) {
            deferred.resolve(response);
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }

    function loadOAuth2Clients(){
        var deferred = $q.defer();
        var url = '/api/noauth/oauth2Clients';
        $http.post(url).then(function success(response) {
            $rootScope.oauth2Clients = response.data;
            deferred.resolve();
        }, function fail() {
            deferred.reject();
        });
        return deferred.promise;
    }
}
