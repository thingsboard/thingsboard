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
