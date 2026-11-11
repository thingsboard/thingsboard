// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
import { HasUUID } from '@shared/models/id/has-uuid';

export class AuditLogId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
