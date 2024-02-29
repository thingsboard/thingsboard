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
import { formatValue, isDefinedAndNotNull, parseFunction } from '@core/utils';
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

export enum PointLabelPosition {
  top = 'top',
  bottom = 'bottom'
}

export enum AxisPosition {
  left = 'left',
  right = 'right',
  top = 'top',
  bottom = 'bottom'
}

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

export enum TimeSeriesChartLineType {
  solid = 'solid',
  dashed = 'dashed',
  dotted = 'dotted'
}

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

export interface TimeSeriesChartAxisSettings {
  show: boolean;
  position: AxisPosition;
  label?: string;
  labelFont?: Font;
  labelColor?: string;
  showLine: boolean;
  lineColor: string;
  showTicks: boolean;
  ticksColor: string;
  showTickLabels: boolean;
  tickLabelFont: Font;
  tickLabelColor: string;
  showSplitLines: boolean;
  splitLinesColor: string;
}

export interface TimeSeriesChartYAxisSettings extends TimeSeriesChartAxisSettings {
  min?: number | string;
  max?: number | string;
  intervalCalculator?: string;
}

export enum TimeSeriesChartThresholdType {
  constant = 'constant',
  latestKey = 'latestKey',
  entity = 'entity'
}

export interface TimeSeriesChartThreshold {
  type: TimeSeriesChartThresholdType;
  value?: number;
  latestKeyName?: string;
  entityAlias?: string;
  entityKeyType?: DataKeyType.attribute | DataKeyType.timeseries;
  entityKey?: string;
  units?: string;
  decimals?: number;
  lineWidth: number;
  lineType: TimeSeriesChartLineType;
  lineColor: string;
  startSymbol: TimeSeriesChartShape;
  startSymbolSize: number;
  endSymbol: TimeSeriesChartShape;
  endSymbolSize: number;
  showLabel: boolean;
  labelPosition: ThresholdLabelPosition;
  labelFont: Font;
  labelColor: string;
}

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
  'series.pointLabel': {
    light: 'rgba(0, 0, 0, 0.76)',
    dark: '#eee'
  }
};

