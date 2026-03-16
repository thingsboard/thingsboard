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
