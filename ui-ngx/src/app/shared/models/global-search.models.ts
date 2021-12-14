///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { GlobalSearchId } from '@shared/models/id/global-search-id';
import { EntityType } from '@shared/models/entity-type.models';
import { TenantId } from '@shared/models/id/tenant-id';
import { EntityId } from '@shared/models/id/entity-id';

export interface GlobalSearchInfo extends BaseData<GlobalSearchId> {
  name: string;
  createdTime: number;
  lastActivityTime?: number;
  type: string;
  tenantInfo?: TenantId;
  ownerInfo?: OwnerId;
}

export class OwnerId implements EntityId {
  entityType: EntityType;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
  name?: string;
}

export enum GlobalSearchEntityTypes {
  DEVICE = EntityType.DEVICE,
  ASSET = EntityType.ASSET,
  RULE_CHAIN = EntityType.RULE_CHAIN,
  DASHBOARD = EntityType.DASHBOARD,
  DEVICE_PROFILE = EntityType.DEVICE_PROFILE,
  CUSTOMER = EntityType.CUSTOMER,
  USER = EntityType.USER,
  ENTITY_VIEW = EntityType.ENTITY_VIEW,
  OTA_PACKAGE = EntityType.OTA_PACKAGE,
  EDGE = EntityType.EDGE,
  TB_RESOURCE = EntityType.TB_RESOURCE,
  TENANT = EntityType.TENANT,
  TENANT_PROFILE = EntityType.TENANT_PROFILE,
  WIDGETS_BUNDLE = EntityType.WIDGETS_BUNDLE
}

export const GlobalSearchEntityTypesTranslation = new Map<GlobalSearchEntityTypes, string>(
  [
    [GlobalSearchEntityTypes.ASSET, 'entity.type-assets'],
    [GlobalSearchEntityTypes.CUSTOMER, 'entity.type-customers'],
    [GlobalSearchEntityTypes.DEVICE, 'entity.type-devices'],
    [GlobalSearchEntityTypes.DASHBOARD, 'entity.type-dashboards'],
    [GlobalSearchEntityTypes.EDGE, 'entity.type-edges'],
    [GlobalSearchEntityTypes.DEVICE_PROFILE, 'entity.type-device-profiles'],
    [GlobalSearchEntityTypes.ENTITY_VIEW, 'entity.type-entity-views'],
    [GlobalSearchEntityTypes.OTA_PACKAGE, 'entity.type-ota-packages'],
    [GlobalSearchEntityTypes.RULE_CHAIN, 'entity.type-rulechains'],
    [GlobalSearchEntityTypes.TB_RESOURCE, 'entity.type-tb-resources'],
    [GlobalSearchEntityTypes.TENANT_PROFILE, 'entity.type-tenant-profiles'],
    [GlobalSearchEntityTypes.USER, 'entity.type-users'],
    [GlobalSearchEntityTypes.WIDGETS_BUNDLE, 'entity.type-widgets-bundle'],
    [GlobalSearchEntityTypes.TENANT, 'entity.type-tenants']
  ]
);
