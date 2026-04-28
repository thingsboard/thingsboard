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

import { BaseData } from '@shared/models/base-data';

export interface WidgetInstalledItemDescriptor {
  type: 'WIDGET';
  widgetTypeId: { id: string };
}

export interface DashboardInstalledItemDescriptor {
  type: 'DASHBOARD';
  dashboardId: { id: string };
}

export interface CalculatedFieldInstalledItemDescriptor {
  type: 'CALCULATED_FIELD';
  calculatedFieldId: { id: string };
  entityId: { entityType: string; id: string };
}

export interface RuleChainInstalledItemDescriptor {
  type: 'RULE_CHAIN';
  ruleChainId: { id: string };
}

export interface DeviceInstalledItemDescriptor {
  type: 'DEVICE';
  createdEntityIds?: { entityType: string; id: string }[];
  dashboardId?: { id: string };
  selectedInstallMethod?: string;
  installState?: Record<string, any>;
}

export interface SolutionTemplateInstalledItemDescriptor {
  type: 'SOLUTION_TEMPLATE';
  createdEntityIds: { entityType: string; id: string }[];
  dashboardId: { id: string };
  publicId: { id: string };
  mainDashboardPublic: boolean;
  details: string;
}

export type IotHubInstalledItemDescriptor =
  | WidgetInstalledItemDescriptor
  | DashboardInstalledItemDescriptor
  | CalculatedFieldInstalledItemDescriptor
  | RuleChainInstalledItemDescriptor
  | DeviceInstalledItemDescriptor
  | SolutionTemplateInstalledItemDescriptor;

export interface InstallItemVersionResult {
  success: boolean;
  errorMessage: string;
  descriptor: IotHubInstalledItemDescriptor;
}

export interface UpdateItemVersionResult {
  success: boolean;
  entityModified: boolean;
  errorMessage: string;
  descriptor: IotHubInstalledItemDescriptor;
}

export interface ItemPublishedVersionInfo {
  itemId: string;
  publishedVersionId: string;
  publishedVersion: string;
}

export interface IotHubInstalledItem extends BaseData<{id: string}> {
  tenantId: { id: string };
  itemId: string;
  itemVersionId: string;
  itemName: string;
  itemType: string;
  version: string;
  descriptor: IotHubInstalledItemDescriptor;
}
