// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { CustomerId } from '@shared/models/id/customer-id';
import { ContactBased } from '@shared/models/contact-based.model';
import { TenantId } from './id/tenant-id';
import { ExportableEntity } from '@shared/models/base-data';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface Customer extends ContactBased<CustomerId>, HasTenantId, HasVersion, ExportableEntity<CustomerId> {
  tenantId: TenantId;
  title: string;
  additionalInfo?: any;
}

export interface ShortCustomerInfo {
  customerId: CustomerId;
  title: string;
  public: boolean;
}
