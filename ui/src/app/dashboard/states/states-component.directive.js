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

/*@ngInject*/
export default function StatesComponent($compile, $templateCache, $controller, statesControllerService) {

    var linker = function (scope, element) {

        function destroyStateController() {
            if (scope.statesController && angular.isFunction(scope.statesController.$onDestroy)) {
                scope.statesController.$onDestroy();
            }
        }

        function init() {

            var stateController = scope.dashboardCtrl.dashboardCtx.stateController;

            stateController.openState = function(id, params, openRightLayout) {
                if (scope.statesController) {
                    scope.statesController.openState(id, params, openRightLayout);
                }
            }

            stateController.updateState = function(id, params, openRightLayout) {
                if (scope.statesController) {
                    scope.statesController.updateState(id, params, openRightLayout);
                }
            }

            stateController.resetState = function() {
                if (scope.statesController) {
                    scope.statesController.resetState();
                }
            }

            stateController.preserveState = function() {
                if (scope.statesController) {
                    var state = scope.statesController.getStateObject();
                    statesControllerService.preserveStateControllerState(scope.statesControllerId, state);
                }
            }

            stateController.cleanupPreservedStates = function() {
                statesControllerService.cleanupPreservedStates();
            }

            stateController.navigatePrevState = function(index) {
                if (scope.statesController) {
                    scope.statesController.navigatePrevState(index);
                }
            }

            stateController.getStateId = function() {
                if (scope.statesController) {
                    return scope.statesController.getStateId();
                } else {
                    return '';
                }
            }

            stateController.getStateParams = function() {
                if (scope.statesController) {
                    return scope.statesController.getStateParams();
                } else {
                    return {};
                }
            }

            stateController.getStateParamsByStateId = function(id) {
                if (scope.statesController) {
                    return scope.statesController.getStateParamsByStateId(id);
                } else {
                    return null;
                }
            }

            stateController.getEntityId = function(entityParamName) {
                if (scope.statesController) {
                    return scope.statesController.getEntityId(entityParamName);
                } else {
                    return null;
                }
            }

        }

        scope.$on('$destroy', function callOnDestroyHook() {
            destroyStateController();
        });

        scope.$watch('scope.dashboardCtrl', function() {
            if (scope.dashboardCtrl.dashboardCtx) {
                init();
            }
        })

        scope.$watch('statesControllerId', function(newValue) {
            if (newValue) {
                if (scope.statesController) {
                    destroyStateController();
                }
                var statesControllerInfo = statesControllerService.getStateController(scope.statesControllerId);
                if (!statesControllerInfo) {
                    //fallback to default
                    statesControllerInfo = statesControllerService.getStateController('default');
                }
                var template = $templateCache.get(statesControllerInfo.templateUrl);
                element.html(template);

                var preservedState = statesControllerService.withdrawStateControllerState(scope.statesControllerId);

                var locals = {
                    preservedState: preservedState
                };
                angular.extend(locals, {$scope: scope, $element: element});
                var controller = $controller(statesControllerInfo.controller, locals, true, 'vm');
                controller.instance = controller();
                scope.statesController = controller.instance;
                scope.statesController.dashboardCtrl = scope.dashboardCtrl;
                scope.statesController.states = scope.states;
                $compile(element.contents())(scope);
            }
        });

        scope.$watch('states', function() {
            if (scope.statesController) {
                scope.statesController.states = scope.states;
            }
        });

    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            statesControllerId: '=',
            dashboardCtrl: '=',
            states: '='
        }
    };
}
