// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import {
  BackgroundSettings,
  BackgroundType,
  ColorSettings,
  constantColor,
  cssUnit,
  Font
} from '@shared/models/widget-settings.models';

export interface LabelValueCardWidgetSettings {
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
  valueFont: Font;
  valueColor: ColorSettings;
  background: BackgroundSettings;
  padding: string;
}

export const labelValueCardWidgetDefaultSettings: LabelValueCardWidgetSettings = {
  autoScale: true,
  showLabel: true,
  label: 'Temperature',
  labelFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  labelColor: constantColor('rgba(0, 0, 0, 0.87)'),
  showIcon: true,
  icon: 'thermostat',
  iconSize: 24,
  iconSizeUnit: 'px',
  iconColor: constantColor('#5469FF'),
  valueFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
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
