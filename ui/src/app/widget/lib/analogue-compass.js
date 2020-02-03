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
/*import tinycolor from 'tinycolor2';*/

/* eslint-disable angular/angularelement */

export default class TbAnalogueCompass {
    constructor(ctx, canvasId) {
        this.ctx = ctx;

        canvasGauges.performance = window.performance; // eslint-disable-line no-undef, angular/window-service

        var gaugeElement = $('#'+canvasId, ctx.$container);

        var settings = ctx.settings;
        var majorTicks = (settings.majorTicks && settings.majorTicks.length > 0) ? angular.copy(settings.majorTicks) :  ["N","NE","E","SE","S","SW","W","NW"];
        majorTicks.push(majorTicks[0]);

        function getFontFamily(fontSettings) {
            var family = fontSettings && fontSettings.family ? fontSettings.family : 'Roboto';
            if (family === 'RobotoDraft') {
                family = 'Roboto';
            }
            return family;
        }

        var gaugeData = {

            renderTo: gaugeElement[0],

            // Generic options
            minValue: 0,
            maxValue: 360,
            majorTicks: majorTicks,
            minorTicks: settings.minorTicks || 22,
            ticksAngle: 360,
            startAngle: 180,
            strokeTicks: settings.showStrokeTicks || false,
            highlights: false,
            valueBox: false,

            //needle
            needleCircleSize: settings.needleCircleSize || 15,
            needleType: 'line',
            needleStart: 75,
            needleEnd: 99,
            needleWidth: 3,
            needleCircleOuter: false,

            //borders
            borders: settings.showBorder || false,
            borderInnerWidth: 0,
            borderMiddleWidth: 0,
            borderOuterWidth: settings.borderOuterWidth || 10,
            borderShadowWidth: 0,

            //colors
            colorPlate: settings.colorPlate || '#222',
            colorMajorTicks: settings.colorMajorTicks || '#f5f5f5',
            colorMinorTicks: settings.colorMinorTicks || '#ddd',
            colorNeedle: settings.colorNeedle || '#f08080',
            colorNeedleEnd: settings.colorNeedle || '#f08080',
            colorNeedleCircleInner: settings.colorNeedleCircle || '#e8e8e8',
            colorNeedleCircleInnerEnd: settings.colorNeedleCircle || '#e8e8e8',
            colorBorderOuter: settings.colorBorder || '#ccc',
            colorBorderOuterEnd: settings.colorBorder || '#ccc',
            colorNeedleShadowDown: "#222",

            //fonts
            fontNumbers: getFontFamily(settings.majorTickFont),
            fontNumbersSize: settings.majorTickFont && settings.majorTickFont.size ? settings.majorTickFont.size : 20,
            fontNumbersStyle: settings.majorTickFont && settings.majorTickFont.style ? settings.majorTickFont.style : 'normal',
            fontNumbersWeight: settings.majorTickFont && settings.majorTickFont.weight ? settings.majorTickFont.weight : '500',
            colorNumbers: settings.majorTickFont && settings.majorTickFont.color ? settings.majorTickFont.color : '#ccc',

            //animations
            animation: settings.animation !== false && !ctx.isMobile,
            animationDuration: (angular.isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
            animationRule: settings.animationRule || 'cycle',
            animationTarget: settings.animationTarget || 'needle'
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
                    "majorTicks": {
                        "title": "主刻度名称",
                        "type": "array",
                        "items": {
                            "title": "刻度名称",
                            "type": "string"
                        }
                    },
                    "minorTicks": {
                        "title": "次刻度个数",
                        "type": "number",
                        "default": 22
                    },
                    "showStrokeTicks": {
                        "title": "显示刻度线",
                        "type": "boolean",
                        "default": false
                    },
                    "needleCircleSize": {
                        "title": "指针环大小",
                        "type": "number",
                        "default": 15
                    },
                    "showBorder": {
                        "title": "显示边框",
                        "type": "boolean",
                        "default": true
                    },
                    "borderOuterWidth": {
                        "title": "边框宽度",
                        "type": "number",
                        "default": 10
                    },
                    "colorPlate": {
                        "title": "表盘颜色",
                        "type": "string",
                        "default": "#222"
                    },
                    "colorMajorTicks": {
                        "title": "主刻度颜色",
                        "type": "string",
                        "default": "#f5f5f5"
                    },
                    "colorMinorTicks": {
                        "title": "次刻度颜色",
                        "type": "string",
                        "default": "#ddd"
                    },
                    "colorNeedle": {
                        "title": "指针颜色",
                        "type": "string",
                        "default": "#f08080"
                    },
                    "colorNeedleCircle": {
                        "title": "指针环颜色",
                        "type": "string",
                        "default": "#e8e8e8"
                    },
                    "colorBorder": {
                        "title": "边框颜色",
                        "type": "string",
                        "default": "#ccc"
                    },
                    "majorTickFont": {
                        "title": "主刻度字体",
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
                                "default": 20
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
                                "default": "#ccc"
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
                    "animationTarget": {
                        "title": "动画目标",
                        "type": "string",
                        "default": "needle"
                    }
                },
                "required": []
            },
            "form": [
                {
                    "key": "majorTicks",
                    "items":[
                        "majorTicks[]"
                    ]
                },
                "minorTicks",
                "showStrokeTicks",
                "needleCircleSize",
                "showBorder",
                "borderOuterWidth",
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
                    "key": "colorNeedleCircle",
                    "type": "color"
                },
                {
                    "key": "colorBorder",
                    "type": "color"
                },
                {
                    "key": "majorTickFont",
                    "items": [
                        "majorTickFont.family",
                        "majorTickFont.size",
                        {
                            "key": "majorTickFont.style",
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
                            "key": "majorTickFont.weight",
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
                            "key": "majorTickFont.color",
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
                },
                {
                    "key": "animationTarget",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "needle",
                            "label": "指针"
                        },
                        {
                            "value": "plate",
                            "label": "表盘"
                        }
                    ]
                }
            ]
        };
    }
}

/* eslint-enable angular/angularelement */
