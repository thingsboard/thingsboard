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

export enum ProgressBarLayout {
  default = 'default',
  simplified = 'simplified'
}

export const progressBarLayouts = Object.keys(ProgressBarLayout) as ProgressBarLayout[];

export const progressBarLayoutTranslations = new Map<ProgressBarLayout, string>(
  [
    [ProgressBarLayout.default, 'widgets.progress-bar.layout-default'],
    [ProgressBarLayout.simplified, 'widgets.progress-bar.layout-simplified']
  ]
);

export const progressBarLayoutImages = new Map<ProgressBarLayout, string>(
  [
    [ProgressBarLayout.default, 'assets/widget/progress-bar/default-layout.svg'],
    [ProgressBarLayout.simplified, 'assets/widget/progress-bar/simplified-layout.svg']
  ]
);

export interface ProgressBarWidgetSettings {
  layout: ProgressBarLayout;
  autoScale: boolean;
  showValue: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  showTicks: boolean;
  tickMin: number;
  tickMax: number;
  ticksFont: Font;
  ticksColor: string;
  barColor: ColorSettings;
  barBackground: string;
  background: BackgroundSettings;
  padding: string;
}

export const progressBarDefaultSettings: ProgressBarWidgetSettings = {
  layout: ProgressBarLayout.default,
  autoScale: true,
  showValue: true,
  valueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showTicks: true,
  tickMin: 0,
  tickMax: 100,
  ticksFont: {
    family: 'Roboto',
    size: 11,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  ticksColor: 'rgba(0,0,0,0.54)',
  barColor: constantColor('rgba(63, 82, 221, 1)'),
  barBackground: 'rgba(0, 0, 0, 0.04)',
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
