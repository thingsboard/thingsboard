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
  ComponentStyle,
  customDateFormat,
  Font, textStyle
} from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import { EChartsTooltipWidgetSettings } from '@home/components/widget/lib/chart/echarts-widget.models';
import { DeepPartial } from '@shared/models/common';
import {
  TimeSeriesChartKeySettings, TimeSeriesChartSeriesType,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { CallbackDataParams, LabelLayoutOptionCallbackParams } from 'echarts/types/dist/shared';
import { formatValue } from '@core/utils';
import { LabelLayoutOption } from 'echarts/types/src/util/types';

export interface BarChartWithLabelsWidgetSettings extends EChartsTooltipWidgetSettings {
  showBarLabel: boolean;
  barLabelFont: Font;
  barLabelColor: string;
  showBarValue: boolean;
  barValueFont: Font;
  barValueColor: string;
  showLegend: boolean;
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const barChartWithLabelsDefaultSettings: BarChartWithLabelsWidgetSettings = {
  showBarLabel: true,
  barLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '12px'
  },
  barLabelColor: 'rgba(0, 0, 0, 0.54)',
  showBarValue: true,
  barValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '700',
    lineHeight: '12px'
  },
  barValueColor: 'rgba(0, 0, 0, 0.76)',
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
  tooltipDateFormat: customDateFormat('MMMM y'),
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

export const barChartWithLabelsTimeSeriesSettings = (settings: BarChartWithLabelsWidgetSettings): DeepPartial<TimeSeriesChartSettings> => ({
  dataZoom: false,
  yAxes: {
    default: {
      show: true,
      showLine: false,
      showTicks: false,
      showTickLabels: true,
      showSplitLines: true,
    }
  },
  xAxis: {
    show: true,
    showLine: true,
    showTicks: false,
    showTickLabels: true,
    showSplitLines: false
  },
  barWidthSettings: {
    barGap: 0,
    intervalGap: 0.5
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
  tooltipShowFocusedSeries: true
});

export const barChartWithLabelsTimeSeriesKeySettings = (settings: BarChartWithLabelsWidgetSettings,
                                                        decimals: number): DeepPartial<TimeSeriesChartKeySettings> => {
  const barValueStyle: ComponentStyle = textStyle(settings.barValueFont);
  delete barValueStyle.lineHeight;
  barValueStyle.fontSize = settings.barValueFont.size;
  barValueStyle.fill = settings.barValueColor;

  const barLabelStyle: ComponentStyle = textStyle(settings.barLabelFont);
  delete barLabelStyle.lineHeight;
  barLabelStyle.fontSize = settings.barLabelFont.size;
  barLabelStyle.fill = settings.barLabelColor;
  return {
    type: TimeSeriesChartSeriesType.bar,
    barSettings: {
      showBorder: false,
      borderWidth: 0,
      borderRadius: 0,
      showLabel: settings.showBarLabel,
      labelPosition: 'insideBottom',
      labelFormatter: (params: CallbackDataParams): string => {
        const labelParts: string[] = [];
        if (settings.showBarValue) {
          const labelValue = formatValue(params.value[1], decimals, '', false);
          labelParts.push(`{value|${labelValue}}`);
        }
        if (settings.showBarLabel) {
          labelParts.push(`{label|${params.seriesName}}`);
        }
        return labelParts.join(' ');
      },
      labelLayout: (params: LabelLayoutOptionCallbackParams): LabelLayoutOption => {
        if (params.rect.width - params.labelRect.width < 2) {
          return {
            y: '100000%',
          };
        } else {
          return {
            hideOverlap: true
          };
        }
      },
      additionalLabelOption: {
        textRotation: Math.PI / 2,
        textDistance: 15,
        textStrokeWidth: 0,
        textAlign: 'left',
        textVerticalAlign: 'middle',
        rich: {
          value: barValueStyle,
          label: barLabelStyle
        }
      }
    }
  };
};
