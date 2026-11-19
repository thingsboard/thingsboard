// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class ExtensionId implements EntityId {
  entityType = EntityType.EXTENSION;
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}
