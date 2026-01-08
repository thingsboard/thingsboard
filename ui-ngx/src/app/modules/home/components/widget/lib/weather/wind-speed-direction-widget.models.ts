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

export enum WindSpeedDirectionLayout {
  default = 'default',
  advanced = 'advanced',
  simplified = 'simplified'
}

export const windSpeedDirectionLayouts = Object.keys(WindSpeedDirectionLayout) as WindSpeedDirectionLayout[];

export const windSpeedDirectionLayoutTranslations = new Map<WindSpeedDirectionLayout, string>(
  [
    [WindSpeedDirectionLayout.default, 'widgets.wind-speed-direction.layout-default'],
    [WindSpeedDirectionLayout.advanced, 'widgets.wind-speed-direction.layout-advanced'],
    [WindSpeedDirectionLayout.simplified, 'widgets.wind-speed-direction.layout-simplified']
  ]
);

export const windSpeedDirectionLayoutImages = new Map<WindSpeedDirectionLayout, string>(
  [
    [WindSpeedDirectionLayout.default, 'assets/widget/wind-speed-direction/default-layout.svg'],
    [WindSpeedDirectionLayout.advanced, 'assets/widget/wind-speed-direction/advanced-layout.svg'],
    [WindSpeedDirectionLayout.simplified, 'assets/widget/wind-speed-direction/simplified-layout.svg']
  ]
);

export interface WindSpeedDirectionWidgetSettings {
  layout: WindSpeedDirectionLayout;
  centerValueFont: Font;
  centerValueColor: ColorSettings;
  ticksColor: string;
  arrowColor: string;
  directionalNamesElseDegrees: boolean;
  majorTicksColor: string;
  majorTicksFont: Font;
  minorTicksColor: string;
  minorTicksFont: Font;
  background: BackgroundSettings;
  padding: string
}

export const windSpeedDirectionDefaultSettings: WindSpeedDirectionWidgetSettings = {
  layout: WindSpeedDirectionLayout.default,
  centerValueFont: {
    family: 'Roboto',
    size: 24,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '32px'
  },
  centerValueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  ticksColor: 'rgba(0, 0, 0, 0.12)',
  arrowColor: 'rgba(0, 0, 0, 0.87)',
  directionalNamesElseDegrees: true,
  majorTicksColor: 'rgba(158, 158, 158, 1)',
  majorTicksFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  minorTicksColor: 'rgba(0, 0, 0, 0.12)',
  minorTicksFont: {
    family: 'Roboto',
    size: 14,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '20px'
  },
  background: {
    type: BackgroundType.color,
    color: '#fff',
    overlay: {
      enabled: false,
      color: 'rgba(255,255,255,0.72)',
      blur: 3
    }
  },
  padding: ''
};
