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
import './gateway-config.scss';

/* eslint-disable import/no-unresolved, import/default */

import gatewayTemplate from './gateway-config.tpl.html';
import gatewayDialogTemplate from './gateway-config-dialog.tpl.html';
import beautify from "js-beautify";

/* eslint-enable import/no-unresolved, import/default */
const js_beautify = beautify.js;

export default angular.module('thingsboard.directives.gatewayConfig', [])
    .directive('tbGatewayConfig', GatewayConfig)
    .name;

/*@ngInject*/
function GatewayConfig() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled: '=ngDisabled',
            titleText: '@?',
            keyPlaceholderText: '@?',
            valuePlaceholderText: '@?',
            noDataText: '@?',
            gatewayConfig: '='
        },
        controller: GatewayConfigController,
        controllerAs: 'vm',
        templateUrl: gatewayTemplate
    };
}

/*@ngInject*/
function GatewayConfigController($scope, $document, $mdDialog, $mdUtil, $window, types) {

    let vm = this;

    vm.kvList = [];
    vm.types = types;
    $scope.$watch('vm.gatewayConfig', () => {
        vm.stopWatchKvList();
        vm.kvList.length = 0;
        if (vm.gatewayConfig) {
            for (var property in vm.gatewayConfig) {
                if (Object.prototype.hasOwnProperty.call(vm.gatewayConfig, property)) {
                    vm.kvList.push(
                        {
                            enabled: vm.gatewayConfig[property].enabled,
                            key: property + '',
                            value: vm.gatewayConfig[property].connector + '',
                            config: js_beautify(vm.gatewayConfig[property].config + '', {indent_size: 4})
                        }
                    );
                }
            }
        }
        $mdUtil.nextTick(() => {
            vm.watchKvList();
        });
    });

    vm.watchKvList = () => {
        $scope.kvListWatcher = $scope.$watch('vm.kvList', () => {
            if (!vm.gatewayConfig) {
                return;
            }
            for (let property in vm.gatewayConfig) {
                if (Object.prototype.hasOwnProperty.call(vm.gatewayConfig, property)) {
                    delete vm.gatewayConfig[property];
                }
            }
            for (let i = 0; i < vm.kvList.length; i++) {
                let entry = vm.kvList[i];
                if (entry.key && entry.value) {
                    let connectorJSON = angular.toJson({enabled: entry.enabled, connector: entry.value, config: angular.fromJson(entry.config)});
                    vm.gatewayConfig [entry.key] = angular.fromJson(connectorJSON);
                }
            }
        }, true);
    };

    vm.stopWatchKvList = () => {
        if ($scope.kvListWatcher) {
            $scope.kvListWatcher();
            $scope.kvListWatcher = null;
        }
    };

    vm.removeKeyVal = (index) => {
        if (index > -1) {
            vm.kvList.splice(index, 1);
        }
    };

    vm.addKeyVal = () => {
        if (!vm.kvList) {
            vm.kvList = [];
        }
        vm.kvList.push(
            {
                enabled: false,
                key: '',
                value: '',
                config: '{}'
            }
        );
    }

    vm.openConfigDialog = ($event, index, config, typeName) => {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: GatewayDialogController,
            controllerAs: 'vm',
            templateUrl: gatewayDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                config: config,
                typeName: typeName
            },
            targetEvent: $event,
            fullscreen: true,
            multiple: true,
        }).then(function (config) {
            if (config) {
                if (index > -1) {
                    vm.kvList[index].config = config;
                }
            }
        }, function () {
        });

    };

    vm.configTypeChange = (keyVal) => {
        for (let prop in types.gatewayConfigType) {
            if (types.gatewayConfigType[prop].value === keyVal.value) {
                if (!keyVal.key) {
                    keyVal.key = vm.configTypeChangeValid(types.gatewayConfigType[prop].name, 0);
                }
            }
        }
    };

    vm.keyValChange = (keyVal, indexKey) => {
        keyVal.key = vm.keyValChangeValid(keyVal.key, 0, indexKey);
    };

    vm.configTypeChangeValid = (name, index) => {
        let newKeyName = index ? name + index : name;
        let indexRes = vm.kvList.findIndex((element) => element.key === newKeyName);
        return indexRes === -1 ? newKeyName : vm.configTypeChangeValid(name, ++index);
    };

    vm.keyValChangeValid = (name, index, indexKey) => {
        angular.forEach(vm.kvList, function (value, key) {
            let nameEq = (index === 0) ? name : name + index;
            if (key !== indexKey && value.key && value.key === nameEq) {
                index++;
                vm.keyValChangeValid(name, index, indexKey);
            }

        });
        return (index === 0) ? name : name + index;
    };
}

/*@ngInject*/
function GatewayDialogController($scope, $mdDialog, $document, $window, config, typeName) {
    let vm = this;
    vm.doc = $document[0];
    vm.config = angular.copy(config);
    vm.typeName = "" + typeName;
    vm.configAreaOptions = {
        useWrapMode: false,
        mode: 'json',
        showGutter: true,
        showPrintMargin: true,
        theme: 'github',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            _ace.$blockScrolling = 1;
        }
    };

    vm.validateConfig = (model, editorName) => {
        if (model && model.length) {
            try {
                angular.fromJson(model);
                $scope.theForm[editorName].$setValidity('configJSON', true);
            } catch (e) {
                $scope.theForm[editorName].$setValidity('configJSON', false);
            }
        }
    };

    vm.save = () => {
        $mdDialog.hide(vm.config);
    };

    vm.cancel = () => {
        $mdDialog.hide();
    };

    vm.beautifyJson = () => {
        var res = js_beautify(vm.config, {indent_size: 4});
        vm.config = res;
    };
}

