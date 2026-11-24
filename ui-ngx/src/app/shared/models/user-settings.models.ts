// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
export interface UserSettings {
  openedMenuSections?: string[];
  menuCollapsed?: boolean;
  notDisplayConnectivityAfterAddDevice?: boolean;
  notDisplayInstructionsAfterAddEdge?: boolean;
  notDisplayConfigurationAfterAddMobileBundle?: boolean;
  notDisplayLicenseHandOff?: boolean;
  includeBundleWidgetsInExport?: boolean;
  includeResourcesInExportWidgetTypes?: boolean;
  includeResourcesInExportDashboard?: boolean;
  isSolutionSidebarExpanded?: boolean;
  aiTour?: AiTourSettings;
  homeCards?: HomeCardsSettings;
}

export enum AiTourStep {
  WELCOME = 'welcome',
  ASSISTANT_TOUR = 'assistantTour',
  AI_BUTTON = 'aiButton',
  HOME = 'home'
}

export type AiTourSettings = Partial<Record<AiTourStep, number>>;

export enum HomeCardId {
  IOT_HUB = 'iotHub',
  ALARMS = 'alarms',
  DASHBOARDS = 'dashboards',
  DEVICES = 'devices',
  ACTIVITY = 'activity',
  GET_STARTED = 'getStarted',
  NOTIFICATIONS = 'notifications',
  QUICK_LINKS = 'quickLinks',
  USAGE = 'usage',
  MOBILE_APP = 'mobileApp'
}

export type HomeCardsSettings = Partial<Record<HomeCardId, boolean>>;

export const initialUserSettings: UserSettings = {
  openedMenuSections: []
};

export enum UserSettingsType {
  GENERAL = 'GENERAL',
  QUICK_LINKS = 'QUICK_LINKS',
  DOC_LINKS = 'DOC_LINKS',
  DASHBOARDS = 'DASHBOARDS',
  GETTING_STARTED = 'GETTING_STARTED'
}

export interface DocumentationLink {
  icon: string;
  name: string;
  link: string;
}

export interface DocumentationLinks {
  links?: DocumentationLink[];
}

export const QUICK_WEB_LINK_PREFIX = 'url:';

export interface QuickLinks {
  links?: string[];
  webLinks?: {[id: string]: DocumentationLink};
}

export interface GettingStarted {
  maxSelectedIndex?: number;
  lastSelectedIndex?: number;
}

export interface AbstractUserDashboardInfo {
  id: string;
  title: string;
  starred: boolean;
}

export interface LastVisitedDashboardInfo extends AbstractUserDashboardInfo {
  lastVisited: number;
}

export interface StarredDashboardInfo extends AbstractUserDashboardInfo {
  starredAt: number;
}

export interface UserDashboardsInfo {
  last: Array<LastVisitedDashboardInfo>;
  starred: Array<StarredDashboardInfo>;
}

export enum UserDashboardAction {
  VISIT = 'VISIT',
  STAR = 'STAR',
  UNSTAR = 'UNSTAR'
}
