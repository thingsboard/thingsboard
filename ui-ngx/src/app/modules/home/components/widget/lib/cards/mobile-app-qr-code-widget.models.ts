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
