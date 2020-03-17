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
/*@ngInject*/
export default function GlobalInterceptor($rootScope, $q, $injector) {

    var toast;
    var translate;
    var userService;
    var types;
    var http;
    var timeout;

    var internalUrlPrefixes = [
        '/api/auth/token',
        '/api/plugins/rpc'
    ];

    var service = {
        request: request,
        requestError: requestError,
        response: response,
        responseError: responseError
    }

    return service;

    function getToast() {
        if (!toast) {
            toast = $injector.get("toast");
        }
        return toast;
    }

    function getTranslate() {
        if (!translate) {
            translate = $injector.get("$translate");
        }
        return translate;
    }

    function getUserService() {
        if (!userService) {
            userService = $injector.get("userService");
        }
        return userService;
    }

    function getTypes() {
        if (!types) {
            types = $injector.get("types");
        }
        return types;
    }

    function getHttp() {
        if (!http) {
            http = $injector.get("$http");
        }
        return http;
    }

    function getTimeout() {
        if (!timeout) {
            timeout = $injector.get("$timeout");
        }
        return timeout;
    }

    function rejectionErrorCode(rejection) {
        if (rejection && rejection.data && rejection.data.errorCode) {
            return rejection.data.errorCode;
        } else {
            return undefined;
        }
    }

    function isTokenBasedAuthEntryPoint(url) {
        return  url.startsWith('/api/') &&
               !url.startsWith(getTypes().entryPoints.login) &&
               !url.startsWith(getTypes().entryPoints.tokenRefresh) &&
               !url.startsWith(getTypes().entryPoints.nonTokenBased);
    }

    function refreshTokenAndRetry(request) {
        return getUserService().refreshJwtToken().then(function success() {
            getUserService().updateAuthorizationHeader(request.config.headers);
            return getHttp()(request.config);
        }, function fail(message) {
            $rootScope.$broadcast('unauthenticated');
            request.status = 401;
            request.data = {};
            request.data.message = message || getTranslate().instant('access.unauthorized');
            return $q.reject(request);
        });
    }

    function isInternalUrlPrefix(url) {
        for (var index in internalUrlPrefixes) {
            if (url.startsWith(internalUrlPrefixes[index])) {
                return true;
            }
        }
        return false;
    }

    function request(config) {
        var rejected = false;
        if (config.url.startsWith('/api/')) {
            var isLoading = !isInternalUrlPrefix(config.url);
            updateLoadingState(config, isLoading);
            if (isTokenBasedAuthEntryPoint(config.url)) {
                if (!getUserService().updateAuthorizationHeader(config.headers) &&
                    !getUserService().refreshTokenPending()) {
                    updateLoadingState(config, false);
                    rejected = true;
                    getUserService().clearJwtToken(false);
                    return $q.reject({ data: {message: getTranslate().instant('access.unauthorized')}, status: 401, config: config});
                } else if (!getUserService().isJwtTokenValid()) {
                    return $q.reject({ refreshTokenPending: true, config: config });
                }
            }
        }
        if (!rejected) {
            return config;
        }
    }

    function requestError(rejection) {
        if (rejection.config.url.startsWith('/api/')) {
            updateLoadingState(rejection.config, false);
        }
        return $q.reject(rejection);
    }

    function response(response) {
        if (response.config.url.startsWith('/api/')) {
            updateLoadingState(response.config, false);
        }
        return response;
    }

    function retryRequest (httpConfig) {
        var thisTimeout =  1000 + Math.random() * 3000;
        return getTimeout()(function() {
            return getHttp()(httpConfig);
        }, thisTimeout);
    }

    function responseError(rejection) {
        if (rejection.config.url.startsWith('/api/')) {
            updateLoadingState(rejection.config, false);
        }
        var unhandled = false;
        var ignoreErrors = rejection.config.ignoreErrors;
        var resendRequest = rejection.config.resendRequest;
        if (rejection.refreshTokenPending || rejection.status === 401) {
            var errorCode = rejectionErrorCode(rejection);
            if (rejection.refreshTokenPending || (errorCode && errorCode === getTypes().serverErrorCode.jwtTokenExpired)) {
                return refreshTokenAndRetry(rejection);
            } else if (errorCode !== getTypes().serverErrorCode.credentialsExpired) {
                unhandled = true;
            }
        } else if (rejection.status === 403) {
            if (!ignoreErrors) {
                $rootScope.$broadcast('forbidden');
            }
        } else if (rejection.status === 429) {
            if (resendRequest) {
                return retryRequest(rejection.config);
            }
        } else if (rejection.status === 0 || rejection.status === -1) {
            getToast().showError(getTranslate().instant('error.unable-to-connect'));
        } else if (!rejection.config.url.startsWith('/api/plugins/rpc')) {
            if (rejection.status === 404) {
                if (!ignoreErrors) {
                    getToast().showError(rejection.config.method + ": " + rejection.config.url + "<br/>" +
                        rejection.status + ": " + rejection.statusText);
                }
            } else {
                unhandled = true;
            }
        }

        if (unhandled && !ignoreErrors) {
            if (rejection.data && !rejection.data.message) {
                getToast().showError(rejection.data);
            } else if (rejection.data && rejection.data.message) {
                getToast().showError(rejection.data.message);
            } else {
                getToast().showError(getTranslate().instant('error.unhandled-error-code', {errorCode: rejection.status}));
            }
        }
        return $q.reject(rejection);
    }

    function updateLoadingState(config, isLoading) {
        if (!config || angular.isUndefined(config.ignoreLoading) || !config.ignoreLoading) {
            $rootScope.loading = isLoading;
        }
    }
}
