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

import { BackgroundType, ColorSettings, constantColor, Font } from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import { pieChartAnimationDefaultSettings, PieChartSettings } from '@home/components/widget/lib/chart/pie-chart.models';
import { DeepPartial } from '@shared/models/common';
import {
  LatestChartTooltipValueType,
  LatestChartWidgetSettings
} from '@home/components/widget/lib/chart/latest-chart.models';
import { mergeDeep } from '@core/utils';
import { EChartsAnimationSettings } from '@home/components/widget/lib/chart/echarts-widget.models';

export enum DoughnutLayout {
  default = 'default',
  with_total = 'with_total'
}

export const doughnutLayouts = Object.keys(DoughnutLayout) as DoughnutLayout[];

export const doughnutLayoutTranslations = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'widgets.doughnut.layout-default'],
    [DoughnutLayout.with_total, 'widgets.doughnut.layout-with-total']
  ]
);

export const doughnutLayoutImages = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'assets/widget/doughnut/default-layout.svg'],
    [DoughnutLayout.with_total, 'assets/widget/doughnut/with-total-layout.svg']
  ]
);

export const horizontalDoughnutLayoutImages = new Map<DoughnutLayout, string>(
  [
    [DoughnutLayout.default, 'assets/widget/doughnut/horizontal-default-layout.svg'],
    [DoughnutLayout.with_total, 'assets/widget/doughnut/horizontal-with-total-layout.svg']
  ]
);

export interface DoughnutWidgetSettings extends LatestChartWidgetSettings {
  layout: DoughnutLayout;
  clockwise: boolean;
  totalValueFont: Font;
  totalValueColor: ColorSettings;
}

export const doughnutDefaultSettings = (horizontal: boolean): DoughnutWidgetSettings => ({
  layout: DoughnutLayout.default,
  autoScale: true,
  clockwise: false,
  sortSeries: false,
  totalValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1'
  },
  totalValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  animation: mergeDeep({} as EChartsAnimationSettings,
    pieChartAnimationDefaultSettings),
  showLegend: true,
  legendPosition: horizontal ? LegendPosition.right : LegendPosition.bottom,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.38)',
  legendValueFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  legendValueColor: 'rgba(0, 0, 0, 0.87)',
  showTooltip: true,
  tooltipValueType: LatestChartTooltipValueType.percentage,
  tooltipValueDecimals: 0,
  tooltipValueFont: {
    family: 'Roboto',
    size: 13,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
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
});

export const doughnutPieChartSettings = (settings: DoughnutWidgetSettings): DeepPartial<PieChartSettings> => ({
  autoScale: settings.autoScale,
  doughnut: true,
  clockwise: settings.clockwise,
  sortSeries: settings.sortSeries,
  showTotal: settings.layout === DoughnutLayout.with_total,
  animation: settings.animation,
  showLegend: settings.showLegend,
  totalValueFont: settings.totalValueFont,
  totalValueColor: settings.totalValueColor,
  showLabel: false,
  borderWidth: 0,
  borderColor: '#fff',
  borderRadius: '50%',
  emphasisScale: false,
  emphasisBorderWidth: 2,
  emphasisBorderColor: '#fff',
  emphasisShadowColor: 'rgba(0, 0, 0, 0.24)',
  emphasisShadowBlur: 8,
  showTooltip: settings.showTooltip,
  tooltipValueType: settings.tooltipValueType,
  tooltipValueDecimals: settings.tooltipValueDecimals,
  tooltipValueFont: settings.tooltipValueFont,
  tooltipValueColor: settings.tooltipValueColor,
  tooltipBackgroundColor: settings.tooltipBackgroundColor,
  tooltipBackgroundBlur: settings.tooltipBackgroundBlur
});
