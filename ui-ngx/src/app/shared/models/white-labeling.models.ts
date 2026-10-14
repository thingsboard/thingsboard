// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { environment as env } from '@env/environment';
import { deepClone, isDefined, isUndefinedOrNull } from '@core/utils';
import { ColorPalette, extendDefaultPalette } from '@shared/models/material.models';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { DomainId } from '@shared/models/id/domain-id';

export interface Favicon {
  url?: string;
}

export interface Palette {
  type: string;
  extends?: string;
  colors?: ColorPalette;
}

export interface PaletteSettings {
  primaryPalette?: Palette;
  accentPalette?: Palette;
}

export interface WhiteLabelingParams {
  logoImageUrl?: string;
  logoImageHeight?: number;
  collapsedLogoImageUrl?: string;
  appTitle?: string;
  favicon?: Favicon;
  paletteSettings?: PaletteSettings;
  helpLinkBaseUrl?: string;
  uiHelpBaseUrl?: string;
  enableHelpLinks?: boolean;
  hideConnectivityDialog?: boolean;
  overrideTrendzName?: boolean;
  primaryColorPanels?: boolean;
  showNameVersion?: boolean;
  platformName?: string;
  platformVersion?: string;
  customCss?: string;
  whiteLabelingEnabled?: boolean;
}

export interface LoginWhiteLabelingParams extends WhiteLabelingParams {
  pageBackgroundColor?: string;
  darkForeground?: boolean;
  domainId?: DomainId;
  baseUrl?: string;
  adminSettingsId?: string;
  showNameBottom?: boolean;
}

export const defaultImageUrl = 'assets/logo_title_black.svg';
export const defaultCollapsedImageUrl = 'assets/small_logo_title_black.svg';

export const defaultWLParams: WhiteLabelingParams = {
  logoImageUrl: defaultImageUrl,
  logoImageHeight: 36,
  collapsedLogoImageUrl: defaultCollapsedImageUrl,
  appTitle: 'ThingsBoard',
  favicon: {
    url: 'thingsboard.ico'
  },
  paletteSettings: {
    primaryPalette: {
      type: 'tb-primary'
    },
    accentPalette: {
      type: 'tb-accent'
    }
  },
  helpLinkBaseUrl: 'https://thingsboard.io',
  enableHelpLinks: true,
  showNameVersion: false,
  platformName: 'ThingsBoard',
  platformVersion: env.tbVersion
};

const defaultLoginImageUrl = 'assets/logo_title_white.svg';

const loginWlParams = deepClone(defaultWLParams) as LoginWhiteLabelingParams;
loginWlParams.logoImageUrl = defaultLoginImageUrl;
loginWlParams.logoImageHeight = 50;
loginWlParams.pageBackgroundColor = '#eee';
loginWlParams.darkForeground = false;

export const defaultLoginWlParams = loginWlParams;

export const tbPrimaryPalette: ColorPalette = extendDefaultPalette('teal', {
  500: '#00695c'
});
export const tbAccentPalette: ColorPalette = extendDefaultPalette('deep-orange', {});

export const tbLoginPrimaryPalette: ColorPalette = extendDefaultPalette('teal', {
  200: '#00c3b6',
  500: '#00695c'
});
export const tbLoginAccentPalette: ColorPalette = extendDefaultPalette('deep-orange', {});

export const mergeDefaults = <T extends WhiteLabelingParams & LoginWhiteLabelingParams>(wlParams: T,
                              targetDefaultWlParams?: T): T => {
  if (!targetDefaultWlParams) {
    targetDefaultWlParams = defaultWLParams as T;
  }
  if (!wlParams) {
    wlParams = {} as T;
  }
  if (!wlParams.pageBackgroundColor && targetDefaultWlParams.pageBackgroundColor) {
    wlParams.pageBackgroundColor = targetDefaultWlParams.pageBackgroundColor;
  }
  if (!wlParams.collapsedLogoImageUrl && !wlParams.logoImageUrl) {
    wlParams.collapsedLogoImageUrl = targetDefaultWlParams.collapsedLogoImageUrl;
  }
  if (!wlParams.logoImageUrl) {
    wlParams.logoImageUrl = targetDefaultWlParams.logoImageUrl;
  }
  if (!wlParams.logoImageHeight) {
    wlParams.logoImageHeight = targetDefaultWlParams.logoImageHeight;
  }
  if (!wlParams.appTitle) {
    wlParams.appTitle = targetDefaultWlParams.appTitle;
  }
  if (!wlParams.favicon || !wlParams.favicon.url) {
    wlParams.favicon = targetDefaultWlParams.favicon;
  }
  if (!wlParams.paletteSettings) {
    wlParams.paletteSettings = targetDefaultWlParams.paletteSettings;
  } else {
    if (!wlParams.paletteSettings.primaryPalette || !wlParams.paletteSettings.primaryPalette.type) {
      wlParams.paletteSettings.primaryPalette = targetDefaultWlParams.paletteSettings.primaryPalette;
    }
    if (!wlParams.paletteSettings.accentPalette || !wlParams.paletteSettings.accentPalette.type) {
      wlParams.paletteSettings.accentPalette = targetDefaultWlParams.paletteSettings.accentPalette;
    }
  }
  if (!wlParams.helpLinkBaseUrl && targetDefaultWlParams.helpLinkBaseUrl) {
    wlParams.helpLinkBaseUrl = targetDefaultWlParams.helpLinkBaseUrl;
  }
  if (isUndefinedOrNull(wlParams.enableHelpLinks) && isDefined(targetDefaultWlParams.enableHelpLinks)) {
    wlParams.enableHelpLinks = targetDefaultWlParams.enableHelpLinks;
  }
  if (isUndefinedOrNull(wlParams.showNameVersion)) {
    wlParams.showNameVersion = targetDefaultWlParams.showNameVersion;
  }
  if (wlParams.platformName === null) {
    wlParams.platformName = targetDefaultWlParams.platformName;
  }
  if (wlParams.platformVersion === null) {
    wlParams.platformVersion = targetDefaultWlParams.platformVersion;
  }
  return wlParams;
};

export const checkWlParams = <T extends WhiteLabelingParams & LoginWhiteLabelingParams>(whiteLabelParams: T): T => {
  if (!whiteLabelParams) {
    whiteLabelParams = {} as T;
  }
  if (!whiteLabelParams.paletteSettings) {
    whiteLabelParams.paletteSettings = {};
  }
  if (!whiteLabelParams.favicon) {
    whiteLabelParams.favicon = {};
  }
  if (isUndefinedOrNull(whiteLabelParams.platformName)) {
    whiteLabelParams.platformName = 'ThingsBoard';
  }
  if (isUndefinedOrNull(whiteLabelParams.platformVersion)) {
    whiteLabelParams.platformVersion = env.tbVersion;
  }
  if (isUndefinedOrNull(whiteLabelParams.showNameBottom)) {
    whiteLabelParams.showNameBottom = true;
  }
  return whiteLabelParams;
};

export enum WhiteLabelingType {
  LOGIN = 'LOGIN',
  GENERAL = 'GENERAL',
  MAIL_TEMPLATES = 'MAIL_TEMPLATES'
}

export interface WhiteLabeling {
  tenantId: TenantId;
  customerId?: CustomerId;
  type: WhiteLabelingType;
  settings?: any;
  domain?: string;
}

export const pageByWhiteLabelingType = new Map<WhiteLabelingType, string>([
    [WhiteLabelingType.GENERAL, '/white-labeling/whiteLabel'],
    [WhiteLabelingType.LOGIN, '/white-labeling/loginWhiteLabel']
]);
