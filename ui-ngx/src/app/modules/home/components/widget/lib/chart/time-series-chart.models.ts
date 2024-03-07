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
  EChartsSeriesItem,
  EChartsTooltipTrigger,
  EChartsTooltipWidgetSettings,
  measureThresholdLabelOffset
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { ComponentStyle, Font, simpleDateFormat, textStyle } from '@shared/models/widget-settings.models';
import { XAXisOption, YAXisOption } from 'echarts/types/dist/shared';
import { CustomSeriesOption, LineSeriesOption } from 'echarts/charts';
import { formatValue, isDefinedAndNotNull, isUndefinedOrNull, parseFunction } from '@core/utils';
import { LinearGradientObject } from 'zrender/lib/graphic/LinearGradient';
import tinycolor from 'tinycolor2';
import Axis2D from 'echarts/types/src/coord/cartesian/Axis2D';
import { Interval } from '@shared/models/time/time.models';
import { ValueAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';
import { SeriesLabelOption } from 'echarts/types/src/util/types';
import {
  BarRenderContext,
  BarVisualSettings,
  renderTimeSeriesBar
} from '@home/components/widget/lib/chart/time-series-chart-bar.models';
import { DataKey } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { TbColorScheme } from '@shared/models/color.models';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';

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

const timeSeriesChartColorScheme: TbColorScheme = {
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

export enum TimeSeriesChartShape {
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

export const timeSeriesChartShapes = Object.keys(TimeSeriesChartShape) as TimeSeriesChartShape[];

export const timeSeriesChartShapeTranslations = new Map<TimeSeriesChartShape, string>(
  [
    [TimeSeriesChartShape.emptyCircle, 'widgets.time-series-chart.shape-empty-circle'],
    [TimeSeriesChartShape.circle, 'widgets.time-series-chart.shape-circle'],
    [TimeSeriesChartShape.rect, 'widgets.time-series-chart.shape-rect'],
    [TimeSeriesChartShape.roundRect, 'widgets.time-series-chart.shape-round-rect'],
    [TimeSeriesChartShape.triangle, 'widgets.time-series-chart.shape-triangle'],
    [TimeSeriesChartShape.diamond, 'widgets.time-series-chart.shape-diamond'],
    [TimeSeriesChartShape.pin, 'widgets.time-series-chart.shape-pin'],
    [TimeSeriesChartShape.arrow, 'widgets.time-series-chart.shape-arrow'],
    [TimeSeriesChartShape.none, 'widgets.time-series-chart.shape-none']
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

export interface TimeSeriesChartYAxisSettings extends TimeSeriesChartAxisSettings {
  min?: number | string;
  max?: number | string;
  intervalCalculator?: string;
}

export interface TimeSeriesChartThreshold {
  type: TimeSeriesChartThresholdType;
  value?: number;
  latestKey?: string;
  latestKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  entityAlias?: string;
  entityKey?: string;
  entityKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  units?: string;
  decimals?: number;
  lineColor: string;
  lineType: TimeSeriesChartLineType;
  lineWidth: number;
  startSymbol: TimeSeriesChartShape;
  startSymbolSize: number;
  endSymbol: TimeSeriesChartShape;
  endSymbolSize: number;
  showLabel: boolean;
  labelPosition: ThresholdLabelPosition;
  labelFont: Font;
  labelColor: string;
}

export const timeSeriesChartThresholdValid = (threshold: TimeSeriesChartThreshold): boolean => {
  if (!threshold.type) {
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
  units: '',
  decimals: 0,
  lineColor: timeSeriesChartColorScheme['threshold.line'].light,
  lineType: TimeSeriesChartLineType.solid,
  lineWidth: 1,
  startSymbol: TimeSeriesChartShape.none,
  startSymbolSize: 5,
  endSymbol: TimeSeriesChartShape.arrow,
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
  labelColor: timeSeriesChartColorScheme['threshold.label'].light
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

export interface TimeSeriesChartNoAggregationBarWidthSettings {
  strategy: TimeSeriesChartNoAggregationBarWidthStrategy;
  groupIntervalWidth?: number;
  separateBarWidth?: number;
}

export interface TimeSeriesChartSettings extends EChartsTooltipWidgetSettings {
  thresholds: TimeSeriesChartThreshold[];
  darkMode: boolean;
  dataZoom: boolean;
  stack: boolean;
  yAxis: TimeSeriesChartYAxisSettings;
  xAxis: TimeSeriesChartAxisSettings;
  noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings;
}

export const timeSeriesChartDefaultSettings: TimeSeriesChartSettings = {
  thresholds: [],
  darkMode: false,
  dataZoom: true,
  stack: false,
  yAxis: {
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
    showTicks: true,
    ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
    showLine: true,
    lineColor: timeSeriesChartColorScheme['axis.line'].light,
    showSplitLines: true,
    splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
  },
  xAxis: {
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
    showTicks: true,
    ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
    showLine: true,
    lineColor: timeSeriesChartColorScheme['axis.line'].light,
    showSplitLines: true,
    splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
  },
  noAggregationBarWidthSettings: {
    strategy: TimeSeriesChartNoAggregationBarWidthStrategy.group,
    groupIntervalWidth: 1000,
    separateBarWidth: 1000
  },
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
  tooltipDateFormat: simpleDateFormat('dd MMM yyyy HH:mm:ss'),
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
  pointShape: TimeSeriesChartShape;
  pointSize: number;
  fillAreaSettings: SeriesFillSettings;
}

export interface BarSeriesSettings {
  showBorder: boolean;
  borderWidth: number;
  borderRadius: number;
  showLabel: boolean;
  labelPosition: SeriesLabelPosition;
  labelFont: Font;
  labelColor: string;
  backgroundSettings: SeriesFillSettings;
}

export interface TimeSeriesChartKeySettings {
  showInLegend: boolean;
  dataHiddenByDefault: boolean;
  type: TimeSeriesChartSeriesType;
  lineSettings: LineSeriesSettings;
  barSettings: BarSeriesSettings;
}

export const timeSeriesChartKeyDefaultSettings: TimeSeriesChartKeySettings = {
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
    pointShape: TimeSeriesChartShape.emptyCircle,
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
  yAxisIndex: number;
  option?: LineSeriesOption | CustomSeriesOption;
  barRenderContext?: BarRenderContext;
}

type TimeSeriesChartThresholdValue = number | string | (number | string)[];

export interface TimeSeriesChartThresholdItem {
  id: string;
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
  units: string;
  option: YAXisOption & ValueAxisBaseOption;
  intervalCalculator?: (axis: Axis2D) => number;
}

export const createTimeSeriesYAxis = (axisId: string, units: string,
                                      decimals: number, settings: TimeSeriesChartYAxisSettings,
                                      darkMode: boolean): TimeSeriesChartYAxis => {
  const yAxisTickLabelStyle = createChartTextStyle(settings.tickLabelFont,
    settings.tickLabelColor, darkMode, 'axis.tickLabel');
  const yAxisNameStyle = createChartTextStyle(settings.labelFont,
    settings.labelColor, darkMode, 'axis.label');
  const yAxis: TimeSeriesChartYAxis = {
    id: axisId,
    units,
    option: {
      show: settings.show,
      type: 'value',
      position: settings.position,
      id: axisId,
      offset: 0,
      alignTicks: true,
      scale: true,
      min: settings.min,
      max: settings.max,
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
        formatter: (value: any) => formatValue(value, decimals, units, false)
      },
      splitLine: {
        show: settings.showSplitLines,
        lineStyle: {
          color: prepareChartThemeColor(settings.splitLinesColor, darkMode, 'axis.splitLine')
        }
      }
    }
  };
  if (settings.intervalCalculator && settings.intervalCalculator.length) {
    yAxis.intervalCalculator = parseFunction(settings.intervalCalculator, ['axis']);
  }
  return yAxis;
};

export const createTimeSeriesXAxisOption = (settings: TimeSeriesChartAxisSettings,
                                            min: number, max: number, darkMode: boolean): XAXisOption => {
  const xAxisTickLabelStyle = createChartTextStyle(settings.tickLabelFont,
    settings.tickLabelColor, darkMode, 'axis.tickLabel');
  const xAxisNameStyle = createChartTextStyle(settings.labelFont,
    settings.labelColor, darkMode, 'axis.label');
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
      hideOverlap: true
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
    max
  };
};

export const generateChartData = (dataItems: TimeSeriesChartDataItem[],
                                  thresholdItems: TimeSeriesChartThresholdItem[],
                                  timeInterval: Interval,
                                  noAggregation: boolean,
                                  noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings,
                                  stack: boolean,
                                  darkMode: boolean): Array<LineSeriesOption | CustomSeriesOption> => {
  let series = generateChartSeries(dataItems, timeInterval,
    noAggregation, noAggregationBarWidthSettings, stack, darkMode);
  if (thresholdItems.length) {
    const thresholds = generateChartThresholds(thresholdItems, darkMode);
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
    const offset = measureThresholdLabelOffset(chart, yAxis.id, item.id, item.value);
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

const generateChartThresholds = (thresholdItems: TimeSeriesChartThresholdItem[], darkMode: boolean): Array<LineSeriesOption> => {
  const series: Array<LineSeriesOption> = [];
  for (const item of thresholdItems) {
    if (isDefinedAndNotNull(item.value)) {
      let seriesOption = item.option;
      if (!item.option) {
        const thresholdLabelStyle = createChartTextStyle(item.settings.labelFont,
          item.settings.labelColor, darkMode, 'threshold.label');
        seriesOption = {
          type: 'line',
          id: item.id,
          dataGroupId: item.id,
          yAxisIndex: item.yAxisIndex,
          animation: true,
          data: [],
          tooltip: {
            show: false
          },
          markLine: {
            animation: true,
            lineStyle: {
              width: item.settings.lineWidth,
              color: prepareChartThemeColor(item.settings.lineColor, darkMode, 'threshold.line'),
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
                             timeInterval: Interval,
                             noAggregation: boolean,
                             noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings,
                             stack: boolean,
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
        item.barRenderContext = {noAggregation, noAggregationBarWidthSettings};
      }
      item.barRenderContext.noAggregation = noAggregation;
      item.barRenderContext.barsCount = barsCount;
      item.barRenderContext.barIndex = stack ? barGroups.indexOf(item.yAxisIndex) : barDataItems.indexOf(item);
      item.barRenderContext.timeInterval = timeInterval;
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
                               dataItems: TimeSeriesChartDataItem[], thresholdDataItems: TimeSeriesChartThresholdItem[],
                               darkMode: boolean): EChartsOption => {
  options.darkMode = darkMode;
  if (Array.isArray(options.yAxis)) {
    for (const yAxis of options.yAxis) {
      yAxis.nameTextStyle.color = prepareChartThemeColor(settings.yAxis.labelColor, darkMode, 'axis.label');
      yAxis.axisLabel.color = prepareChartThemeColor(settings.yAxis.tickLabelColor, darkMode, 'axis.tickLabel');
      yAxis.axisLine.lineStyle.color = prepareChartThemeColor(settings.yAxis.lineColor, darkMode, 'axis.line');
      yAxis.axisTick.lineStyle.color = prepareChartThemeColor(settings.yAxis.ticksColor, darkMode, 'axis.ticks');
      yAxis.splitLine.lineStyle.color = prepareChartThemeColor(settings.yAxis.splitLinesColor, darkMode, 'axis.splitLine');
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
        item.barRenderContext.labelOption.rich.value.color = prepareChartThemeColor(barSettings.labelColor,
          darkMode, 'series.label');
      }
    }
  }
  for (const item of thresholdDataItems) {
    if (Array.isArray(options.series)) {
      const series = options.series.find(s => s.id === item.id);
      if (series) {
        series.markLine.lineStyle.color = prepareChartThemeColor(item.settings.lineColor, darkMode, 'threshold.line');
        if (series.markLine?.label?.show) {
          series.markLine.label.color = prepareChartThemeColor(item.settings.labelColor, darkMode, 'threshold.label');
        }
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
      animation: true,
      dimensions: [
        {name: 'intervalStart', type: 'number'},
        {name: 'intervalEnd', type: 'number'}
      ],
      encode: {
        intervalStart: 2,
        intervalEnd: 3
      }
    };
    item.option = seriesOption;
    if (settings.type === TimeSeriesChartSeriesType.line) {
      const lineSettings = settings.lineSettings;
      const lineSeriesOption = seriesOption as LineSeriesOption;
      lineSeriesOption.type = 'line';
      lineSeriesOption.label = createSeriesLabelOption(item, lineSettings.showPointLabel,
        lineSettings.pointLabelFont, lineSettings.pointLabelColor, lineSettings.pointLabelPosition, darkMode);
      lineSeriesOption.step = lineSettings.step ? lineSettings.stepType : false;
      lineSeriesOption.smooth = lineSettings.smooth;
      lineSeriesOption.lineStyle = {
        width: lineSettings.showLine ? lineSettings.lineWidth : 0,
        type: lineSettings.lineType
      };
      if (lineSettings.fillAreaSettings.type !== SeriesFillType.none) {
        lineSeriesOption.areaStyle = {};
        if (lineSettings.fillAreaSettings.type === SeriesFillType.opacity) {
          lineSeriesOption.areaStyle.opacity = lineSettings.fillAreaSettings.opacity;
        } else {
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
        barSettings.labelFont, barSettings.labelColor, barSettings.labelPosition, darkMode);
      barSeriesOption.renderItem = (params, api) =>
        renderTimeSeriesBar(params, api, item.barRenderContext);
    }
  }
  seriesOption.data = item.data;
  return seriesOption;
};

const createSeriesLabelOption = (item: TimeSeriesChartDataItem, show: boolean,
                                 labelFont: Font, labelColor: string, position: SeriesLabelPosition,
                                 darkMode: boolean): SeriesLabelOption => {
  let labelStyle: ComponentStyle = {};
  if (show) {
    labelStyle = createChartTextStyle(labelFont, labelColor, darkMode, 'series.label');
  }
  return {
    show,
    position,
    formatter: (params): string => {
      const value = formatValue(params.value[1], item.decimals, item.units, false);
      return `{value|${value}}`;
    },
    rich: {
      value: labelStyle
    }
  };
};

const createChartTextStyle = (font: Font, color: string, darkMode: boolean, colorKey?: string): ComponentStyle => {
  const style = textStyle(font);
  delete style.lineHeight;
  style.fontSize = font.size;
  style.color = prepareChartThemeColor(color, darkMode, colorKey);
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
