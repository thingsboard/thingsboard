/*
 * Copyright © 2016 The Thingsboard Authors
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
import $ from 'jquery';
import canvasGauges from 'canvas-gauges';
import tinycolor from 'tinycolor2';

/* eslint-disable angular/angularelement */
export default class TbAnalogueLinearGauge {
    constructor(containerElement, settings, data, canvasId) {

        canvasGauges.performance = window.performance; // eslint-disable-line no-undef, angular/window-service

        var gaugeElement = $('#'+canvasId, containerElement);

        var minValue = settings.minValue || 0;
        var maxValue = settings.maxValue || 100;

        var dataKey = data[0].dataKey;
        var keyColor = settings.defaultColor || dataKey.color;

        var majorTicksCount = settings.majorTicksCount || 10;
        var total = maxValue-minValue;
        var step = (total/majorTicksCount);
        step = parseFloat(parseFloat(step).toPrecision(12));

        var majorTicks = [];
        var highlights = [];
        var tick = 0;

        while(tick<=total) {
            var majorTick = tick + minValue;
            majorTicks.push(majorTick);
            var nextTick = tick+step;
            nextTick = parseFloat(parseFloat(nextTick).toPrecision(12));
            if (tick<total) {
                var highlightColor = tinycolor(keyColor);
                var percent = tick/total;
                highlightColor.setAlpha(percent);
                var highlight = {
                    from: tick,
                    to: nextTick,
                    color: highlightColor.toRgbString()
                }
                highlights.push(highlight);
            }
            tick = nextTick;
        }

        var colorNumbers = tinycolor(keyColor).darken(20).toRgbString();
        var barStrokeColor = tinycolor(keyColor).darken().setAlpha(0.6).toRgbString();

        var progressColorStart = tinycolor(keyColor).setAlpha(0.05).toRgbString();
        var progressColorEnd = tinycolor(keyColor).darken().toRgbString();

        var gaugeData = {

            renderTo: gaugeElement[0],

            /* Generic options */

            minValue: minValue,
            maxValue: maxValue,
            majorTicks: majorTicks,
            minorTicks: settings.minorTicks || 2,
            units: settings.units,
            title: ((settings.showUnitTitle !== false) ?
                (settings.unitTitle && settings.unitTitle.length > 0 ?
                    settings.unitTitle : dataKey.label) : ''),

            borders: settings.showBorder === true,
            borderShadowWidth: (settings.showBorder === true) ? 3 : 0,
            borderOuterWidth: (settings.showBorder === true) ? 3 : 0,
            borderMiddleWidth: (settings.showBorder === true) ? 3 : 0,
            borderInnerWidth: (settings.showBorder === true) ? 3 : 0,

            // borders

            // number formats
            valueInt: settings.valueInt || 3,
            valueDec: (angular.isDefined(settings.valueDec) && settings.valueDec !== null)
                ? settings.valueDec : 2,
            majorTicksInt: 1,
            majorTicksDec: 0,

            valueBox: settings.valueBox !== false,
            valueBoxStroke: 5,
            valueBoxWidth: 0,
            valueText: '',
            valueTextShadow: true,
            valueBoxBorderRadius: 2.5,

            //highlights
            highlights: (settings.highlights && settings.highlights.length > 0) ? settings.highlights : highlights,
            highlightsWidth: (angular.isDefined(settings.highlightsWidth) && settings.highlightsWidth !== null) ? settings.highlightsWidth : 10,

            //fonts
            fontNumbers: settings.numbersFont && settings.numbersFont.family ? settings.numbersFont.family : 'RobotoDraft',
            fontTitle: settings.titleFont && settings.titleFont.family ? settings.titleFont.family : 'RobotoDraft',
            fontUnits: settings.unitsFont && settings.unitsFont.family ? settings.unitsFont.family : 'RobotoDraft',
            fontValue: settings.valueFont && settings.valueFont.family ? settings.valueFont.family : 'RobotoDraft',

            fontNumbersSize: settings.numbersFont && settings.numbersFont.size ? settings.numbersFont.size : 18,
            fontTitleSize: settings.titleFont && settings.titleFont.size ? settings.titleFont.size : 24,
            fontUnitsSize: settings.unitsFont && settings.unitsFont.size ? settings.unitsFont.size : 22,
            fontValueSize: settings.valueFont && settings.valueFont.size ? settings.valueFont.size : 40,

            fontNumbersStyle: settings.numbersFont && settings.numbersFont.style ? settings.numbersFont.style : 'normal',
            fontTitleStyle: settings.titleFont && settings.titleFont.style ? settings.titleFont.style : 'normal',
            fontUnitsStyle: settings.unitsFont && settings.unitsFont.style ? settings.unitsFont.style : 'normal',
            fontValueStyle: settings.valueFont && settings.valueFont.style ? settings.valueFont.style : 'normal',

            fontNumbersWeight: settings.numbersFont && settings.numbersFont.weight ? settings.numbersFont.weight : '500',
            fontTitleWeight: settings.titleFont && settings.titleFont.weight ? settings.titleFont.weight : '500',
            fontUnitsWeight: settings.unitsFont && settings.unitsFont.weight ? settings.unitsFont.weight : '500',
            fontValueWeight: settings.valueFont && settings.valueFont.weight ? settings.valueFont.weight : '500',

            colorNumbers: settings.numbersFont && settings.numbersFont.color ? settings.numbersFont.color : colorNumbers,
            colorTitle: settings.titleFont && settings.titleFont.color ? settings.titleFont.color : '#888',
            colorUnits: settings.unitsFont && settings.unitsFont.color ? settings.unitsFont.color : '#888',
            colorValueText: settings.valueFont && settings.valueFont.color ? settings.valueFont.color : '#444',
            colorValueTextShadow: settings.valueFont && settings.valueFont.shadowColor ? settings.valueFont.shadowColor : 'rgba(0,0,0,0.3)',

            //colors
            colorPlate: settings.colorPlate || '#fff',
            colorMajorTicks: settings.colorMajorTicks || '#444',
            colorMinorTicks: settings.colorMinorTicks || '#666',
            colorNeedle: settings.colorNeedle || keyColor,
            colorNeedleEnd: settings.colorNeedleEnd || keyColor,

            colorValueBoxRect: settings.colorValueBoxRect || '#888',
            colorValueBoxRectEnd: settings.colorValueBoxRectEnd || '#666',
            colorValueBoxBackground: settings.colorValueBoxBackground || '#babab2',
            colorValueBoxShadow: settings.colorValueBoxShadow || 'rgba(0,0,0,1)',
            colorNeedleShadowUp: settings.colorNeedleShadowUp || 'rgba(2,255,255,0.2)',
            colorNeedleShadowDown: settings.colorNeedleShadowDown || 'rgba(188,143,143,0.45)',

            // animations
            animation: settings.animation !== false,
            animationDuration: (angular.isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
            animationRule: settings.animationRule || 'cycle',

            /* Linear gauge specific */

            barStrokeWidth: (angular.isDefined(settings.barStrokeWidth) && settings.barStrokeWidth !== null) ? settings.barStrokeWidth : 2.5,
            colorBarStroke: settings.colorBarStroke || barStrokeColor,
            colorBar: settings.colorBar || "#fff",
            colorBarEnd: settings.colorBarEnd || "#ddd",
            colorBarProgress: settings.colorBarProgress || progressColorStart,
            colorBarProgressEnd: settings.colorBarProgressEnd || progressColorEnd
        };
        this.gauge = new canvasGauges.LinearGauge(gaugeData).draw();
    }

    redraw(width, height, data, sizeChanged) {
        if (sizeChanged) {
            this.gauge.update({width: width, height: height});
        }

        if (data.length > 0) {
            var cellData = data[0];
            if (cellData.data.length > 0) {
                var tvPair = cellData.data[cellData.data.length -
                1];
                var value = tvPair[1];
                this.gauge.value = value;
            }
        }
    }
}

/* eslint-enable angular/angularelement */
