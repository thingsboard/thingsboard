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
