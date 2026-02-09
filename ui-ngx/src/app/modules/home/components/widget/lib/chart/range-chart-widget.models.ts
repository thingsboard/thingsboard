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

import {
  BackgroundSettings,
  BackgroundType,
  ColorRange,
  filterIncludingColorRanges,
  Font,
  simpleDateFormat,
  sortedColorRange,
  ValueFormatProcessor,
  ValueSourceType
} from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import {
  createTimeSeriesChartVisualMapPiece,
  defaultTimeSeriesChartXAxisSettings,
  defaultTimeSeriesChartYAxisSettings,
  LineSeriesStepType,
  ThresholdLabelPosition,
  timeSeriesChartGridDefaultSettings,
  TimeSeriesChartGridSettings,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings,
  TimeSeriesChartThreshold,
  timeSeriesChartThresholdDefaultSettings,
  TimeSeriesChartVisualMapPiece,
  TimeSeriesChartXAxisSettings,
  TimeSeriesChartYAxisSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { isDefinedAndNotNull, isNumber, mergeDeep } from '@core/utils';
import { DeepPartial } from '@shared/models/common';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  chartColorScheme,
  ChartFillType,
  ChartLabelPosition,
  ChartLineType,
  ChartShape
} from '@home/components/widget/lib/chart/chart.models';
import {
  TimeSeriesChartTooltipWidgetSettings
} from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';
import { TbUnit } from '@shared/models/unit.models';

export interface RangeItem {
  index: number;
  from?: number;
  to?: number;
  color: string;
  label: string;
  visible: boolean;
  enabled: boolean;
  piece: TimeSeriesChartVisualMapPiece;
}

export interface RangeChartWidgetSettings extends TimeSeriesChartTooltipWidgetSettings {
  dataZoom: boolean;
  rangeColors: Array<ColorRange>;
  outOfRangeColor: string;
  showRangeThresholds: boolean;
  rangeThreshold: Partial<TimeSeriesChartThreshold>;
  fillArea: boolean;
  fillAreaOpacity: number;
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
  pointShape: ChartShape;
  pointSize: number;
  grid: TimeSeriesChartGridSettings;
  yAxis: TimeSeriesChartYAxisSettings;
  xAxis: TimeSeriesChartXAxisSettings;
  animation: ChartAnimationSettings;
  thresholds: TimeSeriesChartThreshold[];
  showLegend: boolean;
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const rangeChartDefaultSettings: RangeChartWidgetSettings = {
  dataZoom: true,
  rangeColors: [
    {to: -20, color: '#234CC7'},
    {from: -20, to: 0, color: '#305AD7'},
    {from: 0, to: 10, color: '#7191EF'},
    {from: 10, to: 20, color: '#FFA600'},
    {from: 20, to: 30, color: '#F36900'},
    {from: 30, to: 40, color: '#F04022'},
    {from: 40, color: '#D81838'}
  ],
  outOfRangeColor: '#ccc',
  showRangeThresholds: true,
  rangeThreshold: mergeDeep({} as Partial<TimeSeriesChartThreshold>,
    timeSeriesChartThresholdDefaultSettings,
    { lineColor: '#37383b',
      lineType: ChartLineType.dashed,
      startSymbol: ChartShape.circle,
      startSymbolSize: 5,
      endSymbol: ChartShape.arrow,
      endSymbolSize: 7,
      labelPosition: ThresholdLabelPosition.insideEndTop,
      labelColor: '#37383b',
      enableLabelBackground: true}),
  fillArea: true,
  fillAreaOpacity: 0.7,
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
  grid: mergeDeep({} as TimeSeriesChartGridSettings,
    timeSeriesChartGridDefaultSettings),
  yAxis: mergeDeep({} as TimeSeriesChartYAxisSettings,
    defaultTimeSeriesChartYAxisSettings,
    { id: 'default', order: 0, showLine: false, showTicks: false } as TimeSeriesChartYAxisSettings),
  xAxis: mergeDeep({} as TimeSeriesChartXAxisSettings,
    defaultTimeSeriesChartXAxisSettings,
    {showSplitLines: false} as TimeSeriesChartXAxisSettings),
  animation: mergeDeep({} as ChartAnimationSettings,
    chartAnimationDefaultSettings),
  thresholds: [],
  showLegend: true,
  legendPosition: LegendPosition.top,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.76)',
  showTooltip: true,
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
  tooltipDateInterval: true,
  tooltipDateFormat: simpleDateFormat('dd MMM yyyy HH:mm'),
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
  tooltipBackgroundBlur: 4,
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};

