// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { BaseData, HasId } from '@shared/models/base-data';
import { EntityTableConfig } from './entities-table-config.models';

export interface AddEntityDialogData<T extends BaseData<HasId>> {
  entitiesTableConfig: EntityTableConfig<T>;
}

export interface EntityAction<T extends BaseData<HasId>> {
  event: Event;
  action: string;
  entity: T;
}
