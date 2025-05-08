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

import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { HasUUID } from '@shared/models/id/has-uuid';
import { isDefinedAndNotNull } from '@core/utils';

export interface EntityId extends HasUUID {
  entityType: EntityType | AliasEntityType;
}

export function entityIdEquals(entityId1: EntityId, entityId2: EntityId): boolean {
  if (isDefinedAndNotNull(entityId1) && isDefinedAndNotNull(entityId2)) {
    return entityId1.id === entityId2.id && entityId1.entityType === entityId2.entityType;
  } else {
    return entityId1 === entityId2;
  }
}
