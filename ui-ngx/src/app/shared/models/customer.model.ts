// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { CustomerId } from '@shared/models/id/customer-id';
import { ContactBased } from '@shared/models/contact-based.model';
import { TenantId } from './id/tenant-id';
import { ExportableEntity, GroupEntityInfo } from '@shared/models/base-data';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';
import { CustomMenuId } from '@shared/models/id/custom-menu-id';

export interface Customer extends ContactBased<CustomerId>, HasTenantId, HasVersion, ExportableEntity<CustomerId> {
  tenantId: TenantId;
  parentCustomerId?: CustomerId;
  title: string;
  customMenuId?: CustomMenuId;
  additionalInfo?: any;
}

export const publicCustomer = 'Public';

export type CustomerInfo = Customer & GroupEntityInfo<CustomerId>;

export interface ShortCustomerInfo {
  customerId: CustomerId;
  title: string;
  public: boolean;
}
