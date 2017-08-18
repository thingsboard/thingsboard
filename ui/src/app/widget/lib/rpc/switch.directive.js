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

import './switch.scss';

/* eslint-disable import/no-unresolved, import/default */

import switchTemplate from './switch.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.rpc.switch', [])
    .directive('tbSwitch', Switch)
    .name;

/*@ngInject*/
function Switch() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: SwitchController,
        controllerAs: 'vm',
        templateUrl: switchTemplate
    };
}

/*@ngInject*/
function SwitchController($element, $scope) {
    let vm = this;

    vm.showTitle = false;
    vm.value = false;
    vm.error = '';

    var switchElement = angular.element('.switch', $element),
        switchContainer = angular.element('#switch-container', $element),
        mdSwitch = angular.element('md-switch', switchElement),
        onoffContainer = angular.element('.onoff-container', $element),
        onLabel = angular.element('.on-label', $element),
        offLabel = angular.element('.off-label', $element),
        switchTitleContainer = angular.element('.title-container', $element),
        switchTitle = angular.element('.switch-title', $element),
        textMeasure = angular.element('#text-measure', $element),
        switchErrorContainer = angular.element('.error-container', $element),
        switchError = angular.element('.switch-error', $element);


    vm.onValue = onValue;

    $scope.$watch('vm.ctx', () => {
        if (vm.ctx) {
            init();
        }
    });

    function init() {

        vm.title = angular.isDefined(vm.ctx.settings.title) ? vm.ctx.settings.title : '';
        vm.showTitle = vm.title && vm.title.length ? true : false;
        vm.showOnOffLabels = angular.isDefined(vm.ctx.settings.showOnOffLabels) ? vm.ctx.settings.showOnOffLabels : true;
        vm.ctx.resize = resize;
        $scope.$applyAsync(() => {
            resize();
        });
        var initialValue = angular.isDefined(vm.ctx.settings.initialValue) ? vm.ctx.settings.initialValue : false;
        setValue(initialValue);

        var subscription = vm.ctx.defaultSubscription;
        var rpcEnabled = subscription.rpcEnabled;

        vm.isSimulated = $scope.widgetEditMode;

        vm.requestTimeout = 500;
        if (vm.ctx.settings.requestTimeout) {
            vm.requestTimeout = vm.ctx.settings.requestTimeout;
        }
        vm.getValueMethod = 'getValue';
        if (vm.ctx.settings.getValueMethod && vm.ctx.settings.getValueMethod.length) {
            vm.getValueMethod = vm.ctx.settings.getValueMethod;
        }
        vm.setValueMethod = 'setValue';
        if (vm.ctx.settings.setValueMethod && vm.ctx.settings.setValueMethod.length) {
            vm.setValueMethod = vm.ctx.settings.setValueMethod;
        }
        if (!rpcEnabled) {
            onError('Target device is not set!');
        } else {
            if (!vm.isSimulated) {
                rpcRequestValue();
            }
        }
    }

    const switchAspectRation = 2.7893;

    function resize() {
        var width = switchContainer.width();
        var height;
        if (vm.showOnOffLabels) {
            height = switchContainer.height()*2/3;
        } else {
            height = switchContainer.height();
        }
        var ratio = width/height;
        if (ratio > switchAspectRation) {
            width = height*switchAspectRation;
        } else {
            height = width/switchAspectRation;
        }
        switchElement.css({width: width, height: height});
        mdSwitch.css('height', height+'px');
        mdSwitch.css('width', width+'px');
        mdSwitch.css('min-width', width+'px');
        angular.element('.md-container', mdSwitch).css('height', height+'px');
        angular.element('.md-container', mdSwitch).css('width', width+'px');


        if (vm.showTitle) {
            setFontSize(switchTitle, vm.title, switchTitleContainer.height() * 2 / 3, switchTitleContainer.width());
        }

        if (vm.showOnOffLabels) {
            onoffContainer.css({width: width, height: switchContainer.height() / 3});
            setFontSize(onLabel, 'OFF', onoffContainer.height(), onoffContainer.width() / 2);
            setFontSize(offLabel, 'OFF', onoffContainer.height(), onoffContainer.width() / 2);
        }

        setFontSize(switchError, vm.error, switchErrorContainer.height(), switchErrorContainer.width());
    }

    function setValue(value) {
        vm.value = value ? true : false;
    }

    function onValue() {
        rpcUpdateValue(vm.value);
    }

    function onError(error) {
        $scope.$applyAsync(() => {
            vm.error = error;
            setFontSize(switchError, vm.error, switchErrorContainer.height(), switchErrorContainer.width());
        });
    }

    function setFontSize(element, text, fontSize, maxWidth) {
        var textWidth = measureTextWidth(text, fontSize);
        while (textWidth > maxWidth) {
            fontSize--;
            textWidth = measureTextWidth(text, fontSize);
        }
        element.css({'fontSize': fontSize+'px', 'lineHeight': fontSize+'px'});
    }

    function measureTextWidth(text, fontSize) {
        textMeasure.css({'fontSize': fontSize+'px', 'lineHeight': fontSize+'px'});
        textMeasure.text(text);
        return textMeasure.width();
    }

    function rpcRequestValue() {
        vm.error = '';
        vm.ctx.controlApi.sendTwoWayCommand(vm.getValueMethod, null, vm.requestTimeout).then(
            (responseBody) => {
                setValue(responseBody);
            },
            () => {
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
            }
        );
    }

    function rpcUpdateValue(value) {
        if (vm.executingUpdateValue) {
            vm.scheduledValue = value;
            return;
        } else {
            vm.scheduledValue = null;
            vm.rpcValue = value;
            vm.executingUpdateValue = true;
        }
        vm.error = '';
        vm.ctx.controlApi.sendOneWayCommand(vm.setValueMethod, value, vm.requestTimeout).then(
            () => {
                vm.executingUpdateValue = false;
                if (vm.scheduledValue != null && vm.scheduledValue != vm.rpcValue) {
                    rpcUpdateValue(vm.scheduledValue);
                }
            },
            () => {
                vm.executingUpdateValue = false;
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
            }
        );
    }
}
