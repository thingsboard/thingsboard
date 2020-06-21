///
/// Copyright © 2016-2020 The Thingsboard Authors
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
import { TenantId } from '@shared/models/id/tenant-id';
import { CustomerId } from '@shared/models/id/customer-id';
import { EdgeId } from '@shared/models/id/edge-id';
import { EntitySearchQuery } from '@shared/models/relation.models';

export interface Edge extends BaseData<EdgeId> {
  tenantId?: TenantId;
  customerId?: CustomerId;
  name: string;
  type: string;
  secret: string;
  routingKey: string;
  label?: string;
  additionalInfo?: any;
}

export interface EdgeInfo extends Edge {
  customerTitle: string;
  customerIsPublic: boolean;
  // assignedCustomers?: Array<ShortCustomerInfo> //TODO: deaflynx check usage
}

export interface EdgeSearchQuery extends EntitySearchQuery {
  edgeTypes: Array<string>;
}
