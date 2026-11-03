// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface UserSettings {
  openedMenuSections?: string[];
  notDisplayConnectivityAfterAddDevice?: boolean;
  notDisplayInstructionsAfterAddEdge?: boolean;
  notDisplayConfigurationAfterAddMobileBundle?: boolean;
  includeBundleWidgetsInExport?: boolean;
  includeResourcesInExportWidgetTypes?: boolean;
  includeResourcesInExportDashboard?: boolean;
}

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

export interface QuickLinks {
  links?: string[];
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
