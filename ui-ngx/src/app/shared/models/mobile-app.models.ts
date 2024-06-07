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

import { HasTenantId } from '@shared/models/entity.models';

export interface MobileAppSettings extends HasTenantId {
  useDefaultApp: boolean;
  androidConfig: AndroidConfig;
  iosConfig: IosConfig;
  qrCodeConfig: QRCodeConfig;
  defaultGooglePlayLink: string;
  defaultAppStoreLink: string;
}

export interface AndroidConfig {
  enabled: boolean;
  appPackage: string;
  sha256CertFingerprints: string;
  storeLink: string;
}

export interface IosConfig {
  enabled: boolean;
  appId: string;
  storeLink: string;
}

export interface QRCodeConfig {
  showOnHomePage: boolean;
  badgeEnabled: boolean;
  badgePosition: BadgePosition;
  qrCodeLabelEnabled: boolean;
  qrCodeLabel: string;
}

export interface MobileOSBadgeURL {
  iOS: string;
  android: string;
}

export enum BadgePosition {
  RIGHT = 'RIGHT',
  LEFT = 'LEFT'
}

export const badgePositionTranslationsMap = new Map<BadgePosition, string>([
  [BadgePosition.RIGHT, 'admin.mobile-app.right'],
  [BadgePosition.LEFT, 'admin.mobile-app.left']
]);
