// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BadgePosition, QRCodeConfig } from '@shared/models/mobile-app.models';
import { BackgroundType } from '@shared/models/widget-settings.models';
import { WidgetConfig } from '@shared/models/widget.models';

export interface MobileAppQrCodeWidgetSettings extends WidgetConfig {
  useSystemSettings: boolean;
  qrCodeConfig: Omit<QRCodeConfig, 'showOnHomePage'>;
}

export const mobileAppQrCodeWidgetDefaultSettings: MobileAppQrCodeWidgetSettings = {
  useSystemSettings: true,
  qrCodeConfig: {
    badgeEnabled: true,
    badgePosition: BadgePosition.RIGHT,
    qrCodeLabelEnabled: true,
    qrCodeLabel: 'Scan to connect or download mobile app'
  },
  title: 'Download mobile app',
  titleFont: {
    family: 'Roboto',
    size: 16,
    sizeUnit: 'px',
    style: 'normal',
    weight: '500',
    lineHeight: '1.5'
  },
  showTitleIcon: false,
  iconSize: '40',
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
}
