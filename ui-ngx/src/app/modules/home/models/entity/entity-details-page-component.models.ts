// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { EntityComponent } from '@home/components/entity/entity.component';
import { BaseData, HasId } from '@shared/models/base-data';

export interface IEntityDetailsPageComponent {
  entityComponent: EntityComponent<BaseData<HasId>>;
  onToggleEditMode(isEdit: boolean): void;
  reload(): void;
  goBack(): void;
}
