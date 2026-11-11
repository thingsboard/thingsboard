// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { BaseData, HasId } from '@shared/models/base-data';
import { GroupEntityTableConfig } from '@home/models/group/group-entities-table-config.models';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

export interface AddGroupEntityDialogData<T extends BaseData<HasId>> {
  entitiesTableConfig: EntityTableConfig<T> | GroupEntityTableConfig<T>;
}
