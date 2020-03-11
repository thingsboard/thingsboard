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

let defaultDigitalGaugeOptions = Object.assign({}, canvasGauges.GenericOptions, {
        gaugeType: 'arc',
        gaugeWithScale: 0.75,
        dashThickness: 0,
        roundedLineCap: false,

        gaugeColor: '#777',
        levelColors: ['blue'],

        symbol: '',
        label: '',
        hideValue: false,
        hideMinMax: false,

        fontTitle: 'Roboto',

        fontValue: 'Roboto',

        fontMinMaxSize: 10,
        fontMinMaxStyle: 'normal',
        fontMinMaxWeight: '500',
        colorMinMax: '#eee',
        fontMinMax: 'Roboto',

        fontLabelSize: 8,
        fontLabelStyle: 'normal',
        fontLabelWeight: '500',
        colorLabel: '#eee',
        fontLabel: 'Roboto',

        neonGlowBrightness: 0,

        colorTicks: 'gray',
        tickWidth: 4,
        ticks: [],

        isMobile: false

});

const round = Math.round;

export default class CanvasDigitalGauge extends canvasGauges.BaseGauge {

    constructor(options) {
        canvasGauges.performance = window.performance; // eslint-disable-line no-undef, angular/window-service
        options = Object.assign({}, defaultDigitalGaugeOptions, options || {});
        super(CanvasDigitalGauge.configure(options));
        this.initValueClone();
    }

    initValueClone() {
        let canvas = this.canvas;
        this.elementValueClone = canvas.element.cloneNode(true);
        this.contextValueClone = this.elementValueClone.getContext('2d');
        this.elementValueClone.initialized = false;

        this.contextValueClone.translate(canvas.drawX, canvas.drawY);
        this.contextValueClone.save();

        this.elementProgressClone = canvas.element.cloneNode(true);
        this.contextProgressClone = this.elementProgressClone.getContext('2d');
        this.elementProgressClone.initialized = false;

        this.contextProgressClone.translate(canvas.drawX, canvas.drawY);
        this.contextProgressClone.save();

    }

    static configure(options) {

        if (options.value > options.maxValue) {
            options.value = options.maxValue;
        }

        if (options.value < options.minValue) {
            options.value = options.minValue;
        }

        if (options.gaugeType === 'donut') {
            if (!options.donutStartAngle) {
                options.donutStartAngle = 1.5 * Math.PI;
            }
            if (!options.donutEndAngle) {
                options.donutEndAngle = options.donutStartAngle + 2 * Math.PI;
            }
        }

        var colorsCount = options.levelColors.length;
        const inc = colorsCount > 1 ? (1 / (colorsCount - 1)) : 1;
        var isColorProperty = angular.isString(options.levelColors[0]);

        options.colorsRange = [];
        if (options.neonGlowBrightness) {
            options.neonColorsRange = [];
        }

        for (let i = 0; i < options.levelColors.length; i++) {
            const levelColor = options.levelColors[i];
            if (levelColor !== null) {
                let percentage = isColorProperty ? inc * i : CanvasDigitalGauge.normalizeValue(levelColor.value, options.minValue, options.maxValue);
                let tColor = tinycolor(isColorProperty ? levelColor : levelColor.color);
                options.colorsRange.push({
                    pct: percentage,
                    color: tColor.toRgb(),
                    rgbString: tColor.toRgbString()
                });
                if (options.neonGlowBrightness) {
                    tColor = tinycolor(isColorProperty ? levelColor : levelColor.color).brighten(options.neonGlowBrightness);
                    options.neonColorsRange.push({
                        pct: percentage,
                        color: tColor.toRgb(),
                        rgbString: tColor.toRgbString()
                    });
                }
            }
        }

        options.ticksValue = [];
        for(let i = 0; i < options.ticks.length; i++){
            if(options.ticks[i] !== null){
                options.ticksValue.push(CanvasDigitalGauge.normalizeValue(options.ticks[i], options.minValue, options.maxValue))
            }
        }

        if (options.neonGlowBrightness) {
            options.neonColorTitle = tinycolor(options.colorTitle).brighten(options.neonGlowBrightness).toHexString();
            options.neonColorLabel = tinycolor(options.colorLabel).brighten(options.neonGlowBrightness).toHexString();
            options.neonColorValue = tinycolor(options.colorValue).brighten(options.neonGlowBrightness).toHexString();
            options.neonColorMinMax = tinycolor(options.colorMinMax).brighten(options.neonGlowBrightness).toHexString();
        }

        return canvasGauges.BaseGauge.configure(options);
    }

