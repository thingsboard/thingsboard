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
import { BaseData } from '@shared/models/base-data';
import { MobileAppId } from '@shared/models/id/mobile-app-id';
import { OAuth2ClientInfo, PlatformType } from '@shared/models/oauth2.models';
import { MobileAppBundleId } from '@shared/models/id/mobile-app-bundle-id';

export interface QrCodeSettings extends HasTenantId {
  useDefaultApp: boolean;
  mobileAppBundleId: MobileAppBundleId
  androidConfig: AndroidConfig; //TODO: need remove
  iosConfig: IosConfig; //TODO: need remove
  qrCodeConfig: QRCodeConfig;
  defaultGooglePlayLink: string;
  defaultAppStoreLink: string;
  id: {
    id: string;
  }
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

export type QrCodeConfig = AndroidConfig & IosConfig;

export enum MobileAppStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  SUSPENDED = 'SUSPENDED'
}

export const mobileAppStatusTranslations = new Map<MobileAppStatus, string>(
  [
    [MobileAppStatus.DRAFT, 'mobile.status-type.draft'],
    [MobileAppStatus.PUBLISHED, 'mobile.status-type.published'],
    [MobileAppStatus.DEPRECATED, 'mobile.status-type.deprecated'],
    [MobileAppStatus.SUSPENDED, 'mobile.status-type.suspended'],
  ]
);

export interface VersionInfo {
  minVersion: string;
  minVersionReleaseNotes?: string;
  latestVersion: string;
  latestVersionReleaseNotes?: string;
}

export interface StoreInfo {
  sha256CertFingerprints?: string;
  storeLink: string;
  appId?: string;
}

export interface MobileApp extends BaseData<MobileAppId>, HasTenantId {
  pkgName: string;
  appSecret: string;
  platformType: PlatformType;
  status: MobileAppStatus;
  versionInfo: VersionInfo;
  storeInfo: StoreInfo;
}

enum MobileMenuPath {
  HOME = 'HOME',
  ASSETS = 'ASSETS',
  DEVICES = 'DEVICES',
  DEVICE_LIST = 'DEVICE_LIST',
  ALARMS = 'ALARMS',
  DASHBOARDS = 'DASHBOARDS',
  DASHBOARD = 'DASHBOARD',
  AUDIT_LOGS = 'AUDIT_LOGS',
  CUSTOMERS = 'CUSTOMERS',
  CUSTOMER = 'CUSTOMER',
  NOTIFICATION = 'NOTIFICATION',
  CUSTOM = 'CUSTOM'
}

export interface MobileMenuItem {
  label: string;
  icon: string;
  path: MobileMenuPath;
  id: string;
}

export interface MobileLayoutConfig {
  items: MobileMenuItem[];
}

export interface MobileAppBundle extends Omit<BaseData<MobileAppBundleId>, 'label'>, HasTenantId {
  title?: string;
  description?: string;
  androidAppId?: MobileAppId;
  iosAppId?: MobileAppId;
  layoutConfig?: MobileLayoutConfig;
  oauth2Enabled: boolean;
}

export interface MobileAppBundleInfo extends MobileAppBundle {
  androidPkgName: string;
  iosPkgName: string;
  oauth2ClientInfos?: Array<OAuth2ClientInfo>;
}
