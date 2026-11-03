// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  latestChartWidgetDefaultSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { Font } from '@shared/models/widget-settings.models';
import {
  ChartAnimationSettings,
  chartBarDefaultSettings,
  ChartBarSettings,
  chartColorScheme,
  PieChartLabelPosition
} from '@home/components/widget/lib/chart/chart.models';
import { mergeDeep } from '@core/utils';
import {
  barsChartAnimationDefaultSettings,
  BarsChartSettings
} from '@home/components/widget/lib/chart/bars-chart.models';
import { DeepPartial } from '@shared/models/common';

export interface PolarAreaChartWidgetSettings extends LatestChartWidgetSettings {
  axisMin?: number;
  axisMax?: number;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
  angleAxisStartAngle?: number;
  barSettings: ChartBarSettings;
}

export const polarAreaChartWidgetDefaultSettings: PolarAreaChartWidgetSettings = {
  ...latestChartWidgetDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    barsChartAnimationDefaultSettings),
  axisTickLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  axisTickLabelColor: chartColorScheme['axis.tickLabel'].light,
  angleAxisStartAngle: 90,
  barSettings: mergeDeep({} as ChartBarSettings, chartBarDefaultSettings,
    {barWidth: 100, showLabel: true, labelPosition: PieChartLabelPosition.outside} as ChartBarSettings)
};

export const polarAreaChartWidgetBarsChartSettings = (settings: PolarAreaChartWidgetSettings): DeepPartial<BarsChartSettings> => ({
  polar: true,
  axisMin: settings.axisMin,
  axisMax: settings.axisMax,
  axisTickLabelFont: settings.axisTickLabelFont,
  axisTickLabelColor: settings.axisTickLabelColor,
  angleAxisStartAngle: settings.angleAxisStartAngle,
  barSettings: settings.barSettings,
  sortSeries: settings.sortSeries,
  showTotal: false,
  animation: settings.animation,
  showLegend: settings.showLegend,
  legendShowTotal: settings.legendShowTotal,
  showTooltip: settings.showTooltip,
  tooltipValueType: settings.tooltipValueType,
  tooltipValueDecimals: settings.tooltipValueDecimals,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur
});
