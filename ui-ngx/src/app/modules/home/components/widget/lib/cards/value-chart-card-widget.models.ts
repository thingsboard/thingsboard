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

import {
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  constantColor,
  Font
} from '@shared/models/widget-settings.models';

export enum ValueChartCardLayout {
  left = 'left',
  right = 'right'
}

export const valueCartCardLayouts = Object.keys(ValueChartCardLayout) as ValueChartCardLayout[];

export const valueChartCardLayoutTranslations = new Map<ValueChartCardLayout, string>(
  [
    [ValueChartCardLayout.left, 'widgets.value-chart-card.layout-left'],
    [ValueChartCardLayout.right, 'widgets.value-chart-card.layout-right']
  ]
);

export const valueChartCardLayoutImages = new Map<ValueChartCardLayout, string>(
  [
    [ValueChartCardLayout.left, 'assets/widget/value-chart-card/left-layout.svg'],
    [ValueChartCardLayout.right, 'assets/widget/value-chart-card/right-layout.svg']
  ]
);

export interface ValueChartCardWidgetSettings {
  layout: ValueChartCardLayout;
  autoScale: boolean;
  showValue: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  background: BackgroundSettings;
  padding: string;
}

export const valueChartCardDefaultSettings: ValueChartCardWidgetSettings = {
  layout: ValueChartCardLayout.left,
  autoScale: true,
  showValue: true,
  valueFont: {
    family: 'Roboto',
    size: 28,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: '12px'
};
