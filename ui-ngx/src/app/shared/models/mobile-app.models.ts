///
/// Copyright © 2016-2025 The Thingsboard Authors
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
import { deepClone, isNotEmptyStr } from '@core/utils';

export const WEB_URL_REGEX = /^(https?:\/\/)?(localhost|([\p{L}\p{M}\w-]+\.)+[\p{L}\p{M}\w-]+)(:\d+)?(\/[\w\-._~:/?#[\]@!$&'()*+,;=%\p{L}\p{N}]*)?$/u;

export interface QrCodeSettings extends HasTenantId {
  useDefaultApp: boolean;
  mobileAppBundleId: MobileAppBundleId
  androidEnabled: boolean;
  iosEnabled: boolean;
  qrCodeConfig: QRCodeConfig;
  readonly googlePlayLink: string;
  readonly appStoreLink: string;
  id: {
    id: string;
  }
}

export interface QRCodeConfig {
  showOnHomePage: boolean;
  badgeEnabled: boolean;
  badgePosition: BadgePosition;
  qrCodeLabelEnabled: boolean;
  qrCodeLabel: string;
}

export enum BadgePosition {
  RIGHT = 'RIGHT',
  LEFT = 'LEFT'
}

export const badgePositionTranslationsMap = new Map<BadgePosition, string>([
  [BadgePosition.RIGHT, 'admin.mobile-app.right'],
  [BadgePosition.LEFT, 'admin.mobile-app.left']
]);

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
  title?: string;
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
  NOTIFICATIONS = 'NOTIFICATIONS'
}

export enum MobilePageType {
  DEFAULT = 'DEFAULT',
  CUSTOM = 'CUSTOM',
  DASHBOARD = 'DASHBOARD',
  WEB_VIEW = 'WEB_VIEW',
}

export const mobilePageTypeTranslations = new Map<MobilePageType, string>(
  [
    [MobilePageType.CUSTOM, 'mobile.pages-types.custom'],
    [MobilePageType.DASHBOARD, 'mobile.pages-types.dashboard'],
    [MobilePageType.WEB_VIEW, 'mobile.pages-types.web-view'],
  ]
);

export interface MobilePage {
  label?: string;
  icon?: string;
  type: MobilePageType;
  visible: boolean;
}

export interface DefaultMobilePage extends MobilePage {
  id: MobileMenuPath;
}

export interface CustomMobilePage extends MobilePage {
  dashboardId?: string;
  url?: string;
  path?: string;
}

export interface MobileLayoutConfig {
  pages: MobilePage[];
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
  androidPkg?: {
    name: string;
    id: MobileAppId
  };
  iosPkg?: {
    name: string;
    id: MobileAppId
  }
  oauth2ClientInfos?: Array<OAuth2ClientInfo>;
  qrCodeEnabled: boolean;
}

const defaultMobileMenu = [
  MobileMenuPath.HOME,
  MobileMenuPath.ALARMS,
  MobileMenuPath.DEVICES,
  MobileMenuPath.CUSTOMERS,
  MobileMenuPath.ASSETS,
  MobileMenuPath.AUDIT_LOGS,
  MobileMenuPath.NOTIFICATIONS,
  MobileMenuPath.DEVICE_LIST,
  MobileMenuPath.DASHBOARDS
];

export const hideDefaultMenuItems = [
  MobileMenuPath.DEVICE_LIST,
  MobileMenuPath.DASHBOARDS
];

export const getDefaultMobileMenuItem = (): DefaultMobilePage[] => {
  return deepClone(defaultMobileMenu).map(item => ({
    visible: !hideDefaultMenuItems.includes(item),
    type: MobilePageType.DEFAULT,
    id: item
  }))
}

export const isDefaultMobileMenuItem = (item: MobilePage): item is DefaultMobilePage => {
  const path = (item as DefaultMobilePage).id;
  return isNotEmptyStr(path) && defaultMobilePageMap.has(path);
};


const mobilePageEqualToDefault = (item: MobilePage, defaultMobilePage: DefaultMobilePage): boolean => {
  if (isDefaultMobileMenuItem(item) && (hideDefaultMenuItems.includes(item.id) ? !item.visible : item.visible)) {
    return !(item.id !== defaultMobilePage.id || !!item.label || !!item.icon);
  } else {
    return false;
  }
};

export const isDefaultMobilePagesConfig = (items: MobilePage[]): boolean => {
  const defaultMenus = getDefaultMobileMenuItem();
  if (!items?.length && !defaultMenus?.length) {
    return true;
  } else if (items.length !== defaultMenus.length) {
    return false;
  } else {
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      const defaultMenuItem = defaultMenus[i];
      if (!mobilePageEqualToDefault(item, defaultMenuItem)) {
        return false;
      }
    }
    return true;
  }
};

export const mobileMenuDividers = new Map<number, string>([
  [2, 'mobile.mobile-599'],
  [4, 'mobile.tablet-959'],
  [6, 'mobile.max-element-number'],
]);

export const defaultMobilePageMap = new Map<MobileMenuPath, Omit<DefaultMobilePage, 'type' | 'visible'>>([
  [
    MobileMenuPath.HOME,
    {
      id: MobileMenuPath.HOME,
      icon: 'home',
      label: 'Home'
    }
  ],
  [
    MobileMenuPath.ALARMS,
    {
      id: MobileMenuPath.ALARMS,
      icon: 'notifications',
      label: 'Alarms'
    }
  ],
  [
    MobileMenuPath.DEVICES,
    {
      id: MobileMenuPath.DEVICES,
      icon: 'devices_other',
      label: 'Devices'
    }
  ],
  [
    MobileMenuPath.CUSTOMERS,
    {
      id: MobileMenuPath.CUSTOMERS,
      icon: 'supervisor_account',
      label: 'Customers'
    }
  ],
  [
    MobileMenuPath.ASSETS,
    {
      id: MobileMenuPath.ASSETS,
      icon: 'domain',
      label: 'Assets'
    }
  ],
  [
    MobileMenuPath.DEVICE_LIST,
    {
      id: MobileMenuPath.DEVICE_LIST,
      icon: 'devices',
      label: 'Device list'
    }
  ],
  [
    MobileMenuPath.DASHBOARDS,
    {
      id: MobileMenuPath.DASHBOARDS,
      icon: 'dashboard',
      label: 'Dashboards'
    }
  ],
  [
    MobileMenuPath.AUDIT_LOGS,
    {
      id: MobileMenuPath.AUDIT_LOGS,
      icon: 'track_changes',
      label: 'Audit logs'
    }
  ],
  [
    MobileMenuPath.NOTIFICATIONS,
    {
      id: MobileMenuPath.NOTIFICATIONS,
      icon: 'notifications_active',
      label: 'Notification'
    }
  ]
])
