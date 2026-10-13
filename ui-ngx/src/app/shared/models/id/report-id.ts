// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class ReportId implements EntityId {
  entityType = EntityType.REPORT;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
