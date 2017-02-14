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
import $ from 'jquery';
import tinycolor from 'tinycolor2';
import 'justgage';
import Raphael from 'raphael';

/* eslint-disable angular/angularelement */

export default class TbDigitalGauge {
    constructor(containerElement, settings, data) {

        var tbGauge = this;

        window.Raphael = Raphael; // eslint-disable-line no-undef, angular/window-service

        var isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1; // eslint-disable-line no-undef

        var gaugeElement = $(containerElement);

        this.localSettings = {};

        this.localSettings.minValue = settings.minValue || 0;
        this.localSettings.maxValue = settings.maxValue || 100;
        this.localSettings.gaugeType = settings.gaugeType || 'arc';
        this.localSettings.donutStartAngle = (angular.isDefined(settings.donutStartAngle) && settings.donutStartAngle !== null)
            ? settings.donutStartAngle : 90;
        this.localSettings.neonGlowBrightness = settings.neonGlowBrightness || 0;
        this.localSettings.dashThickness = settings.dashThickness || 0;
        this.localSettings.roundedLineCap = settings.roundedLineCap === true;

        var dataKey = data[0].dataKey;
        var keyColor = settings.defaultColor || dataKey.color;

        this.localSettings.title = ((settings.showTitle === true) ?
            (settings.title && settings.title.length > 0 ?
                settings.title : dataKey.label) : '');

        this.localSettings.unitTitle = ((settings.showUnitTitle === true) ?
            (settings.unitTitle && settings.unitTitle.length > 0 ?
                settings.unitTitle : dataKey.label) : '');

        this.localSettings.gaugeWidthScale = settings.gaugeWidthScale || 0.75;
        this.localSettings.gaugeColor = settings.gaugeColor || tinycolor(keyColor).setAlpha(0.2).toRgbString();

        if (!settings.levelColors || settings.levelColors.length <= 0) {
            this.localSettings.levelColors = [keyColor, keyColor];
        } else {
            this.localSettings.levelColors = settings.levelColors.slice();
        }
        if (this.localSettings.neonGlowBrightness) {
            this.localSettings.origLevelColors = [];
            for (var i = 0; i < this.localSettings.levelColors.length; i++) {
                this.localSettings.origLevelColors.push(this.localSettings.levelColors[i]);
                this.localSettings.levelColors[i] = tinycolor(this.localSettings.levelColors[i]).brighten(this.localSettings.neonGlowBrightness).toHexString();
            }
            var colorsCount = this.localSettings.origLevelColors.length;
            var inc = colorsCount > 1 ? (1 / (colorsCount - 1)) : 1;
            this.localSettings.colorsRange = [];
            for (i = 0; i < this.localSettings.origLevelColors.length; i++) {
                var percentage = inc * i;
                var tColor = tinycolor(this.localSettings.origLevelColors[i]);
                this.localSettings.colorsRange[i] = {
                    pct: percentage,
                    color: tColor.toRgb(),
                    rgbString: tColor.toRgbString
                };
            }
        }


        this.localSettings.refreshAnimationType = settings.refreshAnimationType || '>';
        this.localSettings.refreshAnimationTime = settings.refreshAnimationTime || 700;
        this.localSettings.startAnimationType = settings.startAnimationType || '>';
        this.localSettings.startAnimationTime = settings.startAnimationTime || 700;
        this.localSettings.decimals = (angular.isDefined(settings.decimals) && settings.decimals !== null)
            ? settings.decimals : 0;
        this.localSettings.units = settings.units || '';
        this.localSettings.hideValue = settings.showValue !== true;
        this.localSettings.hideMinMax = settings.showMinMax !== true;

        this.localSettings.titleFont = {};
        var settingsTitleFont = settings.titleFont;
        if (!settingsTitleFont) {
            settingsTitleFont = {};
        }

        this.localSettings.titleFont.family = settingsTitleFont.family || 'RobotoDraft';
        this.localSettings.titleFont.size = settingsTitleFont.size ? settingsTitleFont.size : 12;
        this.localSettings.titleFont.style = settingsTitleFont.style ? settingsTitleFont.style : 'normal';
        this.localSettings.titleFont.weight = settingsTitleFont.weight ? settingsTitleFont.weight : '500';
        this.localSettings.titleFont.color = settingsTitleFont.color ? settingsTitleFont.color : keyColor;

        this.localSettings.labelFont = {};
        var settingsLabelFont = settings.labelFont;
        if (!settingsLabelFont) {
            settingsLabelFont = {};
        }

        this.localSettings.labelFont.family = settingsLabelFont.family || 'RobotoDraft';
        this.localSettings.labelFont.size = settingsLabelFont.size ? settingsLabelFont.size : 8;
        this.localSettings.labelFont.style = settingsLabelFont.style ? settingsLabelFont.style : 'normal';
        this.localSettings.labelFont.weight = settingsLabelFont.weight ? settingsLabelFont.weight : '500';
        this.localSettings.labelFont.color = settingsLabelFont.color ? settingsLabelFont.color : keyColor;

        this.localSettings.valueFont = {};
        var settingsValueFont = settings.valueFont;
        if (!settingsValueFont) {
            settingsValueFont = {};
        }

        this.localSettings.valueFont.family = settingsValueFont.family || 'RobotoDraft';
        this.localSettings.valueFont.size = settingsValueFont.size ? settingsValueFont.size : 18;
        this.localSettings.valueFont.style = settingsValueFont.style ? settingsValueFont.style : 'normal';
        this.localSettings.valueFont.weight = settingsValueFont.weight ? settingsValueFont.weight : '500';
        this.localSettings.valueFont.color = settingsValueFont.color ? settingsValueFont.color : keyColor;

        this.localSettings.minMaxFont = {};
        var settingsMinMaxFont = settings.minMaxFont;
        if (!settingsMinMaxFont) {
            settingsMinMaxFont = {};
        }

        this.localSettings.minMaxFont.family = settingsMinMaxFont.family || 'RobotoDraft';
        this.localSettings.minMaxFont.size = settingsMinMaxFont.size ? settingsMinMaxFont.size : 10;
        this.localSettings.minMaxFont.style = settingsMinMaxFont.style ? settingsMinMaxFont.style : 'normal';
        this.localSettings.minMaxFont.weight = settingsMinMaxFont.weight ? settingsMinMaxFont.weight : '500';
        this.localSettings.minMaxFont.color = settingsMinMaxFont.color ? settingsMinMaxFont.color : keyColor;

        if (this.localSettings.neonGlowBrightness) {
            this.localSettings.titleFont.origColor = this.localSettings.titleFont.color;
            this.localSettings.titleFont.color = tinycolor(this.localSettings.titleFont.color).brighten(this.localSettings.neonGlowBrightness).toHexString();
            this.localSettings.labelFont.origColor = this.localSettings.labelFont.color;
            this.localSettings.labelFont.color = tinycolor(this.localSettings.labelFont.color).brighten(this.localSettings.neonGlowBrightness).toHexString();
            this.localSettings.valueFont.origColor = this.localSettings.valueFont.color;
            this.localSettings.valueFont.color = tinycolor(this.localSettings.valueFont.color).brighten(this.localSettings.neonGlowBrightness).toHexString();
            this.localSettings.minMaxFont.origColor = this.localSettings.minMaxFont.color;
            this.localSettings.minMaxFont.color = tinycolor(this.localSettings.minMaxFont.color).brighten(this.localSettings.neonGlowBrightness).toHexString();
        }

        var gaugeOptions = {
            parentNode: gaugeElement[0],
            value: 0,
            min: this.localSettings.minValue,
            max: this.localSettings.maxValue,
            title: this.localSettings.title,
            label: this.localSettings.unitTitle,
            humanFriendlyDecimal: 0,
            gaugeWidthScale: this.localSettings.gaugeWidthScale,
            relativeGaugeSize: true,
            gaugeColor: this.localSettings.gaugeColor,
            levelColors: this.localSettings.levelColors,
            refreshAnimationType: this.localSettings.refreshAnimationType,
            refreshAnimationTime: this.localSettings.refreshAnimationTime,
            startAnimationType: this.localSettings.startAnimationType,
            startAnimationTime: this.localSettings.startAnimationTime,
            humanFriendly: false,
            donut: this.localSettings.gaugeType === 'donut',
            donutStartAngle: this.localSettings.donutStartAngle,
            decimals: this.localSettings.decimals,
            pointer: false,
            symbol: this.localSettings.units,
            hideValue: this.localSettings.hideValue,
            hideMinMax: this.localSettings.hideMinMax,
            titleFontColor: this.localSettings.titleFont.color,
            labelFontColor: this.localSettings.labelFont.color,
            valueFontColor: this.localSettings.valueFont.color,
            valueFontFamily: this.localSettings.valueFont.family
        };

        this.gauge = new JustGage(gaugeOptions); // eslint-disable-line no-undef

        var gParams = this.gauge.params;

        var titleTextElement = $(this.gauge.txtTitle.node);
        titleTextElement.css('fontFamily', this.localSettings.titleFont.family);
        titleTextElement.css('fontSize', this.localSettings.titleFont.size + 'px');
        titleTextElement.css('fontStyle', this.localSettings.titleFont.style);
        titleTextElement.css('fontWeight', this.localSettings.titleFont.weight);
        titleTextElement.css('textTransform', 'uppercase');

        var labelTextElement = $(this.gauge.txtLabel.node);
        labelTextElement.css('fontFamily', this.localSettings.labelFont.family);
        labelTextElement.css('fontSize', this.localSettings.labelFont.size + 'px');
        labelTextElement.css('fontStyle', this.localSettings.labelFont.style);
        labelTextElement.css('fontWeight', this.localSettings.labelFont.weight);
        labelTextElement.css('textTransform', 'uppercase');

        var valueTextElement = $(this.gauge.txtValue.node);
        valueTextElement.css('fontSize', this.localSettings.valueFont.size + 'px');
        valueTextElement.css('fontStyle', this.localSettings.valueFont.style);
        valueTextElement.css('fontWeight', this.localSettings.valueFont.weight);

        var minValTextElement = $(this.gauge.txtMin.node);
        var maxValTextElement = $(this.gauge.txtMax.node);
        minValTextElement.css('fontFamily', this.localSettings.minMaxFont.family);
        maxValTextElement.css('fontFamily', this.localSettings.minMaxFont.family);
        minValTextElement.css('fontSize', this.localSettings.minMaxFont.size+'px');
        maxValTextElement.css('fontSize', this.localSettings.minMaxFont.size+'px');
        minValTextElement.css('fontStyle', this.localSettings.minMaxFont.style);
        maxValTextElement.css('fontStyle', this.localSettings.minMaxFont.style);
        minValTextElement.css('fontWeight', this.localSettings.minMaxFont.weight);
        maxValTextElement.css('fontWeight', this.localSettings.minMaxFont.weight);
        minValTextElement.css('fill', this.localSettings.minMaxFont.color);
        maxValTextElement.css('fill', this.localSettings.minMaxFont.color);

        var gaugeLevelElement = $(this.gauge.level.node);
        var gaugeBackElement = $(this.gauge.gauge.node);

        var w = gParams.widgetW;
        var gws = this.localSettings.gaugeWidthScale;
        var Ro, Ri;
        if (this.localSettings.gaugeType === 'donut') {
            Ro = w / 2 - w / 7;
        } else {
            Ro = w / 2 - w / 10;
        }
        Ri = Ro - w / 6.666666666666667 * gws;
        gParams.strokeWidth = Ro - Ri;

        gParams.viewport = {
            x: 0,
            y: 0,
            width: gParams.canvasW,
            height: gParams.canvasH
        }
        var maxW;
        if (this.localSettings.gaugeType === 'donut') {
            if (gaugeOptions.title && gaugeOptions.title.length > 0) {
                gParams.viewport.height = 140;
            } else {
                gParams.viewport.y = 17;
                gParams.viewport.height = 120;
            }
            gParams.viewport.x = 40;
            gParams.viewport.width = 120;
            $('tspan', labelTextElement).attr('dy', '6');
            if (!this.localSettings.unitTitle || this.localSettings.unitTitle.length === 0) {
                var Cy = gParams.widgetH / 1.95 + gParams.dy;
                gParams.valueY = Cy + (this.localSettings.valueFont.size-4)/2;
                this.gauge.txtValue.attr({"y": gParams.valueY });
            }
        } else if (this.localSettings.gaugeType === 'arc') {
            if (gaugeOptions.title && gaugeOptions.title.length > 0) {
                gParams.viewport.y = 5;
                gParams.viewport.height = 140;
            } else {
                gParams.viewport.y = 40;
                gParams.viewport.height = 100;
            }
            if (this.localSettings.roundedLineCap) {
                $('tspan', minValTextElement).attr('dy', ''+(gParams.strokeWidth/2));
                $('tspan', maxValTextElement).attr('dy', ''+(gParams.strokeWidth/2));
            }
        } else if (this.localSettings.gaugeType === 'horizontalBar') {
            gParams.titleY = gParams.dy + gParams.widgetH / 3.5 + (this.localSettings.title === '' ? 0 : this.localSettings.titleFont.size);
            this.gauge.txtTitle.attr({"y": gParams.titleY });
            gParams.titleBottom = gParams.titleY + (this.localSettings.title === '' ? 0 : 8);

            gParams.valueY = gParams.titleBottom + (this.localSettings.hideValue ? 0 : this.localSettings.valueFont.size);
            gParams.barTop = gParams.valueY + 8;
            gParams.barBottom = gParams.barTop + gParams.strokeWidth;

            this.gauge.txtValue.attr({"y": gParams.valueY });

            if (this.localSettings.hideMinMax && this.localSettings.unitTitle === '') {
                gParams.labelY = gParams.barBottom;
                gParams.barLeft = this.localSettings.minMaxFont.size/3;
                gParams.barRight = gParams.viewport.width - this.localSettings.minMaxFont.size/3;
            } else {
                maxW = Math.max(this.gauge.txtMin.node.getComputedTextLength(), this.gauge.txtMax.node.getComputedTextLength());
                gParams.minX = maxW/2 + this.localSettings.minMaxFont.size/3;
                gParams.maxX = gParams.viewport.width - maxW/2 - this.localSettings.minMaxFont.size/3;
                gParams.barLeft = gParams.minX;
                gParams.barRight = gParams.maxX;
                gParams.labelY = gParams.barBottom + 4 + this.localSettings.labelFont.size;
                this.gauge.txtLabel.attr({"y": gParams.labelY });
                this.gauge.txtMin.attr({"x": gParams.minX, "y": gParams.labelY });
                this.gauge.txtMax.attr({"x": gParams.maxX, "y": gParams.labelY });
            }
            gParams.viewport.y = 40;
            gParams.viewport.height = gParams.labelY-25;
        } else if (this.localSettings.gaugeType === 'verticalBar') {
            gParams.titleY = (this.localSettings.title === '' ? 0 : this.localSettings.titleFont.size) + 8;
            this.gauge.txtTitle.attr({"y": gParams.titleY });
            gParams.titleBottom = gParams.titleY + (this.localSettings.title === '' ? 0 : 8);

            gParams.valueY = gParams.titleBottom + (this.localSettings.hideValue ? 0 : this.localSettings.valueFont.size);
            gParams.barTop = gParams.valueY + 8;
            this.gauge.txtValue.attr({"y": gParams.valueY });

            gParams.labelY = gParams.widgetH - 16;
            if (this.localSettings.unitTitle === '') {
                gParams.barBottom = gParams.labelY;
            } else {
                gParams.barBottom = gParams.labelY - 4 - this.localSettings.labelFont.size;
                this.gauge.txtLabel.attr({"y": gParams.labelY });
            }
            gParams.minX = gParams.maxX = (gParams.widgetW/2 + gParams.dx) + gParams.strokeWidth/2 + this.localSettings.minMaxFont.size/3;
            gParams.minY = gParams.barBottom;
            gParams.maxY = gParams.barTop;
            this.gauge.txtMin.attr({"text-anchor": "start", "x": gParams.minX, "y": gParams.minY });
            this.gauge.txtMax.attr({"text-anchor": "start", "x": gParams.maxX, "y": gParams.maxY });
            gParams.prefWidth = gParams.strokeWidth;
            if (!this.localSettings.hideMinMax) {
                maxW = Math.max(this.gauge.txtMin.node.getComputedTextLength(), this.gauge.txtMax.node.getComputedTextLength());
                gParams.prefWidth += (maxW + this.localSettings.minMaxFont.size ) * 2;
            }
            gParams.viewport.x = (gParams.canvasW - gParams.prefWidth)/2;
            gParams.viewport.width = gParams.prefWidth;
        }
        this.gauge.canvas.setViewBox(gParams.viewport.x, gParams.viewport.y, gParams.viewport.width, gParams.viewport.height, true);

        if (this.localSettings.dashThickness) {
            var Rm = Ri + gParams.strokeWidth * 0.5;
            var circumference = Math.PI * Rm;
            if (this.localSettings.gaugeType === 'donut') {
                circumference *=2;
            }
            var dashCount = Math.floor(circumference / (this.localSettings.dashThickness));
            if (this.localSettings.gaugeType === 'donut') {
                dashCount = (dashCount | 1) - 1;
            } else {
                dashCount = (dashCount - 1) | 1;
            }
            var dashLength = circumference/dashCount;
            gaugeLevelElement.attr('stroke-dasharray', '' + dashLength + 'px');
            gaugeBackElement.attr('stroke-dasharray', '' + dashLength + 'px');
        }

        function getColor(val, pct) {

            var lower, upper, range, rangePct, pctLower, pctUpper, color;

            if (tbGauge.localSettings.colorsRange.length === 1) {
                return tbGauge.localSettings.colorsRange[0].rgbString;
            }
            if (pct === 0) {
                return tbGauge.localSettings.colorsRange[0].rgbString;
            }

            for (var j = 0; j < tbGauge.localSettings.colorsRange.length; j++) {
                if (pct <= tbGauge.localSettings.colorsRange[j].pct) {
                    lower = tbGauge.localSettings.colorsRange[j - 1];
                    upper = tbGauge.localSettings.colorsRange[j];
                    range = upper.pct - lower.pct;
                    rangePct = (pct - lower.pct) / range;
                    pctLower = 1 - rangePct;
                    pctUpper = rangePct;
                    color = tinycolor({
                        r: Math.floor(lower.color.r * pctLower + upper.color.r * pctUpper),
                        g: Math.floor(lower.color.g * pctLower + upper.color.g * pctUpper),
                        b: Math.floor(lower.color.b * pctLower + upper.color.b * pctUpper)
                    });
                    return color.toRgbString();
                }
            }

        }

        this.gauge.canvas.customAttributes.pki = function(value, min, max, w, h, dx, dy, gws, donut, reverse) { // eslint-disable-line no-unused-vars
            var alpha, Rm, Ro, Ri, Cx, Cy, Xm, Ym, Xo, Yo, path;

            if (tbGauge.localSettings.neonGlowBrightness && !isFirefox
                && tbGauge.floodColorElement1 && tbGauge.floodColorElement2) {
                var progress = (value - min) / (max - min);
                var resultColor = getColor(value, progress);
                var brightenColor1 = tinycolor(resultColor).brighten(tbGauge.localSettings.neonGlowBrightness).toRgbString();
                var brightenColor2 = resultColor;
                tbGauge.floodColorElement1.setAttribute('flood-color', brightenColor1);
                tbGauge.floodColorElement2.setAttribute('flood-color', brightenColor2);
            }

            var gaugeType = tbGauge.localSettings.gaugeType;

            if (gaugeType === 'donut') {
                alpha = (1 - 2 * (value - min) / (max - min)) * Math.PI;
                Ro = w / 2 - w / 7;
                Ri = Ro - w / 6.666666666666667 * gws;
                Rm = Ri + (Ro - Ri)/2;

                Cx = w / 2 + dx;
                Cy = h / 1.95 + dy;

                Xm = w / 2 + dx + Rm * Math.cos(alpha);
                Ym = h - (h - Cy) - Rm * Math.sin(alpha);

                path = "M" + (Cx - Rm) + "," + Cy + " ";
                if ((value - min) > ((max - min) / 2)) {
                    path += "A" + Rm + "," + Rm + " 0 0 1 " + (Cx + Rm) + "," + Cy + " ";
                    path += "A" + Rm + "," + Rm + " 0 0 1 " + Xm + "," + Ym + " ";
                } else {
                    path += "A" + Rm + "," + Rm + " 0 0 1 " + Xm + "," + Ym + " ";
                }
                return {
                    path: path
                };

            } else if (gaugeType === 'arc') {
                alpha = (1 - (value - min) / (max - min)) * Math.PI;
                Ro = w / 2 - w / 10;
                Ri = Ro - w / 6.666666666666667 * gws;
                Rm = Ri + (Ro - Ri)/2;

                Cx = w / 2 + dx;
                Cy = h / 1.25 + dy;

                Xm = w / 2 + dx + Rm * Math.cos(alpha);
                Ym = h - (h - Cy) - Rm * Math.sin(alpha);

                path = "M" + (Cx - Rm) + "," + Cy + " ";
                path += "A" + Rm + "," + Rm + " 0 0 1 " + Xm + "," + Ym + " ";

                return {
                    path: path
                };
            } else if (gaugeType === 'horizontalBar') {
                Cx = tbGauge.gauge.params.barLeft;
                Cy = tbGauge.gauge.params.barTop + tbGauge.gauge.params.strokeWidth/2;
                Ro = (tbGauge.gauge.params.barRight - tbGauge.gauge.params.barLeft)/2;
                alpha = (value - min) / (max - min);
                Xo = Cx + 2 * Ro * alpha;
                path = "M" + Cx + "," + Cy + " ";
                path += "H" + " " + Xo;
                return {
                    path: path
                };
            } else if (gaugeType === 'verticalBar') {
                Cx = w / 2 + dx;
                Cy = tbGauge.gauge.params.barBottom;
                Ro = (tbGauge.gauge.params.barBottom - tbGauge.gauge.params.barTop)/2;
                alpha = (value - min) / (max - min);
                Yo = Cy - 2 * Ro * alpha;
                path = "M" + Cx + "," + Cy + " ";
                path += "V" + " " + Yo;
                return {
                    path: path
                };
            }
        };

        var gaugeAttrs = {
            "stroke":  this.gauge.gauge.attrs.fill,
            "fill": 'rgba(0,0,0,0)',
            pki: [  this.gauge.config.max,
                this.gauge.config.min,
                this.gauge.config.max,
                gParams.widgetW,
                gParams.widgetH,
                gParams.dx,
                gParams.dy,
                this.gauge.config.gaugeWidthScale,
                this.gauge.config.donut,
                this.gauge.config.reverse
            ]
        };
        gaugeAttrs['stroke-width'] = gParams.strokeWidth;


        var gaugeLevelAttrs = {
            "stroke":  this.gauge.level.attrs.fill,
            "fill": 'rgba(0,0,0,0)'
        };
        gaugeLevelAttrs['stroke-width'] = gParams.strokeWidth;
        if (this.localSettings.roundedLineCap) {
            gaugeAttrs['stroke-linecap'] = 'round';
            gaugeLevelAttrs['stroke-linecap'] = 'round';
        }

        this.gauge.gauge.attr(gaugeAttrs);
        this.gauge.level.attr(gaugeLevelAttrs);

        this.gauge.level.animate = function(attrs, refreshAnimationTime, refreshAnimationType) {
            if (attrs.fill) {
                attrs.stroke = attrs.fill;
                attrs.fill = 'rgba(0,0,0,0)';
            }
            return Raphael.el.animate.call(tbGauge.gauge.level, attrs, refreshAnimationTime, refreshAnimationType);
        }

        function neonShadow(color) {
            var brightenColor = tinycolor(color).brighten(tbGauge.localSettings.neonGlowBrightness);
            return     '0 0 10px '+brightenColor+','+
                '0 0 20px  '+brightenColor+','+
                '0 0 30px  '+brightenColor+','+
                '0 0 40px  '+ color +','+
                '0 0 70px  '+ color +','+
                '0 0 80px  '+ color +','+
                '0 0 100px  '+ color +','+
                '0 0 150px  '+ color;
        }

        if (this.localSettings.neonGlowBrightness) {
            titleTextElement.css('textShadow', neonShadow(this.localSettings.titleFont.origColor));
            valueTextElement.css('textShadow', neonShadow(this.localSettings.valueFont.origColor));
            labelTextElement.css('textShadow', neonShadow(this.localSettings.labelFont.origColor));
            minValTextElement.css('textShadow', neonShadow(this.localSettings.minMaxFont.origColor));
            maxValTextElement.css('textShadow', neonShadow(this.localSettings.minMaxFont.origColor));
        }

        if (this.localSettings.neonGlowBrightness && !isFirefox) {
            var filterX = (gParams.viewport.x / gParams.viewport.width)*100 + '%';
            var filterY = (gParams.viewport.y / gParams.viewport.height)*100 + '%';
            var svgBackFilterId = 'backBlurFilter' + Math.random();
            var svgBackFilter = document.createElementNS("http://www.w3.org/2000/svg", "filter"); // eslint-disable-line no-undef, angular/document-service
            svgBackFilter.setAttribute('id', svgBackFilterId);
            svgBackFilter.setAttribute('filterUnits', 'userSpaceOnUse');
            svgBackFilter.setAttribute('x', filterX);
            svgBackFilter.setAttribute('y', filterY);
            svgBackFilter.setAttribute('width', '100%');
            svgBackFilter.setAttribute('height', '100%');
            svgBackFilter.innerHTML =
                '<feComponentTransfer>'+
                '<feFuncR type="linear" slope="1.5"/>'+
                '<feFuncG type="linear" slope="1.5"/>'+
                '<feFuncB type="linear" slope="1.5"/>'+
                '</feComponentTransfer>'+
                '<feGaussianBlur stdDeviation="3" result="coloredBlur"></feGaussianBlur>'+
                '<feMerge>'+
                '<feMergeNode in="coloredBlur"/>'+
                '<feMergeNode in="SourceGraphic"/>'+
                '</feMerge>';
            gaugeBackElement.attr('filter', 'url(#'+svgBackFilterId+')');

            var svgFillFilterId = 'fillBlurFilter' + Math.random();
            var svgFillFilter = document.createElementNS("http://www.w3.org/2000/svg", "filter"); // eslint-disable-line no-undef, angular/document-service
            svgFillFilter.setAttribute('id', svgFillFilterId);
            svgFillFilter.setAttribute('filterUnits', 'userSpaceOnUse');
            svgFillFilter.setAttribute('x', filterX);
            svgFillFilter.setAttribute('y', filterY);
            svgFillFilter.setAttribute('width', '100%');
            svgFillFilter.setAttribute('height', '100%');

            var brightenColor1 = tinycolor(this.localSettings.origLevelColors[0]).brighten(this.localSettings.neonGlowBrightness).toRgbString();
            var brightenColor2 = tinycolor(this.localSettings.origLevelColors[0]).toRgbString();
            svgFillFilter.innerHTML =
                '<feFlood flood-color="'+brightenColor1+'" result="flood1" />'+
                '<feComposite in="flood1" in2="SourceGraphic" operator="in" result="floodShape" />'+
                '<feGaussianBlur in="floodShape" stdDeviation="3" result="blur" />'+
                '<feFlood flood-color="'+brightenColor2+'" result="flood2" />'+
                '<feComposite in="flood2" in2="SourceGraphic" operator="in" result="floodShape2" />'+
                '<feGaussianBlur in="floodShape2" stdDeviation="12" result="blur2" />'+
                '<feMerge result="blurs">'+
                '  <feMergeNode in="blur2"/>'+
                '  <feMergeNode in="blur2"/>'+
                '  <feMergeNode in="blur"/>'+
                '  <feMergeNode in="blur"/>'+
                '  <feMergeNode in="SourceGraphic"/>'+
                '</feMerge>';
            this.floodColorElement1 = $('feFlood:nth-of-type(1)', svgFillFilter)[0];
            this.floodColorElement2 = $('feFlood:nth-of-type(2)', svgFillFilter)[0];
            gaugeLevelElement.attr('filter', 'url(#'+svgFillFilterId+')');

            var svgDefsElement = $('svg > defs', containerElement);
            svgDefsElement[0].appendChild(svgBackFilter);
            svgDefsElement[0].appendChild(svgFillFilter);
        } else {
            gaugeBackElement.attr('filter', '');
            gaugeLevelElement.attr('filter', '');
        }
    }

    redraw(data) {
        if (data.length > 0) {
            var cellData = data[0];
            if (cellData.data.length > 0) {
                var tvPair = cellData.data[cellData.data.length -
                1];
                var value = tvPair[1];
                if (this.gauge.config.value !== value) {
                    this.gauge.refresh(value);
                }
            }
        }
    }
}

/* eslint-enable angular/angularelement */
