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

import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';
import { isDefinedAndNotNull, isNotEmptyStr } from '@core/utils';
import { EntityType } from '@shared/models/entity-type.models';
import { User } from '@shared/models/user.model';

export declare type HasId = EntityId | HasUUID;

export interface BaseData<T extends HasId> {
  createdTime?: number;
  id?: T;
  name?: string;
  label?: string;
}

export function sortEntitiesByIds<I extends HasId, T extends BaseData<I>>(entities: T[], entityIds: string[]): T[] {
  entities.sort((entity1, entity2) => {
    const id1 = entity1.id.id;
    const id2 = entity2.id.id;
    const index1 = entityIds.indexOf(id1);
    const index2 = entityIds.indexOf(id2);
    return index1 - index2;
  });
  return entities;
}

export interface ExportableEntity<T extends EntityId> {
  externalId?: T;
}

export function hasIdEquals(id1: HasId, id2: HasId): boolean {
  if (isDefinedAndNotNull(id1) && isDefinedAndNotNull(id2)) {
    return id1.id === id2.id;
  } else {
    return id1 === id2;
  }
}

export function getEntityDisplayName(entity: BaseData<EntityId>): string {
  if (entity?.id?.entityType === EntityType.USER) {
    const user = entity as User;
    const userName = (user?.firstName ?? '') + " " + (user?.lastName ?? '');
    return isNotEmptyStr(userName) ? userName.trim() : entity?.name;
  }
  return isNotEmptyStr(entity?.label) ? entity.label : entity?.name;
}
