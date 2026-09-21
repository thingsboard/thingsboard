// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import {
  timeSeriesChartDefaultSettings,
  TimeSeriesChartSettings
} from '@home/components/widget/lib/chart/time-series-chart.models';
import { BackgroundSettings, BackgroundType, Font } from '@shared/models/widget-settings.models';
import { defaultLegendConfig, LegendConfig, LegendDirection, LegendPosition, widgetType } from '@shared/models/widget.models';
import { mergeDeep } from '@core/utils';

export enum LegendView {
  table = 'table',
  card = 'card'
}

export const legendViews = Object.keys(LegendView) as LegendView[];

export const legendViewTranslationMap = new Map<LegendView, string>(
  [
    [LegendView.table, 'legend.view-table'],
    [LegendView.card, 'legend.view-card']
  ]
);

export interface LegendValueType {
  labelKey: string;
  type: 'min' | 'max' | 'avg' | 'total' | 'latest';
  configKey: 'showMin' | 'showMax' | 'showAvg' | 'showTotal' | 'showLatest';
}

export const legendValueTypes: LegendValueType[] = [
  {labelKey: 'legend.Min', type: 'min', configKey: 'showMin'},
  {labelKey: 'legend.Max', type: 'max', configKey: 'showMax'},
  {labelKey: 'legend.Avg', type: 'avg', configKey: 'showAvg'},
  {labelKey: 'legend.Total', type: 'total', configKey: 'showTotal'},
  {labelKey: 'legend.Latest', type: 'latest', configKey: 'showLatest'}
];

export const legendItemsDirectionApplicable = (legendView: LegendView): boolean => legendView !== LegendView.card;

export interface TimeSeriesChartWidgetSettings extends TimeSeriesChartSettings {
  showLegend: boolean;
  legendColumnTitleFont: Font;
  legendColumnTitleColor: string;
  legendLabelFont: Font;
  legendLabelColor: string;
  legendValueFont: Font;
  legendValueColor: string;
  legendConfig: LegendConfig;
  legendItemsDirection?: LegendDirection;
  legendView?: LegendView;
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
    legendView: LegendView.table,
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
