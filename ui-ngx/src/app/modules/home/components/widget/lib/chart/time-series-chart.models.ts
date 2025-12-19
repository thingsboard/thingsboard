///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  measureThresholdOffset
} from '@home/components/widget/lib/chart/echarts-widget.models';
import {
  autoDateFormat,
  AutoDateFormatSettings,
  ComponentStyle,
  Font,
  tsToFormatTimeUnit,
  ValueSourceConfig,
  ValueSourceType
} from '@shared/models/widget-settings.models';
import {
  TimeAxisBandWidthCalculator,
  VisualMapComponentOption,
  XAXisOption,
  YAXisOption
} from 'echarts/types/dist/shared';
import { CustomSeriesOption, LineSeriesOption } from 'echarts/charts';
import {
  formatValue,
  isDefinedAndNotNull,
  isFunction,
  isNumber,
  isNumeric,
  isUndefined,
  isUndefinedOrNull,
  mergeDeep,
  parseFunction
} from '@core/utils';
import tinycolor from 'tinycolor2';
import { TimeAxisBaseOption, ValueAxisBaseOption } from 'echarts/types/src/coord/axisCommonTypes';
import { SeriesLabelOption } from 'echarts/types/src/util/types';
import { LabelFormatterCallback } from 'echarts';
import {
  BarRenderContext,
  BarRenderSharedContext,
  BarVisualSettings,
  renderTimeSeriesBar
} from '@home/components/widget/lib/chart/time-series-chart-bar.models';
import {
  DataEntry,
  DataKey,
  DataKeySettingsWithComparison,
  DataSet,
  Datasource,
  FormattedData,
  WidgetComparisonSettings
} from '@shared/models/widget.models';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { MarkLine2DDataItemOption } from 'echarts/types/src/component/marker/MarkLineModel';
import { DatePipe } from '@angular/common';
import { BuiltinTextPosition } from 'zrender/src/core/types';
import { CartesianAxisOption } from 'echarts/types/src/coord/cartesian/AxisModel';
import {
  calculateAggIntervalWithWidgetTimeWindow,
  IntervalMath,
  WidgetTimewindow
} from '@shared/models/time/time.models';
import { UtilsService } from '@core/services/utils.service';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  chartBarDefaultSettings,
  ChartBarSettings,
  chartColorScheme,
  ChartFillSettings,
  ChartFillType,
  ChartLabelPosition,
  ChartLineType,
  ChartShape,
  createChartTextStyle,
  createLinearOpacityGradient,
  PieChartLabelPosition,
  prepareChartThemeColor
} from '@home/components/widget/lib/chart/chart.models';
import { BarSeriesLabelOption } from 'echarts/types/src/chart/bar/BarSeries';
import {
  TimeSeriesChartTooltipTrigger,
  TimeSeriesChartTooltipValueFormatFunction,
  TimeSeriesChartTooltipWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';
import { TbUnit, TbUnitConverter } from '@shared/models/unit.models';

type TimeSeriesChartDataEntry = [number, any, number, number];

type TimeSeriesChartDataSet = {name: string; value: TimeSeriesChartDataEntry}[];

export const toTimeSeriesChartDataSet = (data: DataSet, valueConverter?: (value: any) => any): TimeSeriesChartDataSet => {
  if (!data?.length) {
    return [];
  } else {
    return data.map(d => {
      const ts = isDefinedAndNotNull(d[2]) ? d[2][0] : d[0];
      return {
        name: ts + '',
        value: toTimeSeriesChartDataEntry(d, valueConverter)
      };
    });
  }
};

const toTimeSeriesChartDataEntry = (entry: DataEntry, valueConverter?: (value: any) => any): TimeSeriesChartDataEntry => {
  const value = valueConverter ? valueConverter(entry[1]) : entry[1];
  const item: TimeSeriesChartDataEntry = [entry[0], value, entry[0], entry[0]];
  if (isDefinedAndNotNull(entry[2])) {
    item[2] = entry[2][0];
    item[3] = entry[2][1];
  }
  return item;
};

export interface TimeSeriesChartDataItem {
  id: string;
  datasource: Datasource;
  dataKey: DataKey;
  data: TimeSeriesChartDataSet;
  dataSet?: DataSet;
  enabled: boolean;
  units?: string;
  decimals?: number;
  latestData?: FormattedData;
  tooltipValueFormatFunction?: TimeSeriesChartTooltipValueFormatFunction;
  comparisonItem?: boolean;
  xAxisIndex: number;
  yAxisId: TimeSeriesChartYAxisId;
  yAxisIndex: number;
  option?: LineSeriesOption;
  barRenderContext?: BarRenderContext;
  unitConvertor?: TbUnitConverter;
}

export const timeAxisBandWidthCalculator: TimeAxisBandWidthCalculator = (model) => {
  let interval: number;
  const axisOption = model.option;
  const seriesDataIndices = axisOption.axisPointer?.seriesDataIndices;
  if (seriesDataIndices?.length) {
    const seriesDataIndex = seriesDataIndices[0];
    const series = model.ecModel.getSeriesByIndex(seriesDataIndex.seriesIndex);
    if (series) {
      const values = series.getData().getValues(seriesDataIndex.dataIndex);
      const start = values[2];
      const end = values[3];
      if (typeof start === 'number' && typeof end === 'number') {
        interval = Math.max(end - start, 1);
      }
    }
  }
  if (!interval) {
    const tbTimeWindow: WidgetTimewindow = (axisOption as any).tbTimeWindow;
    if (isDefinedAndNotNull(tbTimeWindow)) {
      if (axisOption.axisPointer?.value && typeof axisOption.axisPointer?.value === 'number') {
        const intervalArray = calculateAggIntervalWithWidgetTimeWindow(tbTimeWindow, axisOption.axisPointer.value);
        const start = intervalArray[0];
        const end = intervalArray[1];
        interval = Math.max(end - start, 1);
      } else {
        interval = IntervalMath.numberValue(tbTimeWindow.interval);
      }
    }
  }
  if (interval) {
    const timeScale = model.axis.scale;
    const axisExtent = model.axis.getExtent();
    const dataExtent = timeScale.getExtent();
    const size = Math.abs(axisExtent[1] - axisExtent[0]);
    return interval * (size / (dataExtent[1] - dataExtent[0]));
  }
};


const minDataTs = (dataSet: TimeSeriesChartDataSet): number => dataSet.length ? dataSet.map(data =>
  Number(data.name)).reduce((a, b) => Math.min(a, b)) : undefined;

const maxDataTs = (dataSet: TimeSeriesChartDataSet): number => dataSet.length ? dataSet.map(data =>
  Number(data.name)).reduce((a, b) => Math.max(a, b)) : undefined;

export const adjustTimeAxisExtentToData = (timeAxisOption: TimeAxisBaseOption,
                                           dataItems: TimeSeriesChartDataItem[],
                                           defaultMin: number,
                                           defaultMax: number): void => {
  let min: number;
  let max: number;
  for (const item of dataItems) {
    if (item.enabled) {
      const minTs = minDataTs(item.data);
      if (typeof minTs !== 'undefined') {
        min = (typeof min !== 'undefined') ? Math.min(min, minTs) : minTs;
      }
      const maxTs = maxDataTs(item.data);
      if (typeof maxTs !== 'undefined') {
        max = (typeof max !== 'undefined') ? Math.max(max, maxTs) : maxTs;
      }
    }
  }
  timeAxisOption.min = (typeof min !== 'undefined' && Math.abs(min - defaultMin) < 1000) ? min : defaultMin;
  timeAxisOption.max = (typeof max !== 'undefined' && Math.abs(max - defaultMax) < 1000) ? max : defaultMax;
};


export enum TimeSeriesChartType {
  default = 'default',
  line = 'line',
  bar = 'bar',
  point = 'point',
  state = 'state'
}

export const timeSeriesChartTypeTranslations = new Map<TimeSeriesChartType, string>(
  [
    [TimeSeriesChartType.line, 'widgets.time-series-chart.type-line'],
    [TimeSeriesChartType.bar, 'widgets.time-series-chart.type-bar'],
    [TimeSeriesChartType.point, 'widgets.time-series-chart.type-point']
  ]
);

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

export enum TimeSeriesChartStateSourceType {
  constant = 'constant',
  range = 'range'
}

export const timeSeriesStateSourceTypes = Object.keys(TimeSeriesChartStateSourceType) as TimeSeriesChartStateSourceType[];

export const timeSeriesStateSourceTypeTranslations = new Map<TimeSeriesChartStateSourceType, string>(
  [
    [TimeSeriesChartStateSourceType.constant, 'widgets.time-series-chart.state.type-constant'],
    [TimeSeriesChartStateSourceType.range, 'widgets.time-series-chart.state.type-range']
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
  units?: TbUnit;
  decimals?: number;
  interval?: number;
  splitNumber?: number;
  min?: number | string | ValueSourceConfig;
  max?: number | string | ValueSourceConfig;
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
  labelColor: chartColorScheme['axis.label'].light,
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
  tickLabelColor: chartColorScheme['axis.tickLabel'].light,
  ticksFormatter: null,
  showTicks: true,
  ticksColor: chartColorScheme['axis.ticks'].light,
  showLine: true,
  lineColor: chartColorScheme['axis.line'].light,
  showSplitLines: true,
  splitLinesColor: chartColorScheme['axis.splitLine'].light
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
  labelColor: chartColorScheme['axis.label'].light,
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
  tickLabelColor: chartColorScheme['axis.tickLabel'].light,
  ticksFormat: {},
  showTicks: true,
  ticksColor: chartColorScheme['axis.ticks'].light,
  showLine: true,
  lineColor: chartColorScheme['axis.line'].light,
  showSplitLines: true,
  splitLinesColor: chartColorScheme['axis.splitLine'].light
};

export type TimeSeriesChartYAxes = {[id: TimeSeriesChartYAxisId]: TimeSeriesChartYAxisSettings};

export interface TimeSeriesChartThreshold extends ValueSourceConfig {
  yAxisId: TimeSeriesChartYAxisId;
  units?: string;
  decimals?: number;
  lineColor: string;
  lineType: ChartLineType | number | number[];
  lineWidth: number;
  startSymbol: ChartShape;
  startSymbolSize: number;
  endSymbol: ChartShape;
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
    case ValueSourceType.constant:
      if (isUndefinedOrNull(threshold.value)) {
        return false;
      }
      break;
    case ValueSourceType.latestKey:
      if (!threshold.latestKey || !threshold.latestKeyType) {
        return false;
      }
      break;
    case ValueSourceType.entity:
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
  type: ValueSourceType.constant,
  yAxisId: 'default',
  units: null,
  decimals: 0,
  lineColor: chartColorScheme['threshold.line'].light,
  lineType: ChartLineType.solid,
  lineWidth: 1,
  startSymbol: ChartShape.none,
  startSymbolSize: 5,
  endSymbol: ChartShape.arrow,
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
  labelColor: chartColorScheme['threshold.label'].light,
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

export interface TimeSeriesChartStateSettings {
  label: string;
  value: number;
  sourceType: TimeSeriesChartStateSourceType;
  sourceValue?: any;
  sourceRangeFrom?: number;
  sourceRangeTo?: number;
}

export const timeSeriesChartStateValid = (state: TimeSeriesChartStateSettings): boolean => {
  if (isUndefinedOrNull(state.value) || !state.sourceType) {
    return false;
  }
  switch (state.sourceType) {
    case TimeSeriesChartStateSourceType.constant:
      if (isUndefinedOrNull(state.sourceValue)) {
        return false;
      }
      break;
  }
  return true;
};

export const timeSeriesChartStateValidator = (control: AbstractControl): ValidationErrors | null => {
  const state: TimeSeriesChartStateSettings = control.value;
  if (!timeSeriesChartStateValid(state)) {
    return {
      state: true
    };
  }
  return null;
};

export interface TimeSeriesChartComparisonSettings extends WidgetComparisonSettings {
  comparisonXAxis?: TimeSeriesChartXAxisSettings;
}

export interface TimeSeriesChartGridSettings {
  show: boolean;
  backgroundColor: string;
  borderWidth: number;
  borderColor: string;
}

export const timeSeriesChartGridDefaultSettings: TimeSeriesChartGridSettings = {
  show: false,
  backgroundColor: null,
  borderWidth: 1,
  borderColor: '#ccc'
};

export interface TimeSeriesChartSettings extends TimeSeriesChartTooltipWidgetSettings, TimeSeriesChartComparisonSettings {
  thresholds: TimeSeriesChartThreshold[];
  darkMode: boolean;
  dataZoom: boolean;
  stack: boolean;
  grid: TimeSeriesChartGridSettings;
  yAxes: TimeSeriesChartYAxes;
  xAxis: TimeSeriesChartXAxisSettings;
  animation: ChartAnimationSettings;
  barWidthSettings: TimeSeriesChartBarWidthSettings;
  noAggregationBarWidthSettings: TimeSeriesChartNoAggregationBarWidthSettings;
  visualMapSettings?: TimeSeriesChartVisualMapSettings;
  states?: TimeSeriesChartStateSettings[];
}

export const timeSeriesChartDefaultSettings: TimeSeriesChartSettings = {
  thresholds: [],
  darkMode: false,
  dataZoom: true,
  stack: false,
  grid: mergeDeep({} as TimeSeriesChartGridSettings,
    timeSeriesChartGridDefaultSettings),
  yAxes: {
    default: mergeDeep({} as TimeSeriesChartYAxisSettings,
                       defaultTimeSeriesChartYAxisSettings,
                       { id: 'default', order: 0 } as TimeSeriesChartYAxisSettings)
  },
  xAxis: mergeDeep({} as TimeSeriesChartXAxisSettings,
    defaultTimeSeriesChartXAxisSettings),
  animation: mergeDeep({} as ChartAnimationSettings,
    chartAnimationDefaultSettings),
  barWidthSettings: {
    barGap: 0.3,
    intervalGap: 0.6
  },
  noAggregationBarWidthSettings: mergeDeep({} as TimeSeriesChartNoAggregationBarWidthSettings,
    timeSeriesChartNoAggregationBarWidthDefaultSettings),
  showTooltip: true,
  tooltipTrigger: TimeSeriesChartTooltipTrigger.axis,
  tooltipLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  tooltipLabelColor: 'rgba(0, 0, 0, 0.76)',
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
  tooltipStackedShowTotal: false,
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 4,
  comparisonEnabled: false,
  timeForComparison: 'previousInterval',
  comparisonCustomIntervalValue: 7200000,
  comparisonXAxis: mergeDeep({} as TimeSeriesChartXAxisSettings,
    defaultTimeSeriesChartXAxisSettings,
    { position: AxisPosition.top } as TimeSeriesChartXAxisSettings)
};

export interface LineSeriesSettings {
  showLine: boolean;
  step: boolean;
  stepType: LineSeriesStepType;
  smooth: boolean;
  lineType: ChartLineType;
  lineWidth: number;
  showPoints: boolean;
  showPointLabel: boolean;
  pointLabelPosition: ChartLabelPosition;
  pointLabelFont: Font;
  pointLabelColor: string;
  enablePointLabelBackground: boolean;
  pointLabelBackground: string;
  pointLabelFormatter?: string | LabelFormatterCallback;
  pointShape: ChartShape;
  pointSize: number;
  fillAreaSettings: ChartFillSettings;
}

export interface TimeSeriesChartKeySettings extends DataKeySettingsWithComparison {
  yAxisId: TimeSeriesChartYAxisId;
  showInLegend: boolean;
  dataHiddenByDefault: boolean;
  type: TimeSeriesChartSeriesType;
  lineSettings: LineSeriesSettings;
  barSettings: ChartBarSettings;
  tooltipValueFormatter?: string | TimeSeriesChartTooltipValueFormatFunction;
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
    lineType: ChartLineType.solid,
    lineWidth: 2,
    showPoints: false,
    showPointLabel: false,
    pointLabelPosition: ChartLabelPosition.top,
    pointLabelFont: {
      family: 'Roboto',
      size: 11,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '1'
    },
    pointLabelColor: chartColorScheme['series.label'].light,
    enablePointLabelBackground: false,
    pointLabelBackground: 'rgba(255,255,255,0.56)',
    pointShape: ChartShape.emptyCircle,
    pointSize: 4,
    fillAreaSettings: {
      type: ChartFillType.none,
      opacity: 0.4,
      gradient: {
        start: 100,
        end: 0
      }
    }
  },
  barSettings: mergeDeep({} as ChartBarSettings, chartBarDefaultSettings),
  comparisonSettings: {
    showValuesForComparison: false,
    comparisonValuesLabel: '',
    color: ''
  }
};

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
  unitConvertor?: TbUnitConverter
}

export interface TimeSeriesChartAxis {
  id: string;
  settings: TimeSeriesChartAxisSettings;
  option: CartesianAxisOption;
  minLatestDataKey?: DataKey;
  maxLatestDataKey?: DataKey;
  unitConvertor?: (value: number) => number;
}

export interface TimeSeriesChartYAxis extends TimeSeriesChartAxis {
  decimals: number;
  settings: TimeSeriesChartYAxisSettings;
  option: YAXisOption & ValueAxisBaseOption;
}

export interface TimeSeriesChartXAxis extends TimeSeriesChartAxis {
  settings: TimeSeriesChartXAxisSettings;
  option: XAXisOption;
}

export const createTimeSeriesYAxis = (units: string,
                                      decimals: number,
                                      settings: TimeSeriesChartYAxisSettings,
                                      utils: UtilsService,
                                      darkMode: boolean,
                                      unitConvertor: (x: number) => number): TimeSeriesChartYAxis => {
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
  let configuredTicksGenerator: TimeSeriesChartTicksGenerator;
  let ticksGenerator: TimeSeriesChartTicksGenerator;
  let minInterval: number;
  let interval: number;
  let splitNumber: number;
  if (settings.ticksGenerator) {
    if (isFunction(settings.ticksGenerator)) {
      configuredTicksGenerator = settings.ticksGenerator as TimeSeriesChartTicksGenerator;
    } else if (settings.ticksGenerator.length) {
      configuredTicksGenerator = parseFunction(settings.ticksGenerator, ['extent', 'interval', 'niceTickExtent', 'intervalPrecision']);
    }
  }
  if (!configuredTicksGenerator) {
    interval = settings.interval;
    if (isUndefinedOrNull(interval)) {
      if (isDefinedAndNotNull(settings.splitNumber)) {
        splitNumber = settings.splitNumber;
      } else {
        minInterval = (1 / Math.pow(10, decimals));
      }
    }
  } else {
    ticksGenerator = (extent, ticksInterval, niceTickExtent, intervalPrecision) => {
      const ticks = configuredTicksGenerator(extent, ticksInterval, niceTickExtent, intervalPrecision);
      return ticks?.filter(tick => tick.value >= extent[0] && tick.value <= extent[1]);
    };
  }

  let initialMin: number | string | undefined;
  if (isDefinedAndNotNull(settings.min)) {
    if (typeof settings.min === 'object' && 'type' in settings.min) {
      initialMin = undefined;
    } else if (typeof settings.min === 'number') {
      initialMin = unitConvertor ? unitConvertor(settings.min) : settings.min;
    } else if (typeof settings.min === 'string') {
      initialMin = settings.min;
    }
  }

  let initialMax: number | string | undefined;
  if (isDefinedAndNotNull(settings.max)) {
    if (typeof settings.max === 'object' && 'type' in settings.max) {
      initialMax = undefined;
    } else if (typeof settings.max === 'number') {
      initialMax = unitConvertor ? unitConvertor(settings.max) : settings.max;
    } else if (typeof settings.max === 'string') {
      initialMax = settings.max;
    }
  }

  return {
    id: settings.id,
    decimals,
    settings,
    option: {
      mainType: 'yAxis',
      show: settings.show,
      type: 'value',
      position: settings.position,
      id: settings.id,
      offset: 0,
      alignTicks: true,
      scale: true,
      min: initialMin,
      max: initialMax,
      minInterval,
      splitNumber,
      interval,
      ticksGenerator,
      name: utils.customTranslation(settings.label, settings.label),
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

export const createTimeSeriesXAxis = (id: string,
                                      settings: TimeSeriesChartXAxisSettings,
                                      min: number, max: number,
                                      datePipe: DatePipe,
                                      utils: UtilsService,
                                      darkMode: boolean): TimeSeriesChartXAxis => {
  const xAxisTickLabelStyle = createChartTextStyle(settings.tickLabelFont,
    settings.tickLabelColor, darkMode, 'axis.tickLabel');
  const xAxisNameStyle = createChartTextStyle(settings.labelFont,
    settings.labelColor, darkMode, 'axis.label');
  const ticksFormat = mergeDeep({}, defaultXAxisTicksFormat, settings.ticksFormat);
  return {
    id,
    settings,
    option: {
      mainType: 'xAxis',
      show: settings.show,
      type: 'time',
      position: settings.position,
      id,
      name: utils.customTranslation(settings.label, settings.label),
      nameLocation: 'middle',
      nameTextStyle: {
        color: xAxisNameStyle.color,
        fontStyle: xAxisNameStyle.fontStyle,
        fontWeight: xAxisNameStyle.fontWeight,
        fontFamily: xAxisNameStyle.fontFamily,
        fontSize: xAxisNameStyle.fontSize
      },
      axisPointer: {
        shadowStyle: {
          color: id === 'main' ? 'rgba(210,219,238,0.2)' : 'rgba(150,150,150,0.1)'
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
    }
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

export const updateXAxisTimeWindow = (option: XAXisOption,
                                      timeWindow: WidgetTimewindow) => {
  option.min = timeWindow.minTime;
  option.max = timeWindow.maxTime;
  (option as any).tbTimeWindow = timeWindow;
};

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

export const parseThresholdData = (value: any, valueConvertor?: TbUnitConverter): TimeSeriesChartThresholdValue => {
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
  return valueConvertor ? thresholdValue.map(item => isNumeric(item) ? valueConvertor(Number(item)) : item) : thresholdValue;
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

export const updateDarkMode = (options: EChartsOption,
                               xAxisList: TimeSeriesChartXAxis[],
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
    for (let i = 0; i < options.xAxis.length; i++) {
      const xAxis = options.xAxis[i];
      const xAxisSettings = xAxisList[i].settings;
      xAxis.nameTextStyle.color = prepareChartThemeColor(xAxisSettings.labelColor, darkMode, 'axis.label');
      xAxis.axisLabel.color = prepareChartThemeColor(xAxisSettings.tickLabelColor, darkMode, 'axis.tickLabel');
      xAxis.axisLine.lineStyle.color = prepareChartThemeColor(xAxisSettings.lineColor, darkMode, 'axis.line');
      xAxis.axisTick.lineStyle.color = prepareChartThemeColor(xAxisSettings.ticksColor, darkMode, 'axis.ticks');
      xAxis.splitLine.lineStyle.color = prepareChartThemeColor(xAxisSettings.splitLinesColor, darkMode, 'axis.splitLine');
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
        const barSettings = item.dataKey.settings as ChartBarSettings;
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
      xAxisIndex: item.xAxisIndex,
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
        lineSettings.pointLabelFormatter, false, darkMode) as SeriesLabelOption;
      lineSeriesOption.step = lineSettings.step ? lineSettings.stepType : false;
      lineSeriesOption.smooth = lineSettings.smooth ? 0.25 : false;
      if (lineSettings.smooth) {
        lineSeriesOption.smoothMonotone = 'none';
      }
      lineSeriesOption.lineStyle = {
        width: lineSettings.showLine ? lineSettings.lineWidth : 0,
        type: lineSettings.lineType
      };
      if (lineSettings.fillAreaSettings.type !== ChartFillType.none) {
        lineSeriesOption.areaStyle = {};
        if (lineSettings.fillAreaSettings.type === ChartFillType.opacity) {
          lineSeriesOption.areaStyle.opacity = lineSettings.fillAreaSettings.opacity;
        } else if (lineSettings.fillAreaSettings.type === ChartFillType.gradient) {
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
      if (barSettings.backgroundSettings.type === ChartFillType.none) {
        barVisualSettings.color = seriesColor;
      } else if (barSettings.backgroundSettings.type === ChartFillType.opacity) {
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
  if (seriesOption.type === 'line') {
    const settings: TimeSeriesChartKeySettings = item.dataKey.settings;
    const lineSettings = settings.lineSettings;
    if (!lineSettings.showPoints) {
      seriesOption.showSymbol = item.data.length === 1;
    }
  }
  return seriesOption;
};

const createSeriesLabelOption = (item: TimeSeriesChartDataItem, show: boolean,
                                 labelFont: Font, labelColor: string,
                                 enableBackground: boolean, labelBackground: string,
                                 position: ChartLabelPosition | PieChartLabelPosition | BuiltinTextPosition,
                                 labelFormatter: string | LabelFormatterCallback,
                                 labelColorFill: boolean,
                                 darkMode: boolean): SeriesLabelOption | BarSeriesLabelOption => {
  let labelStyle: ComponentStyle = {};
  if (show) {
    labelStyle = createChartTextStyle(labelFont, labelColor, darkMode, 'series.label', labelColorFill);
  }
  let formatFunction: (...args: any[]) => any;
  if (typeof labelFormatter === 'string' && labelFormatter.length) {
    formatFunction = parseFunction(labelFormatter, ['value']);
  }
  const formatter: LabelFormatterCallback = (params): string => {
    let result: string;
    if (typeof labelFormatter === 'string') {
      if (formatFunction) {
        try {
          result = formatFunction(params.value[1]);
        } catch (_e) {
        }
      }
    } else if (isFunction(labelFormatter)) {
      result = labelFormatter(params);
    }
    if (isUndefined(result)) {
      result = formatValue(params.value[1], item.decimals, item.units, false);
      result = `{value|${result}}`;
    }
    return result;
  };
  const labelOption: SeriesLabelOption | BarSeriesLabelOption = {
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
