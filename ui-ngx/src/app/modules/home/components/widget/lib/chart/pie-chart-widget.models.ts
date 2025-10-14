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

import {
  latestChartWidgetDefaultSettings,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { Font } from '@shared/models/widget-settings.models';
import { DeepPartial } from '@shared/models/common';
import { pieChartAnimationDefaultSettings, PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { isDefinedAndNotNull, mergeDeep } from '@core/utils';
import { ChartAnimationSettings, PieChartLabelPosition } from '@home/components/widget/lib/chart/chart.models';

export interface PieChartWidgetSettings extends LatestChartWidgetSettings {
  showLabel: boolean;
  labelPosition: PieChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  borderWidth: number;
  borderColor: string;
  radius: number;
  clockwise: boolean;
}

export const pieChartWidgetDefaultSettings: PieChartWidgetSettings = {
  ...latestChartWidgetDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    pieChartAnimationDefaultSettings),
  showLabel: true,
  labelPosition: PieChartLabelPosition.outside,
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: 'normal',
    lineHeight: '1.2'
  },
  labelColor: '#000',
  borderWidth: 0,
  borderColor: '#000',
  radius: 80,
  clockwise: false
};

export const pieChartWidgetPieChartSettings = (settings: PieChartWidgetSettings): DeepPartial<PieChartSettings> => ({
  autoScale: false,
  doughnut: false,
  clockwise: settings.clockwise,
  sortSeries: settings.sortSeries,
  showTotal: false,
  animation: settings.animation,
  showLegend: settings.showLegend,
  legendShowTotal: settings.legendShowTotal,
  showLabel: settings.showLabel,
  labelPosition: settings.labelPosition,
  labelFont: settings.labelFont,
  labelColor: settings.labelColor,
  borderWidth: settings.borderWidth,
  borderColor: settings.borderColor,
  radius: isDefinedAndNotNull(settings.radius) ? settings.radius + '%' : undefined,
  emphasisBorderWidth: settings.borderWidth,
  emphasisBorderColor: settings.borderColor,
  showTooltip: settings.showTooltip,
  tooltipValueType: settings.tooltipValueType,
  tooltipValueDecimals: settings.tooltipValueDecimals,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur
});
