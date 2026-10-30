// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { BaseData } from '@shared/models/base-data';
import { EntityType } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL } from '@core/utils';

export interface WidgetInstalledItemDescriptor {
  type: 'WIDGET';
  widgetTypeId: { id: string };
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

/** An entity of an installed device package as the server finds it before an update. */
export interface DevicePackageEntityState {
  stepName: string;
  entityId: { entityType: string; id: string };
  exists: boolean;
  version?: number;
  // Changed since the package installed or last updated it; null when the server has no checksum of it
  modified?: boolean | null;
  shared: boolean;
}

/** A step of a device package update, with its entity resolved from the template of the new version. */
export interface DeviceUpdateStep {
  name: string;
  type: string;
  action: 'KEEP' | 'OVERWRITE' | 'CREATE';
  entityId?: string;
  expectedVersion?: number;
  confirmed?: boolean;
  entity?: any;
  credentials?: any;
  // OVERWRITE and CREATE: the attributes the saved entity gets, as at install
  serverAttributes?: any;
  sharedAttributes?: any;
  resolution?: string;
  // KEEP: the install state entry of the entity, when the new version renamed its step
  previousStepName?: string;
}

export interface DeviceUpdateRequest {
  versionId: string;
  // The version installed when the update was planned: the server refuses the update if the package changed since
  fromVersionId?: string;
  // The install method of the new version's steps
  selectedInstallMethod?: string;
  installState: Record<string, any>;
  steps: DeviceUpdateStep[];
}

/** All of the update was applied, or none of it: failedStep names the step that failed, if one did. */
export interface DeviceUpdateResult {
  success: boolean;
  errorMessage?: string;
  failedStep?: string;
  descriptor?: DeviceInstalledItemDescriptor;
  outputs?: Record<string, any>;
}

export interface SolutionTemplateInstalledItemDescriptor {
  type: 'SOLUTION_TEMPLATE';
  createdEntityIds: { entityType: string; id: string }[];
  dashboardGroupId: { id: string };
  dashboardId: { id: string };
  publicId: { id: string };
  mainDashboardPublic: boolean;
  details: string;
}

export type IotHubInstalledItemDescriptor =
  | WidgetInstalledItemDescriptor
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

/** An entity that deleting an installed item keeps, because other installed items use it too. */
export interface IotHubKeptEntity {
  entityId: { id: string; entityType: string };
  name: string;
}

export interface IotHubInstalledItemKeptEntities {
  entities: IotHubKeptEntity[];
  usedByItemsCount: number;
}

export interface IotHubInstalledItem extends BaseData<{id: string}> {
  itemId: string;
  itemVersionId: string;
  itemName: string;
  itemType: string;
  version: string;
  descriptor: IotHubInstalledItemDescriptor;
}

/** The entity the Open action of an installed item opens, and its URL. */
export const getInstalledItemTarget = (descriptor?: IotHubInstalledItemDescriptor): { url: string; entityType: EntityType } | null => {
  if (!descriptor) {
    return null;
  }
  let entityId: string | null = null;
  let entityType: EntityType | null = null;
  let entityGroupId: string | null = null;
  switch (descriptor.type) {
    case 'DEVICE':
      if (descriptor.dashboardId) {
        entityId = descriptor.dashboardId?.id;
        entityType = EntityType.DASHBOARD;
      } else if (descriptor.createdEntityIds) {
        // A gateway package (e.g. TTN) creates no device, only a converter and an integration
        const found = descriptor.createdEntityIds.find(id => id.entityType === EntityType.DEVICE)
          ?? descriptor.createdEntityIds.find(id => id.entityType === EntityType.INTEGRATION);
        if (found) {
          entityId = found.id;
          entityType = found.entityType as EntityType;
        }
      }
      break;
    case 'WIDGET':
      entityId = descriptor.widgetTypeId?.id;
      entityType = EntityType.WIDGET_TYPE;
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
      entityGroupId = descriptor.dashboardGroupId?.id;
      break;
  }
  if (entityType && entityId) {
    let url: string | null;
    if (descriptor.type === 'ALARM_RULE') {
      url = `/alarms/alarm-rules/${entityId}`;
    } else if (descriptor.type === 'SOLUTION_TEMPLATE' && entityGroupId) {
       url = `/dashboards/groups/${entityGroupId}/${entityId}`;
    } else {
       url = getEntityDetailsPageURL(entityId, entityType);
    }
    return url ? { url, entityType } : null;
  }
  return null;
}

export const getInstalledItemUrl = (descriptor?: IotHubInstalledItemDescriptor): string | null =>
  getInstalledItemTarget(descriptor)?.url ?? null;
