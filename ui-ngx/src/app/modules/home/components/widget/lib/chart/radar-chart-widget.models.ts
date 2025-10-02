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
import {
  ChartAnimationSettings,
  chartColorScheme,
  ChartFillSettings,
  ChartFillType,
  ChartLabelPosition,
  ChartLineType,
  ChartShape
} from '@home/components/widget/lib/chart/chart.models';
import { Font } from '@shared/models/widget-settings.models';
import { mergeDeep } from '@core/utils';
import {
  radarChartAnimationDefaultSettings,
  RadarChartSettings,
  RadarChartShape
} from '@home/components/widget/lib/chart/radar-chart.models';
import { DeepPartial } from '@shared/models/common';

export interface RadarChartWidgetSettings extends LatestChartWidgetSettings {
  shape: RadarChartShape;
  color: string;
  showLine: boolean;
  lineType: ChartLineType;
  lineWidth: number;
  showPoints: boolean;
  pointShape: ChartShape;
  pointSize: number;
  showLabel: boolean;
  labelPosition: ChartLabelPosition;
  labelFont: Font;
  labelColor: string;
  fillAreaSettings: ChartFillSettings;
  normalizeAxes: boolean;
  axisShowLabel: boolean;
  axisLabelFont: Font;
  axisShowTickLabels: boolean;
  axisTickLabelFont: Font;
  axisTickLabelColor: string;
}

export const radarChartWidgetDefaultSettings: RadarChartWidgetSettings = {
  ...latestChartWidgetDefaultSettings,
  animation: mergeDeep({} as ChartAnimationSettings,
    radarChartAnimationDefaultSettings),
  shape: RadarChartShape.polygon,
  color: '#3F52DD',
  showLine: true,
  lineType: ChartLineType.solid,
  lineWidth: 2,
  showPoints: true,
  pointShape: ChartShape.circle,
  pointSize: 4,
  showLabel: false,
  labelPosition: ChartLabelPosition.top,
  labelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  labelColor: chartColorScheme['series.label'].light,
  fillAreaSettings: {
    type: ChartFillType.none,
    opacity: 0.4,
    gradient: {
      start: 80,
      end: 20
    }
  },
  normalizeAxes: false,
  axisShowLabel: true,
  axisLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '600',
    lineHeight: '1'
  },
  axisShowTickLabels: false,
  axisTickLabelFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '1'
  },
  axisTickLabelColor: chartColorScheme['axis.tickLabel'].light
};

export const radarChartWidgetRadarChartSettings = (settings: RadarChartWidgetSettings): DeepPartial<RadarChartSettings> => ({
  shape: settings.shape,
  color: settings.color,
  showLine: settings.showLine,
  lineType: settings.lineType,
  lineWidth: settings.lineWidth,
  showPoints: settings.showPoints,
  pointShape: settings.pointShape,
  pointSize: settings.pointSize,
  showLabel: settings.showLabel,
  labelPosition: settings.labelPosition,
  labelFont: settings.labelFont,
  labelColor: settings.labelColor,
  fillAreaSettings: settings.fillAreaSettings,
  normalizeAxes: settings.normalizeAxes,
  axisShowLabel: settings.axisShowLabel,
  axisLabelFont: settings.axisLabelFont,
  axisShowTickLabels: settings.axisShowTickLabels,
  axisTickLabelFont: settings.axisTickLabelFont,
  axisTickLabelColor: settings.axisTickLabelColor,
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
