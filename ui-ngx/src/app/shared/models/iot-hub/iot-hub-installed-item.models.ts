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
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';

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

export interface AlarmRuleInstalledItemDescriptor {
  type: 'ALARM_RULE';
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
  | AlarmRuleInstalledItemDescriptor
  | RuleChainInstalledItemDescriptor
  | DeviceInstalledItemDescriptor
  | SolutionTemplateInstalledItemDescriptor;

export interface InstallItemVersionResult {
  success: boolean;
  errorMessage: string;
  descriptor: IotHubInstalledItemDescriptor;
}

export enum InstallPlanEntryStatus {
  WILL_INSTALL = 'WILL_INSTALL',
  ALREADY_INSTALLED = 'ALREADY_INSTALLED',
  MISSING = 'MISSING'
}

export interface InstallPlanEntry {
  itemId: string;
  versionId: string;
  name: string;
  type: string;
  version: string;
  status: InstallPlanEntryStatus;
  root: boolean;
  errorMessage?: string;
}

export interface InstallPlan {
  rootVersionId: string;
  entries: InstallPlanEntry[];
}

export interface InstallPlanResult {
  success: boolean;
  rolledBack: boolean;
  errorMessage?: string;
  rootDescriptor?: IotHubInstalledItemDescriptor;
  entries: InstallPlanEntry[];
  missingItemIds: string[];
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

export const getInstalledItemUrl = (descriptor?: IotHubInstalledItemDescriptor): string | null => {
  if (!descriptor) {
    return null;
  }
  let entityId: string | null = null;
  let entityType: EntityType | null = null;
  switch (descriptor.type) {
    case 'DEVICE':
      if (descriptor.dashboardId) {
        entityId = descriptor.dashboardId?.id;
        entityType = EntityType.DASHBOARD;
      } else if (descriptor.createdEntityIds) {
        const found = descriptor.createdEntityIds.find(id => id.entityType === EntityType.DEVICE);
        if (found) {
          entityId = found.id;
          entityType = EntityType.DEVICE;
        }
      }
      break;
    case 'WIDGET':
      entityId = descriptor.widgetTypeId?.id;
      entityType = EntityType.WIDGET_TYPE;
      break;
    case 'DASHBOARD':
      entityId = descriptor.dashboardId?.id;
      entityType = EntityType.DASHBOARD;
      break;
    case 'CALCULATED_FIELD':
    case 'ALARM_RULE':
      entityId = descriptor.calculatedFieldId?.id;
      entityType = EntityType.CALCULATED_FIELD;
      break;
    case 'RULE_CHAIN':
      entityId = descriptor.ruleChainId?.id;
      entityType = EntityType.RULE_CHAIN;
      break;
    case 'SOLUTION_TEMPLATE':
      entityId = descriptor.dashboardId?.id;
      entityType = EntityType.DASHBOARD;
      break;
  }
  if (entityType && entityId) {
    let url: string | null;
    if (descriptor.type === 'ALARM_RULE') {
      url = `/alarms/alarm-rules/${entityId}`;
    } else {
      url = getEntityDetailsPageURL(entityId, entityType);
    }
    return url;
  }
  return null;
}
