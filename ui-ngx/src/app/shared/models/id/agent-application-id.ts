// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class AgentApplicationId implements EntityId {
  entityType = EntityType.AGENT_APPLICATION;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
