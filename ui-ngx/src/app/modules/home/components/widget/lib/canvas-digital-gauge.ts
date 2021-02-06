///
/// Copyright © 2016-2021 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import * as CanvasGauges from 'canvas-gauges';
import { FontStyle, FontWeight } from '@home/components/widget/lib/settings.models';
import * as tinycolor_ from 'tinycolor2';
import { ColorFormats } from 'tinycolor2';
import { isDefined, isDefinedAndNotNull, isString, isUndefined, padValue } from '@core/utils';
import GenericOptions = CanvasGauges.GenericOptions;
import BaseGauge = CanvasGauges.BaseGauge;

const tinycolor = tinycolor_;

export type GaugeType = 'arc' | 'donut' | 'horizontalBar' | 'verticalBar';

export interface DigitalGaugeColorRange {
  pct: number;
  color: ColorFormats.RGBA;
  rgbString: string;
}

export interface ColorLevelSetting {
  value: number;
  color: string;
}

export type levelColors = Array<string | ColorLevelSetting>;

export interface CanvasDigitalGaugeOptions extends GenericOptions {
  gaugeType?: GaugeType;
  gaugeWithScale?: number;
  dashThickness?: number;
  roundedLineCap?: boolean;
  gaugeColor?: string;
  levelColors?: levelColors;
  symbol?: string;
  label?: string;
  hideValue?: boolean;
  hideMinMax?: boolean;
  fontTitle?: string;
  fontValue?: string;
  fontMinMaxSize?: number;
  fontMinMaxStyle?: FontStyle;
  fontMinMaxWeight?: FontWeight;
  colorMinMax?: string;
  fontMinMax?: string;
  fontLabelSize?: number;
  fontLabelStyle?: FontStyle;
  fontLabelWeight?: FontWeight;
  colorLabel?: string;
  colorValue?: string;
  fontLabel?: string;
  neonGlowBrightness?: number;
  isMobile?: boolean;
  donutStartAngle?: number;
  donutEndAngle?: number;

  colorsRange?: DigitalGaugeColorRange[];
  neonColorsRange?: DigitalGaugeColorRange[];
  neonColorTitle?: string;
  neonColorLabel?: string;
  neonColorValue?: string;
  neonColorMinMax?: string;
  timestamp?: number;
  gaugeWidthScale?: number;
  fontTitleHeight?: FontHeightInfo;
  fontLabelHeight?: FontHeightInfo;
  fontValueHeight?: FontHeightInfo;
  fontMinMaxHeight?: FontHeightInfo;

  ticksValue?: number[];
  ticks?: number[];
  colorTicks?: string;
  tickWidth?: number;

  showTimestamp?: boolean;
}

const defaultDigitalGaugeOptions: CanvasDigitalGaugeOptions = { ...GenericOptions,
  ...{
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
  }
};

BaseGauge.initialize('CanvasDigitalGauge', defaultDigitalGaugeOptions);

interface HTMLCanvasElementClone extends HTMLCanvasElement {
  initialized?: boolean;
  renderedTimestamp?: number;
  renderedValue?: number;
  renderedProgress?: string;
}

interface DigitalGaugeCanvasRenderingContext2D extends CanvasRenderingContext2D {
  barDimensions?: BarDimensions;
  currentColor?: string;
}

interface BarDimensions {
  baseX: number;
  baseY: number;
  width: number;
  height: number;
  origBaseX?: number;
  origBaseY?: number;
  fontSizeFactor?: number;
  Ro?: number;
  Cy?: number;
  titleY?: number;
  titleBottom?: number;
  Ri?: number;
  Cx?: number;
  strokeWidth?: number;
  Rm?: number;
  fontValueBaseline?: CanvasTextBaseline;
  fontMinMaxBaseline?: CanvasTextBaseline;
  fontMinMaxAlign?: CanvasTextAlign;
  labelY?: number;
  valueY?: number;
  minY?: number;
  maxY?: number;
  minX?: number;
  maxX?: number;
  barTop?: number;
  barBottom?: number;
  barLeft?: number;
  barRight?: number;
  dashLength?: number;
}

interface FontHeightInfo {
  ascent?: number;
  height?: number;
  descent?: number;
}

