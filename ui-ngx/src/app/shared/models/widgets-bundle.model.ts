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
