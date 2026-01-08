///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { isNumber } from '@core/utils';
import { TbColorScheme } from '@shared/models/color.models';
import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import tinycolor from 'tinycolor2';
import { ComponentStyle, Font, textStyle } from '@shared/models/widget-settings.models';
import { LabelFormatterCallback, RadialGradientObject } from 'echarts';
import { AnimationOptionMixin, LabelLayoutOption } from 'echarts/types/src/util/types';
import { LabelLayoutOptionCallback } from 'echarts/types/dist/shared';
import { BuiltinTextPosition } from 'zrender/src/core/types';
import { WidgetContext } from '@home/models/widget-component.models';

export const chartColorScheme: TbColorScheme = {
  'threshold.line': {
    light: 'rgba(0, 0, 0, 0.76)',
    dark: '#eee'
  },
  'threshold.label': {
    light: 'rgba(0, 0, 0, 0.76)',
    dark: '#eee'
  },
  'axis.line': {
    light: 'rgba(0, 0, 0, 0.54)',
    dark: '#B9B8CE'
  },
  'axis.label': {
    light: 'rgba(0, 0, 0, 0.54)',
    dark: '#B9B8CE'
  },
  'axis.ticks': {
    light: 'rgba(0, 0, 0, 0.54)',
    dark: '#B9B8CE'
  },
  'axis.tickLabel': {
    light: 'rgba(0, 0, 0, 0.54)',
    dark: '#B9B8CE'
  },
  'axis.splitLine': {
    light: 'rgba(0, 0, 0, 0.12)',
    dark: '#484753'
  },
  'series.label': {
    light: 'rgba(0, 0, 0, 0.76)',
    dark: '#eee'
  }
};

export enum ChartShape {
  emptyCircle = 'emptyCircle',
  circle = 'circle',
  rect = 'rect',
  roundRect = 'roundRect',
  triangle = 'triangle',
  diamond = 'diamond',
  pin = 'pin',
  arrow = 'arrow',
  none = 'none'
}

export const chartShapes = Object.keys(ChartShape) as ChartShape[];

export const chartShapeTranslations = new Map<ChartShape, string>(
  [
    [ChartShape.emptyCircle, 'widgets.chart.shape-empty-circle'],
    [ChartShape.circle, 'widgets.chart.shape-circle'],
    [ChartShape.rect, 'widgets.chart.shape-rect'],
    [ChartShape.roundRect, 'widgets.chart.shape-round-rect'],
    [ChartShape.triangle, 'widgets.chart.shape-triangle'],
    [ChartShape.diamond, 'widgets.chart.shape-diamond'],
    [ChartShape.pin, 'widgets.chart.shape-pin'],
    [ChartShape.arrow, 'widgets.chart.shape-arrow'],
    [ChartShape.none, 'widgets.chart.shape-none']
  ]
);

export enum ChartLineType {
  solid = 'solid',
  dashed = 'dashed',
  dotted = 'dotted'
}

export const chartLineTypes = Object.keys(ChartLineType) as ChartLineType[];

export const chartLineTypeTranslations = new Map<ChartLineType, string>(
  [
    [ChartLineType.solid, 'widgets.chart.line-type-solid'],
    [ChartLineType.dashed, 'widgets.chart.line-type-dashed'],
    [ChartLineType.dotted, 'widgets.chart.line-type-dotted']
  ]
);

