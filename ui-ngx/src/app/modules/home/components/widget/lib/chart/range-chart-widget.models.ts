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
  BackgroundSettings,
  BackgroundType,
  ColorRange,
  filterIncludingColorRanges,
  Font,
  simpleDateFormat,
  sortedColorRange
} from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import { EChartsTooltipWidgetSettings } from '@home/components/widget/lib/chart/echarts-widget.models';
import {
  createTimeSeriesChartVisualMapPiece,
  SeriesFillType,
  TimeSeriesChartKeySettings,
  TimeSeriesChartSeriesType,
  TimeSeriesChartSettings,
  TimeSeriesChartShape,
  TimeSeriesChartThreshold,
  TimeSeriesChartThresholdType,
  TimeSeriesChartVisualMapPiece
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { isNumber } from '@core/utils';
import { DeepPartial } from '@shared/models/common';

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

export interface RangeChartWidgetSettings extends EChartsTooltipWidgetSettings {
  dataZoom: boolean;
  rangeColors: Array<ColorRange>;
  outOfRangeColor: string;
  fillArea: boolean;
  showLegend: boolean;
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  background: BackgroundSettings;
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
  fillArea: true,
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
  }
};

export const rangeChartTimeSeriesSettings = (settings: RangeChartWidgetSettings, rangeItems: RangeItem[],
                                             decimals: number, units: string): DeepPartial<TimeSeriesChartSettings> => {
  const thresholds: DeepPartial<TimeSeriesChartThreshold>[] = getMarkPoints(rangeItems).map(item => ({
    type: TimeSeriesChartThresholdType.constant,
    yAxisId: 'default',
    units,
    decimals,
    lineWidth: 1,
    lineColor: '#37383b',
    lineType: [3, 3],
    startSymbol: TimeSeriesChartShape.circle,
    startSymbolSize: 5,
    endSymbol: TimeSeriesChartShape.arrow,
    endSymbolSize: 7,
    showLabel: true,
    labelPosition: 'insideEndTop',
    labelColor: '#37383b',
    additionalLabelOption: {
      backgroundColor: 'rgba(255,255,255,0.56)',
      padding: [4, 5],
      borderRadius: 4,
    },
    value: item
  } as DeepPartial<TimeSeriesChartThreshold>));
  return {
    dataZoom: settings.dataZoom,
    thresholds,
    yAxes: {
      default: {
        show: true,
        showLine: false,
        showTicks: false,
        showTickLabels: true,
        showSplitLines: true,
        decimals,
        units
      }
    },
    xAxis: {
      show: true,
      showLine: true,
      showTicks: true,
      showTickLabels: true,
      showSplitLines: false
    },
    visualMapSettings: {
      outOfRangeColor: settings.outOfRangeColor,
      pieces: rangeItems.map(item => item.piece)
    },
    showTooltip: settings.showTooltip,
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
      showLine: true,
      smooth: false,
      showPoints: false,
      fillAreaSettings: {
        type: settings.fillArea ? SeriesFillType.opacity : SeriesFillType.none,
        opacity: 0.7
      }
    }
  });

export const toRangeItems = (colorRanges: Array<ColorRange>): RangeItem[] => {
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
    rangeItems.push(
      {
        index: counter++,
        color: range.color,
        enabled: true,
        visible: true,
        from,
        to,
        label: rangeItemLabel(from, to),
        piece: createTimeSeriesChartVisualMapPiece(range.color, from, to)
      }
    );
    if (!isNumber(from) || !isNumber(to)) {
      const value = !isNumber(from) ? to : from;
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
