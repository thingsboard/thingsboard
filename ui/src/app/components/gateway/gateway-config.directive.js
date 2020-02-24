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

import gatewayConfigTemplate from './gateway-config.tpl.html';
import gatewayConfigDialogTemplate from './gateway-config-dialog.tpl.html';
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
            gatewayConfig: '=',
            changeAlignment: '=',
            theForm: '=',
            isReadOnly: '='
        },
        controller: GatewayConfigController,
        controllerAs: 'vm',
        templateUrl: gatewayConfigTemplate
    };
}

/*@ngInject*/
function GatewayConfigController($scope, $document, $mdDialog, $mdUtil, $window, types) {
    let vm = this;
    vm.types = types;

    vm.removeConnector = (index) => {
        if (index > -1) {
            vm.gatewayConfig.splice(index, 1);
        }
    };

    vm.addNewConnector = () => {
        vm.gatewayConfig.push({
            enabled: false,
            configType: '',
            config: {},
            name: ''
        });
    };

    vm.openConfigDialog = ($event, index, config, typeName) => {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: GatewayDialogController,
            controllerAs: 'vm',
            templateUrl: gatewayConfigDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                config: config,
                typeName: typeName
            },
            targetEvent: $event,
            fullscreen: true,
            multiple: true,
        }).then(function (config) {
            if (config && index > -1) {
                console.log(config); //eslint-disable-line
                if (!angular.equals(vm.gatewayConfig[index].config, config)) {
                    $scope.gatewayConfiguration.$setDirty();
                }
                vm.gatewayConfig[index].config = config;
            }
        });

    };

    vm.changeConnectorType = (connector) => {
        for (let gatewayConfigTypeKey in types.gatewayConfigType) {
            if (types.gatewayConfigType[gatewayConfigTypeKey].value === connector.configType) {
                if (!connector.name) {
                    connector.name = generateConnectorName(types.gatewayConfigType[gatewayConfigTypeKey].name, 0);
                    break;
                }
            }
        }
    };

    vm.changeConnectorName = (connector, currentConnectorIndex) => {
        connector.name = validateConnectorName(connector.name, 0, currentConnectorIndex);
    };

    function generateConnectorName(name, index) {
        let newKeyName = index ? name + index : name;
        let indexRes = vm.gatewayConfig.findIndex((element) => element.name === newKeyName);
        return indexRes === -1 ? newKeyName : generateConnectorName(name, ++index);
    }

    function validateConnectorName(name, index, currentConnectorIndex) {
        for (let i = 0; i < vm.gatewayConfig.length; i++) {
            let nameEq = (index === 0) ? name : name + index;
            if (i !== currentConnectorIndex && vm.gatewayConfig[i].name === nameEq) {
                index++;
                validateConnectorName(name, index, currentConnectorIndex);
            }
        }
        return (index === 0) ? name : name + index;
    }

    vm.validateJSON = (config) => {
        return angular.equals({}, config);
    };
}

/*@ngInject*/
function GatewayDialogController($scope, $mdDialog, $document, $window, config, typeName) {
    let vm = this;
    vm.config = js_beautify(angular.toJson(config), {indent_size: 4});
    vm.typeName = typeName;
    vm.configAreaOptions = {
        useWrapMode: true,
        mode: 'json',
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
                $scope.theForm[editorName].$setValidity('config', true);
            } catch (e) {
                $scope.theForm[editorName].$setValidity('config', false);
            }
        }
    };

    vm.save = () => {
        $mdDialog.hide(angular.fromJson(vm.config));
    };

    vm.cancel = () => {
        $mdDialog.hide();
    };

    vm.beautifyJson = () => {
        vm.config = js_beautify(vm.config, {indent_size: 4});
    };
}

