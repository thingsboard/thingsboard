// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class CustomerId implements EntityId {
  entityType = EntityType.CUSTOMER;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
