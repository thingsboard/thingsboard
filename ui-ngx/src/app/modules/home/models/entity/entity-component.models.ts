///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { BaseData, HasId } from '@shared/models/base-data';
import { EntityTableConfig } from './entities-table-config.models';

export interface AddEntityDialogData<T extends BaseData<HasId>> {
  entitiesTableConfig: EntityTableConfig<T>;
}

export interface EntityAction<T extends BaseData<HasId>> {
  event: Event;
  action: string;
  entity: T;
}
