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
  ColorSettings,
  ColorType,
  constantColor,
  cssUnit,
  defaultColorFunction,
  Font
} from '@shared/models/widget-settings.models';

export enum CountCardLayout {
  column = 'column',
  row = 'row'
}

export const countCardLayouts = Object.keys(CountCardLayout) as CountCardLayout[];

export const countCardLayoutTranslations = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'widgets.count.layout-column'],
    [CountCardLayout.row, 'widgets.count.layout-row']
  ]
);

export const alarmCountCardLayoutImages = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'assets/widget/alarm-count/column-layout.svg'],
    [CountCardLayout.row, 'assets/widget/alarm-count/row-layout.svg']
  ]
);

export const entityCountCardLayoutImages = new Map<CountCardLayout, string>(
  [
    [CountCardLayout.column, 'assets/widget/entity-count/column-layout.svg'],
    [CountCardLayout.row, 'assets/widget/entity-count/row-layout.svg']
  ]
);

export interface CountWidgetSettings {
  layout: CountCardLayout;
  autoScale: boolean;
  showLabel: boolean;
  label: string;
  labelFont: Font;
  labelColor: ColorSettings;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: ColorSettings;
  showIconBackground: boolean;
  iconBackgroundSize: number;
  iconBackgroundSizeUnit: cssUnit;
  iconBackgroundColor: ColorSettings;
  valueFont: Font;
  valueColor: ColorSettings;
  showChevron: boolean;
  chevronSize: number;
  chevronSizeUnit: cssUnit;
  chevronColor: string;
}

export const countDefaultSettings = (alarmElseEntity: boolean): CountWidgetSettings => ({
  layout: CountCardLayout.column,
  autoScale: true,
  showLabel: true,
  label: alarmElseEntity ? 'Total' : 'Devices',
  labelFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '16px'
  },
  labelColor: constantColor('rgba(0, 0, 0, 0.54)'),
  showIcon: true,
  icon: alarmElseEntity ? 'warning' : 'devices',
  iconSize: 20,
  iconSizeUnit: 'px',
  iconColor: constantColor('rgba(255, 255, 255, 1)'),
  showIconBackground: true,
  iconBackgroundSize: 36,
  iconBackgroundSizeUnit: 'px',
  iconBackgroundColor: alarmElseEntity
    ? {
      color: 'rgba(0, 105, 92, 1)',
      type: ColorType.range,
      rangeList: {
        range: [
          {from: 0, to: 0, color: 'rgba(0, 105, 92, 1)'},
          {from: 1, color: 'rgba(209, 39, 48, 1)'}
        ]
      },
      colorFunction: defaultColorFunction
    }
    : constantColor('rgba(241, 141, 23, 1)'),
  valueFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '24px'
  },
  valueColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showChevron: false,
  chevronSize: 24,
  chevronSizeUnit: 'px',
  chevronColor: 'rgba(0, 0, 0, 0.38)'
});