export enum ChartAnimationEasing {
  linear = 'linear',
  quadraticIn = 'quadraticIn',
  quadraticOut = 'quadraticOut',
  quadraticInOut = 'quadraticInOut',
  cubicIn = 'cubicIn',
  cubicOut = 'cubicOut',
  cubicInOut = 'cubicInOut',
  quarticIn = 'quarticIn',
  quarticOut = 'quarticOut',
  quarticInOut = 'quarticInOut',
  quinticIn = 'quinticIn',
  quinticOut = 'quinticOut',
  quinticInOut = 'quinticInOut',
  sinusoidalIn = 'sinusoidalIn',
  sinusoidalOut = 'sinusoidalOut',
  sinusoidalInOut = 'sinusoidalInOut',
  exponentialIn = 'exponentialIn',
  exponentialOut = 'exponentialOut',
  exponentialInOut = 'exponentialInOut',
  circularIn = 'circularIn',
  circularOut = 'circularOut',
  circularInOut = 'circularInOut',
  elasticIn = 'elasticIn',
  elasticOut = 'elasticOut',
  elasticInOut = 'elasticInOut',
  backIn = 'backIn',
  backOut = 'backOut',
  backInOut = 'backInOut',
  bounceIn = 'bounceIn',
  bounceOut = 'bounceOut',
  bounceInOut = 'bounceInOut'
}

export const chartAnimationEasings = Object.keys(ChartAnimationEasing) as ChartAnimationEasing[];

export enum ChartFillType {
  none = 'none',
  opacity = 'opacity',
  gradient = 'gradient'
}

export const chartFillTypes = Object.keys(ChartFillType) as ChartFillType[];

export const chartFillTypeTranslations = new Map<ChartFillType, string>(
  [
    [ChartFillType.none, 'widgets.chart.fill-type-none'],
    [ChartFillType.opacity, 'widgets.chart.fill-type-opacity'],
    [ChartFillType.gradient, 'widgets.chart.fill-type-gradient']
  ]
);

export enum ChartLabelPosition {
  top = 'top',
  bottom = 'bottom'
}

export const chartLabelPositions = Object.keys(ChartLabelPosition) as ChartLabelPosition[];

export const chartLabelPositionTranslations = new Map<ChartLabelPosition, string>(
  [
    [ChartLabelPosition.top, 'widgets.chart.label-position-top'],
    [ChartLabelPosition.bottom, 'widgets.chart.label-position-bottom']
  ]
);

export enum PieChartLabelPosition {
  inside = 'inside',
  outside = 'outside'
}

export const pieChartLabelPositions = Object.keys(PieChartLabelPosition) as PieChartLabelPosition[];

export const pieChartLabelPositionTranslations = new Map<PieChartLabelPosition, string>(
  [
    [PieChartLabelPosition.inside, 'widgets.chart.label-position-inside'],
    [PieChartLabelPosition.outside, 'widgets.chart.label-position-outside']
  ]
);

export interface ChartAnimationSettings {
  animation: boolean;
  animationThreshold: number;
  animationDuration: number;
  animationEasing: ChartAnimationEasing;
  animationDelay: number;
  animationDurationUpdate: number;
  animationEasingUpdate: ChartAnimationEasing;
  animationDelayUpdate: number;
}

export const chartAnimationDefaultSettings: ChartAnimationSettings = {
  animation: true,
  animationThreshold: 2000,
  animationDuration: 500,
  animationEasing: ChartAnimationEasing.cubicOut,
  animationDelay: 0,
  animationDurationUpdate: 300,
  animationEasingUpdate: ChartAnimationEasing.cubicOut,
  animationDelayUpdate: 0
};

export interface ChartFillSettings {
  type: ChartFillType;
  opacity: number;
  gradient: {
    start: number;
    end: number;
  };
}

export interface ChartBarSettings {
  showBorder: boolean;
  borderWidth: number;
  borderRadius: number;
  barWidth?: number;
  showLabel: boolean;
  labelPosition: ChartLabelPosition | PieChartLabelPosition | BuiltinTextPosition;
  labelFont: Font;
  labelColor: string;
  enableLabelBackground: boolean;
  labelBackground: string;
  labelFormatter?: string | LabelFormatterCallback;
  labelLayout?: LabelLayoutOption | LabelLayoutOptionCallback;
  additionalLabelOption?: {[key: string]: any};
  backgroundSettings: ChartFillSettings;
}