    static normalizeValue (value, min, max) {
        let normalValue = (value - min) / (max - min);
        if (normalValue <= 0) {
            return 0;
        }
        if (normalValue >= 1) {
            return 1;
        }
        return normalValue;
    }

    destroy() {
        this.contextValueClone = null;
        this.elementValueClone = null;
        this.contextProgressClone = null;
        this.elementProgressClone = null;
        super.destroy();
    }

    update(options) {
        this.canvas.onRedraw = null;
        var result = super.update(options);
        this.initValueClone();
        this.canvas.onRedraw = this.draw.bind(this);
        this.draw();
        return result;
    }

    set timestamp(timestamp) {
        this.options.timestamp = timestamp;
        this.draw();
    }

    get timestamp() {
        return this.options.timestamp;
    }

    draw() {
        try {

            let canvas = this.canvas;

            if (!canvas.drawWidth || !canvas.drawHeight) {
                return this;
            }

            let [x, y, w, h] = [
                -canvas.drawX,
                -canvas.drawY,
                canvas.drawWidth,
                canvas.drawHeight
            ];
            let options = this.options;
            if (!canvas.elementClone.initialized) {
                let context = canvas.contextClone;

                // clear the cache
                context.clearRect(x, y, w, h);
                context.save();

                canvas.context.barDimensions = barDimensions(context, options, x, y, w, h);
                this.contextValueClone.barDimensions = canvas.context.barDimensions;
                this.contextProgressClone.barDimensions = canvas.context.barDimensions;

                drawBackground(context, options);

                drawDigitalTitle(context, options);

                if (!options.showTimestamp) {
                    drawDigitalLabel(context, options);
                }

                drawDigitalMinMax(context, options);

                canvas.elementClone.initialized = true;
            }

            var valueChanged = false;
            if (!this.elementValueClone.initialized || angular.isDefined(this._value) && this.elementValueClone.renderedValue !== this._value || (options.showTimestamp && this.elementValueClone.renderedTimestamp !== this.timestamp)) {
                if (angular.isDefined(this._value)) {
                    this.elementValueClone.renderedValue = this._value;
                }
                if (angular.isUndefined(this.elementValueClone.renderedValue)) {
                    this.elementValueClone.renderedValue = this.value;
                }
                let context = this.contextValueClone;
                // clear the cache
                context.clearRect(x, y, w, h);
                context.save();

                context.drawImage(canvas.elementClone, x, y, w, h);
                context.save();

                drawDigitalValue(context, options, this.elementValueClone.renderedValue);

                if (options.showTimestamp) {
                    drawDigitalLabel(context, options);
                    this.elementValueClone.renderedTimestamp = this.timestamp;
                }

                this.elementValueClone.initialized = true;

                valueChanged = true;
            }

            var progress = (canvasGauges.drawings.normalizedValue(options).normal - options.minValue) /
                (options.maxValue - options.minValue);

            var fixedProgress = progress.toFixed(3);

            if (!this.elementProgressClone.initialized || this.elementProgressClone.renderedProgress !== fixedProgress || valueChanged) {
                let context = this.contextProgressClone;
                // clear the cache
                context.clearRect(x, y, w, h);
                context.save();

                context.drawImage(this.elementValueClone, x, y, w, h);
                context.save();

                if (Number(fixedProgress) > 0) {
                    drawProgress(context, options, progress);
                }

                this.elementProgressClone.initialized = true;
                this.elementProgressClone.renderedProgress = fixedProgress;
            }

            this.canvas.commit();

            // clear the canvas
            canvas.context.clearRect(x, y, w, h);
            canvas.context.save();

            canvas.context.drawImage(this.elementProgressClone, x, y, w, h);
            canvas.context.save();

            super.draw();

        } catch (err) {
            canvasGauges.drawings.verifyError(err);
        }
        return this;
    }