export class Drawings {
  static font(options: CanvasGauges.GenericOptions, target: string, baseSize: number): string {
    return options['font' + target + 'Style'] + ' ' +
      options['font' + target + 'Weight'] + ' ' +
      options['font' + target + 'Size'] * baseSize + 'px ' +
      options['font' + target];
  }
  static normalizedValue(options: CanvasGauges.GenericOptions): {normal: number, indented: number} {
    const value = options.value;
    const min = options.minValue;
    const max = options.maxValue;
    const dt = (max - min) * 0.01;
    return {
      normal: value < min ? min : value > max ? max : value,
      indented: value < min ? min - dt : value > max ? max + dt : value
    };
  }
  static verifyError(err: any) {
    if (err instanceof DOMException && (err as any).result === 0x8053000b) {
      return ; // ignore it
    }
    throw err;
  }
}

export class CanvasDigitalGauge extends BaseGauge {

  static heightCache: {[key: string]: FontHeightInfo} = {};

  private elementValueClone: HTMLCanvasElementClone;
  private contextValueClone: DigitalGaugeCanvasRenderingContext2D;

  private elementProgressClone: HTMLCanvasElementClone;
  private contextProgressClone: DigitalGaugeCanvasRenderingContext2D;

  public _value: number;

  constructor(options: CanvasDigitalGaugeOptions) {
    options = {...defaultDigitalGaugeOptions, ...(options || {})};
    super(CanvasDigitalGauge.configure(options));
    this.initValueClone();
  }

