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
import Flow from '@flowjs/ng-flow/dist/ng-flow-standalone.min';
import UrlHandler from './url.handler';

/*@ngInject*/
export default function AppRun($rootScope, $window, $injector, $location, $log, $state, $mdDialog, $filter, $q, loginService, userService, $translate) {

    $window.Flow = Flow;
    var frame = null;
    try {
        frame = $window.frameElement;
    } catch(e) {
        // ie11 fix
    }

    var forbiddenDialog = null;

    $rootScope.iframeMode = false;

    if (frame) {
        $rootScope.iframeMode = true;
        var dataWidgetAttr = angular.element(frame).attr('data-widget');
        if (dataWidgetAttr) {
            $rootScope.editWidgetInfo = angular.fromJson(dataWidgetAttr);
            $rootScope.widgetEditMode = true;
        }
    }

    initWatchers();

    var skipStateChange = false;

    function initWatchers() {
        $rootScope.unauthenticatedHandle = $rootScope.$on('unauthenticated', function (event, doLogout) {
            if (doLogout) {
                gotoPublicModule('login');
            } else {
                UrlHandler($injector, $location);
            }
        });

        $rootScope.authenticatedHandle = $rootScope.$on('authenticated', function () {
            UrlHandler($injector, $location);
        });

        $rootScope.forbiddenHandle = $rootScope.$on('forbidden', function () {
            showForbiddenDialog();
        });

        $rootScope.stateChangeStartHandle = $rootScope.$on('$stateChangeStart', function (evt, to, params) {

            if (skipStateChange) {
                skipStateChange = false;
                return;
            }

            function waitForUserLoaded() {
                if ($rootScope.userLoadedHandle) {
                    $rootScope.userLoadedHandle();
                }
                $rootScope.userLoadedHandle = $rootScope.$on('userLoaded', function () {
                    $rootScope.userLoadedHandle();
                    $state.go(to.name, params);
                });
            }

            function reloadUserFromPublicId() {
                userService.setUserFromJwtToken(null, null, false);
                waitForUserLoaded();
                userService.reloadUser();
            }

            var locationSearch = $location.search();
            var publicId = locationSearch.publicId;
            var activateToken = locationSearch.activateToken;

            if (to.url === '/createPassword?activateToken' && activateToken && activateToken.length) {
                userService.setUserFromJwtToken(null, null, false);
            }

            if (userService.isUserLoaded() === true) {
                if (userService.isAuthenticated()) {
                    if (userService.isPublic()) {
                        if (userService.parsePublicId() !== publicId) {
                            evt.preventDefault();
                            if (publicId && publicId.length > 0) {
                                reloadUserFromPublicId();
                            } else {
                                userService.logout();
                            }
                            return;
                        }
                    }
                    if (userService.forceDefaultPlace(to, params)) {
                        evt.preventDefault();
                        gotoDefaultPlace(params);
                    } else {
                        var authority = userService.getAuthority();
                        if (to.module === 'public') {
                            evt.preventDefault();
                            gotoDefaultPlace(params);
                        } else if (angular.isDefined(to.auth) &&
                            to.auth.indexOf(authority) === -1) {
                            evt.preventDefault();
                            showForbiddenDialog();
                        } else if (to.redirectTo) {
                            evt.preventDefault();
                            $state.go(to.redirectTo, params);
                        } else if (to.name === 'home.dashboards.dashboard' && $rootScope.forceFullscreen) {
                            evt.preventDefault();
                            $state.go('dashboard', params);
                        }
                    }
                } else {
                    if (publicId && publicId.length > 0) {
                        evt.preventDefault();
                        reloadUserFromPublicId();
                    } else if (to.module === 'private') {
                        evt.preventDefault();
                        var redirectParams = {};
                        redirectParams.toName = to.name;
                        redirectParams.params = params;
                        userService.setRedirectParams(redirectParams);
                        gotoPublicModule('login', params);
                    } else {
                        evt.preventDefault();
                        gotoPublicModule(to.name, params);
                    }
                }
            } else {
                evt.preventDefault();
                waitForUserLoaded();
            }
        })

        $rootScope.pageTitle = 'ThingsBoard';

        $rootScope.stateChangeSuccessHandle = $rootScope.$on('$stateChangeSuccess', function (evt, to, params) {
            if (userService.isPublic() && to.name === 'dashboard') {
                $location.search('publicId', userService.getPublicId());
                userService.updateLastPublicDashboardId(params.dashboardId);
            }
            if (angular.isDefined(to.data.pageTitle)) {
                $translate(to.data.pageTitle).then(function (translation) {
                    $rootScope.pageTitle = 'ThingsBoard | ' + translation;
                }, function (translationId) {
                    $rootScope.pageTitle = 'ThingsBoard | ' + translationId;
                });
            }
        })
    }

    function gotoDefaultPlace(params) {
        userService.gotoDefaultPlace(params);
    }

    function gotoPublicModule(name, params) {
        let tasks = [];
        if (name === "login") {
            tasks.push(loginService.loadOAuth2Clients());
        }
        $q.all(tasks).then(
            () => {
                skipStateChange = true;
                $state.go(name, params);
            },
            () => {
                skipStateChange = true;
                $state.go(name, params);
            }
        );
    }

    function showForbiddenDialog() {
        if (forbiddenDialog === null) {
            $translate(['access.access-forbidden',
                'access.access-forbidden-text',
                'action.cancel',
                'action.sign-in']).then(function (translations) {
                if (forbiddenDialog === null) {
                    forbiddenDialog = $mdDialog.confirm()
                        .title(translations['access.access-forbidden'])
                        .htmlContent(translations['access.access-forbidden-text'])
                        .cancel(translations['action.cancel'])
                        .ok(translations['action.sign-in']);
                    $mdDialog.show(forbiddenDialog).then(function () {
                        forbiddenDialog = null;
                        userService.logout();
                    }, function () {
                        forbiddenDialog = null;
                    });
                }
            });
        }
    }
}
