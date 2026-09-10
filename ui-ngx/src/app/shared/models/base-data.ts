// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityId } from '@shared/models/id/entity-id';
import { HasUUID } from '@shared/models/id/has-uuid';
import { isDefinedAndNotNull } from '@core/utils';

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
