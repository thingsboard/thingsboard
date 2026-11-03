// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData, ExportableEntity } from '@shared/models/base-data';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetsBundleId } from '@shared/models/id/widgets-bundle-id';
import { HasTenantId, HasVersion } from '@shared/models/entity.models';

export interface WidgetsBundle extends BaseData<WidgetsBundleId>, HasTenantId, HasVersion, ExportableEntity<WidgetsBundleId> {
  alias?: string;
  title: string;
  image: string;
  scada: boolean;
  description: string;
  order: number;
}
