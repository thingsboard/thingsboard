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
