/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import './kv-map.scss';

/* eslint-disable import/no-unresolved, import/default */

import kvMapTemplate from './kv-map.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.keyValMap', [])
    .directive('tbKeyValMap', KeyValMap)
    .name;

/*@ngInject*/
function KeyValMap() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled:'=ngDisabled',
            titleText: '@?',
            keyPlaceholderText: '@?',
            valuePlaceholderText: '@?',
            noDataText: '@?',
            keyValMap: '='
        },
        controller: KeyValMapController,
        controllerAs: 'vm',
        templateUrl: kvMapTemplate
    };
}

/*@ngInject*/
function KeyValMapController($scope, $mdUtil) {

    let vm = this;

    vm.kvList = [];

    vm.removeKeyVal = removeKeyVal;
    vm.addKeyVal = addKeyVal;

    $scope.$watch('vm.keyValMap', () => {
        stopWatchKvList();
        vm.kvList.length = 0;
        if (vm.keyValMap) {
            for (var property in vm.keyValMap) {
                if (vm.keyValMap.hasOwnProperty(property)) {
                    vm.kvList.push(
                        {
                            key: property + '',
                            value: vm.keyValMap[property]
                        }
                    );
                }
            }
        }
        $mdUtil.nextTick(() => {
            watchKvList();
        });
    });

    function watchKvList() {
        $scope.kvListWatcher = $scope.$watch('vm.kvList', () => {
            if (!vm.keyValMap) {
                return;
            }
            for (var property in vm.keyValMap) {
                if (vm.keyValMap.hasOwnProperty(property)) {
                    delete vm.keyValMap[property];
                }
            }
            for (var i=0;i<vm.kvList.length;i++) {
                var entry = vm.kvList[i];
                vm.keyValMap[entry.key] = entry.value;
            }
        }, true);
    }

    function stopWatchKvList() {
        if ($scope.kvListWatcher) {
            $scope.kvListWatcher();
            $scope.kvListWatcher = null;
        }
    }


    function removeKeyVal(index) {
        if (index > -1) {
            vm.kvList.splice(index, 1);
        }
    }

    function addKeyVal() {
        if (!vm.kvList) {
            vm.kvList = [];
        }
        vm.kvList.push(
            {
                key: '',
                value: ''
            }
        );
    }
}