    getValueColor() {
        if (this.contextProgressClone) {
            var color = this.contextProgressClone.currentColor;
            if (!color) {
                if (this.options.neonGlowBrightness) {
                    color = getProgressColor(0, this.options.neonColorsRange);
                } else {
                    color = getProgressColor(0, this.options.colorsRange);
                }
            }
            return color;
        } else {
            return '#000';
        }
    }
}

/* eslint-disable angular/document-service */
/* eslint-disable no-undef */
function determineFontHeight (options, target, baseSize) {
    var fontStyleStr = 'font-style:' + options['font' + target + 'Style'] + ';font-weight:' +
        options['font' + target + 'Weight'] + ';font-size:' +
        options['font' + target + 'Size'] * baseSize + 'px;font-family:' +
        options['font' + target];
    var result = CanvasDigitalGauge.heightCache[fontStyleStr];
    if (!result)
    {
        var fontStyle = {
            fontFamily: options['font' + target],
            fontSize: options['font' + target + 'Size'] * baseSize + 'px',
            fontWeight: options['font' + target + 'Weight'],
            fontStyle: options['font' + target + 'Style']
        };

        var text = $('<span>Hg</span>').css(fontStyle);
        var block = $('<div style="display: inline-block; width: 1px; height: 0px;"></div>');

        var div = $('<div></div>');
        div.append(text, block);

        var body = $('body');
        body.append(div);

        try {
            result = {};
            block.css({ verticalAlign: 'baseline' });
            result.ascent = block.offset().top - text.offset().top;
            block.css({ verticalAlign: 'bottom' });
            result.height = block.offset().top - text.offset().top;
            result.descent = result.height - result.ascent;
        } finally {
            div.remove();
        }
        CanvasDigitalGauge.heightCache[fontStyleStr] = result;
    }
    return result;
}

/* eslint-enable angular/document-service */
/* eslint-enable no-undef */

