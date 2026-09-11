// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class MobileAppBundleId implements EntityId {
  entityType = EntityType.MOBILE_APP_BUNDLE;
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}
