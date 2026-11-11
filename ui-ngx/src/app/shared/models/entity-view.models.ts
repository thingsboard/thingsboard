// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { BaseData, ExportableEntity, GroupEntityInfo } from '@shared/models/base-data';
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

export type EntityViewInfo = EntityView & GroupEntityInfo<EntityViewId>;

export interface EntityViewSearchQuery extends EntitySearchQuery {
  entityViewTypes: Array<string>;
}
