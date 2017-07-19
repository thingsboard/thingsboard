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

import './shiny-knob.scss';

import CanvasDigitalGauge from './../CanvasDigitalGauge';

/* eslint-disable import/no-unresolved, import/default */

import shinyKnobTemplate from './shiny-knob.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.widgets.rpc.shinyKnob', [])
    .directive('tbShinyKnob', ShinyKnob)
    .name;

/*@ngInject*/
function ShinyKnob() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '='
        },
        controller: ShinyKnobController,
        controllerAs: 'vm',
        templateUrl: shinyKnobTemplate
    };
}

/*@ngInject*/
function ShinyKnobController($element, $scope, $document) {
    let vm = this;

    vm.value = 0;

    var snap = 0;

    var knob = angular.element('.knob', $element),
        knobContainer = angular.element('#knob-container', $element),
       // knobTop = knob.find('.top'),
        knobTopPointerContainer = knob.find('.top-pointer-container'),
        knobTopPointer = knob.find('.top-pointer'),
        knobValueBackground = knob.find('.value-background'),
        knobValue = knob.find('.knob-value'),
        startDeg = -1,
        currentDeg = 0,
        rotation = 0,
        lastDeg = 0;

    var canvasBarElement = angular.element('#canvasBar', $element);

    var levelColors = ['rgb(0, 128, 0)', 'rgb(251, 192, 45)', 'rgb(255, 0, 0)'];//['cyan'];//
    var canvasBar;

    $scope.$watch('vm.ctx', () => {
        if (vm.ctx) {
            init();
        }
    });

    function init() {

        vm.minValue = angular.isDefined(vm.ctx.settings.minValue) ? vm.ctx.settings.minValue : 0;
        vm.maxValue = angular.isDefined(vm.ctx.settings.maxValue) ? vm.ctx.settings.maxValue : 100;

        vm.darkTheme = vm.ctx.settings.theme == 'dark';

        var canvasBarData = {
            renderTo: canvasBarElement[0],
            hideValue: true,
            neonGlowBrightness: 40,//40,//vm.darkTheme ? 40 : 0,
            gaugeWidthScale: 0.2,
            gaugeColor: vm.darkTheme ? 'rgb(23, 26, 28)' : 'rgba(0, 0, 0, 0.75)',
            levelColors: levelColors,
            minValue: vm.minValue,
            maxValue: vm.maxValue,
            gaugeType: 'donut',
            dashThickness: 0,//1.5,
            donutStartAngle: Math.PI,
            animation: false,
            animationDuration: 250,
            animationRule: 'linear'
        };

        canvasBar = new CanvasDigitalGauge(canvasBarData).draw();

        knob.on('mousedown touchstart', (e) => {
            e.preventDefault();
            var offset = knob.offset();
            var center = {
                y : offset.top + knob.height()/2,
                x: offset.left + knob.width()/2
            };

            var a, b, deg, tmp,
                rad2deg = 180/Math.PI;

            knob.on('mousemove.rem touchmove.rem', (e) => {

                e = (e.originalEvent.touches) ? e.originalEvent.touches[0] : e;

                a = center.y - e.pageY;
                b = center.x - e.pageX;
                deg = Math.atan2(a,b)*rad2deg;

                if(deg<0){
                    deg = 360 + deg;
                }

                if(startDeg == -1){
                    startDeg = deg;
                }

                tmp = Math.floor((deg-startDeg) + rotation);

                if(tmp < 0){
                    tmp = 360 + tmp;
                }
                else if(tmp > 359){
                    tmp = tmp % 360;
                }

                if(snap && tmp < snap){
                    tmp = 0;
                }
                if(Math.abs(tmp - lastDeg) > 180){
                    return false;
                }
                currentDeg = tmp;
                lastDeg = tmp;

                knobTopPointerContainer.css('transform','rotate('+(currentDeg)+'deg)');
                turn(currentDeg/359);
            });

            $document.on('mouseup.rem  touchend.rem',() => {
                knob.off('.rem');
                $document.off('.rem');
                rotation = currentDeg;
                startDeg = -1;
            });

        });

        vm.ctx.resize = resize;
        resize();

        var initialValue = angular.isDefined(vm.ctx.settings.initialValue) ? vm.ctx.settings.initialValue : vm.minValue;

        setValue(initialValue);
    }

    function resize() {
        var width = knobContainer.width();
        var height = knobContainer.height();
        var size = Math.min(width, height);
        knob.css({width: size, height: size});
        canvasBar.update({width: size, height: size});
        var valHeight = knobValueBackground.height()/3.3;
        knobValue.css({'fontSize': valHeight+'px', 'lineHeight': valHeight+'px'});
    }

    function turn(ratio) {
        var value = (vm.minValue + (vm.maxValue - vm.minValue)*ratio).toFixed(2);
        if (canvasBar.value != value) {
            canvasBar.value = value;
        }
        knobTopPointer.css({'backgroundColor': canvasBar.getValueColor()});
        knobValue.css({'color': 'cyan'});//canvasBar.getValueColor()});
        onValue(value);
    }

    function setValue(value) {
        var ratio = (value-vm.minValue) / (vm.maxValue - vm.minValue);
        rotation = lastDeg = currentDeg = ratio*360;
        knobTopPointerContainer.css('transform','rotate('+(currentDeg)+'deg)');
        if (canvasBar.value != value) {
            canvasBar.value = value;
        }
        knobTopPointer.css({'backgroundColor': canvasBar.getValueColor()});
        knobValue.css({'color': 'cyan'});//canvasBar.getValueColor()});
        vm.value = value;
    }

    function onValue(value) {
        console.log(`onValue ${value}`); //eslint-disable-line
        $scope.$applyAsync(() => {
            vm.value = value;
        });
    }

}