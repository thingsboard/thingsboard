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

import './entity-state-controller.scss';

/*@ngInject*/
export default function EntityStateController($scope, $location, $state, $stateParams, $q, $translate, utils, types, dashboardUtils, entityService) {

    var vm = this;

    vm.inited = false;

    vm.openState = openState;
    vm.updateState = updateState;
    vm.navigatePrevState = navigatePrevState;
    vm.getStateId = getStateId;
    vm.getStateParams = getStateParams;
    vm.getStateParamsByStateId = getStateParamsByStateId;

    vm.getStateName = getStateName;

    vm.selectedStateIndex = -1;

    function openState(id, params, openRightLayout) {
        if (vm.states && vm.states[id]) {
            resolveEntity(params).then(
                function success(entityName) {
                    params.entityName = entityName;
                    var newState = {
                        id: id,
                        params: params
                    }
                    //append new state
                    stopWatchStateObject();
                    vm.stateObject.push(newState);
                    vm.selectedStateIndex = vm.stateObject.length-1;
                    gotoState(vm.stateObject[vm.stateObject.length-1].id, true, openRightLayout);
                    watchStateObject();
                }
            );
        }
    }

    function updateState(id, params, openRightLayout) {
        if (vm.states && vm.states[id]) {
            resolveEntity(params).then(
                function success(entityName) {
                    params.entityName = entityName;
                    var newState = {
                        id: id,
                        params: params
                    }
                    //replace with new state
                    stopWatchStateObject();
                    vm.stateObject[vm.stateObject.length - 1] = newState;
                    gotoState(vm.stateObject[vm.stateObject.length - 1].id, true, openRightLayout);
                    watchStateObject();
                }
            );
        }
    }

    function navigatePrevState(index) {
        if (index < vm.stateObject.length-1) {
            stopWatchStateObject();
            vm.stateObject.splice(index+1, vm.stateObject.length-index-1);
            vm.selectedStateIndex = vm.stateObject.length-1;
            gotoState(vm.stateObject[vm.stateObject.length-1].id, true);
            watchStateObject();
        }
    }

    function getStateId() {
        if (vm.stateObject && vm.stateObject.length) {
            return vm.stateObject[vm.stateObject.length-1].id;
        } else {
            return '';
        }
    }

    function getStateParams() {
        if (vm.stateObject && vm.stateObject.length) {
            return vm.stateObject[vm.stateObject.length - 1].params;
        } else {
            return {};
        }
    }

    function getStateParamsByStateId(stateId) {
        var stateObj = getStateObjById(stateId);
        if (stateObj) {
            return stateObj.params;
        } else {
            return null;
        }
    }

    function getStateObjById(id) {
        for (var i=0; i < vm.stateObject.length; i++) {
            if (vm.stateObject[i].id === id) {
                return vm.stateObject[i];
            }
        }
        return null;
    }

    function getStateName(index) {
        var result = '';
        if (vm.stateObject[index]) {
            var stateName = vm.states[vm.stateObject[index].id].name;
            var translationId = types.translate.customTranslationsPrefix + stateName;
            var translation = $translate.instant(translationId);
            if (translation != translationId) {
                stateName = translation + '';
            }
            var params = vm.stateObject[index].params;
            if (params && params.entityName) {
                result = utils.insertVariable(stateName, 'entityName', params.entityName);
            } else {
                result = stateName;
            }
        }
        return result;
    }

    function resolveEntity(params) {
        var deferred = $q.defer();
        if (params && params.entityId && params.entityId.id && params.entityId.entityType) {
            entityService.getEntity(params.entityId.entityType, params.entityId.id, {ignoreLoading: true, ignoreErrors: true}).then(
                function success(entity) {
                    var entityName = entity.name;
                    deferred.resolve(entityName);
                },
                function fail() {
                    deferred.reject();
                }
            );
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function parseState(stateJson) {
        var result;
        if (stateJson) {
            try {
                result = angular.fromJson(stateJson);
            } catch (e) {
                result = [ { id: null, params: {} } ];
            }
        }
        if (!result) {
            result = [];
        }
        if (!result.length) {
            result[0] = { id: null, params: {} }
        }
        if (!result[0].id) {
            result[0].id = dashboardUtils.getRootStateId(vm.states);
        }
        return result;
    }

    $scope.$watch('vm.states', function() {
        if (vm.states) {
            if (!vm.inited) {
                vm.inited = true;
                init();
            }
        }
    });

    function init() {
        var initialState = $stateParams.state;
        vm.stateObject = parseState(initialState);
        vm.selectedStateIndex = vm.stateObject.length-1;
        gotoState(vm.stateObject[vm.stateObject.length-1].id, false);

        $scope.$watchCollection(function() {
            return $state.params;
        }, function(){
            var currentState = $state.params.state;
            vm.stateObject = parseState(currentState);
        });

        $scope.$watch('vm.dashboardCtrl.dashboardCtx.state', function() {
            if (vm.stateObject[vm.stateObject.length-1].id !== vm.dashboardCtrl.dashboardCtx.state) {
                stopWatchStateObject();
                vm.stateObject[vm.stateObject.length-1].id = vm.dashboardCtrl.dashboardCtx.state;
                updateLocation();
                watchStateObject();
            }
        });

        watchStateObject();

        if (vm.dashboardCtrl.isMobile) {
            watchSelectedStateIndex();
        }

        $scope.$watch('vm.dashboardCtrl.isMobile', function(newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                if (vm.dashboardCtrl.isMobile) {
                    watchSelectedStateIndex();
                } else {
                    stopWatchSelectedStateIndex();
                }
            }
        });

    }

    function stopWatchStateObject() {
        if (vm.stateObjectWatcher) {
            vm.stateObjectWatcher();
            vm.stateObjectWatcher = null;
        }
    }

    function watchStateObject() {
        vm.stateObjectWatcher = $scope.$watch('vm.stateObject', function(newVal, prevVal) {
            if (!angular.equals(newVal, prevVal) && newVal) {
                vm.selectedStateIndex = vm.stateObject.length-1;
                gotoState(vm.stateObject[vm.stateObject.length-1].id, true);
            }
        }, true);
    }

    function stopWatchSelectedStateIndex() {
        if (vm.selectedStateIndexWatcher) {
            vm.selectedStateIndexWatcher();
            vm.selectedStateIndexWatcher = null;
        }
    }

    function watchSelectedStateIndex() {
        vm.selectedStateIndexWatcher = $scope.$watch('vm.selectedStateIndex', function(newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                navigatePrevState(vm.selectedStateIndex);
            }
        });
    }

    function gotoState(stateId, update, openRightLayout) {
        vm.dashboardCtrl.openDashboardState(stateId, openRightLayout);
        if (update) {
            updateLocation();
        }
    }

    function updateLocation() {
        if (vm.stateObject[vm.stateObject.length-1].id) {
            $location.search({state : angular.toJson(vm.stateObject)});
        }
    }



}
