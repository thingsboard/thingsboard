///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import {
  ECharts,
  EChartsOption,
  EChartsSeriesItem, EChartsShape,
  EChartsTooltipTrigger,
  EChartsTooltipWidgetSettings,
  measureThresholdOffset,
  timeAxisBandWidthCalculator
} from '@home/components/widget/lib/chart/echarts-widget.models';
import {
  autoDateFormat,
  AutoDateFormatSettings,
  ComponentStyle,
  Font,
  textStyle,
  tsToFormatTimeUnit
} from '@shared/models/widget-settings.models';
import {
  LabelLayoutOptionCallback,
  VisualMapComponentOption,
  XAXisOption,
  YAXisOption
} from 'echarts/types/dist/shared';
import { CustomSeriesOption, LineSeriesOption } from 'echarts/charts';
import {
  formatValue,
  isDefinedAndNotNull,
  isFunction, isNumber,
  isNumeric,
  isUndefined,
  isUndefinedOrNull,
  mergeDeep,
  parseFunction
} from '@core/utils';
import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import tinycolor from 'tinycolor2';
import { ValueAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';
import { LabelFormatterCallback, LabelLayoutOption, SeriesLabelOption } from 'echarts/types/src/util/types';
import {
  BarRenderContext,
  BarRenderSharedContext,
  BarVisualSettings,
  renderTimeSeriesBar
} from '@home/components/widget/lib/chart/time-series-chart-bar.models';
import { DataKey } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { TbColorScheme } from '@shared/models/color.models';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';
import { DatePipe } from '@angular/common';
import { BuiltinTextPosition } from 'zrender/src/core/types';

export enum TimeSeriesChartType {
  default = 'default',
  line = 'line',
  bar = 'bar',
  point = 'point'
}

export const timeSeriesChartTypeTranslations = new Map<TimeSeriesChartType, string>(
  [
    [TimeSeriesChartType.line, 'widgets.time-series-chart.type-line'],
    [TimeSeriesChartType.bar, 'widgets.time-series-chart.type-bar'],
    [TimeSeriesChartType.point, 'widgets.time-series-chart.type-point']
  ]
);

export const timeSeriesChartColorScheme: TbColorScheme = {
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

export enum AxisPosition {
  left = 'left',
  right = 'right',
  top = 'top',
  bottom = 'bottom'
}

export const timeSeriesAxisPositions = Object.keys(AxisPosition) as AxisPosition[];

export const timeSeriesAxisPositionTranslations = new Map<AxisPosition, string>(
  [
    [AxisPosition.left, 'widgets.time-series-chart.axis.position-left'],
    [AxisPosition.right, 'widgets.time-series-chart.axis.position-right'],
    [AxisPosition.top, 'widgets.time-series-chart.axis.position-top'],
    [AxisPosition.bottom, 'widgets.time-series-chart.axis.position-bottom']
  ]
);

export enum TimeSeriesChartLineType {
  solid = 'solid',
  dashed = 'dashed',
  dotted = 'dotted'
}

export const timeSeriesLineTypes = Object.keys(TimeSeriesChartLineType) as TimeSeriesChartLineType[];

export const timeSeriesLineTypeTranslations = new Map<TimeSeriesChartLineType, string>(
  [
    [TimeSeriesChartLineType.solid, 'widgets.time-series-chart.line-type-solid'],
    [TimeSeriesChartLineType.dashed, 'widgets.time-series-chart.line-type-dashed'],
    [TimeSeriesChartLineType.dotted, 'widgets.time-series-chart.line-type-dotted']
  ]
);

export enum ThresholdLabelPosition {
  start = 'start',
  middle = 'middle',
  end = 'end',
  insideStart = 'insideStart',
  insideStartTop = 'insideStartTop',
  insideStartBottom = 'insideStartBottom',
  insideMiddle = 'insideMiddle',
  insideMiddleTop = 'insideMiddleTop',
  insideMiddleBottom = 'insideMiddleBottom',
  insideEnd = 'insideEnd',
  insideEndTop = 'insideEndTop',
  insideEndBottom = 'insideEndBottom'
}

export const timeSeriesThresholdLabelPositions = Object.keys(ThresholdLabelPosition) as ThresholdLabelPosition[];

export const timeSeriesThresholdLabelPositionTranslations = new Map<ThresholdLabelPosition, string>(
  [
    [ThresholdLabelPosition.start, 'widgets.time-series-chart.threshold.label-position-start'],
    [ThresholdLabelPosition.middle, 'widgets.time-series-chart.threshold.label-position-middle'],
    [ThresholdLabelPosition.end, 'widgets.time-series-chart.threshold.label-position-end'],
    [ThresholdLabelPosition.insideStart, 'widgets.time-series-chart.threshold.label-position-inside-start'],
    [ThresholdLabelPosition.insideStartTop, 'widgets.time-series-chart.threshold.label-position-inside-start-top'],
    [ThresholdLabelPosition.insideStartBottom, 'widgets.time-series-chart.threshold.label-position-inside-start-bottom'],
    [ThresholdLabelPosition.insideMiddle, 'widgets.time-series-chart.threshold.label-position-inside-middle'],
    [ThresholdLabelPosition.insideMiddleTop, 'widgets.time-series-chart.threshold.label-position-inside-middle-top'],
    [ThresholdLabelPosition.insideMiddleBottom, 'widgets.time-series-chart.threshold.label-position-inside-middle-bottom'],
    [ThresholdLabelPosition.insideEnd, 'widgets.time-series-chart.threshold.label-position-inside-end'],
    [ThresholdLabelPosition.insideEndTop, 'widgets.time-series-chart.threshold.label-position-inside-end-top'],
    [ThresholdLabelPosition.insideEndBottom, 'widgets.time-series-chart.threshold.label-position-inside-end-bottom']
  ]
);

export enum TimeSeriesChartThresholdType {
  constant = 'constant',
  latestKey = 'latestKey',
  entity = 'entity'
}

export const timeSeriesThresholdTypes = Object.keys(TimeSeriesChartThresholdType) as TimeSeriesChartThresholdType[];

export const timeSeriesThresholdTypeTranslations = new Map<TimeSeriesChartThresholdType, string>(
  [
    [TimeSeriesChartThresholdType.constant, 'widgets.time-series-chart.threshold.type-constant'],
    [TimeSeriesChartThresholdType.latestKey, 'widgets.time-series-chart.threshold.type-latest-key'],
    [TimeSeriesChartThresholdType.entity, 'widgets.time-series-chart.threshold.type-entity']
  ]
);

export enum SeriesFillType {
  none = 'none',
  opacity = 'opacity',
  gradient = 'gradient'
}

export const seriesFillTypes = Object.keys(SeriesFillType) as SeriesFillType[];

export const seriesFillTypeTranslations = new Map<SeriesFillType, string>(
  [
    [SeriesFillType.none, 'widgets.time-series-chart.series.fill-type-none'],
    [SeriesFillType.opacity, 'widgets.time-series-chart.series.fill-type-opacity'],
    [SeriesFillType.gradient, 'widgets.time-series-chart.series.fill-type-gradient']
  ]
);

export enum SeriesLabelPosition {
  top = 'top',
  bottom = 'bottom'
}

export const seriesLabelPositions = Object.keys(SeriesLabelPosition) as SeriesLabelPosition[];

export const seriesLabelPositionTranslations = new Map<SeriesLabelPosition, string>(
  [
    [SeriesLabelPosition.top, 'widgets.time-series-chart.series.label-position-top'],
    [SeriesLabelPosition.bottom, 'widgets.time-series-chart.series.label-position-bottom']
  ]
);

export enum LineSeriesStepType {
  start = 'start',
  middle = 'middle',
  end = 'end'
}

export const lineSeriesStepTypes = Object.keys(LineSeriesStepType) as LineSeriesStepType[];

export const lineSeriesStepTypeTranslations = new Map<LineSeriesStepType, string>(
  [
    [LineSeriesStepType.start, 'widgets.time-series-chart.series.line.step-type-start'],
    [LineSeriesStepType.middle, 'widgets.time-series-chart.series.line.step-type-middle'],
    [LineSeriesStepType.end, 'widgets.time-series-chart.series.line.step-type-end']
  ]
);

export enum TimeSeriesChartSeriesType {
  line = 'line',
  bar = 'bar'
}

export const timeSeriesChartSeriesTypes = Object.keys(TimeSeriesChartSeriesType) as TimeSeriesChartSeriesType[];

export const timeSeriesChartSeriesTypeTranslations = new Map<TimeSeriesChartSeriesType, string>(
  [
    [TimeSeriesChartSeriesType.line, 'widgets.time-series-chart.series.type-line'],
    [TimeSeriesChartSeriesType.bar, 'widgets.time-series-chart.series.type-bar']
  ]
);

export const timeSeriesChartSeriesTypeIcons = new Map<TimeSeriesChartSeriesType, string>(
  [
    [TimeSeriesChartSeriesType.line, 'mdi:chart-line'],
    [TimeSeriesChartSeriesType.bar, 'mdi:chart-bar']
  ]
);

export interface TimeSeriesChartAxisSettings {
  show: boolean;
  label?: string;
  labelFont?: Font;
  labelColor?: string;
  position: AxisPosition;
  showTickLabels: boolean;
  tickLabelFont: Font;
  tickLabelColor: string;
  showTicks: boolean;
  ticksColor: string;
  showLine: boolean;
  lineColor: string;
  showSplitLines: boolean;
  splitLinesColor: string;
}

export interface TimeSeriesChartXAxisSettings extends TimeSeriesChartAxisSettings {
  ticksFormat: AutoDateFormatSettings;
}

export const defaultXAxisTicksFormat: AutoDateFormatSettings = {
  millisecond: 'HH:mm:ss SSS',
  second: 'HH:mm:ss',
  minute: 'HH:mm',
  hour: 'HH:mm',
  day: 'MMM dd',
  month: 'MMM',
  year: 'yyyy'
};

export type TimeSeriesChartYAxisId = 'default' | string;

export type TimeSeriesChartTicksGenerator =
  (extent?: number[], interval?: number, niceTickExtent?: number[], intervalPrecision?: number) => {value: number}[];

export type TimeSeriesChartTicksFormatter =
  (value: any) => string;

export interface TimeSeriesChartYAxisSettings extends TimeSeriesChartAxisSettings {
  id?: TimeSeriesChartYAxisId;
  order?: number;
  units?: string;
  decimals?: number;
  interval?: number;
  splitNumber?: number;
  min?: number | string;
  max?: number | string;
  ticksGenerator?: TimeSeriesChartTicksGenerator | string;
  ticksFormatter?: TimeSeriesChartTicksFormatter | string;
}

export const timeSeriesChartYAxisValid = (axis: TimeSeriesChartYAxisSettings): boolean =>
  !(!axis.id || isUndefinedOrNull(axis.order));

export const timeSeriesChartYAxisValidator = (control: AbstractControl): ValidationErrors | null => {
  const axis: TimeSeriesChartYAxisSettings = control.value;
  if (!timeSeriesChartYAxisValid(axis)) {
    return {
      axis: true
    };
  }
  return null;
};

export const getNextTimeSeriesYAxisId = (axes: TimeSeriesChartYAxisSettings[]): TimeSeriesChartYAxisId => {
  let id = 0;
  for (const axis of axes) {
    const existingId = axis.id;
    if (existingId.startsWith('axis')) {
      const idSuffix = existingId.substring('axis'.length);
      if (isNumeric(idSuffix)) {
        id = Math.max(id, Number(idSuffix));
      }
    }
  }
  return 'axis' + (id+1);
};

export const defaultTimeSeriesChartYAxisSettings: TimeSeriesChartYAxisSettings = {
  units: null,
  decimals: 0,
  show: true,
    label: '',
    labelFont: {
    family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '600',
      lineHeight: '1'
  },
  labelColor: timeSeriesChartColorScheme['axis.label'].light,
    position: AxisPosition.left,
    showTickLabels: true,
    tickLabelFont: {
    family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '1'
  },
  tickLabelColor: timeSeriesChartColorScheme['axis.tickLabel'].light,
    ticksFormatter: null,
    showTicks: true,
    ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
    showLine: true,
    lineColor: timeSeriesChartColorScheme['axis.line'].light,
    showSplitLines: true,
    splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
};

export const defaultTimeSeriesChartXAxisSettings: TimeSeriesChartXAxisSettings = {
  show: true,
  label: '',
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '600',
    lineHeight: '1'
  },
  labelColor: timeSeriesChartColorScheme['axis.label'].light,
  position: AxisPosition.bottom,
  showTickLabels: true,
  tickLabelFont: {
    family: 'Roboto',
    size: 10,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  tickLabelColor: timeSeriesChartColorScheme['axis.tickLabel'].light,
  ticksFormat: {},
  showTicks: true,
  ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
  showLine: true,
  lineColor: timeSeriesChartColorScheme['axis.line'].light,
  showSplitLines: true,
  splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
};

export type TimeSeriesChartYAxes = {[id: TimeSeriesChartYAxisId]: TimeSeriesChartYAxisSettings};

export interface TimeSeriesChartThreshold {
  type: TimeSeriesChartThresholdType;
  value?: number;
  latestKey?: string;
  latestKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  entityAlias?: string;
  entityKey?: string;
  entityKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  yAxisId: TimeSeriesChartYAxisId;
  units?: string;
  decimals?: number;
  lineColor: string;
  lineType: TimeSeriesChartLineType | number | number[];
  lineWidth: number;
  startSymbol: EChartsShape;
  startSymbolSize: number;
  endSymbol: EChartsShape;
  endSymbolSize: number;
  showLabel: boolean;
  labelPosition: ThresholdLabelPosition;
  labelFont: Font;
  labelColor: string;
  enableLabelBackground: boolean;
  labelBackground: string;
  additionalLabelOption?: {[key: string]: any};
}

export const timeSeriesChartThresholdValid = (threshold: TimeSeriesChartThreshold): boolean => {
  if (!threshold.type || !threshold.yAxisId) {
    return false;
  }
  switch (threshold.type) {
    case TimeSeriesChartThresholdType.constant:
      if (isUndefinedOrNull(threshold.value)) {
        return false;
      }
      break;
    case TimeSeriesChartThresholdType.latestKey:
      if (!threshold.latestKey || !threshold.latestKeyType) {
        return false;
      }
      break;
    case TimeSeriesChartThresholdType.entity:
      if (!threshold.entityAlias || !threshold.entityKey || !threshold.entityKeyType) {
        return false;
      }
      break;
  }
  return true;
};

export const timeSeriesChartThresholdValidator = (control: AbstractControl): ValidationErrors | null => {
  const threshold: TimeSeriesChartThreshold = control.value;
  if (!timeSeriesChartThresholdValid(threshold)) {
    return {
      threshold: true
    };
  }
  return null;
};

export const timeSeriesChartThresholdDefaultSettings: TimeSeriesChartThreshold = {
  type: TimeSeriesChartThresholdType.constant,
  yAxisId: 'default',
  units: null,
  decimals: 0,
  lineColor: timeSeriesChartColorScheme['threshold.line'].light,
  lineType: TimeSeriesChartLineType.solid,
  lineWidth: 1,
  startSymbol: EChartsShape.none,
  startSymbolSize: 5,
  endSymbol: EChartsShape.arrow,
  endSymbolSize: 5,
  showLabel: true,
  labelPosition: ThresholdLabelPosition.end,
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  labelColor: timeSeriesChartColorScheme['threshold.label'].light,
  enableLabelBackground: false,
  labelBackground: 'rgba(255,255,255,0.56)'
};

export enum TimeSeriesChartNoAggregationBarWidthStrategy {
  group = 'group',
  separate = 'separate'
}

export const timeSeriesChartNoAggregationBarWidthStrategies =
  Object.keys(TimeSeriesChartNoAggregationBarWidthStrategy) as TimeSeriesChartNoAggregationBarWidthStrategy[];

export const timeSeriesChartNoAggregationBarWidthStrategyTranslations = new Map<TimeSeriesChartNoAggregationBarWidthStrategy, string>(
  [
    [TimeSeriesChartNoAggregationBarWidthStrategy.group, 'widgets.time-series-chart.no-aggregation-bar-width-strategy-group'],
    [TimeSeriesChartNoAggregationBarWidthStrategy.separate, 'widgets.time-series-chart.no-aggregation-bar-width-strategy-separate']
  ]
);

export interface TimeSeriesChartBarWidth {
  relative?: boolean;
  relativeWidth?: number;
  absoluteWidth?: number;
}

export interface TimeSeriesChartNoAggregationBarWidthSettings {
  strategy: TimeSeriesChartNoAggregationBarWidthStrategy;
  groupWidth?: TimeSeriesChartBarWidth;
  barWidth?: TimeSeriesChartBarWidth;
}

export const timeSeriesChartNoAggregationBarWidthDefaultSettings: TimeSeriesChartNoAggregationBarWidthSettings = {
  strategy: TimeSeriesChartNoAggregationBarWidthStrategy.group,
  groupWidth: {
    relative: true,
    relativeWidth: 2,
    absoluteWidth: 1000
  },
  barWidth: {
    relative: true,
    relativeWidth: 2,
    absoluteWidth: 1000
  }
};

export interface TimeSeriesChartBarWidthSettings {
  barGap: number;
  intervalGap: number;
}

export enum TimeSeriesChartAnimationEasing {
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

export const timeSeriesChartAnimationEasings = Object.keys(TimeSeriesChartAnimationEasing) as TimeSeriesChartAnimationEasing[];

export interface TimeSeriesChartAnimationSettings {
  animation: boolean;
  animationThreshold: number;
  animationDuration: number;
  animationEasing: TimeSeriesChartAnimationEasing;
  animationDelay: number;
  animationDurationUpdate: number;
  animationEasingUpdate: TimeSeriesChartAnimationEasing;
  animationDelayUpdate: number;
}

export const timeSeriesChartAnimationDefaultSettings: TimeSeriesChartAnimationSettings = {
  animation: true,
  animationThreshold: 2000,
  animationDuration: 500,
  animationEasing: TimeSeriesChartAnimationEasing.cubicOut,
  animationDelay: 0,
  animationDurationUpdate: 300,
  animationEasingUpdate: TimeSeriesChartAnimationEasing.cubicOut,
  animationDelayUpdate: 0
};

export interface TimeSeriesChartVisualMapPiece {
  lt?: number;
  gt?: number;
  lte?: number;
  gte?: number;
  value?: number;
  color?: string;
}

export const createTimeSeriesChartVisualMapPiece = (color: string, from?: number, to?: number): TimeSeriesChartVisualMapPiece => {
  const piece: TimeSeriesChartVisualMapPiece = {
    color
  };
  if (isNumber(from) && isNumber(to)) {
    if (from === to) {
      piece.value = from;
    } else {
      piece.gte = from;
      piece.lt = to;
    }
  } else if (isNumber(from)) {
    piece.gte = from;
  } else if (isNumber(to)) {
    piece.lt = to;
  }
  return piece;
};

export interface TimeSeriesChartVisualMapSettings {
  outOfRangeColor: string;
  pieces: TimeSeriesChartVisualMapPiece[];
}

export interface TimeSeriesChartSettings extends EChartsTooltipWidgetSettings {
  thresholds: TimeSeriesChartThreshold[];
  darkMode: boolean;
  dataZoom: boolean;
  stack: boolean;
  yAxes: TimeSeriesChartYAxes;
  xAxis: TimeSeriesChartXAxisSettings;
  animation: TimeSeriesChartAnimationSettings;
  barWidthSettings: TimeSeriesChartBarWidthSettings;
  noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings;
  visualMapSettings?: TimeSeriesChartVisualMapSettings;
}

export const timeSeriesChartDefaultSettings: TimeSeriesChartSettings = {
  thresholds: [],
  darkMode: false,
  dataZoom: true,
  stack: false,
  yAxes: {
    default: mergeDeep({} as TimeSeriesChartYAxisSettings,
                       defaultTimeSeriesChartYAxisSettings,
                       { id: 'default', order: 0 } as TimeSeriesChartYAxisSettings)
  },
  xAxis: mergeDeep({} as TimeSeriesChartXAxisSettings,
    defaultTimeSeriesChartXAxisSettings),
  animation: mergeDeep({} as TimeSeriesChartAnimationSettings,
    timeSeriesChartAnimationDefaultSettings),
  barWidthSettings: {
    barGap: 0.3,
    intervalGap: 0.6
  },
  noAggregationBarWidthSettings: mergeDeep({} as TimeSeriesChartNoAggregationBarWidthSettings,
    timeSeriesChartNoAggregationBarWidthDefaultSettings),
  showTooltip: true,
  tooltipTrigger: EChartsTooltipTrigger.axis,
  tooltipValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
  tooltipShowDate: true,
  tooltipDateFormat: autoDateFormat(),
  tooltipDateFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)',
  tooltipDateInterval: true,
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 4
};

export interface SeriesFillSettings {
  type: SeriesFillType;
  opacity: number;
  gradient: {
    start: number;
    end: number;
  };
}

export interface LineSeriesSettings {
  showLine: boolean;
  step: boolean;
  stepType: LineSeriesStepType;
  smooth: boolean;
  lineType: TimeSeriesChartLineType;
  lineWidth: number;
  showPoints: boolean;
  showPointLabel: boolean;
  pointLabelPosition: SeriesLabelPosition;
  pointLabelFont: Font;
  pointLabelColor: string;
  enablePointLabelBackground: boolean;
  pointLabelBackground: string;
  pointLabelFormatter?: string | LabelFormatterCallback;
  pointShape: EChartsShape;
  pointSize: number;
  fillAreaSettings: SeriesFillSettings;
}

export interface BarSeriesSettings {
  showBorder: boolean;
  borderWidth: number;
  borderRadius: number;
  showLabel: boolean;
  labelPosition: SeriesLabelPosition | BuiltinTextPosition;
  labelFont: Font;
  labelColor: string;
  enableLabelBackground: boolean;
  labelBackground: string;
  labelFormatter?: string | LabelFormatterCallback;
  labelLayout?: LabelLayoutOption | LabelLayoutOptionCallback;
  additionalLabelOption?: {[key: string]: any};
  backgroundSettings: SeriesFillSettings;
}

export interface TimeSeriesChartKeySettings {
  yAxisId: TimeSeriesChartYAxisId;
  showInLegend: boolean;
  dataHiddenByDefault: boolean;
  type: TimeSeriesChartSeriesType;
  lineSettings: LineSeriesSettings;
  barSettings: BarSeriesSettings;
}

export const timeSeriesChartKeyDefaultSettings: TimeSeriesChartKeySettings = {
  yAxisId: 'default',
  showInLegend: true,
  dataHiddenByDefault: false,
  type: TimeSeriesChartSeriesType.line,
  lineSettings: {
    showLine: true,
    step: false,
    stepType: LineSeriesStepType.start,
    smooth: false,
    lineType: TimeSeriesChartLineType.solid,
    lineWidth: 2,
    showPoints: false,
    showPointLabel: false,
    pointLabelPosition: SeriesLabelPosition.top,
    pointLabelFont: {
      family: 'Roboto',
      size: 11,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '1'
    },
    pointLabelColor: timeSeriesChartColorScheme['series.label'].light,
    enablePointLabelBackground: false,
    pointLabelBackground: 'rgba(255,255,255,0.56)',
    pointShape: EChartsShape.emptyCircle,
    pointSize: 4,
    fillAreaSettings: {
      type: SeriesFillType.none,
      opacity: 0.4,
      gradient: {
        start: 100,
        end: 0
      }
    }
  },
  barSettings: {
    showBorder: false,
    borderWidth: 2,
    borderRadius: 0,
    showLabel: false,
    labelPosition: SeriesLabelPosition.top,
    labelFont: {
      family: 'Roboto',
      size: 11,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '1'
    },
    labelColor: timeSeriesChartColorScheme['series.label'].light,
    enableLabelBackground: false,
    labelBackground: 'rgba(255,255,255,0.56)',
    backgroundSettings: {
      type: SeriesFillType.none,
      opacity: 0.4,
      gradient: {
        start: 100,
        end: 0
      }
    }
  }
};

export interface TimeSeriesChartDataItem extends EChartsSeriesItem {
  yAxisId: TimeSeriesChartYAxisId;
  yAxisIndex: number;
  option?: LineSeriesOption | CustomSeriesOption;
  barRenderContext?: BarRenderContext;
}

type TimeSeriesChartThresholdValue = number | string | (number | string)[];

export interface TimeSeriesChartThresholdItem {
  id: string;
  yAxisId: TimeSeriesChartYAxisId;
  yAxisIndex: number;
  latestDataKey?: DataKey;
  units?: string;
  decimals?: number;
  value: TimeSeriesChartThresholdValue;
  settings: TimeSeriesChartThreshold;
  option?: LineSeriesOption;
}

export interface TimeSeriesChartYAxis {
  id: string;
  decimals: number;
  settings: TimeSeriesChartYAxisSettings;
  option: YAXisOption & ValueAxisBaseOption;
}

export const createTimeSeriesYAxis = (units: string,
                                      decimals: number,
                                      settings: TimeSeriesChartYAxisSettings,
                                      darkMode: boolean): TimeSeriesChartYAxis => {
  const yAxisTickLabelStyle = createChartTextStyle(settings.tickLabelFont,
    settings.tickLabelColor, darkMode, 'axis.tickLabel');
  const yAxisNameStyle = createChartTextStyle(settings.labelFont,
    settings.labelColor, darkMode, 'axis.label');

  let ticksFormatter: TimeSeriesChartTicksFormatter;
  if (settings.ticksFormatter) {
    if (isFunction(settings.ticksFormatter)) {
      ticksFormatter = settings.ticksFormatter as TimeSeriesChartTicksFormatter;
    } else if (settings.ticksFormatter.length) {
      ticksFormatter = parseFunction(settings.ticksFormatter, ['value']);
    }
  }
  let ticksGenerator: TimeSeriesChartTicksGenerator;
  let minInterval: number;
  let interval: number;
  let splitNumber: number;
  if (settings.ticksGenerator) {
    if (isFunction(settings.ticksGenerator)) {
      ticksGenerator = settings.ticksGenerator as TimeSeriesChartTicksGenerator;
    } else if (settings.ticksGenerator.length) {
      ticksGenerator = parseFunction(settings.ticksGenerator, ['extent', 'interval', 'niceTickExtent', 'intervalPrecision']);
    }
  }
  if (!ticksGenerator) {
    interval = settings.interval;
    if (isUndefinedOrNull(interval)) {
      if (isDefinedAndNotNull(settings.splitNumber)) {
        splitNumber = settings.splitNumber;
      } else {
        minInterval = (1 / Math.pow(10, decimals));
      }
    }
  }
  return {
    id: settings.id,
    decimals,
    settings,
    option: {
      show: settings.show,
      type: 'value',
      position: settings.position,
      id: settings.id,
      offset: 0,
      alignTicks: true,
      scale: true,
      min: settings.min,
      max: settings.max,
      minInterval,
      splitNumber,
      interval,
      ticksGenerator,
      name: settings.label,
      nameLocation: 'middle',
      nameRotate: settings.position === AxisPosition.left ? 90 : -90,
      nameTextStyle: {
        color: yAxisNameStyle.color,
        fontStyle: yAxisNameStyle.fontStyle,
        fontWeight: yAxisNameStyle.fontWeight,
        fontFamily: yAxisNameStyle.fontFamily,
        fontSize: yAxisNameStyle.fontSize
      },
      axisLine: {
        show: settings.showLine,
        onZero: false,
        lineStyle: {
          color: prepareChartThemeColor(settings.lineColor, darkMode, 'axis.line')
        }
      },
      axisTick: {
        show: settings.showTicks,
        lineStyle: {
          color: prepareChartThemeColor(settings.ticksColor, darkMode, 'axis.ticks')
        }
      },
      axisLabel: {
        show: settings.showTickLabels,
        color: yAxisTickLabelStyle.color,
        fontStyle: yAxisTickLabelStyle.fontStyle,
        fontWeight: yAxisTickLabelStyle.fontWeight,
        fontFamily: yAxisTickLabelStyle.fontFamily,
        fontSize: yAxisTickLabelStyle.fontSize,
        formatter: (value: any) => {
          let result: string;
          if (ticksFormatter) {
            try {
              result = ticksFormatter(value);
            } catch (_e) {
            }
          }
          if (isUndefined(result)) {
            result = formatValue(value, decimals, units, false);
          }
          return result;
        }
      },
      splitLine: {
        show: settings.showSplitLines,
        lineStyle: {
          color: prepareChartThemeColor(settings.splitLinesColor, darkMode, 'axis.splitLine')
        }
      }
    }
  };
};

export const createTimeSeriesXAxisOption = (settings: TimeSeriesChartXAxisSettings,
                                            min: number, max: number,
                                            datePipe: DatePipe,
                                            darkMode: boolean): XAXisOption => {
  const xAxisTickLabelStyle = createChartTextStyle(settings.tickLabelFont,
    settings.tickLabelColor, darkMode, 'axis.tickLabel');
  const xAxisNameStyle = createChartTextStyle(settings.labelFont,
    settings.labelColor, darkMode, 'axis.label');
  const ticksFormat = mergeDeep({}, defaultXAxisTicksFormat, settings.ticksFormat);
  return {
    show: settings.show,
    type: 'time',
    scale: true,
    position: settings.position,
    name: settings.label,
    nameLocation: 'middle',
    nameTextStyle: {
      color: xAxisNameStyle.color,
      fontStyle: xAxisNameStyle.fontStyle,
      fontWeight: xAxisNameStyle.fontWeight,
      fontFamily: xAxisNameStyle.fontFamily,
      fontSize: xAxisNameStyle.fontSize
    },
    axisTick: {
      show: settings.showTicks,
      lineStyle: {
        color: prepareChartThemeColor(settings.ticksColor, darkMode, 'axis.ticks')
      }
    },
    axisLabel: {
      show: settings.showTickLabels,
      color: xAxisTickLabelStyle.color,
      fontStyle: xAxisTickLabelStyle.fontStyle,
      fontWeight: xAxisTickLabelStyle.fontWeight,
      fontFamily: xAxisTickLabelStyle.fontFamily,
      fontSize: xAxisTickLabelStyle.fontSize,
      hideOverlap: true,
      /** Min/Max time label always visible **/
      /* alignMinLabel: 'left',
      alignMaxLabel: 'right',
      showMinLabel: true,
      showMaxLabel: true, */
      formatter: (value: number, _index: number, extra: {level: number}) => {
        const unit = tsToFormatTimeUnit(value);
        const format = ticksFormat[unit];
        const formatted = datePipe.transform(value, format);
        if (extra.level > 0) {
          return `{primary|${formatted}}`;
        } else {
          return formatted;
        }
      }
    },
    axisLine: {
      show: settings.showLine,
      onZero: false,
      lineStyle: {
        color: prepareChartThemeColor(settings.lineColor, darkMode, 'axis.line')
      }
    },
    splitLine: {
      show: settings.showSplitLines,
      lineStyle: {
        color: prepareChartThemeColor(settings.splitLinesColor, darkMode, 'axis.splitLine')
      }
    },
    min,
    max,
    bandWidthCalculator: timeAxisBandWidthCalculator
  };
};

export const createTimeSeriesVisualMapOption = (settings: TimeSeriesChartVisualMapSettings,
                                                selectedRanges: {[key: number]: boolean}): VisualMapComponentOption => ({
  show: false,
  type: 'piecewise',
  selected: selectedRanges,
  dimension: 1,
  pieces: settings.pieces,
  outOfRange: {
  color: settings.outOfRangeColor
},
  inRange: !settings.pieces.length ? {
    color: settings.outOfRangeColor
  } : undefined
});

export const generateChartData = (dataItems: TimeSeriesChartDataItem[],
                                  thresholdItems: TimeSeriesChartThresholdItem[],
                                  stack: boolean,
                                  noAggregation: boolean,
                                  barRenderSharedContext: BarRenderSharedContext,
                                  darkMode: boolean): Array<LineSeriesOption | CustomSeriesOption> => {
  let series = generateChartSeries(dataItems,
    stack, noAggregation, barRenderSharedContext, darkMode);
  if (thresholdItems.length) {
    const thresholds = generateChartThresholds(thresholdItems);
    series = series.concat(thresholds);
  }
  return series;
};

export const calculateThresholdsOffset = (chart: ECharts,
                                          thresholdItems: TimeSeriesChartThresholdItem[],
                                          yAxisList: TimeSeriesChartYAxis[]): [number, number] => {
  const result: [number, number] = [0, 0];
  for (const item of thresholdItems) {
    const yAxis = yAxisList[item.yAxisIndex];
    const offset = measureThresholdOffset(chart, yAxis.id, item.id, item.value);
    result[0] = Math.max(result[0], offset[0]);
    result[1] = Math.max(result[1], offset[1]);
  }
  return result;
};

export const parseThresholdData = (value: any): TimeSeriesChartThresholdValue => {
  let thresholdValue: TimeSeriesChartThresholdValue;
  if (Array.isArray(value)) {
    thresholdValue = value;
  } else {
    try {
      const parsedData = JSON.parse(value);
      thresholdValue = Array.isArray(parsedData) ? parsedData : [parsedData];
    } catch (e) {
      thresholdValue = [value];
    }
  }
  return thresholdValue;
};

const generateChartThresholds = (thresholdItems: TimeSeriesChartThresholdItem[]): Array<LineSeriesOption> => {
  const series: Array<LineSeriesOption> = [];
  for (const item of thresholdItems) {
    if (isDefinedAndNotNull(item.value)) {
      let seriesOption = item.option;
      if (!item.option) {
        const thresholdLabelStyle = createChartTextStyle(item.settings.labelFont,
          item.settings.labelColor, false, 'threshold.label');
        seriesOption = {
          type: 'line',
          id: item.id,
          dataGroupId: item.id,
          yAxisIndex: item.yAxisIndex,
          data: [],
          markLine: {
            tooltip: {
              show: false
            },
            lineStyle: {
              width: item.settings.lineWidth,
              color: prepareChartThemeColor(item.settings.lineColor, false, 'threshold.line'),
              type: item.settings.lineType
            },
            label: {
              show: item.settings.showLabel,
              position: item.settings.labelPosition,
              color: thresholdLabelStyle.color,
              fontStyle: thresholdLabelStyle.fontStyle,
              fontWeight: thresholdLabelStyle.fontWeight,
              fontFamily: thresholdLabelStyle.fontFamily,
              fontSize: thresholdLabelStyle.fontSize,
              formatter: params => formatValue(params.value, item.decimals,
                item.units, false)
            },
            emphasis: {
              disabled: true
            }
          }
        };
        if (item.settings.enableLabelBackground) {
          seriesOption.markLine.label.backgroundColor = item.settings.labelBackground;
          seriesOption.markLine.label.padding = [4, 5];
          seriesOption.markLine.label.borderRadius = 4;
        }
        if (item.settings.additionalLabelOption) {
          seriesOption.markLine.label = {...seriesOption.markLine.label, ...item.settings.additionalLabelOption};
        }
        item.option = seriesOption;
      }
      seriesOption.markLine.data = [];
      if (Array.isArray(item.value)) {
        for (const val of item.value) {
          seriesOption.markLine.data.push(createThresholdData(val, item));
        }
      } else {
        seriesOption.markLine.data.push(createThresholdData(item.value, item));
      }
      series.push(seriesOption);
    }
  }
  return series;
};

const createThresholdData = (val: string | number, item: TimeSeriesChartThresholdItem): MarkLine2DDataItemOption => [
    {
      xAxis: 'min',
      yAxis: val,
      value: val,
      symbol: item.settings.startSymbol,
      symbolSize: item.settings.startSymbolSize
    },
    {
      xAxis: 'max',
      yAxis: val,
      value: val,
      symbol: item.settings.endSymbol,
      symbolSize: item.settings.endSymbolSize
    }
  ];

const generateChartSeries = (dataItems: TimeSeriesChartDataItem[],
                             stack: boolean,
                             noAggregation: boolean,
                             barRenderSharedContext: BarRenderSharedContext,
                             darkMode: boolean): Array<LineSeriesOption | CustomSeriesOption> => {
  const series: Array<LineSeriesOption | CustomSeriesOption> = [];
  const enabledDataItems = dataItems.filter(d => d.enabled);
  const barDataItems = enabledDataItems.filter(d =>
    d.dataKey.settings.type === TimeSeriesChartSeriesType.bar && d.data.length);
  let barsCount = barDataItems.length;
  const barGroups: number[] = [];
  if (stack) {
    barDataItems.forEach(item => {
      if (barGroups.indexOf(item.yAxisIndex) === -1) {
        barGroups.push(item.yAxisIndex);
      }
    });
    barsCount = barGroups.length;
  }
  for (const item of enabledDataItems) {
    if (item.dataKey.settings.type === TimeSeriesChartSeriesType.bar) {
      if (!item.barRenderContext) {
        item.barRenderContext = {noAggregation,
          shared: barRenderSharedContext};
      }
      item.barRenderContext.noAggregation = noAggregation;
      item.barRenderContext.barsCount = barsCount;
      item.barRenderContext.barIndex = stack ? barGroups.indexOf(item.yAxisIndex) : barDataItems.indexOf(item);
      if (stack) {
        const stackItems = enabledDataItems.filter(d => d.yAxisIndex === item.yAxisIndex);
        item.barRenderContext.currentStackItems = stackItems;
        item.barRenderContext.barStackIndex = stackItems.indexOf(item);
      }
    }
    const seriesOption = createTimeSeriesChartSeries(item, stack, darkMode);
    series.push(seriesOption);
  }
  return series;
};

export const updateDarkMode = (options: EChartsOption, settings: TimeSeriesChartSettings,
                               yAxisList: TimeSeriesChartYAxis[],
                               dataItems: TimeSeriesChartDataItem[],
                               darkMode: boolean): EChartsOption => {
  options.darkMode = darkMode;
  if (Array.isArray(options.yAxis)) {
    for (let i = 0; i < options.yAxis.length; i++) {
      const yAxis = options.yAxis[i];
      const yAxisSettings = yAxisList[i].settings;
      yAxis.nameTextStyle.color = prepareChartThemeColor(yAxisSettings.labelColor, darkMode, 'axis.label');
      yAxis.axisLabel.color = prepareChartThemeColor(yAxisSettings.tickLabelColor, darkMode, 'axis.tickLabel');
      yAxis.axisLine.lineStyle.color = prepareChartThemeColor(yAxisSettings.lineColor, darkMode, 'axis.line');
      yAxis.axisTick.lineStyle.color = prepareChartThemeColor(yAxisSettings.ticksColor, darkMode, 'axis.ticks');
      yAxis.splitLine.lineStyle.color = prepareChartThemeColor(yAxisSettings.splitLinesColor, darkMode, 'axis.splitLine');
    }
  }
  if (Array.isArray(options.xAxis)) {
    for (const xAxis of options.xAxis) {
      xAxis.nameTextStyle.color = prepareChartThemeColor(settings.xAxis.labelColor, darkMode, 'axis.label');
      xAxis.axisLabel.color = prepareChartThemeColor(settings.xAxis.tickLabelColor, darkMode, 'axis.tickLabel');
      xAxis.axisLine.lineStyle.color = prepareChartThemeColor(settings.xAxis.lineColor, darkMode, 'axis.line');
      xAxis.axisTick.lineStyle.color = prepareChartThemeColor(settings.xAxis.ticksColor, darkMode, 'axis.ticks');
      xAxis.splitLine.lineStyle.color = prepareChartThemeColor(settings.xAxis.splitLinesColor, darkMode, 'axis.splitLine');
    }
  }
  for (const item of dataItems) {
    if (item.dataKey.settings.type === TimeSeriesChartSeriesType.line) {
      const lineSettings = item.dataKey.settings as LineSeriesSettings;
      if (item.option.label?.show) {
        item.option.label.rich.value.color = prepareChartThemeColor(lineSettings.pointLabelColor, darkMode, 'series.label');
      }
      if (Array.isArray(options.series)) {
        const series = options.series.find(s => s.id === item.id);
        if (series) {
          if (series.label?.show) {
            series.label.rich.value.color = prepareChartThemeColor(lineSettings.pointLabelColor, darkMode, 'series.label');
          }
        }
      }
    } else {
      if (item.barRenderContext?.labelOption?.show) {
        const barSettings = item.dataKey.settings as BarSeriesSettings;
        (item.barRenderContext.labelOption.rich.value as any).fill = prepareChartThemeColor(barSettings.labelColor,
          darkMode, 'series.label');
      }
    }
  }
  return options;
};

const createTimeSeriesChartSeries = (item: TimeSeriesChartDataItem,
                                     stack: boolean,
                                     darkMode: boolean): LineSeriesOption | CustomSeriesOption => {
  let seriesOption = item.option;
  if (!seriesOption) {
    const dataKey = item.dataKey;
    const settings: TimeSeriesChartKeySettings = dataKey.settings;
    const seriesColor = item.dataKey.color;
    seriesOption = {
      id: item.id,
      dataGroupId: item.id,
      yAxisIndex: item.yAxisIndex,
      name: item.dataKey.label,
      color: seriesColor,
      stack: stack ? (item.yAxisIndex + '') : undefined,
      stackStrategy: 'all',
      emphasis: {
        focus: 'series'
      },
      dimensions: [
        {name: 'x', type: 'time', stack},
        {name: 'y', type: 'float'},
        {name: 'intervalStart', type: 'time'},
        {name: 'intervalEnd', type: 'time'}
      ],
      encode: {
        x: [0, 2, 3],
        y: [1]
      }
    };
    item.option = seriesOption;
    if (settings.type === TimeSeriesChartSeriesType.line) {
      const lineSettings = settings.lineSettings;
      const lineSeriesOption = seriesOption as LineSeriesOption;
      lineSeriesOption.type = 'line';
      lineSeriesOption.label = createSeriesLabelOption(item, lineSettings.showPointLabel,
        lineSettings.pointLabelFont, lineSettings.pointLabelColor,
        lineSettings.enablePointLabelBackground, lineSettings.pointLabelBackground,
        lineSettings.pointLabelPosition,
        lineSettings.pointLabelFormatter, false, darkMode);
      lineSeriesOption.step = lineSettings.step ? lineSettings.stepType : false;
      lineSeriesOption.smooth = lineSettings.smooth;
      if (lineSettings.smooth) {
        lineSeriesOption.smoothMonotone = 'x';
      }
      lineSeriesOption.lineStyle = {
        width: lineSettings.showLine ? lineSettings.lineWidth : 0,
        type: lineSettings.lineType
      };
      if (lineSettings.fillAreaSettings.type !== SeriesFillType.none) {
        lineSeriesOption.areaStyle = {};
        if (lineSettings.fillAreaSettings.type === SeriesFillType.opacity) {
          lineSeriesOption.areaStyle.opacity = lineSettings.fillAreaSettings.opacity;
        } else if (lineSettings.fillAreaSettings.type === SeriesFillType.gradient) {
          lineSeriesOption.areaStyle.opacity = 1;
          lineSeriesOption.areaStyle.color = createLinearOpacityGradient(seriesColor, lineSettings.fillAreaSettings.gradient);
        }
      }
      lineSeriesOption.showSymbol = lineSettings.showPoints;
      lineSeriesOption.symbol = lineSettings.pointShape;
      lineSeriesOption.symbolSize = lineSettings.pointSize;
    } else {
      const barSettings = settings.barSettings;
      const barSeriesOption = (seriesOption as any) as CustomSeriesOption;
      barSeriesOption.type = 'custom';
      const barVisualSettings: BarVisualSettings = {
        color: seriesColor,
        borderColor: seriesColor,
        borderWidth: barSettings.showBorder ? barSettings.borderWidth : 0,
        borderRadius: barSettings.borderRadius
      };
      if (barSettings.backgroundSettings.type === SeriesFillType.none) {
        barVisualSettings.color = seriesColor;
      } else if (barSettings.backgroundSettings.type === SeriesFillType.opacity) {
        barVisualSettings.color = tinycolor(seriesColor).setAlpha(barSettings.backgroundSettings.opacity).toRgbString();
      } else {
        barVisualSettings.color = createLinearOpacityGradient(seriesColor, barSettings.backgroundSettings.gradient);
      }
      item.barRenderContext.visualSettings = barVisualSettings;
      item.barRenderContext.labelOption = createSeriesLabelOption(item, barSettings.showLabel,
        barSettings.labelFont, barSettings.labelColor, barSettings.enableLabelBackground, barSettings.labelBackground,
        barSettings.labelPosition, barSettings.labelFormatter, true, darkMode);
      item.barRenderContext.additionalLabelOption = barSettings.additionalLabelOption;
      barSeriesOption.renderItem = (params, api) =>
        renderTimeSeriesBar(params, api, item.barRenderContext);
      barSeriesOption.labelLayout = barSettings.labelLayout;
    }
  }
  seriesOption.data = item.data;
  return seriesOption;
};

const createSeriesLabelOption = (item: TimeSeriesChartDataItem, show: boolean,
                                 labelFont: Font, labelColor: string,
                                 enableBackground: boolean, labelBackground: string,
                                 position: SeriesLabelPosition | BuiltinTextPosition,
                                 labelFormatter: string | LabelFormatterCallback,
                                 labelColorFill: boolean,
                                 darkMode: boolean): SeriesLabelOption => {
  let labelStyle: ComponentStyle = {};
  if (show) {
    labelStyle = createChartTextStyle(labelFont, labelColor, darkMode, 'series.label', labelColorFill);
  }
  let formatter: LabelFormatterCallback;
  if (isFunction(labelFormatter)) {
    formatter = labelFormatter as LabelFormatterCallback;
  } else if (labelFormatter?.length) {
    const formatFunction = parseFunction(labelFormatter, ['value']);
    formatter = (params): string => {
      let result: string;
      try {
        result = formatFunction(params.value[1]);
      } catch (_e) {
      }
      if (isUndefined(result)) {
        result = formatValue(params.value[1], item.decimals, item.units, false);
      }
      return `{value|${result}}`;
    };
  } else {
    formatter = (params): string => {
      const value = formatValue(params.value[1], item.decimals, item.units, false);
      return `{value|${value}}`;
    };
  }
  const labelOption: SeriesLabelOption = {
    show,
    position,
    formatter,
    rich: {
      value: labelStyle
    }
  };
  if (enableBackground) {
    labelOption.backgroundColor = labelBackground;
    labelOption.padding = [4, 5];
    labelOption.borderRadius = 4;
  }
  return labelOption;
};

const createChartTextStyle = (font: Font, color: string, darkMode: boolean, colorKey?: string, fill = false): ComponentStyle => {
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

const createLinearOpacityGradient = (color: string, gradient: {start: number; end: number}): LinearGradientObject => ({
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

const prepareChartThemeColor = (color: string, darkMode: boolean, colorKey?: string): string => {
  if (darkMode) {
    let colorInstance = tinycolor(color);
    if (colorInstance.isDark()) {
      if (colorKey && timeSeriesChartColorScheme[colorKey]) {
        return timeSeriesChartColorScheme[colorKey].dark;
      } else {
        const rgb = colorInstance.toRgb();
        colorInstance = tinycolor({r: 255 - rgb.r, g: 255 - rgb.g, b: 255 - rgb.b, a: rgb.a});
        return colorInstance.toRgbString();
      }
    }
  }
  return color;
};