function barDimensions(context, options, x, y, w, h) {

    context.barDimensions = {
        baseX: x,
        baseY: y,
        width: w,
        height: h
    };
    
    var bd = context.barDimensions;

    var aspect = 1;

    if (options.gaugeType === 'horizontalBar') {
        aspect = options.title === '' ? 2.5 : 2;
    } else if (options.gaugeType === 'verticalBar') {
        aspect = options.hideMinMax ? 0.35 : 0.5;
    } else if (options.gaugeType === 'arc') {
        aspect = 1.5;
    }

    var currentAspect = w / h;
    if (currentAspect > aspect) {
        bd.width = (h * aspect);
        bd.height = h;
    } else {
        bd.width = w;
        bd.height = w / aspect;
    }

    bd.origBaseX = bd.baseX;
    bd.origBaseY = bd.baseY;
    bd.baseX += (w - bd.width) / 2;
    bd.baseY += (h - bd.height) / 2;

    if (options.gaugeType === 'donut') {
        bd.fontSizeFactor = Math.max(bd.width, bd.height) / 125;
    } else if (options.gaugeType === 'verticalBar' || (options.gaugeType === 'arc' && options.title === '')) {
        bd.fontSizeFactor = Math.max(bd.width, bd.height) / 150;
    } else {
        bd.fontSizeFactor = Math.max(bd.width, bd.height) / 200;
    }

    var gws = options.gaugeWidthScale;

    if (options.neonGlowBrightness) {
        options.fontTitleHeight = determineFontHeight(options, 'Title', bd.fontSizeFactor);
        options.fontLabelHeight = determineFontHeight(options, 'Label', bd.fontSizeFactor);
        options.fontValueHeight = determineFontHeight(options, 'Value', bd.fontSizeFactor);
        options.fontMinMaxHeight = determineFontHeight(options, 'MinMax', bd.fontSizeFactor);
    }

    if (options.gaugeType === 'donut') {
        bd.Ro = bd.width / 2 - bd.width / 20;
        bd.Cy = bd.baseY + bd.height / 2;
        if (options.title && options.title.length > 0) {
            var titleOffset = determineFontHeight(options, 'Title', bd.fontSizeFactor).height;
            titleOffset += bd.fontSizeFactor * 2;
            bd.titleY = bd.baseY + titleOffset;
            titleOffset += bd.fontSizeFactor * 2;
            bd.Cy += titleOffset/2;
            bd.Ro -= titleOffset/2;
        }
        bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws * 1.2;
        bd.Cx = bd.baseX + bd.width / 2;

    } else if (options.gaugeType === 'arc') {
        if (options.title && options.title.length > 0) {
            bd.Ro = bd.width / 2 - bd.width / 7;
            bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws;
        } else {
            bd.Ro = bd.width / 2 - bd.fontSizeFactor * 4;
            bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws * 1.2;
        }
        bd.Cx = bd.baseX + bd.width / 2;
        bd.Cy = bd.baseY + bd.height / 1.25;
    } else if (options.gaugeType === 'verticalBar') {
        bd.Ro = bd.width / 2 - bd.width / 10;
        bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws * (options.hideMinMax ? 4 : 2.5);
    } else { //horizontalBar
        bd.Ro = bd.width / 2 - bd.width / 10;
        bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws;
    }

    bd.strokeWidth = bd.Ro - bd.Ri;
    bd.Rm = bd.Ri + bd.strokeWidth * 0.5;

    bd.fontValueBaseline = 'alphabetic';
    bd.fontMinMaxBaseline = 'alphabetic';
    bd.fontMinMaxAlign = 'center';

    if (options.gaugeType === 'donut') {
        bd.fontValueBaseline = 'middle';
        if (options.label && options.label.length > 0) {
            var valueHeight = determineFontHeight(options, 'Value', bd.fontSizeFactor).height;
            var labelHeight = determineFontHeight(options, 'Label', bd.fontSizeFactor).height;
            var total = valueHeight + labelHeight;
            bd.labelY = bd.Cy + total/2;
            bd.valueY = bd.Cy - total/2 + valueHeight/2;
        } else {
            bd.valueY = bd.Cy;
        }
    } else if (options.gaugeType === 'arc') {
        bd.titleY = bd.Cy - bd.Ro - 12 * bd.fontSizeFactor;
        bd.valueY = bd.Cy;
        bd.labelY = bd.Cy + (8 + options.fontLabelSize) * bd.fontSizeFactor;
        bd.minY = bd.maxY = bd.labelY;
        if (options.roundedLineCap) {
            bd.minY += bd.strokeWidth/2;
            bd.maxY += bd.strokeWidth/2;
        }
        bd.minX = bd.Cx - bd.Rm;
        bd.maxX = bd.Cx + bd.Rm;
    } else if (options.gaugeType === 'horizontalBar') {
        bd.titleY = bd.baseY + 4 * bd.fontSizeFactor +
            (options.title === '' ? 0 : options.fontTitleSize * bd.fontSizeFactor);
        bd.titleBottom = bd.titleY + (options.title === '' ? 0 : 4) * bd.fontSizeFactor;

        bd.valueY = bd.titleBottom +
            (options.hideValue ? 0 : options.fontValueSize * bd.fontSizeFactor);

        bd.barTop = bd.valueY + 8 * bd.fontSizeFactor;
        bd.barBottom = bd.barTop + bd.strokeWidth;

        if (options.hideMinMax && options.label === '') {
            bd.labelY = bd.barBottom;
            bd.barLeft = bd.origBaseX + options.fontMinMaxSize/3 * bd.fontSizeFactor;
            bd.barRight = bd.origBaseX + w + /*bd.width*/ - options.fontMinMaxSize/3 * bd.fontSizeFactor;
        } else {
            context.font = canvasGauges.drawings.font(options, 'MinMax', bd.fontSizeFactor);
            var minTextWidth  = context.measureText(options.minValue+'').width;
            var maxTextWidth  = context.measureText(options.maxValue+'').width;
            var maxW = Math.max(minTextWidth, maxTextWidth);
            bd.minX = bd.origBaseX + maxW/2 + options.fontMinMaxSize/3 * bd.fontSizeFactor;
            bd.maxX = bd.origBaseX + w + /*bd.width*/ - maxW/2 - options.fontMinMaxSize/3 * bd.fontSizeFactor;
            bd.barLeft = bd.minX;
            bd.barRight = bd.maxX;
            bd.labelY = bd.barBottom + (8 + options.fontLabelSize) * bd.fontSizeFactor;
            bd.minY = bd.maxY = bd.labelY;
        }
    } else if (options.gaugeType === 'verticalBar') {
        bd.titleY = bd.baseY + ((options.title === '' ? 0 : options.fontTitleSize) + 8) * bd.fontSizeFactor;
        bd.titleBottom = bd.titleY + (options.title === '' ? 0 : 4) * bd.fontSizeFactor;

        bd.valueY = bd.titleBottom + (options.hideValue ? 0 : options.fontValueSize * bd.fontSizeFactor);
        bd.barTop = bd.valueY + 8 * bd.fontSizeFactor;

        bd.labelY = bd.baseY + bd.height - 16;
        if (options.label === '') {
            bd.barBottom = bd.labelY;
        } else {
            bd.barBottom = bd.labelY - (8 + options.fontLabelSize) * bd.fontSizeFactor;
        }
        bd.minX = bd.maxX =
            bd.baseX + bd.width/2 + bd.strokeWidth/2 + options.fontMinMaxSize/3 * bd.fontSizeFactor;
        bd.minY = bd.barBottom;
        bd.maxY = bd.barTop;
        bd.fontMinMaxBaseline = 'middle';
        bd.fontMinMaxAlign = 'left';
    }

    if (options.dashThickness) {
        var circumference;
        if (options.gaugeType === 'donut') {
            circumference = Math.PI * bd.Rm * 2;
        } else if (options.gaugeType === 'arc') {
            circumference = Math.PI * bd.Rm;
        } else if (options.gaugeType === 'horizontalBar') {
            circumference = bd.barRight - bd.barLeft;
        } else if (options.gaugeType === 'verticalBar') {
            circumference = bd.barBottom - bd.barTop;
        }
        var dashCount = Math.floor(circumference / (options.dashThickness * bd.fontSizeFactor));
        if (options.gaugeType === 'donut') {
            dashCount = (dashCount | 1) - 1;
        } else {
            dashCount = (dashCount - 1) | 1;
        }
        bd.dashLength = Math.ceil(circumference/dashCount);
    }

    return bd;
}

