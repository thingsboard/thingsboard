// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