export const chartBarDefaultSettings: ChartBarSettings = {
  showBorder: false,
  borderWidth: 2,
  borderRadius: 0,
  showLabel: false,
  labelPosition: ChartLabelPosition.top,
  labelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  labelColor: chartColorScheme['series.label'].light,
  enableLabelBackground: false,
  labelBackground: 'rgba(255,255,255,0.56)',
  backgroundSettings: {
    type: ChartFillType.none,
    opacity: 0.4,
    gradient: {
      start: 100,
      end: 0
    }
  }
};

type ChartShapeOffsetFunction = (size: number) => number;

const chartShapeOffsetFunctions = new Map<ChartShape, ChartShapeOffsetFunction>(
  [
    [ChartShape.emptyCircle, size => size / 2 + 1],
    [ChartShape.circle, size => size / 2],
    [ChartShape.rect, size => size / 2],
    [ChartShape.roundRect, size => size / 2],
    [ChartShape.triangle, size => size / 2],
    [ChartShape.diamond, size => size / 2],
    [ChartShape.pin, size => size],
    [ChartShape.arrow, () => 0],
    [ChartShape.none, () => 0],
  ]
);

export const measureSymbolOffset = (symbol: string, symbolSize: any): number => {
  if (isNumber(symbolSize)) {
    if (symbol) {
      const offsetFunction = chartShapeOffsetFunctions.get(symbol as ChartShape);
      if (offsetFunction) {
        return offsetFunction(symbolSize);
      } else {
        return symbolSize / 2;
      }
    }
  } else {
    return 0;
  }
};

export const createLinearOpacityGradient = (color: string, gradient: {start: number; end: number}): LinearGradientObject => ({
  type: 'linear',
  x: 0,
  y: 0,
  x2: 0,
  y2: 1,
  colorStops: [{
    offset: 0, color: tinycolor(color).setAlpha(gradient.start / 100).toRgbString() // color at 0%
  }, {
    offset: 1, color: tinycolor(color).setAlpha(gradient.end / 100).toRgbString() // color at 100%
  }],
  global: false
});

export const createRadialOpacityGradient = (color: string, gradient: {start: number; end: number}): RadialGradientObject => ({
  type: 'radial',
  x: 0.5,
  y: 0.5,
  r: 0.5,
  colorStops: [{
    offset: 0, color: tinycolor(color).setAlpha(gradient.end / 100).toRgbString() // color at 0%
  }, {
    offset: 1, color: tinycolor(color).setAlpha(gradient.start / 100).toRgbString() // color at 100%
  }],
  global: false
});

export const createChartTextStyle = (font: Font, color: string, darkMode: boolean, colorKey?: string, fill = false): ComponentStyle => {
  const style = textStyle(font);
  delete style.lineHeight;
  style.fontSize = font.size;
  if (fill) {
    style.fill = prepareChartThemeColor(color, darkMode, colorKey);
  } else {
    style.color = prepareChartThemeColor(color, darkMode, colorKey);
  }
  return style;
};

export const prepareChartThemeColor = (color: string, darkMode: boolean, colorKey?: string): string => {
  if (darkMode) {
    let colorInstance = tinycolor(color);
    if (colorInstance.isDark()) {
      if (colorKey && chartColorScheme[colorKey]) {
        return chartColorScheme[colorKey].dark;
      } else {
        const rgb = colorInstance.toRgb();
        colorInstance = tinycolor({r: 255 - rgb.r, g: 255 - rgb.g, b: 255 - rgb.b, a: rgb.a});
        return colorInstance.toRgbString();
      }
    }
  }
  return color;
};

export const toAnimationOption = (ctx: WidgetContext, settings: ChartAnimationSettings): AnimationOptionMixin => ({
  animation: settings.animation,
  animationThreshold: settings.animationThreshold,
  animationDuration: settings.animationDuration,
  animationEasing: settings.animationEasing,
  animationDelay: settings.animationDelay,
  animationDurationUpdate: settings.animationDurationUpdate,
  animationEasingUpdate: settings.animationEasingUpdate,
  animationDelayUpdate: settings.animationDelayUpdate
});
