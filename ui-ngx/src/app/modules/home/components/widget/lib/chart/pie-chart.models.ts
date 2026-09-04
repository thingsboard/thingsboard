// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { ColorSettings, constantColor, Font } from '@shared/models/widget-settings.models';
import { latestChartDefaultSettings, LatestChartSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import {
  chartAnimationDefaultSettings,
  ChartAnimationSettings,
  PieChartLabelPosition
} from '@home/components/widget/lib/chart/chart.models';

export interface PieChartSettings extends LatestChartSettings {
  doughnut: boolean;
  radius: string;
  clockwise: boolean;
  totalValueFont: Font;
  totalValueColor: ColorSettings;
  showLabel: boolean;
  labelPosition: PieChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  borderWidth: number;
  borderColor: string;
  borderRadius: string;
  emphasisScale: boolean;
  emphasisBorderWidth: number;
  emphasisBorderColor: string;
  emphasisShadowBlur: number;
  emphasisShadowColor: string;
}

export const pieChartAnimationDefaultSettings: ChartAnimationSettings =
  mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings, {
    animationDuration: 1000,
    animationDurationUpdate: 500
  } as ChartAnimationSettings);

export const pieChartDefaultSettings: PieChartSettings = {
  ...latestChartDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    pieChartAnimationDefaultSettings),
  doughnut: false,
  radius: '80%',
  clockwise: false,
  totalValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  totalValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showLabel: false,
  labelPosition: PieChartLabelPosition.outside,
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: 'normal',
    lineHeight: '1'
  },
  labelColor: '#000',
  borderWidth: 0,
  borderColor: '#000',
  borderRadius: '0%',
  emphasisScale: true,
  emphasisBorderWidth: 0,
  emphasisBorderColor: '#000',
  emphasisShadowBlur: 10,
  emphasisShadowColor: 'rgba(0, 0, 0, 0.5)'
};
