// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { HasUUID } from '@shared/models/id/has-uuid';

export class CustomMenuId implements HasUUID {
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