function drawBackground(context, options) {
    let {barLeft, barRight, barTop, barBottom, width, baseX, strokeWidth} =
        context.barDimensions;
    if (context.barDimensions.dashLength) {
        context.setLineDash([context.barDimensions.dashLength]);
    }
    context.beginPath();
    context.strokeStyle = options.gaugeColor;
    context.lineWidth = strokeWidth;
    if (options.roundedLineCap) {
        context.lineCap = 'round';
    }
    if (options.gaugeType === 'donut') {
        context.arc(context.barDimensions.Cx, context.barDimensions.Cy, context.barDimensions.Rm, options.donutStartAngle, options.donutEndAngle);
        context.stroke();
    } else if (options.gaugeType === 'arc') {
        context.arc(context.barDimensions.Cx, context.barDimensions.Cy, context.barDimensions.Rm, Math.PI, 2*Math.PI);
        context.stroke();
    } else if (options.gaugeType === 'horizontalBar') {
        context.moveTo(barLeft,barTop + strokeWidth/2);
        context.lineTo(barRight,barTop + strokeWidth/2);
        context.stroke();
    } else if (options.gaugeType === 'verticalBar') {
        context.moveTo(baseX + width/2, barBottom);
        context.lineTo(baseX + width/2, barTop);
        context.stroke();
    }
}