export const rangeChartTimeSeriesSettings = (settings: RangeChartWidgetSettings, rangeItems: RangeItem[],
                                             decimals: number, units: TbUnit): DeepPartial<TimeSeriesChartSettings> => {
  let thresholds: DeepPartial<TimeSeriesChartThreshold>[] = settings.showRangeThresholds ? getMarkPoints(rangeItems).map(item => ({
    ...{type: ValueSourceType.constant,
    yAxisId: 'default',
    units,
    decimals,
    value: item},
    ...settings.rangeThreshold
  } as DeepPartial<TimeSeriesChartThreshold>)) : [];
  if (settings.thresholds?.length) {
    thresholds = thresholds.concat(settings.thresholds);
  }
  return {
    dataZoom: settings.dataZoom,
    thresholds,
    grid: settings.grid,
    yAxes: {
      default: {
        ...settings.yAxis,
        decimals,
        units
      }
    },
    xAxis: settings.xAxis,
    animation: settings.animation,
    visualMapSettings: {
      outOfRangeColor: settings.outOfRangeColor,
      pieces: rangeItems.map(item => item.piece)
    },
    showTooltip: settings.showTooltip,
    tooltipLabelFont: settings.tooltipLabelFont,
    tooltipLabelColor: settings.tooltipLabelColor,
    tooltipValueFont: settings.tooltipValueFont,
    tooltipValueColor: settings.tooltipValueColor,
    tooltipShowDate: settings.tooltipShowDate,
    tooltipDateInterval: settings.tooltipDateInterval,
    tooltipDateFormat: settings.tooltipDateFormat,
    tooltipDateFont: settings.tooltipDateFont,
    tooltipDateColor: settings.tooltipDateColor,
    tooltipBackgroundColor: settings.tooltipBackgroundColor,
    tooltipBackgroundBlur: settings.tooltipBackgroundBlur,
  };
};

export const rangeChartTimeSeriesKeySettings = (settings: RangeChartWidgetSettings): DeepPartial<TimeSeriesChartKeySettings> => ({
    type: TimeSeriesChartSeriesType.line,
    lineSettings: {
      showLine: settings.showLine,
      step: settings.step,
      stepType: settings.stepType,
      smooth: settings.smooth,
      lineType: settings.lineType,
      lineWidth: settings.lineWidth,
      showPoints: settings.showPoints,
      showPointLabel: settings.showPointLabel,
      pointLabelPosition: settings.pointLabelPosition,
      pointLabelFont: settings.pointLabelFont,
      pointLabelColor: settings.pointLabelColor,
      enablePointLabelBackground: settings.enablePointLabelBackground,
      pointLabelBackground: settings.pointLabelBackground,
      pointShape: settings.pointShape,
      pointSize: settings.pointSize,
      fillAreaSettings: {
        type: settings.fillArea ? ChartFillType.opacity : ChartFillType.none,
        opacity: settings.fillAreaOpacity
      }
    }
  });

export const toRangeItems = (colorRanges: Array<ColorRange>, valueFormat: ValueFormatProcessor): RangeItem[] => {
  const rangeItems: RangeItem[] = [];
  let counter = 0;
  const ranges = sortedColorRange(filterIncludingColorRanges(colorRanges)).filter(r => isNumber(r.from) || isNumber(r.to));
  for (let i = 0; i < ranges.length; i++) {
    const range = ranges[i];
    let from = range.from;
    const to = range.to;
    if (i > 0) {
      const prevRange = ranges[i - 1];
      if (isNumber(prevRange.to) && isNumber(from) && from < prevRange.to) {
        from = prevRange.to;
      }
    }
    const formatToValue = isDefinedAndNotNull(to) ? Number(valueFormat.format(to)) : to;
    const formatFromValue = isDefinedAndNotNull(from) ? Number(valueFormat.format(from)) : from;
    rangeItems.push(
      {
        index: counter++,
        color: range.color,
        enabled: true,
        visible: true,
        from,
        to,
        label: rangeItemLabel(formatFromValue, formatToValue),
        piece: createTimeSeriesChartVisualMapPiece(range.color, formatFromValue, formatToValue)
      }
    );
    if (!isNumber(from) || !isNumber(to)) {
      const value = !isNumber(from) ? formatToValue : formatFromValue;
      rangeItems.push(
        {
          index: counter++,
          color: 'transparent',
          enabled: true,
          visible: false,
          label: '',
          piece: { gt: value - 0.000000001, lt: value + 0.000000001, color: 'transparent'}
        }
      );
    }
  }
  return rangeItems;
};

const rangeItemLabel = (from?: number, to?: number): string => {
  if (isNumber(from) && isNumber(to)) {
    if (from === to) {
      return `${from}`;
    } else {
      return `${from} - ${to}`;
    }
  } else if (isNumber(from)) {
    return `≥ ${from}`;
  } else if (isNumber(to)) {
    return `< ${to}`;
  } else {
    return null;
  }
};

const getMarkPoints = (ranges: Array<RangeItem>): number[] => {
  const points = new Set<number>();
  for (const range of ranges) {
    if (range.visible) {
      if (isNumber(range.from)) {
        points.add(range.from);
      }
      if (isNumber(range.to)) {
        points.add(range.to);
      }
    }
  }
  return Array.from(points).sort();
};
