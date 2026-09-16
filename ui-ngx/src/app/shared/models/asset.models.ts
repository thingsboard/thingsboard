// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { AssetId } from './id/asset-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { AssetProfileId } from '@shared/models/id/asset-profile-id';
import { RuleChainId } from '@shared/models/id/rule-chain-id';
import { DashboardId } from '@shared/models/id/dashboard-id';
import { EntityInfoData, HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface AssetProfile extends BaseData<AssetProfileId>, HasTenantId, HasVersion, ExportableEntity<AssetProfileId> {
  tenantId?: TenantId;
  name: string;
  description?: string;
  default?: boolean;
  image?: string;
  defaultRuleChainId?: RuleChainId;
  defaultDashboardId?: DashboardId;
  defaultQueueName?: string;
  defaultEdgeRuleChainId?: RuleChainId;
}

export interface AssetProfileInfo extends EntityInfoData {
  tenantId?: TenantId;
  image?: string;
  defaultDashboardId?: DashboardId;
}

export interface Asset extends BaseData<AssetId>, HasTenantId, HasVersion, ExportableEntity<AssetId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  label: string;
  assetProfileId?: AssetProfileId;
  additionalInfo?: any;
}

export interface AssetInfo extends Asset {
  customerTitle: string;
  customerIsPublic: boolean;
  assetProfileName: string;
}

export interface AssetSearchQuery extends EntitySearchQuery {
  assetTypes: Array<string>;
}