function drawText(context, options, target, text, textX, textY) {
    context.fillStyle = options[(options.neonGlowBrightness ? 'neonColor' : 'color') + target];
    context.fillText(text, textX, textY);
}

function drawDigitalTitle(context, options) {
    if (!options.title) return;

    let {titleY, width, baseX, fontSizeFactor} =
        context.barDimensions;

    let textX = round(baseX + width / 2);
    let textY = titleY;

    context.save();
    context.textAlign = 'center';
    context.font = canvasGauges.drawings.font(options, 'Title', fontSizeFactor);
    context.lineWidth = 0;
    drawText(context, options, 'Title', options.title.toUpperCase(), textX, textY);
}

function drawDigitalLabel(context, options) {
    if (!options.label || options.label === '') return;

    let {labelY, baseX, width, fontSizeFactor} =
        context.barDimensions;

    let textX = round(baseX + width / 2);
    let textY = labelY;

    context.save();
    context.textAlign = 'center';
    context.font = canvasGauges.drawings.font(options, 'Label', fontSizeFactor);
    context.lineWidth = 0;
    drawText(context, options, 'Label', options.label.toUpperCase(), textX, textY);

}

function drawDigitalMinMax(context, options) {
    if (options.hideMinMax || options.gaugeType === 'donut') return;

    let {minY, maxY, minX, maxX, fontSizeFactor, fontMinMaxAlign, fontMinMaxBaseline} =
        context.barDimensions;

    context.save();
    context.textAlign = fontMinMaxAlign;
    context.textBaseline = fontMinMaxBaseline;
    context.font = canvasGauges.drawings.font(options, 'MinMax', fontSizeFactor);
    context.lineWidth = 0;
    drawText(context, options, 'MinMax', options.minValue+'', minX, minY);
    drawText(context, options, 'MinMax', options.maxValue+'', maxX, maxY);
}

function padValue(val, options) {
    let dec = options.valueDec;
    let strVal, n;

    val = parseFloat(val);
    n = (val < 0);
    val = Math.abs(val);

    if (dec > 0) {
        strVal = val.toFixed(dec).toString()
    } else {
        strVal = round(val).toString();
    }
    strVal = (n ? '-' : '') + strVal;
    return strVal;
}

function drawDigitalValue(context, options, value) {
    if (options.hideValue) return;

    let {valueY, baseX, width, fontSizeFactor, fontValueBaseline} =
        context.barDimensions;

    let textX = round(baseX + width / 2);
    let textY = valueY;

    let text = options.valueText || padValue(value, options);
    text += options.symbol;

    context.save();
    context.textAlign = 'center';
    context.textBaseline = fontValueBaseline;
    context.font = canvasGauges.drawings.font(options, 'Value', fontSizeFactor);
    context.lineWidth = 0;
    drawText(context, options, 'Value', text, textX, textY);
}

