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

import { latestChartDefaultSettings, LatestChartSettings } from '@home/components/widget/lib/chart/latest-chart.models';
import {
  chartAnimationDefaultSettings,
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

export enum RadarChartShape {
  polygon = 'polygon',
  circle = 'circle'
}

export const radarChartShapes = Object.keys(RadarChartShape) as RadarChartShape[];

export const radarChartShapeTranslations = new Map<RadarChartShape, string>(
  [
    [RadarChartShape.polygon, 'widgets.radar-chart.shape-polygon'],
    [RadarChartShape.circle, 'widgets.radar-chart.shape-circle']
  ]
);

export interface RadarChartSettings extends LatestChartSettings {
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

export const radarChartAnimationDefaultSettings: ChartAnimationSettings =
  mergeDeep({} as ChartAnimationSettings, chartAnimationDefaultSettings, {
    animationDuration: 1000,
    animationDurationUpdate: 500
  } as ChartAnimationSettings);

export const radarChartDefaultSettings: RadarChartSettings = {
  ...latestChartDefaultSettings,
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