  static configure(options: CanvasDigitalGaugeOptions): CanvasDigitalGaugeOptions {

    if (options.value > options.maxValue) {
      options.value = options.maxValue;
    }

    if (options.value < options.minValue) {
      options.value = options.minValue;
    }

    if (options.gaugeType === 'donut') {
      if (!isDefinedAndNotNull(options.donutStartAngle)) {
        options.donutStartAngle = 1.5 * Math.PI;
      }
      if (!isDefinedAndNotNull(options.donutEndAngle)) {
        options.donutEndAngle = options.donutStartAngle + 2 * Math.PI;
      }
    }

    const colorsCount = options.levelColors.length;
    const isColorProperty = isString(options.levelColors[0]);
    const inc = colorsCount > 1 ? (1 / (colorsCount - 1)) : 1;
    options.colorsRange = [];
    if (options.neonGlowBrightness) {
      options.neonColorsRange = [];
    }
    for (let i = 0; i < options.levelColors.length; i++) {
      const levelColor: any = options.levelColors[i];
      if (levelColor !== null) {
        let percentage: number;
        if (isColorProperty) {
          percentage = inc * i;
        } else {
          percentage = CanvasDigitalGauge.normalizeValue(levelColor.value, options.minValue, options.maxValue);
        }
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
    for (const tick of options.ticks) {
      if (tick !== null) {
        options.ticksValue.push(CanvasDigitalGauge.normalizeValue(tick, options.minValue, options.maxValue));
      }
    }

    if (options.neonGlowBrightness) {
      options.neonColorTitle = tinycolor(options.colorTitle).brighten(options.neonGlowBrightness).toHexString();
      options.neonColorLabel = tinycolor(options.colorLabel).brighten(options.neonGlowBrightness).toHexString();
      options.neonColorValue = tinycolor(options.colorValue).brighten(options.neonGlowBrightness).toHexString();
      options.neonColorMinMax = tinycolor(options.colorMinMax).brighten(options.neonGlowBrightness).toHexString();
    }

    return options;
  }

  static normalizeValue(value: number, min: number, max: number): number {
    const normalValue = (value - min) / (max - min);
    if (normalValue <= 0) {
      return 0;
    }
    if (normalValue >= 1) {
      return 1;
    }
    return normalValue;
  }

  private initValueClone() {
    const canvas = this.canvas;
    this.elementValueClone = canvas.element.cloneNode(true) as HTMLCanvasElementClone;
    this.contextValueClone = this.elementValueClone.getContext('2d');
    this.elementValueClone.initialized = false;

    this.contextValueClone.translate(canvas.drawX, canvas.drawY);
    this.contextValueClone.save();

    this.elementProgressClone = canvas.element.cloneNode(true) as HTMLCanvasElementClone;
    this.contextProgressClone = this.elementProgressClone.getContext('2d');
    this.elementProgressClone.initialized = false;

    this.contextProgressClone.translate(canvas.drawX, canvas.drawY);
    this.contextProgressClone.save();
  }

  destroy() {
    this.contextValueClone = null;
    this.elementValueClone = null;
    this.contextProgressClone = null;
    this.elementProgressClone = null;
    super.destroy();
  }

  update(options: GenericOptions): BaseGauge {
    this.canvas.onRedraw = null;
    const result = super.update(options);
    this.initValueClone();
    this.canvas.onRedraw = this.draw.bind(this);
    this.draw();
    return result;
  }

  set timestamp(timestamp: number) {
    (this.options as CanvasDigitalGaugeOptions).timestamp = timestamp;
    this.draw();
  }

  get timestamp(): number {
    return (this.options as CanvasDigitalGaugeOptions).timestamp;
  }

  draw(): CanvasDigitalGauge {
    try {
      const canvas = this.canvas;
      if (!canvas.drawWidth || !canvas.drawHeight) {
        return this;
      }
      const [x, y, w, h] = [
        -canvas.drawX,
        -canvas.drawY,
        canvas.drawWidth,
        canvas.drawHeight
      ];
      const options = this.options as CanvasDigitalGaugeOptions;
      const elementClone = canvas.elementClone as HTMLCanvasElementClone;
      if (!elementClone.initialized) {
        const context = canvas.contextClone;

        // clear the cache
        context.clearRect(x, y, w, h);
        context.save();

        const canvasContext = canvas.context as DigitalGaugeCanvasRenderingContext2D;

        canvasContext.barDimensions = barDimensions(context, options, x, y, w, h);
        this.contextValueClone.barDimensions = canvasContext.barDimensions;
        this.contextProgressClone.barDimensions = canvasContext.barDimensions;

        drawBackground(context, options);

        drawDigitalTitle(context, options);

        if (!options.showTimestamp) {
          drawDigitalLabel(context, options);
        }

        drawDigitalMinMax(context, options);

        elementClone.initialized = true;
      }

      let valueChanged = false;
      if (!this.elementValueClone.initialized ||
           isDefined(this._value) && this.elementValueClone.renderedValue !== this._value ||
           (options.showTimestamp && this.elementValueClone.renderedTimestamp !== this.timestamp)) {
        if (isDefined(this._value)) {
          this.elementValueClone.renderedValue = this._value;
        }
        if (isUndefined(this.elementValueClone.renderedValue)) {
          this.elementValueClone.renderedValue = this.value;
        }
        const context = this.contextValueClone;
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

      const progress = (Drawings.normalizedValue(options).normal - options.minValue) /
        (options.maxValue - options.minValue);

      const fixedProgress = progress.toFixed(3);

      if (!this.elementProgressClone.initialized || this.elementProgressClone.renderedProgress !== fixedProgress || valueChanged) {
        const context = this.contextProgressClone;
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

      // @ts-ignore
      super.draw();

    } catch (err) {
      Drawings.verifyError(err);
    }
    return this;
  }

  getValueColor() {
    if (this.contextProgressClone) {
      let color = this.contextProgressClone.currentColor;
      const options = this.options as CanvasDigitalGaugeOptions;
      if (!color) {
        const progress = (Drawings.normalizedValue(options).normal - options.minValue) /
          (options.maxValue - options.minValue);
        if (options.neonGlowBrightness) {
          color = getProgressColor(progress, options.neonColorsRange);
        } else {
          color = getProgressColor(progress, options.colorsRange);
        }
      }
      return color;
    } else {
      return '#000';
    }
  }
}

function barDimensions(context: DigitalGaugeCanvasRenderingContext2D,
                       options: CanvasDigitalGaugeOptions,
                       x: number, y: number, w: number, h: number): BarDimensions {
  context.barDimensions = {
    baseX: x,
    baseY: y,
    width: w,
    height: h
  };

  const bd = context.barDimensions;

  let aspect = 1;

  if (options.gaugeType === 'horizontalBar') {
    aspect = options.title === '' ? 2.5 : 2;
  } else if (options.gaugeType === 'verticalBar') {
    aspect = options.hideMinMax ? 0.35 : 0.5;
  } else if (options.gaugeType === 'arc') {
    aspect = 1.5;
  }

  const currentAspect = w / h;
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

  const gws = options.gaugeWidthScale;

  if (options.neonGlowBrightness) {
    options.fontTitleHeight = determineFontHeight(options, 'Title', bd.fontSizeFactor);
    options.fontLabelHeight = determineFontHeight(options, 'Label', bd.fontSizeFactor);
    options.fontValueHeight = determineFontHeight(options, 'Value', bd.fontSizeFactor);
    options.fontMinMaxHeight = determineFontHeight(options, 'MinMax', bd.fontSizeFactor);
  }

  if (options.gaugeType === 'donut') {
    bd.Ro = bd.width / 2 - bd.width / 20;
    bd.Cy = bd.baseY + bd.height / 2;
    if (options.title && typeof options.title === 'string' && options.title.length > 0) {
      let titleOffset = determineFontHeight(options, 'Title', bd.fontSizeFactor).height;
      titleOffset += bd.fontSizeFactor * 2;
      bd.titleY = bd.baseY + titleOffset;
      titleOffset += bd.fontSizeFactor * 2;
      bd.Cy += titleOffset / 2;
      bd.Ro -= titleOffset / 2;
    }
    bd.Ri = bd.Ro - bd.width / 6.666666666666667 * gws * 1.2;
    bd.Cx = bd.baseX + bd.width / 2;
  } else if (options.gaugeType === 'arc') {
    if (options.title && typeof options.title === 'string' && options.title.length > 0) {
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
  } else { // horizontalBar
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
      const valueHeight = determineFontHeight(options, 'Value', bd.fontSizeFactor).height;
      const labelHeight = determineFontHeight(options, 'Label', bd.fontSizeFactor).height;
      const total = valueHeight + labelHeight;
      bd.labelY = bd.Cy + total / 2;
      bd.valueY = bd.Cy - total / 2 + valueHeight / 2;
    } else {
      bd.valueY = bd.Cy;
    }
  } else if (options.gaugeType === 'arc') {
    bd.titleY = bd.Cy - bd.Ro - 12 * bd.fontSizeFactor;
    bd.valueY = bd.Cy;
    bd.labelY = bd.Cy + (8 + options.fontLabelSize) * bd.fontSizeFactor;
    bd.minY = bd.maxY = bd.labelY;
    if (options.roundedLineCap) {
      bd.minY += bd.strokeWidth / 2;
      bd.maxY += bd.strokeWidth / 2;
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
      bd.barLeft = bd.origBaseX + options.fontMinMaxSize / 3 * bd.fontSizeFactor;
      bd.barRight = bd.origBaseX + w + /*bd.width*/ -options.fontMinMaxSize / 3 * bd.fontSizeFactor;
    } else {
      context.font = Drawings.font(options, 'MinMax', bd.fontSizeFactor);
      const minTextWidth = context.measureText(options.minValue + '').width;
      const maxTextWidth = context.measureText(options.maxValue + '').width;
      const maxW = Math.max(minTextWidth, maxTextWidth);
      bd.minX = bd.origBaseX + maxW / 2 + options.fontMinMaxSize / 3 * bd.fontSizeFactor;
      bd.maxX = bd.origBaseX + w + /*bd.width*/ -maxW / 2 - options.fontMinMaxSize / 3 * bd.fontSizeFactor;
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
      bd.baseX + bd.width / 2 + bd.strokeWidth / 2 + options.fontMinMaxSize / 3 * bd.fontSizeFactor;
    bd.minY = bd.barBottom;
    bd.maxY = bd.barTop;
    bd.fontMinMaxBaseline = 'middle';
    bd.fontMinMaxAlign = 'left';
  }

  if (options.dashThickness) {
    let circumference;
    if (options.gaugeType === 'donut') {
      circumference = Math.PI * bd.Rm * 2;
    } else if (options.gaugeType === 'arc') {
      circumference = Math.PI * bd.Rm;
    } else if (options.gaugeType === 'horizontalBar') {
      circumference = bd.barRight - bd.barLeft;
    } else if (options.gaugeType === 'verticalBar') {
      circumference = bd.barBottom - bd.barTop;
    }
    let dashCount = Math.floor(circumference / (options.dashThickness * bd.fontSizeFactor));
    if (options.gaugeType === 'donut') {
      // tslint:disable-next-line:no-bitwise
      dashCount = (dashCount | 1) - 1;
    } else {
      // tslint:disable-next-line:no-bitwise
      dashCount = (dashCount - 1) | 1;
    }
    bd.dashLength = Math.ceil(circumference / dashCount);
  }

  return bd;
}

function determineFontHeight(options: CanvasDigitalGaugeOptions, target: string, baseSize: number): FontHeightInfo {
  const fontStyleStr = 'font-style:' + options['font' + target + 'Style'] + ';font-weight:' +
    options['font' + target + 'Weight'] + ';font-size:' +
    options['font' + target + 'Size'] * baseSize + 'px;font-family:' +
    options['font' + target];
  let result = CanvasDigitalGauge.heightCache[fontStyleStr];
  if (!result) {
    const fontStyle = {
      fontFamily: options['font' + target],
      fontSize: options['font' + target + 'Size'] * baseSize + 'px',
      fontWeight: options['font' + target + 'Weight'],
      fontStyle: options['font' + target + 'Style']
    };
    const text = $('<span>Hg</span>').css(fontStyle);
    const block = $('<div style="display: inline-block; width: 1px; height: 0;"></div>');

    const div = $('<div></div>');
    div.append(text, block);

    const body = $('body');
    body.append(div);

    try {
      result = {};
      block.css({verticalAlign: 'baseline'});
      result.ascent = block.offset().top - text.offset().top;
      block.css({verticalAlign: 'bottom'});
      result.height = block.offset().top - text.offset().top;
      result.descent = result.height - result.ascent;
    } finally {
      div.remove();
    }

    CanvasDigitalGauge.heightCache[fontStyleStr] = result;
  }
  return result;
}

function drawBackground(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions) {
  const {barLeft, barRight, barTop, barBottom, width, baseX, strokeWidth} =
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
    context.arc(context.barDimensions.Cx, context.barDimensions.Cy, context.barDimensions.Rm,
      options.donutStartAngle, options.donutEndAngle);
    context.stroke();
  } else if (options.gaugeType === 'arc') {
    context.arc(context.barDimensions.Cx, context.barDimensions.Cy,
      context.barDimensions.Rm, Math.PI, 2 * Math.PI);
    context.stroke();
  } else if (options.gaugeType === 'horizontalBar') {
    context.moveTo(barLeft, barTop + strokeWidth / 2);
    context.lineTo(barRight, barTop + strokeWidth / 2);
    context.stroke();
  } else if (options.gaugeType === 'verticalBar') {
    context.moveTo(baseX + width / 2, barBottom);
    context.lineTo(baseX + width / 2, barTop);
    context.stroke();
  }
}

function drawText(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions,
                  target: string, text: string, textX: number, textY: number) {
  context.fillStyle = options[(options.neonGlowBrightness ? 'neonColor' : 'color') + target];
  context.fillText(text, textX, textY);
}

function drawDigitalTitle(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions) {
  if (!options.title || typeof options.title !== 'string') {
    return;
  }

  const {titleY, width, baseX, fontSizeFactor} =
    context.barDimensions;

  const textX = Math.round(baseX + width / 2);
  const textY = titleY;

  context.save();
  context.textAlign = 'center';
  context.font = Drawings.font(options, 'Title', fontSizeFactor);
  context.lineWidth = 0;
  drawText(context, options, 'Title', options.title.toUpperCase(), textX, textY);
}

function drawDigitalLabel(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions) {
  if (!options.label || options.label === '') {
    return;
  }

  const {labelY, baseX, width, fontSizeFactor} =
    context.barDimensions;

  const textX = Math.round(baseX + width / 2);
  const textY = labelY;

  context.save();
  context.textAlign = 'center';
  context.font = Drawings.font(options, 'Label', fontSizeFactor);
  context.lineWidth = 0;
  drawText(context, options, 'Label', options.label.toUpperCase(), textX, textY);
}

function drawDigitalMinMax(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions) {
  if (options.hideMinMax || options.gaugeType === 'donut') {
    return;
  }

  const {minY, maxY, minX, maxX, fontSizeFactor, fontMinMaxAlign, fontMinMaxBaseline} =
    context.barDimensions;

  context.save();
  context.textAlign = fontMinMaxAlign;
  context.textBaseline = fontMinMaxBaseline;
  context.font = Drawings.font(options, 'MinMax', fontSizeFactor);
  context.lineWidth = 0;
  drawText(context, options, 'MinMax', options.minValue + '', minX, minY);
  drawText(context, options, 'MinMax', options.maxValue + '', maxX, maxY);
}

function drawDigitalValue(context: DigitalGaugeCanvasRenderingContext2D, options: CanvasDigitalGaugeOptions, value: any) {
  if (options.hideValue) {
    return;
  }

  const {valueY, baseX, width, fontSizeFactor, fontValueBaseline} =
    context.barDimensions;

  const textX = Math.round(baseX + width / 2);
  const textY = valueY;

  let text = options.valueText || padValue(value, options.valueDec);
  text += options.symbol;

  context.save();
  context.textAlign = 'center';
  context.textBaseline = fontValueBaseline;
  context.font = Drawings.font(options, 'Value', fontSizeFactor);
  context.lineWidth = 0;
  drawText(context, options, 'Value', text, textX, textY);
}

function getProgressColor(progress: number, colorsRange: DigitalGaugeColorRange[]): string {

  if (progress === 0 || colorsRange.length === 1) {
    return colorsRange[0].rgbString;
  }

  for (let j = 0; j < colorsRange.length; j++) {
    if (progress <= colorsRange[j].pct) {
      const lower = colorsRange[j - 1];
      const upper = colorsRange[j];
      const range = upper.pct - lower.pct;
      const rangePct = (progress - lower.pct) / range;
      const pctLower = 1 - rangePct;
      const pctUpper = rangePct;
      const color = tinycolor({
        r: Math.floor(lower.color.r * pctLower + upper.color.r * pctUpper),
        g: Math.floor(lower.color.g * pctLower + upper.color.g * pctUpper),
        b: Math.floor(lower.color.b * pctLower + upper.color.b * pctUpper)
      });
      return color.toRgbString();
    }
  }
}

function drawArcGlow(context: DigitalGaugeCanvasRenderingContext2D,
                     Cx: number, Cy: number, Ri: number, Rm: number, Ro: number,
                     color: string, progress: number, isDonut: boolean,
                     donutStartAngle?: number, donutEndAngle?: number) {
  context.setLineDash([]);
  const strokeWidth = Ro - Ri;
  const blur = 0.55;
  const edge = strokeWidth * blur;
  context.lineWidth = strokeWidth + edge;
  const stop = blur / (2 * blur + 2);
  const glowGradient = context.createRadialGradient(Cx, Cy, Ri - edge / 2, Cx, Cy, Ro + edge / 2);
  const color1 = tinycolor(color).setAlpha(0.5).toRgbString();
  const color2 = tinycolor(color).setAlpha(0).toRgbString();
  glowGradient.addColorStop(0, color2);
  glowGradient.addColorStop(stop, color1);
  glowGradient.addColorStop(1.0 - stop, color1);
  glowGradient.addColorStop(1, color2);
  context.strokeStyle = glowGradient;
  context.beginPath();
  const e = 0.01 * Math.PI;
  if (isDonut) {
    context.arc(Cx, Cy, Rm, donutStartAngle - e, donutStartAngle +
      (donutEndAngle - donutStartAngle) * progress + e);
  } else {
    context.arc(Cx, Cy, Rm, Math.PI - e, Math.PI + Math.PI * progress + e);
  }
  context.stroke();
}

function drawBarGlow(context: DigitalGaugeCanvasRenderingContext2D, startX: number, startY: number,
                     endX: number, endY: number, color: string, strokeWidth: number, isVertical: boolean) {
  context.setLineDash([]);
  const blur = 0.55;
  const edge = strokeWidth * blur;
  context.lineWidth = strokeWidth + edge;
  const stop = blur / (2 * blur + 2);
  const gradientStartX = isVertical ? startX - context.lineWidth / 2 : 0;
  const gradientStartY = isVertical ? 0 : startY - context.lineWidth / 2;
  const gradientStopX = isVertical ? startX + context.lineWidth / 2 : 0;
  const gradientStopY = isVertical ? 0 : startY + context.lineWidth / 2;

  const glowGradient = context.createLinearGradient(gradientStartX, gradientStartY, gradientStopX, gradientStopY);
  const color1 = tinycolor(color).setAlpha(0.5).toRgbString();
  const color2 = tinycolor(color).setAlpha(0).toRgbString();
  glowGradient.addColorStop(0, color2);
  glowGradient.addColorStop(stop, color1);
  glowGradient.addColorStop(1.0 - stop, color1);
  glowGradient.addColorStop(1, color2);
  context.strokeStyle = glowGradient;
  const dx = isVertical ? 0 : 0.05 * context.lineWidth;
  const dy = isVertical ? 0.05 * context.lineWidth : 0;
  context.beginPath();
  context.moveTo(startX - dx, startY + dy);
  context.lineTo(endX + dx, endY - dy);
  context.stroke();
}

function drawTickArc(context: DigitalGaugeCanvasRenderingContext2D, tickValues: number[], Cx: number, Cy: number,
                     Ri: number, Rm: number, Ro: number, startAngle: number, endAngle: number,
                     color: string, tickWidth: number) {
  if (!tickValues.length) {
    return;
  }

  const strokeWidth = Ro - Ri;
  context.beginPath();
  context.lineWidth = tickWidth;
  context.strokeStyle = color;
  for (const tick of tickValues) {
    const angle = startAngle + tick * endAngle;
    const x1 = Cx + (Ri + strokeWidth) * Math.cos(angle);
    const y1 = Cy + (Ri + strokeWidth) * Math.sin(angle);
    const x2 = Cx + Ri * Math.cos(angle);
    const y2 = Cy + Ri * Math.sin(angle);
    context.moveTo(x1, y1);
    context.lineTo(x2, y2);
  }
  context.stroke();
}

function drawTickBar(context: DigitalGaugeCanvasRenderingContext2D, tickValues: number[], startX: number, startY: number,
                     distanceBar: number, strokeWidth: number, isVertical: boolean, color: string, tickWidth: number) {
  if (!tickValues.length) {
    return;
  }

  context.beginPath();
  context.lineWidth = tickWidth;
  context.strokeStyle = color;
  for (const tick of tickValues) {
    const tickValue = tick * distanceBar;
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


function drawProgress(context: DigitalGaugeCanvasRenderingContext2D,
                      options: CanvasDigitalGaugeOptions, progress: number) {
  let neonColor;
  if (options.neonGlowBrightness) {
    context.currentColor = neonColor = getProgressColor(progress, options.neonColorsRange);
  } else {
    context.currentColor = context.strokeStyle = getProgressColor(progress, options.colorsRange);
  }

  const {barLeft, barRight, barTop, baseX, width, barBottom, Cx, Cy, Rm, Ro, Ri, strokeWidth} =
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
    context.arc(Cx, Cy, Rm, options.donutStartAngle, options.donutStartAngle +
      (options.donutEndAngle - options.donutStartAngle) * progress);
    context.stroke();
    if (options.neonGlowBrightness && !options.isMobile) {
      drawArcGlow(context, Cx, Cy, Ri, Rm, Ro, neonColor, progress, true,
        options.donutStartAngle, options.donutEndAngle);
    }
    drawTickArc(context, options.ticksValue, Cx, Cy, Ri, Rm, Ro, options.donutStartAngle,
      options.donutEndAngle - options.donutStartAngle, options.colorTicks, options.tickWidth);
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
    context.moveTo(barLeft, barTop + strokeWidth / 2);
    context.lineTo(barLeft + (barRight - barLeft) * progress, barTop + strokeWidth / 2);
    context.stroke();
    if (options.neonGlowBrightness && !options.isMobile) {
      drawBarGlow(context, barLeft, barTop + strokeWidth / 2,
        barLeft + (barRight - barLeft) * progress, barTop + strokeWidth / 2,
        neonColor, strokeWidth, false);
    }
    drawTickBar(context, options.ticksValue, barLeft, barTop, barRight - barLeft, strokeWidth,
      false, options.colorTicks, options.tickWidth);
  } else if (options.gaugeType === 'verticalBar') {
    if (options.neonGlowBrightness) {
      context.strokeStyle = neonColor;
    }
    context.beginPath();
    context.moveTo(baseX + width / 2, barBottom);
    context.lineTo(baseX + width / 2, barBottom - (barBottom - barTop) * progress);
    context.stroke();
    if (options.neonGlowBrightness && !options.isMobile) {
      drawBarGlow(context, baseX + width / 2, barBottom,
        baseX + width / 2, barBottom - (barBottom - barTop) * progress,
        neonColor, strokeWidth, true);
    }
    drawTickBar(context, options.ticksValue, baseX + width / 2, barTop, barTop - barBottom, strokeWidth,
      true, options.colorTicks, options.tickWidth);
  }

}
