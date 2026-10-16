// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BackgroundSettings, BackgroundType, Font } from '@shared/models/widget-settings.models';

export interface UnreadNotificationWidgetSettings {
  maxNotificationDisplay: number;
  showCounter: boolean;
  counterValueFont: Font;
  counterValueColor: string;
  counterColor: string;

  enableViewAll: boolean;
  enableFilter: boolean;
  enableMarkAsRead: boolean;
  background: BackgroundSettings;
  padding: string;
}

export const unreadNotificationDefaultSettings: UnreadNotificationWidgetSettings = {
  maxNotificationDisplay: 6,
  showCounter: true,
  counterValueFont: {
    family: 'Roboto',
    size: 12,
    sizeUnit: 'px',
    style: 'normal',
    weight: '600',
    lineHeight: ''
  },
  counterValueColor: '#fff',
  counterColor: '#305680',
  enableViewAll: true,
  enableFilter: true,
  enableMarkAsRead: true,
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
