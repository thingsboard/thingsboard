///
/// Copyright © 2016-2024 The Thingsboard Authors
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
  AndroidConfig,
  BadgePosition,
  BadgeStyle,
  IosConfig,
  QRCodeConfig
} from '@shared/models/mobile-app.models';

export interface MobileAppQrCodeWidgetSettings {
  useSystemSettings: boolean;
  androidConfig: Pick<AndroidConfig, 'enabled'>;
  iosConfig: Pick<IosConfig, 'enabled'>;
  qrCodeConfig: Omit<QRCodeConfig, 'showOnHomePage'>;
}

export const mobileAppQrCodeWidgetDefaultSettings: MobileAppQrCodeWidgetSettings = {
  useSystemSettings: true,
  androidConfig: {
    enabled: true
  },
  iosConfig: {
    enabled: true
  },
  qrCodeConfig: {
    badgeEnabled: true,
    badgeStyle: BadgeStyle.ORIGINAL,
    badgePosition: BadgePosition.RIGHT,
    qrCodeLabelEnabled: true,
    qrCodeLabel: 'Scan to connect or download mobile app'
  }
}