export const timeSeriesChartThresholdDefaultSettings: TimeSeriesChartThreshold = {
  type: TimeSeriesChartThresholdType.constant,
  units: '',
  decimals: 0,
  lineWidth: 1,
  lineType: TimeSeriesChartLineType.solid,
  lineColor: timeSeriesChartColorScheme['threshold.line'].light,
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

export interface TimeSeriesChartSettings extends EChartsTooltipWidgetSettings {
  darkMode: boolean;
  dataZoom: boolean;
  stack: boolean;
  thresholds: TimeSeriesChartThreshold[];
  xAxis: TimeSeriesChartAxisSettings;
  yAxis: TimeSeriesChartYAxisSettings;
}

export const timeSeriesChartDefaultSettings: TimeSeriesChartSettings = {
  darkMode: false,
  dataZoom: true,
  stack: false,
  thresholds: [],
  xAxis: {
    show: true,
    position: AxisPosition.bottom,
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
    showLine: true,
    lineColor: timeSeriesChartColorScheme['axis.line'].light,
    showTicks: true,
    ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
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
    showSplitLines: true,
    splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
  },
  yAxis: {
    show: true,
    position: AxisPosition.left,
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
    showLine: true,
    lineColor: timeSeriesChartColorScheme['axis.line'].light,
    showTicks: true,
    ticksColor: timeSeriesChartColorScheme['axis.ticks'].light,
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
    showSplitLines: true,
    splitLinesColor: timeSeriesChartColorScheme['axis.splitLine'].light
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
  tooltipDateInterval: true,
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
  tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
  tooltipBackgroundBlur: 4
};

export enum SeriesFillType {
  none = 'none',
  opacity = 'opacity',
  gradient = 'gradient'
}

export interface SeriesFillSettings {
  type: SeriesFillType;
  opacity: number;
  gradient: {
    start: number;
    end: number;
  };
}

export interface LineSeriesSettings {
  step: false | 'start' | 'end' | 'middle';
  smooth: boolean;
  showLine: boolean;
  lineWidth: number;
  lineType: TimeSeriesChartLineType;
  fillAreaSettings: SeriesFillSettings;
  showPoints: boolean;
  pointShape: TimeSeriesChartShape;
  pointSize: number;
}

export interface BarSeriesSettings {
  showBorder: boolean;
  borderWidth: number;
  borderRadius: number;
  backgroundSettings: SeriesFillSettings;
}

export enum TimeSeriesChartSeriesType {
  line = 'line',
  bar = 'bar'
}

export interface TimeSeriesChartKeySettings {
  dataHiddenByDefault: boolean;
  showInLegend: boolean;
  showPointLabel: boolean;
  pointLabelPosition: PointLabelPosition;
  pointLabelFont: Font;
  pointLabelColor: string;
  type: TimeSeriesChartSeriesType;
  lineSettings: LineSeriesSettings;
  barSettings: BarSeriesSettings;
}

export const timeSeriesChartKeyDefaultSettings: TimeSeriesChartKeySettings = {
  showInLegend: true,
  dataHiddenByDefault: false,
  showPointLabel: false,
  pointLabelPosition: PointLabelPosition.top,
  pointLabelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  pointLabelColor: timeSeriesChartColorScheme['series.pointLabel'].light,
  type: TimeSeriesChartSeriesType.line,
  lineSettings: {
    step: false,
    smooth: false,
    showLine: true,
    lineWidth: 2,
    lineType: TimeSeriesChartLineType.solid,
    fillAreaSettings: {
      type: SeriesFillType.none,
      opacity: 0.4,
      gradient: {
        start: 100,
        end: 0
      }
    },
    showPoints: false,
    pointShape: TimeSeriesChartShape.emptyCircle,
    pointSize: 4
  },
  barSettings: {
    showBorder: false,
    borderWidth: 2,
    borderRadius: 0,
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
                                  stack: boolean,
                                  darkMode: boolean): Array<LineSeriesOption | CustomSeriesOption> => {
  let series = generateChartSeries(dataItems, timeInterval, stack, darkMode);
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
            symbol: [item.settings.startSymbol, item.settings.endSymbol],
            symbolSize: [item.settings.startSymbolSize, item.settings.endSymbolSize],
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
          seriesOption.markLine.data.push({
            yAxis: val
          });
        }
      } else {
        seriesOption.markLine.data.push({
          yAxis: item.value
        });
      }
      series.push(seriesOption);
    }
  }
  return series;
};

const generateChartSeries = (dataItems: TimeSeriesChartDataItem[],
                             timeInterval: Interval,
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
        item.barRenderContext = {};
      }
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
      if (item.option.label?.show) {
        item.option.label.rich.value.color = prepareChartThemeColor(item.dataKey.settings.pointLabelColor, darkMode, 'series.pointLabel');
      }
      if (Array.isArray(options.series)) {
        const series = options.series.find(s => s.id === item.id);
        if (series) {
          if (series.label?.show) {
            series.label.rich.value.color = prepareChartThemeColor(item.dataKey.settings.pointLabelColor, darkMode, 'series.pointLabel');
          }
        }
      }
    } else {
      if (item.barRenderContext?.labelOption?.show) {
        item.barRenderContext.labelOption.rich.value.color = prepareChartThemeColor(item.dataKey.settings.pointLabelColor,
          darkMode, 'series.pointLabel');
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
    let pointLabelStyle: ComponentStyle = {};
    if (settings.showPointLabel) {
      pointLabelStyle = createChartTextStyle(settings.pointLabelFont, settings.pointLabelColor, darkMode, 'series.pointLabel');
    }
    const label: SeriesLabelOption = {
      show: settings.showPointLabel,
      position: settings.pointLabelPosition,
      formatter: (params): string => {
        const value = formatValue(params.value[1], item.decimals, item.units, false);
        return `{value|${value}}`;
      },
      rich: {
        value: pointLabelStyle
      }
    };
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
      lineSeriesOption.label = label;
      lineSeriesOption.step = lineSettings.step;
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
      item.barRenderContext.labelOption = label;
      barSeriesOption.renderItem = (params, api) =>
        renderTimeSeriesBar(params, api, item.barRenderContext);
    }
  }
  seriesOption.data = item.data;
  return seriesOption;
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
