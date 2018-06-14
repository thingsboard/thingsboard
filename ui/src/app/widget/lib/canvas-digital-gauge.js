/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
                    this.gauge.value = value;
                    this.gauge._value = value;
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
                    "gaugeType": {
                        "title": "Gauge type",
                        "type": "string",
                        "default": "arc"
                    },
                    "donutStartAngle": {
                        "title": "Angle to start from when in donut mode",
                        "type": "number",
                        "default": 90
                    },
                    "neonGlowBrightness": {
                        "title": "Neon glow effect brightness, (0-100), 0 - disable effect",
                        "type": "number",
                        "default": 0
                    },
                    "dashThickness": {
                        "title": "Thickness of the stripes, 0 - no stripes",
                        "type": "number",
                        "default": 0
                    },
                    "roundedLineCap": {
                        "title": "Display rounded line cap",
                        "type": "boolean",
                        "default": false
                    },
                    "title": {
                        "title": "Gauge title",
                        "type": "string",
                        "default": null
                    },
                    "showTitle": {
                        "title": "Show gauge title",
                        "type": "boolean",
                        "default": false
                    },
                    "unitTitle": {
                        "title": "Unit title",
                        "type": "string",
                        "default": null
                    },
                    "showUnitTitle": {
                        "title": "Show unit title",
                        "type": "boolean",
                        "default": false
                    },
                    "showTimestamp": {
                        "title": "Show value timestamp",
                        "type": "boolean",
                        "default": false
                    },
                    "timestampFormat": {
                        "title": "Timestamp format",
                        "type": "string",
                        "default": "yyyy-MM-dd HH:mm:ss"
                    },
                    "showValue": {
                        "title": "Show value text",
                        "type": "boolean",
                        "default": true
                    },
                    "showMinMax": {
                        "title": "Show min and max values",
                        "type": "boolean",
                        "default": true
                    },
                    "gaugeWidthScale": {
                        "title": "Width of the gauge element",
                        "type": "number",
                        "default": 0.75
                    },
                    "defaultColor": {
                        "title": "Default color",
                        "type": "string",
                        "default": null
                    },
                    "gaugeColor": {
                        "title": "Background color of the gauge element",
                        "type": "string",
                        "default": null
                    },
                    "levelColors": {
                        "title": "Colors of indicator, from lower to upper",
                        "type": "array",
                        "items": {
                            "title": "Color",
                            "type": "string"
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
                        "default": "linear"
                    },
                    "titleFont": {
                        "title": "Gauge title font",
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
                                "default": 12
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
                    "labelFont": {
                        "title": "Font of label showing under value",
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
                                "default": 8
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
                    "valueFont": {
                        "title": "Font of label showing current value",
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
                    "minMaxFont": {
                        "title": "Font of minimum and maximum labels",
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
                                "default": 10
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
                            "label": "Arc"
                        },
                        {
                            "value": "donut",
                            "label": "Donut"
                        },
                        {
                            "value": "horizontalBar",
                            "label": "Horizontal bar"
                        },
                        {
                            "value": "verticalBar",
                            "label": "Vertical bar"
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
                            "key": "labelFont.weight",
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
                            "key": "minMaxFont.weight",
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
