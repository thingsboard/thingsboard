// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  timeSeriesChartDefaultSettings,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { BackgroundSettings, BackgroundType, Font } from '@shared/models/widget-settings.models';
import { defaultLegendConfig, LegendConfig, LegendPosition, widgetType } from '@shared/models/widget.models';
import { mergeDeep } from '@core/utils';

export interface TimeSeriesChartWidgetSettings extends TimeSeriesChartSettings {
  showLegend: boolean;
  legendColumnTitleFont: Font;
  legendColumnTitleColor: string;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  legendConfig: LegendConfig;
  background: BackgroundSettings;
  padding: string;
}

export const timeSeriesChartWidgetDefaultSettings: TimeSeriesChartWidgetSettings =
  mergeDeep({} as TimeSeriesChartWidgetSettings, timeSeriesChartDefaultSettings as TimeSeriesChartWidgetSettings, {
    showLegend: true,
    legendColumnTitleFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '16px'
    },
    legendColumnTitleColor: 'rgba(0, 0, 0, 0.38)',
    legendLabelFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '400',
      lineHeight: '16px'
    },
    legendLabelColor: 'rgba(0, 0, 0, 0.76)',
    legendValueFont: {
      family: 'Roboto',
      size: 12,
      sizeUnit: 'px',
      style: 'normal',
      weight: '500',
      lineHeight: '16px'
    },
    legendValueColor: 'rgba(0, 0, 0, 0.87)',
    legendConfig: {...defaultLegendConfig(widgetType.timeseries), position: LegendPosition.top},
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
  } as TimeSeriesChartWidgetSettings);
