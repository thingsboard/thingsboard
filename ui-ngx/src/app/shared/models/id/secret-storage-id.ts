// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class SecretStorageId implements EntityId {
  entityType = EntityType.SECRET;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
