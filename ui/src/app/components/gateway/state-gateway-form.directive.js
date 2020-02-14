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
import './state-gateway-form.scss';
import './state-gateway-dialog.scss';
/* eslint-disable import/no-unresolved, import/default */

import stateGatewayFormTemplate from './state-gateway-form.tpl.html';
import stateGatewayDialogTemplate from "./state-gateway-dialog.tpl.html";

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.stateGatewayForm', [])
    .directive('tbStateGatewayForm', StateGatewayForm)
    .name;

/*@ngInject*/
function StateGatewayForm() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            disabled: '=ngDisabled',
            keyPlaceholderText: '@?',
            valuePlaceholderText: '@?',
            noDataText: '@?',
            formId: '=',
            ctx: '=',
            theForm: '='
        },
        controller: StateGatewayFormController,
        controllerAs: 'vm',
        templateUrl: stateGatewayFormTemplate
    };

}

/*@ngInject*/
function StateGatewayFormController($scope, $mdDialog, $injector, $document, $mdExpansionPanel, $translate, types, entityService, utils) {
    $scope.$mdExpansionPanel = $mdExpansionPanel;
    let vm = this;
    vm.isReadOnly = false;
    vm.isState = true;
    vm.isDashboard = false;

    $scope.$watch('vm.ctx', function() {
        if (vm.ctx && vm.ctx.defaultSubscription) {
            vm.settings = vm.ctx.settings;
            if (vm.ctx.datasources && vm.ctx.datasources.length) {
                vm.deviceName = vm.ctx.datasources[0].name;
                vm.isDashboard = vm.deviceName ? true : false;
                if (vm.ctx.settings) {
                    vm.isReadOnly = vm.ctx.settings.readOnly;
                }
                initializeConfig();
            }
        }
    });


    function updateWidgetDisplaying() {
        if (vm.ctx && vm.ctx.$container) {
            vm.changeAlignment = (vm.ctx.$container[0].offsetWidth <= 425);
        }
    }

    function initWidgetSettings() {
        if (vm.settings.stateGatewayTitle && vm.settings.stateGatewayTitle.length) {
            vm.stateGatewayTitle = utils.customTranslation(vm.settings.stateGatewayTitle, vm.settings.stateGatewayTitle);
        } else {
            vm.stateGatewayTitle = $translate.instant('gateway.state-title');
        }
        vm.ctx.widgetActions = [vm.showConfig];
        vm.ctx.widgetTitle = vm.stateGatewayTitle;
    }


    function initializeConfig() {
        updateWidgetDisplaying();
        initWidgetSettings();
    }

    $scope.$on('state-gateway-form-resize', function (event, formId) {
        if (vm.formId == formId) {
            updateWidgetDisplaying();
        }
    });

    updateWidgetDisplaying();

 

    vm.showConfig = {
        name: "gateway.show-config-tip",
        show: true,
        onAction: function($event) {
            vm.openShowConfigDialog($event)
        },
        icon: "visibility"
    };

    vm.openShowConfigDialog =($event) => {
        if ($event) {
            $event.stopPropagation();
        }
        $mdDialog.show({
            controller: StateGatewayDialogController,
            controllerAs: 'vm',
            templateUrl: stateGatewayDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {
                deviceName: vm.deviceName,
                isReadOnly: vm.isReadOnly,
                isState: vm.isState
            },
            bindToController: true,
            targetEvent: $event,
            fullscreen: true,
            multiple: true
        }).then(function() {
        }, function () {
        });
    }

}


/*@ngInject*/
function StateGatewayDialogController($scope, $mdDialog, deviceName, isReadOnly, isState) {
    let vm = this;
    vm.deviceName = deviceName;
    vm.isReadOnly = isReadOnly;
    vm.isState = isState;
    vm.cancel = () => {
        $mdDialog.hide();
    };
}
