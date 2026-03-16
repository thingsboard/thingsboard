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
  cssUnit,
  DateFormatSettings,
  Font,
  lastUpdateAgoDateFormat
} from '@shared/models/widget-settings.models';

export enum ValueCardLayout {
  square = 'square',
  vertical = 'vertical',
  centered = 'centered',
  simplified = 'simplified',
  horizontal = 'horizontal',
  horizontal_reversed = 'horizontal_reversed'
}

export const valueCardLayouts = (horizontal: boolean): ValueCardLayout[] => {
  if (horizontal) {
    return [ValueCardLayout.horizontal, ValueCardLayout.horizontal_reversed];
  } else {
    return [ValueCardLayout.square, ValueCardLayout.vertical, ValueCardLayout.centered, ValueCardLayout.simplified];
  }
};

export const valueCardLayoutTranslations = new Map<ValueCardLayout, string>(
  [
    [ValueCardLayout.square, 'widgets.value-card.layout-square'],
    [ValueCardLayout.vertical, 'widgets.value-card.layout-vertical'],
    [ValueCardLayout.centered, 'widgets.value-card.layout-centered'],
    [ValueCardLayout.simplified, 'widgets.value-card.layout-simplified'],
    [ValueCardLayout.horizontal, 'widgets.value-card.layout-horizontal'],
    [ValueCardLayout.horizontal_reversed, 'widgets.value-card.layout-horizontal-reversed']
  ]
);

export const valueCardLayoutImages = new Map<ValueCardLayout, string>(
  [
    [ValueCardLayout.square, 'assets/widget/value-card/square-layout.svg'],
    [ValueCardLayout.vertical, 'assets/widget/value-card/vertical-layout.svg'],
    [ValueCardLayout.centered, 'assets/widget/value-card/centered-layout.svg'],
    [ValueCardLayout.simplified, 'assets/widget/value-card/simplified-layout.svg'],
    [ValueCardLayout.horizontal, 'assets/widget/value-card/horizontal-layout.svg'],
    [ValueCardLayout.horizontal_reversed, 'assets/widget/value-card/horizontal-reversed-layout.svg']
  ]
);

export interface ValueCardWidgetSettings {
  layout: ValueCardLayout;
  autoScale: boolean;
  showLabel: boolean;
  labelFont: Font;
  labelColor: ColorSettings;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: ColorSettings;
  valueFont: Font;
  valueColor: ColorSettings;
  showDate: boolean;
  dateFormat: DateFormatSettings;
  dateFont: Font;
  dateColor: ColorSettings;
  background: BackgroundSettings;
  padding: string;
}

export const valueCardDefaultSettings = (horizontal: boolean): ValueCardWidgetSettings => ({
  layout: horizontal ? ValueCardLayout.horizontal : ValueCardLayout.square,
  autoScale: true,
  showLabel: true,
  labelFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1.5'
  },
  labelColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showIcon: true,
  icon: 'thermostat',
  iconSize: 40,
  iconSizeUnit: 'px',
  iconColor: constantColor('#5469FF'),
  valueFont: {
    family: 'Roboto',
    size: 52,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '100%'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showDate: true,
  dateFormat: lastUpdateAgoDateFormat(),
  dateFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1.33'
  },
  dateColor: constantColor('rgba(0, 0, 0, 0.38)'),
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
});
