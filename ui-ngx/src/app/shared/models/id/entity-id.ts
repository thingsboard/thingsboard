// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { HasUUID } from '@shared/models/id/has-uuid';
import { isDefinedAndNotNull } from '@core/utils';

export interface EntityId extends HasUUID {
  entityType: EntityType | AliasEntityType;
}

export const entityIdEquals = (entityId1: EntityId, entityId2: EntityId): boolean => {
  if (isDefinedAndNotNull(entityId1) && isDefinedAndNotNull(entityId2)) {
    return entityId1.id === entityId2.id && entityId1.entityType === entityId2.entityType;
  } else {
    return entityId1 === entityId2;
  }
};

const entityIdsSort = (entityId1: EntityId, entityId2: EntityId): number => {
  if (entityId1.entityType === entityId2.entityType) {
    if (isDefinedAndNotNull(entityId1.id) && isDefinedAndNotNull(entityId2.id)) {
      return entityId1.id.localeCompare(entityId2.id);
    } else if (!isDefinedAndNotNull(entityId1.id) && !isDefinedAndNotNull(entityId2.id)) {
      return 0;
    } else {
      return isDefinedAndNotNull(entityId1.id) ? 1 : -1;
    }
  } else {
    if (isDefinedAndNotNull(entityId1.entityType) && isDefinedAndNotNull(entityId2.entityType)) {
      return entityId1.entityType.localeCompare(entityId2.entityType);
    } else if (!isDefinedAndNotNull(entityId1.entityType) && !isDefinedAndNotNull(entityId2.entityType)) {
      return 0;
    } else {
      return isDefinedAndNotNull(entityId1.entityType) ? 1 : -1;
    }
  }
};

export const entityIdsEquals = (entityIds1: EntityId[], entityIds2: EntityId[]): boolean => {
  if (isDefinedAndNotNull(entityIds1) && isDefinedAndNotNull(entityIds2)) {
    if (entityIds1.length === entityIds2.length) {
      entityIds1 = [...entityIds1].sort(entityIdsSort);
      entityIds2 = [...entityIds2].sort(entityIdsSort);
      for (let index = 0; index < entityIds1.length; index++) {
        if (!entityIdEquals(entityIds1[index], entityIds2[index])) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  } else {
    return entityIds1 === entityIds2;
  }
};

export const entityIdsContains = (entityIds: EntityId[], checkEntityId: EntityId): boolean => {
  for (const entityId of entityIds) {
    if (entityIdEquals(entityId, checkEntityId)) {
      return true;
    }
  }
  return false;
}
