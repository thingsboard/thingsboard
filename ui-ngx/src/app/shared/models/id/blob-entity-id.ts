// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class BlobEntityId implements EntityId {
  entityType = EntityType.BLOB_ENTITY;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
