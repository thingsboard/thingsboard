// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { HasUUID } from '@shared/models/id/has-uuid';

export class EventId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
