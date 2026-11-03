// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class CalculatedFieldId implements EntityId {
  entityType = EntityType.CALCULATED_FIELD;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
