// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class GroupPermissionId implements EntityId {
  entityType = EntityType.GROUP_PERMISSION;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
