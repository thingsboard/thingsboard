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

import './led-indicator.scss';

import tinycolor from 'tinycolor2';

/* eslint-disable import/no-unresolved, import/default */

import ledIndicatorTemplate from './led-indicator.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.rpc.ledIndicator', [])
    .directive('tbLedIndicator', LedIndicator)
    .name;

/*@ngInject*/
function LedIndicator() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: LedIndicatorController,
        controllerAs: 'vm',
        templateUrl: ledIndicatorTemplate
    };
}

/*@ngInject*/
function LedIndicatorController($element, $scope, $timeout) {
    let vm = this;

    vm.showTitle = false;
    vm.value = false;
    vm.error = '';

    var led = angular.element('.led', $element),
        ledContainer = angular.element('#led-container', $element),
        textMeasure = angular.element('#text-measure', $element),
        ledTitleContainer = angular.element('.title-container', $element),
        ledTitle = angular.element('.led-title', $element),
        ledErrorContainer = angular.element('.error-container', $element),
        ledError = angular.element('.led-error', $element);

    $scope.$watch('vm.ctx', () => {
        if (vm.ctx) {
            init();
        }
    });

    $scope.$on('$destroy', () => {
        vm.destroyed = true;
        if (vm.requestValueTimeoutHandle) {
            $timeout.cancel(vm.requestValueTimeoutHandle);
        }
    });

    resize();

    function init() {

        vm.title = angular.isDefined(vm.ctx.settings.title) ? vm.ctx.settings.title : '';
        vm.showTitle = vm.title && vm.title.length ? true : false;

        var origColor = angular.isDefined(vm.ctx.settings.ledColor) ? vm.ctx.settings.ledColor : 'green';

        vm.ledColor = tinycolor(origColor).brighten(30).toHexString();
        vm.ledMiddleColor = tinycolor(origColor).toHexString();
        vm.disabledColor = tinycolor(origColor).darken(40).toHexString();
        vm.disabledMiddleColor = tinycolor(origColor).darken(60).toHexString();

        vm.ctx.resize = resize;
        $scope.$applyAsync(() => {
            resize();
        });
        var initialValue = angular.isDefined(vm.ctx.settings.initialValue) ? vm.ctx.settings.initialValue : false;
        setValue(initialValue, true);

        var subscription = vm.ctx.defaultSubscription;
        var rpcEnabled = subscription.rpcEnabled;

        vm.isSimulated = $scope.widgetEditMode;

        vm.requestTimeout = 500;
        if (vm.ctx.settings.requestTimeout) {
            vm.requestTimeout = vm.ctx.settings.requestTimeout;
        }
        vm.valuePollingInterval = 500;
        if (vm.ctx.settings.valuePollingInterval) {
            vm.valuePollingInterval = vm.ctx.settings.valuePollingInterval;
        }
        vm.getValueMethod = 'getValue';
        if (vm.ctx.settings.getValueMethod && vm.ctx.settings.getValueMethod.length) {
            vm.getValueMethod = vm.ctx.settings.getValueMethod;
        }
        if (!rpcEnabled) {
            onError('Target device is not set!');
        } else {
            if (!vm.isSimulated) {
                rpcRequestValue();
            }
        }
    }

    function resize() {
        var width = ledContainer.width();
        var height = ledContainer.height();
        var size = Math.min(width, height);

        led.css({width: size, height: size});

        if (vm.showTitle) {
            setFontSize(ledTitle, vm.title, ledTitleContainer.height() * 2 / 3, ledTitleContainer.width());
        }
        setFontSize(ledError, vm.error, ledErrorContainer.height(), ledErrorContainer.width());
    }

    function setValue(value, forceUpdate) {
        if (vm.value != value || forceUpdate) {
            vm.value = value;
            updateColor();
        }
    }

    function updateColor() {
        var color = vm.value ? vm.ledColor : vm.disabledColor;
        var middleColor = vm.value ? vm.ledMiddleColor : vm.disabledMiddleColor;
        var boxShadow = `#000 0 -1px 6px 1px, inset ${middleColor} 0 -1px 8px, ${color} 0 3px 11px`;
        led.css({'backgroundColor': color});
        led.css({'boxShadow': boxShadow});
        if (vm.value) {
            led.removeClass( 'disabled' );
        } else {
            led.addClass( 'disabled' );
        }
    }

    function onError(error) {
        $scope.$applyAsync(() => {
            vm.error = error;
            setFontSize(ledError, vm.error, ledErrorContainer.height(), ledErrorContainer.width());
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
        if (vm.destroyed) {
            return;
        }
        vm.error = '';
        vm.ctx.controlApi.sendTwoWayCommand(vm.getValueMethod, null, vm.requestTimeout).then(
            (responseBody) => {
                var newValue = responseBody ? true : false;
                setValue(newValue);
                if (vm.requestValueTimeoutHandle) {
                    $timeout.cancel(vm.requestValueTimeoutHandle);
                }
                vm.requestValueTimeoutHandle = $timeout(rpcRequestValue, vm.valuePollingInterval);
            },
            () => {
                var errorText = vm.ctx.defaultSubscription.rpcErrorText;
                onError(errorText);
                if (vm.requestValueTimeoutHandle) {
                    $timeout.cancel(vm.requestValueTimeoutHandle);
                }
                vm.requestValueTimeoutHandle = $timeout(rpcRequestValue, vm.valuePollingInterval);
            }
        );
    }

}
