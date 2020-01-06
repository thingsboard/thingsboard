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
import $ from 'jquery';
import canvasGauges from 'canvas-gauges';
import tinycolor from 'tinycolor2';

/* eslint-disable angular/angularelement */

export default class TbAnalogueRadialGauge {
    constructor(ctx, canvasId) {

        this.ctx = ctx;

        canvasGauges.performance = window.performance; // eslint-disable-line no-undef, angular/window-service

        var gaugeElement = $('#'+canvasId, ctx.$container);

        var settings = ctx.settings;

        var minValue = settings.minValue || 0;
        var maxValue = settings.maxValue || 100;

        var dataKey = ctx.data[0].dataKey;
        var keyColor = settings.defaultColor || dataKey.color;

        var majorTicksCount = settings.majorTicksCount || 10;
        var total = maxValue-minValue;
        var step = (total/majorTicksCount);

        var valueInt = settings.valueInt || 3;

        var valueDec = getValueDec(settings);

        step = parseFloat(parseFloat(step).toFixed(valueDec));

        var majorTicks = [];
        var highlights = [];
        var tick = minValue;

        while(tick<=maxValue) {
            majorTicks.push(tick);
            var nextTick = tick+step;
            nextTick = parseFloat(parseFloat(nextTick).toFixed(valueDec));
            if (tick<maxValue) {
                var highlightColor = tinycolor(keyColor);
                var percent = (tick-minValue)/total;
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

        function getUnits(settings) {
            var dataKey;
            if (ctx.data && ctx.data[0]) {
                dataKey = ctx.data[0].dataKey;
            }
            if (dataKey && dataKey.units && dataKey.units.length) {
                return dataKey.units;
            } else {
                return angular.isDefined(settings.units) && settings.units.length > 0 ? settings.units : ctx.units;
            }
        }

        function getValueDec(settings) {
            var dataKey;
            if (ctx.data && ctx.data[0]) {
                dataKey = ctx.data[0].dataKey;
            }
            if (dataKey && angular.isDefined(dataKey.decimals)) {
                return dataKey.decimals;
            } else {
                return (angular.isDefined(settings.valueDec) && settings.valueDec !== null)
                    ? settings.valueDec : ctx.decimals;
            }
        }

        function getFontFamily(fontSettings) {
            var family = fontSettings && fontSettings.family ? fontSettings.family : 'Roboto';
            if (family === 'RobotoDraft') {
                family = 'Roboto';
            }
            return family;
        }

        var gaugeData = {

            renderTo: gaugeElement[0],

            /* Generic options */

            minValue: minValue,
            maxValue: maxValue,
            majorTicks: majorTicks,
            minorTicks: settings.minorTicks || 2,
            units: getUnits(settings),
            title: ((settings.showUnitTitle !== false) ?
                (settings.unitTitle && settings.unitTitle.length > 0 ?
                    settings.unitTitle : dataKey.label) : ''),

            borders: settings.showBorder !== false,
            borderShadowWidth: (settings.showBorder !== false) ? 3 : 0,

            // borders
            //borderOuterWidth: (settings.showBorder !== false) ? 3 : 0,
            //borderMiddleWidth: (settings.showBorder !== false) ? 3 : 0,
            //borderInnerWidth: (settings.showBorder !== false) ? 3 : 0,
            //borderShadowWidth: (settings.showBorder !== false) ? 3 : 0,

            // number formats
            valueInt: valueInt,
            valueDec: valueDec,
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
            highlightsWidth: (angular.isDefined(settings.highlightsWidth) && settings.highlightsWidth !== null) ? settings.highlightsWidth : 15,

            //fonts
            fontNumbers: getFontFamily(settings.numbersFont),
            fontTitle: getFontFamily(settings.titleFont),
            fontUnits: getFontFamily(settings.unitsFont),
            fontValue: getFontFamily(settings.valueFont),

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
            animation: settings.animation !== false && !ctx.isMobile,
            animationDuration: (angular.isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
            animationRule: settings.animationRule || 'cycle',

            /* Radial gauge specific */

            ticksAngle: settings.ticksAngle || 270,
            startAngle: settings.startAngle || 45,

            // colors

            colorNeedleCircleOuter: '#f0f0f0',
            colorNeedleCircleOuterEnd: '#ccc',
            colorNeedleCircleInner: '#e8e8e8', //tinycolor(keyColor).lighten(30).toRgbString(),//'#e8e8e8',
            colorNeedleCircleInnerEnd: '#f5f5f5',

            // needle
            needleCircleSize: settings.needleCircleSize || 10,
            needleCircleInner: true,
            needleCircleOuter: true,

            // custom animations
            animationTarget: 'needle' // 'needle' or 'plate'

        };
        this.gauge = new canvasGauges.RadialGauge(gaugeData).draw();
    }

    update() {
        if (this.ctx.data.length > 0) {
            var cellData = this.ctx.data[0];
            if (cellData.data.length > 0) {
                var tvPair = cellData.data[cellData.data.length -
                1];
                var value = tvPair[1];
                if(value !== this.gauge.value) {
                    this.gauge.value = value;
                }
            }
        }

    }

    mobileModeChanged() {
        var animation = this.ctx.settings.animation !== false && !this.ctx.isMobile;
        this.gauge.update({animation: animation});
    }

    resize() {
        this.gauge.update({width: this.ctx.width, height: this.ctx.height});
    }

    static get settingsSchema() {
        return {
            "schema": {
                "type": "object",
                "title": "Settings",
                "properties": {
                    "minValue": {
                        "title": "Minimum value",
                        "type": "number",
                        "default": 0
                    },
                    "maxValue": {
                        "title": "Maximum value",
                        "type": "number",
                        "default": 100
                    },
                    "unitTitle": {
                        "title": "Unit title",
                        "type": "string",
                        "default": null
                    },
                    "showUnitTitle": {
                        "title": "Show unit title",
                        "type": "boolean",
                        "default": true
                    },
                    "majorTicksCount": {
                        "title": "Major ticks count",
                        "type": "number",
                        "default": null
                    },
                    "minorTicks": {
                        "title": "Minor ticks count",
                        "type": "number",
                        "default": 2
                    },
                    "valueBox": {
                        "title": "Show value box",
                        "type": "boolean",
                        "default": true
                    },
                    "valueInt": {
                        "title": "Digits count for integer part of value",
                        "type": "number",
                        "default": 3
                    },
                    "defaultColor": {
                        "title": "Default color",
                        "type": "string",
                        "default": null
                    },
                    "colorPlate": {
                        "title": "Plate color",
                        "type": "string",
                        "default": "#fff"
                    },
                    "colorMajorTicks": {
                        "title": "Major ticks color",
                        "type": "string",
                        "default": "#444"
                    },
                    "colorMinorTicks": {
                        "title": "Minor ticks color",
                        "type": "string",
                        "default": "#666"
                    },
                    "colorNeedle": {
                        "title": "Needle color",
                        "type": "string",
                        "default": null
                    },
                    "colorNeedleEnd": {
                        "title": "Needle color - end gradient",
                        "type": "string",
                        "default": null
                    },
                    "colorNeedleShadowUp": {
                        "title": "Upper half of the needle shadow color",
                        "type": "string",
                        "default": "rgba(2,255,255,0.2)"
                    },
                    "colorNeedleShadowDown": {
                        "title": "Drop shadow needle color.",
                        "type": "string",
                        "default": "rgba(188,143,143,0.45)"
                    },
                    "colorValueBoxRect": {
                        "title": "Value box rectangle stroke color",
                        "type": "string",
                        "default": "#888"
                    },
                    "colorValueBoxRectEnd": {
                        "title": "Value box rectangle stroke color - end gradient",
                        "type": "string",
                        "default": "#666"
                    },
                    "colorValueBoxBackground": {
                        "title": "Value box background color",
                        "type": "string",
                        "default": "#babab2"
                    },
                    "colorValueBoxShadow": {
                        "title": "Value box shadow color",
                        "type": "string",
                        "default": "rgba(0,0,0,1)"
                    },
                    "highlights": {
                        "title": "Highlights",
                        "type": "array",
                        "items": {
                            "title": "Highlight",
                            "type": "object",
                            "properties": {
                                "from": {
                                    "title": "From",
                                    "type": "number"
                                },
                                "to": {
                                    "title": "To",
                                    "type": "number"
                                },
                                "color": {
                                    "title": "Color",
                                    "type": "string"
                                }
                            }
                        }
                    },
                    "highlightsWidth": {
                        "title": "Highlights width",
                        "type": "number",
                        "default": 15
                    },
                    "showBorder": {
                        "title": "Show border",
                        "type": "boolean",
                        "default": true
                    },
                    "numbersFont": {
                        "title": "Tick numbers font",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "Font family",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "Size",
                                "type": "number",
                                "default": 18
                            },
                            "style": {
                                "title": "Style",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "Weight",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "color",
                                "type": "string",
                                "default": null
                            }
                        }
                    },
                    "titleFont": {
                        "title": "Title text font",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "Font family",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "Size",
                                "type": "number",
                                "default": 24
                            },
                            "style": {
                                "title": "Style",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "Weight",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "color",
                                "type": "string",
                                "default": "#888"
                            }
                        }
                    },
                    "unitsFont": {
                        "title": "Units text font",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "Font family",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "Size",
                                "type": "number",
                                "default": 22
                            },
                            "style": {
                                "title": "Style",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "Weight",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "color",
                                "type": "string",
                                "default": "#888"
                            }
                        }
                    },
                    "valueFont": {
                        "title": "Value text font",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "Font family",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "Size",
                                "type": "number",
                                "default": 40
                            },
                            "style": {
                                "title": "Style",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "Weight",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "color",
                                "type": "string",
                                "default": "#444"
                            },
                            "shadowColor": {
                                "title": "Shadow color",
                                "type": "string",
                                "default": "rgba(0,0,0,0.3)"
                            }
                        }
                    },
                    "animation": {
                        "title": "Enable animation",
                        "type": "boolean",
                        "default": true
                    },
                    "animationDuration": {
                        "title": "Animation duration",
                        "type": "number",
                        "default": 500
                    },
                    "animationRule": {
                        "title": "Animation rule",
                        "type": "string",
                        "default": "cycle"
                    },
                    "startAngle": {
                        "title": "Start ticks angle",
                        "type": "number",
                        "default": 45
                    },
                    "ticksAngle": {
                        "title": "Ticks angle",
                        "type": "number",
                        "default": 270
                    },
                    "needleCircleSize": {
                        "title": "Needle circle size",
                        "type": "number",
                        "default": 10
                    }
                },
                "required": []
            },
            "form": [
                "startAngle",
                "ticksAngle",
                "needleCircleSize",
                "minValue",
                "maxValue",
                "unitTitle",
                "showUnitTitle",
                "majorTicksCount",
                "minorTicks",
                "valueBox",
                "valueInt",
                {
                    "key": "defaultColor",
                    "type": "color"
                },
                {
                    "key": "colorPlate",
                    "type": "color"
                },
                {
                    "key": "colorMajorTicks",
                    "type": "color"
                },
                {
                    "key": "colorMinorTicks",
                    "type": "color"
                },
                {
                    "key": "colorNeedle",
                    "type": "color"
                },
                {
                    "key": "colorNeedleEnd",
                    "type": "color"
                },
                {
                    "key": "colorNeedleShadowUp",
                    "type": "color"
                },
                {
                    "key": "colorNeedleShadowDown",
                    "type": "color"
                },
                {
                    "key": "colorValueBoxRect",
                    "type": "color"
                },
                {
                    "key": "colorValueBoxRectEnd",
                    "type": "color"
                },
                {
                    "key": "colorValueBoxBackground",
                    "type": "color"
                },
                {
                    "key": "colorValueBoxShadow",
                    "type": "color"
                },
                {
                    "key": "highlights",
                    "items": [
                        "highlights[].from",
                        "highlights[].to",
                        {
                            "key": "highlights[].color",
                            "type": "color"
                        }
                    ]
                },
                "highlightsWidth",
                "showBorder",
                {
                    "key": "numbersFont",
                    "items": [
                        "numbersFont.family",
                        "numbersFont.size",
                        {
                            "key": "numbersFont.style",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "italic",
                                    "label": "Italic"
                                },
                                {
                                    "value": "oblique",
                                    "label": "Oblique"
                                }
                            ]
                        },
                        {
                            "key": "numbersFont.weight",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "bold",
                                    "label": "Bold"
                                },
                                {
                                    "value": "bolder",
                                    "label": "Bolder"
                                },
                                {
                                    "value": "lighter",
                                    "label": "Lighter"
                                },
                                {
                                    "value": "100",
                                    "label": "100"
                                },
                                {
                                    "value": "200",
                                    "label": "200"
                                },
                                {
                                    "value": "300",
                                    "label": "300"
                                },
                                {
                                    "value": "400",
                                    "label": "400"
                                },
                                {
                                    "value": "500",
                                    "label": "500"
                                },
                                {
                                    "value": "600",
                                    "label": "600"
                                },
                                {
                                    "value": "700",
                                    "label": "800"
                                },
                                {
                                    "value": "800",
                                    "label": "800"
                                },
                                {
                                    "value": "900",
                                    "label": "900"
                                }
                            ]
                        },
                        {
                            "key": "numbersFont.color",
                            "type": "color"
                        }
                    ]
                },
                {
                    "key": "titleFont",
                    "items": [
                        "titleFont.family",
                        "titleFont.size",
                        {
                            "key": "titleFont.style",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "italic",
                                    "label": "Italic"
                                },
                                {
                                    "value": "oblique",
                                    "label": "Oblique"
                                }
                            ]
                        },
                        {
                            "key": "titleFont.weight",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "bold",
                                    "label": "Bold"
                                },
                                {
                                    "value": "bolder",
                                    "label": "Bolder"
                                },
                                {
                                    "value": "lighter",
                                    "label": "Lighter"
                                },
                                {
                                    "value": "100",
                                    "label": "100"
                                },
                                {
                                    "value": "200",
                                    "label": "200"
                                },
                                {
                                    "value": "300",
                                    "label": "300"
                                },
                                {
                                    "value": "400",
                                    "label": "400"
                                },
                                {
                                    "value": "500",
                                    "label": "500"
                                },
                                {
                                    "value": "600",
                                    "label": "600"
                                },
                                {
                                    "value": "700",
                                    "label": "800"
                                },
                                {
                                    "value": "800",
                                    "label": "800"
                                },
                                {
                                    "value": "900",
                                    "label": "900"
                                }
                            ]
                        },
                        {
                            "key": "titleFont.color",
                            "type": "color"
                        }
                    ]
                },
                {
                    "key": "unitsFont",
                    "items": [
                        "unitsFont.family",
                        "unitsFont.size",
                        {
                            "key": "unitsFont.style",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "italic",
                                    "label": "Italic"
                                },
                                {
                                    "value": "oblique",
                                    "label": "Oblique"
                                }
                            ]
                        },
                        {
                            "key": "unitsFont.weight",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "bold",
                                    "label": "Bold"
                                },
                                {
                                    "value": "bolder",
                                    "label": "Bolder"
                                },
                                {
                                    "value": "lighter",
                                    "label": "Lighter"
                                },
                                {
                                    "value": "100",
                                    "label": "100"
                                },
                                {
                                    "value": "200",
                                    "label": "200"
                                },
                                {
                                    "value": "300",
                                    "label": "300"
                                },
                                {
                                    "value": "400",
                                    "label": "400"
                                },
                                {
                                    "value": "500",
                                    "label": "500"
                                },
                                {
                                    "value": "600",
                                    "label": "600"
                                },
                                {
                                    "value": "700",
                                    "label": "800"
                                },
                                {
                                    "value": "800",
                                    "label": "800"
                                },
                                {
                                    "value": "900",
                                    "label": "900"
                                }
                            ]
                        },
                        {
                            "key": "unitsFont.color",
                            "type": "color"
                        }
                    ]
                },
                {
                    "key": "valueFont",
                    "items": [
                        "valueFont.family",
                        "valueFont.size",
                        {
                            "key": "valueFont.style",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "italic",
                                    "label": "Italic"
                                },
                                {
                                    "value": "oblique",
                                    "label": "Oblique"
                                }
                            ]
                        },
                        {
                            "key": "valueFont.weight",
                            "type": "rc-select",
                            "multiple": false,
                            "items": [
                                {
                                    "value": "normal",
                                    "label": "Normal"
                                },
                                {
                                    "value": "bold",
                                    "label": "Bold"
                                },
                                {
                                    "value": "bolder",
                                    "label": "Bolder"
                                },
                                {
                                    "value": "lighter",
                                    "label": "Lighter"
                                },
                                {
                                    "value": "100",
                                    "label": "100"
                                },
                                {
                                    "value": "200",
                                    "label": "200"
                                },
                                {
                                    "value": "300",
                                    "label": "300"
                                },
                                {
                                    "value": "400",
                                    "label": "400"
                                },
                                {
                                    "value": "500",
                                    "label": "500"
                                },
                                {
                                    "value": "600",
                                    "label": "600"
                                },
                                {
                                    "value": "700",
                                    "label": "800"
                                },
                                {
                                    "value": "800",
                                    "label": "800"
                                },
                                {
                                    "value": "900",
                                    "label": "900"
                                }
                            ]
                        },
                        {
                            "key": "valueFont.color",
                            "type": "color"
                        },
                        {
                            "key": "valueFont.shadowColor",
                            "type": "color"
                        }
                    ]
                },
                "animation",
                "animationDuration",
                {
                    "key": "animationRule",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "linear",
                            "label": "Linear"
                        },
                        {
                            "value": "quad",
                            "label": "Quad"
                        },
                        {
                            "value": "quint",
                            "label": "Quint"
                        },
                        {
                            "value": "cycle",
                            "label": "Cycle"
                        },
                        {
                            "value": "bounce",
                            "label": "Bounce"
                        },
                        {
                            "value": "elastic",
                            "label": "Elastic"
                        },
                        {
                            "value": "dequad",
                            "label": "Dequad"
                        },
                        {
                            "value": "dequint",
                            "label": "Dequint"
                        },
                        {
                            "value": "decycle",
                            "label": "Decycle"
                        },
                        {
                            "value": "debounce",
                            "label": "Debounce"
                        },
                        {
                            "value": "delastic",
                            "label": "Delastic"
                        }
                    ]
                }
            ]
        };
    }

}

/* eslint-enable angular/angularelement */
