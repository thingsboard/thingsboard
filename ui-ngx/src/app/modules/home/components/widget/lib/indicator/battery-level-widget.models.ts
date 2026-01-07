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
  ColorType,
  constantColor,
  defaultColorFunction,
  Font
} from '@shared/models/widget-settings.models';

export enum BatteryLevelLayout {
  vertical_solid = 'vertical_solid',
  horizontal_solid = 'horizontal_solid',
  vertical_divided = 'vertical_divided',
  horizontal_divided = 'horizontal_divided'
}

export const batteryLevelLayouts = Object.keys(BatteryLevelLayout) as BatteryLevelLayout[];

export const batteryLevelLayoutTranslations = new Map<BatteryLevelLayout, string>(
  [
    [BatteryLevelLayout.vertical_solid, 'widgets.battery-level.layout-vertical-solid'],
    [BatteryLevelLayout.horizontal_solid, 'widgets.battery-level.layout-horizontal-solid'],
    [BatteryLevelLayout.vertical_divided, 'widgets.battery-level.layout-vertical-divided'],
    [BatteryLevelLayout.horizontal_divided, 'widgets.battery-level.layout-horizontal-divided']
  ]
);

export const batteryLevelLayoutImages = new Map<BatteryLevelLayout, string>(
  [
    [BatteryLevelLayout.vertical_solid, 'assets/widget/battery-level/vertical-solid-layout.svg'],
    [BatteryLevelLayout.horizontal_solid, 'assets/widget/battery-level/horizontal-solid-layout.svg'],
    [BatteryLevelLayout.vertical_divided, 'assets/widget/battery-level/vertical-divided-layout.svg'],
    [BatteryLevelLayout.horizontal_divided, 'assets/widget/battery-level/horizontal-divided-layout.svg']
  ]
);

export interface BatteryLevelWidgetSettings {
  layout: BatteryLevelLayout;
  sectionsCount: number;
  showValue: boolean;
  autoScaleValueSize: boolean;
  valueFont: Font;
  valueColor: ColorSettings;
  batteryLevelColor: ColorSettings;
  batteryShapeColor: ColorSettings;
  background: BackgroundSettings;
  padding: string;
}

export const batteryLevelDefaultSettings: BatteryLevelWidgetSettings = {
  layout: BatteryLevelLayout.vertical_solid,
  sectionsCount: 4,
  showValue: true,
  autoScaleValueSize: true,
  valueFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  batteryLevelColor: {
    color: 'rgba(224, 224, 224, 1)',
    type: ColorType.range,
    rangeList: {
      range: [
        {from: null, to: 25, color: 'rgba(227, 71, 71, 1)'},
        {from: 25, to: 50, color: 'rgba(246, 206, 67, 1)'},
        {from: 50, to: null, color: 'rgba(92, 223, 144, 1)'}
      ]
    },
    colorFunction: defaultColorFunction
  },
  batteryShapeColor: {
    color: 'rgba(224, 224, 224, 0.32)',
    type: ColorType.range,
    rangeList: {
      range: [
        {from: null, to: 25, color: 'rgba(227, 71, 71, 0.32)'},
        {from: 25, to: 50, color: 'rgba(246, 206, 67, 0.32)'},
        {from: 50, to: null, color: 'rgba(92, 223, 144, 0.32)'}
      ]
    },
    colorFunction: defaultColorFunction
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
  padding: '12px'
};

