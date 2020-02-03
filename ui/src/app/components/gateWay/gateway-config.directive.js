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
            gatewayConfig: '=',
            changeAlignment: '='
        },
        controller: GatewayConfigController,
        controllerAs: 'vm',
        templateUrl: gatewayTemplate
    };
}

/*@ngInject*/
function GatewayConfigController($scope, $document, $mdDialog, $mdUtil, $window, types, toast, $timeout, $compile, $translate) {  //eslint-disable-line

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
                    let connectorJSON = angular.toJson({
                        enabled: entry.enabled,
                        connector: entry.value,
                        config: angular.fromJson(entry.config)
                    });
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
        vm.checkboxValid(keyVal);
    };

    vm.keyValChange = (keyVal, indexKey) => {
        keyVal.key = vm.keyValChangeValid(keyVal.key, 0, indexKey);
        vm.checkboxValid(keyVal);
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

    vm.buttonValid = (config) => {
        return (angular.equals("{}", config)) ? "md-warn" : "md-primary";
    };

    vm.checkboxValid = (keyVal) => {
        if (!keyVal.key || angular.equals("", keyVal.key)
            || !keyVal.value || angular.equals("", keyVal.value)
            || angular.equals("{}", keyVal.config)) {
            return keyVal.enabled = false;
        }
        return true;
    };
    vm.checkboxValidMouseover = ($event, keyVal) => {
        console.log($event, keyVal);     //eslint-disable-line
        vm.checkboxValidClick ($event, keyVal);
    };

    vm.checkboxValidClick = ($event, keyVal) => {
        if (!vm.checkboxValid(keyVal)) {
            let errTxt = "";
            if (!keyVal.key || angular.equals("", keyVal.key)) {
                errTxt = $translate.instant('gateway.keyval-name-err');
            }

            if (!keyVal.value || angular.equals("", keyVal.value)) {
                errTxt += '<div>' + $translate.instant('gateway.keyval-type-err') + '</div>';
            }

            if (angular.equals("{}", keyVal.config)) {
                errTxt += '<div>' + $translate.instant('gateway.keyval-config-err') + '</div>';
            }
            if (!angular.equals("", errTxt)) {
                displayTooltip($event, '<div class="tb-rule-node-tooltip tb-lib-tooltip">' +
                    '<div id="tb-node-content" layout="column">' +
                    '<div class="tb-node-title">' + $translate.instant('gateway.keyval-save-err') + '</div>' +
                    '<div class="tb-node-details">' + errTxt + '</div>' +
                    '</div>' +
                    '</div>');
            }
        }
        else {
            destroyTooltips();
        }
    };


    function displayTooltip(event, content) {
        destroyTooltips();
        vm.tooltipTimeout = $timeout(() => {
            var element = angular.element(event.target);
            element.tooltipster(
                {
                    theme: 'tooltipster-shadow',
                    delay: 10,
                    animation: 'grow',
                    side: 'right'
                }
            );
            var contentElement = angular.element(content);
            $compile(contentElement)($scope);
            var tooltip = element.tooltipster('instance');
            tooltip.content(contentElement);
            tooltip.open();
        }, 500);
    }

    function destroyTooltips() {
        if (vm.tooltipTimeout) {
            $timeout.cancel(vm.tooltipTimeout);
            vm.tooltipTimeout = null;
        }
        var instances = angular.element.tooltipster.instances();
        instances.forEach((instance) => {
            if (!instance.isErrorTooltip) {
                instance.destroy();
            }
        });
    }
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
        vm.config = js_beautify(vm.config, {indent_size: 4});
    };
}

