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
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EntityViewId } from '@shared/models/id/entity-view-id';
import { EntityId } from '@shared/models/id/entity-id';
import { EntitySearchQuery } from '@shared/models/relation.models';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface AttributesEntityView {
  cs: Array<string>;
  ss: Array<string>;
  sh: Array<string>;
}

export interface TelemetryEntityView {
  timeseries: Array<string>;
  attributes: AttributesEntityView;
}

export interface EntityView extends BaseData<EntityViewId>, HasTenantId, HasVersion, ExportableEntity<EntityViewId> {
  tenantId: TenantId;
  customerId: CustomerId;
  entityId: EntityId;
  name: string;
  type: string;
  keys: TelemetryEntityView;
  startTimeMs: number;
  endTimeMs: number;
  additionalInfo?: any;
}

export interface EntityViewInfo extends EntityView {
  customerTitle: string;
  customerIsPublic: boolean;
}

export interface EntityViewSearchQuery extends EntitySearchQuery {
  entityViewTypes: Array<string>;
}
