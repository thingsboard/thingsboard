// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BackgroundSettings, BackgroundType, cssUnit, Font } from '@shared/models/widget-settings.models';

export interface LabelCardWidgetSettings {
  autoScale: boolean;
  label: string;
  labelFont: Font;
  labelColor: string;
  showIcon: boolean;
  icon: string;
  iconSize: number;
  iconSizeUnit: cssUnit;
  iconColor: string;
  background: BackgroundSettings;
  padding: string;
}

export const labelCardWidgetDefaultSettings: LabelCardWidgetSettings = {
  autoScale: true,
  label: 'Thermostat',
  labelFont: {
    family: 'Roboto',
    size: 20,
    sizeUnit: 'px',
    style: 'normal',
    weight: '400',
    lineHeight: '24px'
  },
  labelColor: 'rgba(0, 0, 0, 0.87)',
  showIcon: true,
  icon: 'thermostat',
  iconSize: 24,
  iconSizeUnit: 'px',
  iconColor: '#5469FF',
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
