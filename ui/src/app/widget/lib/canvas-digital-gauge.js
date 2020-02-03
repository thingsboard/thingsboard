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
import tinycolor from 'tinycolor2';
import canvasGauges from 'canvas-gauges';
import CanvasDigitalGauge from './CanvasDigitalGauge';

/* eslint-disable angular/angularelement */
export default class TbCanvasDigitalGauge {

    constructor(ctx, canvasId) {
        this.ctx = ctx;

        canvasGauges.performance = window.performance; // eslint-disable-line no-undef, angular/window-service

        var gaugeElement = $('#'+canvasId, ctx.$container);
        var settings = ctx.settings;

        this.localSettings = {};
        this.localSettings.minValue = settings.minValue || 0;
        this.localSettings.maxValue = settings.maxValue || 100;
        this.localSettings.gaugeType = settings.gaugeType || 'arc';
        this.localSettings.neonGlowBrightness = settings.neonGlowBrightness || 0;
        this.localSettings.dashThickness = settings.dashThickness || 0;
        this.localSettings.roundedLineCap = settings.roundedLineCap === true;

        var dataKey = ctx.data[0].dataKey;
        var keyColor = settings.defaultColor || dataKey.color;

        this.localSettings.unitTitle = ((settings.showUnitTitle === true) ?
            (settings.unitTitle && settings.unitTitle.length > 0 ?
                settings.unitTitle : dataKey.label) : '');

        this.localSettings.showTimestamp = settings.showTimestamp == true ? true : false;
        this.localSettings.timestampFormat = settings.timestampFormat && settings.timestampFormat.length ? settings.timestampFormat : 'yyyy-MM-dd HH:mm:ss';

        this.localSettings.gaugeWidthScale = settings.gaugeWidthScale || 0.75;
        this.localSettings.gaugeColor = settings.gaugeColor || tinycolor(keyColor).setAlpha(0.2).toRgbString();

        if (!settings.levelColors || settings.levelColors.length <= 0) {
            this.localSettings.levelColors = [keyColor];
        } else {
            this.localSettings.levelColors = settings.levelColors.slice();
        }

        this.localSettings.decimals = angular.isDefined(dataKey.decimals) ? dataKey.decimals :
            ((angular.isDefined(settings.decimals) && settings.decimals !== null)
            ? settings.decimals : ctx.decimals);

        this.localSettings.units = dataKey.units && dataKey.units.length ? dataKey.units :
            (angular.isDefined(settings.units) && settings.units.length > 0 ? settings.units : ctx.units);

        this.localSettings.hideValue = settings.showValue !== true;
        this.localSettings.hideMinMax = settings.showMinMax !== true;

        this.localSettings.title = ((settings.showTitle === true) ?
            (settings.title && settings.title.length > 0 ?
                settings.title : dataKey.label) : '');

        if (!this.localSettings.unitTitle && this.localSettings.showTimestamp) {
            this.localSettings.unitTitle = ' ';
        }

        this.localSettings.titleFont = {};
        var settingsTitleFont = settings.titleFont;
        if (!settingsTitleFont) {
            settingsTitleFont = {};
        }

        function getFontFamily(fontSettings) {
            var family = fontSettings && fontSettings.family ? fontSettings.family : 'Roboto';
            if (family === 'RobotoDraft') {
                family = 'Roboto';
            }
            return family;
        }

        this.localSettings.titleFont.family = getFontFamily(settingsTitleFont);
        this.localSettings.titleFont.size = settingsTitleFont.size ? settingsTitleFont.size : 12;
        this.localSettings.titleFont.style = settingsTitleFont.style ? settingsTitleFont.style : 'normal';
        this.localSettings.titleFont.weight = settingsTitleFont.weight ? settingsTitleFont.weight : '500';
        this.localSettings.titleFont.color = settingsTitleFont.color ? settingsTitleFont.color : keyColor;

        this.localSettings.valueFont = {};
        var settingsValueFont = settings.valueFont;
        if (!settingsValueFont) {
            settingsValueFont = {};
        }

        this.localSettings.valueFont.family = getFontFamily(settingsValueFont);
        this.localSettings.valueFont.size = settingsValueFont.size ? settingsValueFont.size : 18;
        this.localSettings.valueFont.style = settingsValueFont.style ? settingsValueFont.style : 'normal';
        this.localSettings.valueFont.weight = settingsValueFont.weight ? settingsValueFont.weight : '500';
        this.localSettings.valueFont.color = settingsValueFont.color ? settingsValueFont.color : keyColor;

        this.localSettings.minMaxFont = {};
        var settingsMinMaxFont = settings.minMaxFont;
        if (!settingsMinMaxFont) {
            settingsMinMaxFont = {};
        }

        this.localSettings.minMaxFont.family = getFontFamily(settingsMinMaxFont);
        this.localSettings.minMaxFont.size = settingsMinMaxFont.size ? settingsMinMaxFont.size : 10;
        this.localSettings.minMaxFont.style = settingsMinMaxFont.style ? settingsMinMaxFont.style : 'normal';
        this.localSettings.minMaxFont.weight = settingsMinMaxFont.weight ? settingsMinMaxFont.weight : '500';
        this.localSettings.minMaxFont.color = settingsMinMaxFont.color ? settingsMinMaxFont.color : keyColor;

        this.localSettings.labelFont = {};
        var settingsLabelFont = settings.labelFont;
        if (!settingsLabelFont) {
            settingsLabelFont = {};
        }

        this.localSettings.labelFont.family = getFontFamily(settingsLabelFont);
        this.localSettings.labelFont.size = settingsLabelFont.size ? settingsLabelFont.size : 8;
        this.localSettings.labelFont.style = settingsLabelFont.style ? settingsLabelFont.style : 'normal';
        this.localSettings.labelFont.weight = settingsLabelFont.weight ? settingsLabelFont.weight : '500';
        this.localSettings.labelFont.color = settingsLabelFont.color ? settingsLabelFont.color : keyColor;


        var gaugeData = {

            renderTo: gaugeElement[0],

            gaugeWidthScale: this.localSettings.gaugeWidthScale,
            gaugeColor: this.localSettings.gaugeColor,
            levelColors: this.localSettings.levelColors,

            title: this.localSettings.title,

            fontTitleSize: this.localSettings.titleFont.size,
            fontTitleStyle: this.localSettings.titleFont.style,
            fontTitleWeight: this.localSettings.titleFont.weight,
            colorTitle: this.localSettings.titleFont.color,
            fontTitle: this.localSettings.titleFont.family,

            fontValueSize:  this.localSettings.valueFont.size,
            fontValueStyle: this.localSettings.valueFont.style,
            fontValueWeight: this.localSettings.valueFont.weight,
            colorValue: this.localSettings.valueFont.color,
            fontValue: this.localSettings.valueFont.family,

            fontMinMaxSize: this.localSettings.minMaxFont.size,
            fontMinMaxStyle: this.localSettings.minMaxFont.style,
            fontMinMaxWeight: this.localSettings.minMaxFont.weight,
            colorMinMax: this.localSettings.minMaxFont.color,
            fontMinMax: this.localSettings.minMaxFont.family,

            fontLabelSize: this.localSettings.labelFont.size,
            fontLabelStyle: this.localSettings.labelFont.style,
            fontLabelWeight: this.localSettings.labelFont.weight,
            colorLabel: this.localSettings.labelFont.color,
            fontLabel: this.localSettings.labelFont.family,

            minValue: this.localSettings.minValue,
            maxValue: this.localSettings.maxValue,
            gaugeType: this.localSettings.gaugeType,
            dashThickness: this.localSettings.dashThickness,
            roundedLineCap: this.localSettings.roundedLineCap,

            symbol: this.localSettings.units,
            label: this.localSettings.unitTitle,
            showTimestamp: this.localSettings.showTimestamp,
            hideValue: this.localSettings.hideValue,
            hideMinMax: this.localSettings.hideMinMax,

            valueDec: this.localSettings.decimals,

            neonGlowBrightness: this.localSettings.neonGlowBrightness,

            // animations
            animation: settings.animation !== false && !ctx.isMobile,
            animationDuration: (angular.isDefined(settings.animationDuration) && settings.animationDuration !== null) ? settings.animationDuration : 500,
            animationRule: settings.animationRule || 'linear',

            isMobile: ctx.isMobile

        };

        this.gauge = new CanvasDigitalGauge(gaugeData).draw();

    }

