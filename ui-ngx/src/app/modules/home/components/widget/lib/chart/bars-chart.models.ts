// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { latestChartDefaultSettings, LatestChartSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  chartBarDefaultSettings,
  ChartBarSettings,
  chartColorScheme
} from '@home/components/widget/lib/chart/chart.models';
import { Font } from '@shared/models/widget-settings.models';

export interface BarsChartSettings extends LatestChartSettings {
  polar: boolean;
  axisMin?: number | string;
  axisMax?: number | string;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
  angleAxisStartAngle?: number;
  barSettings: ChartBarSettings;
}

export const barsChartAnimationDefaultSettings: ChartAnimationSettings =
  mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings, {
    animationDuration: 1000,
    animationDurationUpdate: 500
  } as ChartAnimationSettings);

export const barsChartDefaultSettings: BarsChartSettings = {
  ...latestChartDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    barsChartAnimationDefaultSettings),
  polar: false,
  axisTickLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  axisTickLabelColor: chartColorScheme['axis.tickLabel'].light,
  angleAxisStartAngle: 0,
  barSettings: mergeDeep({} as ChartBarSettings, chartBarDefaultSettings,
    {barWidth: 80, showLabel: true} as ChartBarSettings)
};