function getProgressColor(progress, colorsRange) {

    var lower, upper, range, rangePct, pctLower, pctUpper, color;

    if (progress === 0 || colorsRange.length === 1) {
        return colorsRange[0].rgbString;
    }

    for (var j = 0; j < colorsRange.length; j++) {
        if (progress <= colorsRange[j].pct) {
            lower = colorsRange[j - 1];
            upper = colorsRange[j];
            range = upper.pct - lower.pct;
            rangePct = (progress - lower.pct) / range;
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

function drawArcGlow(context, Cx, Cy, Ri, Rm, Ro, color, progress, isDonut, donutStartAngle, donutEndAngle) {
    context.setLineDash([]);
    var strokeWidth = Ro - Ri;
    var blur = 0.55;
    var edge = strokeWidth*blur;
    context.lineWidth = strokeWidth+edge;
    var stop = blur/(2*blur+2);
    var glowGradient = context.createRadialGradient(Cx,Cy,Ri-edge/2,Cx,Cy,Ro+edge/2);
    var color1 = tinycolor(color).setAlpha(0.5).toRgbString();
    var color2 = tinycolor(color).setAlpha(0).toRgbString();
    glowGradient.addColorStop(0,color2);
    glowGradient.addColorStop(stop,color1);
    glowGradient.addColorStop(1.0-stop,color1);
    glowGradient.addColorStop(1,color2);
    context.strokeStyle = glowGradient;
    context.beginPath();
    var e = 0.01 * Math.PI;
    if (isDonut) {
        context.arc(Cx, Cy, Rm, donutStartAngle - e, donutStartAngle + (donutEndAngle - donutStartAngle) * progress + e);
    } else {
        context.arc(Cx, Cy, Rm, Math.PI - e, Math.PI + Math.PI * progress + e);
    }
    context.stroke();
}

function drawBarGlow(context, startX, startY, endX, endY, color, strokeWidth, isVertical) {
    context.setLineDash([]);
    var blur = 0.55;
    var edge = strokeWidth*blur;
    context.lineWidth = strokeWidth+edge;
    var stop = blur/(2*blur+2);
    var gradientStartX = isVertical ? startX - context.lineWidth/2 : 0;
    var gradientStartY = isVertical ? 0 : startY - context.lineWidth/2;
    var gradientStopX = isVertical ? startX + context.lineWidth/2 : 0;
    var gradientStopY = isVertical ? 0 : startY + context.lineWidth/2;

    var glowGradient = context.createLinearGradient(gradientStartX,gradientStartY,gradientStopX,gradientStopY);
    var color1 = tinycolor(color).setAlpha(0.5).toRgbString();
    var color2 = tinycolor(color).setAlpha(0).toRgbString();
    glowGradient.addColorStop(0,color2);
    glowGradient.addColorStop(stop,color1);
    glowGradient.addColorStop(1.0-stop,color1);
    glowGradient.addColorStop(1,color2);
    context.strokeStyle = glowGradient;
    var dx = isVertical ? 0 : 0.05 * context.lineWidth;
    var dy = isVertical ? 0.05 * context.lineWidth : 0;
    context.beginPath();
    context.moveTo(startX - dx, startY + dy);
    context.lineTo(endX + dx, endY - dy);
    context.stroke();
}

function drawTickArc(context, tickValues, Cx, Cy, Ri, Rm, Ro, startAngle, endAngle, color, tickWidth) {
    if(!tickValues.length) {
        return;
    }

    const strokeWidth = Ro - Ri;
    context.beginPath();
    context.lineWidth = tickWidth;
    context.strokeStyle = color;
    for (let i = 0; i < tickValues.length; i++) {
        var angle = startAngle + tickValues[i] * endAngle;
        var x1 = Cx + (Ri + strokeWidth) * Math.cos(angle);
        var y1 = Cy + (Ri + strokeWidth) * Math.sin(angle);
        var x2 = Cx + Ri * Math.cos(angle);
        var y2 = Cy + Ri * Math.sin(angle);
        context.moveTo(x1, y1);
        context.lineTo(x2, y2);
    }
    context.stroke();
}

function drawTickBar(context, tickValues, startX, startY, distanceBar, strokeWidth, isVertical, color, tickWidth) {
    if(!tickValues.length) {
        return;
    }

    context.beginPath();
    context.lineWidth = tickWidth;
    context.strokeStyle = color;
    for (let i = 0; i < tickValues.length; i++) {
        let tickValue = tickValues[i] * distanceBar;
        if (isVertical) {
            context.moveTo(startX - strokeWidth / 2, startY + tickValue - distanceBar);
            context.lineTo(startX + strokeWidth / 2, startY + tickValue - distanceBar);
        } else {
            context.moveTo(startX + tickValue, startY);
            context.lineTo(startX + tickValue, startY + strokeWidth);
        }
    }
    context.stroke();
}

function drawProgress(context, options, progress) {
    var neonColor;
    if (options.neonGlowBrightness) {
        context.currentColor = neonColor = getProgressColor(progress, options.neonColorsRange);
    } else {
        context.currentColor = context.strokeStyle = getProgressColor(progress, options.colorsRange);
    }

    let {barLeft, barRight, barTop, baseX, width, barBottom, Cx, Cy, Rm, Ro, Ri, strokeWidth} =
        context.barDimensions;

    if (context.barDimensions.dashLength) {
        context.setLineDash([context.barDimensions.dashLength]);
    }
    context.lineWidth = strokeWidth;
    if (options.roundedLineCap) {
        context.lineCap = 'round';
    } else {
        context.lineCap = 'butt';
    }
    if (options.gaugeType === 'donut') {
        if (options.neonGlowBrightness) {
            context.strokeStyle = neonColor;
        }
        context.beginPath();
        context.arc(Cx, Cy, Rm, options.donutStartAngle, options.donutStartAngle + (options.donutEndAngle - options.donutStartAngle) * progress);
        context.stroke();
        if (options.neonGlowBrightness && !options.isMobile) {
            drawArcGlow(context, Cx, Cy, Ri, Rm, Ro, neonColor, progress, true, options.donutStartAngle, options.donutEndAngle);
        }
        drawTickArc(context, options.ticksValue, Cx, Cy, Ri, Rm, Ro, options.donutStartAngle, options.donutEndAngle - options.donutStartAngle, options.colorTicks, options.tickWidth);
    } else if (options.gaugeType === 'arc') {
        if (options.neonGlowBrightness) {
            context.strokeStyle = neonColor;
        }
        context.beginPath();
        context.arc(Cx, Cy, Rm, Math.PI, Math.PI + Math.PI * progress);
        context.stroke();
        if (options.neonGlowBrightness && !options.isMobile) {
            drawArcGlow(context, Cx, Cy, Ri, Rm, Ro, neonColor, progress, false);
        }
        drawTickArc(context, options.ticksValue, Cx, Cy, Ri, Rm, Ro, Math.PI, Math.PI, options.colorTicks, options.tickWidth);
    } else if (options.gaugeType === 'horizontalBar') {
        if (options.neonGlowBrightness) {
            context.strokeStyle = neonColor;
        }
        context.beginPath();
        context.moveTo(barLeft,barTop + strokeWidth/2);
        context.lineTo(barLeft + (barRight-barLeft)*progress, barTop + strokeWidth/2);
        context.stroke();
        if (options.neonGlowBrightness && !options.isMobile) {
            drawBarGlow(context, barLeft, barTop + strokeWidth/2, barLeft + (barRight-barLeft)*progress, barTop + strokeWidth/2,
                neonColor, strokeWidth, false);
        }
        drawTickBar(context, options.ticksValue, barLeft, barTop, barRight - barLeft, strokeWidth, false, options.colorTicks, options.tickWidth);
    } else if (options.gaugeType === 'verticalBar') {
        if (options.neonGlowBrightness) {
            context.strokeStyle = neonColor;
        }
        context.beginPath();
        context.moveTo(baseX + width/2, barBottom);
        context.lineTo(baseX + width/2, barBottom - (barBottom-barTop)*progress);
        context.stroke();
        if (options.neonGlowBrightness && !options.isMobile) {
            drawBarGlow(context, baseX + width/2, barBottom, baseX + width/2, barBottom - (barBottom-barTop)*progress,
                neonColor, strokeWidth, true);
        }
        drawTickBar(context, options.ticksValue, baseX + width / 2, barTop, barTop - barBottom, strokeWidth, true, options.colorTicks, options.tickWidth);
    }
}

CanvasDigitalGauge.heightCache = [];

canvasGauges.BaseGauge.initialize('CanvasDigitalGauge', defaultDigitalGaugeOptions);

/* eslint-enable angular/angularelement */
