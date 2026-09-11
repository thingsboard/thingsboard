// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { EntityType } from '@shared/models/entity-type.models';
import { Observable } from 'rxjs';
import { EntityAlias } from '@shared/models/alias.models';

export interface EntityAliasSelectCallbacks {
  createEntityAlias: (alias: string, allowedEntityTypes: Array<EntityType>) => Observable<EntityAlias>;
  editEntityAlias: (alias: EntityAlias, allowedEntityTypes: Array<EntityType>) => Observable<EntityAlias>;
}
