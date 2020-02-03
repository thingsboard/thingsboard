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
                "title": "设置",
                "properties": {
                    "minValue": {
                        "title": "最小值",
                        "type": "number",
                        "default": 0
                    },
                    "maxValue": {
                        "title": "最大值",
                        "type": "number",
                        "default": 100
                    },
                    "unitTitle": {
                        "title": "单位",
                        "type": "string",
                        "default": null
                    },
                    "showUnitTitle": {
                        "title": "显示单位",
                        "type": "boolean",
                        "default": true
                    },
                    "majorTicksCount": {
                        "title": "主刻度个数",
                        "type": "number",
                        "default": null
                    },
                    "minorTicks": {
                        "title": "次刻度个数",
                        "type": "number",
                        "default": 2
                    },
                    "valueBox": {
                        "title": "显示值框",
                        "type": "boolean",
                        "default": true
                    },
                    "valueInt": {
                        "title": "值整数部分的数字位数",
                        "type": "number",
                        "default": 3
                    },
                    "defaultColor": {
                        "title": "缺省颜色",
                        "type": "string",
                        "default": null
                    },
                    "colorPlate": {
                        "title": "表盘颜色",
                        "type": "string",
                        "default": "#fff"
                    },
                    "colorMajorTicks": {
                        "title": "主刻度颜色",
                        "type": "string",
                        "default": "#444"
                    },
                    "colorMinorTicks": {
                        "title": "次刻度颜色",
                        "type": "string",
                        "default": "#666"
                    },
                    "colorNeedle": {
                        "title": "指针颜色",
                        "type": "string",
                        "default": null
                    },
                    "colorNeedleEnd": {
                        "title": "指针颜色 - 渐变到针头",
                        "type": "string",
                        "default": null
                    },
                    "colorNeedleShadowUp": {
                        "title": "指针阴影上半部分颜色",
                        "type": "string",
                        "default": "rgba(2,255,255,0.2)"
                    },
                    "colorNeedleShadowDown": {
                        "title": "指针投影颜色.",
                        "type": "string",
                        "default": "rgba(188,143,143,0.45)"
                    },
                    "colorValueBoxRect": {
                        "title": "值框边框颜色",
                        "type": "string",
                        "default": "#888"
                    },
                    "colorValueBoxRectEnd": {
                        "title": "值框边框颜色 - 渐变结束",
                        "type": "string",
                        "default": "#666"
                    },
                    "colorValueBoxBackground": {
                        "title": "值框背景色",
                        "type": "string",
                        "default": "#babab2"
                    },
                    "colorValueBoxShadow": {
                        "title": "值框阴影颜色",
                        "type": "string",
                        "default": "rgba(0,0,0,1)"
                    },
                    "highlights": {
                        "title": "高亮",
                        "type": "array",
                        "items": {
                            "title": "高亮",
                            "type": "object",
                            "properties": {
                                "from": {
                                    "title": "从",
                                    "type": "number"
                                },
                                "to": {
                                    "title": "到",
                                    "type": "number"
                                },
                                "color": {
                                    "title": "颜色",
                                    "type": "string"
                                }
                            }
                        }
                    },
                    "highlightsWidth": {
                        "title": "高亮宽度",
                        "type": "number",
                        "default": 15
                    },
                    "showBorder": {
                        "title": "显示边框",
                        "type": "boolean",
                        "default": true
                    },
                    "numbersFont": {
                        "title": "刻度数字字体",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "字体",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "大小",
                                "type": "number",
                                "default": 18
                            },
                            "style": {
                                "title": "样式",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "权重",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "颜色",
                                "type": "string",
                                "default": null
                            }
                        }
                    },
                    "titleFont": {
                        "title": "标题字体",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "字体",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "大小",
                                "type": "number",
                                "default": 24
                            },
                            "style": {
                                "title": "样式",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "权重",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "颜色",
                                "type": "string",
                                "default": "#888"
                            }
                        }
                    },
                    "unitsFont": {
                        "title": "单位字体",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "字体",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "大小",
                                "type": "number",
                                "default": 22
                            },
                            "style": {
                                "title": "样式",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "权重",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "颜色",
                                "type": "string",
                                "default": "#888"
                            }
                        }
                    },
                    "valueFont": {
                        "title": "数值字体",
                        "type": "object",
                        "properties": {
                            "family": {
                                "title": "字体",
                                "type": "string",
                                "default": "Roboto"
                            },
                            "size": {
                                "title": "大小",
                                "type": "number",
                                "default": 40
                            },
                            "style": {
                                "title": "样式",
                                "type": "string",
                                "default": "normal"
                            },
                            "weight": {
                                "title": "权重",
                                "type": "string",
                                "default": "500"
                            },
                            "color": {
                                "title": "颜色",
                                "type": "string",
                                "default": "#444"
                            },
                            "shadowColor": {
                                "title": "阴影颜色",
                                "type": "string",
                                "default": "rgba(0,0,0,0.3)"
                            }
                        }
                    },
                    "animation": {
                        "title": "启用动画",
                        "type": "boolean",
                        "default": true
                    },
                    "animationDuration": {
                        "title": "动画持续毫秒",
                        "type": "number",
                        "default": 500
                    },
                    "animationRule": {
                        "title": "动画规则",
                        "type": "string",
                        "default": "cycle"
                    },
                    "startAngle": {
                        "title": "开始刻度角",
                        "type": "number",
                        "default": 45
                    },
                    "ticksAngle": {
                        "title": "刻度角",
                        "type": "number",
                        "default": 270
                    },
                    "needleCircleSize": {
                        "title": "指针环大小",
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
                                    "label": "常规"
                                },
                                {
                                    "value": "italic",
                                    "label": "斜体"
                                },
                                {
                                    "value": "oblique",
                                    "label": "仿斜体"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "bold",
                                    "label": "粗体"
                                },
                                {
                                    "value": "bolder",
                                    "label": "更粗"
                                },
                                {
                                    "value": "lighter",
                                    "label": "更细"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "italic",
                                    "label": "斜体"
                                },
                                {
                                    "value": "oblique",
                                    "label": "仿斜体"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "bold",
                                    "label": "粗体"
                                },
                                {
                                    "value": "bolder",
                                    "label": "更粗"
                                },
                                {
                                    "value": "lighter",
                                    "label": "更细"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "italic",
                                    "label": "斜体"
                                },
                                {
                                    "value": "oblique",
                                    "label": "仿斜体"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "bold",
                                    "label": "粗体"
                                },
                                {
                                    "value": "bolder",
                                    "label": "更粗"
                                },
                                {
                                    "value": "lighter",
                                    "label": "更细"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "italic",
                                    "label": "斜体"
                                },
                                {
                                    "value": "oblique",
                                    "label": "仿斜体"
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
                                    "label": "常规"
                                },
                                {
                                    "value": "bold",
                                    "label": "粗体"
                                },
                                {
                                    "value": "bolder",
                                    "label": "更粗"
                                },
                                {
                                    "value": "lighter",
                                    "label": "更细"
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
                            "label": "线性"
                        },
                        {
                            "value": "quad",
                            "label": "象限"
                        },
                        {
                            "value": "quint",
                            "label": "五重峰"
                        },
                        {
                            "value": "cycle",
                            "label": "循环"
                        },
                        {
                            "value": "bounce",
                            "label": "弹跳"
                        },
                        {
                            "value": "elastic",
                            "label": "弹簧"
                        },
                        {
                            "value": "dequad",
                            "label": "反象限"
                        },
                        {
                            "value": "dequint",
                            "label": "反五重峰"
                        },
                        {
                            "value": "decycle",
                            "label": "反循环"
                        },
                        {
                            "value": "debounce",
                            "label": "反弹跳"
                        },
                        {
                            "value": "delastic",
                            "label": "反弹簧"
                        }
                    ]
                }
            ]
        };
    }

}

/* eslint-enable angular/angularelement */
