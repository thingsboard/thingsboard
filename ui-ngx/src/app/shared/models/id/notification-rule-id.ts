// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class NotificationRuleId implements EntityId {
  entityType = EntityType.NOTIFICATION_RULE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
