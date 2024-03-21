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

import {
  BackgroundSettings,
  BackgroundType,
  ColorRange,
  Font,
  simpleDateFormat
} from '@shared/models/widget-settings.models';
import { LegendPosition } from '@shared/models/widget.models';
import { EChartsTooltipWidgetSettings } from '@home/components/widget/lib/chart/echarts-widget.models';

export interface RangeChartWidgetSettings extends EChartsTooltipWidgetSettings {
  dataZoom: boolean;
  rangeColors: Array<ColorRange>;
  outOfRangeColor: string;
  fillArea: boolean;
  showLegend: boolean;
  legendPosition: LegendPosition;
  legendLabelFont: Font;
  legendLabelColor: string;
  background: BackgroundSettings;
}

export const rangeChartDefaultSettings: RangeChartWidgetSettings = {
  dataZoom: true,
  rangeColors: [
    {to: -20, color: '#234CC7'},
    {from: -20, to: 0, color: '#305AD7'},
    {from: 0, to: 10, color: '#7191EF'},
    {from: 10, to: 20, color: '#FFA600'},
    {from: 20, to: 30, color: '#F36900'},
    {from: 30, to: 40, color: '#F04022'},
    {from: 40, color: '#D81838'}
  ],
  outOfRangeColor: '#ccc',
  fillArea: true,
  showLegend: true,
  legendPosition: LegendPosition.top,
  legendLabelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  legendLabelColor: 'rgba(0, 0, 0, 0.76)',
  showTooltip: true,
  tooltipValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '16px'
  },
  tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
  tooltipShowDate: true,
  tooltipDateInterval: true,
  tooltipDateFormat: simpleDateFormat('dd MMM yyyy HH:mm'),
  tooltipDateFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  tooltipDateColor: 'rgba(0, 0, 0, 0.76)',
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
};
