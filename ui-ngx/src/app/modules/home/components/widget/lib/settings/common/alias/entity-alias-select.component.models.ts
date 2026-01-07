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

import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { EntityAlias } from '@shared/models/alias.models';

export interface EntityAliasSelectCallbacks {
  createEntityAlias: (alias: string, allowedEntityTypes: Array<EntityType>) => Observable<EntityAlias>;
  editEntityAlias: (alias: EntityAlias, allowedEntityTypes: Array<EntityType>) => Observable<EntityAlias>;
}