    update() {
        if (this.ctx.data.length > 0) {
            var cellData = this.ctx.data[0];
            if (cellData.data.length > 0) {
                var tvPair = cellData.data[cellData.data.length -
                1];
                var timestamp;
                if (this.localSettings.showTimestamp) {
                    timestamp = tvPair[0];
                    var filter= this.ctx.$scope.$injector.get('$filter');
                    var timestampDisplayValue = filter('date')(timestamp, this.localSettings.timestampFormat);
                    this.gauge.options.label = timestampDisplayValue;
                }
                var value = tvPair[1];
                if(value !== this.gauge.value) {
                    this.gauge._value = value;
                    this.gauge.value = value;
                } else if (this.localSettings.showTimestamp && this.gauge.timestamp != timestamp) {
                    this.gauge.timestamp = timestamp;
                }
            }
        }
    }

    mobileModeChanged() {
        var animation = this.ctx.settings.animation !== false && !this.ctx.isMobile;
        this.gauge.update({animation: animation, isMobile: this.ctx.isMobile});
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
                    "gaugeType": {
                        "title": "仪表类型",
                        "type": "string",
                        "default": "arc"
                    },
                    "donutStartAngle": {
                        "title": "在甜甜圈模式下开始的角度",
                        "type": "number",
                        "default": 90
                    },
                    "neonGlowBrightness": {
                        "title": "霓虹灯发光特效。亮度, (0-100), 0 - 禁用特效",
                        "type": "number",
                        "default": 0
                    },
                    "dashThickness": {
                        "title": "跳舞粗细, 0 - 无条纹",
                        "type": "number",
                        "default": 0
                    },
                    "roundedLineCap": {
                        "title": "显示圆形端点",
                        "type": "boolean",
                        "default": false
                    },
                    "title": {
                        "title": "仪表标题",
                        "type": "string",
                        "default": null
                    },
                    "showTitle": {
                        "title": "显示仪表标题",
                        "type": "boolean",
                        "default": false
                    },
                    "unitTitle": {
                        "title": "单位",
                        "type": "string",
                        "default": null
                    },
                    "showUnitTitle": {
                        "title": "显示单位",
                        "type": "boolean",
                        "default": false
                    },
                    "showTimestamp": {
                        "title": "显示数据时间戳",
                        "type": "boolean",
                        "default": false
                    },
                    "timestampFormat": {
                        "title": "时间戳格式",
                        "type": "string",
                        "default": "yyyy-MM-dd HH:mm:ss"
                    },
                    "showValue": {
                        "title": "显示数据文本",
                        "type": "boolean",
                        "default": true
                    },
                    "showMinMax": {
                        "title": "显示最小最大值",
                        "type": "boolean",
                        "default": true
                    },
                    "gaugeWidthScale": {
                        "title": "仪表宽度",
                        "type": "number",
                        "default": 0.75
                    },
                    "defaultColor": {
                        "title": "缺省颜色",
                        "type": "string",
                        "default": null
                    },
                    "gaugeColor": {
                        "title": "仪表背景色",
                        "type": "string",
                        "default": null
                    },
                    "levelColors": {
                        "title": "指示器颜色, 从下到上",
                        "type": "array",
                        "items": {
                            "title": "Color",
                            "type": "string"
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
                        "default": "linear"
                    },
                    "titleFont": {
                        "title": "仪表标题字体",
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
                                "default": 12
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
                    "labelFont": {
                        "title": "数值下面的标注字体",
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
                                "default": 8
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
                    "valueFont": {
                        "title": "当前值标注的字体",
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
                    "minMaxFont": {
                        "title": "最小最大值字体",
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
                                "default": 10
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
                    }
                }
            },
            "form": [
                "minValue",
                "maxValue",
                {
                    "key": "gaugeType",
                    "type": "rc-select",
                    "multiple": false,
                    "items": [
                        {
                            "value": "arc",
                            "label": "圆弧"
                        },
                        {
                            "value": "donut",
                            "label": "甜甜圈"
                        },
                        {
                            "value": "horizontalBar",
                            "label": "水平条形图"
                        },
                        {
                            "value": "verticalBar",
                            "label": "垂直条形图"
                        }
                    ]
                },
                "donutStartAngle",
                "neonGlowBrightness",
                "dashThickness",
                "roundedLineCap",
                "title",
                "showTitle",
                "unitTitle",
                "showUnitTitle",
                "showTimestamp",
                "timestampFormat",
                "showValue",
                "showMinMax",
                "gaugeWidthScale",
                {
                    "key": "defaultColor",
                    "type": "color"
                },
                {
                    "key": "gaugeColor",
                    "type": "color"
                },
                {
                    "key": "levelColors",
                    "items": [
                        {
                            "key": "levelColors[]",
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
                                    "label": "更淡"
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
                    "key": "labelFont",
                    "items": [
                        "labelFont.family",
                        "labelFont.size",
                        {
                            "key": "labelFont.style",
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
                            "key": "labelFont.weight",
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
                                    "label": "更淡"
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
                            "key": "labelFont.color",
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
                                    "label": "更淡"
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
                        }
                    ]
                },
                {
                    "key": "minMaxFont",
                    "items": [
                        "minMaxFont.family",
                        "minMaxFont.size",
                        {
                            "key": "minMaxFont.style",
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
                            "key": "minMaxFont.weight",
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
                                    "label": "更淡"
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
                            "key": "minMaxFont.color",
                            "type": "color"
                        }
                    ]
                }
            ]
        };
    }
}
/* eslint-enable angular/angularelement */
